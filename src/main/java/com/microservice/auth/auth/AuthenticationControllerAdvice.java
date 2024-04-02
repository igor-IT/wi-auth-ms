package com.microservice.auth.auth;

import com.microservice.auth.exceptions.*;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.support.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.Locale;

@RestControllerAdvice
@RequiredArgsConstructor
public class AuthenticationControllerAdvice extends ResponseEntityExceptionHandler {
	private final MessageSource messageSource;
	@ExceptionHandler(PhoneNumberTakenException.class)
	@ResponseStatus(HttpStatus.CONFLICT)
	public ResponseEntity<Object> handlePhoneNumberTakenException(PhoneNumberTakenException e) {
		String errorMessage = resolveLocal(e.getLocale(), "error.phoneNumberTaken");
		return ResponseEntity.status(HttpStatus.CONFLICT).body(errorMessage);
	}

	@ExceptionHandler(InvalidCodeException.class)
	@ResponseStatus(HttpStatus.UNAUTHORIZED)
	public ResponseEntity<Object> handleInvalidCodeException(InvalidCodeException e) {
		String errorMessage = resolveLocal(e.getLocale(), "error.invalidCode");
		return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorMessage);
	}

	@ExceptionHandler(PhoneNotFoundException.class)
	@ResponseStatus(HttpStatus.NOT_FOUND)
	public ResponseEntity<Object> handlePhoneNotFoundException(PhoneNotFoundException e) {
		String errorMessage = resolveLocal(e.getLocale(), "error.phoneNotFound");
		return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorMessage);
	}

	@ExceptionHandler(InvalidPasswordException.class)
	@ResponseStatus(HttpStatus.NOT_FOUND)
	public ResponseEntity<Object> handleInvalidPasswordException(InvalidPasswordException e) {
		return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<String> handleValidationExceptions(MethodArgumentNotValidException ex) {
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex.getMessage());
	}

	@ExceptionHandler(TooManyRequestException.class)
	public ResponseEntity<String> handleTooManyRequestException(TooManyRequestException ex) {
		String errorMessage = resolveLocal(ex.getLocale(), "error.userAlreadyExist");
		return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(errorMessage);
	}
	@ExceptionHandler(UserAlreadyExistException.class)
	public ResponseEntity<String> handleUserAlreadyExistException(UserAlreadyExistException ex) {
		String errorMessage = resolveLocal(ex.getLocale(), "error.tooManyRequests");
		return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(errorMessage);
	}
	@ExceptionHandler(UserNotExistException.class)
	public ResponseEntity<String> handleUserNotExistException(UserNotExistException ex) {
		String errorMessage = resolveLocal(ex.getLocale(), "error.userNotExist");
		return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(errorMessage);
	}

	@NotNull
	private String resolveLocal(com.microservice.auth.user.Locale locale, String message) {
		return messageSource.getMessage(message, null,
				locale.equals(com.microservice.auth.user.Locale.EN) ? Locale.ENGLISH : new Locale("ru"));
	}
}