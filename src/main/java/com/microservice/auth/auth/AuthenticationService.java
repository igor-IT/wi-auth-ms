package com.microservice.auth.auth;

import com.microservice.auth.code.*;
import com.microservice.auth.config.JwtService;
import com.microservice.auth.exceptions.InvalidCodeException;
import com.microservice.auth.exceptions.PhoneNotFoundException;
import com.microservice.auth.exceptions.UserAlreadyExistException;
import com.microservice.auth.exceptions.UserNotExistException;
import com.microservice.auth.token.RefreshToken;
import com.microservice.auth.token.RefreshTokenRepository;
import com.microservice.auth.user.Locale;
import com.microservice.auth.user.*;
import lombok.RequiredArgsConstructor;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;

import static com.microservice.auth.code.CodeStatus.USED;
import static com.microservice.auth.code.CodeStatus.VALIDATED;

@Service
@RequiredArgsConstructor
public class AuthenticationService {
	private final UserRepository repository;
	private final RefreshTokenRepository refreshTokenRepository;
	private final RoleRepository roleRepository;
	private final CodeRepository codeRepository;
	private final UserRepository userRepository;
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
	}

	public AuthenticationResponse register(RegisterRequest request) {
		validateCode(request.getPhone(), request.getLocale());

		Role role = roleRepository.findByName(RoleStatus.USER.name()).orElseThrow();
		var user = User.builder()
				.firstname(request.getFirstname())
				.lastname(request.getLastname())
				.email(request.getEmail())
				.phone(request.getPhone())
				.password(passwordEncoder.encode(request.getPassword()))
				.locale(request.getLocale())
				.roles(new HashSet<>(List.of(role)))
				.accountType(request.getAccountType())
				.build();
		repository.save(user);
		return generateTokens(user);
	}

	public AuthenticationResponse createPassword(CreatePasswordRequest request) {
		validateCode(request.getPhone(), request.getLocale());
		var user = userRepository.findByPhone(request.getPhone())
				.orElseThrow(() -> new UserNotExistException(request.getLocale()));
		user.setPassword(passwordEncoder.encode(request.getPassword()));
		repository.save(user);
		return generateTokens(user);
	}

	public AuthenticationResponse authenticate(AuthenticationRequest request) {
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
		return AuthenticationResponse.builder()
				.accessToken(jwtToken)
				.refreshToken(refreshToken)
				.build();
	}

	public void requestSMS(SMSCodeRequest request) {
		String phone = request.getPhone();
		if (repository.existsByPhone(phone)) {
			throw new UserAlreadyExistException(request.getLocale());
		} else {
			buildCode(phone, Type.REGISTRATION, request);
		}
	}

	public void requestSMSByReset(SMSCodeRequest request) {
		String phone = request.getPhone();
		if (!repository.existsByPhone(phone)) {
			throw new UserNotExistException(request.getLocale());
		} else {
			buildCode(phone, Type.valueOf(request.getType()), request);
		}
	}

	public void validateSMS(ValidateRequest request) {
		codeRepository.findFirstByPhoneOrderByDateDesc(request.getPhone())
				.map(code -> {
					if (!Objects.equals(code.getCode(), request.getCode())) {
						throw new InvalidCodeException(request.getLocale());
					}
					code.setStatus(VALIDATED);
					code.setType(request.getType());
					return codeRepository.save(code);
				})
				.orElseThrow(() -> new PhoneNotFoundException(request.getLocale()));
	}

	private void buildCode(String phone, Type type, SMSCodeRequest request) {
		var code = Code.builder()
				.status(CodeStatus.CREATED)
				.code(String.format("%04d", new Random().nextInt(10000)))
				.phone(phone)
				.type(type)
				.date(new Date())
				.build();
		codeRepository.save(code);
		streamBridge.send("code-topic", new CodeSendEvent(code.getCode(), request.getLocale()));
	}

	private AuthenticationResponse generateTokens(User user) {
		var jwtToken = jwtService.generateToken(user);
		var refreshToken = jwtService.generateRefreshToken(user);
		saveUserToken(user, refreshToken);
		return AuthenticationResponse.builder()
				.accessToken(jwtToken)
				.refreshToken(refreshToken)
				.build();
	}

	private void validateCode(String phone, Locale locale) {
		Code code = codeRepository.findFirstByPhoneOrderByDateDesc(phone)
				.orElseThrow(() -> new PhoneNotFoundException(locale));
		if (code.getStatus() != VALIDATED) {
			throw new InvalidCodeException(locale);
		}
		code.setStatus(USED);
		codeRepository.save(code);
	}
	private void revokeAllUserTokens(User user) {
		var validUserTokens = refreshTokenRepository.findAllByUserId((user.getId()));
		if (validUserTokens.isEmpty())
			return;
		refreshTokenRepository.deleteAll(validUserTokens);
	}
}
