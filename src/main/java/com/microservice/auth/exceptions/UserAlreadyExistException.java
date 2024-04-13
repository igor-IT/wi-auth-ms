package com.microservice.auth.exceptions;

import com.microservice.auth.data.Locale;

public class UserAlreadyExistException extends BaseException {

	public UserAlreadyExistException(Locale locale) {
		super(locale);
	}
}
