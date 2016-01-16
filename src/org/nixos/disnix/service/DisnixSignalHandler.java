/* 
 * Copyright (c) 2008-2016 Sander van der Burg
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
import java.util.*;
import org.freedesktop.dbus.*;
import org.nixos.disnix.Disnix;

/**
 * Waits and reacts on D-Bus signals received from the Disnix core service
 */
public class DisnixSignalHandler implements DBusSigHandler
{
	/** Hashtable mapping job IDs on threads */
	private Hashtable<Integer, DisnixThread> pids;
	
	/**
	 * Creates a new signal handler instance
	 */
	public DisnixSignalHandler()
	{
		pids = new Hashtable<Integer, DisnixThread>();
	}
	
	/**
	 * Adds a new job on which the signal handler can react
	 * 
	 * @param pid Job ID received by the core service
	 * @param thread Thread in which the job is executed
	 */
	public void addPid(int pid, DisnixThread thread)
	{
		pids.put(pid, thread);
	}
	
	/**
	 * Handles an incoming D-Bus signal
	 * 
	 * @param s Representation of the received D-Bus signal
	 */
	public void handle(DBusSignal s)
	{	
		DisnixThread thread;
		int pid = -1;
		
		/* Only handle signals that we expect */
		if(s instanceof Disnix.finish)
		{
			pid = ((Disnix.finish)s).pid;
			System.err.println("Caught finish signal from pid: "+pid);
		}
		else if(s instanceof Disnix.success)
		{
			pid = ((Disnix.success)s).pid;
			System.err.println("Caught success signal from pid: "+pid);
		}
		else if(s instanceof Disnix.failure)
		{
			pid = ((Disnix.failure)s).pid;
			System.err.println("Caught failure signal from pid: "+pid);
		}
		else
		{
			System.err.println("Caught unknown signal: " + s);
			return;
		}
		
		/* Get the corresponding pid from the table, or wait until it has been added */
		do
		{
			thread = pids.remove(pid);
		}
		while(thread == null);
		
		/* Resume the thread attached to the received PID */
		thread.setSource(s);
		thread.resume();
	}
}
