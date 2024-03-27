package com.microservice.auth.exceptions;

public class TooManyRequestException extends RuntimeException{
		public TooManyRequestException() {
			super("Too many request!");
		}
}
