package org.nixos.disnix.client;

import jargs.gnu.CmdLineParser;

import java.io.*;
import java.util.*;
import javax.xml.namespace.*;
import org.apache.axiom.om.*;
import org.apache.axiom.om.impl.builder.*;
import org.apache.axiom.om.xpath.*;

public class DisnixCollectGarbage
{
	public static void printUsage()
	{
		System.out.println("Usage:");
		System.out.println("disnix-soap-collect-garbage [ -d | --delete-old ]");
		System.out.println("disnix-soap-collect-garbage [ -h | --help ]");
	}
	
	public static void collectGarbage(String infrastructureFile, boolean deleteOld) throws Exception
	{
		InputStream is = NixInterface.getInstance().evaluateToXML(infrastructureFile);
		StAXOMBuilder builder = new StAXOMBuilder(is);
		OMElement documentElement = builder.getDocumentElement();
		
		AXIOMXPath xpathTargetEPR = new AXIOMXPath("/expr/attrs/attr/attrs/attr/string");		
		List<OMElement> nodeListXpathTargetEPR = xpathTargetEPR.selectNodes(documentElement);
		
		// Iterate
		for(OMElement targetEPRNode : nodeListXpathTargetEPR)
		{
			String targetEPR = targetEPRNode.getAttributeValue(new QName("value"));
			System.out.println("Running garbage collect on: "+targetEPR);
			DisnixInterface disnixInterface = new DisnixInterface(targetEPR);
			disnixInterface.collectGarbage(deleteOld);
		}
	}
	
	public static void main(String[] args) throws Exception
	{
		/* Create command line option parser */
		
		CmdLineParser parser = new CmdLineParser();
		CmdLineParser.Option opt_delete_old = parser.addBooleanOption('d', "delete-old");
		CmdLineParser.Option opt_help = parser.addBooleanOption('h', "help");
				
		try
		{
			/* Parse command line options */
			parser.parse(args);
			
			/* Execute operation */
			
			Boolean value_help = (Boolean)parser.getOptionValue(opt_help);
			Boolean value_delete_old = (Boolean)parser.getOptionValue(opt_delete_old);
			
			if(value_help != null)
			{
				printUsage();
				System.exit(0);
			}
			else
			{
				if(value_delete_old == null)
					value_delete_old = false;
				
				/* Determine infrastructure file */
				String[] otherArgs = parser.getRemainingArgs();			
				String infrastructureFile = otherArgs[otherArgs.length - 1];
				
				collectGarbage(infrastructureFile, value_delete_old);
			}
		}
		catch(CmdLineParser.OptionException ex)
		{
			printUsage();
			ex.printStackTrace();
		}
	}
}
