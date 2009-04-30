package com.dgis.JOuST;


/**
 * This interface serves as an additional abstraction layer between the interface
 * hardware and library users. It provides an event interface over top of
 * an ObdSerial interface.
 *
 * Copyright (C) 2009 Giacomo Ferrari
 * @author Giacomo Ferrari
 */
public interface IOBDEventDriver {

	
	/**
	 * Schedule the system to query for the specified pid exactly once,
	 * as soon as possible, calling the given listener when the results arrive.
	 * @param list The listener to handle incoming data for this request.
	 * @param pid the pid to request.
	 * @return true if scheduling was successful.
	 */
	public boolean scheduleRepeating(PIDResultListener list, int pid);
	public boolean scheduleRepeating(PIDResultListener list, String name) throws PIDNotFoundException;

	/**
	 * Schedule the system to repeatedly query for the specified pid as fast as
	 * possible, calling the given listener when the results arrive.
	 * @param list The listener to handle incoming data for this request.
	 * @param pid the pid to request.
	 * @return true if scheduling was successful.
	 */
	public boolean scheduleOnce(PIDResultListener list, int pid);
	public boolean scheduleOnce(PIDResultListener list, String name) throws PIDNotFoundException;
	
	/**
	 * Removes all requests to be handled by a certain PIDResultListener.
	 * @param list remove all references to this.
	 * @return true if at least one request was removed.
	 */
	public boolean remove(PIDResultListener list);
	
	/**
	 * Removes all requests to a certain pid.
	 * @param pid remove all references to this.
	 * @return true if at least one request was removed.
	 */
	public boolean removeAll(int pid);

	/**
	 * Remove all requests.
	 * @return true if clear succeeded.
	 */
	public boolean clear();
	
	/**
	 * Stop all requests. Instance may not be reused.
	 * Asynchronous stop - operations will possibly cease
	 * _after_ this method returns.
	 * @param onStop Run this when stop is completed.
	 * @return true if halt succeeded.
	 */
	public boolean stop(Runnable onStop);

}