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
import org.freedesktop.dbus.exceptions.*;
import org.nixos.disnix.Disnix;
import javax.activation.*;
import java.io.*;
import java.util.*;

public class DisnixService
{
	private Disnix disnixInterface;
	private DisnixSignalHandler handler;
	
	public DisnixService() throws DBusException
	{	
		handler = new DisnixSignalHandler();
		
		System.out.println("Connecting to system bus");
		DBusConnection con = DBusConnection.getConnection(DBusConnection.SYSTEM);
		
		//System.out.println("Connecting to session bus");
		//DBusConnection con = DBusConnection.getConnection(DBusConnection.SESSION);
		
		System.out.println("Register signal handlers");
		con.addSigHandler(Disnix.finish.class, handler);			
		con.addSigHandler(Disnix.success.class, handler);
		con.addSigHandler(Disnix.failure.class, handler);
		
		System.out.println("Getting remote object");
		disnixInterface = (Disnix)con.getRemoteObject("org.nixos.disnix.Disnix", "/org/nixos/disnix/Disnix", Disnix.class);
	}
	
	public /*void*/ int importm(final String closure) throws Exception
	{
		DisnixThread disnixThread = new DisnixThread()
		{
			public void run()
			{
				try
				{
					int pid = disnixInterface.get_job_id();
					handler.addPid(pid, this);
					disnixInterface.importm(pid, closure);
					suspend();
					waitForNotificationToResume();
				}
				catch(InterruptedException ex)
				{
					ex.printStackTrace();
				}
			}
		};
		Thread thread = new Thread(disnixThread);
		thread.start();
		thread.join();
		
		if(disnixThread.getSource() instanceof Disnix.failure)
			throw new Exception("Import failed!");
		
		return 0;
	}
	
	public /*void*/ int importLocalFile(DataHandler dataHandler) throws Exception
	{
		/* Generate temp file name */		
		File tempFile = File.createTempFile("disnix_closure_", null);
		
		/* Save file on local filesystem */
		FileOutputStream fos = new FileOutputStream(tempFile);
		dataHandler.writeTo(fos);
		fos.flush();
		fos.close();
		
		/* Import the closure */
		importm(tempFile.toString());
		
		return 0;
	}
	
	public String export(final String[] derivation) throws Exception
	{
		DisnixThread disnixThread = new DisnixThread()
		{
			public void run()
			{
				try
				{
					int pid = disnixInterface.get_job_id();
					handler.addPid(pid, this);
					disnixInterface.export(pid, derivation);
					suspend();
					waitForNotificationToResume();
				}
				catch(InterruptedException ex)
				{
					ex.printStackTrace();
				}
			}
		};
		Thread thread = new Thread(disnixThread);
		thread.start();
		thread.join();
		
		if(disnixThread.getSource() instanceof Disnix.failure)
			throw new Exception("Realise failed!");
		else if(disnixThread.getSource() instanceof Disnix.success)
			return ((Disnix.success)disnixThread.getSource()).derivation[0];
		else
			throw new Exception("Unknown event caught!"+disnixThread.getSource());
	}
	
	public DataHandler exportRemoteFile(final String[] derivation) throws Exception
	{
		/* First, export the closure */
		String closurePath = export(derivation);
		
		/* Create and return a data handler pointing to the export */
		return new DataHandler(new FileDataSource(closurePath));		
	}
	
	public String[] printInvalid(final String[] derivation) throws Exception
	{
		DisnixThread disnixThread = new DisnixThread()
		{
			public void run()
			{
				try
				{
					int pid = disnixInterface.get_job_id();
					handler.addPid(pid, this);
					disnixInterface.print_invalid(pid, derivation);
					suspend();
					waitForNotificationToResume();
				}
				catch(InterruptedException ex)
				{
					ex.printStackTrace();
				}
			}
		};
		Thread thread = new Thread(disnixThread);
		thread.start();
		thread.join();
		
		if(disnixThread.getSource() instanceof Disnix.failure)
			throw new Exception("Print invalid failed!");
		else if(disnixThread.getSource() instanceof Disnix.success)
			return ((Disnix.success)disnixThread.getSource()).derivation;
		else
			throw new Exception("Unknown event caught!"+disnixThread.getSource()); 
	}
	
	public String[] realise(final String[] derivation) throws Exception
	{
		DisnixThread disnixThread = new DisnixThread()
		{
			public void run()
			{
				try
				{
					int pid = disnixInterface.get_job_id();
					handler.addPid(pid, this);
					disnixInterface.realise(pid, derivation);
					suspend();
					waitForNotificationToResume();
				}
				catch(InterruptedException ex)
				{
					ex.printStackTrace();
				}
			}
		};
		Thread thread = new Thread(disnixThread);
		thread.start();
		thread.join();
		
		if(disnixThread.getSource() instanceof Disnix.failure)
			throw new Exception("Realise failed!");
		else if(disnixThread.getSource() instanceof Disnix.success)
			return ((Disnix.success)disnixThread.getSource()).derivation;
		else
			throw new Exception("Unknown event caught!"+disnixThread.getSource());
	}
	
	public /*void*/ int set(final String profile, final String derivation) throws Exception
	{
		DisnixThread disnixThread = new DisnixThread()
		{
			public void run()
			{
				try
				{
					int pid = disnixInterface.get_job_id();
					handler.addPid(pid, this);
					disnixInterface.set(pid, profile, derivation);
					suspend();
					waitForNotificationToResume();
				}
				catch(InterruptedException ex)
				{
					ex.printStackTrace();
				}
			}
		};
		Thread thread = new Thread(disnixThread);
		thread.start();
		thread.join();
		
		if(disnixThread.getSource() instanceof Disnix.failure)
			throw new Exception("Set failed!");
		
		return 0;
	}
	
	public String[] queryInstalled(final String profile) throws Exception
	{
		DisnixThread disnixThread = new DisnixThread()
		{
			public void run()
			{
				try
				{
					int pid = disnixInterface.get_job_id();
					handler.addPid(pid, this);
					disnixInterface.query_installed(pid, profile);
					suspend();
					waitForNotificationToResume();
				}
				catch(InterruptedException ex)
				{
					ex.printStackTrace();
				}
			}
		};
		Thread thread = new Thread(disnixThread);
		thread.start();
		thread.join();
		
		if(disnixThread.getSource() instanceof Disnix.failure)
			throw new Exception("Query installed failed!");
		else if(disnixThread.getSource() instanceof Disnix.success)
			return ((Disnix.success)disnixThread.getSource()).derivation;
		else
			throw new Exception("Unknown event caught!"+disnixThread.getSource());
	}
	
	public String[] queryRequisites(final String[] derivation) throws Exception
	{
		DisnixThread disnixThread = new DisnixThread()
		{
			public void run()
			{
				try
				{
					int pid = disnixInterface.get_job_id();
					handler.addPid(pid, this);
					disnixInterface.query_requisites(pid, derivation);
					suspend();
					waitForNotificationToResume();
				}
				catch(InterruptedException ex)
				{
					ex.printStackTrace();
				}
			}
		};
		Thread thread = new Thread(disnixThread);
		thread.start();
		thread.join();
		
		if(disnixThread.getSource() instanceof Disnix.failure)
			throw new Exception("Query requisites failed!");
		else if(disnixThread.getSource() instanceof Disnix.success)
			return ((Disnix.success)disnixThread.getSource()).derivation;
		else
			throw new Exception("Unknown event caught!"+disnixThread.getSource());
	}
	
	public /*void*/ int collectGarbage(final boolean deleteOld) throws Exception
	{
		DisnixThread disnixThread = new DisnixThread()
		{
			public void run()
			{
				try
				{
					int pid = disnixInterface.get_job_id();
					handler.addPid(pid, this);
					disnixInterface.collect_garbage(pid, deleteOld);
					suspend();
					waitForNotificationToResume();
				}
				catch(InterruptedException ex)
				{
					ex.printStackTrace();
				}
			}
		};
		Thread thread = new Thread(disnixThread);
		thread.start();
		thread.join();
		
		if(disnixThread.getSource() instanceof Disnix.failure)
			throw new Exception("Collect garbage failed!");
		
		return 0;
	}
	
	public /*void*/ int activate(final String derivation, final String type, final String[] arguments) throws Exception
	{
		DisnixThread disnixThread = new DisnixThread()
		{
			public void run()
			{
				try
				{
					int pid = disnixInterface.get_job_id();
					handler.addPid(pid, this);
					disnixInterface.activate(pid, derivation, type, arguments);
					suspend();
					waitForNotificationToResume();
				}
				catch(InterruptedException ex)
				{
					ex.printStackTrace();
				}
			}
		};
		Thread thread = new Thread(disnixThread);
		thread.start();
		thread.join();
		
		if(disnixThread.getSource() instanceof Disnix.failure)
			throw new Exception("Activation failed!");
		
		return 0;
	}
	
	public /*void*/ int deactivate(final String derivation, final String type, final String[] arguments) throws Exception
	{
		DisnixThread disnixThread = new DisnixThread()
		{
			public void run()
			{
				try
				{
					int pid = disnixInterface.get_job_id();
					handler.addPid(pid, this);
					disnixInterface.deactivate(pid, derivation, type, arguments);
					suspend();
					waitForNotificationToResume();
				}
				catch(InterruptedException ex)
				{
					ex.printStackTrace();
				}
			}
		};
		Thread thread = new Thread(disnixThread);
		thread.start();
		thread.join();
		
		if(disnixThread.getSource() instanceof Disnix.failure)
			throw new Exception("Deactivation failed!");
		
		return 0;
	}
	
	public void lock() throws Exception
	{
		DisnixThread disnixThread = new DisnixThread()
		{
			public void run()
			{
				try
				{
					int pid = disnixInterface.get_job_id();
					handler.addPid(pid, this);
					disnixInterface.lock(pid);
					suspend();
					waitForNotificationToResume();
				}
				catch(InterruptedException ex)
				{
					ex.printStackTrace();
				}
			}
		};
		Thread thread = new Thread(disnixThread);
		thread.start();
		thread.join();
		
		if(disnixThread.getSource() instanceof Disnix.failure)
			throw new Exception("Lock failed!");		
	}
	
	public void unlock() throws Exception
	{
		DisnixThread disnixThread = new DisnixThread()
		{
			public void run()
			{
				try
				{
					int pid = disnixInterface.get_job_id();
					handler.addPid(pid, this);
					disnixInterface.unlock(pid);
					suspend();
					waitForNotificationToResume();
				}
				catch(InterruptedException ex)
				{
					ex.printStackTrace();
				}
			}
		};
		Thread thread = new Thread(disnixThread);
		thread.start();
		thread.join();
		
		if(disnixThread.getSource() instanceof Disnix.failure)
			throw new Exception("Unlock failed!");		
	}
}
