package com.dgis.JOuST;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Queue;

import com.dgis.JOuST.serial.ObdSerial;
import com.dgis.JOuST.serial.PIDResultListener;

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
 * This class serves as an additional abstraction layer between the interface
 * hardware and library users. It provides an event interface over top of
 * an ObdSerial interface.
 *
 * Copyright (C) 2009 Giacomo Ferrari
 * @author Giacomo Ferrari
 */
public class ObdInterfaceDriver {
	private ObdSerial device;
	
	private Thread requester;
	
	//TODO: Make this lockless? Do we care?
	private Queue<PidListenerQueueItem> processingQueue = new LinkedList<PidListenerQueueItem>();
	
	/**
	 * Initialize this ObdInterfaceDriver. 
	 * @param device ObdSerial instance to use. Should be ready to accept requestPid()'s.
	 */
	public ObdInterfaceDriver(ObdSerial device) {
		if(!device.isOpen()) throw new IllegalArgumentException("ObdSerial device passed to ObdInterfaceDriver is not open!");
		spawnRequesterThread();
	}

	private void spawnRequesterThread() {
		requester = new Thread(new Runnable(){
			@Override
			public void run() {
				
			}
		});
	}
}

final class PidListenerQueueItem {
	public PIDResultListener listener;
	public int pid;
	public boolean persistent;
}
