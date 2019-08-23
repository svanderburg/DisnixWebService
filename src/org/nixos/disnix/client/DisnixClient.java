/*
 * Copyright (c) 2008-2019 Sander van der Burg
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

/**
 * Command line interface which connects to a DisnixWebService instance.
 */
public class DisnixClient
{
	/**
	 * Prints the usage of the command-line tool.
	 */
	private static void printUsage()
	{
		System.out.println("Usage: disnix-soap-client --target targetEPR operation [OPTION] paths\n"+
			"\n"+
			"The command `disnix-soap-client' provides remote access to a `disnix-service'\n"+
			"instance running on a machine in the network by using a SOAP/HTTP connection.\n"+
			"This allows the user to perform remote deployment operations on a target machine\n"+
			"through SOAP.\n"+
			"\n"+
			"In most cases this command is not used directly, but is used by specifying the\n"+
			"--interface option for a Disnix command-line utility (such as `disnix-env') or\n"+
			"by setting the `DISNIX_CLIENT_INTERFACE' environment variable. By using one of\n"+
			"those properties, the Disnix tools will use the given interface instead of the\n"+
			"standard `disnix-client' which only provides loopback access.\n"+
			"\n"+
			"Options:\n"+
			"\n"+
			"Operations:\n"+
			"\n"+
			"Operations:\n"+
			"      --import               Imports a given closure into the Nix store of the\n"+
			"                             target machine. Optionally, transfers the closure\n"+
			"                             from this machine to the target machine\n"+
			"      --export               Exports the closure of a given Nix store path of\n"+
			"                             the target machine into a file, and optionally\n"+
			"                             downloads it\n"+
			"      --print-invalid        Prints all the paths that are not valid in the Nix\n"+
			"                             store of the target machine\n"+
			"  -r, --realise              Realises the given store derivation on the target\n"+
			"                             machine\n"+
			"      --set                  Creates a Disnix profile only containing the given\n"+
			"                             derivation on the target machine\n"+
			"  -q, --query-installed      Queries all the installed services on the given\n"+
			"                             target machine\n"+
			"      --query-requisites     Queries all the requisites (intra-dependencies) of\n"+
			"                             the given services on the target machine\n"+
			"      --collect-garbage      Collects garbage on the given target machine\n"+
			"      --activate             Activates the given service on the target machine\n"+
			"      --deactivate           Deactivates the given service on the target machine\n"+
			"      --lock                 Acquires a lock on a Disnix profile of the target\n"+
			"                             machine\n"+
			"      --unlock               Release the lock on a Disnix profile of the target\n"+
			"                             machine\n"+
			"      --snapshot             Snapshots the logical state of a component on the\n"+
			"                             given target machine\n"+
			"      --restore              Restores the logical state of a component on the\n"+
			"                             given target machine\n"+
			"      --delete-state         Deletes the state of a component on the given\n"+
			"                             machine\n"+
			"      --query-all-snapshots  Queries all available snapshots of a component on\n"+
			"                             the given target machine\n"+
			"      --query-latest-snapshot\n"+
			"                             Queries the latest snapshot of a component on the\n"+
			"                             given target machine\n"+
			"      --print-missing-snapshots\n"+
			"                             Prints the paths of all snapshots not present on\n"+
			"                             the given target machine\n"+
			"      --import-snapshots     Imports the specified snapshots into the remote\n"+
			"                             snapshot store\n"+
			"      --export-snapshots     Exports the specified snapshot to the local\n"+
			"                             snapshot store\n"+
			"      --resolve-snapshots    Converts the relative paths to the snapshots to\n"+
			"                             absolute paths\n"+
			"      --clean-snapshots      Removes older snapshots from the snapshot store\n"+
			"      --capture-config       Captures the configuration of the machine from the\n"+
			"                             Dysnomia container properties in a Nix expression\n"+
			"      --shell                Spawns a Dysnomia shell to run arbitrary\n"+
			"                             maintenance tasks\n"+
			"      --help                 Shows the usage of this command to the user\n"+
			"      --version              Shows the version of this command to the user\n"+
			"\n"+
			"General options:\n"+
			"  -t, --target=TARGET        Specifies the hostname and optional port number of\n"+
			"                             the SSH server used to connect to the target\n"+
			"                             machine\n"+
			"\n"+
			"Import/Export/Import snapshots/Export snapshots/Shell options:\n"+
			"      --localfile            Specifies that the given paths are stored locally\n"+
			"                             and must be transferred to the remote machine if\n"+
			"                             needed\n"+
			"      --remotefile           Specifies that the given paths are stored remotely\n"+
			"                             and must transferred from the remote machine if\n"+
			"                             needed\n"+
			"\n"+
			"Shell options:\n"+
			"      --command=COMMAND      Commands to execute in the shell session\n"+
			"\n"+
			"Set/Query installed/Lock/Unlock options:\n"+
			"  -p, --profile=PROFILE      Name of the Disnix profile. Defaults to: default\n"+
			"\n"+
			"Collect garbage options:\n"+
			"  -d, --delete-old           Indicates whether all older generations of Nix\n"+
			"                             profiles must be removed as well\n"+
			"\n"+
			"Activation/Deactivation/Snapshot/Restore/Delete state options:\n"+
			"      --type=TYPE            Specifies the activation module that should be\n"+
			"                             used, such as echo or process.\n"+
			"      --arguments=ARGUMENTS  Specifies the arguments passed to the Dysnomia\n"+
			"                             module, which is a string with key=value pairs\n"+
			"      --container=CONTAINER  Name of the container in which the component is\n"+
			"                             managed. If omitted it will default to the same\n"+
			"                             value as the type.\n" +
			"\n"+
			"Query all snapshots/Query latest snapshot options:\n"+
			"  -C, --container=CONTAINER  Name of the container in which the component is managed\n"+
			"  -c, --component=COMPONENT  Name of the component hosted in a container\n"+
			"\n"+
			"Clean snapshots options:\n"+
			"      --keep=NUM             Amount of snapshot generations to keep. Defaults\n"+
			"                             to: 1\n"+
			"  -C, --container=CONTAINER  Name of the container to filter on\n"+
			"  -c, --component=COMPONENT  Name of the component to filter on\n"+
			"\n"+
			"Environment:\n"+
			"  DISNIX_PROFILE             Sets the name of the profile that stores the\n"+
			"                             manifest on the coordinator machine and the\n"+
			"                             deployed services per machine on each target\n"+
			"                             (Defaults to: default)\n"+
			"  DYSNOMIA_STATEDIR          Specifies where the snapshots must be stored on the\n"+
			"                             coordinator machine (defaults to: /var/dysnomia)\n");
	}

	private static void printVersion()
	{
		System.out.println("disnix-soap-client (DisnixWebService 0.6)\n");
		System.out.println("Copyright (C) 2008-2019 Sander van der Burg");
	}
	
	/**
	 * Prints the given string array through the given print stream using a
	 * specific separator.
	 *
	 * @param string Array of strings
	 * @param printStream Print stream, such as the standard output or standard error
	 * @param separator Separator between strings, such as a line feed
	 */
	private static void printStringArray(String[] string, PrintStream printStream, String separator)
	{
		for(int i = 0; i < string.length; i++)
		{
			printStream.print(string[i]);
			printStream.print(separator);
		}
	}
	
	/**
	 * Main method, which is executed when this tool is invoked from the command line.
	 * 
	 * @param args Array of command-line parameters
	 */
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
		CmdLineParser.Option opt_snapshot = parser.addBooleanOption("snapshot");
		CmdLineParser.Option opt_restore = parser.addBooleanOption("restore");
		CmdLineParser.Option opt_delete_state = parser.addBooleanOption("delete-state");
		CmdLineParser.Option opt_query_all_snapshots = parser.addBooleanOption("query-all-snapshots");
		CmdLineParser.Option opt_query_latest_snapshot = parser.addBooleanOption("query-latest-snapshot");
		CmdLineParser.Option opt_print_missing_snapshots = parser.addBooleanOption("print-missing-snapshots");
		CmdLineParser.Option opt_import_snapshots = parser.addBooleanOption("import-snapshots");
		CmdLineParser.Option opt_export_snapshots = parser.addBooleanOption("export-snapshots");
		CmdLineParser.Option opt_resolve_snapshots = parser.addBooleanOption("resolve-snapshots");
		CmdLineParser.Option opt_clean_snapshots = parser.addBooleanOption("clean-snapshots");
		CmdLineParser.Option opt_capture_config = parser.addBooleanOption("capture-config");
		CmdLineParser.Option opt_shell = parser.addBooleanOption("shell");
		CmdLineParser.Option opt_help = parser.addBooleanOption('h', "help");
		CmdLineParser.Option opt_version = parser.addBooleanOption('v', "version");

		/* Other attributes */
		CmdLineParser.Option opt_target = parser.addStringOption('t', "target");
		CmdLineParser.Option opt_localfile = parser.addBooleanOption("localfile");
		CmdLineParser.Option opt_remotefile = parser.addBooleanOption("remotefile");
		CmdLineParser.Option opt_profile = parser.addStringOption('p', "profile");
		CmdLineParser.Option opt_delete_old = parser.addBooleanOption('d', "delete-old");
		CmdLineParser.Option opt_type = parser.addStringOption("type");
		CmdLineParser.Option opt_arguments = parser.addStringOption("arguments");
		CmdLineParser.Option opt_container = parser.addStringOption("container");
		CmdLineParser.Option opt_component = parser.addStringOption("component");
		CmdLineParser.Option opt_keep = parser.addIntegerOption("keep");
		CmdLineParser.Option opt_command = parser.addIntegerOption("command");
		
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
			Boolean value_snapshot = (Boolean)parser.getOptionValue(opt_snapshot);
			Boolean value_restore = (Boolean)parser.getOptionValue(opt_restore);
			Boolean value_delete_state = (Boolean)parser.getOptionValue(opt_delete_state);
			Boolean value_query_all_snapshots = (Boolean)parser.getOptionValue(opt_query_all_snapshots);
			Boolean value_query_latest_snapshot = (Boolean)parser.getOptionValue(opt_query_latest_snapshot);
			Boolean value_print_missing_snapshots = (Boolean)parser.getOptionValue(opt_print_missing_snapshots);
			Boolean value_import_snapshots = (Boolean)parser.getOptionValue(opt_import_snapshots);
			Boolean value_export_snapshots = (Boolean)parser.getOptionValue(opt_export_snapshots);
			Boolean value_resolve_snapshots = (Boolean)parser.getOptionValue(opt_resolve_snapshots);
			Boolean value_clean_snapshots = (Boolean)parser.getOptionValue(opt_clean_snapshots);
			Boolean value_capture_config = (Boolean)parser.getOptionValue(opt_capture_config);
			Boolean value_shell = (Boolean)parser.getOptionValue(opt_shell);
			Boolean value_help = (Boolean)parser.getOptionValue(opt_help);
			Boolean value_version = (Boolean)parser.getOptionValue(opt_version);

			String value_target = (String)parser.getOptionValue(opt_target);
			Boolean value_localfile = (Boolean)parser.getOptionValue(opt_localfile);
			Boolean value_remotefile = (Boolean)parser.getOptionValue(opt_remotefile);
			String value_profile = (String)parser.getOptionValue(opt_profile);
			Boolean value_delete_old = (Boolean)parser.getOptionValue(opt_delete_old);
			String value_type = (String)parser.getOptionValue(opt_type);
			Vector<String> value_arguments = parser.getOptionValues(opt_arguments);
			String value_component = (String)parser.getOptionValue(opt_component);
			String value_container = (String)parser.getOptionValue(opt_container);
			Integer keep = (Integer)parser.getOptionValue(opt_keep);
			String command = (String)parser.getOptionValue(opt_command);
			
			String[] derivation = parser.getRemainingArgs();
			
			/* Display usage or version if requested */
			
			if(value_help != null)
			{
				printUsage();
				System.exit(0);
			}
			else if(value_version != null)
			{
				printVersion();
				System.exit(0);
			}
			
			/* Validate command line options */
			
			if(value_target == null)
			{
				System.err.println("ERROR: A targetEPR must be specified!");
				System.exit(1);
			}
			
			/* Create SOAP connection interface */
			
			DisnixInterface disnixInterface = new DisnixInterface(value_target);
			
			/* Execute operation */
			
			if(value_import != null)
			{
				if(value_remotefile != null)
					disnixInterface.importm(derivation[0]);
				else if(value_localfile != null)
					disnixInterface.importLocalFile(derivation[0]);
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
					String result = disnixInterface.exportRemoteFile(derivation);
					System.out.println(result);
				}
				else if(value_localfile != null)
				{
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
				String[] result = disnixInterface.printInvalid(derivation);
				printStringArray(result, System.out, "\n");
			}
			else if(value_realise != null)
			{
				String[] result = disnixInterface.realise(derivation);
				printStringArray(result, System.out, "\n");
			}
			else if(value_set != null)
			{
				String profile;
				
				if(value_profile == null)
					profile = "default";
				else
					profile = value_profile;
				
				disnixInterface.set(profile, derivation[0]);
			}
			else if(value_query_installed != null)
			{
				String profile;
				
				if(value_profile == null)
					profile = "default";
				else
					profile = value_profile;
				
				String[] result = disnixInterface.queryInstalled(profile);
				printStringArray(result, System.out, "\n");
			}
			else if(value_query_requisites != null)
			{
				String[] result = disnixInterface.queryRequisites(derivation);
				printStringArray(result, System.out, "\n");
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
				String container;
				
				if(value_container == null)
					container = value_type;
				else
					container = value_container;
				
				/* Convert arguments vector to array */
				String[] arguments = new String[value_arguments.size()];
				value_arguments.toArray(arguments);
				
				/* Invoke operation */
				disnixInterface.activate(derivation[0], container, value_type, arguments);
			}
			else if(value_deactivate != null)
			{
				String container;
				
				if(value_container == null)
					container = value_type;
				else
					container = value_container;
				
				/* Convert arguments vector to array */
				String[] arguments = new String[value_arguments.size()];
				value_arguments.toArray(arguments);
				
				/* Invoke operation */
				disnixInterface.deactivate(derivation[0], container, value_type, arguments);
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
			else if(value_snapshot != null)
			{
				String container;
				
				if(value_container == null)
					container = value_type;
				else
					container = value_container;
				
				/* Convert arguments vector to array */
				String[] arguments = new String[value_arguments.size()];
				value_arguments.toArray(arguments);
				
				disnixInterface.snapshot(derivation[0], container, value_type, arguments);
			}
			else if(value_restore != null)
			{
				String container;
				
				if(value_container == null)
					container = value_type;
				else
					container = value_container;
				
				/* Convert arguments vector to array */
				String[] arguments = new String[value_arguments.size()];
				value_arguments.toArray(arguments);
				
				disnixInterface.restore(derivation[0], container, value_type, arguments);
			}
			else if(value_delete_state != null)
			{
				String container;
				
				if(value_container == null)
					container = value_type;
				else
					container = value_container;
				
				/* Convert arguments vector to array */
				String[] arguments = new String[value_arguments.size()];
				value_arguments.toArray(arguments);
				
				disnixInterface.deleteState(derivation[0], container, value_type, arguments);
			}
			else if(value_query_all_snapshots != null)
			{
				String[] result = disnixInterface.queryAllSnapshots(value_container, value_component);
				printStringArray(result, System.out, "\n");
			}
			else if(value_query_latest_snapshot != null)
			{
				String[] result = disnixInterface.queryLatestSnapshot(value_container, value_component);
				printStringArray(result, System.out, "\n");
			}
			else if(value_print_missing_snapshots != null)
			{
				String[] result = disnixInterface.printMissingSnapshots(derivation);
				printStringArray(result, System.out, "\n");
			}
			else if(value_resolve_snapshots != null)
			{
				String[] result = disnixInterface.resolveSnapshots(derivation);
				printStringArray(result, System.out, "\n");
			}
			else if(value_import_snapshots != null)
			{
				if(value_remotefile != null)
					disnixInterface.importSnapshots(value_container, value_component, derivation);
				else if(value_localfile != null)
					disnixInterface.importLocalSnapshots(value_container, value_component, derivation[0]);
				else
				{
					System.err.println("ERROR: Either --localfile or --remotefile should be specified!");
					System.exit(1);
				}
			}
			else if(value_export_snapshots != null)
			{
				String result = disnixInterface.exportRemoteSnapshots(derivation);
				System.out.println(result);
			}
			else if(value_clean_snapshots != null)
			{
				if(keep == null)
					keep = 1;
				
				disnixInterface.cleanSnapshots(keep, value_container, value_component);
			}
			else if(value_capture_config != null)
			{
				String result = disnixInterface.captureConfig();
				System.out.println(result);
			}
			else if(value_shell != null)
			{
				System.err.println("ERROR: This operation is unsupported by this client!");
				System.exit(1);
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
