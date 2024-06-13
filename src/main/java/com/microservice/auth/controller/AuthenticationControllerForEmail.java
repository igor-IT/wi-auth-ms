package com.microservice.auth.controller;

import com.microservice.auth.exceptions.TooManyRequestException;
import com.microservice.auth.request.*;
import com.microservice.auth.response.AuthenticationResponse;
import com.microservice.auth.service.AuthenticationService;
import com.microservice.auth.service.LimiterService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("${application.security.jwt.auth-base-url}")
@Validated
@Slf4j
public class AuthenticationControllerForEmail {
	@Value("${application.security.jwt.access-token-header}")
	private String accessTokenHeader;

	@Value("${application.security.jwt.refresh-token-header}")
	private String refreshTokenHeader;

	private final AuthenticationService service;
	private final LimiterService bucket;

	@Autowired
	public AuthenticationControllerForEmail(AuthenticationService service, LimiterService limiterService) {
		log.info("AuthenticationControllerForPhone created");
		this.service = service;
		this.bucket = limiterService;
	}

	@PostMapping("/sign-up/by-email/create-account")
	@ResponseStatus(HttpStatus.CREATED)
	public ResponseEntity<AuthenticationResponse> register(
			@Valid @RequestBody RegisterRequestByEmail request
	) {
		AuthenticationResponse response = service.registerByEmail(request);
		HttpHeaders headers = setHttpHeaders(response);
		return ResponseEntity.ok().headers(headers).build();
	}

	@PostMapping("/sign-in/by-email")
	public ResponseEntity<AuthenticationResponse> authenticate(
			@Valid @RequestBody AuthenticationRequestByEmail request
	) {
		AuthenticationResponse response = service.authenticateByEmail(request);
		HttpHeaders headers = setHttpHeaders(response);
		return ResponseEntity.ok().headers(headers).build();
	}
	@PostMapping("/sign-up/by-email/request-code")
	@ResponseStatus(HttpStatus.OK)
	public void requestCode(@Valid @RequestBody EmailCodeRequest request) {
		if (bucket.tryConsume(request.getEmail())) {
			service.requestCodeEmail(request);
		} else {
			throw new TooManyRequestException(request.getLocale());
		}
	}

//	@PostMapping("/reset-password/by-phone/request-code")
//	@ResponseStatus(HttpStatus.OK)
//	public void resetPasswordRequest(@Valid @RequestBody SMSCodeRequest request) {
//		if (bucket.tryConsume(request.getPhone())) {
//			service.requestSMSByReset(request);
//		} else {
//			throw new TooManyRequestException(request.getLocale());
//		}
//	}

//	@PostMapping("/reset-password/by-phone/create-password")
//	@ResponseStatus(HttpStatus.OK)
//	public ResponseEntity<AuthenticationResponse> createPassword(@Valid @RequestBody CreatePasswordRequest request) {
//		if (bucket.tryConsume(request.getPhone())) {
//			AuthenticationResponse response = service.createPassword(request);
//			HttpHeaders headers = setHttpHeaders(response);
//			return ResponseEntity.ok().headers(headers).build();
//		} else {
//			throw new TooManyRequestException(request.getLocale());
//		}
//	}

	@PostMapping("/validate-email-code")
	@ResponseStatus(HttpStatus.OK)
	public void validateSMS(@Valid @RequestBody ValidateRequest request) {
		service.validateCode(request);
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
