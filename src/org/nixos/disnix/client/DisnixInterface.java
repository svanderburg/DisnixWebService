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
	
	public void install(String file, String args, boolean isAttr) throws AxisFault
	{
		QName operation = new QName(NAME_SPACE, "install");
		Object[] args_param = new Object[] { file, args, isAttr };
		
		serviceClient.invokeRobust(operation, args_param);
	}
	
	public void upgrade(String derivation) throws AxisFault
	{
		QName operation = new QName(NAME_SPACE, "upgrade");
		Object[] args_param = new Object[] { derivation };
		
		serviceClient.invokeRobust(operation, args_param);
	}
	
	public void uninstall(String derivation) throws AxisFault
	{
		QName operation = new QName(NAME_SPACE, "uninstall");
		Object[] args_param = new Object[] { derivation };
		
		serviceClient.invokeRobust(operation, args_param);
	}
	
	public String[] instantiate(String files, String attrPath) throws AxisFault
	{
		QName operation = new QName(NAME_SPACE, "instantiate");
		Object[] args_param = new Object[] { files, attrPath };
		Class<?>[] returnTypes = new Class[] { String[].class };
		Object[] response = serviceClient.invokeBlocking(operation, args_param, returnTypes);
		return (String[])response[0];
	}
	
	public String[] instantiateExpression(String file, String attrPath) throws AxisFault
	{
		DataHandler dataHandler = new DataHandler(new FileDataSource(file));
		QName operation = new QName(NAME_SPACE, "instantiateExpression");
		Object[] args_param = new Object[] { dataHandler, attrPath };
		Class<?>[] returnTypes = new Class[] { String[].class };
		Object[] response = serviceClient.invokeBlocking(operation, args_param, returnTypes);
		return (String[])response[0];
	}
	
	public String realise(String derivation) throws AxisFault
	{
		QName operation = new QName(NAME_SPACE, "realise");
		Object[] args_param = new Object[] { derivation };
		Class<?>[] returnTypes = new Class[] { String.class };
		Object[] response = serviceClient.invokeBlocking(operation, args_param, returnTypes);
		return (String)response[0];
	}
	
	public void importm(String path) throws AxisFault
	{
		QName operation = new QName(NAME_SPACE, "importm");
		Object[] args_param = new Object[] { path };
		
		serviceClient.invokeRobust(operation, args_param);
	}
	
	public void importClosure(String path) throws AxisFault
	{
		DataHandler dataHandler = new DataHandler(new FileDataSource(path));
		
		QName operation = new QName(NAME_SPACE, "importClosure");
		Object[] args_param = new Object[] { dataHandler };
		
		serviceClient.invokeRobust(operation, args_param);
	}
	
	public String[] printInvalidPaths(String path) throws AxisFault
	{
		QName operation = new QName(NAME_SPACE, "printInvalidPaths");
		Object[] args_param = new Object[] { path };
		Class<?>[] returnTypes = new Class[] { String[].class };
		Object[] response = serviceClient.invokeBlocking(operation, args_param, returnTypes);
		return (String[])response[0];
	}
	
	public void collectGarbage(boolean deleteOld) throws AxisFault
	{
		QName operation = new QName(NAME_SPACE, "collectGarbage");
		Object[] args_param = new Object[] { deleteOld };
		
		serviceClient.invokeRobust(operation, args_param);
	}
}
