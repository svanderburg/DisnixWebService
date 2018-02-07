/* 
 * Copyright (c) 2008-2018 Sander van der Burg
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.nixos.disnix.service;
import org.freedesktop.dbus.*;
import org.nixos.disnix.*;

/**
 * A thread in which a job is started on the core Disnix service and
 * will wait until the job is finished.
 */
public abstract class DisnixThread implements Runnable
{
	/** Origin of the D-Bus signal that notifies this thread */
	protected DBusSignal source;
	
	/** Indicates whether this thread is suspended */
	private boolean suspended = false;
	
	/** Interface used to communicate to the core D-Bus service */
	private Disnix disnixInterface;

	/** Signal handler which waits and reacts on D-Bus signals of the Disnix core service */
	private DisnixSignalHandler handler;
	
	/** Process ID of the running D-Bus task */
	protected int pid;
	
	/** Directory name in which log files are stored */
	private String logdir;
	
	public DisnixThread(int pid, Disnix disnixInterface, DisnixSignalHandler handler, String logdir)
	{
		this.pid = pid;
		this.disnixInterface = disnixInterface;
		this.handler = handler;
		this.logdir = logdir;
	}
	
	/**
	 * Sets the origin of the D-Bus signal that notifies this thread
	 * 
	 * @param source D-Bus signal notifying this thread
	 */
	public void setSource(DBusSignal source)
	{
		this.source = source;
	}
	
	/**
	 * Returns the origin of the D-Bus signal that notifies this thread
	 * 
	 * @return D-Bus signal notifying this thread
	 */
	public DBusSignal getSource()
	{
		return source;
	}
	
	/**
	 * Indicates that this thread should be suspended.
	 */
	public synchronized void suspend()
	{
		suspended = true;
	}
	
	/**
	 * Resumes the thread if it has been suspended.
	 */
	public synchronized void resume()
	{
		if(suspended)
		{
			suspended = false;
			notify();
		}
	}
	
	/**
	 * Indicates whether the thread has been suspended
	 *
	 * @return true iff it has been suspended
	 */
	public synchronized boolean isSuspended()
	{
		return suspended;
	}
	
	/**
	 * Suspends this thread until it receives a notification.
	 * 
	 * @throws InterruptedException If this thread is interrupted
	 */
	public synchronized void waitForNotificationToResume() throws InterruptedException
	{
		while(suspended)
			wait();
	}
	
	/**
	 * Invokes a D-Bus operation
	 */
	public abstract void invokeOperation();
	
	/**
	 * Runs the job on the core Disnix service.
	 */
	public void run()
	{
		try
		{
			handler.addPid(pid, this);
			invokeOperation();
			suspend();
			waitForNotificationToResume();
		}
		catch(InterruptedException ex)
		{
			ex.printStackTrace();
		}
	}
	
	/**
	 * Waits for a signal and returns when a finish signal has been received.
	 * 
	 * @throws Exception If something went wrong
	 */
	public void waitForFinish() throws Exception
	{
		Thread thread = new Thread(this);
		thread.start();
		thread.join();
		
		if(source instanceof Disnix.finish)
			return;
		else if(source instanceof Disnix.failure)
			throw DisnixException.constructDisnixException(pid, logdir);
		else
			throw new Exception("Unknown event caught: "+source);
	}
	
	/**
	 * Waits for a signal and returns when a success signal has been received.
	 * 
	 * @return A string array with payload, typically an array of paths
	 * @throws Exception If something went wrong
	 */
	public String[] waitForSuccess() throws Exception
	{
		Thread thread = new Thread(this);
		thread.start();
		thread.join();
		
		if(source instanceof Disnix.success)
			return ((Disnix.success)source).derivation;
		else if(source instanceof Disnix.failure)
			throw DisnixException.constructDisnixException(pid, logdir);
		else
			throw new Exception("Unknown event caught: "+source);
	}
}
