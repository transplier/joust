package com.dgis.JOuST.serial;

import gnu.io.PortInUseException;
import gnu.io.UnsupportedCommOperationException;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import com.dgis.util.Logger;
import com.dgis.util.SerialHelper;

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
 * Test program that establishes a connection to the scan tool and attempts to request data.
 *
 * Copyright (C) 2009 Giacomo Ferrari
 * @author Giacomo Ferrari
 */

public class TestConnectAndID {

	public static void main(String[] args) throws IOException, PortInUseException, UnsupportedCommOperationException {
		System.out.println("S for serial, C for console.");
		char choice = 's';//(char) System.in.read();
		
		InputStream in;
		OutputStream out;
		
		gnu.io.SerialPort port = null;
		
		Logger log = Logger.getInstance();
		
		if(choice=='s'){
			String dev = args[0];
			int speed = 38400;
			outln("Opening port...");
			port = SerialHelper.open(dev, speed, "JOuST Tester", log);
			in=port.getInputStream();
			out=port.getOutputStream();
		} else if(choice=='c'){
			in=System.in;
			out=System.out;
		} else {
			System.err.println("Wrong key, doofus.");
			return;
		}
		
		log.setPrintStream(System.err);
		log.setTimestamps(false);
		log.setLevel(Logger.LEVEL_VERBOSE);
		log.setShowCaller(true);
		
		try{
			
			runDataGettingLoop(in, out);
		} catch (Exception e){
			e.printStackTrace();
		}
		if(port!=null)
			port.close();

	}

	private static void runDataGettingLoop(InputStream in, OutputStream out) throws IOException {
		ObdSerial underTest = new ElmSerial(in, out);
		ResetResult result = underTest.resetAndHandshake();
		outln("Result of reset: "+result.response.toString());
		if(result.foundDevice == false){
			outln("Device not found, quitting.");
			in.close();
			out.close();
			return;
		}
		String device = underTest.getInterfaceIdentifier();
		outln("Detected interface: "+(device==null?"None":device));
		//Try getting various data as a test.
		long startTime = System.currentTimeMillis();
		int count=0;
		
		
		PIDResultListener throttleList = new PIDResultListener(){
			@Override
			public void dataReceived(int pid, int numBytes, byte[] data) {
				float rawThrottle = (float)Integer.valueOf(ElmSerial.bytesToString(data).substring(4), 16);
				rawThrottle*=100f/255f;
				System.out.println("Throttle: "+rawThrottle);
			}
			@Override
			public void error(String msg) {
				System.err.println(msg);
			}
		};
		PIDResultListener speedList = new PIDResultListener(){
			@Override
			public void dataReceived(int pid, int numBytes, byte[] data) {
				float rawSpeed = (float)Integer.valueOf(ElmSerial.bytesToString(data).substring(4), 16);
				rawSpeed/=1.609;
				System.out.println("Speed: "+rawSpeed);
			}
			@Override
			public void error(String msg) {
				System.err.println(msg);
			}
		};
		PIDResultListener rpmList = new PIDResultListener(){
			@Override
			public void dataReceived(int pid, int numBytes, byte[] data) {
				float rawRPM = (float)Integer.valueOf(ElmSerial.bytesToString(data).substring(4), 16);
				rawRPM/=4.;
				System.out.println("RPM: "+rawRPM);
			}
			@Override
			public void error(String msg) {
				System.err.println(msg);
			}
		};
		
		while(System.in.available()==0){
			underTest.requestPID(throttleList, 0x11, 1);
			underTest.requestPID(speedList, 0x0D, 1);
			underTest.requestPID(rpmList, 0x0C, 2);
			count++;				
			outln(""+(count/((System.currentTimeMillis()-startTime)/1000.)));
		}
		outln("Closing port...");
		
		underTest.stop();
		in.close();
		out.close();
	}

	private static void outln(String string) {
		System.out.println(string);
	}

}
