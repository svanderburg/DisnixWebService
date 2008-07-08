package org.nixos.disnix.client;
import java.io.*;
import java.util.*;
import org.apache.axiom.om.impl.builder.*;
import org.apache.axiom.om.*;
import org.apache.axiom.om.xpath.*;

public class Test
{
	public static void main(String[] args) throws Exception
	{
		String filename = "/nix/store/z9iilscgc4jkjbk5vj0dfw6c50d3f00f-distribution-export/output.xml";
		FileInputStream is = new FileInputStream(filename);
		StAXOMBuilder builder = new StAXOMBuilder(is);
		OMElement documentElement = builder.getDocumentElement();
		
		System.out.println("root is: "+documentElement.getLocalName());
		
		AXIOMXPath xpathGetAttrs = new AXIOMXPath("/expr/list/attrs");
		List<OMNode> nodeListGetAttrs = (List<OMNode>)xpathGetAttrs.selectNodes(documentElement);
		
		// Iterate over all attribute sets in list
		for(OMNode getAttrsNode : nodeListGetAttrs)
		{
			AXIOMXPath xpathAttrName = new AXIOMXPath("attr");
			List<OMNode> nodeListAttrName = (List<OMNode>)xpathAttrName.selectNodes(getAttrsNode);
			
			for(OMNode attrNameNode : nodeListAttrName)
			{
				System.out.println(attrNameNode);
				
			}
		}
		
		is.close();
	}
}
