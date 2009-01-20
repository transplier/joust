package com.dgis.JOuST.serial;

import com.dgis.util.Logger;
import com.dgis.util.SerialHelper;

public class TestConnectAndID {

	public static void main(String[] args) {
		Logger log = Logger.getInstance();
		log.setPrintStream(System.err);
		log.setTimestamps(false);
		log.setLevel(Logger.LEVEL_VERBOSE);
		log.setShowCaller(true);
		
		String dev = args[0];
		int speed = 38400;
		try{
			gnu.io.SerialPort port = SerialHelper.open(dev, speed, "JOuST Tester", log);
			ObdSerial underTest = new ElmSerial(port.getInputStream(), port.getOutputStream());
			outln("Opening port...");
			ResetResult result = underTest.resetAndHandshake();
			outln("Result of reset: "+result.response.toString());
			if(result.foundDevice == false){
				outln("Device not found, quitting.");
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
			
			while(System.in.available()>0){
				underTest.requestPID(throttleList, 0x11, 1);
				underTest.requestPID(speedList, 0x0D, 1);
				underTest.requestPID(rpmList, 0x0C, 2);
				count++;				
				outln(""+(count/((System.currentTimeMillis()-startTime)/1000.)));
			}
			outln("Closing port...");
			
			underTest.stop();
		} catch (Exception e){
			e.printStackTrace();
		}
	}

	private static void outln(String string) {
		System.out.println(string);
	}

}
