package com.alibou.security.exceptions;

public class UserAlreadyExistException extends RuntimeException {
	public UserAlreadyExistException() {
		super("User already exist!");
	}
}
