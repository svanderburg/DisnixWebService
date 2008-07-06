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
import org.freedesktop.dbus.*;

public abstract class DisnixThread implements Runnable
{
	protected DBusSignal source;
	private boolean suspended = false;
	
	public void setSource(DBusSignal source)
	{
		this.source = source;
	}
	
	public DBusSignal getSource()
	{
		return source;
	}
	
	public synchronized void suspend()
	{		
		suspended = true;
	}
	
	public synchronized void resume()
	{
		if(suspended)
		{
			suspended = false;
			notify();
		}
	}
	
	public synchronized void waitForNotificationToResume() throws InterruptedException
	{
		while(suspended)
			wait();
	}
	
	public abstract void run();
}
