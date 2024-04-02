package com.microservice.auth.exceptions;

import com.microservice.auth.user.Locale;

public class InvalidPhoneFormatException extends BaseException {

	public InvalidPhoneFormatException(Locale locale) {
		super(locale);
	}
}
