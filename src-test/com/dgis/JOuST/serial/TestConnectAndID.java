package com.dgis.JOuST.serial;

import gnu.io.PortInUseException;
import gnu.io.UnsupportedCommOperationException;

import java.awt.Font;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.Socket;

import javax.swing.*;

import org.junit.runners.model.FrameworkMethod;

import com.dgis.JOuST.IOBDEventDriver;
import com.dgis.JOuST.OBDEventDriver;
import com.dgis.JOuST.PIDNotFoundException;
import com.dgis.JOuST.PIDResultListener;
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
		System.out.println("S for serial, C for console, N for network.");
		char choice = (char) System.in.read();
		
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
		} else if(choice=='n'){
			Socket s = new Socket("10.121.10.219", 99);
			in=s.getInputStream();
			out=s.getOutputStream();
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

	private static JLabel makeLabel(String text) {
		JLabel lab = new JLabel(text);
		lab.setFont(new Font("Arial", Font.BOLD, 32));
		return lab;
	}
	
	private static void runDataGettingLoop(final InputStream in, final OutputStream out) throws IOException, PIDNotFoundException {
		final ObdSerial underTest = new ElmSerial(in, out);
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
		
		JFrame frame = new JFrame();
		frame.getContentPane().setLayout(new java.awt.GridLayout(6, 2));
		final JLabel throttleLabel = makeLabel("");
		final JLabel speedLabel = makeLabel("");
		final JLabel rpmLabel = makeLabel("");
		final JLabel mafLabel = makeLabel("");
		final JLabel mpgLabel = makeLabel("");
		final JLabel avgMpgLabel = makeLabel("");
		frame.getContentPane().add(makeLabel("Throttle: "));
		frame.getContentPane().add(throttleLabel);
		frame.getContentPane().add(makeLabel("Speed: "));
		frame.getContentPane().add(speedLabel);
		frame.getContentPane().add(makeLabel("RPM: "));
		frame.getContentPane().add(rpmLabel);
		frame.getContentPane().add(makeLabel("MAF: "));
		frame.getContentPane().add(mafLabel);
		frame.getContentPane().add(makeLabel("MPG: "));
		frame.getContentPane().add(mpgLabel);
		frame.getContentPane().add(makeLabel("Avg MPG: "));
		frame.getContentPane().add(avgMpgLabel);
		
		frame.setSize(400, 600);
		frame.setVisible(true);
		
		FileOutputStream log = new FileOutputStream("log.txt");
		final PrintStream log_ps = new PrintStream(log);
		
		PIDResultListener pidList = new PIDResultListener(){
			double maf=-1;
			double speed=-1;
			double mpg=-1;
			@Override
			public synchronized void dataReceived(int pid, int numBytes, byte[] data) {
				switch(pid){
				case 0x11:
					double rawThrottle = (double)Integer.valueOf(ElmSerial.bytesToString(data).substring(4), 16);
					rawThrottle*=100f/255f;
					System.out.println("Throttle: "+rawThrottle);
					log_ps.println("Throttle, "+rawThrottle);
					throttleLabel.setText(""+rawThrottle);
					break;
				case 0x0D:
					double rawSpeed = (double)Integer.valueOf(ElmSerial.bytesToString(data).substring(4), 16);
					rawSpeed/=1.609;
					speed=rawSpeed;
					System.out.println("Speed: "+rawSpeed);
					log_ps.println("Speed, "+rawSpeed);
					speedLabel.setText(""+rawSpeed);
					break;
				case 0x0C:
					double rawRPM = (double)Integer.valueOf(ElmSerial.bytesToString(data).substring(4), 16);
					rawRPM/=4.;
					rpmLabel.setText(""+rawRPM);
					System.out.println("RPM: "+rawRPM);
					log_ps.println("RPM, "+rawRPM);
					break;
				case 0x10:
					double rawMAF = (double)Integer.valueOf(ElmSerial.bytesToString(data).substring(4), 16);
					rawMAF/=100.;
					maf=rawMAF;
					mafLabel.setText(""+rawMAF);
					System.out.println("MAF: "+rawMAF);
					log_ps.println("MAF, "+rawMAF);
					break;
				default:
					error("unknown pid", pid);
				}
				processMPG();
			}
			static final double STOICH = 14.7;
			static final double GASOLINE_DENSITY = 6.17;
			static final double GRAM_PER_POUND = 453.59237;
			static final double MILE_PER_KM = 0.62137119;
			static final double SECOND_PER_HOUR = 3600;
			
			static final int AVG_SIZE=100;
			double[] mpgs = new double[AVG_SIZE];
			int ptr=-1;
			private void processMPG() {
				if(maf >= 0 && speed >= 0){
					mpg = (STOICH * GASOLINE_DENSITY * GRAM_PER_POUND * speed * MILE_PER_KM)/(SECOND_PER_HOUR * maf);
					mpgLabel.setText(""+mpg);
					System.out.println("MPG: "+mpg);
					log_ps.println("MPG, "+mpg);
					maf=-1;
					speed=-1;
					if(ptr==-1){
						for(int x=0;x<AVG_SIZE; x++)
							mpgs[x]=mpg;
						ptr=0;
					}
					mpgs[ptr] = mpg;
					ptr = (ptr+1) % AVG_SIZE;
					double average = 0;
					for(int x=0;x<AVG_SIZE; x++)
						average+=mpgs[x];
					average/=AVG_SIZE;
					System.out.println("Avg MPG: "+average);
					log_ps.println("Avg MPG, "+average);
					avgMpgLabel.setText(""+average);
				}
			}
			@Override
			public void error(String msg, int pid) {
				System.err.println(msg);
			}
		};
		
		System.out.println("Starting data acquisition.");
		
		IOBDEventDriver event = new OBDEventDriver(underTest);
		
		event.scheduleRepeating(pidList, "Absolute Throttle Position");
		event.scheduleRepeating(pidList, "Speed");
		event.scheduleRepeating(pidList, "RPM");
		event.scheduleRepeating(pidList, "MAF");

		while(System.in.available()==0 || System.in.read() != 'q'){
		}
		outln("Closing port...");
		event.stop(new Runnable() {
			@Override
			public void run() {
				try {
					underTest.stop();
					in.close();
					out.close();
				} catch (Exception e){}
			}
		});
		frame.setVisible(false);
		frame.dispose();
		log_ps.close();
		System.exit(0);
	}

	private static void outln(String string) {
		System.out.println(string);
	}

}
