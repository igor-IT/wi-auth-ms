package com.alibou.security.exceptions;

public class PhoneNotFoundException extends RuntimeException{
		public PhoneNotFoundException() {
			super("Phone not found");
		}
}
