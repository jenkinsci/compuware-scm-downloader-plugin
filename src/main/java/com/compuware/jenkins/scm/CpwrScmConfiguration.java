/**
 * These materials contain confidential information and trade secrets of Compuware Corporation. You shall maintain the materials
 * as confidential and shall not disclose its contents to any third party except as may be required by law or regulation. Use,
 * disclosure, or reproduction is prohibited without the prior express written permission of Compuware Corporation.
 * 
 * All Compuware products listed within the materials are trademarks of Compuware Corporation. All other company or product
 * names are trademarks of their respective owners.
 * 
 * Copyright (c) 2015 Compuware Corporation. All rights reserved.
 */
package com.compuware.jenkins.scm;

import hudson.model.BuildListener;
import hudson.model.AbstractProject;
import hudson.scm.ChangeLogParser;
import hudson.scm.SCM;
import hudson.security.ACL;
import java.util.Collections;
import java.util.List;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;
import com.cloudbees.plugins.credentials.domains.DomainRequirement;
import com.cloudbees.plugins.credentials.matchers.IdMatcher;
import com.compuware.jenkins.scm.global.SCMGlobalConfiguration;

/**
 * 
 */
public abstract class CpwrScmConfiguration extends SCM
{
	private final String m_hostPort;
	private final String m_filterPattern;
	private final String m_fileExtension;
	private String m_topazCLILocation;
	private final String m_credentialsId;
	private final String m_codePage;

	protected CpwrScmConfiguration(String hostPort, String filterPattern, String fileExtension, String credentialsId,
			String codePage)
	{
		m_hostPort = getTrimmedValue(hostPort);
		m_filterPattern = getTrimmedValue(filterPattern);
		m_fileExtension = getTrimmedValue(fileExtension);
		m_credentialsId = getTrimmedValue(credentialsId);
		m_codePage = getTrimmedValue(codePage);
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
	 * @return <code>String</code> the trimmed value or an empty string if the value is null
	 */
	private String getTrimmedValue(String value)
	{
		String trimmedValue = ""; //$NON-NLS-1$

		if (value != null)
		{
			trimmedValue = value.trim();
		}
		return trimmedValue;
	}

	/**
	 * Gets the value of the 'Host:Port'
	 * 
	 * @return <code>String</code> value of m_hostport
	 */
	public String getHostPort()
	{
		return m_hostPort;
	}

	/**
	 * Gets the value of the 'Filter pattern'
	 * 
	 * @return <code>String</code> value of m_filterPattern
	 */
	public String getFilterPattern()
	{
		return m_filterPattern;
	}

	/**
	 * Gets the value of the 'Filter Extension'
	 * 
	 * @return <code>String</code> value of m_fileExtension
	 */
	public String getFileExtension()
	{
		return m_fileExtension;
	}

	/**
	 * Gets the value of the 'Login Credentials'
	 * 
	 * @return <code>String</code> value of m_credentialsId
	 */
	public String getCredentialsId()
	{
		return m_credentialsId;
	}

	/**
	 * Gets the value of the 'Code Page'
	 * 
	 * @return <code>String</code> value of m_codePage
	 */
	public String getCodePage()
	{
		return m_codePage;
	}

	/**
	 * Gets the value of the 'Topaz CLI Location'
	 * 
	 * @return <code>String</code> value of m_topazCLILocation
	 */
	public String getTopazCLILocation()
	{
		SCMGlobalConfiguration globalConfig = SCMGlobalConfiguration.get();
		m_topazCLILocation = globalConfig.getTopazCLILocation();

		return m_topazCLILocation;
	}

	/**
	 * Gets the host name;
	 * 
	 * @return <code>String</code> the host name
	 */
	public String getHost()
	{
		String host = getHostPort();

		int index = host.indexOf(':');
		if (index > 0)
		{
			host = host.substring(0, index);
		}

		return host;
	}

	/**
	 * Gets the port for the host connection.
	 * 
	 * @return <code>String</code> the port
	 */
	public String getPort()
	{
		String port = getHostPort();

		int index = port.indexOf(':');
		if (index > 0)
		{
			port = port.substring(index + 1);
		}

		return port;
	}

	/**
	 * Retrieves login information given a credential ID
	 * 
	 * @return a Jenkins credential with login information
	 */
	protected StandardUsernamePasswordCredentials getLoginInformation()
	{
		StandardUsernamePasswordCredentials credential = null;

		AbstractProject<?, ?> nullProject = null;
		List<StandardUsernamePasswordCredentials> credentials = CredentialsProvider
				.lookupCredentials(StandardUsernamePasswordCredentials.class, nullProject, ACL.SYSTEM,
						Collections.<DomainRequirement> emptyList());

		IdMatcher matcher = new IdMatcher(getCredentialsId());
		for (StandardUsernamePasswordCredentials c : credentials)
		{
			if (matcher.matches(c))
			{
				if (c instanceof StandardUsernamePasswordCredentials)
				{
					credential = (StandardUsernamePasswordCredentials) c;
				}
			}
		}

		return credential;
	}

	/**
	 * Validate that there is a host and port defined.
	 * 
	 * @param listener
	 *            Build listener
	 */
	protected void validateHostPort(BuildListener listener)
	{
		String hostPort = getHostPort();
		if (hostPort.isEmpty() == false)
		{
			int colonIndex = hostPort.indexOf(':');
			if ((colonIndex > 0) && (hostPort.length() > colonIndex + 1))
			{
				listener.getLogger().println(Messages.hostPort() + " = " + getHostPort()); //$NON-NLS-1$
			}
			else
			{
				throw new IllegalArgumentException(Messages.checkoutInvalidParameterValueError(Messages.hostPort(), hostPort));
			}
		}
		else
		{
			throw new IllegalArgumentException(Messages.checkoutMissingParameterError(Messages.hostPort()));
		}
	}
}
