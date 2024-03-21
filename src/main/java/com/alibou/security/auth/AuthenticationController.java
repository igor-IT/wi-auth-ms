package com.alibou.security.auth;

import com.alibou.security.exceptions.TooManyRequestException;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.time.Duration;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthenticationController {

	private final AuthenticationService service;
	private final Bucket bucket;

	@Autowired
	public AuthenticationController(AuthenticationService service) {
		this.service = service;
		Bandwidth limit = Bandwidth.classic(20, Refill.greedy(20, Duration.ofMinutes(1)));
		this.bucket = Bucket.builder()
				.addLimit(limit)
				.build();
	}

	@PostMapping("/sign-up/by-phone/create-account")
	@ResponseStatus(HttpStatus.CREATED)
	public ResponseEntity<AuthenticationResponse> register(
			@RequestBody RegisterRequest request
	) {
		AuthenticationResponse response = service.register(request);
		HttpHeaders headers = new HttpHeaders();
		headers.add("X-AUTH-ACCESS-TOKEN", "Bearer " + response.getAccessToken());
		headers.add("X-AUTH-REFRESH-TOKEN", response.getRefreshToken());
		return ResponseEntity.ok().headers(headers).build();
	}

	@PostMapping("/authenticate")
	public ResponseEntity<AuthenticationResponse> authenticate(
			@RequestBody AuthenticationRequest request
	) {
		return ResponseEntity.ok(service.authenticate(request));
	}

	@PostMapping("/refresh-token")
	public void refreshToken(
			HttpServletRequest request,
			HttpServletResponse response
	) throws IOException {
		service.refreshToken(request, response);
	}


	@PostMapping("/sign-up/by-phone/request-sms-code")
	@ResponseStatus(HttpStatus.OK)
	public void requestSMS(@RequestBody SMSCodeRequest request) {
		if (bucket.tryConsume(1)) {
			service.requestSMS(request);

		} else {
			throw new TooManyRequestException();
		}
	}

	@PostMapping("/validate-sms-code")
	@ResponseStatus(HttpStatus.OK)
	public void validateSMS(@RequestBody ValidateRequest request) {
		service.validateSMS(request);
	}


}
