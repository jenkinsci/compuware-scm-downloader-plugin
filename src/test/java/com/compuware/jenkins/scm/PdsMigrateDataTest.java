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
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.recipes.LocalData;
import com.compuware.jenkins.scm.util.CpwrScmConfigTestUtils;

/**
 * Pds data migration unit tests.
 */
public class PdsMigrateDataTest
{
	// Member Variables
	@Rule
	public JenkinsRule m_jenkinsRule = new JenkinsRule();

	/**
	 * Perform a round trip migration test on the configuration.
	 * <p>
	 * An existing project is loaded, migrated, saved, and reloaded where the original configuration is compared against
	 * the reloaded configuration. The test project is loaded from a .zip file that mimics a Jenkins project's
	 * layout within.
	 * 
	 * See test resource for the migration test: src/test/resources/com.compuware.jenkins.scm/<test>/<test method>.zip
	 */
	@Test
	@LocalData
	public void migrateDataTest()
	{
		try
		{
			CpwrScmConfigTestUtils.migrateDataTest(m_jenkinsRule);
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