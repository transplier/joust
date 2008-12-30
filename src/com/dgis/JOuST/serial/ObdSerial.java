package com.dgis.JOuST.serial;

import java.io.IOException;

import gnu.io.PortInUseException;
import gnu.io.UnsupportedCommOperationException;

public interface ObdSerial {
	
	// timeouts
	public static final int  OBD_REQUEST_TIMEOUT=   9900;
	public static final int  ATZ_TIMEOUT=           1500;
	public static final int  AT_TIMEOUT=            130;
	public static final int  ECU_TIMEOUT=           5000;
	
	
	void open_comport() throws PortNotFoundException, PortInUseException, UnsupportedCommOperationException, IOException;
	void close_comport() throws IOException;
	void send_command(byte[] command) throws IOException;
	ELMReadResult read_comport(byte[] buf, int timeout) throws IOException;
	ELMResponse process_response(byte[] cmd_sent, byte[] msg_received) throws IOException;
	//const char *get_protocol_string(int interface_type, int protocol_id);
	String getErrorMessage();
	
	ResetResult reset_proc() throws IOException;
	
	public byte[] request_pid(int pid, int numBytes) throws IOException;
	
	public ElmSerialState getState();
}

enum ElmSerialState {
	READY, NOT_OPEN
}

enum ELMResponse{
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
	INTERFACE_ELM320{public String toString(){return "ELM 320";}},
	INTERFACE_ELM322{public String toString(){return "ELM 322";}},
	INTERFACE_ELM323{public String toString(){return "ELM 323";}},
	INTERFACE_ELM327{public String toString(){return "ELM 327";}},
	
	PROTOCOL_INIT_ERROR;
	
	public String toString(){ return getMessage(); }
	
	String getMessage(){
		return getMessage(this);
	}
	// Adapted from ScanTool
	public static String getMessage(ELMResponse error) {
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

class ResetResult{
	public boolean foundDevice;
	public ELMResponse response;
	//public OBDInterfaceType
	public ResetResult(ELMResponse response, boolean found) {
		this.response=response;
		foundDevice=found;
	}
}
