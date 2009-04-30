package com.dgis.JOuST;

public class PIDNotFoundException extends Exception {
	private static final long serialVersionUID = 1L;

	public int pid;
	
	public PIDNotFoundException(int pid){
		super("Could not find PID "+pid+" in configuration!");
		this.pid=pid;
	}
}
