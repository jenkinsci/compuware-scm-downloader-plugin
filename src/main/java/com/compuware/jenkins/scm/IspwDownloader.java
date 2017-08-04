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
public class IspwDownloader extends AbstractDownloader
{
	private IspwConfiguration m_ispwConfig;

	/**
	 * Constructs the PDS downloader for the given configuration.
	 * 
	 * @param config
	 *            PDS configuration
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
		ArgumentListBuilder args = new ArgumentListBuilder();
		EnvVars env = build.getEnvironment(listener);
		CpwrGlobalConfiguration globalConfig = CpwrGlobalConfiguration.get();

		VirtualChannel vChannel = launcher.getChannel();
		Properties remoteProperties = vChannel.call(new RemoteSystemProperties());
		String remoteFileSeparator = remoteProperties.getProperty(Constants.FILE_SEPARATOR);

		String osFile = launcher.isUnix() ? Constants.TOPAZ_CLI_SH : Constants.TOPAZ_CLI_BAT;
		String cliLocation = globalConfig.getTopazCLILocation(launcher);

		String cliBatchFile = cliLocation + remoteFileSeparator + osFile;
		listener.getLogger().println("cliBatchFile path: " + cliBatchFile); //$NON-NLS-1$

		FilePath cliBatchFileRemote = new FilePath(vChannel, cliBatchFile);
		listener.getLogger().println("cliBatchFile remote path: " + cliBatchFileRemote.getRemote()); //$NON-NLS-1$

		args.add(cliBatchFileRemote.getRemote());

		String topazCliWorkspace = workspaceFilePath.getRemote() + remoteFileSeparator + Constants.TOPAZ_CLI_WORKSPACE;
		listener.getLogger().println("TopazCLI workspace: " + topazCliWorkspace); //$NON-NLS-1$

		HostConnection connection = globalConfig.getHostConnection(m_ispwConfig.getConnectionId());
		String host = ArgumentUtils.escapeForScript(connection.getHost());
		String port = ArgumentUtils.escapeForScript(connection.getPort());
		String codePage = connection.getCodePage();
		String timeout = ArgumentUtils.escapeForScript(connection.getTimeout());

		StandardUsernamePasswordCredentials credentials = globalConfig.getLoginInformation(build.getParent(),
				m_ispwConfig.getCredentialsId());
		String username = ArgumentUtils.escapeForScript(credentials.getUsername());
		String password = ArgumentUtils.escapeForScript(credentials.getPassword().getPlainText());

		String serverStream = ArgumentUtils.escapeForScript(m_ispwConfig.getServerStream());
		String serverApp = ArgumentUtils.escapeForScript(m_ispwConfig.getServerApplication());
		String serverLevel = ArgumentUtils.escapeForScript(m_ispwConfig.getServerLevel());
		String levelOption = ArgumentUtils.escapeForScript(m_ispwConfig.getLevelOption());

		args.add(Constants.HOST_PARM, host);
		args.add(Constants.PORT_PARM, port);
		args.add(Constants.USERID_PARM, username);
		args.add(Constants.PW_PARM);
		args.add(password, true);
		args.add(Constants.ISPW_SERVER_STREAM_PARAM, serverStream);
		args.add(Constants.ISPW_SERVER_APP_PARAM, serverApp);
		args.add(Constants.ISPW_SERVER_LEVEL_PARAM, serverLevel);
		args.add(Constants.ISPW_LEVEL_OPTION_PARAM, levelOption);
		args.add(Constants.TIMEOUT_PARM, timeout);

		String runtimeConfig = m_ispwConfig.getServerConfig();
		String componentName = m_ispwConfig.getFilterName();
		String componentType = m_ispwConfig.getFilterType();

		if (!runtimeConfig.isEmpty())
		{
			runtimeConfig = ArgumentUtils.escapeForScript(runtimeConfig);
			args.add(Constants.ISPW_SERVER_CONFIG_PARAM, runtimeConfig);
		}

		if (!componentName.isEmpty())
		{
			componentName = ArgumentUtils.escapeForScript(componentName);
			args.add(Constants.ISPW_FILTER_NAME_PARAM, componentName);
		}

		if (!componentType.isEmpty())
		{
			componentType = ArgumentUtils.escapeForScript(componentType);
			args.add(Constants.ISPW_FILTER_TYPE_PARAM, componentType);
		}

		args.add(Constants.TARGET_FOLDER_PARM, workspaceFilePath.getRemote());
		args.add(Constants.SCM_TYPE_PARM, Constants.ISPW);
		args.add(Constants.CODE_PAGE_PARM, codePage);
		args.add(Constants.DATA_PARM, topazCliWorkspace);

		FilePath workDir = new FilePath(vChannel, workspaceFilePath.getRemote());
		workDir.mkdirs();
		int exitValue = launcher.launch().cmds(args).envs(env).stdout(listener.getLogger()).pwd(workDir).join();

		return (exitValue == 0);
	}
}
