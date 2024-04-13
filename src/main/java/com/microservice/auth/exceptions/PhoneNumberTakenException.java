package com.microservice.auth.exceptions;

import com.microservice.auth.data.Locale;

public class PhoneNumberTakenException extends BaseException {

	public PhoneNumberTakenException(Locale locale) {
		super(locale);
	}
}
