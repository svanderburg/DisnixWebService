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
import jargs.gnu.*;
import org.apache.axis2.*;

public class DisnixClient
{
	public static void printUsage()
	{
		System.out.println("Usage:\n"+
						   "disnix-soap-client {-i | --install} [--remotefile filename] [-A attributePath] args targetEPR\n"+
						   "disnix-soap-client {-u | --upgrade} derivation targetEPR\n"+
						   "disnix-soap-client {-e | --uninstall} derivation targetEPR\n"+
						   "disnix-soap-client --instantiate {--remotefile filename | --localfile filename} {-A attributePath | --attr attributePath} targetEPR\n"+
						   "disnix-soap-client {-r | --realise } pathname targetEPR\n"+
						   "disnix-soap-client --import {--remotefile filename | --localfile filename} targetEPR\n"+
						   "disnix-soap-client --print-invalid-paths path targetEPR\n"+
						   "disnix-soap-client --collect-garbage [-d | --delete-old]\n"+
						   "disnix-soap-client --type type --activate path\n"+
						   "disnix-soap-client --type type --deactivate path\n"+
						   "disnix-soap-client {-h | --help}");
	}
		
	public static void main(String[] args)
	{
		/* Create command line option parser */
		
		CmdLineParser parser = new CmdLineParser();
		CmdLineParser.Option opt_install = parser.addBooleanOption('i', "install");
		CmdLineParser.Option opt_upgrade = parser.addStringOption('u', "upgrade");
		CmdLineParser.Option opt_uninstall = parser.addStringOption('e', "uninstall");
		CmdLineParser.Option opt_instantiate = parser.addBooleanOption("instantiate");
		CmdLineParser.Option opt_realise = parser.addStringOption('r', "realise");
		CmdLineParser.Option opt_import = parser.addBooleanOption("import");
		CmdLineParser.Option opt_print_invalid_paths = parser.addStringOption("print-invalid-paths");
		CmdLineParser.Option opt_collect_garbage = parser.addBooleanOption("collect-garbage");
		CmdLineParser.Option opt_activate = parser.addStringOption("activate");
		CmdLineParser.Option opt_deactivate = parser.addStringOption("deactivate");
		CmdLineParser.Option opt_help = parser.addBooleanOption('h', "help");		
		CmdLineParser.Option opt_remotefile = parser.addStringOption("remotefile");
		CmdLineParser.Option opt_localfile = parser.addStringOption("localfile");
		CmdLineParser.Option opt_attribute = parser.addStringOption('A', "attr");		
		CmdLineParser.Option opt_delete_old = parser.addBooleanOption('d', "delete-old");
		CmdLineParser.Option opt_type = parser.addStringOption("type");
		
		try
		{
			/* Parse command line options */
			parser.parse(args);
			
			/* Display usage if requested */
			
			Boolean value_help = (Boolean)parser.getOptionValue(opt_help); 
			if(value_help != null)
			{
				printUsage();
				System.exit(0);
			}
			
			/* Determine targetEPR URL */
			String[] otherArgs = parser.getRemainingArgs();			
			String targetEPR = otherArgs[otherArgs.length - 1];
			
			/* Create SOAP connection interface */
			
			System.out.println("Connecting to target endpoint reference: "+targetEPR);
			DisnixInterface disnixInterface = new DisnixInterface(targetEPR);
			
			/* Execute operation */
			
			Boolean value_install = (Boolean)parser.getOptionValue(opt_install);
			String value_upgrade = (String)parser.getOptionValue(opt_upgrade);
			String value_uninstall = (String)parser.getOptionValue(opt_uninstall);
			Boolean value_instantiate = (Boolean)parser.getOptionValue(opt_instantiate);
			String value_realise = (String)parser.getOptionValue(opt_realise); 
			Boolean value_import = (Boolean)parser.getOptionValue(opt_import);
			String value_print_invalid_paths = (String)parser.getOptionValue(opt_print_invalid_paths);
			Boolean value_collect_garbage = (Boolean)parser.getOptionValue(opt_collect_garbage);
			String value_activate = (String)parser.getOptionValue(opt_activate);
			String value_deactivate = (String)parser.getOptionValue(opt_deactivate);
			
			if(value_install != null)
			{
				String value_remotefile = (String)parser.getOptionValue(opt_remotefile);
				String value_attribute = (String)parser.getOptionValue(opt_attribute);
				String install_args = "";
				boolean isAttr;
				
				if(value_remotefile != null)
					System.out.println("Using remote file: "+value_remotefile);
				else
					value_remotefile = "";
				
				if(value_attribute != null)
				{
					isAttr = true;
					install_args = value_attribute;
					System.out.println("Install attribute: "+install_args);
				}
				else
				{
					isAttr = false;
					for(int i = 0; i < otherArgs.length - 1; i++)
						install_args += " "+ otherArgs[i];
					
					System.out.println("Install derivation: "+install_args);
				}
				
				disnixInterface.install(value_remotefile, install_args, isAttr);
			}
			else if(value_upgrade != null)
			{
				System.out.println("Upgrading derivation: "+value_upgrade);
				disnixInterface.upgrade(value_upgrade);
			}
			else if(value_uninstall != null)
			{
				System.out.println("Uninstalling derivation: "+value_uninstall);
				disnixInterface.uninstall(value_uninstall);
			}
			else if(value_instantiate != null)
			{
				String value_remotefile = (String)parser.getOptionValue(opt_remotefile);
				String value_localfile = (String)parser.getOptionValue(opt_localfile);
				String value_attribute = (String)parser.getOptionValue(opt_attribute);
				
				if(value_attribute == null)
					value_attribute = "";
				
				if(value_remotefile != null)
				{
					System.out.println("Instantiate remote file: "+value_remotefile+" and attribute path: "+value_attribute);
					String[] ret = disnixInterface.instantiate(value_remotefile, value_attribute);
					for(int i=0; i<ret.length; i++)
						System.out.println(ret[i]);
				}
				else if(value_localfile != null)
				{
					System.out.println("Instantiate local file: "+value_localfile+" and attribute path: "+value_attribute);
					String ret[] = disnixInterface.instantiateExpression(value_localfile, value_attribute);
					for(int i=0; i<ret.length; i++)
						System.out.println(ret[i]);
				}
				else
				{
					System.err.println("Either a remote or local file must be specified!");
					printUsage();
					System.exit(1);
				}
			}
			else if(value_realise != null)
			{
				System.out.println("Realise path: "+value_realise);
				disnixInterface.realise(value_realise);
			}
			else if(value_import != null)
			{
				String value_remotefile = (String)parser.getOptionValue(opt_remotefile);
				String value_localfile = (String)parser.getOptionValue(opt_localfile);
				
				if(value_remotefile != null)
				{
					System.out.println("Import remote closure: "+value_remotefile);					
					disnixInterface.importm(value_remotefile);
				}
				else if(value_localfile != null)
				{
					System.out.println("Import local closure: "+value_localfile);
					disnixInterface.importClosure(value_localfile);
				}				
				else
				{
					System.err.println("Either a remote or local file must be specified!");
					printUsage();
					System.exit(1);
				}
			}
			else if(value_print_invalid_paths != null)
			{
				System.out.println("Print invalid paths of: "+value_print_invalid_paths);
				String[] ret = disnixInterface.printInvalidPaths(value_print_invalid_paths);
				for(int i=0; i<ret.length; i++)
					System.out.println(ret[i]);
			}
			else if(value_collect_garbage != null)
			{
				Boolean value_delete_old = (Boolean)parser.getOptionValue(opt_delete_old);
				if(value_delete_old == null)
					value_delete_old = false;
				
				System.out.println("Collect garbage. Delete old generations: "+value_delete_old);
				disnixInterface.collectGarbage(value_delete_old);
			}
			else if(value_activate != null)
			{
				String value_type = (String)parser.getOptionValue(opt_type);
				
				System.out.println("Activating: "+value_activate);
				disnixInterface.activate(value_activate, value_type);
			}
			else if(value_deactivate != null)
			{
				String value_type = (String)parser.getOptionValue(opt_type);
				
				System.out.println("Deactivating: "+value_deactivate);
				disnixInterface.deactivate(value_deactivate, value_type);
			}
			else
			{
				System.err.println("You must specify an operation!");
				printUsage();
				System.exit(1);
			}
		}
		catch(CmdLineParser.OptionException ex)
		{
			printUsage();
			ex.printStackTrace();
		}
		catch(AxisFault ex)
		{
			ex.printStackTrace();
		}
		
		System.out.println("Operation finished!");
	}
}
