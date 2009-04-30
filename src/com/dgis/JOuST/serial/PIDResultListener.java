package com.dgis.JOuST.serial;

public interface PIDResultListener{
	void dataReceived(int pid, int numBytes, byte[] data);
	void error(String msg); 
}

