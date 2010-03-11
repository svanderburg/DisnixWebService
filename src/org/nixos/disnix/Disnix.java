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
	public int get_job_id();
	
	@DBusMemberName("import")
	public void importm(int pid, String closure);
	
	public void export(int pid, String[] derivation);
	
	public void print_invalid(int pid, String[] derivation);
	
	public void realise(int pid, String[] derivation);
	
	public void set(int pid, String profile, String derivation);
	
	public void query_installed(int pid, String profile);
	
	public void query_requisites(int pid, String[] derivation);
	
	public void collect_garbage(int pid, boolean delete_old);
	
	public void activate(int pid, String derivation, String type, String[] arguments);
	
	public void deactivate(int pid, String derivation, String type, String[] arguments);
	
	public int lock(int pid, String profile);
	
	public int unlock(int pid, String profile);
	
	public static class finish extends DBusSignal
	{
		public final int pid;
		
		public finish(String objectpath, int pid) throws DBusException
		{
			super(objectpath, pid);
			this.pid = pid;
		}
	}
	
	public static class success extends DBusSignal
	{
		public final int pid;
		
		public final String[] derivation;
		
		public success(String objectpath, int pid, String[] derivation) throws DBusException
		{
			super(objectpath, pid, derivation);
			this.pid = pid;
			this.derivation = derivation;
		}
	}
	
	public static class failure extends DBusSignal
	{
		public final int pid;
		
		public failure(String objectpath, int pid) throws DBusException
		{
			super(objectpath, pid);
			this.pid = pid;
		}
	}
}
