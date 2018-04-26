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
 * 
 */
package com.compuware.jenkins.scm;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;
import com.cloudbees.plugins.credentials.domains.DomainRequirement;
import com.cloudbees.plugins.credentials.matchers.IdMatcher;
import com.compuware.jenkins.common.configuration.CpwrGlobalConfiguration;
import com.compuware.jenkins.common.configuration.HostConnection;
import hudson.AbortException;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.Item;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.scm.ChangeLogParser;
import hudson.scm.SCMRevisionState;
import hudson.security.ACL;

/**
 * Captures the configuration information for a ISPW SCM.
 */
public abstract class AbstractIspwConfiguration extends AbstractConfiguration
{
	private String m_credentialsId;
	private String m_serverConfig;

	public AbstractIspwConfiguration(String connectionId, String credentialsId, String serverConfig)
	{
		m_connectionId = getTrimmedValue(connectionId);
		m_serverConfig = getTrimmedValue(serverConfig);
		m_credentialsId = getTrimmedValue(credentialsId);
	}

	/**
	 * Method that is first called when a build is run. All dataset retrieval stems from here.
	 * 
	 * @param launcher
	 *            The machine that the files will be checked out.
	 * @param workspaceFilePath
	 *            a directory to check out the source code.
	 * @param listener
	 *            Build listener
	 * @param changelogFile
	 *            Upon a successful return, this file should capture the changelog. When there's no change, this file should
	 *            contain an empty entry
	 * @param baseline
	 *            used for polling - this parameter is not used
	 * 
	 * @throws IOException
	 * @throws InterruptedException
	 */
	@Override
	public void checkout(Run<?, ?> build, Launcher launcher, FilePath workspaceFilePath, TaskListener listener,
			File changelogFile, SCMRevisionState baseline) throws IOException, InterruptedException
	{
		boolean rtnValue = false;

		try
		{
			validateParameters(launcher, listener, build.getParent());

			IspwDownloader downloader = new IspwDownloader(this);
			rtnValue = downloader.getSource(build, launcher, workspaceFilePath, listener, changelogFile);

			if (!rtnValue)
			{
				throw new AbortException();
			}
		}
		catch (IllegalArgumentException e)
		{
			listener.getLogger().println(e.getMessage());
			throw new AbortException();
		}
	}

	/* (non-Javadoc)
	 * @see hudson.scm.SCM#createChangeLogParser()
	 */
	@Override
	public ChangeLogParser createChangeLogParser()
	{
		return null;
	}

	/**
	 * Returns a copy of the string, with leading and trailing whitespace omitted.
	 * 
	 * @param value
	 *            the string to trim
	 * 
	 * @return <code>String</code> the trimmed value or an empty string if the value is null
	 */
	protected String getTrimmedValue(String value)
	{
		String trimmedValue = ""; //$NON-NLS-1$

		if (value != null)
		{
			trimmedValue = value.trim();
		}
		return trimmedValue;
	}

	/**
	 * Gets the unique identifier of the 'Host connection'.
	 * 
	 * @return <code>String</code> value of connectionId
	 */
	public String getConnectionId()
	{
		return m_connectionId;
	}

	/**
	 * Gets the value of the 'Runtime configuration'
	 * 
	 * @return <code>String</code> value of serverConfig
	 */
	public String getServerConfig()
	{
		return m_serverConfig;
	}

	/**
	 * Gets the value of the 'Login credentials'
	 * 
	 * @return <code>String</code> value of credentialsId
	 */
	public String getCredentialsId()
	{
		return m_credentialsId;
	}

	/**
	 * Retrieves login information given a credential ID
	 * 
	 * @param project
	 *            the Jenkins project
	 *
	 * @return a Jenkins credential with login information
	 */
	protected StandardUsernamePasswordCredentials getLoginInformation(Item project)
	{
		StandardUsernamePasswordCredentials credential = null;

		List<StandardUsernamePasswordCredentials> credentials = CredentialsProvider.lookupCredentials(
				StandardUsernamePasswordCredentials.class, project, ACL.SYSTEM, Collections.<DomainRequirement> emptyList());

		IdMatcher matcher = new IdMatcher(getCredentialsId());
		for (StandardUsernamePasswordCredentials c : credentials)
		{
			if (matcher.matches(c))
			{
				credential = c;
			}
		}

		return credential;
	}

	/**
	 * Validates the server configuration parameters.
	 *
	 * @param launcher
	 *            The machine that the files will be checked out.
	 * @param listener
	 *            Build listener
	 * @param project
	 *            the Jenkins project
	 */
	public void validateServerParameters(CpwrGlobalConfiguration globalConfig, Launcher launcher, TaskListener listener,
			Item project)
	{
		if (getLoginInformation(project) != null)
		{
			listener.getLogger().println(Messages.username() + " = " + getLoginInformation(project).getUsername()); //$NON-NLS-1$
		}
		else
		{
			throw new IllegalArgumentException(Messages.checkoutMissingParameterError(Messages.loginCredentials()));
		}

		HostConnection connection = globalConfig.getHostConnection(m_connectionId);
		if (connection != null)
		{
			listener.getLogger().println(Messages.hostConnection() + " = " + connection.getHost() + ":" + connection.getPort()); //$NON-NLS-1$ //$NON-NLS-2$
		}
		else
		{
			throw new IllegalArgumentException(Messages.checkoutMissingParameterError(Messages.hostConnection()));
		}

		if (getServerConfig() != null)
		{
			listener.getLogger().println(Messages.ispwServerConfig() + " = " + getServerConfig()); //$NON-NLS-1$
		}
		else
		{
			throw new IllegalArgumentException(Messages.checkoutMissingParameterError(Messages.ispwServerConfig()));
		}
	}

	/**
	 * Validates the Cli location.
	 *
	 * @param globalConfig
	 *            The global configuration
	 * @param launcher
	 *            The machine that the files will be checked out.
	 * @param listener
	 *            Build listener
	 */
	public void validateCliLocation(CpwrGlobalConfiguration globalConfig, Launcher launcher, TaskListener listener)
	{
		String cliLocation = globalConfig.getTopazCLILocation(launcher);

		if ((cliLocation != null) && !cliLocation.isEmpty())
		{
			listener.getLogger().println(Messages.topazCLILocation() + " = " + cliLocation); //$NON-NLS-1$
		}
		else
		{
			throw new IllegalArgumentException(Messages.checkoutMissingParameterError(Messages.topazCLILocation()));
		}
	}

	/**
	 * Validates the filter configuration parameters.
	 *
	 * @param launcher
	 *            The machine that the files will be checked out.
	 * @param listener
	 *            Build listener
	 * @param project
	 *            the Jenkins project
	 */
	public abstract void validateFilterParameters(Launcher launcher, TaskListener listener, Item project);

	/**
	 * Validates the configuration parameters
	 * 
	 * @param launcher
	 *            The machine that the files will be checked out.
	 * @param listener
	 *            Build listener
	 * @param project
	 *            the Jenkins project
	 */
	public abstract void validateParameters(Launcher launcher, TaskListener listener, Item project);

	/**
	 * Plugin does not support polling. We handle file changes in the CLI.
	 */
	@Override
	public boolean supportsPolling()
	{
		return false;
	}

	/**
	 * Calculates any revisions from previous builds. Method required to support Pipeline. We handle file changes in the CLI.
	 */
	@Override
	public SCMRevisionState calcRevisionsFromBuild(Run<?, ?> build, FilePath workspace, Launcher launcher,
			TaskListener listener) throws IOException, InterruptedException
	{
		return null;
	}

}