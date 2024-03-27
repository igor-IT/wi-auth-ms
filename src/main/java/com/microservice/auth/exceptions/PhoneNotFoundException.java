package com.microservice.auth.exceptions;

public class PhoneNotFoundException extends RuntimeException{
		public PhoneNotFoundException() {
			super("Phone not found");
		}
}
