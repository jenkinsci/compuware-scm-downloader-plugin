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
	private String m_componentType = StringUtils.EMPTY;
	private String m_folderName = StringUtils.EMPTY;
	private String m_filterFiles = "false"; //$NON-NLS-1$
	private String m_filterFolders = "false"; //$NON-NLS-1$

	// Backward compatibility
	private transient @Deprecated String m_hostPort;
	private transient @Deprecated String m_codePage;

	@DataBoundConstructor
	public IspwConfiguration(String connectionId, String credentialsId, String serverConfig, String serverStream,
			String serverApplication, String serverLevel, String levelOption, EnableComponents filterFiles,
			EnableFolders filterFolders)
	{
		m_connectionId = getTrimmedValue(connectionId);
		m_credentialsId = getTrimmedValue(credentialsId);
		m_serverConfig = getTrimmedValue(serverConfig);
		m_serverStream = getTrimmedValue(serverStream);
		m_serverApplication = getTrimmedValue(serverApplication);
		m_serverLevel = getTrimmedValue(serverLevel);
		m_levelOption = getTrimmedValue(levelOption);

		if (filterFiles != null)
		{
			m_filterFiles = "true"; //$NON-NLS-1$
			m_componentType = filterFiles.getComponentType();
		}

		if (filterFolders != null)
		{
			m_filterFolders = "true"; //$NON-NLS-1$
			m_folderName = filterFolders.getFolderName();
		}
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
	 * Gets the value of the 'Component type'
	 * 
	 * @return <code>String</code> value of m_componentType
	 */
	public String getComponentType()
	{
		return m_componentType;
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

	/**
	 * Gets the value of the 'Components' checkbox
	 * 
	 * @return <code>String</code> value of m_filterFiles
	 */
	public String getFilterFiles()
	{
		return m_filterFiles.toLowerCase();
	}

	/**
	 * Gets the value of the 'Folders' checkbox
	 * 
	 * @return <code>String</code> value of m_filterFolders
	 */
	public String getFilterFolders()
	{
		return m_filterFolders.toLowerCase();
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

		if (!getFolderName().isEmpty())
		{
			listener.getLogger().println(Messages.ispwFolderName() + " = " + getFolderName()); //$NON-NLS-1$
		}
		else if ("true".equals(getFilterFolders()) && getFolderName().isEmpty()) //$NON-NLS-1$
		{
			throw new IllegalArgumentException(Messages.checkoutMissingParameterError(Messages.ispwFolderName()));
		}

		if (!getComponentType().isEmpty())
		{
			listener.getLogger().println(Messages.ispwComponentType() + " = " + getComponentType()); //$NON-NLS-1$
		}
		else if ("true".equals(getFilterFiles()) && getComponentType().isEmpty()) //$NON-NLS-1$
		{
			throw new IllegalArgumentException(Messages.checkoutMissingParameterError(Messages.ispwComponentType()));
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
		 * Validator for the 'Component type' text field.
		 * 
		 * @param value
		 *            value passed from the "componentType" field
		 * 
		 * @return validation message
		 */
		public FormValidation doCheckComponentType(@QueryParameter String value)
		{
			String tempValue = StringUtils.trimToEmpty(value);
			if (tempValue.isEmpty())
			{
				return FormValidation.error(Messages.checkIspwComponentTypeError());
			}

			return FormValidation.ok();
		}

		/**
		 * Validator for the 'Folder name' text field.
		 * 
		 * @param value
		 *            value passed from the "folderName" field
		 * 
		 * @return validation message
		 */
		public FormValidation doCheckFolderName(@QueryParameter String value)
		{
			String tempValue = StringUtils.trimToEmpty(value);
			if (tempValue.isEmpty())
			{
				return FormValidation.error(Messages.checkIspwFolderNameError());
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

	/**
	 * This class is a nullable object that binds the data from the optionalBlock <code>filterFiles</code> in the
	 * jelly.config file. If an object of this type is null it means the checkbox to enable the optionalBlock has not been
	 * selected. The data from the optionalBlock is sent over in the format:<p>
	 * filterFiles : {componentType : " " }<p>
	 * Where the value of componentType is the text entered in the text field of the form.
	 */
	public static class EnableComponents
	{
		private String componentType;

		/**
		 *
		 * @param componentType The text enter in the Component type field of the form
		 */
		@DataBoundConstructor
		public EnableComponents(String componentType)
		{
			this.componentType = componentType;
		}

		/**
		 * 
		 * @return the component type entered in the text field within the optional block
		 */
		public String getComponentType()
		{
			return componentType;
		}

		/**
		 * Method that overrides the default .toString() method to return "true if the box has been checked or "false" if the
		 * box has not been checked
		 * @return "true" or "false" ased on whether the componentType eists or not
		 */
		public String toString()
		{
			return Boolean.toString(componentType != null);
		}
	}
	
	/**
	 * This class is a nullable object that binds the data from the optionalBlock <code>filterFolders</code> in the
	 * jelly.config file. If an object of this type is null it means the checkbox to enable the optionalBlock has not been
	 * selected. The data from the optionalBlock is sent over in the format:<p>
	 * filterFolders : {folderName : " " }<p>
	 * Where the value of folderName is the text entered in the text field of the form.
	 */
	public static class EnableFolders
	{

		private String folderName;

		/**
		 * 
		 * @param folderName the text enter in the Folder name field of the form
		 */
		@DataBoundConstructor
		public EnableFolders(String folderName)
		{
			this.folderName = folderName;
		}

		/**
		 * 
		 * @return the folder name entered in the text field within the optional block
		 */
		public String getFolderName()
		{
			return folderName;
		}

		/**
		 * Method that overrides the default .toString() method to return "true" if the box has been checked or "false" if the
		 * box has not been checked
		 * @return "true" or "false" based on whether the folderName exists
		 */
		public String toString()
		{
			return Boolean.toString(folderName != null);
		}
	}
}
