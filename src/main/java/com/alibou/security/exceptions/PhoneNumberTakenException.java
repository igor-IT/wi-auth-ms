package com.alibou.security.exceptions;

public class PhoneNumberTakenException extends RuntimeException {

    public PhoneNumberTakenException() {
        super("PHONE_NUMBER_TAKEN");
    }
}
