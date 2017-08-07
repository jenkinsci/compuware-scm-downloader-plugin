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
 * 
 */
package com.compuware.jenkins.scm;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Properties;
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;
import com.compuware.jenkins.common.configuration.CpwrGlobalConfiguration;
import com.compuware.jenkins.common.configuration.HostConnection;
import com.compuware.jenkins.common.utils.ArgumentUtils;
import com.compuware.jenkins.scm.utils.Constants;
import hudson.EnvVars;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.remoting.VirtualChannel;
import hudson.util.ArgumentListBuilder;

/**
 * Class used to download PDS members. This class will utilize the Topaz command line interface to do the download.
 */
public class EndevorDownloader extends AbstractDownloader
{
	private EndevorConfiguration m_endevorConfig;

	/**
	 * Constructs an Endevor downloader for the given Endevor configuration.
	 * 
	 * @param config
	 *            the Endevor configuration
	 */
	public EndevorDownloader(EndevorConfiguration config)
	{
		m_endevorConfig = config;
	}

	/* 
	 * (non-Javadoc)
	 * @see com.compuware.jenkins.scm.AbstractDownloader#getSource(hudson.model.AbstractBuild, hudson.Launcher, hudson.FilePath, hudson.model.BuildListener, java.io.File, java.lang.String)
	 */
	@Override
	public boolean getSource(Run<?, ?> build, Launcher launcher, FilePath workspaceFilePath, TaskListener listener,
			File changelogFile) throws InterruptedException, IOException
	{
		PrintStream logger = listener.getLogger();
		CpwrGlobalConfiguration globalConfig = CpwrGlobalConfiguration.get();

		ArgumentListBuilder args = new ArgumentListBuilder();
		EnvVars env = build.getEnvironment(listener);
		VirtualChannel vChannel = launcher.getChannel();
		Properties remoteProperties = vChannel.call(new RemoteSystemProperties());
		String remoteFileSeparator = remoteProperties.getProperty(Constants.FILE_SEPARATOR);

		String osFile = launcher.isUnix() ? Constants.TOPAZ_CLI_SH : Constants.TOPAZ_CLI_BAT;

		String cliScriptFile = globalConfig.getTopazCLILocation(launcher) + remoteFileSeparator + osFile;
		logger.println("cliScriptFile: " + cliScriptFile); //$NON-NLS-1$

		String cliScriptFileRemote = new FilePath(vChannel, cliScriptFile).getRemote();
		logger.println("cliScriptFileRemote: " + cliScriptFileRemote); //$NON-NLS-1$

		HostConnection connection = globalConfig.getHostConnection(m_endevorConfig.getConnectionId());
		String host = ArgumentUtils.escapeForScript(connection.getHost());
		String port = ArgumentUtils.escapeForScript(connection.getPort());
		String codePage = connection.getCodePage();
		String timeout = ArgumentUtils.escapeForScript(connection.getTimeout());

		StandardUsernamePasswordCredentials credentials = globalConfig.getLoginInformation(build.getParent(),
				m_endevorConfig.getCredentialsId());
		String userId = ArgumentUtils.escapeForScript(credentials.getUsername());
		String password = ArgumentUtils.escapeForScript(credentials.getPassword().getPlainText());

		String cdDatasets = ArgumentUtils.escapeForScript(convertFilterPattern(m_endevorConfig.getFilterPattern()));
		String fileExtension = ArgumentUtils.escapeForScript(m_endevorConfig.getFileExtension());
		String topazCliWorkspace = workspaceFilePath.getRemote() + remoteFileSeparator + Constants.TOPAZ_CLI_WORKSPACE;
		logger.println("topazCliWorkspace: " + topazCliWorkspace); //$NON-NLS-1$

		args.add(cliScriptFileRemote);
		args.add(Constants.HOST_PARM, host);
		args.add(Constants.PORT_PARM, port);
		args.add(Constants.CODE_PAGE_PARM, codePage);
		args.add(Constants.TIMEOUT_PARM, timeout);
		args.add(Constants.USERID_PARM, userId);
		args.add(Constants.PW_PARM);
		args.add(password, true);
		args.add(Constants.FILTER_PARM, cdDatasets);
		args.add(Constants.TARGET_FOLDER_PARM, workspaceFilePath.getRemote());
		args.add(Constants.SCM_TYPE_PARM, Constants.ENDEVOR);
		args.add(Constants.FILE_EXT_PARM, fileExtension);
		args.add(Constants.DATA_PARM, topazCliWorkspace);

		FilePath workDir = new FilePath(vChannel, workspaceFilePath.getRemote());
		workDir.mkdirs();

		int exitValue = launcher.launch().cmds(args).envs(env).stdout(listener.getLogger()).pwd(workDir).join();
		logger.println("Call " + osFile + " exited with value = " + exitValue); //$NON-NLS-1$ //$NON-NLS-2$

		return (exitValue == 0);
	}
}