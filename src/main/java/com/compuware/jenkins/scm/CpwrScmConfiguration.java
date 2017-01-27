/**
 * The MIT License (MIT)
 * 
 * Copyright (c) 2017 Compuware Corporation
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

import java.util.Collections;
import java.util.List;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;
import com.cloudbees.plugins.credentials.domains.DomainRequirement;
import com.cloudbees.plugins.credentials.matchers.IdMatcher;
import com.compuware.jenkins.scm.global.SCMGlobalConfiguration;
import hudson.Launcher;
import hudson.model.Item;
import hudson.model.TaskListener;
import hudson.scm.ChangeLogParser;
import hudson.scm.SCM;
import hudson.security.ACL;

/**
 * 
 */
public abstract class CpwrScmConfiguration extends SCM
{
	private final String m_hostPort;
	private final String m_filterPattern;
	private final String m_fileExtension;
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
	 * Gets the value of the 'Topaz CLI Location' based on node
	 * 
	 * @param launcher
	 * 			the launcher associated with the current node
	 * @return <code>String</code> value of topazCLILocation
	 */
	public String getTopazCLILocation(Launcher launcher)
	{
		SCMGlobalConfiguration globalConfig = SCMGlobalConfiguration.get();
		String topazCLILocation = globalConfig.getTopazCLILocation(launcher);

		return topazCLILocation;
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
	 * @param project
	 *			the Jenkins project
	 * @return a Jenkins credential with login information
	 */
	protected StandardUsernamePasswordCredentials getLoginInformation(Item project)
	{
		StandardUsernamePasswordCredentials credential = null;

		List<StandardUsernamePasswordCredentials> credentials = CredentialsProvider
				.lookupCredentials(StandardUsernamePasswordCredentials.class, project, ACL.SYSTEM,
						Collections.<DomainRequirement> emptyList());

		IdMatcher matcher = new IdMatcher(getCredentialsId());
		for (StandardUsernamePasswordCredentials c : credentials)
		{
			if (matcher.matches(c))
			{
				credential = (StandardUsernamePasswordCredentials) c;
			}
		}

		return credential;
	}

	/**
	 * Validate that there is a host and port defined.
	 * 
	 * @param listener
	 *            Task listener
	 */
	protected void validateHostPort(TaskListener listener)
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
