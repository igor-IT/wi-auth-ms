package com.microservice.auth.code;

import com.microservice.auth.user.Locale;

public record CodeSendEvent(String code, Locale locale) {
}
