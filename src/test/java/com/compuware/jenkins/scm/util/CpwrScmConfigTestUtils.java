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

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import org.jvnet.hudson.test.JenkinsRule;
import com.compuware.jenkins.scm.CpwrScmConfiguration;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;

/**
 * Utility class to handle test file routines.
 */
@SuppressWarnings("nls")
public class CpwrScmConfigTestUtils
{
	/**
	 * Hidden constructor.
	 */
	private CpwrScmConfigTestUtils()
	{
	}

	/**
	 * Validates the construction of a Compuware SCM configuration, verifying configuration values.
	 * 
	 * @param scmConfig
	 *            SCM configuration to validate
	 */
	public static void validateCpwrScmConfigurationConstruction(CpwrScmConfiguration scmConfig)
	{
		String className = scmConfig.getClass().getName();
		assertThat(String.format("Expected %s.getConnectionId() to return %s", className, TestConstants.EXPECTED_CONNECTION_ID),
				scmConfig.getConnectionId(), is(equalTo(TestConstants.EXPECTED_CONNECTION_ID)));

		assertThat(
				String.format("Expected %s.getCredentialsId() to return %s", className, TestConstants.EXPECTED_CREDENTIALS_ID),
				scmConfig.getCredentialsId(), is(equalTo(TestConstants.EXPECTED_CREDENTIALS_ID)));

		assertThat(
				String.format("Expected %s.getFilterPattern() to return %s", className, TestConstants.EXPECTED_FILTER_PATTERN),
				scmConfig.getFilterPattern(), is(equalTo(TestConstants.EXPECTED_FILTER_PATTERN)));

		assertThat(
				String.format("Expected %s.getFileExtension() to return %s", className, TestConstants.EXPECTED_FILE_EXTENSION),
				scmConfig.getFileExtension(), is(equalTo(TestConstants.EXPECTED_FILE_EXTENSION)));
	}

	/**
	 * Tests the results of a SCM download execution.
	 * <p>
	 * A project is created, configured and executed where the log is examined to verify results.
	 */
	public static void executionTest(JenkinsRule jenkinsRule, CpwrScmConfiguration scmConfig)
	{
		try
		{
			FreeStyleProject project = jenkinsRule.createFreeStyleProject("TestProject");
			project.setScm(scmConfig);

			// don't expect the build to succeed since no CLI exists
			if (project.scheduleBuild(null))
			{
				while (project.getLastCompletedBuild() == null)
				{
					// wait for the build to complete before obtaining the log
					continue;
				}

				FreeStyleBuild build = project.getLastCompletedBuild();
				String logFileOutput = JenkinsRule.getLog(build);

				String expectedConnectionStr = String.format("-host \"%s\" -port \"%s\"", TestConstants.EXPECTED_HOST,
						TestConstants.EXPECTED_PORT);
				assertThat("Expected log to contain Host connection: " + expectedConnectionStr + '.', logFileOutput,
						containsString(expectedConnectionStr));

				String expectedCodePageStr = String.format("-code %s", TestConstants.EXPECTED_CODE_PAGE);
				assertThat("Expected log to contain Host code page: " + expectedCodePageStr + '.', logFileOutput,
						containsString(expectedCodePageStr));

				String expectedTimeoutStr = String.format("-timeout \"%s\"", TestConstants.EXPECTED_TIMEOUT);
				assertThat("Expected log to contain Host timeout: " + expectedTimeoutStr + '.', logFileOutput,
						containsString(expectedTimeoutStr));

				String expectedCredentialsStr = String.format("-id \"%s\" -pass %s", TestConstants.EXPECTED_USER_ID,
						TestConstants.EXPECTED_PASSWORD);
				assertThat("Expected log to contain Login credentials: " + expectedCredentialsStr + '.', logFileOutput,
						containsString(expectedCredentialsStr));

				assertThat(
						String.format("Expected log to contain filter pattern: \"%s\".", TestConstants.EXPECTED_FILTER_PATTERN),
						logFileOutput, containsString(TestConstants.EXPECTED_FILTER_PATTERN));

				assertThat(
						String.format("Expected log to contain file extension: \"%s\".", TestConstants.EXPECTED_FILE_EXTENSION),
						logFileOutput, containsString(TestConstants.EXPECTED_FILE_EXTENSION));
			}
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