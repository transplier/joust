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
			//Try getting various data as a test.
			long startTime = System.currentTimeMillis();
			int count=0;
			outln("Throttle\tSpeed\tRPM\tRate");
			while(true){
				byte[] throttleResult = underTest.request_pid(0x11, 1);
				byte[] speedResult = underTest.request_pid(0x0D, 1);
				byte[] rpmResult = underTest.request_pid(0x0C, 2);
				count++;
				float rawThrottle = (float)Integer.valueOf(ElmSerial.bytesToString(throttleResult).substring(4), 16);
				float rawSpeed = (float)Integer.valueOf(ElmSerial.bytesToString(speedResult).substring(4), 16);
				float rawRPM = (float)Integer.valueOf(ElmSerial.bytesToString(rpmResult).substring(4), 16);
				rawThrottle*=100f/255f;
				outln(rawThrottle+"\t"+(rawSpeed/1.609)+"\t"+(rawRPM/4.)+"\t"+(count/((System.currentTimeMillis()-startTime)/1000.)));
				
				if(rawRPM/4>4000) break;
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
