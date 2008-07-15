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
import java.io.*;
import java.util.*;

public class NixInterface
{
	private final static NixInterface nixInterface = new NixInterface();
	
	private NixInterface()
	{
	}
	
	public static NixInterface getInstance()
	{
		return nixInterface;
	}
	
	public String queryResolve(String path) throws IOException
	{		
		String storePath = "";
		String line;
		Process p = Runtime.getRuntime().exec("nix-store --query --resolve "+path);
		BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
		
		while((line = br.readLine()) != null)
		{
			System.out.println(line);
			storePath = line;
		}
		br.close();
		
		return storePath;
	}
	
	public Stack<String> queryRequisites(String storePath) throws IOException
	{
		Stack<String> allStorePaths = new Stack<String>();
		String line;
		Process p = Runtime.getRuntime().exec("nix-store --query --requisites "+storePath);
		BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
		while((line = br.readLine()) != null)
		{
			System.out.println(line);
			allStorePaths.push(line);
		}
		br.close();
		
		return allStorePaths;
	}
	
	public void export(String[] invalidPaths) throws IOException
	{
		/* Put all invalid paths in one string */
		String invalidPathsStr = "";
			
		for(int i=0; i<invalidPaths.length; i++)
			invalidPathsStr += " "+invalidPaths[i];
					
		if(invalidPaths.length > 0)
		{
			System.out.println("Creating serialisation...");
			Process p = Runtime.getRuntime().exec("nix-store --export "+invalidPathsStr);
			InputStream is = p.getInputStream();
			FileOutputStream os = new FileOutputStream("/tmp/out.closure");
			byte[] buf = new byte[1024];
			int i = 0;
			
			while((i = is.read(buf)) != -1)
				os.write(buf, 0, i);
			
			is.close();
			os.close();
		}
	}
	
	public InputStream toXML(String expr) throws IOException
	{
		Process p = Runtime.getRuntime().exec("nix-instantiate --eval-only --xml "+expr);
		return p.getInputStream();
	}
}
