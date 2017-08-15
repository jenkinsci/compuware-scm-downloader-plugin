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
 * 
 */
package com.compuware.jenkins.scm;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardListBoxModel;
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;
import com.cloudbees.plugins.credentials.domains.DomainRequirement;
import com.cloudbees.plugins.credentials.matchers.IdMatcher;
import com.compuware.jenkins.common.configuration.CpwrGlobalConfiguration;
import com.compuware.jenkins.common.configuration.HostConnection;
import hudson.AbortException;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.Util;
import hudson.model.Item;
import hudson.model.Job;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.scm.ChangeLogParser;
import hudson.scm.SCM;
import hudson.scm.SCMDescriptor;
import hudson.scm.SCMRevisionState;
import hudson.security.ACL;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import hudson.util.ListBoxModel.Option;
import jenkins.model.Jenkins;
import net.sf.json.JSONObject;

/**
 * Captures the configuration information for a ISPW SCM.
 */
public class IspwConfiguration extends SCM
{
	private String m_connectionId;
	private String m_credentialsId;
	private String m_serverConfig;
	private String m_serverStream;
	private String m_serverApplication;
	private String m_serverLevel;
	private String m_levelOption;
	private String m_filterType;
	private String m_folderName;
	private String m_filterFiles;
	private String m_filterFolders;

	// Backward compatibility
	private transient @Deprecated String m_hostPort;
	private transient @Deprecated String m_codePage;

	@DataBoundConstructor
	public IspwConfiguration(String connectionId, String credentialsId, String serverConfig, String serverStream,
			String serverApplication, String serverLevel, String levelOption, String filterType, String folderName, boolean filterFiles, boolean filterFolders)
	{
		m_connectionId = getTrimmedValue(connectionId);
		m_credentialsId = getTrimmedValue(credentialsId);
		m_serverConfig = getTrimmedValue(serverConfig);
		m_serverStream = getTrimmedValue(serverStream);
		m_serverApplication = getTrimmedValue(serverApplication);
		m_serverLevel = getTrimmedValue(serverLevel);
		m_levelOption = getTrimmedValue(levelOption);
		m_filterType = getTrimmedValue(filterType);
		m_folderName = getTrimmedValue(folderName);
		m_filterFiles = filterFiles + StringUtils.EMPTY;
		m_filterFolders = filterFolders + StringUtils.EMPTY;
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

    /**
     *  Handle data migration
     *  
     *  In 2.0 "hostPort" and "codePage" were removed and replaced by a list of host connections. This list is 
     *  a global and created with the Global Configuration page. If old hostPort and codePage properties exist, then 
     *  a an attempt is made to create a new host connection with these properties and add it to the list of global 
     *  host connections, as long as there is no other host connection already existing with the same properties. 
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
	 * Gets the unique identifier of the 'Host connection'.
	 * 
	 * @return <code>String</code> value of m_connectionId
	 */
	public String getConnectionId()
	{
		return m_connectionId;
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
	 * Gets the value of the 'Config'
	 * 
	 * @return <code>String</code> value of m_serverConfig
	 */
	public String getServerConfig()
	{
		return m_serverConfig;
	}

	/**
	 * Gets the value of the 'Stream'
	 * 
	 * @return <code>String</code> value of m_serverStream
	 */
	public String getServerStream()
	{
		return m_serverStream;
	}

	/**
	 * Gets the value of the 'Application'
	 * 
	 * @return <code>String</code> value of m_serverApplication
	 */
	public String getServerApplication()
	{
		return m_serverApplication;
	}

	/**
	 * Gets the value of the 'Level'
	 * 
	 * @return <code>String</code> value of m_serverLevel
	 */
	public String getServerLevel()
	{
		return m_serverLevel;
	}

	/**
	 * Gets the value of the 'Level Option'
	 * 
	 * @return <code>String</code> value of m_levelOption
	 */
	public String getLevelOption()
	{
		return m_levelOption;
	}

	/**
	 * Gets the value of the 'Filter Type'
	 * 
	 * @return <code>String</code> value of m_filterType
	 */
	public String getFilterType()
	{
		return m_filterType;
	}

	/**
	 * Gets the value of the 'Folder Name'
	 * 
	 * @return <code>String</code> value of m_folderName
	 */
	public String getFolderName()
	{
		return m_folderName;
	}

	public String getFilterFiles() {
		return m_filterFiles;
	}

	public String getFilterFolders() {
		return m_filterFolders;
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
	 * Validates the configuration parameters.
	 *
	 * @param launcher
	 *            The machine that the files will be checked out.
	 * @param listener
	 *            Build listener
	 * @param project
	 *            the Jenkins project
	 */
	public void validateParameters(Launcher launcher, TaskListener listener, Item project)
	{
		CpwrGlobalConfiguration globalConfig = CpwrGlobalConfiguration.get();

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

		if (!getServerStream().isEmpty())
		{
			listener.getLogger().println(Messages.ispwServerStream() + " = " + getServerStream()); //$NON-NLS-1$
		}
		else
		{
			throw new IllegalArgumentException(Messages.checkoutMissingParameterError(Messages.ispwServerStream()));
		}

		if (!getServerApplication().isEmpty())
		{
			listener.getLogger().println(Messages.ispwServerApp() + " = " + getServerApplication()); //$NON-NLS-1$
		}
		else
		{
			throw new IllegalArgumentException(Messages.checkoutMissingParameterError(Messages.ispwServerApp()));
		}

		if (!getServerLevel().isEmpty())
		{
			listener.getLogger().println(Messages.ispwServerLevel() + " = " + getServerLevel()); //$NON-NLS-1$
		}
		else
		{
			throw new IllegalArgumentException(Messages.checkoutMissingParameterError(Messages.ispwServerLevel()));
		}

		if (!getLevelOption().isEmpty())
		{
			listener.getLogger().println(Messages.ispwLevelOption() + " = " + getLevelOption()); //$NON-NLS-1$
		}
		else
		{
			throw new IllegalArgumentException(Messages.checkoutMissingParameterError(Messages.ispwLevelOption()));
		}

		if (getFolderName() != null)
		{
			listener.getLogger().println(Messages.ispwfolderName() + " = " + getFolderName()); //$NON-NLS-1$
		}
		else
		{
			throw new IllegalArgumentException(Messages.checkoutMissingParameterError(Messages.ispwfolderName()));
		}

		if (getFilterType() != null)
		{
			listener.getLogger().println(Messages.ispwfilterType() + " = " + getFilterType()); //$NON-NLS-1$
		}
		else
		{
			throw new IllegalArgumentException(Messages.checkoutMissingParameterError(Messages.ispwfilterType()));
		}

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

	/**
	 * DescriptorImpl is used to create instances of <code>IspwConfiguration</code>. It also contains the global configuration
	 * options as fields, just like the <code>IspwConfiguration</code> contains the configuration options for a job
	 */
	@Extension
	public static class DescriptorImpl extends SCMDescriptor<IspwConfiguration>
	{
		public DescriptorImpl()
		{
			super(IspwConfiguration.class, null);
			load();
		}

		/**
		 * Necessary to display UI in Jenkins Pipeline.
		 */
		@SuppressWarnings("rawtypes")
		@Override
		public boolean isApplicable(Job project)
		{
			return true;
		}

		/**
		 * Displays the name of the SCM, the name that appears when configuring a Jenkins job.
		 * 
		 * @return the <code>String</code> value of the SCM display name
		 */
		@Override
		public String getDisplayName()
		{
			return Messages.displayNameISPW();
		}

		/**
		 * The method is called when the global configuration page is submitted. In the method the data in the web form should
		 * be copied to the Descriptor's fields. To persist the fields to the global configuration XML file, the
		 * <code>save()</code> method must be called. Data is defined in the global.jelly page.
		 * 
		 * @param req
		 *            Stapler request
		 * @param formData
		 *            Form data
		 * 
		 * @return TRUE if able to configure and continue to next page
		 * 
		 * @throws FormException
		 */
		@Override
		public boolean configure(StaplerRequest req, JSONObject formData) throws FormException
		{
			save();
			return super.configure(req, formData);
		}

		/**
		 * Validator for the 'Host connection' field.
		 * 
		 * @param connectionId
		 *            unique identifier for the host connection passed from the config.jelly "connectionId" field
		 * 
		 * @return validation message
		 */
		public FormValidation doCheckConnectionId(@QueryParameter String connectionId)
		{
			String tempValue = StringUtils.trimToEmpty(connectionId);
			if (tempValue.isEmpty())
			{
				return FormValidation.error(Messages.checkHostConnectionError());
			}

			return FormValidation.ok();
		}

		/**
		 * Fills in the Host Connection selection box with applicable connections.
		 * 
		 * @param context
		 *            filter for host connections
		 * @param connectionId
		 *            an existing host connection identifier; can be null
		 * @param project
		 *            the Jenkins project
		 * 
		 * @return host connection selections
		 */
		public ListBoxModel doFillConnectionIdItems(@AncestorInPath Jenkins context, @QueryParameter String connectionId,
				@AncestorInPath Item project)
		{
			CpwrGlobalConfiguration globalConfig = CpwrGlobalConfiguration.get();
			HostConnection[] hostConnections = globalConfig.getHostConnections();

			ListBoxModel model = new ListBoxModel();
			model.add(new Option(StringUtils.EMPTY, StringUtils.EMPTY, false));

			for (HostConnection connection : hostConnections)
			{
				boolean isSelected = false;
				if (connectionId != null)
				{
					isSelected = connectionId.matches(connection.getConnectionId());
				}

				model.add(new Option(connection.getDescription() + " [" + connection.getHostPort() + ']', //$NON-NLS-1$
						connection.getConnectionId(), isSelected));
			}

			return model;
		}

		/**
		 * Validator for the 'Login Credential' field
		 * 
		 * @param value
		 *            value passed from the config.jelly "loginCredential" field
		 * 
		 * @return validation message
		 */
		public FormValidation doCheckCredentialsId(@QueryParameter String value)
		{
			String tempValue = StringUtils.trimToEmpty(value);
			if (tempValue.isEmpty())
			{
				return FormValidation.error(Messages.checkLoginCredentialsError());
			}

			return FormValidation.ok();
		}

		/**
		 * Validator for the 'Stream' text field.
		 * 
		 * @param value
		 *            value passed from the "serverStream" field
		 * 
		 * @return validation message
		 */
		public FormValidation doCheckServerStream(@QueryParameter String value)
		{
			String tempValue = StringUtils.trimToEmpty(value);
			if (tempValue.isEmpty())
			{
				return FormValidation.error(Messages.checkIspwServerStreamError());
			}

			return FormValidation.ok();
		}

		/**
		 * Validator for the 'Application' text field.
		 * 
		 * @param value
		 *            value passed from the "serverApplication" field
		 * 
		 * @return validation message
		 */
		public FormValidation doCheckServerApplication(@QueryParameter String value)
		{
			String tempValue = StringUtils.trimToEmpty(value);
			if (tempValue.isEmpty())
			{
				return FormValidation.error(Messages.checkIspwServerAppError());
			}

			return FormValidation.ok();
		}

		/**
		 * Validator for the 'Level' text field.
		 * 
		 * @param value
		 *            value passed from the "serverLevel" field
		 * 
		 * @return validation message
		 */
		public FormValidation doCheckServerLevel(@QueryParameter String value)
		{
			String tempValue = StringUtils.trimToEmpty(value);
			if (tempValue.isEmpty())
			{
				return FormValidation.error(Messages.checkIspwServerLevelError());
			}

			return FormValidation.ok();
		}

		/**
		 * Fills in the Login Credential selection box with applicable Jenkins credentials
		 * 
		 * @param context
		 *            filter for credentials
		 * 
		 * @return credential selections
		 */
		public ListBoxModel doFillCredentialsIdItems(@AncestorInPath Jenkins context, @QueryParameter String credentialsId,
				@AncestorInPath Item project)
		{
			List<StandardUsernamePasswordCredentials> creds = CredentialsProvider.lookupCredentials(
					StandardUsernamePasswordCredentials.class, project, ACL.SYSTEM,
					Collections.<DomainRequirement> emptyList());

			StandardListBoxModel model = new StandardListBoxModel();

			model.add(new Option(StringUtils.EMPTY, StringUtils.EMPTY, false));

			for (StandardUsernamePasswordCredentials c : creds)
			{
				boolean isSelected = false;

				if (credentialsId != null)
				{
					isSelected = credentialsId.matches(c.getId());
				}

				String description = Util.fixEmptyAndTrim(c.getDescription());
				model.add(new Option(c.getUsername() + (description != null ? " (" + description + ")" : StringUtils.EMPTY), //$NON-NLS-1$ //$NON-NLS-2$
						c.getId(), isSelected));
			}

			return model;
		}

		/**
		 * Fills in the Level option selection box with ISPW level options
		 *
		 * @return level option selections
		 */
		public ListBoxModel doFillLevelOptionItems()
		{
			ListBoxModel levelOptionModel = new ListBoxModel();

			levelOptionModel.add(Messages.ispwDropLevelOnly(), "0"); //$NON-NLS-1$
			levelOptionModel.add(Messages.ispwDropLevelAbove(), "1"); //$NON-NLS-1$

			return levelOptionModel;
		}
	}
}
