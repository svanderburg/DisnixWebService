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
package org.nixos.disnix.client;
import javax.xml.namespace.*;
import org.apache.axis2.*;
import org.apache.axis2.rpc.client.*;
import org.apache.axis2.client.*;
import org.apache.axis2.addressing.*;
import org.apache.axis2.transport.http.*;
import javax.activation.*;
import java.io.*;

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
	}
	
	public void importm(String closure) throws AxisFault
	{
		QName operation = new QName(NAME_SPACE, "importm");
		Object[] args = { closure };
		
		serviceClient.invokeRobust(operation, args);
	}
	
	public void importLocalFile(String closure) throws AxisFault
	{
		DataHandler	dataHandler = new DataHandler(new FileDataSource(closure));
		
		QName operation = new QName(NAME_SPACE, "importLocalFile");
		Object[] args = { dataHandler };
		
		serviceClient.invokeRobust(operation, args);
	}
	
	public String export(String[] derivation) throws AxisFault
	{
		QName operation = new QName(NAME_SPACE, "export");
		Object[] args = { derivation };
		Class<?>[] returnTypes = { String[].class };
		Object[] response = serviceClient.invokeBlocking(operation, args, returnTypes);
		return (String)response[0];
	}
	
	public void exportRemoteFile(String[] derivation) throws AxisFault, IOException
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
	}
	
	public String[] printInvalid(String[] derivation) throws AxisFault
	{
		QName operation = new QName(NAME_SPACE, "printInvalid");
		Object[] args = { derivation };
		Class<?>[] returnTypes = { String[].class };
		Object[] response = serviceClient.invokeBlocking(operation, args, returnTypes);
		return (String[])response[0];
	}
	
	public String[] realise(String[] derivation) throws AxisFault
	{
		QName operation = new QName(NAME_SPACE, "realise");
		Object[] args = { derivation };
		Class<?>[] returnTypes = { String[].class };
		Object[] response = serviceClient.invokeBlocking(operation, args, returnTypes);
		return (String[])response[0];
	}
	
	public void set(String profile, String derivation) throws AxisFault
	{
		QName operation = new QName(NAME_SPACE, "set");
		Object[] args = { profile, derivation };
		
		serviceClient.invokeRobust(operation, args);
	}
	
	public String[] queryInstalled(String profile) throws AxisFault
	{
		QName operation = new QName(NAME_SPACE, "queryInstalled");
		Object[] args = { profile };
		Class<?>[] returnTypes = { String[].class };
		Object[] response = serviceClient.invokeBlocking(operation, args, returnTypes);
		return (String[])response[0];
	}
	
	public String[] queryRequisites(String[] derivation) throws AxisFault
	{
		QName operation = new QName(NAME_SPACE, "queryRequisites");
		Object[] args = { derivation };
		Class<?>[] returnTypes = { String[].class };
		Object[] response = serviceClient.invokeBlocking(operation, args, returnTypes);
		return (String[])response[0];
	}
	
	public void collectGarbage(boolean deleteOld) throws AxisFault
	{
		QName operation = new QName(NAME_SPACE, "collectGarbage");
		Object[] args = { deleteOld };
		
		serviceClient.invokeRobust(operation, args);
	}
	
	public void activate(String derivation, String type, String[] arguments) throws AxisFault
	{
		QName operation = new QName(NAME_SPACE, "activate");
		Object[] args = { derivation, type, arguments };
		
		serviceClient.invokeRobust(operation, args);
	}
	
	public void deactivate(String derivation, String type, String[] arguments) throws AxisFault
	{
		QName operation = new QName(NAME_SPACE, "deactivate");
		Object[] args = { derivation, type, arguments };
		
		serviceClient.invokeRobust(operation, args);
	}
	
	public void lock(String profile) throws AxisFault
	{
		QName operation = new QName(NAME_SPACE, "lock");
		Object[] args = { profile };
		
		serviceClient.invokeRobust(operation, args);
	}
	
	public void unlock(String profile) throws AxisFault
	{
		QName operation = new QName(NAME_SPACE, "unlock");
		Object[] args = { profile };
		
		serviceClient.invokeRobust(operation, args);
	}
}
