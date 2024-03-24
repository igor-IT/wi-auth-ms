package com.alibou.security.exceptions;

	public class UserNotExistException extends RuntimeException {
		public UserNotExistException() {
			super("User is not exist!");
		}
	}
