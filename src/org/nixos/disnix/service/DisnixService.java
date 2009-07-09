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
		System.out.println("DisnixService constructor");
		
		handler = new DisnixSignalHandler();
		
		System.out.println("connecting to session bus");
		//DBusConnection con = DBusConnection.getConnection(DBusConnection.SESSION);
		DBusConnection con = DBusConnection.getConnection(DBusConnection.SYSTEM);
		
		System.out.println("register signal handlers");
		con.addSigHandler(Disnix.finish.class, handler);			
		con.addSigHandler(Disnix.success.class, handler);
		con.addSigHandler(Disnix.failure.class, handler);
		
		System.out.println("Getting remote object");
		disnixInterface = (Disnix)con.getRemoteObject("org.nixos.disnix.Disnix", "/org/nixos/disnix/Disnix", Disnix.class);
	}
	
	public void install(final String file, final String args, final boolean isAttr) throws Exception
	{
		DisnixThread disnixThread = new DisnixThread()
		{
			public void run()
			{
				try
				{
					String pid = disnixInterface.install(file, args, isAttr);
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
			throw new Exception("Installation failed!");
	}
	
	public void upgrade(final String derivation) throws Exception
	{
		DisnixThread disnixThread = new DisnixThread()
		{
			public void run()
			{
				try
				{
					String pid = disnixInterface.upgrade(derivation);
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
			throw new Exception("Installation failed!");
	}
	
	public void uninstall(final String derivation) throws Exception
	{
		DisnixThread disnixThread = new DisnixThread()
		{
			public void run()
			{
				try
				{
					String pid = disnixInterface.uninstall(derivation);
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
			throw new Exception("Installation failed!");
	}
	
	public String[] instantiate(final String files, final String attrPath) throws Exception
	{
		DisnixThread disnixThread = new DisnixThread()
		{
			public void run()
			{
				try
				{
					String pid = disnixInterface.instantiate(files, attrPath);
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
			throw new Exception("Installation failed!");
		else if(disnixThread.getSource() instanceof Disnix.success)
		{
			String retPath = ((Disnix.success)disnixThread.getSource()).path;
			StringTokenizer tokenizer = new StringTokenizer(retPath, "\n");
			String[] ret = new String[tokenizer.countTokens()];
			int count = 0;
			
			while(tokenizer.hasMoreTokens())
			{
				ret[count] = tokenizer.nextToken();
				count++;
			}
			
			return ret;
		}
		else
			throw new Exception("Unknown event caught!"+disnixThread.getSource()); 
	}
	
	public String[] instantiateExpression(DataHandler dataHandler, String attrPath) throws Exception
	{
		/* Generate temp file name */		
		File tempFile = File.createTempFile("disnix_nixexpr_", null);
		
		/* Save file on local filesystem */
		FileOutputStream fos = new FileOutputStream(tempFile);
		dataHandler.writeTo(fos);
		fos.flush();
		fos.close();
		
		/* Instantiate the expression */
		return instantiate(tempFile.toString(), attrPath);
	}
	
	public String realise(final String derivation) throws Exception
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
			throw new Exception("Installation failed!");
		else if(disnixThread.getSource() instanceof Disnix.success)
			return ((Disnix.success)disnixThread.getSource()).path;
		else
			throw new Exception("Unknown event caught!"+disnixThread.getSource()); 
	}
	
	public void importm(final String path) throws Exception
	{
		DisnixThread disnixThread = new DisnixThread()
		{
			public void run()
			{
				try
				{
					String pid = disnixInterface.importm(path);
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
			throw new Exception("Installation failed!");
	}
	
	public void importClosure(DataHandler dataHandler) throws Exception
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
	}
	
	public String[] printInvalidPaths(final String path) throws Exception
	{
		DisnixThread disnixThread = new DisnixThread()
		{
			public void run()
			{
				try
				{
					String pid = disnixInterface.print_invalid_paths(path);
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
			throw new Exception("Installation failed!");
		else if(disnixThread.getSource() instanceof Disnix.success)
		{
			String retPath = ((Disnix.success)disnixThread.getSource()).path;
			StringTokenizer tokenizer = new StringTokenizer(retPath, "\n");
			String[] ret = new String[tokenizer.countTokens()];
			int count = 0;
			
			while(tokenizer.hasMoreTokens())
			{
				ret[count] = tokenizer.nextToken();
				count++;
			}
			
			return ret;
		}
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
			throw new Exception("Installation failed!");
	}
	
	public void activate(final String path, final String type) throws Exception
	{
		DisnixThread disnixThread = new DisnixThread()
		{
			public void run()
			{
				try
				{
					String pid = disnixInterface.activate(path, type);
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
			throw new Exception("Installation failed!");		
	}
	
	public void deactivate(final String path, final String type) throws Exception
	{
		DisnixThread disnixThread = new DisnixThread()
		{
			public void run()
			{
				try
				{
					String pid = disnixInterface.deactivate(path, type);
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
			throw new Exception("Installation failed!");		
	}
}
