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
	int read_comport(byte[] buf, int timeout) throws IOException;
	ELMResponse process_response(byte[] cmd_sent, byte[] msg_received) throws IOException;
	//int find_valid_response(char *buf, char *response, const char *filter, char **stop);
	//const char *get_protocol_string(int interface_type, int protocol_id);
	String getErrorMessage();
	
	void reset_proc() throws IOException;
	
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
	INTERFACE_ELM320,
	INTERFACE_ELM322,
	INTERFACE_ELM323,
	INTERFACE_ELM327,
}

class ResetResult{
	public ELMResponse interfaceType;
	//public OBDInterfaceType
}
