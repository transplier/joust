package com.dgis.JOuST.serial;

public class PortNotFoundException extends Exception {

	private static final long serialVersionUID = 1L;

	private String device;
	
	public PortNotFoundException(String serialDevice) {
		device=serialDevice;
	}

	public String getDevice() {
		return device;
	}
	

}
