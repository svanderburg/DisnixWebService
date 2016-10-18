/* 
 * Copyright (c) 2008-2016 Sander van der Burg
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
package org.nixos.disnix.client;
import javax.xml.namespace.*;
import javax.activation.*;
import java.io.*;
import java.util.*;

import org.apache.axis2.*;
import org.apache.axis2.rpc.client.*;
import org.apache.axis2.client.*;
import org.apache.axis2.addressing.*;
import org.apache.axis2.transport.http.*;

/**
 * Provides a SOAP client interface to the Disnix Service.
 */
public class DisnixInterface
{
	/** Provides an RPC interface to the Disnix Service */
	private RPCServiceClient serviceClient;
	
	/** Name space used by Disnix operations */
	private static final String NAME_SPACE = "http://service.disnix.nixos.org";
	
	/** Time-out value of SOAP RPC calls */
	private static final long TIMEOUT = 24 * 60 * 60 * 1000; // 24 hours
	
	/**
	 * Creates a new Disnix interface instance.
	 * 
	 * @param serviceURL URL of the Disnix Service
	 * @throws AxisFault If creating a RPC service client fails
	 */
	public DisnixInterface(String serviceURL) throws AxisFault
	{
		serviceClient = new RPCServiceClient();
		Options options = serviceClient.getOptions();
		EndpointReference targetEPR = new EndpointReference(serviceURL);
		options.setTo(targetEPR);
		/* Timeout */
		options.setTimeOutInMilliSeconds(TIMEOUT);
		/* MTOM settings */
		options.setProperty(Constants.Configuration.ENABLE_MTOM, Constants.VALUE_TRUE);
		options.setProperty(Constants.Configuration.CACHE_ATTACHMENTS, Constants.VALUE_TRUE);
		options.setProperty(Constants.Configuration.ATTACHMENT_TEMP_DIR, "/tmp");
		options.setProperty(Constants.Configuration.FILE_SIZE_THRESHOLD, "4000");		
		/* Transport settings */
		options.setTransportInProtocol(Constants.TRANSPORT_HTTP);
		options.setProperty(HTTPConstants.MC_GZIP_REQUEST, Boolean.TRUE);
		options.setProperty(HTTPConstants.MC_ACCEPT_GZIP, Boolean.TRUE);
		
		if(System.getenv("DISNIX_SOAP_CLIENT_USERNAME") != null)
		{
			/* Authentication settings */
			HttpTransportProperties.Authenticator auth = new HttpTransportProperties.Authenticator();
			auth.setUsername(System.getenv("DISNIX_SOAP_CLIENT_USERNAME"));
			auth.setPassword(System.getenv("DISNIX_SOAP_CLIENT_PASSWORD"));
			options.setProperty(HTTPConstants.AUTHENTICATE, auth);
		}
	}
	
	/**
	 * Imports a closure into the the Nix store
	 * 
	 * @param closure Path on the server to a file containing the closure
	 * @throws AxisFault If an error occurs with the transport
	 */
	public void importm(String closure) throws AxisFault
	{
		try
		{
			QName operation = new QName(NAME_SPACE, "importm");
			Object[] args = { closure };
			
			serviceClient.invokeRobust(operation, args);
		}
		catch(AxisFault ex)
		{
			throw ex;
		}
		finally
		{
			serviceClient.cleanup();
			serviceClient.cleanupTransport();
		}
	}
	
	/**
	 * First transfers a closure from the client to the service
	 * and then imports the closure into the Nix store.
	 * 
	 * @param closure Path on the client to a file containing the closure
	 * @throws AxisFault If an error occurs with the transport
	 */
	public void importLocalFile(String closure) throws AxisFault
	{
		try
		{
			DataHandler	dataHandler = new DataHandler(new FileDataSource(closure));
			
			QName operation = new QName(NAME_SPACE, "importLocalFile");
			Object[] args = { dataHandler };
			
			serviceClient.invokeRobust(operation, args);
		}
		catch(AxisFault ex)
		{
			throw ex;
		}
		finally
		{
			serviceClient.cleanup();
			serviceClient.cleanupTransport();
		}
	}
	
	/**
	 * Exports derivations into a file containing the closure
	 * 
	 * @param derivation Path to the Nix store component om the server
	 * @return Path to the file containing the closure
	 * @throws AxisFault If an error occurs with the transport
	 */
	public String export(String[] derivation) throws AxisFault
	{
		try
		{
			QName operation = new QName(NAME_SPACE, "export");
			Object[] args = { derivation };
			Class<?>[] returnTypes = { String.class };
			Object[] response = serviceClient.invokeBlocking(operation, args, returnTypes);
			return (String)response[0];
		}
		catch(AxisFault ex)
		{
			throw ex;
		}
		finally
		{
			serviceClient.cleanup();
			serviceClient.cleanupTransport();
		}
	}
	
	/**
	 * Exports derivations into a file containing the closure and downloads
	 * it from the client.
	 * 
	 * @param derivation Path to the Nix store component on the server
	 * @return Path to the temp dir in which the downloaded snapshot is stored
	 * @throws AxisFault If an error occurs with the transport
	 * @throws IOException If an error occurs with downloading the file
	 */
	public String exportRemoteFile(String[] derivation) throws AxisFault, IOException
	{
		try
		{
			QName operation = new QName(NAME_SPACE, "exportRemoteFile");
			Object[] args = { derivation };
			Class<?>[] returnTypes = { DataHandler.class };
			Object[] response = serviceClient.invokeBlocking(operation, args, returnTypes);
			
			/* Retrieve the data handler */
			DataHandler dataHandler = (DataHandler)response[0];
			
			/* Generate temp file name */
			File tempFile = File.createTempFile("disnix_closure_", null);
			
			/* Save file on local filesystem */
			FileOutputStream fos = new FileOutputStream(tempFile);
			dataHandler.writeTo(fos);
			fos.flush();
			fos.close();
			
			return tempFile.getAbsolutePath();
		}
		catch(AxisFault ex)
		{
			throw ex;
		}
		finally
		{
			serviceClient.cleanup();
			serviceClient.cleanupTransport();
		}
	}
	
	/**
	 * Returns the Nix store paths which are not valid.
	 * 
	 * @param derivation Array of Nix store paths
	 * @return Array of paths which are invalid
	 * @throws AxisFault If an error occurs with the transport
	 */
	public String[] printInvalid(String[] derivation) throws AxisFault
	{
		try
		{
			QName operation = new QName(NAME_SPACE, "printInvalid");
			Object[] args = { derivation };
			Class<?>[] returnTypes = { String[].class };
			Object[] response = serviceClient.invokeBlocking(operation, args, returnTypes);
			return (String[])response[0];
		}
		catch(AxisFault ex)
		{
			throw ex;
		}
		finally
		{
			serviceClient.cleanup();
			serviceClient.cleanupTransport();
		}
	}
	
	/**
	 * Realises the given Nix store derivations.
	 * 
	 * @param derivation An array of Nix store paths to store derivations
	 * @return Array of paths containing the build results
	 * @throws AxisFault If an error occurs with the transport
	 */
	public String[] realise(String[] derivation) throws AxisFault
	{
		try
		{
			QName operation = new QName(NAME_SPACE, "realise");
			Object[] args = { derivation };
			Class<?>[] returnTypes = { String[].class };
			Object[] response = serviceClient.invokeBlocking(operation, args, returnTypes);
			return (String[])response[0];
		}
		catch(AxisFault ex)
		{
			throw ex;
		}
		finally
		{
			serviceClient.cleanup();
			serviceClient.cleanupTransport();
		}
	}
	
	/**
	 * Sets the profile containing the given derivation.
	 * 
	 * @param profile Name of the profile
	 * @param derivation Path to a derivation
	 * @throws AxisFault If an error occurs with the transport
	 */
	public void set(String profile, String derivation) throws AxisFault
	{
		try
		{
			QName operation = new QName(NAME_SPACE, "set");
			Object[] args = { profile, derivation };
			
			serviceClient.invokeRobust(operation, args);
		}
		catch(AxisFault ex)
		{
			throw ex;
		}
		finally
		{
			serviceClient.cleanup();
			serviceClient.cleanupTransport();
		}
	}
	
	/**
	 * Queries the Nix store paths of the installed components in
	 * the given profile.
	 * 
	 * @param profile Name of the profile
	 * @return Array of Nix store components
	 * @throws AxisFault If an error occurs with the transport
	 */
	public String[] queryInstalled(String profile) throws AxisFault
	{
		try
		{
			QName operation = new QName(NAME_SPACE, "queryInstalled");
			Object[] args = { profile };
			Class<?>[] returnTypes = { String[].class };
			Object[] response = serviceClient.invokeBlocking(operation, args, returnTypes);
			return (String[])response[0];
		}
		catch(AxisFault ex)
		{
			throw ex;
		}
		finally
		{
			serviceClient.cleanup();
			serviceClient.cleanupTransport();
		}
	}
	
	/**
	 * Queries the requisites of the given derivations.
	 * 
	 * @param derivation Array of Nix store components
	 * @return Array of requisites of the given Nix store components
	 * @throws AxisFault If an error occurs with the transport
	 */
	public String[] queryRequisites(String[] derivation) throws AxisFault
	{
		try
		{
			QName operation = new QName(NAME_SPACE, "queryRequisites");
			Object[] args = { derivation };
			Class<?>[] returnTypes = { String[].class };
			Object[] response = serviceClient.invokeBlocking(operation, args, returnTypes);
			return (String[])response[0];
		}
		catch(AxisFault ex)
		{
			throw ex;
		}
		finally
		{
			serviceClient.cleanup();
			serviceClient.cleanupTransport();
		}
	}
	
	/**
	 * Removes obsolete Nix components.
	 * 
	 * @param deleteOld Inidicated whether to delete old generation profiles
	 * @throws AxisFault If an error occurs with the transport
	 */
	public void collectGarbage(boolean deleteOld) throws AxisFault
	{
		try
		{
			QName operation = new QName(NAME_SPACE, "collectGarbage");
			Object[] args = { deleteOld };
			
			serviceClient.invokeRobust(operation, args);
		}
		catch(AxisFault ex)
		{
			throw ex;
		}
		finally
		{
			serviceClient.cleanup();
			serviceClient.cleanupTransport();
		}
	}
	
	/**
	 * Activates a given service.
	 * 
	 * @param derivation Nix store path of the service
	 * @param container Name of the container to which a component is deployed
	 * @param type Type identifier of the service
	 * @param arguments Array of key=value pairs containing environment variables for the activation scripts
	 * @throws AxisFault If an error occurs with the transport
	 */
	public void activate(String derivation, String container, String type, String[] arguments) throws AxisFault
	{
		try
		{
			QName operation = new QName(NAME_SPACE, "activate");
			Object[] args = { derivation, container, type, arguments };
			
			serviceClient.invokeRobust(operation, args);
		}
		catch(AxisFault ex)
		{
			throw ex;
		}
		finally
		{
			serviceClient.cleanup();
			serviceClient.cleanupTransport();
		}
	}
	
	/**
	 * Deactivates a given service.
	 * 
	 * @param derivation Nix store path of the service
	 * @param container Name of the container to which a component is deployed
	 * @param type Type identifier of the service
	 * @param arguments Array of key=value pairs containing environment variables for the activation scripts
	 * @throws AxisFault If an error occurs with the transport
	 */
	public void deactivate(String derivation, String container, String type, String[] arguments) throws AxisFault
	{
		try
		{
			QName operation = new QName(NAME_SPACE, "deactivate");
			Object[] args = { derivation, container, type, arguments };
			
			serviceClient.invokeRobust(operation, args);
		}
		catch(AxisFault ex)
		{
			throw ex;
		}
		finally
		{
			serviceClient.cleanup();
			serviceClient.cleanupTransport();
		}
	}
	
	/**
	 * Acquires a lock on the given profile
	 * 
	 * @param profile Name of the profile
	 * @throws AxisFault If an error occurs with the transport
	 */
	public void lock(String profile) throws AxisFault
	{
		try
		{
			QName operation = new QName(NAME_SPACE, "lock");
			Object[] args = { profile };
			
			serviceClient.invokeRobust(operation, args);
		}
		catch(AxisFault ex)
		{
			throw ex;
		}
		finally
		{
			serviceClient.cleanup();
			serviceClient.cleanupTransport();
		}
	}
	
	/**
	 * Releases the lock on the given profile
	 * 
	 * @param profile Name of the profile
	 * @throws AxisFault If an error occurs with the transport
	 */
	public void unlock(String profile) throws AxisFault
	{
		try
		{
			QName operation = new QName(NAME_SPACE, "unlock");
			Object[] args = { profile };
			
			serviceClient.invokeRobust(operation, args);
		}
		catch(AxisFault ex)
		{
			throw ex;
		}
		finally
		{
			serviceClient.cleanup();
			serviceClient.cleanupTransport();
		}
	}
	
	/**
	 * Deletes the state of a component.
	 * 
	 * @param derivation Nix store path to or name of the component
	 * @param container Name of the container to which a component is deployed
	 * @param type Type identifier of the service
	 * @param arguments Array of key=value pairs containing environment variables for the activation scripts
	 * @throws AxisFault If an error occurs with the transport
	 */
	public void deleteState(String derivation, String container, String type, String[] arguments) throws AxisFault
	{
		try
		{
			QName operation = new QName(NAME_SPACE, "deleteState");
			Object[] args = { derivation, container, type, arguments };
			
			serviceClient.invokeRobust(operation, args);
		}
		catch(AxisFault ex)
		{
			throw ex;
		}
		finally
		{
			serviceClient.cleanup();
			serviceClient.cleanupTransport();
		}
	}
	
	/**
	 * Snapshots the state of a component
	 * 
	 * @param derivation Nix store path to or name of the component
	 * @param container Name of the container to which a component is deployed
	 * @param type Type identifier of the service
	 * @param arguments Array of key=value pairs containing environment variables for the activation scripts
	 * @throws AxisFault If an error occurs with the transport
	 */
	public void snapshot(String derivation, String container, String type, String[] arguments) throws AxisFault
	{
		try
		{
			QName operation = new QName(NAME_SPACE, "snapshot");
			Object[] args = { derivation, container, type, arguments };
			
			serviceClient.invokeRobust(operation, args);
		}
		catch(AxisFault ex)
		{
			throw ex;
		}
		finally
		{
			serviceClient.cleanup();
			serviceClient.cleanupTransport();
		}
	}
	
	/**
	 * Restores the state of a component.
	 * 
	 * @param derivation Nix store path to or name of the component
	 * @param container Name of the container to which a component is deployed
	 * @param type Type identifier of the service
	 * @param arguments Array of key=value pairs containing environment variables for the activation scripts
	 * @throws AxisFault If an error occurs with the transport
	 */
	public void restore(String derivation, String container, String type, String[] arguments) throws AxisFault
	{
		try
		{
			QName operation = new QName(NAME_SPACE, "restore");
			Object[] args = { derivation, container, type, arguments };
			
			serviceClient.invokeRobust(operation, args);
		}
		catch(AxisFault ex)
		{
			throw ex;
		}
		finally
		{
			serviceClient.cleanup();
			serviceClient.cleanupTransport();
		}
	}
	
	/**
	 * Queries the paths to the all snapshots taken of a component deployed to a container.
	 * 
	 * @param container Name of the container to which a component is deployed
	 * @param component Name of the deployed component
	 * @return An array of relative paths to the snapshots
	 * @throws AxisFault If an error occurs with the transport
	 */
	public String[] queryAllSnapshots(String container, String component) throws AxisFault
	{
		try
		{
			QName operation = new QName(NAME_SPACE, "queryAllSnapshots");
			Object[] args = { container, component };
			Class<?>[] returnTypes = { String[].class };
			Object[] response = serviceClient.invokeBlocking(operation, args, returnTypes);
			return (String[])response[0];
		}
		catch(AxisFault ex)
		{
			throw ex;
		}
		finally
		{
			serviceClient.cleanup();
			serviceClient.cleanupTransport();
		}
	}
	
	/**
	 * Queries the path to the last snapshot taken of a component deployed to a container.
	 * 
	 * @param container Name of the container to which a component is deployed
	 * @param component Name of the deployed component
	 * @return An array of relative paths to the snapshots
	 * @throws AxisFault If an error occurs with the transport
	 */
	public String[] queryLatestSnapshot(String container, String component) throws AxisFault
	{
		try
		{
			QName operation = new QName(NAME_SPACE, "queryLatestSnapshot");
			Object[] args = { container, component };
			Class<?>[] returnTypes = { String[].class };
			Object[] response = serviceClient.invokeBlocking(operation, args, returnTypes);
			return (String[])response[0];
		}
		catch(AxisFault ex)
		{
			throw ex;
		}
		finally
		{
			serviceClient.cleanup();
			serviceClient.cleanupTransport();
		}
	}
	
	/**
	 * Prints the absolute paths of the snapshots that are missing on the target machine.
	 * 
	 * @param component An array of snapshot paths of a component
	 * @return All snapshots that are missing
	 * @throws AxisFault If an error occurs with the transport
	 */
	public String[] printMissingSnapshots(String[] component) throws AxisFault
	{
		try
		{
			QName operation = new QName(NAME_SPACE, "printMissingSnapshots");
			Object[] args = { component };
			Class<?>[] returnTypes = { String[].class };
			Object[] response = serviceClient.invokeBlocking(operation, args, returnTypes);
			return (String[])response[0];
		}
		catch(AxisFault ex)
		{
			throw ex;
		}
		finally
		{
			serviceClient.cleanup();
			serviceClient.cleanupTransport();
		}
	}
	
	/**
	 * Imports snapshots into the snapshot store for a specific component deployed to a container.
	 * 
	 * @param container Name of the container to which a component is deployed
	 * @param component Name of the deployed component
	 * @param snapshots An array of paths to snapshots that must be imported
	 * @throws AxisFault If an error occurs with the transport
	 */
	public void importSnapshots(String container, String component, String[] snapshots) throws AxisFault
	{
		try
		{
			QName operation = new QName(NAME_SPACE, "importSnapshots");
			Object[] args = { container, component, snapshots };
			
			serviceClient.invokeRobust(operation, args);
		}
		catch(AxisFault ex)
		{
			throw ex;
		}
		finally
		{
			serviceClient.cleanup();
			serviceClient.cleanupTransport();
		}
	}
	
	private static void populatePathsVector(File[] paths, Vector<String> files)
	{
		for(File path : paths)
		{
			if(path.isDirectory())
			{
				File[] subPaths = path.listFiles();
				populatePathsVector(subPaths, files);
			}
			else
				files.add(path.getAbsolutePath());
		}
	}
	
	/**
	 * Transfers a set of snapshots to the target machine and imports them into the remote
	 * snapshot store.
	 * 
	 * @param container Name of the container to which a component is deployed
	 * @param component Name of the deployed component
	 * @param snapshot Path to a snapshot
	 * @throws AxisFault If an error occurs with the transport
	 */
	public void importLocalSnapshots(String container, String component, String snapshot) throws AxisFault
	{
		try
		{	
			Vector<String> pathsVector = new Vector<String>();
			populatePathsVector(new File[] { new File(snapshot) }, pathsVector);
			
			DataHandler[] dataHandlers = new DataHandler[pathsVector.size()];
			String[] paths = new String[pathsVector.size()];
			pathsVector.toArray(paths);
			
			for(int i = 0; i < dataHandlers.length; i++)
				dataHandlers[i] = new DataHandler(new FileDataSource(paths[i]));
			
			QName operation = new QName(NAME_SPACE, "importLocalSnapshots");
			Object[] args = { container, component, snapshot, dataHandlers, paths };
			
			serviceClient.invokeRobust(operation, args);
		}
		catch(AxisFault ex)
		{
			throw ex;
		}
		finally
		{
			serviceClient.cleanup();
			serviceClient.cleanupTransport();
		}
	}
	
	/**
	 * Retrieves a set of snapshots from the remote machine.
	 * 
	 * @param snapshots Absolute paths to the snapshots to retrieve
	 * @return Path to a temp directory containing the retrieved snapshots
	 * @throws AxisFault If an error occurs with the transport
	 * @throws IOException If an error occurs with writing the snapshot
	 * @throws FileNotFoundException If the file cannot be found
	 */
	public String exportRemoteSnapshots(String[] snapshots) throws AxisFault, IOException, FileNotFoundException
	{
		try
		{	
			File tempDir = File.createTempFile("disnix_snapshot_", null);
			tempDir.delete();
			tempDir.mkdir();

			for(int i = 0; i < snapshots.length; i++)
			{
				/* Retrieves the absolute paths to the snapshots */
				String[] paths;
				
				{
					QName operation = new QName(NAME_SPACE, "exportRemoteSnapshotPaths");
					Object[] args = { snapshots[i] };
					Class<?>[] returnTypes = { String[].class };
					Object[] response = serviceClient.invokeBlocking(operation, args, returnTypes);
					paths = (String[])response[0];
				}
				
				/* Fetch the snapshots themselves */
				DataHandler[] dataHandlers;
				
				{
					QName operation = new QName(NAME_SPACE, "exportRemoteSnapshots");
					Object[] args = { snapshots[i] };
					Class<?>[] returnTypes = { DataHandler[].class };
					Object[] response = serviceClient.invokeBlocking(operation, args, returnTypes);
					dataHandlers = (DataHandler[])response[0];
				}	
			
				String relativePath = paths[i].substring(snapshots[i].length());
				String snapshotName = new File(snapshots[i]).getName();
				File targetFile = new File(tempDir.getAbsolutePath() + File.separator + snapshotName + File.separator + relativePath);
				
				if(targetFile.getParentFile() != null) // If the file is part of a directory then create it first
					targetFile.getParentFile().mkdirs();
				
				FileOutputStream fos = new FileOutputStream(targetFile);
				dataHandlers[i].writeTo(fos);
				fos.flush();
				fos.close();
			}
			
			/* Return the path to the tempdir containing the retrieved snapshot */
			return tempDir.getAbsolutePath();
		}
		catch(AxisFault ex)
		{
			throw ex;
		}
		finally
		{
			serviceClient.cleanup();
			serviceClient.cleanupTransport();
		}
	}
	
	/**
	 * Resolves the absolute paths of a set of snapshots from their relative paths.
	 * 
	 * @param snapshots An array of relative paths to snapshots
	 * @return An array of absolute paths
	 * @throws AxisFault If an error occurs with the transport
	 */
	public String[] resolveSnapshots(String[] snapshots) throws AxisFault
	{
		try
		{
			QName operation = new QName(NAME_SPACE, "resolveSnapshots");
			Object[] args = { snapshots };
			Class<?>[] returnTypes = { String[].class };
			Object[] response = serviceClient.invokeBlocking(operation, args, returnTypes);
			return (String[])response[0];
		}
		catch(AxisFault ex)
		{
			throw ex;
		}
		finally
		{
			serviceClient.cleanup();
			serviceClient.cleanupTransport();
		}
	}
	
	/**
	 * Cleans all older generation snapshots from the remote machine.
	 * 
	 * @param keep Amount of snapshot generations to keep
	 * @param container Name of the container to filter on or null to consult all containers
	 * @param component Name of the component to filter on or null to consult all components
	 * @throws AxisFault If an error occurs with the transport
	 */
	public void cleanSnapshots(int keep, String container, String component) throws AxisFault
	{
		try
		{
			QName operation = new QName(NAME_SPACE, "cleanSnapshots");
			Object[] args = { keep, container, component };
			
			serviceClient.invokeRobust(operation, args);
		}
		catch(AxisFault ex)
		{
			throw ex;
		}
		finally
		{
			serviceClient.cleanup();
			serviceClient.cleanupTransport();
		}
	}
	
	public String captureConfig() throws AxisFault
	{
		try
		{
			QName operation = new QName(NAME_SPACE, "captureConfig");
			Object[] args = {};
			Class<?>[] returnTypes = { String.class };
			Object[] response = serviceClient.invokeBlocking(operation, args, returnTypes);
			return (String)response[0];
		}
		catch(AxisFault ex)
		{
			throw ex;
		}
		finally
		{
			serviceClient.cleanup();
			serviceClient.cleanupTransport();
		}
	}
}
