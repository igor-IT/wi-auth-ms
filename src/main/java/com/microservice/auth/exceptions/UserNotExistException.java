package com.microservice.auth.exceptions;

	public class UserNotExistException extends RuntimeException {
		public UserNotExistException() {
			super("User is not exist!");
		}
	}
