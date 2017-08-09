/**
 * The MIT License (MIT)
 * 
 * Copyright (c) 2016, 2017 Compuware Corporation
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
package com.compuware.jenkins.scm.utils;

/**
 * Constants used by the Source Control Management plugin.
 */
@SuppressWarnings("nls")
public class ScmConstants
{
	/**
	 * Private constructor.
	 * <p>
	 * All constants should be accessed statically.
	 */
	private ScmConstants()
	{
	}

	// Constants
	public static final String FILE_EXT_PARM = "-ext";
	public static final String FILTER_PARM = "-filter";
	public static final String SCM_TYPE_PARM = "-scm";

	public static final String ENDEVOR = "endevor";
	public static final String ISPW = "ispw";
	public static final String PDS = "pds";

	public static final String ISPW_SERVER_CONFIG_PARAM = "-ispwServerConfig";
	public static final String ISPW_SERVER_STREAM_PARAM = "-ispwServerStream";
	public static final String ISPW_SERVER_APP_PARAM = "-ispwServerApp";
	public static final String ISPW_SERVER_LEVEL_PARAM = "-ispwServerLevel";
	public static final String ISPW_LEVEL_OPTION_PARAM = "-ispwLevelOption";
	public static final String ISPW_FILTER_NAME_PARAM = "-ispwFilterName";
	public static final String ISPW_FILTER_TYPE_PARAM = "-ispwFilterType";

	public static final String SCM_DOWNLOADER_CLI_BAT = "SCMDownloaderCLI.bat";
	public static final String SCM_DOWNLOADER_CLI_SH = "SCMDownloaderCLI.sh";
}