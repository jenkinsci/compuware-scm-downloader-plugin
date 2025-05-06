/**
 * The MIT License (MIT)
 * 
 * Copyright (c) 2015 - 2019 Compuware Corporation
 * (c) Copyright 2015 - 2025 BMC Software, Inc.
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
public class IspwContainerConfiguration extends AbstractIspwConfiguration
{
	private String ispwContainerName;
	private String ispwContainerType;
	private String ispwServerLevel = StringUtils.EMPTY;
	private String ispwComponentType = StringUtils.EMPTY;
	private boolean ispwDownloadAll = false;
	private boolean ispwDownloadIncl = DescriptorImpl.ispwDownloadIncl;
	private boolean cpCategorizeOnComponentType = DescriptorImpl.cpCategorizeOnComponentType;
	private String ispwTargetFolder;

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
	 * @param containerName
	 *            - selected ispw container name
	 * @param containerType
	 *            - selected ispw container type (assignment, release...)
	 * @param serverLevel
	 *            - selected ispw level
	 * @param componentType
	 *            - The component type to filter for
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
	public IspwContainerConfiguration(String connectionId, String credentialsId, String serverConfig, String containerName,
			String containerType, String serverLevel, String componentType, 
			boolean ispwDownloadAll, String targetFolder, boolean ispwDownloadIncl, boolean cpCategorizeOnComponentType)
	{
		super(connectionId, credentialsId, serverConfig);

		ispwContainerType = getTrimmedValue(containerType);
		ispwContainerName = getTrimmedValue(containerName);
		ispwServerLevel = getTrimmedValue(serverLevel);
		ispwComponentType = getTrimmedValue(componentType);
		ispwTargetFolder = getTrimmedValue(targetFolder);
		this.ispwDownloadAll = ispwDownloadAll;
		this.ispwDownloadIncl = ispwDownloadIncl;
		this.cpCategorizeOnComponentType = cpCategorizeOnComponentType;
	}

	/**
	 * Gets the value of the 'Container name'
	 * 
	 * @return <code>String</code> value of containerName
	 */
	public String getContainerName()
	{
		return ispwContainerName;
	}

	/**
	 * Gets the value of the 'Container type'
	 * 
	 * @return <code>String</code> value of containerType
	 */
	public String getContainerType()
	{
		return ispwContainerType;
	}

	/**
	 * Gets the value of the 'Level'
	 * 
	 * @return <code>String</code> value of serverLevel
	 */
	public String getServerLevel()
	{
		return ispwServerLevel;
	}

	/**
	 * Gets the value of the 'Component type'
	 * 
	 * @return <code>String</code> value of componentType
	 */
	public String getComponentType()
	{
		return ispwComponentType;
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
		return ispwTargetFolder;
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
	 * Categorize the source files to different folders according to Component Type
	 * 
	 * @return true if categorize on component type
	 */
	public boolean getCpCategorizeOnComponentType() 
	{
		return cpCategorizeOnComponentType;
	}
	
	/**
	 * Validates the configuration parameters
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

	/**
	 * Validates the configuration filter parameters.
	 *
	 * @param launcher
	 *            The machine that the files will be checked out.
	 * @param listener
	 *            Build listener
	 * @param project
	 *            the Jenkins project
	 */
	public void validateFilterParameters(Launcher launcher, TaskListener listener, Item project)
	{
		if (!getContainerName().isEmpty())
		{
			listener.getLogger().println(Messages.ispwContainerName() + " = " + getContainerName()); //$NON-NLS-1$
		}
		else
		{
			throw new IllegalArgumentException(Messages.checkoutMissingParameterError(Messages.ispwContainerName()));
		}

		if (!getContainerType().isEmpty())
		{
			listener.getLogger().println(Messages.ispwContainerType() + " = " + getContainerType()); //$NON-NLS-1$
		}
		else
		{
			throw new IllegalArgumentException(Messages.checkoutMissingParameterError(Messages.ispwContainerType()));
		}

	}
	
	/**
	 * DescriptorImpl is used to create instances of <code>IspwContainerConfiguration</code>. It also contains the global
	 * configuration options as fields, just like the <code>IspwContainerConfiguration</code> contains the configuration options
	 * for a job
	 */
	@Extension
	public static class DescriptorImpl extends AbstractConfigurationImpl<IspwContainerConfiguration>
	{
		public static final boolean ispwDownloadIncl = true;
		public static final boolean cpCategorizeOnComponentType = false;
		
		public DescriptorImpl()
		{
			super(IspwContainerConfiguration.class, null);
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
			return Messages.displayNameIspwContainer();
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
		 * Validator for the 'Container name' field
		 * 
		 * @param value
		 *            value passed from the config.jelly "containerName" field
		 * 
		 * @return validation message
		 */
		public FormValidation doCheckContainerName(@QueryParameter String value)
		{
			String tempValue = StringUtils.trimToEmpty(value);
			if (tempValue.isEmpty())
			{
				return FormValidation.error(Messages.checkIspwContainerNameError());
			}

			return FormValidation.ok();
		}

		/**
		 * Validator for the 'Container type' field
		 * 
		 * @param value
		 *            value passed from the config.jelly "containerType" field
		 * 
		 * @return validation message
		 */
		public FormValidation doCheckContainerType(@QueryParameter String value)
		{
			String tempValue = StringUtils.trimToEmpty(value);
			if (tempValue.isEmpty())
			{
				return FormValidation.error(Messages.checkIspwContainerTypeError());
			}

			return FormValidation.ok();
		}

		/**
		 * Fills in the Container type selection box with ISPW container types
		 *
		 * @return container type selections
		 */
		public ListBoxModel doFillContainerTypeItems()
		{
			ListBoxModel containerTypeModel = new ListBoxModel();

			containerTypeModel.add(Messages.ispwAssignment(), "0"); //$NON-NLS-1$
			containerTypeModel.add(Messages.ispwRelease(), "1"); //$NON-NLS-1$
			containerTypeModel.add(Messages.ispwSet(), "2"); //$NON-NLS-1$

			return containerTypeModel;
		}
	}
	
	@Initializer(before = InitMilestone.PLUGINS_STARTED)
	public static void xStreamCompatibility() {
		Items.XSTREAM2.aliasField("ispwDownloadIncl", IspwContainerConfiguration.class, "ispwDownloadIncl"); //$NON-NLS-1$ //$NON-NLS-2$
	}
}
