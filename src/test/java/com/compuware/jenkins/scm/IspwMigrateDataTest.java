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
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.recipes.LocalData;
import com.compuware.jenkins.common.configuration.CpwrGlobalConfiguration;
import com.compuware.jenkins.common.configuration.HostConnection;
import com.compuware.jenkins.scm.util.TestConstants;
import hudson.model.FreeStyleProject;
import hudson.model.TopLevelItem;

/**
 * ISPW data migration unit tests.
 */
@SuppressWarnings("nls")
public class IspwMigrateDataTest
{
	// Member Variables
	@Rule
	public JenkinsRule m_jenkinsRule = new JenkinsRule();

	/**
	 * Perform a round trip test on the configuration.
	 * <p>
	 * A project is created, configured, submitted / saved, and reloaded where the original configuration is compared against
	 * the reloaded configuration for equality.
	 */
	@Test
	@LocalData
	public void migrateDataTest()
	{
		try
		{
			TopLevelItem item = m_jenkinsRule.jenkins.getItem("TestProject");
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
		IspwConfiguration config = (IspwConfiguration) project.getScm();

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