package com.microservice.auth.exceptions;

public class PhoneNumberTakenException extends RuntimeException {

    public PhoneNumberTakenException() {
        super("PHONE_NUMBER_TAKEN");
    }
}
