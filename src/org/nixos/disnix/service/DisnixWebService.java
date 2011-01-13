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
import org.freedesktop.dbus.*;
import org.freedesktop.dbus.exceptions.*;
import org.nixos.disnix.Disnix;
import javax.activation.*;
import java.io.*;

/**
 * Provides a SOAP interface to operations of the core Disnix service
 */
public class DisnixWebService
{
	/** Interface used to communicate to the core D-Bus service */
	private Disnix disnixInterface;
	
	/** Signal handler which waits and reacts on D-Bus signals of the Disnix core service */
	private DisnixSignalHandler handler;
	
	/** 
	 * Creates a new SOAP interface instance.
	 * 
	 * @throws DBusException If the connection with the D-Bus service fails
	 */
	public DisnixWebService() throws DBusException
	{
		handler = new DisnixSignalHandler();
		
		System.out.println("Connecting to system bus");
		DBusConnection con = DBusConnection.getConnection(DBusConnection.SYSTEM);
		
		System.out.println("Register signal handlers");
		con.addSigHandler(Disnix.finish.class, handler);			
		con.addSigHandler(Disnix.success.class, handler);
		con.addSigHandler(Disnix.failure.class, handler);
		
		System.out.println("Getting remote object");
		disnixInterface = (Disnix)con.getRemoteObject("org.nixos.disnix.Disnix", "/org/nixos/disnix/Disnix", Disnix.class);
	}
	
	/**
	 * @see org.nixos.disnix.client.DisnixInterface#importm(String)
	 */
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
	
	/**
	 * @see org.nixos.disnix.client.DisnixInterface#importLocalFile(String) 
	 */
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
	
	/**
	 * @see org.nixos.disnix.client.DisnixInterface#export(String[])
	 */
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
	
	/**
	 * @see org.nixos.disnix.client.DisnixInterface#exportRemoteFile(String[])
	 */
	public DataHandler exportRemoteFile(final String[] derivation) throws Exception
	{
		/* First, export the closure */
		String closurePath = export(derivation);
		
		/* Create and return a data handler pointing to the export */
		return new DataHandler(new FileDataSource(closurePath));		
	}
	
	/**
	 * @see org.nixos.disnix.client.DisnixInterface#printInvalid(String[])
	 */
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
	
	/**
	 * @see org.nixos.disnix.client.DisnixInterface#realise(String[])
	 */
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
	
	/**
	 * @see org.nixos.disnix.client.DisnixInterface#set(String, String)
	 */
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
	
	/**
	 * @see org.nixos.disnix.client.DisnixInterface#queryInstalled(String)
	 */
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
	
	/**
	 * @see org.nixos.disnix.client.DisnixInterface#queryRequisites(String[])
	 */
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
	
	/**
	 * @see org.nixos.disnix.client.DisnixInterface#collectGarbage(boolean)
	 */
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
	
	/**
	 * @see org.nixos.disnix.client.DisnixInterface#activate(String, String, String[])
	 */
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
	
	/**
	 * @see org.nixos.disnix.client.DisnixInterface#deactivate(String, String, String[])
	 */
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
	
	/**
	 * @see org.nixos.disnix.client.DisnixInterface#lock(String)
	 */
	public /*void*/ int lock(final String profile) throws Exception
	{
		DisnixThread disnixThread = new DisnixThread()
		{
			public void run()
			{
				try
				{
					int pid = disnixInterface.get_job_id();
					handler.addPid(pid, this);
					disnixInterface.lock(pid, profile);
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
		
		return 0;
	}
	
	/**
	 * @see org.nixos.disnix.client.DisnixInterface#unlock(String)
	 */
	public /*void*/ int unlock(final String profile) throws Exception
	{
		DisnixThread disnixThread = new DisnixThread()
		{
			public void run()
			{
				try
				{
					int pid = disnixInterface.get_job_id();
					handler.addPid(pid, this);
					disnixInterface.unlock(pid, profile);
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
		
		return 0;
	}
}
