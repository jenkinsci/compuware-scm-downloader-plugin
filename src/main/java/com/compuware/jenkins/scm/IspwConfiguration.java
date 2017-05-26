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
import com.compuware.jenkins.scm.global.SCMGlobalConfiguration;
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
import hudson.model.Descriptor.FormException;
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

public class IspwConfiguration extends SCM
{
	private final String m_hostPort;
	private final String m_credentialsId;
	private final String m_codePage;
	private final String m_serverConfig;
	private final String m_serverStream;
	private final String m_serverApplication;
	private final String m_serverLevel;
	private final String m_levelOption;
	private final String m_filterType;
	private final String m_filterName;
	
	@DataBoundConstructor
	public IspwConfiguration(String hostPort, String credentialsId, String codePage, String serverConfig, String serverStream,
			String serverApplication, String serverLevel, String levelOption, String filterType, String filterName)
	{
		m_hostPort = getTrimmedValue(hostPort);
		m_credentialsId = getTrimmedValue(credentialsId);
		m_codePage = getTrimmedValue(codePage);
		m_serverConfig = getTrimmedValue(serverConfig);
		m_serverStream = getTrimmedValue(serverStream);
		m_serverApplication = getTrimmedValue(serverApplication);
		m_serverLevel = getTrimmedValue(serverLevel);
		m_levelOption = getTrimmedValue(levelOption);
		m_filterType = getTrimmedValue(filterType);
		m_filterName = getTrimmedValue(filterName);			
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
			
			IspwDownloader downloader = new IspwDownloader(this);
			rtnValue = downloader.getSource(build, launcher, workspaceFilePath, listener, changelogFile);

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
	 * Gets the value of the 'Host'
	 * 
	 * @return <code>String</code> value of m_host
	 */
	public String getHostPort()
	{
		return m_hostPort;
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
	 * Gets the value of the 'Filter Name'
	 * 
	 * @return <code>String</code> value of m_filterName
	 */
	public String getFilterName()
	{
		return m_filterName;
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
	 * Validates the configuration parameters.
	 * @param launcher
	 *            The machine that the files will be checked out.
	 * @param listener
	 *            Build listener
	 * @param project the Jenkins project           
	 */
	public void validateParameters(Launcher launcher, TaskListener listener, Item project)
	{

		if (getLoginInformation(project) != null)
		{
			listener.getLogger().println(Messages.username() + " = " + getLoginInformation(project).getUsername()); //$NON-NLS-1$
		}
		else
		{
			throw new IllegalArgumentException(Messages.checkoutMissingParameterError(Messages.loginCredentials()));
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
		
		if (getServerConfig() != null)
		{
			listener.getLogger().println(Messages.ispwServerConfig() + " = " + getServerConfig());
		}
		else
		{
			throw new IllegalArgumentException(Messages.checkoutMissingParameterError(Messages.ispwServerConfig()));
		}
		
		if (getServerStream().isEmpty() == false)
		{
			listener.getLogger().println(Messages.ispwServerStream() + " = " + getServerStream());
		}
		else
		{
			throw new IllegalArgumentException(Messages.checkoutMissingParameterError(Messages.ispwServerStream()));
		}
		
		if (getServerApplication().isEmpty() == false)
		{
			listener.getLogger().println(Messages.ispwServerApp() + " = " + getServerApplication());
		}
		else
		{
			throw new IllegalArgumentException(Messages.checkoutMissingParameterError(Messages.ispwServerApp()));
		}
		
		if (getServerLevel().isEmpty() == false)
		{
			listener.getLogger().println(Messages.ispwServerLevel() + " = " + getServerLevel());
		}
		else
		{
			throw new IllegalArgumentException(Messages.checkoutMissingParameterError(Messages.ispwServerLevel()));
		}
		
		if (getLevelOption().isEmpty() == false)
		{
			listener.getLogger().println(Messages.ispwLevelOption() + " = " + getLevelOption());
		}
		else
		{
			throw new IllegalArgumentException(Messages.checkoutMissingParameterError(Messages.ispwLevelOption()));
		}

		if (getFilterName() != null)
		{
			listener.getLogger().println(Messages.ispwfilterName() + " = " + getFilterName());
		}
		else
		{
			throw new IllegalArgumentException(Messages.checkoutMissingParameterError(Messages.ispwfilterName()));
		}
		
		if (getFilterType() != null)
		{
			listener.getLogger().println(Messages.ispwfilterType() + " = " + getFilterType());
		}
		else
		{
			throw new IllegalArgumentException(Messages.checkoutMissingParameterError(Messages.ispwfilterType()));
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
	 * 
	 * DescriptorImpl is used to create instances of <code>IspwConfiguration</code>. It also contains the global configuration
	 * options as fields, just like the <code>IspwConfiguration</code> contains the configuration options for a job
	 * 
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
		 * Validator for the 'Host:port' text field.
		 * 
		 * @param value
		 *            value passed from the config.jelly "host:port" field
		 * 
		 * @return validation message
		 * 
		 * @throws IOException
		 * @throws ServletException
		 */
		public FormValidation doCheckHostPort(@QueryParameter String value) throws IOException, ServletException
		{
			String tempValue = StringUtils.trimToEmpty(value);
			if (tempValue.isEmpty() == true)
			{
				return FormValidation.error(Messages.checkHostPortEmptyError());
			}
			else
			{
				String[] hostPort = StringUtils.split(tempValue, Constants.COLON);
				if (hostPort.length != 2)
				{
					return FormValidation.error(Messages.checkHostPortFormatError());
				}
				else
				{
					String host = StringUtils.trimToEmpty(hostPort[0]);
					if (host.isEmpty() == true)
					{
						return FormValidation.error(Messages.checkHostPortMissingHostError());
					}

					String port = StringUtils.trimToEmpty(hostPort[1]);
					if (port.isEmpty() == true)
					{
						return FormValidation.error(Messages.checkHostPortMissingPortError());
					}
					else if (StringUtils.isNumeric(port) == false)
					{
						return FormValidation.error(Messages.checkHostPortInvalidPorttError());
					}
				}
			}

			return FormValidation.ok();
		}
		

		/**
		 * Validator for the 'Login Credential' field
		 * 
		 * @param value
		 *            value passed from the config.jelly "loginCredential" field
		 * @return validation message
		 * @throws IOException
		 * @throws ServletException
		 */
		public FormValidation doCheckCredentialsId(@QueryParameter String value) throws IOException, ServletException
		{
			String tempValue = StringUtils.trimToEmpty(value);
			if (tempValue.isEmpty() == true)
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
		 * 
		 * @throws IOException
		 * @throws ServletException
		 */
		public FormValidation doCheckServerStream(@QueryParameter String value) throws IOException, ServletException
		{
			String tempValue = StringUtils.trimToEmpty(value);
			if (tempValue.isEmpty() == true)
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
		 * 
		 * @throws IOException
		 * @throws ServletException
		 */
		public FormValidation doCheckServerApplication(@QueryParameter String value) throws IOException, ServletException
		{
			String tempValue = StringUtils.trimToEmpty(value);
			if (tempValue.isEmpty() == true)
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
		 * 
		 * @throws IOException
		 * @throws ServletException
		 */
		public FormValidation doCheckServerLevel(@QueryParameter String value) throws IOException, ServletException
		{
			String tempValue = StringUtils.trimToEmpty(value);
			if (tempValue.isEmpty() == true)
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

			model.add(new Option(StringUtils.EMPTY, StringUtils.EMPTY, false));

			for (StandardUsernamePasswordCredentials c : creds)
			{
				boolean isSelected = false;

				if (credentialsId != null)
				{
					isSelected = credentialsId.matches(c.getId());
				}

				String description = Util.fixEmptyAndTrim(c.getDescription());
				model.add(new Option(c.getUsername()
						+ (description != null ? " (" + description + ")" : StringUtils.EMPTY), c.getId(), isSelected)); //$NON-NLS-1$ //$NON-NLS-2$
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
		 * Fills in the Level option selection box with ISPW level options
		 *
		 * @return level option selections
		 * @throws IOException
		 * @throws ServletException
		 */
		public ListBoxModel doFillLevelOptionItems() throws IOException, ServletException
		{
			ListBoxModel levelOptionModel = new ListBoxModel();
			
			levelOptionModel.add(Messages.ispwDropLevelOnly(), "0");
			levelOptionModel.add(Messages.ispwDropLevelAbove(), "1");
			
			return levelOptionModel;
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
