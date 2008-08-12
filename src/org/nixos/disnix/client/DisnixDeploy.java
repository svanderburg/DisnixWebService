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
		/* Get set of target deploy items */
		String target_filename = args[0];
		
		DistributionModel target_model = new DistributionModel(target_filename);
		Vector<DistributionElement> targetDeployElements = target_model.getResult();
		
		Vector<DistributionElement> sourceDeployElements;
		
		/* Get set of source deploy items */
		if(args.length >= 2)
		{
			String source_filename = args[1];
			DistributionModel source_model = new DistributionModel(source_filename);
			sourceDeployElements = source_model.getResult();
		}
		else
			sourceDeployElements = new Vector<DistributionElement>(); // Empty set				
		
		/* Determine elements to install and to remove */
		
		Vector<DistributionElement> intersection = new Vector<DistributionElement>(targetDeployElements);
		intersection.retainAll(sourceDeployElements); 
		
		Vector<DistributionElement> elementsToInstall = new Vector<DistributionElement>(targetDeployElements);
		elementsToInstall.removeAll(intersection);
		
		Vector<DistributionElement> elementsToUninstall = new Vector<DistributionElement>(sourceDeployElements);
		elementsToUninstall.removeAll(intersection);
		
		/* Copy all closures to install to the target machines */
		
		for(DistributionElement e : targetDeployElements)
		{
			DisnixInterface disnixInterface = new DisnixInterface(e.getTargetEPR());
			DisnixCopyClosure.copyClosure(e.getService(), disnixInterface);
		}
		
		/* Install the closures in the environments on the target machines */
		
		for(DistributionElement e : elementsToInstall)
		{
			DisnixInterface disnixInterface = new DisnixInterface(e.getTargetEPR());
			disnixInterface.install("", e.getService(), false);
		}
		
		/* Call the activation hook for each element to install */
		
		for(DistributionElement e : elementsToInstall)
		{
			DisnixInterface disnixInterface = new DisnixInterface(e.getTargetEPR());
			disnixInterface.activate(e.getService());			
		}
		
		/* Call the deactivation hook for each element to install */
		
		for(DistributionElement e : elementsToUninstall)
		{
			DisnixInterface disnixInterface = new DisnixInterface(e.getTargetEPR());
			disnixInterface.deactivate(e.getService());			
		}
		
		/* Uninstall the closures in the environments on the target machines */
		
		for(DistributionElement e : elementsToUninstall)
		{
			DisnixInterface disnixInterface = new DisnixInterface(e.getTargetEPR());
			disnixInterface.uninstall(e.getService());
		}
	}
}
