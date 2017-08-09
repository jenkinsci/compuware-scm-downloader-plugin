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
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import org.apache.commons.lang.StringUtils;
import org.jvnet.hudson.test.JenkinsRule;
import com.compuware.jenkins.common.configuration.CpwrGlobalConfiguration;
import com.compuware.jenkins.common.configuration.HostConnection;
import com.compuware.jenkins.scm.CpwrScmConfiguration;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.TopLevelItem;

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

	/**
	 * Test migration of the host connection.
	 * @param jenkinsRule
	 *            the Jenkins rule
	 */
	public static void migrateDataTest(JenkinsRule jenkinsRule)
	{
		try
		{
			TopLevelItem item = jenkinsRule.jenkins.getItem("TestProject");
			assertDataMigrated(item);
		}
		catch (Exception e)
		{
			// Add the print of the stack trace because the exception message is not enough to troubleshoot the root issue. For
			// example, if the exception is constructed without a message, you get no information from executing fail().
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	/**
	 * Test data has been migrated.
	 * 
	 * @param proj
	 *            project being migrated
	 * @throws IOException
	 */
	private static void assertDataMigrated(TopLevelItem proj) throws IOException
	{
		assertThat(proj, instanceOf(FreeStyleProject.class));
		FreeStyleProject project = (FreeStyleProject) proj;
		CpwrScmConfiguration config = (CpwrScmConfiguration) project.getScm();

		assertNotNull(config.getConnectionId());

		CpwrGlobalConfiguration globalConfig = CpwrGlobalConfiguration.get();
		HostConnection connection = globalConfig.getHostConnection(config.getConnectionId());
		assertNotNull(connection);

		File inputFile = project.getConfigFile().getFile();
		BufferedReader br = new BufferedReader(new FileReader(inputFile));
		try
		{
			String line = null;

			// Lets use the TreeMap for always correct ordering
			while ((line = br.readLine()) != null)
			{
				line = line.trim();

				String tagName = line.substring(0, line.indexOf(">") + 1);
				if (TestConstants.HOST_PORT_OPEN_TAG.equals(tagName))
				{
					String hostPort = StringUtils.substringBetween(line, tagName, TestConstants.HOST_PORT_CLOSE_TAG);
					String expectedHost = StringUtils.substringBefore(hostPort, TestConstants.COLON);
					String expectedPort = StringUtils.substringAfter(hostPort, TestConstants.COLON);
					assertThat(String.format("Expected HostConnection.getHost() to return %s", expectedHost),
							connection.getHost(), is(equalTo(expectedHost)));
					assertThat(String.format("Expected HostConnection.getPort() to return %s", expectedPort),
							connection.getPort(), is(equalTo(expectedPort)));
				}
				else if (TestConstants.CODE_PAGE_OPEN_TAG.equals(tagName))
				{
					String expectedCodePage = StringUtils.substringBetween(line, tagName, TestConstants.CODE_PAGE_CLOSE_TAG);
					assertThat(String.format("Expected HostConnection.getCodePage() to return %s", expectedCodePage),
							connection.getCodePage(), is(equalTo(expectedCodePage)));
				}
			}
		}
		finally
		{
			br.close();
		}
	}
}