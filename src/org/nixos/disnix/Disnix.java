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
package org.nixos.disnix;
import org.freedesktop.dbus.*;
import org.freedesktop.dbus.exceptions.*;

public interface Disnix extends DBusInterface
{
	public void acknowledge(String pid);
	
	@DBusMemberName("import")
	public String importm(String[] derivation);
	
	public String export(String[] derivation);
	
	public String print_invalid(String[] derivation);
	
	public String realise(String[] derivation);
	
	public String set(String profile, String derivation);
	
	public String query_installed(String profile);
	
	public String query_requisites(String[] derivation);
	
	public String collect_garbage(boolean delete_old);
	
	public String activate(String derivation, String type, String[] arguments);
	
	public String deactivate(String derivation, String type, String[] arguments);
	
	public String lock();
	
	public String unlock();
	
	public static class finish extends DBusSignal
	{
		public final String pid;
		
		public finish(String objectpath, String pid) throws DBusException
		{
			super(objectpath, pid);
			this.pid = pid;
		}
	}
	
	public static class success extends DBusSignal
	{
		public final String pid;
		
		public final String[] derivation;
		
		public success(String objectpath, String pid, String[] derivation) throws DBusException
		{
			super(objectpath, pid, derivation);
			this.pid = pid;
			this.derivation = derivation;
		}
	}
	
	public static class failure extends DBusSignal
	{
		public final String pid;
		
		public failure(String objectpath, String pid) throws DBusException
		{
			super(objectpath, pid);
			this.pid = pid;
		}
	}
}
