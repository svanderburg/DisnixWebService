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
import java.util.*;

public class DisnixCopyClosure
{
	public static void copyClosure(String path, DisnixInterface disnixInterface) throws Exception
	{	
		/* Determine real store path */
		
		String storePath = NixInterface.getInstance().queryResolve(path);
		
		/* Get the closure of the store path */
		System.out.println("Getting the store path closure");
		
		Stack<String> allStorePaths = NixInterface.getInstance().queryRequisites(storePath);
		
		/* Get all invalid paths of the closure from the target machine */
		
		String paths = "";
		String pathelem = null;
		
		while(!allStorePaths.empty())
		{
			pathelem = allStorePaths.pop();
			paths += " " + pathelem;
		}
		
		String[] invalidPaths = disnixInterface.printInvalidPaths(paths);
		
		/* Serialise the missing paths */
		
		NixInterface.getInstance().export(invalidPaths);
					
		if(invalidPaths.length > 0)
		{
			/* Import serialisation in remote store */			
			disnixInterface.importClosure("/tmp/out.closure");
		}
	}
	
	public static void main(String[] args) throws Exception
	{
		/* Get command line options */
		
		if(args.length != 2)
		{
			System.out.println("Usage:");
			System.out.println("disnix-soap-copy-closure path targetEPR");
			System.exit(1);
		}
		
		String path = args[0];
		String targetEPR = args[1];

		/* Connect to the SOAP interface */
		System.out.println("Connecting to target endpoint reference");
		DisnixInterface disnixInterface = new DisnixInterface(targetEPR);
		
		copyClosure(path, disnixInterface);
	}
}
