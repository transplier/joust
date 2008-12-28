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

	private static final int EMPTY = 0;
	private static final int DATA = 1;
	private static final int PROMPT = 2;
	private static final int TIMEOUT = 3;

	private static final byte SPECIAL_DELIMITER = '\t';

	private static Logger logger = Logger.getInstance();

	private String serialDevice;
	private int baud;

	private InputStream input;
	private OutputStream output;
	private SerialPort port;

	// ///PROTOCOL SPECIFIC VARIABLES/////
	private int errorCode = SERIAL_ERROR;
	private int device;

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
			logger.logWarning("Could not find port " + serialDevice);
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
		return getErrorMessage(errorCode);
	}

	// Adapted from ScanTool
	private String getErrorMessage(int error) {
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
			return String.format("Unknown error occured: %i", error);
		}
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
	public int process_response(byte[] cmd_sent, byte[] msg_received)
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
					int res = read_comport(temp_buf, AT_TIMEOUT);
					if (res == PROMPT)
						break;
					if (res == TIMEOUT) {
						timedOut = true;
						break;
					}
				}
				if (!timedOut) {
					send_command("atl0"); // turn off linefeeds
					while (true) {
						int res = read_comport(temp_buf, AT_TIMEOUT);
						if (res == PROMPT)
							break;
						if (res == TIMEOUT) {
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
						return DATA_ERROR2;
					else
						return RUBBISH;
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
			return HEX_DATA;

		int nulPos = 0;
		for (int p = 0; p < msg_received.length; p++)
			if (msg_received[p] == 0) {
				nulPos = p;
				break;
			}
		String msg2 = new String(msg_received, 0, nulPos);

		if (msg2.equals("NODATA"))
			return ERR_NO_DATA;
		if (msg2.contains("UNABLETOCONNECT"))
			return UNABLE_TO_CONNECT;
		if (msg2.contains("BUSBUSY"))
			return BUS_BUSY;
		if (msg2.contains("DATAERROR"))
			return DATA_ERROR;
		if (msg2.contains("BUSERROR") || msg2.contains("FBERROR"))
			return BUS_ERROR;
		if (msg2.contains("CANERROR"))
			return CAN_ERROR;
		if (msg2.contains("BUFFERFULL"))
			return BUFFER_FULL;
		if (msg2.contains("BUSINIT:ERROR") || msg2.contains("BUSINIT:...ERROR"))
			return BUS_INIT_ERROR;
		if (msg2.contains("BUS INIT:") || msg2.contains("BUS INIT:..."))
			return SERIAL_ERROR;
		if (msg2.contains("?"))
			return UNKNOWN_CMD;
		if (msg2.contains("ELM320"))
			return INTERFACE_ELM320;
		if (msg2.contains("ELM322"))
			return INTERFACE_ELM322;
		if (msg2.contains("ELM323"))
			return INTERFACE_ELM323;
		if (msg2.contains("ELM327"))
			return INTERFACE_ELM327;

		return RUBBISH;
	}

	@Override
	public int read_comport(byte[] buf, int timeout) throws IOException {
		if (input.available() == 0) {
			try {
				Thread.sleep(timeout);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			if (input.available() == 0)
				return TIMEOUT;
		}
		int len = input.read(buf);
		if (len == 0)
			return EMPTY;
		logger.logVerbose("RX: " + new String(buf));
		for (int p = 0; p < len; p++) {
			if (buf[p] == '>') {
				return PROMPT;
			}
		}
		return DATA;
	}

	@Override
	public void send_command(byte[] command) throws IOException {
		output.write(command);
		output.write(new byte[] { '\r' });
		output.flush();
	}

	// Lifted from Scantool
	String get_protocol_string(int interface_type, int protocol_id) {
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
	void reset_proc() throws IOException {
		logger.logInfo("Resetting hardware interface.");
		// case RESET_START:
		// wait until we either get a prompt or the timer times out
		long time = System.currentTimeMillis();
		while (true) {
			if (input.available() > 0) {
				if (input.read() == PROMPT)
					break;
			} else {
				if (System.currentTimeMillis() - time > ATZ_TIMEOUT)
					break;
			}
		}
		send_command("atz"); // reset the chip

		// case RESET_WAIT_RX:
		byte[] buf = new byte[128];
		int status = read_comport(buf, ATZ_TIMEOUT); // read comport
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		if (status == DATA) // if new data detected in com port buffer
			response.append(new String(buf)); // append contents of buf to
												// response
		else if (status == PROMPT) // if '>' detected
		{
			response.append(new String(buf));
			device = process_response("atz".getBytes(), response.toString()
					.getBytes());
			if (device == INTERFACE_ELM323 || device == INTERFACE_ELM327) {
				logger.logInfo("Waiting for ECU timeout...");
				RESET_ECU_TIMEOUT();
				return;
			} else
				return;
		} else if (status == TIMEOUT) // if the timer timed out
		{
			logger.logWarning("Interface was not found");
			return;
		}
	}

	void RESET_ECU_TIMEOUT() throws IOException {
		// if (serial_time_out) // if the timer timed out
		// {
		if (device == INTERFACE_ELM327) {
			send_command("0100");
			response = new StringBuffer(256);
			logger.logInfo("Detecting OBD protocol...");
			RESET_WAIT_0100();
			return;
		} else
			return;
		// }
	}

	void RESET_WAIT_0100() throws IOException {
		byte[] buf = new byte[128];
		int status = read_comport(buf, ECU_TIMEOUT);

		if (status == DATA) // if new data detected in com port buffer
			response.append(new String(buf)); // append contents of buf to
												// response
		else if (status == PROMPT) // if we got the prompt
		{
			response.append(new String(buf));
			status = process_response("0100".getBytes(), response.toString()
					.getBytes());

			if (status == ERR_NO_DATA || status == UNABLE_TO_CONNECT)
				logger
						.logWarning("Protocol could not be detected. Please check connection to the vehicle, and make sure the ignition is ON");
			else if (status != HEX_DATA)
				logger.logWarning("Communication error");

			return;
		} else if (status == TIMEOUT) // if the timer timed out
		{
			logger.logWarning("Interface not found");
			return;
		}
	}

}
