package com.dgis.JOuST.serial;

import java.io.IOException;

import gnu.io.PortInUseException;
import gnu.io.UnsupportedCommOperationException;

public interface ObdSerial {
	
	//process_response return values
	public static final int HEX_DATA = 0;
	public static final int BUS_BUSY = 1;
	public static final int BUS_ERROR = 2;
	public static final int BUS_INIT_ERROR = 3;
	public static final int UNABLE_TO_CONNECT = 4;
	public static final int CAN_ERROR = 5;
	public static final int DATA_ERROR = 6;
	public static final int DATA_ERROR2 = 7;
	public static final int ERR_NO_DATA = 8;
	public static final int BUFFER_FULL = 9;
	public static final int SERIAL_ERROR = 10;
	public static final int UNKNOWN_CMD = 11;
	public static final int RUBBISH = 12;

	public static final int  INTERFACE_ID=       13;
	public static final int  INTERFACE_ELM320=   13;
	public static final int  INTERFACE_ELM322=   14;
	public static final int  INTERFACE_ELM323=   15;
	public static final int  INTERFACE_ELM327=   16;

	// timeouts
	public static final int  OBD_REQUEST_TIMEOUT=   9900;
	public static final int  ATZ_TIMEOUT=           1500;
	public static final int  AT_TIMEOUT=            130;
	public static final int  ECU_TIMEOUT=           5000;
	
	
	void open_comport() throws PortNotFoundException, PortInUseException, UnsupportedCommOperationException, IOException;
	void close_comport() throws IOException;
	void send_command(byte[] command) throws IOException;
	int read_comport(byte[] buf) throws IOException;
	void start_serial_timer(int delay);
	void stop_serial_timer();
	int process_response(byte[] cmd_sent, byte[] msg_received) throws IOException;
	//int find_valid_response(char *buf, char *response, const char *filter, char **stop);
	//const char *get_protocol_string(int interface_type, int protocol_id);
	String getErrorMessage();
	
	public ElmSerialState getState();
}

enum ElmSerialState {
	READY, NOT_OPEN
}
