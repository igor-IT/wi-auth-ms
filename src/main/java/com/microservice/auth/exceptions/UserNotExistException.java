package com.microservice.auth.exceptions;

import com.microservice.auth.user.Locale;

public class UserNotExistException extends BaseException {
	public UserNotExistException(Locale locale) {
		super(locale);
	}
}
