package com.alibou.security.exceptions;

public class InvalidCodeException extends RuntimeException{
		public InvalidCodeException() {
			super("Code is not right");
		}
}
