/**
 * The MIT License (MIT)
 * 
 * Copyright (c) 2015 - 2018 Compuware Corporation
 * (c) Copyright 2015 - 2018, 2021 BMC Software, Inc.
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
import java.nio.file.InvalidPathException;
import java.nio.file.Paths;

import org.apache.commons.lang.StringUtils;

import com.cloudbees.plugins.credentials.common.StandardCredentials;
import com.compuware.jenkins.common.configuration.CpwrGlobalConfiguration;
import com.compuware.jenkins.common.configuration.HostConnection;

import hudson.AbortException;
import hudson.Launcher;
import hudson.model.Item;
import hudson.model.TaskListener;
import hudson.scm.ChangeLogParser;

/**
 * Abstract class containing common data and methods for SCM configurations.
 */
public abstract class CpwrScmConfiguration extends AbstractConfiguration
{
	// Member Variables
	private String m_credentialsId;
	private String m_filterPattern;
	private String m_fileExtension;
	private String m_targetFolder;

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

	/* 
	 * (non-Javadoc)
	 * @see hudson.scm.SCM#createChangeLogParser()
	 */
	@Override
	public ChangeLogParser createChangeLogParser()
	{
		return new CpwrChangeLogParser();
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
	 * Validates the configuration parameters.
	 * 
	 * @param launcher
	 *            the machine that the files will be checked out
	 * @param listener
	 *            build listener
	 * @param project
	 *            the Jenkins project
	 * @throws AbortException
	 *             if an error occurs getting the credentials user
	 */
	protected void validateParameters(Launcher launcher, TaskListener listener, Item project) throws AbortException
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

		StandardCredentials credentials = globalConfig.getLoginCredentials(project, getCredentialsId());
		if (credentials != null)
		{
			logger.println(Messages.username() + " = " + globalConfig.getCredentialsUser(credentials)); //$NON-NLS-1$
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

		validateTargetFolder(logger);

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

	/**
	 * Validates that the source download location is a valid path name.
	 * 
	 * @param logger
	 *            used to log any messages
	 */
	protected void validateTargetFolder(PrintStream logger)
	{
		String targetFolder = getTargetFolder();
		if (StringUtils.isNotEmpty(targetFolder))
		{
			logger.println(Messages.targetFolder() + " = " + targetFolder); //$NON-NLS-1$
			try
			{
				Paths.get(targetFolder);
			}
			catch (InvalidPathException exc)
			{
				throw new IllegalArgumentException(Messages.invalidSourceDownloadLocation(exc.getLocalizedMessage()));
			}
		}
	}
}