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

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.ResourceBundle;
import java.util.Set;
import javax.servlet.ServletException;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardListBoxModel;
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;
import com.cloudbees.plugins.credentials.domains.DomainRequirement;
import com.compuware.jenkins.scm.utils.Constants;
import hudson.AbortException;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.Util;
import hudson.model.Item;
import hudson.model.Job;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.scm.SCMDescriptor;
import hudson.scm.SCMRevisionState;
import hudson.security.ACL;
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
	 * @param hostPort
	 *            'Host':'Port'
	 * @param filterPattern
	 *            filter for the datasets to be retrieved from the mainframe
	 * @param fileExtension
	 *            file extension for the incoming datasets
	 * @param credentialsId
	 *            unique id of the selected credential
	 * @param codePage
	 *            code page
	 */
	@DataBoundConstructor
	public PdsConfiguration(String hostPort, String filterPattern, String fileExtension, String credentialsId, String codePage)
	{
		super(hostPort, filterPattern, fileExtension, credentialsId, codePage);
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
	 * @param baseline  used for polling - this parameter is not used           
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

			PdsDownloader downloader = new PdsDownloader(this);
			rtnValue = downloader.getSource(build, launcher, workspaceFilePath, listener, changelogFile, getFilterPattern());

			if (rtnValue == false)
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
	 * Validates the configuration parameters.
	 * @param launcher
	 *            The machine that the files will be checked out.
	 * @param listener
	 *            Build listener
	 */
	public void validateParameters(Launcher launcher, TaskListener listener, Item project)
	{
		if (getLoginInformation(project) != null)
		{
			listener.getLogger().println(Messages.username() + " = " + getLoginInformation(project).getUsername()); //$NON-NLS-1$
		}
		else
		{
			throw new IllegalArgumentException(Messages.checkoutMissingParameterError(Messages.loginCredential()));
		}

		validateHostPort(listener);
		
		
		if (getCodePage().isEmpty() == false)
		{
			listener.getLogger().println(Messages.codePage() + " = " + getCodePage()); //$NON-NLS-1$
		}
		else
		{
			throw new IllegalArgumentException(Messages.checkoutMissingParameterError(Messages.codePage()));
		}

		if (getFilterPattern().isEmpty() == false)
		{
			listener.getLogger().println(Messages.filterPattern() + " = " + getFilterPattern()); //$NON-NLS-1$
		}
		else
		{
			throw new IllegalArgumentException(Messages.checkoutMissingParameterError(Messages.filterPattern()));
		}

		if (getFileExtension().isEmpty() == false)
		{
			listener.getLogger().println(Messages.fileExtension() + " = " + getFileExtension()); //$NON-NLS-1$
		}
		else
		{
			throw new IllegalArgumentException(Messages.checkoutMissingParameterError(Messages.fileExtension()));
		}

		String cliLocation = getTopazCLILocation(launcher);
		if ((cliLocation != null) && (cliLocation.isEmpty() == false))
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
	public SCMRevisionState calcRevisionsFromBuild(Run<?, ?> build, FilePath workspace, Launcher launcher, TaskListener listener)
            throws IOException, InterruptedException
    {
    	return null;
    }      

	/**
	 * Returns the ScmDescriptor for the SCM object. The ScmDescriptor is used to create new instances of the SCM.
	 */
	@Override
	public DescriptorImpl getDescriptor()
	{
		return (DescriptorImpl) super.getDescriptor();
	}

	/**
	 * 
	 * DescriptorImpl is used to create instances of <code>PdsConfiguration</code>. It also contains the global configuration
	 * options as fields, just like the <code>PdsConfiguration</code> contains the configuration options for a job
	 * 
	 */
	@Extension
	public static class DescriptorImpl extends SCMDescriptor<PdsConfiguration>
	{
		public DescriptorImpl()
		{
			super(PdsConfiguration.class, null);
			load();
		}

		/**
		 * Necessary to display UI in Jenkins Pipeline.
		 */
		@Override 
		public boolean isApplicable(Job project) {
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
		 * @return TRUE if able to configure and continue to next page
		 * @throws FormException
		 */
		@Override
		public boolean configure(StaplerRequest req, JSONObject formData) throws FormException
		{		
			save();
			return super.configure(req, formData);
		}

		/**
		 * Validator for the 'Filter pattern' text field
		 * 
		 * @param value
		 *            value passed from the "filterPattern" field
		 * @return validation message
		 * @throws IOException
		 * @throws ServletException
		 */
		public FormValidation doCheckFilterPattern(@QueryParameter String value) throws IOException, ServletException
		{
			if (value.trim().isEmpty())
			{
				return FormValidation.error(Messages.checkFilterPatternEmptyError());
			}

			return FormValidation.ok();
		}

		/**
		 * Validator for the 'Password' text field
		 * 
		 * @param value
		 *            value passed from the config.jelly "password" field
		 * @return validation message
		 * @throws IOException
		 * @throws ServletException
		 */
		public FormValidation doCheckHostPort(@QueryParameter String value) throws IOException, ServletException
		{
			value = value.trim();
			if (value.isEmpty())
			{
				return FormValidation.error(Messages.checkHostPortEmptyError());
			}
			else if (value.contains(Constants.COLON) == false)
			{
				return FormValidation.error(Messages.checkHostPortFormatError());
			}
			else
			{
				int colonIndex = value.indexOf(Constants.COLON);
				if ((colonIndex == 0))
				{
					return FormValidation.error(Messages.checkHostPortMissingHostError());
				}
				else if (colonIndex == (value.length() - 1))
				{
					return FormValidation.error(Messages.checkHostPortMissingPortError());
				}
			}

			return FormValidation.ok();
		}

		/**
		 * Validator for the 'File Extension' text field
		 * 
		 * @param value
		 *            value passed from the config.jelly "fileExtension" field
		 * @return validation message
		 * @throws IOException
		 * @throws ServletException
		 */
		public FormValidation doCheckFileExtension(@QueryParameter String value) throws IOException, ServletException
		{
			if (value.trim().isEmpty())
			{
				return FormValidation.error(Messages.checkFileExtensionEmptyError());
			}
			else if (value.contains(".")) //$NON-NLS-1$
			{
				return FormValidation.error(Messages.checkFileExtensionFormatError());
			}

			return FormValidation.ok();
		}

		/**
		 * Validator for the 'Login Credential' field
		 * 
		 * @param value
		 *            value passed from the config.jelly "fileExtension" field
		 * @return validation message
		 * @throws IOException
		 * @throws ServletException
		 */
		public FormValidation doCheckCredentialsId(@QueryParameter String value) throws IOException, ServletException
		{
			if (value.equalsIgnoreCase(Constants.EMPTY_STRING))
			{
				return FormValidation.error(Messages.checkLoginCredentialError());
			}

			return FormValidation.ok();
		}

		/**
		 * Fills in the Login Credential selection box with applicable Jenkins credentials
		 * 
		 * @param context
		 *            filter for credentials
		 * @return credential selections
		 * @throws IOException
		 * @throws ServletException
		 */
		public ListBoxModel doFillCredentialsIdItems(@AncestorInPath Jenkins context, @QueryParameter String credentialsId, @AncestorInPath Item project) throws IOException, ServletException
		{
			List<StandardUsernamePasswordCredentials> creds = CredentialsProvider
					.lookupCredentials(StandardUsernamePasswordCredentials.class, project, ACL.SYSTEM,
							Collections.<DomainRequirement> emptyList());

			StandardListBoxModel model = new StandardListBoxModel();

			model.add(new Option(Constants.EMPTY_STRING, Constants.EMPTY_STRING, false));

			for (StandardUsernamePasswordCredentials c : creds)
			{
				boolean isSelected = false;

				if (credentialsId != null)
				{
					isSelected = credentialsId.matches(c.getId());
				}

				String description = Util.fixEmptyAndTrim(c.getDescription());
				model.add(new Option(c.getUsername()
						+ (description != null ? " (" + description + ")" : Constants.EMPTY_STRING), c.getId(), isSelected)); //$NON-NLS-1$ //$NON-NLS-2$
			}

			return model;
		}
		
		/**
		 * Fills in the Code page selection box with code pages
		 *
		 * @return code page selections
		 * @throws IOException
		 * @throws ServletException
		 */
		public ListBoxModel doFillCodePageItems() throws IOException, ServletException
		{
			ListBoxModel codePageModel = new ListBoxModel();
			
			ResourceBundle cpBundle = ResourceBundle.getBundle("com.compuware.jenkins.scm.codePageMappings"); //$NON-NLS-1$
			Set<String> cpNumberSet = cpBundle.keySet();

			// sort the code page values (for display purposes)
			List<String> cpNumberList = new ArrayList<String>(cpNumberSet);
			Collections.sort(cpNumberList, new NumericStringComparator());

			Iterator<String> iterator = cpNumberList.iterator();
			while (iterator.hasNext() == true)
			{
				String cpNumber = iterator.next();
				String cpDescription = cpBundle.getString(cpNumber);
				
				codePageModel.add(cpDescription, cpNumber);
			}
			
			return codePageModel;
		}
		
		/**
		 * Comparator for comparing Strings numerically.
		 */
		private static class NumericStringComparator implements Comparator<String>, Serializable
		{
			private static final long serialVersionUID = 1L;

			/*
			 * (non-Javadoc)
			 * 
			 * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
			 */
			@Override
			public int compare(String numStr1, String numStr2)
			{
				int intVal1 = Integer.parseInt(numStr1);
				int intVal2 = Integer.parseInt(numStr2);

				return intVal1 - intVal2;
			}
		}
	}
}
