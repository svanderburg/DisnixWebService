/* 
 * Copyright (c) 2008-2015 Sander van der Burg
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

package org.nixos.disnix;
import org.freedesktop.dbus.*;
import org.freedesktop.dbus.exceptions.*;

/**
 * An interface used to communicate with the core Disnix services through
 * the D-Bus session or system bus.
 * 
 * This interface is generated from the D-Bus interface definition of Disnix.
 */
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
	
	public void lock(int pid, String profile);
	
	public void unlock(int pid, String profile);
	
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
