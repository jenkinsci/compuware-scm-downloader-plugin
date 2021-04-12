/**
 * The MIT License (MIT)
 * 
 * Copyright (c) 2015 - 2019 Compuware Corporation
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software 
 * and associated documentation files (the "Software"), to deal in the Software without restriction, 
 * including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, 
 * and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, 
 * subject to the following conditions: The above copyright notice and this permission notice shall be 
 * included in all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT 
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. 
 * IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, 
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE 
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
*/
package com.compuware.jenkins.scm;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.security.KeyStoreException;
import java.util.Properties;
import java.util.UUID;

import org.apache.commons.lang.StringUtils;

import com.cloudbees.plugins.credentials.common.PasswordCredentials;
import com.cloudbees.plugins.credentials.common.StandardCertificateCredentials;
import com.cloudbees.plugins.credentials.common.StandardCredentials;
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;
import com.cloudbees.plugins.credentials.common.UsernameCredentials;
import com.compuware.jenkins.common.configuration.CpwrGlobalConfiguration;
import com.compuware.jenkins.common.configuration.HostConnection;
import com.compuware.jenkins.common.utils.ArgumentUtils;
import com.compuware.jenkins.common.utils.CLIVersionUtils;
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
 * Class used to download PDS members. This class will utilize the Topaz command line interface to do the download.
 */
public class PdsDownloader extends AbstractDownloader
{
	// Member Variables
	private PdsConfiguration pdsConfig;

	/**
	 * Constructs the PDS downloader for the given configuration.
	 * 
	 * @param config
	 *            PDS configuration
	 */
	public PdsDownloader(PdsConfiguration config)
	{
		pdsConfig = config;
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
		
		assert launcher != null;
        VirtualChannel vChannel = launcher.getChannel();

        //Check CLI compatibility
        FilePath cliDirectory = new FilePath(vChannel, globalConfig.getTopazCLILocation(launcher));
		String cliVersion = CLIVersionUtils.getCLIVersion(cliDirectory, ScmConstants.DOWNLOADER_MINIMUM_CLI_VERSION);
		CLIVersionUtils.checkCLICompatibility(cliVersion, ScmConstants.DOWNLOADER_MINIMUM_CLI_VERSION);

		assert vChannel != null;
        Properties remoteProperties = vChannel.call(new RemoteSystemProperties());
		String remoteFileSeparator = remoteProperties.getProperty(CommonConstants.FILE_SEPARATOR_PROPERTY_KEY);
		String osFile = launcher.isUnix() ? ScmConstants.SCM_DOWNLOADER_CLI_SH : ScmConstants.SCM_DOWNLOADER_CLI_BAT;

		String cliScriptFile = globalConfig.getTopazCLILocation(launcher) + remoteFileSeparator + osFile;
		logger.println("cliScriptFile: " + cliScriptFile); //$NON-NLS-1$
		String cliScriptFileRemote = new FilePath(vChannel, cliScriptFile).getRemote();
		logger.println("cliScriptFileRemote: " + cliScriptFileRemote); //$NON-NLS-1$
		HostConnection connection = globalConfig.getHostConnection(pdsConfig.getConnectionId());
		String host = ArgumentUtils.escapeForScript(connection.getHost());
		String port = ArgumentUtils.escapeForScript(connection.getPort());
		String protocol = connection.getProtocol();
		String codePage = connection.getCodePage();
		String timeout = ArgumentUtils.escapeForScript(connection.getTimeout());
		
		StandardCredentials  credentials = globalConfig.getUserLoginInformation(build.getParent(),
				pdsConfig.getCredentialsId());
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

		String sourceLocation = pdsConfig.getTargetFolder();
		if (StringUtils.isNotEmpty(sourceLocation))
		{
			targetFolder = ArgumentUtils.resolvePath(sourceLocation, workspaceFilePath.getRemote());
			logger.println("Source download folder: " + targetFolder); //$NON-NLS-1$
		}

		String topazCliWorkspace = workspaceFilePath.getRemote() + remoteFileSeparator + CommonConstants.TOPAZ_CLI_WORKSPACE
				+ UUID.randomUUID().toString();
		FilePath topazDataDir = new FilePath(vChannel, topazCliWorkspace);
		logger.println("topazCliWorkspace: " + topazCliWorkspace); //$NON-NLS-1$

		String cdDatasets = ArgumentUtils.escapeForScript(convertFilterPattern(pdsConfig.getFilterPattern()));
		String fileExtension = ArgumentUtils.escapeForScript(pdsConfig.getFileExtension());

		// build the list of arguments to pass to the CLI
		ArgumentListBuilder args = new ArgumentListBuilder();
		args.add(cliScriptFileRemote);
		args.add(CommonConstants.HOST_PARM, host);
		args.add(CommonConstants.PORT_PARM, port);
		if(userId != null)
		{
			args.add(CommonConstants.USERID_PARM, userId);
		}
		if(certificateStr!= null)
		{
			args.add(CommonConstants.CERT_PARM, certificateStr);
		}
		else {
			args.add(CommonConstants.CERT_PARM, "");
		}
		args.add(CommonConstants.PW_PARM);
		args.add(password, true);

		// do not pass protocol on command line if null, empty, blank, or 'None'
		if (StringUtils.isNotBlank(protocol) && !StringUtils.equalsIgnoreCase(protocol, "none")) { //$NON-NLS-1$
			CLIVersionUtils.checkProtocolSupported(cliVersion);
			args.add(CommonConstants.PROTOCOL_PARM, protocol);
		}

		args.add(CommonConstants.CODE_PAGE_PARM, codePage);
		args.add(CommonConstants.TIMEOUT_PARM, timeout);
		args.add(ScmConstants.SCM_TYPE_PARM, ScmConstants.PDS);
		args.add(CommonConstants.TARGET_FOLDER_PARM, targetFolder);
		args.add(CommonConstants.DATA_PARM, topazCliWorkspace);
		args.add(ScmConstants.FILTER_PARM, cdDatasets);
		args.add(ScmConstants.FILE_EXT_PARM, fileExtension);
		
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
			topazDataDir.deleteRecursive();
			return true;
		}
	}
}
