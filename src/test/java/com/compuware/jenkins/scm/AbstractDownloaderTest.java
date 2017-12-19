/**
 * These materials contain confidential information and trade secrets of Compuware Corporation. You shall maintain the materials
 * as confidential and shall not disclose its contents to any third party except as may be required by law or regulation. Use,
 * disclosure, or reproduction is prohibited without the prior express written permission of Compuware Corporation.
 * 
 * All Compuware products listed within the materials are trademarks of Compuware Corporation. All other company or product
 * names are trademarks of their respective owners.
 * 
 * Copyright (c) 2015 - 2018 Compuware Corporation. All rights reserved.
 */
package com.compuware.jenkins.scm;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import java.io.File;
import java.io.IOException;
import org.apache.commons.lang.StringUtils;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.Run;
import hudson.model.TaskListener;

/**
 * AbstractDownloader unit tests.
 */
@SuppressWarnings("nls")
public class AbstractDownloaderTest
{
	@Rule
	public JenkinsRule j = new JenkinsRule();

	private TestDownloader m_testDownloader = new TestDownloader();

	@Test
	public void convertFilterPatternNullTest()
	{
		String convertedText = m_testDownloader.convertFilterPattern(null);
		assertNull("Expecting null result for null input", convertedText);
	}

	@Test
	public void convertFilterPatternSpacesTest()
	{
		// Single space
		// Test " "
		// Test "a.b.c d.e.f"
		// Test "a.b.c d.e.f "
		// Test " a.b.c d.e.f "

		// Multiple Spaces
		// Test " "
		// Test "a.b.c d.e.f"
		// Test "a.b.c d.e.f "
		// Test " a.b.c d.e.f "
		assertTrue(true);
	}

	@Test
	public void convertFilterPatternNewlineTest()
	{
		// Test "\n"
		String input = "\n";
		String expectedResults = StringUtils.EMPTY;
		testFilterPattern(input, expectedResults);

		// Test "a.b.c\nd.e.f"
		input = "a.b.c\nd.e.f";
		expectedResults = "a.b.c,d.e.f";
		testFilterPattern(input, expectedResults);

		// Test "a.b.c\nd.e.f\n"
		input = "a.b.c\nd.e.f\n";
		expectedResults = "a.b.c,d.e.f";
		testFilterPattern(input, expectedResults);

		// Test "\na.b.c\nd.e.f\n"
		input = "\na.b.c\nd.e.f\n";
		expectedResults = "a.b.c,d.e.f";
		testFilterPattern(input, expectedResults);

		// Test "\n\n\n"
		input = "\n\n\n";
		expectedResults = StringUtils.EMPTY;
		testFilterPattern(input, expectedResults);

		// Test "a.b.c\n\n\nd.e.f"
		input = "a.b.c\n\n\nd.e.f";
		expectedResults = "a.b.c,d.e.f";
		testFilterPattern(input, expectedResults);

		// Test "a.b.c\n\nd.e.f\n\n"
		input = "a.b.c\n\nd.e.f\n\n";
		expectedResults = "a.b.c,d.e.f";
		testFilterPattern(input, expectedResults);

		// Test "\n\na.b.c\n\n\nd.e.f\n\n\n\n"
		input = "\n\na.b.c\n\n\nd.e.f\n\n\n\n";
		expectedResults = "a.b.c,d.e.f";
		testFilterPattern(input, expectedResults);
	}

	@Test
	public void convertFilterPatternTabsTest()
	{
		// Test "\t"
		String input = "\t";
		String expectedResults = StringUtils.EMPTY;
		testFilterPattern(input, expectedResults);

		// Test "a.b.c\td.e.f"
		input = "a.b.c\td.e.f";
		expectedResults = "a.b.c,d.e.f";
		testFilterPattern(input, expectedResults);

		// Test "a.b.c\td.e.f\t"
		input = "a.b.c\td.e.f\t";
		expectedResults = "a.b.c,d.e.f";
		testFilterPattern(input, expectedResults);

		// Test "\ta.b.c\td.e.f\t"
		input = "\ta.b.c\td.e.f\t";
		expectedResults = "a.b.c,d.e.f";
		testFilterPattern(input, expectedResults);

		// Test "\t\t\t"
		input = "\t\t\t";
		expectedResults = StringUtils.EMPTY;
		testFilterPattern(input, expectedResults);

		// Test "a.b.c\t\t\td.e.f"
		input = "a.b.c\t\t\td.e.f";
		expectedResults = "a.b.c,d.e.f";
		testFilterPattern(input, expectedResults);

		// Test "a.b.c\t\td.e.f\t\t"
		input = "a.b.c\t\td.e.f\t\t";
		expectedResults = "a.b.c,d.e.f";
		testFilterPattern(input, expectedResults);

		// Test "\t\ta.b.c\t\t\td.e.f\t\t\t\t"
		input = "\t\ta.b.c\t\t\td.e.f\t\t\t\t";
		expectedResults = "a.b.c,d.e.f";
		testFilterPattern(input, expectedResults);
	}

	@Test
	public void convertFilterPatternMixedTest()
	{
		// Mixed delimiters
		// Test " \t \n \t\t\n"
		String input = "  \t \n \t\t\n";
		String expectedResults = StringUtils.EMPTY;
		testFilterPattern(input, expectedResults);

		// Test "a.b.c\td.e.f\ng.h.i"
		input = "a.b.c\td.e.f\ng.h.i";
		expectedResults = "a.b.c,d.e.f,g.h.i";
		testFilterPattern(input, expectedResults);

		// Test " \t\na.b.c\n \t d.e.f g.h.i,j.k.l\n\t "
		input = " \t\na.b.c\n \t d.e.f g.h.i,j.k.l\n\t   ";
		expectedResults = "a.b.c,d.e.f,g.h.i,j.k.l";
		testFilterPattern(input, expectedResults);
		assertTrue(true);
	}

	@Test
	public void convertFilterPatternCommaTest()
	{
		String input = "a.b.c,d.e.f";
		String expectedResults = "a.b.c,d.e.f";
		testFilterPattern(input, expectedResults);
	}

	private void testFilterPattern(String input, String expectedResults)
	{
		String msg = String.format("Input: %s, Expected: %s", input, expectedResults);
		String convertedText = m_testDownloader.convertFilterPattern(input);
		assertEquals(msg, expectedResults, convertedText);
	}

	private class TestDownloader extends AbstractDownloader
	{
		/* (non-Javadoc)
		 * @see com.compuware.jenkins.scm.AbstractDownloader#getSource(hudson.model.Run, hudson.Launcher, hudson.FilePath, hudson.model.TaskListener, java.io.File, java.lang.String)
		 */
		@Override
		public boolean getSource(Run<?, ?> build, Launcher launcher, FilePath workspaceFilePath, TaskListener listener,
				File changelogFile) throws InterruptedException, IOException
		{
			return false;
		}
	};
}
