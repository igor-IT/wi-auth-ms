package com.alibou.security.auth;

import com.alibou.security.code.Code;
import com.alibou.security.code.CodeRepository;
import com.alibou.security.code.CodeSendEvent;
import com.alibou.security.code.CodeStatus;
import com.alibou.security.config.JwtService;
import com.alibou.security.exceptions.InvalidCodeException;
import com.alibou.security.exceptions.PhoneNotFoundException;
import com.alibou.security.exceptions.UserAlreadyExistException;
import com.alibou.security.token.RefreshToken;
import com.alibou.security.token.RefreshTokenRepository;
import com.alibou.security.token.TokenRepository;
import com.alibou.security.user.Role;
import com.alibou.security.user.User;
import com.alibou.security.user.UserRepository;
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

import static com.alibou.security.code.CodeStatus.USED;
import static com.alibou.security.code.CodeStatus.VALIDATED;

@Service
@RequiredArgsConstructor
public class AuthenticationService {
	private final UserRepository repository;
	private final TokenRepository tokenRepository;
	private final RefreshTokenRepository refreshTokenRepository;
	private final CodeRepository codeRepository;
	private final PasswordEncoder passwordEncoder;
	private final JwtService jwtService;
	private final AuthenticationManager authenticationManager;
	private final StreamBridge streamBridge;

	private void saveUserToken(User user, String jwtToken) {
		var token = RefreshToken.builder()
				.phone(user.getPhone())
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
				.firstname(request.getFirst_name())
				.lastname(request.getLast_name())
				.phone(request.getPhone())
				.password(passwordEncoder.encode(request.getPassword()))
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


	private void revokeAllUserTokens(User user) {
		var validUserTokens = tokenRepository.findAllByUserIdAndExpiredFalseAndRevokedFalse((user.getId()));
		if (validUserTokens.isEmpty())
			return;
		validUserTokens.forEach(token -> {
			token.setExpired(true);
			token.setRevoked(true);
		});
		tokenRepository.saveAll(validUserTokens);
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
		var code = Code.builder()
				.status(CodeStatus.CREATED)
				.code(String.format("%04d", new Random().nextInt(10000)))
				.phone(phone)
				.date(new Date())
				.build();
		codeRepository.save(code);
		streamBridge.send("code-topic", new CodeSendEvent(code.getCode(), request.getLocale()));
	}

	public void validateSMS(ValidateRequest request) {
		codeRepository.findFirstByPhoneOrderByDateDesc(request.getPhone())
				.map(code -> {
					if (!Objects.equals(code.getCode(), request.getCode())) {
						throw new InvalidCodeException();
					}
					code.setStatus(VALIDATED);
					return codeRepository.save(code);
				})
				.orElseThrow(PhoneNotFoundException::new);


	}
}
