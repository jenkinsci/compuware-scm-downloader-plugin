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
package com.compuware.jenkins.scm.util;

/**
 * Constants used for tests.
 */
@SuppressWarnings("nls")
public class TestConstants
{
	public static final String COLON = ":";

	public static final String DESCRIPTION = "description";
	public static final String HOST_PORT = "hostPort";
	public static final String CODE_PAGE = "codePage";
	public static final String TIMEOUT = "timeout";
	public static final String CONNECTION_ID = "connectionId";
	public static final String TOPAZ_CLI_LOCATION_LINUX = "topazCLILocationLinux";
	public static final String TOPAZ_CLI_LOCATION_WINDOWS = "topazCLILocationWindows";

	public static final String EXPECTED_CONNECTION_ID = "12345";
	public static final String EXPECTED_CREDENTIALS_ID = "67890";
	public static final String EXPECTED_FILTER_PATTERN = "XDEVREG.XPED.COBOL";
	public static final String EXPECTED_FILE_EXTENSION = "cbl";

	public static final String EXPECTED_HOST = "cw01";
	public static final String EXPECTED_PORT = "30947";
	public static final String EXPECTED_CODE_PAGE = "1047";
	public static final String EXPECTED_TIMEOUT = "123";
	public static final String EXPECTED_USER_ID = "xdevreg";
	public static final String EXPECTED_PASSWORD = "********";
	public static final String EXPECTED_TARGET_FOLDER = "../sources";

	public static final String HOST_PORT_OPEN_TAG = "<m__hostPort>";
	public static final String HOST_PORT_CLOSE_TAG = "</m__hostPort>";
	public static final String CODE_PAGE_OPEN_TAG = "<m__codePage>";
	public static final String CODE_PAGE_CLOSE_TAG = "</m__codePage>";
}
