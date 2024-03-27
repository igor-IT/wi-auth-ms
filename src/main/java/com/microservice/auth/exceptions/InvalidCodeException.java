package com.microservice.auth.exceptions;

public class InvalidCodeException extends RuntimeException{
		public InvalidCodeException() {
			super("Code is not right");
		}
}
