package com.dgis.JOuST.serial;

import static org.junit.Assert.*;

import java.io.IOException;

import org.junit.Test;

public class ElmSerialTest {

	@Test
	public void testElmSerial() {
	}

	@Test
	public void testOpen_comport() {
	}

	@Test
	public void testClose_comport() {
	}

	@Test
	public void testGetErrorMessage() {
	}

	@Test
	public void testGetState() {
	}

	private void strcpy(byte[] buf, String s) {
		for (int x = 0; x < s.length(); x++)
			buf[x] = (byte) s.charAt(x);
		buf[s.length()] = 0;
	}

	@Test
	public void testProcess_response() throws IOException {
		ElmSerial test = new ElmSerial("", 0);
		byte[] buf = new byte[200];
		strcpy(buf, "ELM320");
		int ret = test.process_response(null, buf);
		assertTrue((ret == ObdSerial.INTERFACE_ELM320));

		strcpy(buf, "ELM323");
		ret = test.process_response(null, buf);
		assertTrue((ret == ObdSerial.INTERFACE_ELM323));

		strcpy(buf, "9f");
		ret = test.process_response(null, buf);
		assertTrue((ret == ObdSerial.HEX_DATA));

		strcpy(buf, "9F");
		ret = test.process_response(null, buf);
		assertTrue((ret == ObdSerial.HEX_DATA));

		strcpy(buf, "97");
		ret = test.process_response(null, buf);
		assertTrue((ret == ObdSerial.HEX_DATA));

		strcpy(buf, "AF");
		ret = test.process_response(null, buf);
		assertTrue((ret == ObdSerial.HEX_DATA));

		strcpy(buf, "FA");
		ret = test.process_response(null, buf);
		assertTrue((ret == ObdSerial.HEX_DATA));

		strcpy(buf, "<DATA ERROR>");
		ret = test.process_response(null, buf);
		assertTrue((ret == ObdSerial.DATA_ERROR2));
	}

	@Test
	public void testRead_comport() {
	}

	@Test
	public void testSend_command() {
	}


}
