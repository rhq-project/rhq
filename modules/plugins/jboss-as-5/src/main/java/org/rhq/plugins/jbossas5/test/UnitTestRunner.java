/*
 * RHQ Management Platform
 * Copyright (C) 2005-2008 Red Hat, Inc.
 * All rights reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */
package org.rhq.plugins.jbossas5.test;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.testng.TestListenerAdapter;
import org.testng.TestNG;
import org.testng.reporters.TextReporter;

/**
 * Runs unit tests that need to run within a JBossAS 5.x instance to communicate with the profile service in-process.
 *
 * @author Ian Springer
 */
public class UnitTestRunner
{
    private final Log log = LogFactory.getLog(this.getClass());

    public void runUnitTests()
    {
        log.info(">>>>>>>>>>>>>>>>>>>>>>>>>>>> Running unit tests...");
        TestListenerAdapter reporter = new TextReporter(null, 2);
        TestNG testResults = new TestNG();
        testResults.setTestClasses(new Class[]{TxConnectionFactoryTest.class});
        testResults.addListener(reporter);
        try
        {
            testResults.run();
        }
        catch (Exception e)
        {
            log.fatal("Error while attempting to run unit tests!", e);
        }
        if (testResults.hasFailure())
            log.error("Unit test failures!");
    }
}
