package com.microservice.auth.exceptions;

import com.microservice.auth.data.Locale;

public class InvalidPhoneFormatException extends BaseException {

	public InvalidPhoneFormatException(Locale locale) {
		super(locale);
	}
}
