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
 */
package com.compuware.jenkins.scm;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import com.compuware.jenkins.scm.util.ScmTestUtils;
import com.compuware.jenkins.scm.util.TestConstants;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;

/**
 * IspwConfiguration unit tests.
 */
@SuppressWarnings("nls")
public class IspwConfigurationTest
{
	// Constants
	private static final String EXPECTED_SERVER_CONFIG = "TPZP";
	private static final String EXPECTED_SERVER_STREAM = "PLAY";
	private static final String EXPECTED_SERVER_APPLICATION = "PLAY";
	private static final String EXPECTED_SERVER_LEVEL = "DEV1";
	private static final String EXPECTED_LEVEL_OPTION = "Selected level only";
	private static final String EXPECTED_FILTER_TYPE = "COB";
	private static final String EXPECTED_FOLDER_NAME = "TREXX12";
	private static final boolean EXPECTED_GET_FILES = true;
	private static final boolean EXPECTED_GET_FOLDERS = true;

	// Member Variables
	@Rule
	public JenkinsRule m_jenkinsRule = new JenkinsRule();

	@Before
	public void setup()
	{
		try
		{
			ScmTestUtils.setupGlobalConfiguration();
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
	 * Tests the construction of the configuration, verifying configuration values.
	 */
	@Test
	public void constructConfigurationTest()
	{
		IspwConfiguration scm = new IspwConfiguration(TestConstants.EXPECTED_CONNECTION_ID,
				TestConstants.EXPECTED_CREDENTIALS_ID, EXPECTED_SERVER_CONFIG, EXPECTED_SERVER_STREAM,
				EXPECTED_SERVER_APPLICATION, EXPECTED_SERVER_LEVEL, EXPECTED_LEVEL_OPTION, EXPECTED_FILTER_TYPE,
				EXPECTED_FOLDER_NAME, EXPECTED_GET_FILES, EXPECTED_GET_FOLDERS);

		assertThat(
				String.format("Expected IspwConfiguration.getConnectionId() to return %s",
						TestConstants.EXPECTED_CONNECTION_ID),
				scm.getConnectionId(), is(equalTo(TestConstants.EXPECTED_CONNECTION_ID)));

		assertThat(
				String.format("Expected IspwConfiguration.getCredentialsId() to return %s",
						TestConstants.EXPECTED_CREDENTIALS_ID),
				scm.getCredentialsId(), is(equalTo(TestConstants.EXPECTED_CREDENTIALS_ID)));

		assertThat(String.format("Expected IspwConfiguration.getServerConfig() to return %s", EXPECTED_SERVER_CONFIG),
				scm.getServerConfig(), is(equalTo(EXPECTED_SERVER_CONFIG)));

		assertThat(String.format("Expected IspwConfiguration.getServerStream() to return %s", EXPECTED_SERVER_STREAM),
				scm.getServerStream(), is(equalTo(EXPECTED_SERVER_STREAM)));

		assertThat(String.format("Expected IspwConfiguration.getServerApplication() to return %s", EXPECTED_SERVER_APPLICATION),
				scm.getServerApplication(), is(equalTo(EXPECTED_SERVER_APPLICATION)));

		assertThat(String.format("Expected IspwConfiguration.getServerLevel() to return %s", EXPECTED_SERVER_LEVEL),
				scm.getServerLevel(), is(equalTo(EXPECTED_SERVER_LEVEL)));

		assertThat(String.format("Expected IspwConfiguration.getLevelOption() to return %s", EXPECTED_LEVEL_OPTION),
				scm.getLevelOption(), is(equalTo(EXPECTED_LEVEL_OPTION)));

		assertThat(String.format("Expected IspwConfiguration.getFilterType() to return %s", EXPECTED_FILTER_TYPE),
				scm.getFilterType(), is(equalTo(EXPECTED_FILTER_TYPE)));

		assertThat(String.format("Expected IspwConfiguration.getFolderName() to return %s", EXPECTED_FOLDER_NAME),
				scm.getFolderName(), is(equalTo(EXPECTED_FOLDER_NAME)));
		
		assertThat(String.format("Expected IspwConfiguration.getGetFiles() to return %s", EXPECTED_GET_FILES + ""),
				scm.getFilterFiles(), is(equalTo(EXPECTED_GET_FILES + "")));
		
		assertThat(String.format("Expected IspwConfiguration.getGetFolders() to return %s", EXPECTED_GET_FOLDERS + ""),
				scm.getFilterFolders(), is(equalTo(EXPECTED_GET_FOLDERS + "")));
	}

	/**
	 * Tests the results of an download execution.
	 * <p>
	 * A project is created, configured and executed where the log is examined to verify parameters being passed to the CLI. The
	 * build is not expected to succeed since no CLI exists
	 */
	@Test
	public void executionTest()
	{
		try
		{
			FreeStyleProject project = m_jenkinsRule.createFreeStyleProject("TestProject");
			project.setScm(new IspwConfiguration(TestConstants.EXPECTED_CONNECTION_ID, TestConstants.EXPECTED_CREDENTIALS_ID,
					EXPECTED_SERVER_CONFIG, EXPECTED_SERVER_STREAM, EXPECTED_SERVER_APPLICATION, EXPECTED_SERVER_LEVEL,
					EXPECTED_LEVEL_OPTION, EXPECTED_FILTER_TYPE, EXPECTED_FOLDER_NAME, EXPECTED_GET_FILES, EXPECTED_GET_FOLDERS));

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

				assertThat(String.format("Expected log to contain server config: \"%s\".", EXPECTED_SERVER_CONFIG),
						logFileOutput, containsString(EXPECTED_SERVER_CONFIG));

				assertThat(String.format("Expected log to contain server stream: \"%s\".", EXPECTED_SERVER_STREAM),
						logFileOutput, containsString(EXPECTED_SERVER_STREAM));

				assertThat(String.format("Expected log to contain server application: \"%s\".", EXPECTED_SERVER_APPLICATION),
						logFileOutput, containsString(EXPECTED_SERVER_APPLICATION));

				assertThat(String.format("Expected log to contain server level: \"%s\".", EXPECTED_SERVER_LEVEL), logFileOutput,
						containsString(EXPECTED_SERVER_LEVEL));

				assertThat(String.format("Expected log to contain level option: \"%s\".", EXPECTED_LEVEL_OPTION), logFileOutput,
						containsString(EXPECTED_LEVEL_OPTION));

				assertThat(String.format("Expected log to contain filter type: \"%s\".", EXPECTED_FILTER_TYPE), logFileOutput,
						containsString(EXPECTED_FILTER_TYPE));

				assertThat(String.format("Expected log to contain folder name: \"%s\".", EXPECTED_FOLDER_NAME), logFileOutput,
						containsString(EXPECTED_FOLDER_NAME));
				
				assertThat(String.format("Expected log to contain get files: \"%s\".", EXPECTED_GET_FILES + ""), logFileOutput,
						containsString(EXPECTED_GET_FILES + ""));
				
				assertThat(String.format("Expected log to contain get folders: \"%s\".", EXPECTED_GET_FOLDERS + ""), logFileOutput,
						containsString(EXPECTED_GET_FOLDERS + ""));
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
	 * Perform a round trip test on the configuration.
	 * <p>
	 * A project is created, configured, submitted / saved, and reloaded where the original configuration is compared against
	 * the reloaded configuration for equality.
	 */
	@Test
	public void roundTripTest()
	{
		try
		{
			IspwConfiguration scmConfig = new IspwConfiguration(TestConstants.EXPECTED_CONNECTION_ID,
					TestConstants.EXPECTED_CREDENTIALS_ID, EXPECTED_SERVER_CONFIG, EXPECTED_SERVER_STREAM,
					EXPECTED_SERVER_APPLICATION, EXPECTED_SERVER_LEVEL, EXPECTED_LEVEL_OPTION, EXPECTED_FILTER_TYPE,
					EXPECTED_FOLDER_NAME, EXPECTED_GET_FILES, EXPECTED_GET_FOLDERS);
			ScmTestUtils.roundTripTest(m_jenkinsRule, scmConfig,
					"connectionId,credentialsId,serverConfig,serverStream,serverApplication,serverLevel,levelOption,filterType,folderName,getFiles,getFolders");
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