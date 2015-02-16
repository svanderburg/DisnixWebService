/* 
 * Copyright (c) 2008-2010 Sander van der Burg
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
	 * Runs the job on the core Disnix service.
	 */
	public abstract void run();
}
