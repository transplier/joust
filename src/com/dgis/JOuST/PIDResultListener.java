package com.dgis.JOuST;

public interface PIDResultListener{
	void dataReceived(int pid, int numBytes, byte[] data);
	void error(String msg, int pid); 
}

