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

import static org.junit.Assert.fail;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import com.compuware.jenkins.scm.util.CpwrScmConfigTestUtils;
import com.compuware.jenkins.scm.util.ScmTestUtils;
import com.compuware.jenkins.scm.util.TestConstants;

/**
 * PdsConfiguration unit tests.
 */
@SuppressWarnings("nls")
public class PdsConfigurationTest
{
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
		PdsConfiguration scmConfig = new PdsConfiguration(TestConstants.EXPECTED_CONNECTION_ID,
				TestConstants.EXPECTED_FILTER_PATTERN, TestConstants.EXPECTED_FILE_EXTENSION,
				TestConstants.EXPECTED_CREDENTIALS_ID);
		CpwrScmConfigTestUtils.validateCpwrScmConfigurationConstruction(scmConfig);
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
			PdsConfiguration scmConfig = new PdsConfiguration(TestConstants.EXPECTED_CONNECTION_ID,
					TestConstants.EXPECTED_FILTER_PATTERN, TestConstants.EXPECTED_FILE_EXTENSION,
					TestConstants.EXPECTED_CREDENTIALS_ID);
			CpwrScmConfigTestUtils.executionTest(m_jenkinsRule, scmConfig);
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
			PdsConfiguration scmConfig = new PdsConfiguration(TestConstants.EXPECTED_CONNECTION_ID,
					TestConstants.EXPECTED_FILTER_PATTERN, TestConstants.EXPECTED_FILE_EXTENSION,
					TestConstants.EXPECTED_CREDENTIALS_ID);
			ScmTestUtils.roundTripTest(m_jenkinsRule, scmConfig, "connectionId,filterPattern,fileExtension,credentialsId");
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