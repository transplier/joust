package com.dgis.JOuST.serial;

import java.io.IOException;

public interface ObdSerial {
	
	// timeouts, in milliseconds
	public static final int  OBD_REQUEST_TIMEOUT=   9900;
	public static final int  ECU_TIMEOUT=           5000;
	
	/**
	 * Attempts to stop all communication used by this instance.
	 * Subsequent calls to connect() or requestPID() should throw exceptions.
	 * This should release all communications resources used by the instance
	 * (as in, all streams should be .close()'d).
	 * Implementation specific whether the instance can be re-opened.
	 * @throws IOException
	 */
	public void stop() throws IOException;
	
	/**
	 * Attempts to establish communications to the controller.
	 * Will automatically re-open the last port used if
	 * close() was called.
	 * @return
	 * @throws IOException
	 */
	public ResetResult resetAndHandshake() throws IOException;
	
	/**
	 * Requests an arbitrary PID from the ECU. May return before request
	 * is completed.
	 * @param list the listener to call when the data is received.
	 * @param pid the PID to query.
	 * @param numBytes the number of bytes to expect in return.
	 * @throws IOException
	 * @throws SerialPortStateException
	 */
	public void requestPID(PIDResultListener list, int pid, int numBytes) throws IOException;
	
	/**
	 * @return the state of the connection (ignores protocol state,
	 * reports if close() has been called).
	 */
	public boolean isOpen();

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
