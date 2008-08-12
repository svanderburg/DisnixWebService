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
	public String install(String file, String args, boolean isAttr);
	
	public String upgrade(String derivation);
	
	public String uninstall(String derivation);
	
	public String instantiate(String files, String attrPath);
	
	public String realise(String derivation);
	
	@DBusMemberName("import")
	public String importm(String path);
	
	public String print_invalid_paths(String path);
	
	public String collect_garbage(boolean delete_old);
	
	public String activate(String path);
	
	public String deactivate(String path);
	
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
		
		public final String path;
		
		public success(String objectpath, String pid, String path) throws DBusException
		{
			super(objectpath, pid, path);
			this.pid = pid;
			this.path = path;
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
