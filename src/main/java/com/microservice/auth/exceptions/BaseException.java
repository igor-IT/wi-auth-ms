package com.microservice.auth.exceptions;

import com.microservice.auth.data.Locale;
import lombok.Getter;

@Getter
abstract public class BaseException extends RuntimeException {
	private final Locale locale;

	public BaseException(Locale locale) {
		super();
		this.locale = locale;
	}
}
