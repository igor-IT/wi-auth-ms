package com.microservice.auth.service;

import com.microservice.auth.config.JwtService;
import com.microservice.auth.data.*;
import com.microservice.auth.exceptions.InvalidCodeException;
import com.microservice.auth.exceptions.PhoneOrEmailNotFoundException;
import com.microservice.auth.exceptions.UserAlreadyExistException;
import com.microservice.auth.exceptions.UserNotExistException;
import com.microservice.auth.persistence.*;
import com.microservice.auth.persistence.RefreshTokenRepository;
import com.microservice.auth.data.Locale;
import com.microservice.auth.request.*;
import com.microservice.auth.response.AuthenticationResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;

import static com.microservice.auth.data.CodeStatus.USED;
import static com.microservice.auth.data.CodeStatus.VALIDATED;

@Service
@Slf4j
@RequiredArgsConstructor
public class AuthenticationService {
	private final UserRepository repository;
	private final RefreshTokenRepository refreshTokenRepository;
	private final RoleRepository roleRepository;
	private final CodeRepository codeRepository;
	//private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;
	private final JwtService jwtService;
	private final AuthenticationManager authenticationManager;
	private final StreamBridge streamBridge;

	private void saveUserToken(User user, String jwtToken) {
		var token = RefreshToken.builder()
				.userId(user.getId())
				.token(jwtToken)
				.expiryDate(Instant.now().plusMillis(jwtService.getRefreshExpiration()))
				.build();
		refreshTokenRepository.save(token);
		log.info("User`s token have been saved");
	}

	public AuthenticationResponse registerByPhone(RegisterRequestByPhone request) {
		if (null == request.getPhone() || request.getPhone().isBlank()){
			throw new IllegalArgumentException("Phone cannot be null or blank");
		}
		validateCodeByPhone(request.getPhone(), request.getLocale());

		Role role = roleRepository.findById("661a68f87cb7bb79bc96c5c0").orElseThrow();
		var user = User.builder()
				.firstname(request.getFirstname())
				.lastname(request.getLastname())
				.email(null)
				.phone(request.getPhone())
				.imageId(request.getMediaPhotoId())
				.isEnabled(true)
				.isPhoneVerified(true)
				.isEmailVerified(false)
				.registrationDate(new Date())
				.password(passwordEncoder.encode(request.getPassword()))
				.locale(request.getLocale())
				.roles(new HashSet<>(List.of(role)))
				.accountType(request.getAccountType())
				.build();
		repository.save(user);
		log.info("User {} registered", user.getId());
		return generateTokens(user);
	}

	public AuthenticationResponse registerByEmail(RegisterRequestByEmail request) {
		if (null == request.getEmail() || request.getEmail().isBlank()){
			throw new IllegalArgumentException("Email cannot be null or blank");
		}
		validateCodeByEmail(request.getEmail(), request.getLocale());

		//Role role = roleRepository.findById("661a68f87cb7bb79bc96c5c0").orElseThrow();
		var user = User.builder()
				.firstname(request.getFirstname())
				.lastname(request.getLastname())
				.email(request.getEmail())
				.phone(null)
				.imageId(request.getMediaPhotoId())
				.isEnabled(true)
				.isPhoneVerified(false)
				.isEmailVerified(true)
				.registrationDate(new Date())
				.password(passwordEncoder.encode(request.getPassword()))
				.locale(request.getLocale())
				.roles(new HashSet<>(new ArrayList<>()))
				.accountType(request.getAccountType())
				.build();
		repository.save(user);
		log.info("User {} registered", user.getId());
		return generateTokens(user);
	}

//	public AuthenticationResponse createPassword(CreatePasswordRequest request) {
//		validateCode(request.getPhone(), request.getLocale());
//		var user = repository.findByPhone(request.getPhone())
//				.orElseThrow(() -> new UserNotExistException(request.getLocale()));
//		user.setPassword(passwordEncoder.encode(request.getPassword()));
//		repository.save(user);
//		log.info("For user {} created and save password", user.getId());
//		return generateTokens(user);
//	}

	public AuthenticationResponse authenticateByPhone(AuthenticationRequestByPhone request) {
		authenticationManager.authenticate(
				new UsernamePasswordAuthenticationToken(
						request.getPhone(),
						request.getPassword()
				)
		);
		var user = repository.findByPhone(request.getPhone())
				.orElseThrow();
		var jwtToken = jwtService.generateToken(user);
		var refreshToken = jwtService.generateRefreshToken(user);
		revokeAllUserTokens(user);
		saveUserToken(user, refreshToken);
		log.info("User {} authenticated", user.getId());
		return AuthenticationResponse.builder()
				.accessToken(jwtToken)
				.refreshToken(refreshToken)
				.build();
	}

	public AuthenticationResponse authenticateByEmail(AuthenticationRequestByEmail request) {
		authenticationManager.authenticate(
				new UsernamePasswordAuthenticationToken(
						request.getEmail(),
						request.getPassword()
				)
		);
		var user = repository.findByEmail(request.getEmail())
				.orElseThrow();
		var jwtToken = jwtService.generateToken(user);
		var refreshToken = jwtService.generateRefreshToken(user);
		revokeAllUserTokens(user);
		saveUserToken(user, refreshToken);
		log.info("User {} authenticated", user.getId());
		return AuthenticationResponse.builder()
				.accessToken(jwtToken)
				.refreshToken(refreshToken)
				.build();
	}

	public AuthenticationResponse authenticateWithOauth(String email) {
		var user = repository.findByEmail(email).orElseThrow();
		var jwtToken = jwtService.generateToken(user);
		var refreshToken = jwtService.generateRefreshToken(user);
		revokeAllUserTokens(user);
		saveUserToken(user, refreshToken);
		log.info("User {} have been authenticated with 0auth", user.getId());
		return AuthenticationResponse.builder()
				.accessToken(jwtToken)
				.refreshToken(refreshToken)
				.build();
	}

	public void requestCodeSMS(SMSCodeRequest request) {
		String phone = request.getPhone();
		if (repository.existsByPhone(phone)) {
			log.warn("User with phone {} already exist", phone);
			throw new UserAlreadyExistException(request.getLocale());
		} else {
			buildCode(phone, Type.REGISTRATION, request, null);
		}
	}

	public void requestCodeEmail(EmailCodeRequest request) {
		String email = request.getEmail();
		if (repository.existsByEmail(email)) {
			log.warn("User with email {} already exist", email);
			throw new UserAlreadyExistException(request.getLocale());
		} else {
			buildCode(email, Type.REGISTRATION, null,request);
		}
	}

	public void requestSMSByReset(SMSCodeRequest request) {
		String phone = request.getPhone();
		if (!repository.existsByPhone(phone)) {
			log.warn("User with phone {} does not exist", phone);
			throw new UserNotExistException(request.getLocale());
		} else {
			buildCode(phone, Type.valueOf(request.getType()), request, null);
		}
	}

	public void validateCode(ValidateRequest request) {
		codeRepository.findFirstByClientOrderByDateDesc(request.getClientIdentifier())
				.map(code -> {
					if (!Objects.equals(code.getCode(), request.getCode())) {
						log.warn("Code in db does not equal with code in request");
						throw new InvalidCodeException(request.getLocale());
					}
					code.setStatus(VALIDATED);
					code.setType(request.getType());
					log.info("Code {} has been successfully validated", code.getCode());
					return codeRepository.save(code);
				})
				.orElseThrow(() -> new PhoneOrEmailNotFoundException(request.getLocale()));
	}

	private void buildCode(String client, Type type, SMSCodeRequest smsCodeRequest,EmailCodeRequest emailCodeRequest) {
		if (null != smsCodeRequest){
			var code = Code.builder()
					.status(CodeStatus.CREATED)
					.code(String.format("%04d", new Random().nextInt(10000)))
					.resource(ResourceType.PHONE)
					.client(client)
					.type(type)
					.date(new Date())
					.build();
			codeRepository.save(code);
			log.info("Code {} created", code.getId());
			streamBridge.send("code-topic", new CodeSendEvent(code.getCode(), smsCodeRequest.getLocale()));
			log.info("Message sent to code-topic");
		}
		else if (null != emailCodeRequest){
			Code code = Code.builder()
                    .status(CodeStatus.CREATED)
                    .code(String.format("%04d", new Random().nextInt(10000)))
					.resource(ResourceType.EMAIL)
                    .client(client)
                    .type(type)
                    .date(new Date())
                    .build();
            codeRepository.save(code);
            log.info("Code {} created", code.getId());
            streamBridge.send("code-topic", new CodeSendEvent(code.getCode(), emailCodeRequest.getLocale()));
            log.info("Message sent to code-topic");
		}
	}

	private AuthenticationResponse generateTokens(User user) {
		var jwtToken = jwtService.generateToken(user);
		var refreshToken = jwtService.generateRefreshToken(user);
		saveUserToken(user, refreshToken);
		log.info("Tokens have been generated for User {}", user.getId());
		return AuthenticationResponse.builder()
				.accessToken(jwtToken)
				.refreshToken(refreshToken)
				.build();
	}

	private void validateCodeByPhone(String phone, Locale locale) {
		Code code = codeRepository.findFirstByClientOrderByDateDesc(phone)
				.orElseThrow(() -> new PhoneOrEmailNotFoundException(locale));
		if (code.getStatus() != VALIDATED) {
			log.warn("Code status is not VALIDATED");
			throw new InvalidCodeException(locale);
		}
		code.setStatus(USED);
		codeRepository.save(code);
	}

	private void validateCodeByEmail(String email, Locale locale) {
		Code code = codeRepository.findFirstByClientOrderByDateDesc(email)
				.orElseThrow(() -> new PhoneOrEmailNotFoundException(locale));
		if (code.getStatus() != VALIDATED) {
			log.warn("Code status is not VALIDATED");
			throw new InvalidCodeException(locale);
		}
		code.setStatus(USED);
		codeRepository.save(code);
	}

	private void revokeAllUserTokens(User user) {
		var validUserTokens = refreshTokenRepository.findAllByUserId((user.getId()));
		if (validUserTokens.isEmpty())
			return;
		log.info("All tokens user {} have been deleted", user.getId());
		refreshTokenRepository.deleteAll(validUserTokens);
	}
}
