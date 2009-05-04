package com.dgis.JOuST.serial;

import java.io.IOException;

import com.dgis.JOuST.PIDNotFoundException;
import com.dgis.JOuST.PIDResultListener;

/*
 * Copyright (C) 2009 Giacomo Ferrari
 * This file is part of JOuST.
 *  JOuST is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  JOuST is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with JOuST.  If not, see <http://www.gnu.org/licenses/>.
 */

/**
 * A generic interface to an OBD controller.
 *
 * Copyright (C) 2009 Giacomo Ferrari
 * @author Giacomo Ferrari
 */

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
	 */
	public void requestPID(PIDResultListener list, int pid, int numBytes) throws IOException;
	
	/**
	 * Requests an arbitrary PID from the ECU. May return before request
	 * is completed. Must get size information from config file.
	 * @param list the listener to call when the data is received.
	 * @param pid the PID to query.
	 * @throws IOException
	 * @throws SerialPortStateException
	 * @throws PIDNotFoundException
	 */
	public void requestPID(PIDResultListener list, int pid) throws IOException, PIDNotFoundException;
	
	/**
	 * Requests an arbitrary PID from the ECU by name. May return before request
	 * is completed. Must get information from config file.
	 * @param list the listener to call when the data is received.
	 * @param pid the PID to query.
	 * @throws IOException
	 * @throws SerialPortStateException
	 * @throws PIDNotFoundException
	 */
	public void requestPID(PIDResultListener list, String name) throws IOException, PIDNotFoundException;
	
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