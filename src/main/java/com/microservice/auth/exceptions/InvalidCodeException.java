package com.microservice.auth.exceptions;

import com.microservice.auth.data.Locale;

public class InvalidCodeException extends BaseException {
	public InvalidCodeException(Locale locale) {
		super(locale);
	}
}
