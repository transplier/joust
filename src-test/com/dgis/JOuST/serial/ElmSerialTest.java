package com.dgis.JOuST.serial;

import static org.junit.Assert.*;

import gnu.io.PortInUseException;
import gnu.io.UnsupportedCommOperationException;

import java.io.IOException;

import org.junit.Test;

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
 * Some tests for ElmSerial
 *
 * Copyright (C) 2009 Giacomo Ferrari
 * @author Giacomo Ferrari
 */

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
	public void testProcess_response() throws IOException, PortInUseException, UnsupportedCommOperationException {
		ElmSerial test = new ElmSerial(null, null);
		byte[] buf = new byte[200];
		strcpy(buf, "ELM320");
		ElmResponseVisitor visit = new AElmResponseVisitor(){
			@Override
			Object defaultCase() {
				fail();
				return null;
			}
			@Override
			public Object interfaceFound(ELMInterfaceType type) {
				assertTrue((type == ELMInterfaceType.INTERFACE_ELM320));
				return null;
			}
		};
		
		test.process_response(visit, null, buf);

		
		visit = new AElmResponseVisitor(){
			@Override
			Object defaultCase() {
				fail();
				return null;
			}
			@Override
			public Object interfaceFound(ELMInterfaceType type) {
				assertTrue((type == ELMInterfaceType.INTERFACE_ELM323));
				return null;
			}
		};		
		strcpy(buf, "SEARCHING...ELM323");
		test.process_response(visit, null, buf);

		
		visit = new AElmResponseVisitor(){
			@Override
			Object defaultCase() {
				fail();
				return null;
			}
			@Override
			public Object hexData() {
				return null;
			}
		};	
		
		strcpy(buf, "9f");
		test.process_response(visit, null, buf);

		strcpy(buf, "BUS INIT: ...OK\n\r9F\t\t\r\n");
		test.process_response(visit, null, buf);

		strcpy(buf, "97  ");
		test.process_response(visit, null, buf);

		strcpy(buf, " AF");
		test.process_response(visit, null, buf);

		strcpy(buf, "FA");
		test.process_response(visit, null, buf);
		
		
		buf = new byte[] { 52, 49, 32, 48, 48, 32, 57, 56, 32, 49, 56, 32, 56,
				48, 32, 48, 49, 32, 13, 52, 49, 32, 48, 48, 32, 66, 69, 32, 51,
				70, 32, 69, 56, 32, 49, 51, 32, 13, 13, 62, 0, 0, 0, 0, 0, 0,
				0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
				0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
				0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
				0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, };
		byte[] req = "0100".getBytes();
		System.err.println("\n");
		test.process_response(visit, req, buf);
		
		buf = new byte[] { 52, 120, 32, 48, 48, 32, 57, 56, 32, 49, 56, 32, 56,
				48, 32, 48, 49, 32, 13, 52, 49, 32, 48, 48, 32, 66, 69, 32, 51,
				70, 32, 69, 56, 32, 49, 51, 32, 13, 13, 62, 0, 0, 0, 0, 0, 0,
				0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
				0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
				0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
				0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, };
		System.err.println("\n");
		visit = new AElmResponseVisitor(){
			@Override
			Object defaultCase() {
				fail();
				return null;
			}
			@Override
			public Object rubbish() {
				return null;
			}
		};
		test.process_response(visit, req, buf);

		visit = new AElmResponseVisitor(){
			@Override
			Object defaultCase() {
				fail();
				return null;
			}
			@Override
			public Object dataError2() {
				return null;
			}
		};
		strcpy(buf, "<DATA ERROR>");
		test.process_response(visit, null, buf);
		
		visit = new AElmResponseVisitor(){
			@Override
			Object defaultCase() {
				fail();
				return null;
			}
			@Override
			public Object unableToConnect() {
				return null;
			}
		};
		strcpy(buf, ">\n\rUNABLETOCONNECT\n");
		test.process_response(visit, null, buf);
	}

	@Test
	public void testRead_comport() {
	}

	@Test
	public void testSend_command() {
	}
	
	@Test
	public void testBytesToString() {
		byte[] a = "Hello".getBytes();
		assertTrue(ElmSerial.bytesToString(a).equals("Hello"));
		
		a = "Hello\0".getBytes();
		assertTrue(ElmSerial.bytesToString(a).equals("Hello"));
		
		a = "Hell\0o\0".getBytes();
		assertTrue(ElmSerial.bytesToString(a).equals("Hell"));
		a = "\0Hell\0o\0".getBytes();
		assertTrue(ElmSerial.bytesToString(a).equals(""));
	}

	@Test
	public void testFind_valid_response(){
		byte[] buf = new byte[255];
		int[] end = new int[1];
		
		boolean res = ElmSerial.find_valid_response(buf, "41 11 25 \r41 11 25 \r\r>", "4111", end);
		assertTrue(res);
		String r = ElmSerial.bytesToString(buf);
		assertTrue(r.equals("411125"));
	}

}
