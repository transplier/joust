package com.dgis.JOuST;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import com.dgis.JOuST.serial.ObdSerial;

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
public class OBDEventDriver implements IOBDEventDriver {
	private ObdSerial device;
	
	private Thread requester;
	
	//TODO: Add locks around this everywhere.
	private Queue<PIDQueueItem> processingQueue = new LinkedList<PIDQueueItem>();

	private volatile boolean shutdownFlag = false;
	
	private volatile Runnable onStop = null;
	
	/**
	 * Initialize this ObdInterfaceDriver. 
	 * @param device ObdSerial instance to use. Should be ready to accept requestPid()'s.
	 */
	public OBDEventDriver(ObdSerial device) {
		if(!device.isOpen()) throw new IllegalArgumentException("ObdSerial device passed to ObdInterfaceDriver is not open!");
		this.device = device;
		spawnRequesterThread();
	}
	
	private boolean schedule(PIDResultListener list, int pid, boolean persistent) {
		//try to find the pid already in the list.
		for(PIDQueueItem qi : processingQueue){
			if(qi.pid == pid){
				PIDListenerQueueItem lqi = new PIDListenerQueueItem(list, persistent);
				qi.listeners.add(lqi);
				return true;
			}
		}
		
		//Need a new PIDQueueItem
		PIDQueueItem qi = new PIDQueueItem(pid, new PIDListenerQueueItem(list, persistent));
		return processingQueue.add(qi);
	}
	
	private void issueRequest() {
		final PIDQueueItem qi = processingQueue.poll();
		if(qi == null){
			//nothing in queue!
			//TODO stop thread, restart on adding a request. Otherwise, 100% cpu use infinite loop!
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} //TODO this is a workaround.
			return; 
		}
		
		try {
			device.requestPID(new PIDResultListener() {

				@Override
				public void dataReceived(int pid, int numBytes, byte[] data) {
					for(PIDListenerQueueItem lqi : qi.listeners)
						lqi.listener.dataReceived(pid, numBytes, data);
					cleanup();
				}

				@Override
				public void error(String msg, int pid) {
					for(PIDListenerQueueItem lqi : qi.listeners)
						lqi.listener.error(msg, pid);
					cleanup();
				}
				
				private void cleanup() {
					//by re-adding here, we avoid re-request of pid until this request is done.
					ArrayList<PIDListenerQueueItem> toRemove = new ArrayList<PIDListenerQueueItem>();
					for(PIDListenerQueueItem lqi : qi.listeners) {
						if(!lqi.persistent) toRemove.add(lqi);
					}
					qi.listeners.removeAll(toRemove);
					
					//TODO: check to make sure nobody added anything to processingQueue related to qi.pid
					//Alternately, lock the queue and just call enqueue() a bunch of times.
					if(qi.listeners.size() != 0) processingQueue.add(qi);
				}
				
			}, qi.pid);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (PIDNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private void spawnRequesterThread() {
		requester = new Thread(new Runnable(){
			@Override
			public void run() {
				while(!shutdownFlag) {
					issueRequest();
				}
				if(onStop != null) onStop.run();
			}
		});
		//requester.setDaemon(true);
		requester.start();
	}

	private int LookupPid(String name) throws PIDNotFoundException {
		Integer pid = OBDInterface.PID_NAMES.get(name);
		if(pid == null) throw new PIDNotFoundException(-1);
		return(pid);
	}

	
	/**
	 * @see com.dgis.JOuST.IOBDEventDriver#scheduleRepeating(com.dgis.JOuST.PIDResultListener, int)
	 */
	public boolean scheduleRepeating(final PIDResultListener list, int pid) {
		return schedule(list, pid, true);
	}
	
	/**
	 * @see com.dgis.JOuST.IOBDEventDriver#scheduleOnce(com.dgis.JOuST.PIDResultListener, int)
	 */
	public boolean scheduleOnce(PIDResultListener list, int pid) {
		return schedule(list, pid, false);
	}

	@Override
	public boolean scheduleOnce(PIDResultListener list, String name) throws PIDNotFoundException {
		return schedule(list, LookupPid(name), false);
	}

	@Override
	public boolean scheduleRepeating(PIDResultListener list, String name) throws PIDNotFoundException {
		return schedule(list, LookupPid(name), true);
	}
	
	/**
	 * @see com.dgis.JOuST.IOBDEventDriver#stop()
	 */
	public boolean stop(Runnable onStop) {
		shutdownFlag  = true;
		try {
			requester.join();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		return true;
	}

	@Override
	public boolean remove(PIDResultListener list) {
		//TODO
		return false;
	}

	@Override
	public boolean removeAll(int pid) {
		//TODO
		return false;
	}

	@Override
	public boolean clear() {
		processingQueue = new LinkedList<PIDQueueItem>();
		return true;
	}
	
}

final class PIDListenerQueueItem {
	public PIDResultListener listener;
	public boolean persistent;
	public PIDListenerQueueItem(PIDResultListener list, boolean persist) {
		listener=list;
		persistent=persist;
	}
}
final class PIDQueueItem {
	public List<PIDListenerQueueItem> listeners = new ArrayList<PIDListenerQueueItem>();
	int pid;
	public PIDQueueItem(int pid, PIDListenerQueueItem ... lists) {
		this.pid=pid;
		for(PIDListenerQueueItem lqi : lists)
			listeners.add(lqi);
	}
}