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
	
	public void importm(final String[] derivation) throws Exception
	{
		DisnixThread disnixThread = new DisnixThread()
		{
			public void run()
			{
				try
				{
					String pid = disnixInterface.importm(derivation);
					handler.addPid(pid, this);
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
	}
	
	public void importLocalFile(DataHandler[] dataHandler) throws Exception
	{
		String[] tempFileName = new String[dataHandler.length];
		
		for(int i = 0; i < dataHandler.length; i++)
		{
			/* Generate temp file name */		
			File tempFile = File.createTempFile("disnix_closure_", null);
			
			/* Save file on local filesystem */
			FileOutputStream fos = new FileOutputStream(tempFile);
			dataHandler[i].writeTo(fos);
			fos.flush();
			fos.close();
			
			/* Add temp file name to array */
			tempFileName[i] = tempFile.toString();
		}
		
		/* Import the closure */
		importm(tempFileName);
	}
	
	public String export(final String[] derivation) throws Exception
	{
		DisnixThread disnixThread = new DisnixThread()
		{
			public void run()
			{
				try
				{
					String pid = disnixInterface.export(derivation);
					handler.addPid(pid, this);
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
					String pid = disnixInterface.print_invalid(derivation);
					handler.addPid(pid, this);
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
					String pid = disnixInterface.realise(derivation);
					handler.addPid(pid, this);
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
	
	public void set(final String profile, final String derivation) throws Exception
	{
		DisnixThread disnixThread = new DisnixThread()
		{
			public void run()
			{
				try
				{
					String pid = disnixInterface.set(profile, derivation);
					handler.addPid(pid, this);
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
	}
	
	public String[] queryInstalled(final String profile) throws Exception
	{
		DisnixThread disnixThread = new DisnixThread()
		{
			public void run()
			{
				try
				{
					String pid = disnixInterface.query_installed(profile);
					handler.addPid(pid, this);
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
					String pid = disnixInterface.query_requisites(derivation);
					handler.addPid(pid, this);
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
	
	public void collectGarbage(final boolean deleteOld) throws Exception
	{
		DisnixThread disnixThread = new DisnixThread()
		{
			public void run()
			{
				try
				{
					String pid = disnixInterface.collect_garbage(deleteOld);
					handler.addPid(pid, this);
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
	}
	
	public void activate(final String derivation, final String type, final String[] arguments) throws Exception
	{
		DisnixThread disnixThread = new DisnixThread()
		{
			public void run()
			{
				try
				{
					String pid = disnixInterface.activate(derivation, type, arguments);
					handler.addPid(pid, this);
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
	}
	
	public void deactivate(final String derivation, final String type, final String[] arguments) throws Exception
	{
		DisnixThread disnixThread = new DisnixThread()
		{
			public void run()
			{
				try
				{
					String pid = disnixInterface.deactivate(derivation, type, arguments);
					handler.addPid(pid, this);
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
	}
	
	public void lock() throws Exception
	{
		DisnixThread disnixThread = new DisnixThread()
		{
			public void run()
			{
				try
				{
					String pid = disnixInterface.lock();
					handler.addPid(pid, this);
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
					String pid = disnixInterface.unlock();
					handler.addPid(pid, this);
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
