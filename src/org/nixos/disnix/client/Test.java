package org.nixos.disnix.client;
import java.io.*;
import org.apache.axiom.om.impl.builder.*;
import org.apache.axiom.om.*;

public class Test
{
	public static void main(String[] args) throws Exception
	{
		InputStream is = NixInterface.getInstance().evaluateToXML("infrastructure.nix");
		StAXOMBuilder builder = new StAXOMBuilder(is);
		OMElement documentElement = builder.getDocumentElement();
		
		System.out.println("root is: "+documentElement.getLocalName());
		is.close();
	}
}
