/* 
 * Copyright (c) 2008-2010 Sander van der Burg
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package org.nixos.disnix.client;
import jargs.gnu.*;
import java.util.*;
import java.io.*;

public class DisnixClient
{
	private static void printUsage()
	{
		System.out.println("Usage:\n"+
				           "disnix-soap-client --import {--remotefile | --localfile } derivation\n"+
				           "disnix-soap-client --export {--remotefile | --localfile } derivation\n"+
				           "disnix-soap-client --print-invalid derivations\n"+
						   "disnix-soap-client {-r | --realise } derivations\n"+
						   "disnix-soap-client --set [{-p | --profile} profile] derivation\n"+
						   "disnix-soap-client {-q | --query-installed} [{-p | --profile} profile]\n"+
						   "disnix-soap-client --query-requisites derivations\n"+
						   "disnix-soap-client --collect-garbage {-d | --delete-old}\n"+
						   "disnix-soap-client --activate --type type --arguments arguments derivation\n"+
						   "disnix-soap-client --deactivate --type type --arguments arguments derivation\n"+
						   "disnix-soap-client --lock [{-p | --profile} profile]\n"+
						   "disnix-soap-client --unlock [{-p | --profile} profile]\n"+
						   "disnix-soap-client {-h | --help}");
	}
	
	private static void printStringArray(String[] string, PrintStream printStream, String separator)
	{
		for(int i = 0; i < string.length; i++)
		{
			printStream.print(string[i]);
			printStream.print(separator);
		}
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
			Vector<String> value_arguments = parser.getOptionValues(opt_arguments);
			
			String[] derivation = parser.getRemainingArgs();						
			
			/* Display usage if requested */
			
			if(value_help != null)
			{
				printUsage();
				System.exit(0);
			}
			
			/* Validate command line options */
			
			if(value_target == null)
			{
				System.err.println("ERROR: A targetEPR must be specified!");
				System.exit(1);
			}
			
			/* Create SOAP connection interface */
			
			System.err.println("Connecting to target endpoint reference: "+value_target);
			DisnixInterface disnixInterface = new DisnixInterface(value_target);
			
			/* Execute operation */
			
			if(value_import != null)
			{
				if(value_remotefile != null)
				{
					System.err.println("Importing remote closure: "+derivation[0]);
					disnixInterface.importm(derivation[0]);
				}
				else if(value_localfile != null)
				{
					System.err.println("Import local closure: "+derivation[0]);
					disnixInterface.importLocalFile(derivation[0]);
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
				System.err.println("Print invalid: ");
				printStringArray(derivation, System.err, "");
				System.err.println("\nReturns:");
				String[] result = disnixInterface.printInvalid(derivation);
				printStringArray(result, System.out, "");
				System.out.println();
			}
			else if(value_realise != null)
			{
				System.err.println("Realise: ");
				printStringArray(derivation, System.err,"");
				System.err.println("\nReturns:");
				String[] result = disnixInterface.realise(derivation);
				printStringArray(result, System.out, "");
				System.out.println();
			}
			else if(value_set != null)
			{
				String profile;
				
				if(value_profile == null)
					profile = "default";
				else
					profile = value_profile;
				
				System.err.println("Set profile: "+profile+" derivation: "+derivation[0]);
				
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
				printStringArray(result, System.out, "");
				System.out.println();
			}
			else if(value_query_requisites != null)
			{
				System.err.println("Query requisites: ");
				printStringArray(derivation, System.err, "");				
				System.err.println("\nReturns:");
				
				String[] result = disnixInterface.queryRequisites(derivation);
				printStringArray(result, System.out, "");
				System.out.println();
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
				/* Convert arguments vector to array */
				String[] arguments = new String[value_arguments.size()];
				value_arguments.toArray(arguments);
				
				/* Invoke operation */				
				System.err.print("Activate derivation: "+derivation[0]+" of type: "+value_type+" with arguments: ");
				printStringArray(arguments, System.err, " ");
				System.err.println();
				
				disnixInterface.activate(derivation[0], value_type, arguments);
			}
			else if(value_deactivate != null)
			{		
				/* Convert arguments vector to array */
				String[] arguments = new String[value_arguments.size()];
				value_arguments.toArray(arguments);
				
				/* Invoke operation */
				System.err.println("Deactivate derivation: "+derivation[0]+" of type: "+value_type+" with arguments: ");
				printStringArray(arguments, System.err, " ");
				System.err.println();
				
				disnixInterface.deactivate(derivation[0], value_type, arguments);
			}
			else if(value_lock != null)
			{
				String profile;
				
				if(value_profile == null)
					profile = "default";
				else
					profile = value_profile;
				
				System.err.println("Acquiring lock on profile: "+profile);
				disnixInterface.lock(profile);
			}
			else if(value_unlock != null)
			{
				String profile;
				
				if(value_profile == null)
					profile = "default";
				else
					profile = value_profile;
				
				System.err.println("Releasing lock on profile: "+profile);
				disnixInterface.unlock(profile);
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
			System.exit(1);
		}
	}
}
