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
		ELMResponse ret = test.process_response(null, buf);
		assertTrue((ret == ELMResponse.INTERFACE_ELM320));

		strcpy(buf, "ELM323");
		ret = test.process_response(null, buf);
		assertTrue((ret == ELMResponse.INTERFACE_ELM323));

		strcpy(buf, "9f");
		ret = test.process_response(null, buf);
		assertTrue((ret == ELMResponse.HEX_DATA));

		strcpy(buf, "9F");
		ret = test.process_response(null, buf);
		assertTrue((ret == ELMResponse.HEX_DATA));

		strcpy(buf, "97");
		ret = test.process_response(null, buf);
		assertTrue((ret == ELMResponse.HEX_DATA));

		strcpy(buf, "AF");
		ret = test.process_response(null, buf);
		assertTrue((ret == ELMResponse.HEX_DATA));

		strcpy(buf, "FA");
		ret = test.process_response(null, buf);
		assertTrue((ret == ELMResponse.HEX_DATA));

		buf = new byte[] { 52, 49, 32, 48, 48, 32, 57, 56, 32, 49, 56, 32, 56,
				48, 32, 48, 49, 32, 13, 52, 49, 32, 48, 48, 32, 66, 69, 32, 51,
				70, 32, 69, 56, 32, 49, 51, 32, 13, 13, 62, 0, 0, 0, 0, 0, 0,
				0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
				0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
				0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
				0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, };
		ret = test.process_response("0100".getBytes(), buf);
		assertTrue((ret == ELMResponse.HEX_DATA));

		strcpy(buf, "<DATA ERROR>");
		ret = test.process_response(null, buf);
		assertTrue((ret == ELMResponse.DATA_ERROR2));
	}

	@Test
	public void testRead_comport() {
	}

	@Test
	public void testSend_command() {
	}


}
