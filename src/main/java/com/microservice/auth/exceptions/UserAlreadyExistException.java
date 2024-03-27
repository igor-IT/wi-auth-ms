package com.microservice.auth.exceptions;

public class UserAlreadyExistException extends RuntimeException {
	public UserAlreadyExistException() {
		super("User already exist!");
	}
}
