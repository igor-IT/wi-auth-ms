package com.microservice.auth.exceptions;

import com.microservice.auth.user.Locale;
import lombok.Getter;

@Getter
public class PhoneNotFoundException extends BaseException {

	public PhoneNotFoundException(Locale locale) {
		super(locale);
	}
}
