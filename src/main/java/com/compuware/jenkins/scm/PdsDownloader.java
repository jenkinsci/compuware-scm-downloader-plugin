package com.compuware.jenkins.scm;

import hudson.FilePath;
import hudson.model.BuildListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import org.apache.commons.lang.SystemUtils;
import com.compuware.jenkins.scm.utils.Constants;

/**
 * Class used to download PDS members. This class will utilize the Topaz command line interface to do the download.
 */
public class PdsDownloader extends AbstractDownloader
{
	private PdsConfiguration m_pdsConfig;

	/**
	 * Constructs the PDS downloader for the given configuration.
	 * 
	 * @param config
	 *            PDS configuration
	 */
	public PdsDownloader(PdsConfiguration config)
	{
		m_pdsConfig = config;
	}

	/**
	 * Download the PDS members for the datasets specified in the Jenkins configuration.
	 * 
	 * @param listener
	 *            Build listener
	 * @param workspaceFilePath
	 *            Workspace file path
	 * @param filterPattern
	 *            Source filter pattern
	 * @return TRUE if able to retrieve source
	 */
	public boolean getSource(BuildListener listener, FilePath workspaceFilePath, String filterPattern)
	{
		boolean rtnValue = true;

		String datasets = convertFilterPattern(filterPattern);
		listener.getLogger().println("Comma delimited datasets: " + datasets); //$NON-NLS-1$

		// Calling the Topaz HCI code that will run as an OSGI instance. The OSGI instance has a command line interface to
		// download PDS members.
		String osFile = Constants.TOPAZ_CLI_BAT;
		if (SystemUtils.IS_OS_LINUX == true)
		{
			osFile = Constants.TOPAZ_CLI_SH;
		}
		String cliBatchFile = m_pdsConfig.getTopazCLILocation() + File.separator + osFile;
		String topazCliWorkspace = workspaceFilePath.getRemote() + File.separator + Constants.TOPAZ_CLI_WORKSPACE;
		listener.getLogger().println("TopazCLI workspace: " + topazCliWorkspace); //$NON-NLS-1$

		ProcessBuilder processBldr = new ProcessBuilder(cliBatchFile, Constants.HOST_PARM, m_pdsConfig.getHost(),
				Constants.PORT_PARM, m_pdsConfig.getPort(), Constants.USERID_PARM, m_pdsConfig.getLoginInformation()
						.getUsername(), Constants.PASSWORD_PARM,
				m_pdsConfig.getLoginInformation().getPassword().getPlainText(), Constants.FILTER_PARM, wrapInQuotes(datasets),
				Constants.TARGET_FOLDER_PARM, wrapInQuotes(workspaceFilePath.getRemote()), Constants.SCM_TYPE_PARM,
				Constants.PDS, Constants.FILE_EXT_PARM, m_pdsConfig.getFileExtension(), Constants.CODE_PAGE_PARM, m_pdsConfig.getCodePage(), Constants.DATA_PARM,
				wrapInQuotes(topazCliWorkspace));

		try
		{
			// invoke the bat file that will start the OSGI instance.
			Process process = processBldr.start();

			// Get the input stream connected to the normal output of the process so we can right all the messages to the
			// Jenkins console output.
			InputStream is = process.getInputStream();
			InputStreamReader isr = new InputStreamReader(is);
			BufferedReader br = new BufferedReader(isr);
			String line;

			while ((line = br.readLine()) != null)
			{
				listener.getLogger().println(line);
			}

			// check the return value of the process
			int exitValue = process.waitFor();
			listener.getLogger().println("Call TopazCLI.bat exited with exit value = " + exitValue); //$NON-NLS-1$
			if (exitValue != 0)
			{
				rtnValue = false;
			}
		}
		catch (IOException e)
		{
			rtnValue = false;
			listener.getLogger().println(e.getMessage());
		}
		catch (InterruptedException e)
		{
			rtnValue = false;
			listener.getLogger().println(e.getMessage());
		}

		return rtnValue;
	}
}