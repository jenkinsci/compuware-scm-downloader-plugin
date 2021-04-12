/**
 * The MIT License (MIT)
 * 
 * Copyright (c) 2015 - 2019 Compuware Corporation
 * (c) Copyright 2015 - 2019, 2021 BMC Software, Inc.
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
import java.security.KeyStoreException;
import java.util.Properties;
import org.apache.commons.lang.StringUtils;
import com.cloudbees.plugins.credentials.common.StandardCertificateCredentials;
import com.cloudbees.plugins.credentials.common.StandardCredentials;
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
	private AbstractIspwConfiguration ispwConfiguration;
	private IspwConfiguration ispwRepositoryConfig;
	private IspwContainerConfiguration ispwContainerConfig;

	/**
	 * Constructor.
	 * 
	 * @param config
	 *            the <code>AbstractIspwConfiguration</code> to use for the download
	 */
	public IspwDownloader(AbstractIspwConfiguration config)
	{
		ispwConfiguration = config;
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
		
		assert launcher!=null;
		VirtualChannel vChannel = launcher.getChannel();
		
		assert vChannel!=null;
		Properties remoteProperties = vChannel.call(new RemoteSystemProperties());
		String remoteFileSeparator = remoteProperties.getProperty(CommonConstants.FILE_SEPARATOR_PROPERTY_KEY);
		String osFile = launcher.isUnix() ? ScmConstants.SCM_DOWNLOADER_CLI_SH : ScmConstants.SCM_DOWNLOADER_CLI_BAT;

		String cliScriptFile = globalConfig.getTopazCLILocation(launcher) + remoteFileSeparator + osFile;
		logger.println("cliScriptFile: " + cliScriptFile); //$NON-NLS-1$
		String cliScriptFileRemote = new FilePath(vChannel, cliScriptFile).getRemote();
		logger.println("cliScriptFileRemote: " + cliScriptFileRemote); //$NON-NLS-1$

		ArgumentListBuilder args = new ArgumentListBuilder();
		
		// server args
		HostConnection connection = globalConfig.getHostConnection(ispwConfiguration.getConnectionId());
		String host = ArgumentUtils.escapeForScript(connection.getHost());
		String port = ArgumentUtils.escapeForScript(connection.getPort());
		String protocol = connection.getProtocol();
		String codePage = connection.getCodePage();
		String timeout = ArgumentUtils.escapeForScript(connection.getTimeout());
		StandardCredentials  credentials = globalConfig.getUserLoginInformation(build.getParent(),
				ispwConfiguration.getCredentialsId());
		String password = null;
		String userId = null;
		String  certificateStr = null;
		
		if (credentials instanceof StandardUsernamePasswordCredentials ) {
			userId = ArgumentUtils.escapeForScript(((StandardUsernamePasswordCredentials) credentials).getUsername());
			password = ArgumentUtils.escapeForScript(((StandardUsernamePasswordCredentials) credentials).getPassword().getPlainText());	
		}
		
		if(credentials instanceof StandardCertificateCredentials){
			StandardCertificateCredentials credentialsCer = (StandardCertificateCredentials)credentials;
			try {
				certificateStr = globalConfig.getCertificate(credentialsCer);
			} catch (KeyStoreException e) {
				throw new AbortException(String.format("Unable to get the certificate Exception: %s", e.getMessage())); //$NON-NLS-1$ //$NON-NLS-2$
			}
			password = ArgumentUtils.escapeForScript(credentialsCer.getPassword().getPlainText());
		}
		String targetFolder = ArgumentUtils.escapeForScript(workspaceFilePath.getRemote());
		String topazCliWorkspace = workspaceFilePath.getRemote() + remoteFileSeparator + CommonConstants.TOPAZ_CLI_WORKSPACE;
		logger.println("TopazCliWorkspace: " + topazCliWorkspace); //$NON-NLS-1$

		// filter args
		String serverStream = StringUtils.EMPTY;
		String serverApp = StringUtils.EMPTY;
		String serverLevel = StringUtils.EMPTY;
		String levelOption = StringUtils.EMPTY;
		String filterFiles = StringUtils.EMPTY;
		String filterFolders = StringUtils.EMPTY;
		String containerName = StringUtils.EMPTY;
		String containerType = StringUtils.EMPTY;
		String downloadAll = StringUtils.EMPTY;
		
		boolean ispwDownloadInclBool = false;
		String ispwDownloadIncl = StringUtils.EMPTY;
		
		boolean ispwDownloadWithCompileOnlyBool = false;
		String ispwDownloadWithCompileOnly = StringUtils.EMPTY;

		if (ispwConfiguration instanceof IspwConfiguration)
		{
			ispwRepositoryConfig = (IspwConfiguration) ispwConfiguration;
			
			serverStream = ArgumentUtils.escapeForScript(ispwRepositoryConfig.getServerStream());
			serverApp = ArgumentUtils.escapeForScript(ispwRepositoryConfig.getServerApplication());
			serverLevel = ArgumentUtils.escapeForScript(ispwRepositoryConfig.getServerLevel());
			levelOption = ArgumentUtils.escapeForScript(ispwRepositoryConfig.getLevelOption());
			filterFiles = ArgumentUtils.escapeForScript(ispwRepositoryConfig.getFilterFiles());
			filterFolders = ArgumentUtils.escapeForScript(ispwRepositoryConfig.getFilterFolders());
			downloadAll = ArgumentUtils.escapeForScript(Boolean.toString(ispwRepositoryConfig.getIspwDownloadAll()));
			
			ispwDownloadInclBool = ispwRepositoryConfig.getIspwDownloadIncl();
			ispwDownloadIncl = ArgumentUtils.escapeForScript(Boolean.toString(ispwRepositoryConfig.getIspwDownloadIncl()));
			
			ispwDownloadWithCompileOnlyBool = ispwRepositoryConfig.getIspwDownloadWithCompileOnly();
			ispwDownloadWithCompileOnly = ArgumentUtils.escapeForScript(Boolean.toString(ispwRepositoryConfig.getIspwDownloadWithCompileOnly()));
			
			String sourceLocation = ispwRepositoryConfig.getTargetFolder();
			if (StringUtils.isNotEmpty(sourceLocation))
			{
				targetFolder = ArgumentUtils.resolvePath(sourceLocation, workspaceFilePath.getRemote());
				targetFolder = targetFolder.replaceAll("'", StringUtils.EMPTY); //$NON-NLS-1$
				logger.println("Source download folder: " + targetFolder); //$NON-NLS-1$
			}
		}
		else if (ispwConfiguration instanceof IspwContainerConfiguration)
		{
			ispwContainerConfig = (IspwContainerConfiguration) ispwConfiguration;
			
			String sourceLocation = ispwContainerConfig.getTargetFolder();
			if (StringUtils.isNotEmpty(sourceLocation))
			{
				targetFolder = ArgumentUtils.resolvePath(sourceLocation, workspaceFilePath.getRemote());
				targetFolder = targetFolder.replaceAll("'", StringUtils.EMPTY); //$NON-NLS-1$
				logger.println("Source download folder: " + targetFolder); //$NON-NLS-1$
			}

			containerName = ArgumentUtils.escapeForScript(ispwContainerConfig.getContainerName());
			containerType = ArgumentUtils.escapeForScript(ispwContainerConfig.getContainerType());
			downloadAll = ArgumentUtils.escapeForScript(Boolean.toString(ispwContainerConfig.getIspwDownloadAll()));
			
			ispwDownloadInclBool = ispwContainerConfig.getIspwDownloadIncl();
			ispwDownloadIncl = ArgumentUtils.escapeForScript(Boolean.toString(ispwContainerConfig.getIspwDownloadIncl()));
		}
		// build the list of arguments to pass to the CLI
		args.add(cliScriptFileRemote);
		args.add(CommonConstants.HOST_PARM, host);
		args.add(CommonConstants.PORT_PARM, port);
		if (userId != null) {
			args.add(CommonConstants.USERID_PARM, userId);
		}
		if (certificateStr != null) {
			args.add(CommonConstants.CERT_PARM, certificateStr);
		} else {
			args.add(CommonConstants.CERT_PARM, "");
		}
		args.add(CommonConstants.PW_PARM);
		args.add(password, true);
		args.add(CommonConstants.PROTOCOL_PARM, protocol);
		args.add(CommonConstants.CODE_PAGE_PARM, codePage);
		args.add(CommonConstants.TIMEOUT_PARM, timeout);
		args.add(CommonConstants.TARGET_FOLDER_PARM, targetFolder);
		args.add(CommonConstants.DATA_PARM, topazCliWorkspace);

		// optional and filter-specific args
		String runtimeConfig = ispwConfiguration.getServerConfig();
		
		if (!runtimeConfig.isEmpty())
		{
			runtimeConfig = ArgumentUtils.escapeForScript(runtimeConfig);
			args.add(ScmConstants.ISPW_SERVER_CONFIG_PARAM, runtimeConfig);
		}

		if (ispwRepositoryConfig != null)
		{
			// ISPW Repo-specific args
			args.add(ScmConstants.SCM_TYPE_PARM, ScmConstants.ISPW);
			args.add(ScmConstants.ISPW_SERVER_STREAM_PARAM, serverStream);
			args.add(ScmConstants.ISPW_SERVER_APP_PARAM, serverApp);
			args.add(ScmConstants.ISPW_SERVER_LEVEL_PARAM, serverLevel);
			args.add(ScmConstants.ISPW_LEVEL_OPTION_PARAM, levelOption);
			args.add(ScmConstants.ISPW_FILTER_FILES_PARAM, filterFiles);
			args.add(ScmConstants.ISPW_FILTER_FOLDERS_PARAM, filterFolders);

			// Optional args
			String componentName = ispwRepositoryConfig.getFolderName();
			if (!componentName.isEmpty())
			{
				componentName = ArgumentUtils.escapeForScript(componentName);
				args.add(ScmConstants.ISPW_FOLDER_NAME_PARAM, componentName);
			}

			String componentType = ispwRepositoryConfig.getComponentType();
			if (!componentType.isEmpty())
			{
				componentType = ArgumentUtils.escapeForScript(componentType);
				args.add(ScmConstants.ISPW_COMPONENT_TYPE_PARAM, componentType);
			}
			
			if (ispwDownloadWithCompileOnlyBool)
			{
				args.add(ScmConstants.ISPW_DOWNLOAD_WITH_COMPILE_ONLY, ispwDownloadWithCompileOnly);
			}
		}
		else if (ispwContainerConfig != null)
		{
			// ISPW Container-specific args
			args.add(ScmConstants.SCM_TYPE_PARM, ScmConstants.ISPWC);
			args.add(ScmConstants.ISPW_CONTAINER_NAME_PARAM, containerName);
			args.add(ScmConstants.ISPW_CONTAINER_TYPE_PARAM, containerType);

			// Optional args
			serverLevel = ispwContainerConfig.getServerLevel();
			if (!serverLevel.isEmpty())
			{
				serverLevel = ArgumentUtils.escapeForScript(serverLevel);
				args.add(ScmConstants.ISPW_SERVER_LEVEL_PARAM, serverLevel);
			}

			String componentType = ispwContainerConfig.getComponentType();
			if (!componentType.isEmpty())
			{
				componentType = ArgumentUtils.escapeForScript(componentType);
				args.add(ScmConstants.ISPW_COMPONENT_TYPE_PARAM, componentType);
			}
		}

		args.add(ScmConstants.ISPW_DOWNLOAD_ALL_PARAM, downloadAll);
		
		//only add the option if true to keep compatible with older version
		if(ispwDownloadInclBool)
		{
			args.add(ScmConstants.ISPW_DOWNLOAD_INCL_PARM, ispwDownloadIncl);
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