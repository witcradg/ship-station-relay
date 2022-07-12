package io.witcradg.ordertrackingapi.service;


public class InvalidPhoneNumberException extends Exception {

	String message;

	public InvalidPhoneNumberException(String string) {
		message = string;
	}

	public String toString() {
		return ("InvalidPhoneNumberException Occurred for : " + message);
	}

	/**
	 * 	
	 */
	private static final long serialVersionUID = 1L;
}
