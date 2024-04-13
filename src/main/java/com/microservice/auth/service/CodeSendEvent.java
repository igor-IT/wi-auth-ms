package com.microservice.auth.service;

import com.microservice.auth.data.Locale;

public record CodeSendEvent(String code, Locale locale) {
}
