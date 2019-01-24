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
package com.compuware.jenkins.scm;

import java.io.File;
import java.io.IOException;
import org.apache.commons.lang.StringUtils;
import com.compuware.jenkins.common.utils.CommonConstants;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.Run;
import hudson.model.TaskListener;

/**
 * Abstract source downloader.
 */
public abstract class AbstractDownloader
{
	/**
	 * Converts the given filter pattern from a multi-line String to a comma-delimited string.
	 * 
	 * @param filterPattern
	 *            the <code>String</code> dataset filter
	 * 
	 * @return comma-delimited <code>String</code> of dataset filters
	 */
	public String convertFilterPattern(String filterPattern)
	{
		String cdDatasets = null;

		if (filterPattern != null)
		{
			cdDatasets = StringUtils.normalizeSpace(filterPattern);
			cdDatasets = StringUtils.replace(cdDatasets, CommonConstants.SPACE, CommonConstants.COMMA);
		}

		return cdDatasets;
	}

	/**
	 * Download the mainframe sources specified in the Jenkins configuration.
	 * 
	 * @param build
	 *            the current running Jenkins build
	 * @param launcher
	 *            the machine that the files will be checked out.
	 * @param workspaceFilePath
	 *            a directory to check out the source code.
	 * @param listener
	 *            build listener
	 * @param changelogFile
	 *            upon a successful return, this file should capture the changelog. When there's no change, this file should
	 *            contain an empty entry
	 * 
	 * @return <code>boolean</code> if the build was successful
	 * 
	 * @throws InterruptedException
	 *             if an error in checking CLI compatibility, retrieving the environment or launching the CLI command
	 * @throws IOException
	 *             if an error in checking CLI compatibility, retrieving the environment or launching the CLI command
	 */
	public abstract boolean getSource(Run<?, ?> build, Launcher launcher, FilePath workspaceFilePath, TaskListener listener,
			File changelogFile) throws InterruptedException, IOException;
}