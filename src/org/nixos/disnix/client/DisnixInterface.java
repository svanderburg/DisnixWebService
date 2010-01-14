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
package org.nixos.disnix.client;
import javax.xml.namespace.*;
import org.apache.axis2.*;
import org.apache.axis2.rpc.client.*;
import org.apache.axis2.client.*;
import org.apache.axis2.addressing.*;
import org.apache.axis2.transport.http.*;
import javax.activation.*;
import java.io.*;

public class DisnixInterface
{
	private RPCServiceClient serviceClient;
	
	private static final String NAME_SPACE = "http://service.disnix.nixos.org";
	
	private static final long TIMEOUT = 24 * 60 * 60 * 1000; // 24 hours
	
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
		Object[] args_param = { closure };
		
		serviceClient.invokeRobust(operation, args_param);
	}
	
	public void importLocalFile(String closure) throws AxisFault
	{
		DataHandler	dataHandler = new DataHandler(new FileDataSource(closure));
		
		QName operation = new QName(NAME_SPACE, "importLocalFile");
		Object[] args_param = { dataHandler };
		
		serviceClient.invokeRobust(operation, args_param);
	}
	
	public String export(String[] derivation) throws AxisFault
	{
		QName operation = new QName(NAME_SPACE, "export");
		Object[] args_param = { derivation };
		Class<?>[] returnTypes = { String[].class };
		Object[] response = serviceClient.invokeBlocking(operation, args_param, returnTypes);
		return (String)response[0];
	}
	
	public void exportRemoteFile(String[] derivation) throws AxisFault, IOException
	{
		QName operation = new QName(NAME_SPACE, "exportRemoteFile");
		Object[] args_param = { derivation };
		Class<?>[] returnTypes = { DataHandler.class };
		Object[] response = serviceClient.invokeBlocking(operation, args_param, returnTypes);
		
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
		Object[] args_param = { derivation };
		Class<?>[] returnTypes = { String[].class };
		Object[] response = serviceClient.invokeBlocking(operation, args_param, returnTypes);
		return (String[])response[0];
	}
	
	public String[] realise(String[] derivation) throws AxisFault
	{
		QName operation = new QName(NAME_SPACE, "realise");
		Object[] args_param = { derivation };
		Class<?>[] returnTypes = { String[].class };
		Object[] response = serviceClient.invokeBlocking(operation, args_param, returnTypes);
		return (String[])response[0];
	}
	
	public void set(String profile, String derivation) throws AxisFault
	{
		QName operation = new QName(NAME_SPACE, "set");
		Object[] args_param = { profile, derivation };
		
		serviceClient.invokeRobust(operation, args_param);
	}
	
	public String[] queryInstalled(String profile) throws AxisFault
	{
		QName operation = new QName(NAME_SPACE, "queryInstalled");
		Object[] args_param = { profile };
		Class<?>[] returnTypes = { String[].class };
		Object[] response = serviceClient.invokeBlocking(operation, args_param, returnTypes);
		return (String[])response[0];
	}
	
	public String[] queryRequisites(String[] derivation) throws AxisFault
	{
		QName operation = new QName(NAME_SPACE, "queryRequisites");
		Object[] args_param = { derivation };
		Class<?>[] returnTypes = { String[].class };
		Object[] response = serviceClient.invokeBlocking(operation, args_param, returnTypes);
		return (String[])response[0];
	}
	
	public void collectGarbage(boolean deleteOld) throws AxisFault
	{
		QName operation = new QName(NAME_SPACE, "collectGarbage");
		Object[] args_param = { deleteOld };
		
		serviceClient.invokeRobust(operation, args_param);
	}
	
	public void activate(String derivation, String type, String[] arguments) throws AxisFault
	{
		QName operation = new QName(NAME_SPACE, "activate");
		Object[] args_param = { derivation, type, arguments };
		
		serviceClient.invokeRobust(operation, args_param);
	}
	
	public void deactivate(String derivation, String type, String[] arguments) throws AxisFault
	{
		QName operation = new QName(NAME_SPACE, "deactivate");
		Object[] args_param = { derivation, type, arguments };
		
		serviceClient.invokeRobust(operation, args_param);
	}
}
