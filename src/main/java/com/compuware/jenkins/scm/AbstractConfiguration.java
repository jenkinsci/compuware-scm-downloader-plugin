/**
 * The MIT License (MIT)
 * 
 * Copyright (c) 2015 - 2019 Compuware Corporation
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

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.QueryParameter;

import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardCredentials;
import com.cloudbees.plugins.credentials.domains.DomainRequirement;
import com.compuware.jenkins.common.configuration.CpwrGlobalConfiguration;
import com.compuware.jenkins.common.configuration.HostConnection;

import hudson.AbortException;
import hudson.Util;
import hudson.init.InitMilestone;
import hudson.init.Initializer;
import hudson.model.AbstractProject;
import hudson.model.Item;
import hudson.scm.SCM;
import hudson.security.ACL;
import hudson.util.ListBoxModel;
import hudson.util.ListBoxModel.Option;
import jenkins.model.Jenkins;

/**
 * Common class data and methods for all SCM configurations.
 */
public abstract class AbstractConfiguration extends SCM
{
	private static final Logger LOGGER = Logger.getLogger("hudson.AbstractConfiguration"); //$NON-NLS-1$

	protected String m_connectionId;

	// Backward compatibility
	protected transient @Deprecated String m_hostPort;
	protected transient @Deprecated String m_codePage;

	protected transient boolean m_isMigrated = false;

	private static final Object lock = new Object();

	/**
	 * Return true if the configuration is migrated.
	 * 
	 * @return true if the configuration is migrated
	 */
	protected boolean isMigrated()
	{
		return m_isMigrated;
	}

	/**
	 * Called when object has been deserialized from a stream.
	 *
	 * <p>
	 * Data migration:
	 * 
	 * <pre>
	 * In 2.0 "hostPort" and "codePage" were removed and replaced by a list of host connections. This list is a global and
	 * created with the Global Configuration page. If old hostPort and codePage properties exist, then a an attempt is made to
	 * create a new host connection with these properties and add it to the list of global host connections, as long as there is
	 * no other host connection already existing with the same properties.
	 * </pre>
	 * 
	 * @return {@code this}, or a replacement for {@code this}.
	 */
	protected Object readResolve()
	{
		// Migrate from 1.X to 2.0
		if (m_hostPort != null && m_codePage != null)
		{
			migrateConnectionInfo();
		}

		return this;
	}

	/**
	 * Migrate configuration information from 1.X to 2.0.
	 * 
	 * <p>
	 * Data migration:
	 * 
	 * <pre>
	 * In 2.0 "hostPort" and "codePage" were removed and replaced by a list of host connections. This list is a global and
	 * created with the Global Configuration page. If old hostPort and codePage properties exist, then a an attempt is made to
	 * create a new host connection with these properties and add it to the list of global host connections, as long as there is
	 * no other host connection already existing with the same properties.
	 * </pre>
	 */
	private void migrateConnectionInfo()
	{
		synchronized (lock)
		{
			CpwrGlobalConfiguration globalConfig = CpwrGlobalConfiguration.get();
			HostConnection connection = globalConfig.getHostConnection(m_hostPort, m_codePage);
			if (connection == null)
			{
				String description = m_hostPort + " " + m_codePage; //$NON-NLS-1$
				connection = new HostConnection(description, m_hostPort, null, m_codePage, null, null, null);
				globalConfig.addHostConnection(connection);
			}
			else
			{
				// Connection might exist if one originally migrated, reverted, and now is migrating again.
			}

			m_connectionId = connection.getConnectionId();
			m_isMigrated = true;
		}
	}

	@Initializer(before = InitMilestone.COMPLETED, after = InitMilestone.JOB_LOADED)
	public static void jobLoaded() throws IOException
	{
		LOGGER.fine("Initialization milestone: All jobs have been loaded"); //$NON-NLS-1$
		Jenkins jenkins = Jenkins.getInstance();
		for (AbstractProject<?, ?> project : jenkins.getAllItems(AbstractProject.class))
		{
			try
			{
				SCM scmConfig = project.getScm();
				if (scmConfig instanceof AbstractConfiguration && ((AbstractConfiguration) scmConfig).isMigrated())
				{
					project.save();

					LOGGER.info(String.format(
									"Project %s has been migrated.", //$NON-NLS-1$
									project.getFullName()));
				}
			}
			catch (IOException e)
			{
				LOGGER.log(Level.SEVERE, String.format("Failed to upgrade job %s", project.getFullName()), e); //$NON-NLS-1$
			}
		}
	}

	/**
	 * Fills in the Login Credentials selection box with applicable connections.
	 * 
	 * @param context
	 *            filter for login credentials
	 * @param credentialsId
	 *            existing login credentials; can be null
	 * @param project
	 *            the Jenkins project
	 * 
	 * @return login credentials selection
	 */
	public ListBoxModel doFillCredentialsIdItems(@AncestorInPath Jenkins context, @QueryParameter String credentialsId,
			@AncestorInPath Item project) {
		List<StandardCredentials> creds = CredentialsProvider.lookupCredentials(StandardCredentials.class, project, ACL.SYSTEM,
				Collections.<DomainRequirement>emptyList());

		ListBoxModel model = new ListBoxModel();
		model.add(new Option(StringUtils.EMPTY, StringUtils.EMPTY, false));

		for (StandardCredentials c : creds) {
			boolean isSelected = false;
			if (credentialsId != null) {
				isSelected = credentialsId.matches(c.getId());
			}

			String description = Util.fixEmptyAndTrim(c.getDescription());
			try {
				model.add(new Option(CpwrGlobalConfiguration.get().getCredentialsUser(c)
						+ (description != null ? (" (" + description + ')') : StringUtils.EMPTY), c.getId(), isSelected)); //$NON-NLS-1$
			} catch (AbortException e) {
				LOGGER.log(Level.WARNING, e.getMessage());
			}
		}

		return model;
	}
}
