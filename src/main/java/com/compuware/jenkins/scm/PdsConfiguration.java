/**
 * The MIT License (MIT)
 * 
 * Copyright (c) 2015 - 2019 Compuware Corporation
 * (c) Copyright 2015 - 2019, 2021 BMC Software, Inc.
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

import java.io.File;
import java.io.IOException;

import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

import com.compuware.jenkins.common.configuration.CpwrGlobalConfiguration;
import com.compuware.jenkins.common.configuration.HostConnection;

import hudson.AbortException;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.Item;
import hudson.model.Job;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.scm.SCMDescriptor;
import hudson.scm.SCMRevisionState;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import hudson.util.ListBoxModel.Option;
import jenkins.model.Jenkins;
import net.sf.json.JSONObject;

/**
 * Captures the configuration information for a PDS SCM.
 */
public class PdsConfiguration extends CpwrScmConfiguration
{
	/**
	 * Gets the data from the configuration page. The parameter names must match the field names set by
	 * <code>config.jelly</code>.
	 * 
	 * @param connectionId
				  a unique host connection identifier
	 * @param filterPattern
	 *            filter for the datasets to be retrieved from the mainframe
	 * @param fileExtension
	 *            file extension for the incoming datasets
	 * @param credentialsId
	 *            unique id of the selected credential
	 * @param targetFolder
	 *            source download location
	 */
	@DataBoundConstructor
	public PdsConfiguration(String connectionId, String filterPattern, String fileExtension, String credentialsId, String targetFolder)
	{
		super(connectionId, filterPattern, fileExtension, credentialsId, targetFolder);
	}

	/**
	 * Method that is first called when a build is run. All dataset retrieval stems from here.
	 * 
	 * @param launcher
	 *            the machine that the files will be checked out
	 * @param workspaceFilePath
	 *            a directory to check out the source code
	 * @param listener
	 *            build listener
	 * @param changelogFile
	 *            upon a successful return, this file should capture the changelog. When there's no change, this file should
	 *            contain an empty entry
	 * @param baseline
	 *            used for polling (this parameter is not used)
	 * 
	 * @throws IOException if an I/O error occurs downloading source 
	 * @throws InterruptedException if downloading source is interrupted by another thread
	 */
	@Override
	public void checkout(Run<?, ?> build, Launcher launcher, FilePath workspaceFilePath, TaskListener listener,
			File changelogFile, SCMRevisionState baseline) throws IOException, InterruptedException
	{
		boolean rtnValue = false;

		try
		{
			validateParameters(launcher, listener, build.getParent());

			PdsDownloader downloader = new PdsDownloader(this);

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
	public SCMRevisionState calcRevisionsFromBuild(Run<?, ?> build, FilePath workspace, Launcher launcher, TaskListener listener)
            throws IOException, InterruptedException
    {
    	return null;
    }      

	/**
	 * Returns the ScmDescriptor for the SCM object. The ScmDescriptor is used to create new instances of the SCM.
	 */
	@Override
	public PdsDescriptorImpl getDescriptor()
	{
		return (PdsDescriptorImpl) super.getDescriptor();
	}

	/**
	 * DescriptorImpl is used to create instances of <code>PdsConfiguration</code>. It also contains the global configuration
	 * options as fields, just like the <code>PdsConfiguration</code> contains the configuration options for a job
	 */
	@Extension
	public static class PdsDescriptorImpl extends SCMDescriptor<PdsConfiguration>
	{
		/**
		 * Constructor.
		 */
		public PdsDescriptorImpl()
		{
			super(PdsConfiguration.class, null);
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
			return Messages.displayNamePDS();
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
		 * @return <code>true</code> if able to configure and continue to next page
		 * 
		 * @throws FormException if an error occurs submitting the form data
		 */
		@Override
		public boolean configure(StaplerRequest req, JSONObject formData) throws FormException
		{		
			save();
			return super.configure(req, formData);
		}

		/**
		 * Validator for the 'Filter pattern' text field.
		 * 
		 * @param value
		 *            value passed from the config.jelly "filterPattern" field
		 * 
		 * @return validation message
		 */
		public FormValidation doCheckFilterPattern(@QueryParameter String value)
		{
			String tempValue = StringUtils.trimToEmpty(value);
			if (tempValue.isEmpty())
			{
				return FormValidation.error(Messages.checkFilterPatternEmptyError());
			}

			return FormValidation.ok();
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
		 * Validator for the 'File extension to assign' text field.
		 * 
		 * @param value
		 *            value passed from the config.jelly "fileExtension" field
		 * 
		 * @return validation message
		 */
		public FormValidation doCheckFileExtension(@QueryParameter String value)
		{
			String tempValue = StringUtils.trimToEmpty(value);
			if (tempValue.isEmpty())
			{
				return FormValidation.error(Messages.checkFileExtensionEmptyError());
			}
			else if (!StringUtils.isAlphanumeric(tempValue))
			{
				return FormValidation.error(Messages.checkFileExtensionFormatError());
			}

			return FormValidation.ok();
		}

		/**
		 * Validator for the 'Login Credentials' field.
		 * 
		 * @param value
		 *            value passed from the config.jelly "credentialsId" field
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
	}
}