package com.microservice.auth.exceptions;

import com.microservice.auth.data.Locale;
import lombok.Getter;

@Getter
public class PhoneOrEmailNotFoundException extends BaseException {

	public PhoneOrEmailNotFoundException(Locale locale) {
		super(locale);
	}
}
