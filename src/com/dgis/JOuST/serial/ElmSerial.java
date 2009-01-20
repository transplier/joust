package com.dgis.JOuST.serial;

import gnu.io.CommPortIdentifier;
import gnu.io.PortInUseException;
import gnu.io.SerialPort;
import gnu.io.UnsupportedCommOperationException;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collections;
import java.util.List;

import com.dgis.JOuST.OBDInterface;
import com.dgis.util.Logger;

public class ElmSerial implements ObdSerial {
	
	public static final int  ATZ_TIMEOUT=           1500;
	public static final int  AT_TIMEOUT=            130;

	private static final byte SPECIAL_DELIMITER = '\r';

	private static Logger logger = Logger.getInstance();

	private String serialDevice;
	private int baud;

	private InputStream input;
	private OutputStream output;
	private SerialPort port;
	
	private SerialState portState = SerialState.CLOSED;

	// ///PROTOCOL SPECIFIC VARIABLES/////
	private ELMInterfaceType device=ELMInterfaceType.UNKNOWN_INTERFACE;

	public ElmSerial(String serialDevice, int baud) throws PortNotFoundException, PortInUseException, UnsupportedCommOperationException, IOException {
		this.serialDevice = serialDevice;
		this.baud = baud;
		open();
	}

	@SuppressWarnings("unchecked")
	public void open() throws PortNotFoundException, PortInUseException,
			UnsupportedCommOperationException, IOException {
		if(portState == SerialState.OPEN){
			logger.logWarning("port was already open.");
			return;
		}
		logger.logInfo("Trying to open " + serialDevice + " @ " + baud
				+ " baud");
		while (portState != SerialState.CLOSED) {
			logger
					.logWarning("Port was already opened by us, reopening...");
			input.close();
		}
		// Get the set of all ports seen by RXTX
		List<CommPortIdentifier> portIdentifiers = Collections
				.list(CommPortIdentifier.getPortIdentifiers());
		StringBuffer found = new StringBuffer();
		for (CommPortIdentifier id : portIdentifiers)
			found.append(id.getName() + " ");
		logger.logInfo("Found the following ports:" + found.toString());

		// Sift through them to find the right one.
		CommPortIdentifier portId = null; // will be set if port found
		for (CommPortIdentifier pid : portIdentifiers) {
			// Is the name the one we wanted?
			if (pid.getName().equals(serialDevice)) {
				// Is it a serial device?
				if (pid.getPortType() == CommPortIdentifier.PORT_SERIAL) {

					portId = pid;
					break;

				} else {
					// TODO Make a config option to ignore wrong type
					logger
							.logWarning(serialDevice
									+ " does not seem to be a serial device (type: "
									+ pid.getPortType() + "), ignoring.");
				}

			}
		}

		if (portId == null) {
			logger.logWarning("Could not find usable port " + serialDevice);
			throw new PortNotFoundException(serialDevice);
		}

		// We now have a valid portId.

		// Try to lock port
		logger.logVerbose("Trying to get exclusive access to "
				+ serialDevice);
		try {
			port = (SerialPort) portId.open(OBDInterface.APPLICATION_NAME,
					10000 // Wait max. 10 sec. to acquire port
					);
			logger.logVerbose("Access granted.");

			try {
				// Locked OK. Try setting parameters and opening port.
				port.setSerialPortParams(baud, SerialPort.DATABITS_8,
						SerialPort.STOPBITS_1, SerialPort.PARITY_NONE);

				try {
					input = port.getInputStream();
					output = port.getOutputStream();
					portState=SerialState.OPEN;
					// WE ARE DONE.
					logger.logInfo(serialDevice + " opened.");
					return;
				} catch (IOException e) {
					logger.logWarning("Cannot open port: "
							+ e.getLocalizedMessage());
					throw e;
				}

			} catch (UnsupportedCommOperationException e) {
				logger.logWarning(serialDevice
						+ " does not seem to support 8N1 @ " + baud);
				throw e;
			}
		} catch (PortInUseException inUseException) {
			logger.logWarning(serialDevice + " seems to be in use by "
					+ inUseException.currentOwner);
			throw inUseException;
		}
	}

	@Override
	public void close() throws IOException {
		logger.logInfo("Closing " + port.getName());
		input.close();
		output.close();
		port.close();
		input=null;
		output=null;
		portState = SerialState.CLOSED;
	}

	@Override
	public SerialState getState() {
		// TODO Make this not dumb.
		return SerialState.OPEN;
	}

	private void send_command(String c) throws IOException {
		byte[] buf = new byte[c.length()];
		for (int x = 0; x < buf.length; x++)
			buf[x] = (byte) c.charAt(x);
		send_command(buf);
	}

	/**
	 * Will attempt to determine the type of response that was received from the device.
	 * @param cmd_sent
	 * @param msg_received
	 * @return the return value of the visitor call.
	 * @throws IOException
	 */
	public Object process_response(ElmResponseVisitor visit, byte[] cmd_sent, byte[] msg_received)
			throws IOException {
		int i = 0;
		int msgPos = 0; //Start of the message. May not be 0 if echo is on.
		if (cmd_sent != null) {
			//See if msg_received starts with cmd_sent.
			//If so, we know echo is on, and we disable it.
			//Also keep track of where the start of the reply
			//should be (msgPos).
			boolean echoOn = true;
			for (i = 0; i<cmd_sent.length && cmd_sent[i] != 0; i++) {
				if (cmd_sent[i] != msg_received[msgPos]) // if the characters are not the same
				{
					echoOn = false;
					break;
				}
				msgPos++;
			}

			if (echoOn)
				turnOffEcho();
			else
				msgPos = 0;
		}

		// Strip off nulls and special characters at start of reply.
		while (msg_received[msgPos] > 0 && (msg_received[msgPos] <= ' '))
			msgPos++;

		// Pull out reply string.
		StringBuffer msgBuf = new StringBuffer(msg_received.length-msgPos);
		//Accept characters until null hit.
		for(int j=msgPos; j<msg_received.length; j++){
			if(msg_received[j]==0) break;
			msgBuf.append((char)msg_received[j]);
		}
		String msg = msgBuf.toString();
		
		//Collapse whitespace & prompt
		msg = msg.replaceAll("\\s|>", "");
		
		//Get rid of useless bits...
		if (msg.startsWith("SEARCHING..."))
			msg=msg.substring(12);
		else if (msg.startsWith("BUSINIT:OK"))
			msg=msg.substring(12);
		else if (msg.startsWith("BUSINIT:...OK"))
			msg=msg.substring(15);
		
		//Check for <DATA ERROR>
		int indexOfLT = msg.indexOf('<');
		if(indexOfLT>=0){
			if(msg.startsWith("<DATAERROR", indexOfLT)) //Remember, spaces are gone, as is >
				return visit.dataError2();
			else
				return visit.rubbish();
		}
		
		//Check for hex number.
		boolean isHex = true;
		//Check every character for non-hexness
		for(char c : msg.toCharArray()){
			if(!(Character.isDigit(c) || (c>='a' && c<='f') || (c>='A' && c <= 'F'))){
				isHex=false;
				break;
			}
		}
		if(isHex) {return visit.hexData();}
		
		if (msg.contains("NODATA"))
			{return visit.noData();}
		if (msg.contains("UNABLETOCONNECT"))
			{return visit.unableToConnect();}
		if (msg.contains("BUSBUSY"))
			{return visit.busBusy();}
		if (msg.contains("DATAERROR"))
			{return visit.dataError();}
		if (msg.contains("BUSERROR") || msg.contains("FBERROR"))
			{return visit.busError();}
		if (msg.contains("CANERROR"))
			{return visit.CANError();}
		if (msg.contains("BUFFERFULL"))
			{return visit.bufferIsFull();}
		if (msg.contains("BUSINIT:ERROR") || msg.contains("BUSINIT:...ERROR"))
			{return visit.busInitError();}
		if (msg.contains("BUSINIT:") || msg.contains("BUSINIT:..."))
			{return visit.serialError();}
		if (msg.contains("?"))
			{return visit.unknownCommand();}
		if (msg.contains("ELM320"))
			{return visit.interfaceFound(ELMInterfaceType.INTERFACE_ELM320);}
		if (msg.contains("ELM322"))
			{return visit.interfaceFound(ELMInterfaceType.INTERFACE_ELM322);}
		if (msg.contains("ELM323"))
			{return visit.interfaceFound(ELMInterfaceType.INTERFACE_ELM323);}
		if (msg.contains("ELM327"))
			{return visit.interfaceFound(ELMInterfaceType.INTERFACE_ELM327);}

		logger.logWarning("Warning: Discarded apparent noise: |"+msg+"|");
		return visit.rubbish();
	}

	private void turnOffEcho() throws IOException {
		byte[] temp_buf = new byte[80];
		send_command("ate0"); // turn off the echo
		// wait for chip response or timeout
		// TODO test timeout
		boolean timedOut = false;
		while (true) {
			ELMReadResult res = read_comport(temp_buf, AT_TIMEOUT);
			if (res == ELMReadResult.PROMPT)
				break;
			if (res == ELMReadResult.TIMEOUT) {
				timedOut = true;
				break;
			}
		}
		if (!timedOut) {
			send_command("atl0"); // turn off linefeeds
			while (true) {
				ELMReadResult res = read_comport(temp_buf, AT_TIMEOUT);
				if (res == ELMReadResult.PROMPT)
					break;
				if (res == ELMReadResult.TIMEOUT) {
					timedOut = true;
					break;
				}
			}
		}		
	}

	public ELMReadResult read_comport(byte[] buf, int timeout) throws IOException {
		long startTime = System.currentTimeMillis();
		while(input.available()==0){
			try {
				Thread.sleep(10);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			if(System.currentTimeMillis()-startTime > timeout){
				//TODO log something
				return ELMReadResult.TIMEOUT;
			}
		}
		int len = input.read(buf);
		if (len == 0)
			return ELMReadResult.EMPTY;
		logger.logSuperfine("RX: " + new String(buf));
		for (int p = 0; p < len; p++) {
			if (buf[p] == '>') {
				return ELMReadResult.PROMPT;
			}
		}
		return ELMReadResult.DATA;
	}

	public void send_command(byte[] command) throws IOException {
		output.write(command);
		output.write(new byte[] { '\r' });
		output.flush();
	}

	// Lifted from Scantool
	String get_protocol_string(ELMInterfaceType interface_type, int protocol_id) {
		switch (interface_type) {
		case INTERFACE_ELM320:
			return "SAE J1850 PWM (41.6 kBit/s)";
		case INTERFACE_ELM322:
			return "SAE J1850 VPW (10.4 kBit/s)";
		case INTERFACE_ELM323:
			return "ISO 9141-2 / ISO 14230-4 (KWP2000)";
		case INTERFACE_ELM327:
			switch (protocol_id) {
			case 0:
				return "N/A";
			case 1:
				return "SAE J1850 PWM (41.6 kBit/s)";
			case 2:
				return "SAE J1850 VPW (10.4 kBit/s)";
			case 3:
				return "ISO 9141-2";
			case 4:
				return "ISO 14230-4 KWP2000 (5-baud init)";
			case 5:
				return "ISO 14230-4 KWP2000 (fast init)";
			case 6:
				return "ISO 15765-4 CAN (11-bit ID, 500 kBit/s)";
			case 7:
				return "ISO 15765-4 CAN (29-bit ID, 500 kBit/s)";
			case 8:
				return "ISO 15765-4 CAN (11-bit ID, 250 kBit/s)";
			case 9:
				return "ISO 15765-4 CAN (29-bit ID, 250 kBit/s)";
			}
		}

		return "unknown";
	}

	// TODO Find what 'stop' is
	boolean find_valid_response(byte[] buf, byte[] response, String filter,
			int[] stop) {
		int in_ptr = 0; // in response
		int out_ptr = 0; // in buf
		buf[0] = 0;

		String responseString = new String(response);

		while (response[in_ptr] != 0) {
			// TODO check this logic
			if (responseString.startsWith(filter)) {
				while (response[in_ptr] > 0
						&& response[in_ptr] != SPECIAL_DELIMITER) // copy
																	// valid
																	// response
																	// into buf
				{
					out_ptr = in_ptr;
					in_ptr++;
					out_ptr++;
				}
				out_ptr = 0; // terminate string
				if (response[in_ptr] == SPECIAL_DELIMITER)
					in_ptr++;
				break;
			} else {
				// skip to the next delimiter
				while (response[in_ptr] > 0
						&& response[in_ptr] != SPECIAL_DELIMITER)
					in_ptr++;
				if (response[in_ptr] == SPECIAL_DELIMITER) // skip the
															// delimiter
					in_ptr++;
			}
		}

		if (stop != null)
			stop[0] = in_ptr;

		if (buf[0] != 0)
			return true;
		else
			return false;
	}

	// TODO make this not ugly
	StringBuffer response = new StringBuffer(256);
	//Lifted from Scantool.
	public ResetResult resetAndHandshake() throws IOException, SerialPortStateException {
		logger.logInfo("Resetting hardware interface.");
		if(portState != SerialState.OPEN){
			logger.logWarning("resetAndHandshake called without an open port.");
			throw new SerialPortStateException();
		}
		// case RESET_START:
		// wait until we either get a prompt or the timer times out
		long time = System.currentTimeMillis();
		while (true) {
			if (input.available() > 0) {
				if (input.read() == '>')
					break;
			} else {
				if (System.currentTimeMillis() - time > ATZ_TIMEOUT)
					break;
			}
		}
		logger.logVerbose("Sending ATZ.");
		send_command("atz"); // reset the chip

		// case RESET_WAIT_RX:
		byte[] buf = new byte[128];
		try {
			Thread.sleep(ATZ_TIMEOUT);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		ELMReadResult status = read_comport(buf, ATZ_TIMEOUT); // read comport
		while (status == ELMReadResult.DATA){ // if new data detected in com port buffer
			response.append(new String(buf)); // append contents of buf to
												// response
			status = read_comport(buf, ATZ_TIMEOUT);
		}
		if (status == ELMReadResult.PROMPT) // if '>' detected
		{
			logger.logVerbose("Got prompt.");
			response.append(new String(buf));
			process_response(new AElmResponseVisitor(){
				@Override
				public Object interfaceFound(ELMInterfaceType type) {
					device=type;
					return null;
				}
				@Override
				Object defaultCase() {
					String s = "Unexpected response while trying to find device identifier!";
					logger.logError(s);
					return null;
				}
			}, "atz".getBytes(), response.toString()
					.getBytes());
			logger.logVerbose("Response: "+device);
			switch(device){
			case INTERFACE_ELM323: case INTERFACE_ELM327:
				logger.logInfo("Found an "+device.toString());
				logger.logInfo("Waiting for ECU timeout...");
				return RESET_ECU_TIMEOUT();
			case INTERFACE_ELM320: case INTERFACE_ELM322:
				logger.logInfo("Found a "+device.toString());
				return new ResetResult(device.toString(), true);
			default:
				logger.logWarning("Unexpected response while trying to identify device: "+response.toString());
				return new ResetResult("Unexpected response while trying to identify device: "+response.toString(), false);	
			}
		} else if (status == ELMReadResult.TIMEOUT) // if the timer timed out
		{
			logger.logWarning("Interface was not found - time out.");
			return new ResetResult(device.toString(), false);
		}
		else{
			logger.logWarning("Unexpected response: "+device.toString());
			return new ResetResult(device.toString(), false);
		}
	}

	ResetResult RESET_ECU_TIMEOUT() throws IOException {
		// if (serial_time_out) // if the timer timed out
		// {
		if (device == ELMInterfaceType.INTERFACE_ELM327) {
			logger.logVerbose("Sending 0100...");
			send_command("0100");
			response = new StringBuffer(256);
			logger.logInfo("Detecting OBD protocol...");
			return RESET_WAIT_0100();
		} else //TODO Is this right?
			return new ResetResult(device.toString(), true);
		// }
	}

	ResetResult RESET_WAIT_0100() throws IOException {
		byte[] buf = new byte[128];
		while(true){
			ELMReadResult readStatus = read_comport(buf, ECU_TIMEOUT);
			//logger.logVerbose("Response: "+readStatus.toString());
			if (readStatus == ELMReadResult.DATA){ // if new data detected in com port buffer
				String dta = new String(buf);
				response.append(dta);
				continue;
			}
													// response
			else if (readStatus == ELMReadResult.PROMPT) // if we got the prompt
			{
				response.append(new String(buf));
				//TODO: semi-hack
				ResetResult res = (ResetResult) process_response(new AElmResponseVisitor(){
					@Override
					Object defaultCase() {
						return new ResetResult(device.toString(), true);
					}
					@Override
					public Object hexData() {
						return new ResetResult("Communication error.", false);
					}
					@Override
					public Object noData() {
						return new ResetResult("Did not receive a response.", false);
					}
					@Override
					public Object unableToConnect() {
						return new ResetResult("Unable to connect to interface.", false);
					}
				}, "0100".getBytes(), response.toString()
						.getBytes());
				return res;
	
			} else if (readStatus == ELMReadResult.TIMEOUT) // if the timer timed out
			{
				logger.logWarning("Interface not found");
				return new ResetResult("Did not receive a response.", false);
			}
			return new ResetResult("Interface not found.", false);
		}
	}

	@Override
	public void requestPID(final PIDResultListener list, final int pid, final int numBytes) throws IOException, SerialPortStateException {
		if (getState() == SerialState.OPEN) {
			String cmd = String.format("01%02X", pid);
			send_command(cmd); // send command for that particular sensor
			final byte[] buf = new byte[256];
			final StringBuffer response = new StringBuffer(255);
			ELMReadResult response_status = ELMReadResult.DATA;
			long start_time = System.currentTimeMillis();
			while (true) {
				response_status = read_comport(buf, OBD_REQUEST_TIMEOUT); // read comport
				String r = bytesToString(buf);
				response.append(r);
				if (response_status == ELMReadResult.DATA) // if data detected in com port buffer
				{
					continue;
				} else if (response_status == ELMReadResult.PROMPT) // if '>' detected
				{
					process_response(new AElmResponseVisitor(){
							@Override
							Object defaultCase(){
								list.error("Did not get a hexadecimal value back from interface when requesting PID#"+String.format("%02X",pid));
								return null;
							}
							@Override
							public Object hexData() {
								String cmd = String.format("41%02X", pid);
								if (find_valid_response(buf, response.toString(), cmd,
										null)) {
									buf[4 + numBytes* 2] = 0;  // solves problem where response is padded with zeroes (i.e., '41 05 7C 00 00 00')
									//TODO calculate value here as per
									//sensor->formula((int)strtol(buf + 4, NULL, 16), buf); //plug the value into formula
									list.dataReceived(pid, numBytes, buf);
								} else {
									//TODO log something- got nothing back.
									list.error("Got no data back from interface when requesting PID#"+String.format("%02X",pid));
								}
								return null;
							}
						}, cmd.getBytes(), response.toString().getBytes());

				} else if(response_status == ELMReadResult.EMPTY){
					if(System.currentTimeMillis() - start_time > OBD_REQUEST_TIMEOUT){
						//TODO log timeout
						list.error("Got no data back from interface when requesting PID#"+String.format("%02X",pid));
						return;
					} else {
						//Not enough data, still not timed out
						continue;
					}
				} else {
					//TODO log something.
					list.error("Unknown error: "+ response_status.name());
				}
			}
		} else {
			logger.logWarning("requestPID called without an open port.");
			throw new SerialPortStateException();
		}
	}

	// Lifted from ScanTool
	// TODO Convert this to Java style
	public static boolean find_valid_response(byte[] buf, String response,
			String filter, int[] endOfResp) {
		int in_ptr = 0; // in response
		int out_ptr = 0; // in buf

		buf[0] = 0;

		response = response.replaceAll(" ", "");
		
		while (in_ptr < response.length()) {
			if (response.startsWith(filter, in_ptr)) {
				while (in_ptr < response.length()
						&& response.charAt(in_ptr) != SPECIAL_DELIMITER)
				{
					//char x = response.charAt(in_ptr);
					buf[out_ptr] = (byte)response.charAt(in_ptr);
					in_ptr++;
					out_ptr++;
				}
				buf[out_ptr] = 0; // terminate string
				if (response.charAt(in_ptr) == SPECIAL_DELIMITER)
					in_ptr++;
				break;
			} else {
				// skip to the next delimiter
				while (in_ptr < response.length()
						&& response.charAt(in_ptr) != SPECIAL_DELIMITER)
					in_ptr++;
				if (response.charAt(in_ptr) != SPECIAL_DELIMITER) // skip the
																	// delimiter
					in_ptr++;
			}
		}

		if (endOfResp != null)
			endOfResp[0] = in_ptr;

		String r = bytesToString(buf);

		if (r.length() > 0)
			return true;
		else
			return false;
	}
	
	/**
	 * Converts a null-terminated array of bytes to a string.
	 * 
	 * @param buf
	 * @return
	 */
	static String bytesToString(byte[] buf){
		if(buf[0]==0) return "";
		int off = 0;
		for (int i = 0; i < buf.length && buf[i] != 0; i++)
			off = i;
		return new String(buf, 0, off+1);
	}

	@Override
	public String getInterfaceIdentifier() {
		return device.toString();
	}
}


enum ELMInterfaceType{
	INTERFACE_ELM320{public String toString(){return "ELM 320";}},
	INTERFACE_ELM322{public String toString(){return "ELM 322";}},
	INTERFACE_ELM323{public String toString(){return "ELM 323";}},
	INTERFACE_ELM327{public String toString(){return "ELM 327";}},
	UNKNOWN_INTERFACE
}

interface ELMResponse{
	void visit(ElmResponseVisitor visitor);
}
interface ElmResponseVisitor{

	Object dataError2();

	Object noData();

	Object interfaceFound(ELMInterfaceType type);

	Object unknownCommand();

	Object serialError();

	Object busInitError();

	Object bufferIsFull();

	Object busError();

	Object CANError();

	Object dataError();

	Object busBusy();

	Object unableToConnect();

	Object hexData();

	Object rubbish();
	
}

abstract class AElmResponseVisitor implements ElmResponseVisitor{
	public Object dataError2(){return defaultCase();}
	public Object interfaceFound(ELMInterfaceType type){return defaultCase();}
	public Object unknownCommand(){return defaultCase();}
	public Object serialError(){return defaultCase();}
	public Object busInitError(){return defaultCase();}
	public Object bufferIsFull(){return defaultCase();}
	public Object busError(){return defaultCase();}
	public Object CANError(){return defaultCase();}
	public Object dataError(){return defaultCase();}
	public Object busBusy(){return defaultCase();}
	public Object unableToConnect(){return defaultCase();}
	public Object hexData(){return defaultCase();}
	public Object noData(){return defaultCase();}
	public Object rubbish(){return defaultCase();}
	abstract Object defaultCase();
}

enum ELMResponseCode{
	//process_response return values
	HEX_DATA,
	BUS_BUSY,
	BUS_ERROR,
	BUS_INIT_ERROR,
	UNABLE_TO_CONNECT,
	CAN_ERROR,
	DATA_ERROR,
	DATA_ERROR2,
	ERR_NO_DATA,
	BUFFER_FULL,
	SERIAL_ERROR,
	UNKNOWN_CMD,
	RUBBISH,
	INTERFACE_ID,
	PROTOCOL_INIT_ERROR;
	
	public String toString(){ return getMessage(); }
	
	String getMessage(){
		return getMessage(this);
	}
	// Adapted from ScanTool
	public static String getMessage(ELMResponseCode error) {
		switch (error) {
		case BUS_ERROR:
			return "Bus Error: OBDII bus is shorted to Vbatt or Ground.";

		case BUS_BUSY:
			return "OBD Bus Busy. Try again.";

		case BUS_INIT_ERROR:
			return "OBD Bus Init Error. Check connection to the vehicle, make sure the vehicle is OBD-II compliant, and ignition is ON.";

		case UNABLE_TO_CONNECT:
			return "Unable to connect to OBD bus. Check connection to the vehicle. Make sure the vehicle is OBD-II compliant, and ignition is ON.";

		case CAN_ERROR:
			return "CAN Error. Check connection to the vehicle. Make sure the vehicle is OBD-II compliant, and ignition is ON.";

		case DATA_ERROR:
		case DATA_ERROR2:
			return "Data Error: there has been a loss of data. You may have a bad connection to the vehicle, check the cable and try again.";

		case BUFFER_FULL:
			return "Hardware data buffer overflow.";

		case SERIAL_ERROR:
		case UNKNOWN_CMD:
		case RUBBISH:
			return "Serial Link Error: please check connection between computer and scan tool.";
		default:
			return error.name();
		}
	}
}

enum ELMReadResult{
	EMPTY,
	DATA,
	PROMPT,
	TIMEOUT,
}