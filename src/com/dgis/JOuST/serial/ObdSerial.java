package com.dgis.JOuST.serial;

import java.io.IOException;

public interface ObdSerial {
	
	// timeouts, in milliseconds
	public static final int  OBD_REQUEST_TIMEOUT=   9900;
	public static final int  ECU_TIMEOUT=           5000;
	
	/**
	 * Attempts to release all communication ports used by this instance.
	 * Subsequent calls to connect() or requestPID() should throw exceptions.
	 * @throws IOException
	 */
	public void close() throws IOException, SerialPortStateException;
	
	/**
	 * Attempts to establish communications to the controller.
	 * Will automatically re-open the last port used if
	 * close() was called.
	 * @return
	 * @throws IOException
	 */
	public ResetResult resetAndHandshake() throws IOException, SerialPortStateException;
	
	/**
	 * Requests an arbitrary PID from the ECU. May return before request
	 * is completed.
	 * @param list the listener to call when the data is received.
	 * @param pid the PID to query.
	 * @param numBytes the number of bytes to expect in return.
	 * @throws IOException
	 * @throws SerialPortStateException
	 */
	public void requestPID(PIDResultListener list, int pid, int numBytes) throws IOException, SerialPortStateException;
	
	/**
	 * @return the state of the serial connection (ignores protocol state, OS reported state only).
	 */
	public SerialState getState();

	/**
	 * Gets an arbitrary string identifying the device to the user.
	 * @return
	 */
	public String getInterfaceIdentifier();
}

interface PIDResultListener{
	void dataReceived(int pid, int numBytes, byte[] data);
	void error(String msg); 
}

class SerialPortStateException extends Exception{
	private static final long serialVersionUID = 1L;
}

enum SerialState {
	OPEN, ERROR, CLOSED
}



class ResetResult{
	/**
	 * Was the interface found?
	 */
	public boolean foundDevice;
	/**
	 * Arbitrary response string detailing failure or success.
	 */
	public String response;

	public ResetResult(String response, boolean found) {
		this.response=response;
		foundDevice=found;
	}
}
