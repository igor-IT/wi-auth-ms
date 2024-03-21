package com.alibou.security.exceptions;

public class TooManyRequestException extends RuntimeException{
		public TooManyRequestException() {
			super("Too many request!");
		}
}
