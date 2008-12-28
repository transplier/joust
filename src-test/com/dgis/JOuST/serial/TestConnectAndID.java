package com.dgis.JOuST.serial;

import com.dgis.util.Logger;

public class TestConnectAndID {

	public static void main(String[] args) {
		Logger log = Logger.getInstance();
		log.setPrintStream(System.err);
		log.setLevel(Logger.LEVEL_VERBOSE);
		log.setShowCaller(true);
		
		String dev = args[0];
		int speed = 38400;
		ElmSerial underTest = new ElmSerial(dev, speed);
		try{
			outln("Opening port...");
			underTest.open_comport();
			underTest.reset_proc();
			outln("Closing port...");
			underTest.close_comport();
		} catch (Exception e){
			e.printStackTrace();
		}
	}

	private static void outln(String string) {
		System.out.println(string);
	}

}
