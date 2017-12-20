/**
 * The MIT License (MIT)
 * 
 * Copyright (c) 2015 - 2018 Compuware Corporation
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

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import org.apache.commons.lang.StringUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;
import com.compuware.jenkins.scm.util.ScmTestUtils;
import com.compuware.jenkins.scm.util.TestConstants;
import hudson.model.FreeStyleProject;

/**
 * CpwrScmConfiguration unit test
 */
@SuppressWarnings("nls")
public class CpwrScmConfigurationTest
{
	private static char ILLEGAL_FILE_CHAR_WIN = '*';

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
	 * Test retrieval of a project's login information.
	 */
	@Test
	public void testGetLoginInformation()
	{
		try
		{
			FreeStyleProject project = m_jenkinsRule.createFreeStyleProject("TestProject");
			PdsConfiguration scmConfig = new PdsConfiguration(TestConstants.EXPECTED_CONNECTION_ID,
					TestConstants.EXPECTED_FILTER_PATTERN, TestConstants.EXPECTED_FILE_EXTENSION,
					TestConstants.EXPECTED_CREDENTIALS_ID, TestConstants.EXPECTED_TARGET_FOLDER);
			project.setScm(scmConfig);

			// Test passing a null project and still find credentials
			StandardUsernamePasswordCredentials credential = scmConfig.getLoginInformation(null);

			assertNotNull(credential);
			assertThat(String.format("Expected getId() to return %s", TestConstants.EXPECTED_CREDENTIALS_ID),
					credential.getId(), is(equalTo(TestConstants.EXPECTED_CREDENTIALS_ID)));

			// Test pass the project and find credentials
			credential = scmConfig.getLoginInformation(project);

			assertNotNull(credential);
			assertThat(String.format("Expected getId() to return %s", TestConstants.EXPECTED_CREDENTIALS_ID),
					credential.getId(), is(equalTo(TestConstants.EXPECTED_CREDENTIALS_ID)));

			// Test unable to find credentials
			PdsConfiguration scmConfig2 = new PdsConfiguration(TestConstants.EXPECTED_CONNECTION_ID,
					TestConstants.EXPECTED_FILTER_PATTERN, TestConstants.EXPECTED_FILE_EXTENSION, "blah", TestConstants.EXPECTED_TARGET_FOLDER);
			project.setScm(scmConfig2);

			credential = scmConfig2.getLoginInformation(project);

			assertNull(credential);
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
	 * Test the validation of the target folder.
	 */
	@Test
	public void validateTargetFolderTest()
	{
		// valid folder
		try
		{
			PdsConfiguration scmConfig = new PdsConfiguration(TestConstants.EXPECTED_CONNECTION_ID,
					TestConstants.EXPECTED_FILTER_PATTERN, TestConstants.EXPECTED_FILE_EXTENSION,
					TestConstants.EXPECTED_CREDENTIALS_ID, TestConstants.EXPECTED_TARGET_FOLDER);
			scmConfig.validateTargetFolder(System.out);
		}
		catch (Exception e)
		{
			e.printStackTrace();
			fail(e.getMessage());
		}

		// invalid folder - NOTE: this test will fail on Linux because Linux accepts any character for a folder name
		try
		{
			String invalidFolderName = "sou" + ILLEGAL_FILE_CHAR_WIN + "rce";
			PdsConfiguration scmConfig = new PdsConfiguration(TestConstants.EXPECTED_CONNECTION_ID,
					TestConstants.EXPECTED_FILTER_PATTERN, TestConstants.EXPECTED_FILE_EXTENSION,
					TestConstants.EXPECTED_CREDENTIALS_ID, invalidFolderName);
			scmConfig.validateTargetFolder(System.out);
			fail("Expected an IllegalArgumentException for a source directory with an invalid folder name.");
		}
		catch (Exception e)
		{
			// expected exception
		}

		// empty
		try
		{
			PdsConfiguration scmConfig = new PdsConfiguration(TestConstants.EXPECTED_CONNECTION_ID,
					TestConstants.EXPECTED_FILTER_PATTERN, TestConstants.EXPECTED_FILE_EXTENSION,
					TestConstants.EXPECTED_CREDENTIALS_ID, StringUtils.EMPTY);
			scmConfig.validateTargetFolder(System.out);
		}
		catch (Exception e)
		{
			e.printStackTrace();
			fail(e.getMessage());
		}

		// multiple subfolders
		try
		{
			PdsConfiguration scmConfig = new PdsConfiguration(TestConstants.EXPECTED_CONNECTION_ID,
					TestConstants.EXPECTED_FILTER_PATTERN, TestConstants.EXPECTED_FILE_EXTENSION,
					TestConstants.EXPECTED_CREDENTIALS_ID, "source/a/b");
			scmConfig.validateTargetFolder(System.out);
		}
		catch (Exception e)
		{
			e.printStackTrace();
			fail(e.getMessage());
		}

	}
}