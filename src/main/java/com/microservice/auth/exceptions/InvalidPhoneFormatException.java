package com.microservice.auth.exceptions;

public class InvalidPhoneFormatException extends RuntimeException {

    public InvalidPhoneFormatException() {
        super("INVALID_PHONE_FORMAT");
    }
}
