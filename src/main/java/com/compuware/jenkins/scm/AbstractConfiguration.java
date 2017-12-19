/**
 * These materials contain confidential information and trade secrets of Compuware Corporation. You shall maintain the materials
 * as confidential and shall not disclose its contents to any third party except as may be required by law or regulation. Use,
 * disclosure, or reproduction is prohibited without the prior express written permission of Compuware Corporation.
 * 
 * All Compuware products listed within the materials are trademarks of Compuware Corporation. All other company or product
 * names are trademarks of their respective owners.
 * 
 * Copyright (c) 2015 - 2018 Compuware Corporation. All rights reserved.
 */
package com.compuware.jenkins.scm;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import com.compuware.jenkins.common.configuration.CpwrGlobalConfiguration;
import com.compuware.jenkins.common.configuration.HostConnection;
import hudson.init.InitMilestone;
import hudson.init.Initializer;
import hudson.model.AbstractProject;
import hudson.scm.SCM;
import jenkins.model.Jenkins;

/**
 * Common class data and methods for all SCM configurations.
 */
public abstract class AbstractConfiguration extends SCM
{
	private static Logger m_logger = Logger.getLogger("hudson.AbstractConfiguration"); //$NON-NLS-1$

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
				connection = new HostConnection(description, m_hostPort, m_codePage, null, null);
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
		m_logger.fine("Initialization milestone: All jobs have been loaded"); //$NON-NLS-1$
		Jenkins jenkins = Jenkins.getInstance();
		for (AbstractProject<?, ?> project : jenkins.getAllItems(AbstractProject.class))
		{
			try
			{
				SCM scmConfig = project.getScm();
				if (scmConfig instanceof AbstractConfiguration && ((AbstractConfiguration) scmConfig).isMigrated())
				{
					project.save();

					m_logger.info(String.format(
									"Project %s has been migrated.", //$NON-NLS-1$
									project.getFullName()));
				}
			}
			catch (IOException e)
			{
				m_logger.log(Level.SEVERE, String.format("Failed to upgrade job %s", project.getFullName()), e); //$NON-NLS-1$
			}
		}
	}
}
