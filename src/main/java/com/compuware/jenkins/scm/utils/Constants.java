/**
 * The MIT License (MIT)
 * 
 * Copyright (c) 2016, 2017 Compuware Corporation
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
package com.compuware.jenkins.scm.utils;

/**
 * Constants used by the Source Control Management plugin.
 */
@SuppressWarnings("nls")
public class Constants
{
	/**
	 * Private constructor.
	 * <p>
	 * All constants should be accessed statically.
	 */
	private Constants()
	{
	}

	// Constants
	public static final String FILE_SEPARATOR = "file.separator";
	
	public static final String CODE_PAGE_PARM = "-code";
	public static final String DATA_PARM = "-data";
	public static final String FILE_EXT_PARM = "-ext";
	public static final String FILTER_PARM = "-filter";
	public static final String HOST_PARM = "-host";
	public static final String PW_PARM = "-pass";
	public static final String PORT_PARM = "-port";
	public static final String SCM_TYPE_PARM = "-scm";
	public static final String USERID_PARM = "-id";
	public static final String TARGET_FOLDER_PARM = "-targetFolder";
	public static final String TIMEOUT_PARM = "-timeout";

	public static final String ISPW = "ispw";
	public static final String ISPW_SERVER_CONFIG_PARAM = "-ispwServerConfig";
	public static final String ISPW_SERVER_STREAM_PARAM = "-ispwServerStream";
	public static final String ISPW_SERVER_APP_PARAM= "-ispwServerApp";
	public static final String ISPW_SERVER_LEVEL_PARAM = "-ispwServerLevel";
	public static final String ISPW_LEVEL_OPTION_PARAM = "-ispwLevelOption";
	public static final String ISPW_FILTER_NAME_PARAM = "-ispwFilterName";
	public static final String ISPW_FILTER_TYPE_PARAM = "-ispwFilterType";

	public static final String COLON = ":";
	public static final String COMMA = ",";
	public static final String LINE_RETURN = "\n";
	public static final String SPACE = " ";

	public static final String DOUBLE_QUOTE = "\"";
	public static final String DOUBLE_QUOTE_ESCAPED = "\"\"";

	public static final String ENDEVOR = "endevor";
	public static final String PDS = "pds";

	public static final String SCM_DOWNLOADER_CLI_BAT = "SCMDownloaderCLI.bat";
	public static final String SCM_DOWNLOADER_CLI_SH = "SCMDownloaderCLI.sh";
	public static final String TOPAZ_CLI_WORKSPACE = "TopazCliWkspc";
}