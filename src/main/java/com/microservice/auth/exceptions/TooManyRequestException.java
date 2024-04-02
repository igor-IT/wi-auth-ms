package com.microservice.auth.exceptions;

import com.microservice.auth.user.Locale;

public class TooManyRequestException extends BaseException {
	public TooManyRequestException(Locale locale) {
		super(locale);
	}
}
