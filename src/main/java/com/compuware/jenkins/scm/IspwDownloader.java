/**
 * The MIT License (MIT)
 * 
 * Copyright (c) 2015 - 2018 Compuware Corporation
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
import com.compuware.jenkins.common.configuration.CpwrGlobalConfiguration;
import com.compuware.jenkins.common.configuration.HostConnection;
import com.compuware.jenkins.common.utils.ArgumentUtils;
import com.compuware.jenkins.common.utils.CommonConstants;
import com.compuware.jenkins.scm.utils.ScmConstants;
import hudson.AbortException;
import hudson.EnvVars;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.remoting.VirtualChannel;
import hudson.util.ArgumentListBuilder;

/**
 * Class used to download ISPW components. This class will utilize the Topaz command line interface to do the download.
 */
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
		CpwrGlobalConfiguration globalConfig = CpwrGlobalConfiguration.get();
		VirtualChannel vChannel = launcher.getChannel();
		Properties remoteProperties = vChannel.call(new RemoteSystemProperties());
		String remoteFileSeparator = remoteProperties.getProperty(CommonConstants.FILE_SEPARATOR_PROPERTY_KEY);
		String osFile = launcher.isUnix() ? ScmConstants.SCM_DOWNLOADER_CLI_SH : ScmConstants.SCM_DOWNLOADER_CLI_BAT;

		String cliScriptFile = globalConfig.getTopazCLILocation(launcher) + remoteFileSeparator + osFile;
		logger.println("cliScriptFile: " + cliScriptFile); //$NON-NLS-1$
		String cliScriptFileRemote = new FilePath(vChannel, cliScriptFile).getRemote();
		logger.println("cliScriptFileRemote: " + cliScriptFileRemote); //$NON-NLS-1$
		HostConnection connection = globalConfig.getHostConnection(m_ispwConfig.getConnectionId());
		String host = ArgumentUtils.escapeForScript(connection.getHost());
		String port = ArgumentUtils.escapeForScript(connection.getPort());
		String codePage = connection.getCodePage();
		String timeout = ArgumentUtils.escapeForScript(connection.getTimeout());
		StandardUsernamePasswordCredentials credentials = globalConfig.getLoginInformation(build.getParent(),
				m_ispwConfig.getCredentialsId());
		String userId = ArgumentUtils.escapeForScript(credentials.getUsername());
		String password = ArgumentUtils.escapeForScript(credentials.getPassword().getPlainText());
		String targetFolder = ArgumentUtils.escapeForScript(workspaceFilePath.getRemote());
		String topazCliWorkspace = workspaceFilePath.getRemote() + remoteFileSeparator + CommonConstants.TOPAZ_CLI_WORKSPACE;
		logger.println("TopazCliWorkspace: " + topazCliWorkspace); //$NON-NLS-1$
		String serverStream = ArgumentUtils.escapeForScript(m_ispwConfig.getServerStream());
		String serverApp = ArgumentUtils.escapeForScript(m_ispwConfig.getServerApplication());
		String serverLevel = ArgumentUtils.escapeForScript(m_ispwConfig.getServerLevel());
		String levelOption = ArgumentUtils.escapeForScript(m_ispwConfig.getLevelOption());
		String filterFiles = ArgumentUtils.escapeForScript(m_ispwConfig.getFilterFiles());
		String filterFolders = ArgumentUtils.escapeForScript(m_ispwConfig.getFilterFolders());

		// build the list of arguments to pass to the CLI
		ArgumentListBuilder args = new ArgumentListBuilder();
		args.add(cliScriptFileRemote);
		args.add(CommonConstants.HOST_PARM, host);
		args.add(CommonConstants.PORT_PARM, port);
		args.add(CommonConstants.USERID_PARM, userId);
		args.add(CommonConstants.PW_PARM);
		args.add(password, true);
		args.add(CommonConstants.CODE_PAGE_PARM, codePage);
		args.add(CommonConstants.TIMEOUT_PARM, timeout);
		args.add(ScmConstants.SCM_TYPE_PARM, ScmConstants.ISPW);
		args.add(CommonConstants.TARGET_FOLDER_PARM, targetFolder);
		args.add(CommonConstants.DATA_PARM, topazCliWorkspace);
		args.add(ScmConstants.ISPW_SERVER_STREAM_PARAM, serverStream);
		args.add(ScmConstants.ISPW_SERVER_APP_PARAM, serverApp);
		args.add(ScmConstants.ISPW_SERVER_LEVEL_PARAM, serverLevel);
		args.add(ScmConstants.ISPW_LEVEL_OPTION_PARAM, levelOption);
		args.add(ScmConstants.ISPW_FILTER_FILES_PARAM, filterFiles);
		args.add(ScmConstants.ISPW_FILTER_FOLDERS_PARAM, filterFolders);

		String runtimeConfig = m_ispwConfig.getServerConfig();
		if (!runtimeConfig.isEmpty())
		{
			runtimeConfig = ArgumentUtils.escapeForScript(runtimeConfig);
			args.add(ScmConstants.ISPW_SERVER_CONFIG_PARAM, runtimeConfig);
		}

		String componentName = m_ispwConfig.getFolderName();
		if (!componentName.isEmpty())
		{
			componentName = ArgumentUtils.escapeForScript(componentName);
			args.add(ScmConstants.ISPW_FOLDER_NAME_PARAM, componentName);
		}

		String componentType = m_ispwConfig.getComponentType();
		if (!componentType.isEmpty())
		{
			componentType = ArgumentUtils.escapeForScript(componentType);
			args.add(ScmConstants.ISPW_COMPONENT_TYPE_PARAM, componentType);
		}

		// create the CLI workspace (in case it doesn't already exist)
		EnvVars env = build.getEnvironment(listener);
		FilePath workDir = new FilePath(vChannel, workspaceFilePath.getRemote());
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