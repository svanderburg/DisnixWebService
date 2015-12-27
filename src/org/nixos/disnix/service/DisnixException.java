package org.nixos.disnix.service;

import java.io.*;

/**
 * An exception that bundles the output of the logfile of the corresponding job
 */
public class DisnixException extends Exception
{
	/**
	 * Constructs a Disnix exception by consulting the corresponding logfile of the job
	 * @param pid Id of the job
	 * @param logdir Path to the directory in which the logfiles are stored
	 * @return A DisnixException instance
	 * @throws FileNotFoundException If the log file cannot be found
	 * @throws IOException If the log file cannot be read
	 */
	public static DisnixException constructDisnixException(int pid, String logdir) throws FileNotFoundException, IOException
	{
		String line;
		String failure = "";
		BufferedReader br = new BufferedReader(new FileReader(logdir + File.separatorChar + pid));
		
		while((line = br.readLine()) != null)
			failure += line + "\n";
		
		br.close();
		
		return new DisnixException(failure);
	}
	
	private DisnixException(String failure)
	{
		super(failure);
	}
}
