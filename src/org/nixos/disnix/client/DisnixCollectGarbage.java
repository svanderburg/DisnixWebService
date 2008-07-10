package org.nixos.disnix.client;

import java.io.*;
import java.util.*;
import javax.xml.namespace.*;
import org.apache.axiom.om.*;
import org.apache.axiom.om.impl.builder.*;
import org.apache.axiom.om.xpath.*;

public class DisnixCollectGarbage
{
	public static void collectGarbage(String infrastructureFile, boolean deleteOld) throws Exception
	{
		FileInputStream is = new FileInputStream(infrastructureFile);
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
		collectGarbage(args[0], false);
	}
}
