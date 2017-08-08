/**
 * These materials contain confidential information and trade secrets of Compuware Corporation. You shall maintain the materials
 * as confidential and shall not disclose its contents to any third party except as may be required by law or regulation. Use,
 * disclosure, or reproduction is prohibited without the prior express written permission of Compuware Corporation.
 * 
 * All Compuware products listed within the materials are trademarks of Compuware Corporation. All other company or product
 * names are trademarks of their respective owners.
 * 
 * Copyright (c) 2017 Compuware Corporation. All rights reserved.
 */
package com.compuware.jenkins.scm.util;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import java.io.IOException;
import org.jvnet.hudson.test.JenkinsRule;
import org.kohsuke.stapler.Stapler;
import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.SystemCredentialsProvider;
import com.cloudbees.plugins.credentials.impl.UsernamePasswordCredentialsImpl;
import com.compuware.jenkins.common.configuration.CpwrGlobalConfiguration;
import hudson.model.FreeStyleProject;
import hudson.scm.SCM;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

/**
 * Utility class to handle test file routines.
 */
@SuppressWarnings("nls")
public class ScmTestUtils
{
	/**
	 * Hidden constructor.
	 */
	private ScmTestUtils()
	{
	}

	/**
	 * Setup the global configuration.
	 * 
	 * @throws IOException
	 */
	public static void setupGlobalConfiguration() throws IOException
	{
		JSONObject hostConnection = new JSONObject();
		hostConnection.put(TestConstants.DESCRIPTION, "TestConnection");
		hostConnection.put(TestConstants.HOST_PORT, TestConstants.EXPECTED_HOST + ':' + TestConstants.EXPECTED_PORT);
		hostConnection.put(TestConstants.CODE_PAGE, TestConstants.EXPECTED_CODE_PAGE);
		hostConnection.put(TestConstants.TIMEOUT, TestConstants.EXPECTED_TIMEOUT);
		hostConnection.put(TestConstants.CONNECTION_ID, TestConstants.EXPECTED_CONNECTION_ID);

		JSONArray hostConnections = new JSONArray();
		hostConnections.add(hostConnection);

		JSONObject json = new JSONObject();
		json.put("hostConn", hostConnections);
		json.put(TestConstants.TOPAZ_CLI_LOCATION_LINUX, "/opt/Compuware/TopazCLI");
		json.put(TestConstants.TOPAZ_CLI_LOCATION_WINDOWS, "C:\\Program Files\\Compuware\\Topaz Workbench CLI");

		CpwrGlobalConfiguration globalConfig = CpwrGlobalConfiguration.get();
		globalConfig.configure(Stapler.getCurrentRequest(), json);

		SystemCredentialsProvider.getInstance().getCredentials().add(new UsernamePasswordCredentialsImpl(CredentialsScope.USER,
				TestConstants.EXPECTED_CREDENTIALS_ID, null, TestConstants.EXPECTED_USER_ID, TestConstants.EXPECTED_PASSWORD));
		SystemCredentialsProvider.getInstance().save();
	}

	/**
	 * Perform a round trip test on the SCM configuration.
	 * <p>
	 * A project is created, configured, submitted / saved, and reloaded where the original configuration is compared against
	 * the reloaded configuration for equality.
	 * 
	 * @param jenkinsRule
	 *            the Jenkins rule
	 * @param scmConfig
	 *            the configuration to perform the round trip on
	 * @properties ','-separated list of property names that are compared.
	 */
	public static void roundTripTest(JenkinsRule jenkinsRule, SCM scmConfig, String properties)
	{
		try
		{
			FreeStyleProject project = jenkinsRule.createFreeStyleProject("TestProject");
			project.setScm(scmConfig);

			// workaround for eclipse compiler Ambiguous method call
			project.save();
			jenkinsRule.jenkins.reload();

			FreeStyleProject reloaded = jenkinsRule.jenkins.getItemByFullName(project.getFullName(), FreeStyleProject.class);
			assertNotNull(reloaded);

			SCM after = (SCM) reloaded.getScm();
			assertNotNull(after);

			jenkinsRule.assertEqualBeans(scmConfig, after, properties);
		}
		catch (Exception e)
		{
			// Add the print of the stack trace because the exception message is not enough to troubleshoot the root issue. For
			// example, if the exception is constructed without a message, you get no information from executing fail().
			e.printStackTrace();
			fail(e.getMessage());
		}
	}
}