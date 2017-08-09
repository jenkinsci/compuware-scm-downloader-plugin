/**
 * These materials contain confidential information and trade secrets of Compuware Corporation. You shall maintain the materials
 * as confidential and shall not disclose its contents to any third party except as may be required by law or regulation. Use,
 * disclosure, or reproduction is prohibited without the prior express written permission of Compuware Corporation.
 * 
 * All Compuware products listed within the materials are trademarks of Compuware Corporation. All other company or product
 * names are trademarks of their respective owners.
 * 
 * Copyright (c) 2017 Compuware Corporation. All rights reserved.
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

	public static final String HOST_PORT_OPEN_TAG = "<m__hostPort>";
	public static final String HOST_PORT_CLOSE_TAG = "</m__hostPort>";
	public static final String CODE_PAGE_OPEN_TAG = "<m__codePage>";
	public static final String CODE_PAGE_CLOSE_TAG = "</m__codePage>";
}
