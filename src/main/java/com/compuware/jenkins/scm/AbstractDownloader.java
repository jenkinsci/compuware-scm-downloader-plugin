/**
 * The MIT License (MIT)
 * 
 * Copyright (c) 2016 Compuware Corporation
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software 
 * and associated documentation files (the "Software"), to deal in the Software without restriction, 
 * including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, 
 * and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, 
 * subject to the following conditions: The above copyright notice and this permission notice shall be 
 * included in all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT 
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. 
 * IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, 
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE 
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
*/
package com.compuware.jenkins.scm;

import hudson.FilePath;
import hudson.Launcher;
import hudson.model.BuildListener;
import hudson.model.AbstractBuild;
import java.io.File;
import java.io.IOException;
import com.compuware.jenkins.scm.utils.Constants;

/**
 * Abstract source downloader
 */
public abstract class AbstractDownloader
{
	/**
	 * Convert the filter pattern from a multi-line String to a comma delimited string.
	 * 
	 * @param filter
	 *            the dataset filter
	 * @return comma delimited list of dataset filters
	 */
	protected String convertFilterPattern(String filter)
	{
		String convertedFilter = filter;

		if (convertedFilter != null)
		{
			convertedFilter = convertedFilter.replace(Constants.LINE_RETURN, Constants.COMMA);
			char lastChar = convertedFilter.charAt(convertedFilter.length() - 1);
			if (lastChar == ',')
			{
				convertedFilter = convertedFilter.substring(0, (convertedFilter.length() - 1));
			}
		}

		return convertedFilter;
	}

	/**
	 * Download the mainframe sources specified in the Jenkins configuration.
	 * 
	 * @param launcher
	 *            The machine that the files will be checked out.
	 * @param workspaceFilePath
	 *            a directory to check out the source code.
	 * @param listener
	 *            Build listener
	 * @param changelogFile
	 *            Upon a successful return, this file should capture the changelog. When there's no change, this file should
	 *            contain an empty entry
	 * @param filterPattern
	 *            Source filter pattern
	 * @return <code>boolean</code> if the build was successful
	 * @throws IOException
	 * @throws InterruptedException
	 */
	public abstract boolean getSource(AbstractBuild<?, ?> build, Launcher launcher, FilePath workspaceFilePath, BuildListener listener,
			File changelogFile, String filterPattern) throws InterruptedException, IOException;

}
