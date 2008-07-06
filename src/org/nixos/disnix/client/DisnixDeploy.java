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

public class DisnixDeploy
{
	public static void main(String[] args) throws Exception
	{
		String compositionsExpr = "compositions.nix";
		Vector<String> closuresToSend = new Vector<String>();
		
		System.out.println("Getting all compositions...");
		
		Vector<String> compositions = NixInterface.getInstance().queryAttributePaths(compositionsExpr);
		
		/* Instantiate and realise each composition */
		
		for(int i=0; i<compositions.size(); i++)
		{
			System.out.println("Instantatie composition: "+compositions.elementAt(i));
			String drvpath = NixInterface.getInstance().instantiate(compositionsExpr, compositions.elementAt(i)).elementAt(0);
							
			System.out.println("Realise derivation: "+drvpath);
			String storepath = NixInterface.getInstance().realise(drvpath);
			
			System.out.println("result: "+storepath);
			closuresToSend.add(storepath);
		}
		
		/* Connect to EPR */
		
		DisnixInterface disnixInterface = new DisnixInterface("http://localhost:8080/axis2/services/DisnixService");
		
		/* Copy closures to target machines */
		
		for(int i=0; i<closuresToSend.size(); i++)
		{
			System.out.println("Copy closure: "+closuresToSend.elementAt(i)+" to target EPR");
			DisnixCopyClosure.copyClosure(closuresToSend.elementAt(i), disnixInterface);
		}
		
		/* Activate closures on target machines */
		
		for(int i=0; i<closuresToSend.size(); i++)
		{
			System.out.println("Install closure: "+closuresToSend.elementAt(i));
			disnixInterface.install("", closuresToSend.elementAt(i), false);
		}
	}
}
