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
import java.util.*;

public class DisnixClient
{
	public static void printUsage()
	{
		System.out.println("Usage:\n"+
				           "disnix-soap-client --import {--remotefile filename | --localfile filename} derivation\n"+
				           "disnix-soap-client --export {--remotefile filename | --localfile filename} derivation\n"+
				           "disnix-soap-client --print-invalid derivations\n"+
						   "disnix-soap-client {-r | --realise } derivations\n"+
						   "disnix-soap-client --set [{-p | --profile} profile] derivation\n"+
						   "disnix-soap-client {-q | --query-installed} [{-p | --profile} profile]\n"+
						   "disnix-soap-client --query-requisites derivations\n"+
						   "disnix-soap-client --collect-garbage {-d | --delete-old}\n"+
						   "disnix-soap-client --activate --type type --arguments arguments derivation\n"+
						   "disnix-soap-client --deactivate --type type --arguments arguments derivation\n"+
						   "disnix-soap-client --lock\n"+
						   "disnix-soap-client --unlock\n"+
						   "disnix-soap-client {-h | --help}");
	}
		
	public static void main(String[] args)
	{
		/* Create command line option parser */
		CmdLineParser parser = new CmdLineParser();
		
		/* Operations */
		CmdLineParser.Option opt_import = parser.addBooleanOption("import");
		CmdLineParser.Option opt_export = parser.addBooleanOption("export");
		CmdLineParser.Option opt_print_invalid = parser.addBooleanOption("print-invalid");
		CmdLineParser.Option opt_realise = parser.addBooleanOption('r', "realise");
		CmdLineParser.Option opt_set = parser.addBooleanOption("set");
		CmdLineParser.Option opt_query_installed = parser.addBooleanOption('q', "query-installed");
		CmdLineParser.Option opt_query_requisites = parser.addBooleanOption("query-requisites");
		CmdLineParser.Option opt_collect_garbage = parser.addBooleanOption("collect-garbage");
		CmdLineParser.Option opt_activate = parser.addBooleanOption("activate");
		CmdLineParser.Option opt_deactivate = parser.addBooleanOption("deactivate");
		CmdLineParser.Option opt_lock = parser.addBooleanOption("lock");
		CmdLineParser.Option opt_unlock = parser.addBooleanOption("unlock");
		CmdLineParser.Option opt_help = parser.addBooleanOption('h', "help");
		
		/* Other attributes */
		CmdLineParser.Option opt_target = parser.addStringOption('t', "target");
		CmdLineParser.Option opt_localfile = parser.addBooleanOption("localfile");
		CmdLineParser.Option opt_remotefile = parser.addBooleanOption("remotefile");
		CmdLineParser.Option opt_profile = parser.addStringOption('p', "profile");
		CmdLineParser.Option opt_delete_old = parser.addBooleanOption('d', "delete-old");
		CmdLineParser.Option opt_type = parser.addStringOption("type");
		CmdLineParser.Option opt_arguments = parser.addStringOption("arguments");
		
		try
		{
			/* Parse command line options */
			parser.parse(args);
			
			/* Retrieve option values */
			Boolean value_import = (Boolean)parser.getOptionValue(opt_import);
			Boolean value_export = (Boolean)parser.getOptionValue(opt_export);
			Boolean value_print_invalid = (Boolean)parser.getOptionValue(opt_print_invalid);
			Boolean value_realise = (Boolean)parser.getOptionValue(opt_realise);
			Boolean value_set = (Boolean)parser.getOptionValue(opt_set);
			Boolean value_query_installed = (Boolean)parser.getOptionValue(opt_query_installed);
			Boolean value_query_requisites = (Boolean)parser.getOptionValue(opt_query_requisites);
			Boolean value_collect_garbage = (Boolean)parser.getOptionValue(opt_collect_garbage);
			Boolean value_activate = (Boolean)parser.getOptionValue(opt_activate);
			Boolean value_deactivate = (Boolean)parser.getOptionValue(opt_deactivate);
			Boolean value_lock = (Boolean)parser.getOptionValue(opt_lock);
			Boolean value_unlock = (Boolean)parser.getOptionValue(opt_unlock);
			Boolean value_help = (Boolean)parser.getOptionValue(opt_help); 
			
			String value_target = (String)parser.getOptionValue(opt_target);
			Boolean value_localfile = (Boolean)parser.getOptionValue(opt_localfile);
			Boolean value_remotefile = (Boolean)parser.getOptionValue(opt_remotefile);
			String value_profile = (String)parser.getOptionValue(opt_profile);
			Boolean value_delete_old = (Boolean)parser.getOptionValue(opt_delete_old);
			String value_type = (String)parser.getOptionValue(opt_type);
			String value_arguments = (String)parser.getOptionValue(opt_arguments);
			
			String[] derivation = parser.getRemainingArgs();
			
			/* Validate command line options */
			
			if(value_target == null)
			{
				System.err.println("ERROR: A targetEPR must be specified!");
				System.exit(1);
			}
			
			/* Display usage if requested */
			
			if(value_help != null)
			{
				printUsage();
				System.exit(0);
			}
			
			/* Create SOAP connection interface */
			
			System.err.println("Connecting to target endpoint reference: "+value_target);
			DisnixInterface disnixInterface = new DisnixInterface(value_target);
			
			/* Execute operation */
			
			if(value_import != null)
			{
				if(value_remotefile != null)
				{
					System.err.println("Importing remote derivation: "+derivation);
					disnixInterface.importm(derivation);
				}
				else if(value_localfile != null)
				{
					System.err.println("Import local derivation: "+derivation);
					disnixInterface.importLocalFile(derivation);
				}
				else
				{
					System.err.println("ERROR: Either --localfile or --remotefile should be specified!");
					System.exit(1);
				}
			}
			else if(value_export != null)
			{
				if(value_remotefile != null)
				{
					System.err.println("Exporting remote derivation: "+derivation);
					disnixInterface.exportRemoteFile(derivation);
				}
				else if(value_localfile != null)
				{
					System.err.println("Exporting local derivation: "+derivation);
					String result = disnixInterface.export(derivation);
					System.out.println(result);
				}
				else
				{
					System.err.println("ERROR: Either --localfile or --remotefile should be specified!");
					System.exit(1);
				}
			}
			else if(value_print_invalid != null)
			{
				System.err.println("Print invalid: "+derivation);
				String[] result = disnixInterface.printInvalid(derivation);
				System.out.println(result);
			}
			else if(value_realise != null)
			{
				System.err.println("Realise: "+derivation);
				String[] result = disnixInterface.realise(derivation);
				System.out.println(result);
			}
			else if(value_set != null)
			{
				String profile;
				
				if(value_profile == null)
					profile = "default";
				else
					profile = value_profile;
				
				System.err.println("Set profile: "+profile+" derivation: "+derivation);
				
				disnixInterface.set(profile, derivation[0]);
			}
			else if(value_query_installed != null)
			{
				String profile;
				
				if(value_profile == null)
					profile = "default";
				else
					profile = value_profile;
				
				System.err.println("Query installed: "+profile);
				
				String[] result = disnixInterface.queryInstalled(profile);
				System.out.println(result);
			}
			else if(value_query_requisites != null)
			{
				System.err.println("Query requisites: "+derivation);
				String[] result = disnixInterface.queryRequisites(derivation);
				System.out.println(result);
			}
			else if(value_collect_garbage != null)
			{
				boolean deleteOld;
				
				if(value_delete_old == null)
					deleteOld = false;
				else
					deleteOld = value_delete_old;
				
				System.err.println("Collect garbage. Delete old: "+deleteOld);
				
				disnixInterface.collectGarbage(deleteOld);
			}
			else if(value_activate != null)
			{
				/* Create arguments string */
				StringTokenizer st = new StringTokenizer(value_arguments);
				String[] arguments = new String[st.countTokens()];
				
				for(int i = 0; i < st.countTokens(); i++)
					arguments[i] = st.nextToken();
				
				/* Invoke operation */
				disnixInterface.activate(derivation[0], value_type, arguments);
			}
			else if(value_deactivate != null)
			{
				/* Create arguments string */
				StringTokenizer st = new StringTokenizer(value_arguments);
				String[] arguments = new String[st.countTokens()];
				
				for(int i = 0; i < st.countTokens(); i++)
					arguments[i] = st.nextToken();
				
				/* Invoke operation */
				disnixInterface.deactivate(derivation[0], value_type, arguments);
			}
			else if(value_lock != null)
			{
			}
			else if(value_unlock != null)
			{
			}
			else
			{
				System.err.println("ERROR: An operation must be specified!");
				System.exit(1);
			}
		}
		catch(CmdLineParser.OptionException ex)
		{
			printUsage();
			ex.printStackTrace();
		}
		catch(Exception ex)
		{
			ex.printStackTrace();
		}
	}
}
