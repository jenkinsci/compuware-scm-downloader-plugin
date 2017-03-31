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

import java.io.PrintStream;
import java.util.Collections;
import java.util.List;
import org.apache.commons.lang.StringUtils;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;
import com.cloudbees.plugins.credentials.domains.DomainRequirement;
import com.cloudbees.plugins.credentials.matchers.IdMatcher;
import com.compuware.jenkins.scm.global.SCMGlobalConfiguration;
import com.compuware.jenkins.scm.utils.Constants;
import hudson.Launcher;
import hudson.model.Item;
import hudson.model.TaskListener;
import hudson.scm.ChangeLogParser;
import hudson.scm.SCM;
import hudson.security.ACL;

public abstract class CpwrScmConfiguration extends SCM
{
	// Member Variables
	private final String m_hostPort;
	private final String m_credentialsId;
	private final String m_codePage;
	private final String m_filterPattern;
	private final String m_fileExtension;

	/**
	 * Constructor.
	 * 
	 * @param hostPort
	 * @param filterPattern
	 * @param fileExtension
	 * @param credentialsId
	 * @param codePage
	 */
	protected CpwrScmConfiguration(String hostPort, String filterPattern, String fileExtension, String credentialsId,
			String codePage)
	{
		m_hostPort = StringUtils.trimToEmpty(hostPort);
		m_filterPattern = StringUtils.trimToEmpty(filterPattern);
		m_fileExtension = StringUtils.trimToEmpty(fileExtension);
		m_credentialsId = StringUtils.trimToEmpty(credentialsId);
		m_codePage = StringUtils.trimToEmpty(codePage);
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
	 * Gets the value of the 'Host:Port'.
	 * 
	 * @return <code>String</code> value of m_hostport
	 */
	public String getHostPort()
	{
		return m_hostPort;
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
	 * Gets the value of the 'Code Page'.
	 * 
	 * @return <code>String</code> value of m_codePage
	 */
	public String getCodePage()
	{
		return m_codePage;
	}

	/**
	 * Gets the value of the 'Topaz CLI Location' based on node.
	 * 
	 * @param launcher
	 *            the launcher associated with the current node
	 * 
	 * @return <code>String</code> value of topazCLILocation
	 */
	public String getTopazCLILocation(Launcher launcher)
	{
		SCMGlobalConfiguration globalConfig = SCMGlobalConfiguration.get();
		return globalConfig.getTopazCLILocation(launcher);
	}

	/**
	 * Gets the host.
	 * 
	 * @return the <code>String</code> host
	 */
	public String getHost()
	{
		return StringUtils.substringBefore(getHostPort(), Constants.COLON);
	}

	/**
	 * Gets the port.
	 * 
	 * @return the <code>String</code> port
	 */
	public String getPort()
	{
		return StringUtils.substringAfter(getHostPort(), Constants.COLON);
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

		validateHostPort(logger);

		StandardUsernamePasswordCredentials credentials = getLoginInformation(project);
		if (credentials != null)
		{
			logger.println(Messages.username() + " = " + credentials.getUsername()); //$NON-NLS-1$
		}
		else
		{
			throw new IllegalArgumentException(Messages.checkoutMissingParameterError(Messages.loginCredentials()));
		}

		String codePage = getCodePage();
		if (codePage.isEmpty() == false)
		{
			logger.println(Messages.codePage() + " = " + codePage); //$NON-NLS-1$
		}
		else
		{
			throw new IllegalArgumentException(Messages.checkoutMissingParameterError(Messages.codePage()));
		}

		String filterPattern = getFilterPattern();
		if (filterPattern.isEmpty() == false)
		{
			logger.println(Messages.filterPattern() + " = " + filterPattern); //$NON-NLS-1$
		}
		else
		{
			throw new IllegalArgumentException(Messages.checkoutMissingParameterError(Messages.filterPattern()));
		}

		String fileExtension = getFileExtension();
		if (fileExtension.isEmpty() == false)
		{
			logger.println(Messages.fileExtension() + " = " + fileExtension); //$NON-NLS-1$
		}
		else
		{
			throw new IllegalArgumentException(Messages.checkoutMissingParameterError(Messages.fileExtension()));
		}

		String cliLocation = getTopazCLILocation(launcher);
		if (StringUtils.isEmpty(cliLocation) == false)
		{
			logger.println(Messages.topazCLILocation() + " = " + cliLocation); //$NON-NLS-1$
		}
		else
		{
			throw new IllegalArgumentException(Messages.checkoutMissingParameterError(Messages.topazCLILocation()));
		}
	}

	/**
	 * Validate that there is a host and port defined.
	 * 
	 * @param logger
	 *            the logger to write messages to
	 */
	protected void validateHostPort(PrintStream logger)
	{
		String hostPort = getHostPort();
		if (hostPort.isEmpty() == true)
		{
			throw new IllegalArgumentException(Messages.checkoutMissingParameterError(Messages.hostPort()));
		}
		else
		{
			String[] hostPortValues = StringUtils.split(hostPort, Constants.COLON);
			if (hostPortValues.length != 2)
			{
				throw new IllegalArgumentException(Messages.checkoutInvalidParameterValueError(Messages.hostPort(), hostPort));
			}
			else
			{
				String host = StringUtils.trimToEmpty(hostPortValues[0]);
				if (host.isEmpty() == true)
				{
					throw new IllegalArgumentException(
							Messages.checkoutInvalidParameterValueError(Messages.hostPort(), hostPort));
				}

				String port = StringUtils.trimToEmpty(hostPortValues[1]);
				if (port.isEmpty() == true || StringUtils.isNumeric(port) == false)
				{
					throw new IllegalArgumentException(
							Messages.checkoutInvalidParameterValueError(Messages.hostPort(), hostPort));
				}
				else
				{
					logger.println(Messages.hostPort() + " = " + hostPort); //$NON-NLS-1$
				}
			}
		}
	}
}