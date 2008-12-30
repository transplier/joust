package com.dgis.JOuST.serial;

import com.dgis.util.Logger;

public class TestConnectAndID {

	public static void main(String[] args) {
		Logger log = Logger.getInstance();
		log.setPrintStream(System.err);
		log.setTimestamps(false);
		log.setLevel(Logger.LEVEL_VERBOSE);
		log.setShowCaller(true);
		
		String dev = args[0];
		int speed = 38400;
		ElmSerial underTest = new ElmSerial(dev, speed);
		try{
			outln("Opening port...");
			underTest.open_comport();
			outln("Result of reset: "+underTest.reset_proc().response.toString());
			ELMResponse device = underTest.getDevice();
			outln("Detected interface: "+(device==null?"None":device.toString()));
			//Try getting throttle position as a test.
			while(true){
				byte[] result = underTest.request_pid(0x11, 1);
				float rawPos = (float)Integer.valueOf(ElmSerial.bytesToString(result).substring(4), 16);
				rawPos*=100f/255f;
				outln("Throttle position: "+rawPos);
				if(rawPos>80) break;
			}
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
