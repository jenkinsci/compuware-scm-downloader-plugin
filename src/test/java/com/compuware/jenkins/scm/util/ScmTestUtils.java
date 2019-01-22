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
		hostConnection.put(TestConstants.PROTOCOL, TestConstants.EXPECTED_ENCRYPTION_PROTOCOL);
		hostConnection.put(TestConstants.CODE_PAGE, TestConstants.EXPECTED_CODE_PAGE);
		hostConnection.put(TestConstants.TIMEOUT, TestConstants.EXPECTED_TIMEOUT);
		hostConnection.put(TestConstants.CONNECTION_ID, TestConstants.EXPECTED_CONNECTION_ID);
		hostConnection.put(TestConstants.CES_URL, TestConstants.EXPECTED_CES_URL);

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