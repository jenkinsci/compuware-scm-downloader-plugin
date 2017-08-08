/**
 * The MIT License (MIT)
 * 
 * Copyright (c) 2017 Compuware Corporation
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions: The above copyright notice and this permission notice
 * shall be included in all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE
 * WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE,
 * ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package com.compuware.jenkins.scm;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Properties;
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;
import com.compuware.jenkins.scm.utils.Constants;
import hudson.AbortException;
import hudson.EnvVars;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.remoting.VirtualChannel;
import hudson.util.ArgumentListBuilder;

public class IspwDownloader extends AbstractDownloader
{
	// Member Variables
	private IspwConfiguration m_ispwConfig;

	/**
	 * Constructor.
	 * 
	 * @param config
	 *            the <code>ISPWConfiguration</code> to use for the download
	 */
	public IspwDownloader(IspwConfiguration config)
	{
		m_ispwConfig = config;
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.compuware.jenkins.scm.AbstractDownloader#getSource(hudson.model.Run, hudson.Launcher, hudson.FilePath, hudson.model.TaskListener, java.io.File)
	 */
	@Override
	public boolean getSource(Run<?, ?> build, Launcher launcher, FilePath workspaceFilePath, TaskListener listener,
			File changelogFile) throws InterruptedException, IOException
	{
		// obtain argument values to pass to the CLI
		PrintStream logger = listener.getLogger();
		VirtualChannel vChannel = launcher.getChannel();
		Properties remoteProperties = vChannel.call(new RemoteSystemProperties());
		String remoteFileSeparator = remoteProperties.getProperty(Constants.FILE_SEPARATOR);
        boolean isShell = launcher.isUnix();
		String osFile = isShell ? Constants.SCM_DOWNLOADER_CLI_SH : Constants.SCM_DOWNLOADER_CLI_BAT;
        
		String cliScriptFile = m_ispwConfig.getTopazCLILocation(launcher) + remoteFileSeparator + osFile;
		logger.println("cliScriptFile: " + cliScriptFile); //$NON-NLS-1$
		String cliScriptFileRemote = new FilePath(vChannel, cliScriptFile).getRemote();
		logger.println("cliScriptFileRemote: " + cliScriptFileRemote); //$NON-NLS-1$
		String host = escapeForScript(m_ispwConfig.getHost(), isShell);
		String port = escapeForScript(m_ispwConfig.getPort(), isShell);
		StandardUsernamePasswordCredentials credentials = m_ispwConfig.getLoginInformation(build.getParent());
		String userId = escapeForScript(credentials.getUsername(), isShell);
		String password = escapeForScript(credentials.getPassword().getPlainText(), isShell);
		String codePage = m_ispwConfig.getCodePage();
		String targetFolder = escapeForScript(workspaceFilePath.getRemote(), isShell);
		String topazCliWorkspace = workspaceFilePath.getRemote() + remoteFileSeparator + Constants.TOPAZ_CLI_WORKSPACE;
		logger.println("TopazCliWorkspace: " + topazCliWorkspace); //$NON-NLS-1$
		String serverStream = escapeForScript(m_ispwConfig.getServerStream(), isShell);
		String serverApp = escapeForScript(m_ispwConfig.getServerApplication(), isShell);
		String serverLevel = escapeForScript(m_ispwConfig.getServerLevel(), isShell);
		String levelOption = escapeForScript(m_ispwConfig.getLevelOption(), isShell);
				
		// build the list of arguments to pass to the CLI
		ArgumentListBuilder args = new ArgumentListBuilder();
		args.add(cliScriptFileRemote);
		args.add(Constants.HOST_PARM, host);
		args.add(Constants.PORT_PARM, port);
		args.add(Constants.USERID_PARM, userId);
		args.add(Constants.PW_PARM);
		args.add(password, true);
		args.add(Constants.CODE_PAGE_PARM, codePage);
		args.add(Constants.SCM_TYPE_PARM, Constants.ISPW);
		args.add(Constants.TARGET_FOLDER_PARM, targetFolder);
		args.add(Constants.DATA_PARM, topazCliWorkspace);
		args.add(Constants.ISPW_SERVER_STREAM_PARAM, serverStream);
		args.add(Constants.ISPW_SERVER_APP_PARAM, serverApp);
		args.add(Constants.ISPW_SERVER_LEVEL_PARAM, serverLevel);
		args.add(Constants.ISPW_LEVEL_OPTION_PARAM, levelOption);

		String runtimeConfig = m_ispwConfig.getServerConfig();
		if (!runtimeConfig.isEmpty())
		{
			runtimeConfig = escapeForScript(runtimeConfig, isShell);
			args.add(Constants.ISPW_SERVER_CONFIG_PARAM, runtimeConfig);
		}
		
		String componentName = m_ispwConfig.getFilterName();
		if (!componentName.isEmpty())
		{
			componentName = escapeForScript(componentName, isShell);
			args.add(Constants.ISPW_FILTER_NAME_PARAM, componentName);
		}
		
		String componentType = m_ispwConfig.getFilterType();
		if (!componentType.isEmpty())
		{
			componentType = escapeForScript(componentType, isShell);
			args.add(Constants.ISPW_FILTER_TYPE_PARAM, componentType);
		}
		
		// create the CLI workspace (in case it doesn't already exist)
		EnvVars env = build.getEnvironment(listener);
		FilePath workDir = new FilePath (vChannel, workspaceFilePath.getRemote());
		workDir.mkdirs();

		// invoke the CLI (execute the batch/shell script)
		int exitValue = launcher.launch().cmds(args).envs(env).stdout(logger).pwd(workDir).join();
		if (exitValue != 0)
		{
			throw new AbortException("Call " + osFile + " exited with value = " + exitValue); //$NON-NLS-1$ //$NON-NLS-2$
		}
		else
		{
			logger.println("Call " + osFile + " exited with value = " + exitValue); //$NON-NLS-1$ //$NON-NLS-2$
			return true;
		}
	}
}