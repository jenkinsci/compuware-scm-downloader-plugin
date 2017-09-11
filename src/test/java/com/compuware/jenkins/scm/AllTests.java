/**
 * These materials contain confidential information and trade secrets of Compuware Corporation. You shall maintain the materials
 * as confidential and shall not disclose its contents to any third party except as may be required by law or regulation. Use,
 * disclosure, or reproduction is prohibited without the prior express written permission of Compuware Corporation.
 * 
 * All Compuware products listed within the materials are trademarks of Compuware Corporation. All other company or product
 * names are trademarks of their respective owners.
 * 
 * Copyright (c) 2010, 2017 Compuware Corporation. All rights reserved.
 */
package com.compuware.jenkins.scm;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

/**
 * Unit test suite for Host Services.
 */
@RunWith(Suite.class)
@Suite.SuiteClasses({
	EndevorMigrateDataTest.class,
	IspwMigrateDataTest.class,
	PdsMigrateDataTest.class,
	AllMigrateDataTest.class
})
public class AllTests
{
}