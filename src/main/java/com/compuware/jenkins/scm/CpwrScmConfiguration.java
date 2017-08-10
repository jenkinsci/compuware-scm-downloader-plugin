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

import java.io.PrintStream;
import java.util.Collections;
import java.util.List;
import org.apache.commons.lang.StringUtils;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;
import com.cloudbees.plugins.credentials.domains.DomainRequirement;
import com.cloudbees.plugins.credentials.matchers.IdMatcher;
import com.compuware.jenkins.common.configuration.CpwrGlobalConfiguration;
import com.compuware.jenkins.common.configuration.HostConnection;
import hudson.Launcher;
import hudson.model.Item;
import hudson.model.TaskListener;
import hudson.scm.ChangeLogParser;
import hudson.scm.SCM;
import hudson.security.ACL;

/**
 * Abstsract class containing common data and methods for SCM configurations.
 */
public abstract class CpwrScmConfiguration extends SCM
{
	// Member Variables
	private String m_connectionId;
	private String m_credentialsId;
	private String m_filterPattern;
	private String m_fileExtension;
	private String m_targetFolder;

	// Backward compatibility
	private transient @Deprecated String m_hostPort;
	private transient @Deprecated String m_codePage;

	/**
	 * Constructor.
	 * 
	 * @param connectionId
	 *            a unique host connection identifier
	 * @param filterPattern
	 *            filter for the datasets to be retrieved from the mainframe
	 * @param fileExtension
	 *            file extension for the incoming datasets
	 * @param credentialsId
	 *            unique id of the selected credential
	 * @param targetFolder
	 *            source download location
	 */
	protected CpwrScmConfiguration(String connectionId, String filterPattern, String fileExtension, String credentialsId, String targetFolder)
	{
		m_connectionId = StringUtils.trimToEmpty(connectionId);
		m_filterPattern = StringUtils.trimToEmpty(filterPattern);
		m_fileExtension = StringUtils.trimToEmpty(fileExtension);
		m_credentialsId = StringUtils.trimToEmpty(credentialsId);
		m_targetFolder = StringUtils.trimToEmpty(targetFolder);
	}

	/**
	 * Handle data migration
	 * 
	 * In 2.0 "hostPort" and "codePage" were removed and replaced by a list of host connections. This list is a global and
	 * created with the Global Configuration page. If old hostPort and codePage properties exist, then a an attempt is made to
	 * create a new host connection with these properties and add it to the list of global host connections, as long as there is
	 * no other host connection already existing with the same properties.
	 * 
	 * @return the configuration
	 */
	protected Object readResolve()
	{
		// Migrate from 1.X to 2.0
		if (m_hostPort != null && m_codePage != null)
		{
			CpwrGlobalConfiguration globalConfig = CpwrGlobalConfiguration.get();
			if (!globalConfig.hostConnectionExists(m_hostPort, m_codePage))
			{
				String description = m_hostPort + " " + m_codePage; //$NON-NLS-1$
				HostConnection connection = new HostConnection(description, m_hostPort, m_codePage, null, null);
				globalConfig.addHostConnection(connection);
				m_connectionId = connection.getConnectionId();
			}
		}

		return this;
	}

	/* 
	 * (non-Javadoc)
	 * @see hudson.scm.SCM#createChangeLogParser()
	 */
	@Override
	public ChangeLogParser createChangeLogParser()
	{
		return null;
	}

	/**
	 * Gets the unique identifier of the 'Host connection'.
	 * 
	 * @return <code>String</code> value of m_connectionId
	 */
	public String getConnectionId()
	{
		return m_connectionId;
	}

	/**
	 * Gets the value of the 'Filter pattern'.
	 * 
	 * @return <code>String</code> value of m_filterPattern
	 */
	public String getFilterPattern()
	{
		return m_filterPattern;
	}

	/**
	 * Gets the value of the 'Filter Extension'.
	 * 
	 * @return <code>String</code> value of m_fileExtension
	 */
	public String getFileExtension()
	{
		return m_fileExtension;
	}

	/**
	 * Gets the value of the 'Login Credentials'.
	 * 
	 * @return <code>String</code> value of m_credentialsId
	 */
	public String getCredentialsId()
	{
		return m_credentialsId;
	}

	/**
	 * Gets the value of the 'Source download location'
	 * 
	 * @return <code>String</code> value of m_targetFolder
	 */
	public String getTargetFolder()
	{
		return m_targetFolder;
	}

	/**
	 * Retrieves login information given a credential ID.
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
	 * Validates the configuration parameters.
	 * 
	 * @param launcher
	 *            the machine that the files will be checked out
	 * @param listener
	 *            build listener
	 * @param project
	 *            the Jenkins project
	 */
	protected void validateParameters(Launcher launcher, TaskListener listener, Item project)
	{
		PrintStream logger = listener.getLogger();
		CpwrGlobalConfiguration globalConfig = CpwrGlobalConfiguration.get();

		HostConnection connection = globalConfig.getHostConnection(m_connectionId);
		if (connection != null)
		{
			logger.println(Messages.hostConnection() + " = " + connection.getHost() + ":" + connection.getPort()); //$NON-NLS-1$ //$NON-NLS-2$
		}
		else
		{
			throw new IllegalArgumentException(Messages.checkoutMissingParameterError(Messages.hostConnection()));
		}

		StandardUsernamePasswordCredentials credentials = getLoginInformation(project);
		if (credentials != null)
		{
			logger.println(Messages.username() + " = " + credentials.getUsername()); //$NON-NLS-1$
		}
		else
		{
			throw new IllegalArgumentException(Messages.checkoutMissingParameterError(Messages.loginCredentials()));
		}

		String filterPattern = getFilterPattern();
		if (!filterPattern.isEmpty())
		{
			logger.println(Messages.filterPattern() + " = " + filterPattern); //$NON-NLS-1$
		}
		else
		{
			throw new IllegalArgumentException(Messages.checkoutMissingParameterError(Messages.filterPattern()));
		}

		String fileExtension = getFileExtension();
		if (!fileExtension.isEmpty())
		{
			logger.println(Messages.fileExtension() + " = " + fileExtension); //$NON-NLS-1$
		}
		else
		{
			throw new IllegalArgumentException(Messages.checkoutMissingParameterError(Messages.fileExtension()));
		}

		String targetFolder = getTargetFolder();
		if (!StringUtils.isEmpty(targetFolder))
		{
			logger.println(Messages.targetFolder() + " = " + targetFolder); //$NON-NLS-1$
		}

		String cliLocation = globalConfig.getTopazCLILocation(launcher);
		if (!StringUtils.isEmpty(cliLocation))
		{
			logger.println(Messages.topazCLILocation() + " = " + cliLocation); //$NON-NLS-1$
		}
		else
		{
			throw new IllegalArgumentException(Messages.checkoutMissingParameterError(Messages.topazCLILocation()));
		}
	}
}