package org.nixos.disnix.client;
import java.io.*;
import java.util.*;
import org.apache.axiom.om.impl.builder.*;
import org.apache.axiom.om.*;
import org.apache.axiom.om.xpath.*;
import javax.xml.namespace.*;

public class DistributionModel
{
	private Vector<DistributionElement> result;
	
	public DistributionModel(String filename) throws Exception
	{
		result = new Vector<DistributionElement>();
		
		FileInputStream is = new FileInputStream(filename);
		StAXOMBuilder builder = new StAXOMBuilder(is);
		OMElement documentElement = builder.getDocumentElement();
				
		AXIOMXPath xpathGetAttrs = new AXIOMXPath("/expr/list/attrs");
		List<OMNode> nodeListGetAttrs = xpathGetAttrs.selectNodes(documentElement);
		
		// Iterate over all attribute sets in the list
		for(OMNode getAttrsNode : nodeListGetAttrs)
		{
			AXIOMXPath xpathAttrName = new AXIOMXPath("attr");
			List<OMElement> nodeListAttrName = xpathAttrName.selectNodes(getAttrsNode);
			
			// Iterate over attributes in attributeset
			String service = "";
			String target = "";
			
			for(OMElement attrNameNode : nodeListAttrName)
			{			
				AXIOMXPath xpathValue = new AXIOMXPath("string");
				List<OMElement> nodeListValue = xpathValue.selectNodes(attrNameNode);
								
				// Print the value
				for(OMElement valueNode : nodeListValue)
				{
					String value = valueNode.getAttribute(new QName("value")).getAttributeValue();				
					String name = attrNameNode.getAttribute(new QName("name")).getAttributeValue();
					
					if(name.equals("service"))
						service = value;
					else if(name.equals("target"))
						target = value;
				}
			}
			
			DistributionElement e = new DistributionElement(service, target);
			result.add(e);
		}
		
		is.close();
	}
	
	public Vector<DistributionElement> getResult()
	{
		return result;
	}
}
