package com.microservice.auth.exceptions;

import com.microservice.auth.data.Locale;

public class UserNotExistException extends BaseException {
	public UserNotExistException(Locale locale) {
		super(locale);
	}
}
