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
		DisnixThread thread = null;
		
		/* Only handle signals that we expect */
		if(s instanceof Disnix.finish)
		{
			System.out.println("Caught finish signal!");
			int pid = ((Disnix.finish)s).pid;			
			thread = pids.remove(pid);
		}
		else if(s instanceof Disnix.success)
		{
			System.out.println("Caught success signal!");
			int pid = ((Disnix.success)s).pid;			
			thread = pids.remove(pid);
		}
		else if(s instanceof Disnix.failure)
		{
			System.out.println("Caught failure signal!");
			int pid = ((Disnix.failure)s).pid;			
			thread = pids.remove(pid);
		}
		
		/* Resume the thread attached to the received PID */
		if(thread != null)
		{
			thread.setSource(s);
			thread.resume();
		}
	}
}
