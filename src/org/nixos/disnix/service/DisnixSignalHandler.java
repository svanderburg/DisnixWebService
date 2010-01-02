/*
 * DisnixService - A SOAP layer for Disnix
 * Copyright (C) 2008  Sander van der Burg
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */
package org.nixos.disnix.service;
import java.util.*;
import org.freedesktop.dbus.*;
import org.nixos.disnix.Disnix;

public class DisnixSignalHandler implements DBusSigHandler
{
	private Hashtable<String, DisnixThread> pids;
	
	public DisnixSignalHandler()
	{
		pids = new Hashtable<String, DisnixThread>();
	}
	
	public void addPid(String pid, DisnixThread thread)
	{
		pids.put(pid, thread);
	}
	
	public void handle(DBusSignal s)
	{	
		if(s instanceof Disnix.finish)
		{
			System.out.println("Caught finish signal!");
			String pid = ((Disnix.finish)s).pid;			
			DisnixThread thread = pids.remove(pid);
			thread.setSource(s);
			thread.resume();
		}
		else if(s instanceof Disnix.success)
		{
			System.out.println("Caught success signal!");
			String pid = ((Disnix.success)s).pid;			
			DisnixThread thread = pids.remove(pid);
			thread.setSource(s);
			thread.resume();
		}
		else if(s instanceof Disnix.failure)
		{
			System.out.println("Caught failure signal!");
			String pid = ((Disnix.failure)s).pid;			
			DisnixThread thread = pids.remove(pid);
			thread.setSource(s);
			thread.resume();
		}
	}
}
