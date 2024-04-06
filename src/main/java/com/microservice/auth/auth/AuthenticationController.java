package com.microservice.auth.auth;

import com.microservice.auth.exceptions.TooManyRequestException;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Validated
@Slf4j
public class AuthenticationController {
	@Value("${application.security.jwt.access-token-header}")
	private String accessTokenHeader;

	@Value("${application.security.jwt.refresh-token-header}")
	private String refreshTokenHeader;

	private final AuthenticationService service;
	private final Bucket bucket;

	@Autowired
	public AuthenticationController(AuthenticationService service) {
		log.info("AuthenticationController created");
		this.service = service;
		Bandwidth limit = Bandwidth.classic(20, Refill.greedy(20, Duration.ofMinutes(1)));
		this.bucket = Bucket.builder()
				.addLimit(limit)
				.build();
	}

	@PostMapping("/sign-up/by-phone/create-account")
	@ResponseStatus(HttpStatus.CREATED)
	public ResponseEntity<AuthenticationResponse> register(
			@Valid @RequestBody RegisterRequest request
	) {
		AuthenticationResponse response = service.register(request);
		HttpHeaders headers = setHttpHeaders(response);
		return ResponseEntity.ok().headers(headers).build();
	}

	@PostMapping("/sign-in/by-phone")
	public ResponseEntity<AuthenticationResponse> authenticate(
			@Valid @RequestBody AuthenticationRequest request
	) {
		AuthenticationResponse response = service.authenticate(request);
		HttpHeaders headers = setHttpHeaders(response);
		return ResponseEntity.ok().headers(headers).build();
	}


	@PostMapping("/sign-up/by-phone/request-sms-code")
	@ResponseStatus(HttpStatus.OK)
	public void requestSMS(@Valid @RequestBody SMSCodeRequest request) {
		if (bucket.tryConsume(1)) {
			service.requestSMS(request);

		} else {
			throw new TooManyRequestException(request.getLocale());
		}
	}

	@PostMapping("/reset-password/by-phone/request-sms-code")
	@ResponseStatus(HttpStatus.OK)
	public void resetPasswordRequest(@Valid @RequestBody SMSCodeRequest request) {
		if (bucket.tryConsume(1)) {
			service.requestSMSByReset(request);
		} else {
			throw new TooManyRequestException(request.getLocale());
		}
	}

	@PostMapping("/reset-password/by-phone/create-password")
	@ResponseStatus(HttpStatus.OK)
	public ResponseEntity<AuthenticationResponse> createPassword(@Valid @RequestBody CreatePasswordRequest request) {
		if (bucket.tryConsume(1)) {
			AuthenticationResponse response = service.createPassword(request);
			HttpHeaders headers = setHttpHeaders(response);
			return ResponseEntity.ok().headers(headers).build();
		} else {
			throw new TooManyRequestException(request.getLocale());
		}
	}

	@PostMapping("/validate-sms-code")
	@ResponseStatus(HttpStatus.OK)
	public void validateSMS(@Valid @RequestBody ValidateRequest request) {
		service.validateSMS(request);
	}

	@NotNull
	private HttpHeaders setHttpHeaders(AuthenticationResponse response) {
		HttpHeaders headers = new HttpHeaders();
		String bearer = "Bearer ";
		headers.add(accessTokenHeader, bearer + response.getAccessToken());
		headers.add(refreshTokenHeader, response.getRefreshToken());
		return headers;
	}

}
