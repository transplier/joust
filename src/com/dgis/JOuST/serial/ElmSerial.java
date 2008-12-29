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

	private static final byte SPECIAL_DELIMITER = '\t';

	private static Logger logger = Logger.getInstance();

	private String serialDevice;
	private int baud;

	private InputStream input;
	private OutputStream output;
	private SerialPort port;

	// ///PROTOCOL SPECIFIC VARIABLES/////
	private ELMResponse errorCode = ELMResponse.SERIAL_ERROR;
	private ELMResponse device;

	public ElmSerial(String serialDevice, int baud) {
		this.serialDevice = serialDevice;
		this.baud = baud;
	}

	@SuppressWarnings("unchecked")
	@Override
	public void open_comport() throws PortNotFoundException,
			PortInUseException, UnsupportedCommOperationException, IOException {
		logger.logInfo("Trying to open " + serialDevice + " @ " + baud + " baud");

		// Get the set of all ports seen by RXTX
		List<CommPortIdentifier> portIdentifiers = Collections
				.list(CommPortIdentifier.getPortIdentifiers());
		StringBuffer found = new StringBuffer();
		for(CommPortIdentifier id : portIdentifiers)
			found.append(id.getName()+" ");
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
					logger.logWarning(serialDevice
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
		logger.logVerbose("Trying to get exclusive access to " + serialDevice);
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
	public void close_comport() throws IOException {
		logger.logInfo("Closing " + port.getName());
		input.close();
		output.close();
		port.close();
	}

	@Override
	public String getErrorMessage() {
		return errorCode.getMessage();
	}

	@Override
	public ElmSerialState getState() {
		// TODO Make this not dumb.
		return ElmSerialState.READY;
	}

	private void send_command(String c) throws IOException {
		byte[] buf = new byte[c.length()];
		for (int x = 0; x < buf.length; x++)
			buf[x] = (byte) c.charAt(x);
		send_command(buf);
	}

	@Override
	public ELMResponse process_response(byte[] cmd_sent, byte[] msg_received)
			throws IOException {
		int i = 0;
		int msgPos = 0;
		boolean echo_on = true; // echo status
		boolean is_hex_num = true;
		byte[] temp_buf = new byte[80];

		if (cmd_sent != null) {
			for (i = 0; i<cmd_sent.length && cmd_sent[i] != 0; i++) {
				if (cmd_sent[i] != msg_received[msgPos]) // if the characters
				// are not the same,
				{
					echo_on = false; // say that echo is off
					break; // break out of the loop
				}
				msgPos++;
			}

			if (echo_on == true) // if echo is on
			{
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
			} else
				// if echo is off
				msgPos = 0;
		}

		// Find start of string
		while (msg_received[msgPos] > 0 && (msg_received[msgPos] <= ' '))
			msgPos++;

		// Pull out string
		String msg = new String(msg_received, msgPos, msg_received.length
				- msgPos);
		if (msg.contains("SEARCHING..."))
			msgPos += 13;
		else if (msg.contains("BUS INIT: OK"))
			msgPos += 13;
		else if (msg.contains("BUS INIT: ...OK"))
			msgPos += 16;

		// Loop until null encountered
		for (i = 0; msg_received[msgPos] > 0 && msgPos < msg_received.length; msgPos++) // loop
																						// to
																						// copy
																						// data
		{
			if (msg_received[msgPos] > ' ') // if the character is not a special
			// character or space
			{
				if (msg_received[msgPos] == '<') // Detect <DATA_ERROR
				{
					String msg2 = new String(msg_received, msgPos,
							msg_received.length - msgPos);
					if (msg2.startsWith("<DATA ERROR"))
						return ELMResponse.DATA_ERROR2;
					else
						return ELMResponse.RUBBISH;
				}
				msg_received[i] = msg_received[msgPos]; // rewrite response
				boolean isHex = Character.isDigit((char) msg_received[msgPos]);
				isHex |= (msg_received[msgPos] >= 'A' && msg_received[msgPos] <= 'F')
						|| (msg_received[msgPos] >= 'a' && msg_received[msgPos] <= 'f');
				if (!isHex && msg_received[msgPos] != ':')
					is_hex_num = false;
				i++;
			} else if (((msg_received[msgPos] == '\n') || (msg_received[msgPos] == '\r'))
					&& (msg_received[i - 1] != SPECIAL_DELIMITER)) // if the
				// character
				// is a CR
				// or LF
				msg_received[i++] = SPECIAL_DELIMITER; // replace CR with
			// SPECIAL_DELIMITER
		}

		if (i > 0)
			if (msg_received[i - 1] == SPECIAL_DELIMITER)
				i--;
		msg_received[i] = '\0'; // terminate the string

		if (is_hex_num)
			return ELMResponse.HEX_DATA;

		int nulPos = 0;
		for (int p = 0; p < msg_received.length; p++)
			if (msg_received[p] == 0) {
				nulPos = p;
				break;
			}
		String msg2 = new String(msg_received, 0, nulPos);

		if (msg2.equals("NODATA"))
			return ELMResponse.ERR_NO_DATA;
		if (msg2.contains("UNABLETOCONNECT"))
			return ELMResponse.UNABLE_TO_CONNECT;
		if (msg2.contains("BUSBUSY"))
			return ELMResponse.BUS_BUSY;
		if (msg2.contains("DATAERROR"))
			return ELMResponse.DATA_ERROR;
		if (msg2.contains("BUSERROR") || msg2.contains("FBERROR"))
			return ELMResponse.BUS_ERROR;
		if (msg2.contains("CANERROR"))
			return ELMResponse.CAN_ERROR;
		if (msg2.contains("BUFFERFULL"))
			return ELMResponse.BUFFER_FULL;
		if (msg2.contains("BUSINIT:ERROR") || msg2.contains("BUSINIT:...ERROR"))
			return ELMResponse.BUS_INIT_ERROR;
		if (msg2.contains("BUS INIT:") || msg2.contains("BUS INIT:..."))
			return ELMResponse.SERIAL_ERROR;
		if (msg2.contains("?"))
			return ELMResponse.UNKNOWN_CMD;
		if (msg2.contains("ELM320"))
			return ELMResponse.INTERFACE_ELM320;
		if (msg2.contains("ELM322"))
			return ELMResponse.INTERFACE_ELM322;
		if (msg2.contains("ELM323"))
			return ELMResponse.INTERFACE_ELM323;
		if (msg2.contains("ELM327"))
			return ELMResponse.INTERFACE_ELM327;

		return ELMResponse.RUBBISH;
	}

	@Override
	public ELMReadResult read_comport(byte[] buf, int timeout) throws IOException {
		if (input.available() == 0) {
			try {
				Thread.sleep(timeout);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			if (input.available() == 0)
				return ELMReadResult.TIMEOUT;
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

	@Override
	public void send_command(byte[] command) throws IOException {
		output.write(command);
		output.write(new byte[] { '\r' });
		output.flush();
	}

	// Lifted from Scantool
	String get_protocol_string(ELMResponse interface_type, int protocol_id) {
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
	public ResetResult reset_proc() throws IOException {
		logger.logInfo("Resetting hardware interface.");
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
		if (status == ELMReadResult.DATA){ // if new data detected in com port buffer
			response.append(new String(buf)); // append contents of buf to
												// response
			logger.logWarning("Got rubbish!");
			//TODO No idea what to return here.
			return new ResetResult(ELMResponse.RUBBISH, false);
		}
		else if (status == ELMReadResult.PROMPT) // if '>' detected
		{
			logger.logVerbose("Got prompt.");
			response.append(new String(buf));
			device = process_response("atz".getBytes(), response.toString()
					.getBytes());
			logger.logVerbose("Response: "+device);
			switch(device){
			case INTERFACE_ELM323: case INTERFACE_ELM327:
				logger.logInfo("Found an "+device.toString());
				logger.logInfo("Waiting for ECU timeout...");
				return RESET_ECU_TIMEOUT();
			case INTERFACE_ELM320: case INTERFACE_ELM322:
				logger.logInfo("Found a "+device.toString());
				return new ResetResult(device, true);
			default:
				logger.logWarning("Unexpected response: "+device.toString());
				return new ResetResult(device, false);	
			}
		} else if (status == ELMReadResult.TIMEOUT) // if the timer timed out
		{
			logger.logWarning("Interface was not found - time out.");
			return new ResetResult(device, false);
		}
		else{
			logger.logWarning("Unexpected response: "+device.toString());
			return new ResetResult(device, false);
		}
	}

	ResetResult RESET_ECU_TIMEOUT() throws IOException {
		// if (serial_time_out) // if the timer timed out
		// {
		if (device == ELMResponse.INTERFACE_ELM327) {
			logger.logVerbose("Sending 0100...");
			send_command("0100");
			response = new StringBuffer(256);
			logger.logInfo("Detecting OBD protocol...");
			return RESET_WAIT_0100();
		} else //TODO Is this right?
			return new ResetResult(device, true);
		// }
	}

	ResetResult RESET_WAIT_0100() throws IOException {
		byte[] buf = new byte[128];
		ELMReadResult readStatus = read_comport(buf, ECU_TIMEOUT);
		//logger.logVerbose("Response: "+readStatus.toString());
		if (readStatus == ELMReadResult.DATA){ // if new data detected in com port buffer
			String dta = new String(buf);
			response.append(dta); // append contents of buf to
			//TODO I have no idea what to return here.
			logger.logVerbose("Unexpected data: "+dta);
			return new ResetResult(ELMResponse.RUBBISH, false);
		}
												// response
		else if (readStatus == ELMReadResult.PROMPT) // if we got the prompt
		{
			response.append(new String(buf));
			ELMResponse status = process_response("0100".getBytes(), response.toString()
					.getBytes());
			logger.logVerbose("Response: "+status.toString());
			if (status == ELMResponse.ERR_NO_DATA || status == ELMResponse.UNABLE_TO_CONNECT){
				return new ResetResult(status, false);
			}
			else if (status != ELMResponse.HEX_DATA){
				logger.logWarning("Communication error");
				return new ResetResult(status, false);
			}
			return new ResetResult(status, true);

		} else if (readStatus == ELMReadResult.TIMEOUT) // if the timer timed out
		{
			logger.logWarning("Interface not found");
			return new ResetResult(ELMResponse.ERR_NO_DATA, false);
		}
		return new ResetResult(ELMResponse.RUBBISH, false);
	}

	public ELMResponse getDevice() {
		return device;
	}

}
