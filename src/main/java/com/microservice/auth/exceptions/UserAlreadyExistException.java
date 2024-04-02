package com.microservice.auth.exceptions;

import com.microservice.auth.user.Locale;

public class UserAlreadyExistException extends BaseException {

	public UserAlreadyExistException(Locale locale) {
		super(locale);
	}
}
