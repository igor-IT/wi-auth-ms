package com.microservice.auth.auth;

import com.microservice.auth.code.*;
import com.microservice.auth.config.JwtService;
import com.microservice.auth.exceptions.InvalidCodeException;
import com.microservice.auth.exceptions.PhoneNotFoundException;
import com.microservice.auth.exceptions.UserAlreadyExistException;
import com.microservice.auth.exceptions.UserNotExistException;
import com.microservice.auth.token.RefreshToken;
import com.microservice.auth.token.RefreshTokenRepository;
import com.microservice.auth.user.Role;
import com.microservice.auth.user.User;
import com.microservice.auth.user.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.Instant;
import java.util.Date;
import java.util.Objects;
import java.util.Random;

import static com.microservice.auth.code.CodeStatus.USED;
import static com.microservice.auth.code.CodeStatus.VALIDATED;

@Service
@RequiredArgsConstructor
public class AuthenticationService {
	private final UserRepository repository;
	private final RefreshTokenRepository refreshTokenRepository;
	private final CodeRepository codeRepository;
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
		Code code = codeRepository.findFirstByPhoneOrderByDateDesc(request.getPhone())
				.orElseThrow(PhoneNotFoundException::new);
		if (code.getStatus() != VALIDATED || !Objects.equals(request.getCode(), code.getCode())) {
			throw new InvalidCodeException();
		}
		// TODO: ADD VALIDATION FOR PASSWORD
		code.setStatus(USED);
		codeRepository.save(code);

		var user = User.builder()
				.firstname(request.getFirstname())
				.lastname(request.getLastname())
				.email(request.getEmail())
				.phone(request.getPhone())
				.password(passwordEncoder.encode(request.getPassword()))
				.locale(request.getLocale())
				.role(Role.USER)
				.build();
		repository.save(user);
		var jwtToken = jwtService.generateToken(user);
		var refreshToken = jwtService.generateRefreshToken(user);
		saveUserToken(user, refreshToken);
		return AuthenticationResponse.builder()
				.accessToken(jwtToken)
				.refreshToken(refreshToken)
				.build();
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
		saveUserToken(user, jwtToken);
		return AuthenticationResponse.builder()
				.accessToken(jwtToken)
				.refreshToken(refreshToken)
				.build();
	}

	// TODO: Maybe should add "is expired" field to token ???
	private void revokeAllUserTokens(User user) {
		var validUserTokens = refreshTokenRepository.findAllByUserId((user.getId()));
		if (validUserTokens.isEmpty())
			return;
		refreshTokenRepository.deleteAll(validUserTokens);
	}

	public void refreshToken(
			HttpServletRequest request,
			HttpServletResponse response
	) throws IOException {
		final String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
		final String refreshToken;
		final String userEmail;
		if (authHeader == null || !authHeader.startsWith("Bearer ")) {
			return;
		}
		refreshToken = authHeader.substring(7);
		userEmail = jwtService.extractPhone(refreshToken);
		if (userEmail != null) {
			var user = this.repository.findByPhone(userEmail)
					.orElseThrow();
			if (jwtService.isTokenValid(refreshToken, user)) {
				var accessToken = jwtService.generateToken(user);
				revokeAllUserTokens(user);
				saveUserToken(user, refreshToken);
				var authResponse = AuthenticationResponse.builder()
						.accessToken(accessToken)
						.refreshToken(refreshToken)
						.build();
				new ObjectMapper().writeValue(response.getOutputStream(), authResponse);
			}
		}
	}

	public void requestSMS(SMSCodeRequest request) {
		String phone = request.getPhone();
		if (repository.existsByPhone(phone)) {
			throw new UserAlreadyExistException();
		}
		buildCode(phone, Type.REGISTRATION, request);
	}
	public void requestSMSByReset(SMSCodeRequest request) {
		String phone = request.getPhone();
		if (!repository.existsByPhone(phone)) {
			throw new UserNotExistException();
		}
		buildCode(phone, Type.RESET_PASSWORD, request);
	}

	public void validateSMS(ValidateRequest request) {
		codeRepository.findFirstByPhoneOrderByDateDesc(request.getPhone())
				.map(code -> {
					if (!Objects.equals(code.getCode(), request.getCode())) {
						throw new InvalidCodeException();
					}
					code.setStatus(VALIDATED);
					code.setType(request.getType());
					return codeRepository.save(code);
				})
				.orElseThrow(PhoneNotFoundException::new);


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
}
