/**
 * The MIT License (MIT)
 * 
 * Copyright (c) 2015 - 2019 Compuware Corporation
 * (c) Copyright 2019-2025 BMC Software, Inc.
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

import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

import com.compuware.jenkins.common.configuration.CpwrGlobalConfiguration;
import com.compuware.jenkins.common.configuration.HostConnection;

import hudson.AbortException;
import hudson.Extension;
import hudson.Launcher;
import hudson.init.InitMilestone;
import hudson.init.Initializer;
import hudson.model.Item;
import hudson.model.Items;
import hudson.model.Job;
import hudson.model.TaskListener;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import hudson.util.ListBoxModel.Option;
import jenkins.model.Jenkins;
import net.sf.json.JSONObject;

/**
 * Captures the configuration information for a ISPW SCM.
 */
public class IspwConfiguration extends AbstractIspwConfiguration
{
	private static final String TRUE = "true"; //$NON-NLS-1$
	private static final String FALSE = "false"; //$NON-NLS-1$
	private String m_serverStream;
	private String m_serverApplication;
	private String m_serverSubAppl;
	private String m_serverLevel;
	private String m_levelOption;
	private String m_componentType = StringUtils.EMPTY;
	private String m_folderName = StringUtils.EMPTY;
	private String m_filterFiles = FALSE;
	private String m_filterFolders = FALSE;
	private boolean ispwDownloadAll = false;
	private  boolean ispwDownloadIncl = DescriptorImpl.ispwDownloadIncl;
	private boolean ispwDownloadWithCompileOnly = DescriptorImpl.ispwDownloadWithCompileOnly;
	private boolean cpCategorizeOnComponentType = DescriptorImpl.cpCategorizeOnComponentType;
	private String m_targetFolder;

	/**
	 * Gets the data from the configuration page. The parameter names must match the field names set by
	 * <code>config.jelly</code>.
	 * 
	 * @param connectionId
	 *            - a unique host connection identifier
	 * @param credentialsId
	 *            - unique id of the selected credential
	 * @param serverConfig
	 *            - runtime configuration
	 * @param serverStream
	 *            - selected ispw stream
	 * @param serverApplication
	 *            - selected ispw application
	 * @param serverSubAppl
	 *            - selected ispw subAppl
	 * @param serverLevel
	 *            - selected ispw level
	 * @param levelOption
	 *            - 0 (all in selected level only) or 1 (all in selected level and above)
	 * @param componentType
	 *            - The component type to filter for
	 * @param folderName
	 *            - comma-delimited folder names to filter on
	 * @param ispwDownloadAll
	 *            - whether to keep files in sync within the specified target Folder
	 * @param targetFolder
	 *            - source download location
	 * @param ispwDownloadIncl
	 *            - whether to download the INCL impacts
	 * @param cpCategorizeOnComponentType
	 * 			  - whether to categorize the source files to different folders according to Component Type
	 */
	@DataBoundConstructor
	public IspwConfiguration(String connectionId, String credentialsId, String serverConfig, String serverStream,
			String serverApplication, String serverSubAppl, String serverLevel, String levelOption, String componentType, String folderName,
			boolean ispwDownloadAll, String targetFolder, boolean ispwDownloadIncl, boolean ispwDownloadWithCompileOnly, boolean cpCategorizeOnComponentType)
	{
		super(connectionId, credentialsId, serverConfig);

		m_serverStream = getTrimmedValue(serverStream);
		m_serverApplication = getTrimmedValue(serverApplication);
		
		if(serverSubAppl!=null)
		{
			m_serverSubAppl= getTrimmedValue(serverSubAppl);
		}
		
		m_serverLevel = getTrimmedValue(serverLevel);
		m_levelOption = getTrimmedValue(levelOption);
		m_targetFolder = getTrimmedValue(targetFolder);

		if (componentType != null && !componentType.isEmpty())
		{
			m_filterFiles = TRUE;
			m_componentType = getTrimmedValue(componentType);
		}
		if (folderName != null && !folderName.isEmpty())
		{
			m_filterFolders = TRUE;
			m_folderName = getTrimmedValue(folderName);
		}

		this.ispwDownloadAll = ispwDownloadAll;
		this.ispwDownloadIncl = ispwDownloadIncl;
		this.ispwDownloadWithCompileOnly = ispwDownloadWithCompileOnly;
		this.cpCategorizeOnComponentType = cpCategorizeOnComponentType;
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
	 * Gets the value of the 'SubAppl'
	 * 
	 * @return <code>String</code> value of m_serverSubAppl
	 */
	public String getServerSubAppl()
	{
		return m_serverSubAppl;
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
	 * This field indicates whether to clear unmatched items from the workspace as well as whether to only download items when
	 * the update date has changed.
	 * 
	 * @return the ispwDownloadAll
	 */
	public boolean getIspwDownloadAll()
	{
		return ispwDownloadAll;
	}

	/**
	 * Gets the value of the targetFolder
	 * 
	 * @return string containing the targetFolder location
	 */
	public String getTargetFolder()
	{
		return m_targetFolder;
	}
	
	/**
	 * This field determines whether or not to download the INCL impacts
	 * 
	 * @return the ispwDownloadAll
	 */
	public boolean getIspwDownloadIncl() 
	{
		return ispwDownloadIncl;
	}

	/**
	 * This field determine whether to download compile only in case of repository download
	 * 
	 * @return true if download compile only
	 */
	public boolean getIspwDownloadWithCompileOnly()
	{
		return ispwDownloadWithCompileOnly;
	}
	
	/**
	 * Categorize the source files to different folders according to Component Type
	 * 
	 * @return true if categorize on component type
	 */
	public boolean getCpCategorizeOnComponentType() 
	{
		return cpCategorizeOnComponentType;
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
	 * @throws AbortException
	 *             if an error occurs validating the parameters
	 */
	public void validateParameters(Launcher launcher, TaskListener listener, Item project) throws AbortException
	{
		CpwrGlobalConfiguration globalConfig = CpwrGlobalConfiguration.get();

		validateServerParameters(globalConfig, launcher, listener, project);
		validateFilterParameters(launcher, listener, project);
		validateCliLocation(globalConfig, launcher, listener);
	}

	/* (non-Javadoc)
	 * @see com.compuware.jenkins.scm.AbstractIspwConfiguration#validateFilterParameters(hudson.Launcher, hudson.model.TaskListener, hudson.model.Item)
	 */
	@Override
	public void validateFilterParameters(Launcher launcher, TaskListener listener, Item project)
	{
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

	}

	/**
	 * DescriptorImpl is used to create instances of <code>IspwConfiguration</code>. It also contains the global configuration
	 * options as fields, just like the <code>IspwConfiguration</code> contains the configuration options for a job
	 */
	@Extension
	public static class DescriptorImpl extends AbstractConfigurationImpl<IspwConfiguration>
	{
		public static final boolean ispwDownloadIncl = false;
		public static final boolean ispwDownloadWithCompileOnly = false;
		public static final boolean cpCategorizeOnComponentType = false;
		
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
			return Messages.displayNameIspwRepository();
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
			if (project == null) {
				Jenkins.get().checkPermission(Jenkins.ADMINISTER);
			} else {
				project.checkPermission(Item.CONFIGURE);
			}

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
	
	@Initializer(before = InitMilestone.PLUGINS_STARTED)
	public static void xStreamCompatibility() {
		Items.XSTREAM2.aliasField("ispwDownloadIncl", IspwConfiguration.class, "ispwDownloadIncl"); //$NON-NLS-1$ //$NON-NLS-2$
	}
}