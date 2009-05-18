/*
 * Jopr Management Platform
 * Copyright (C) 2005-2009 Red Hat, Inc.
 * All rights reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2, as
 * published by the Free Software Foundation, and/or the GNU Lesser
 * General Public License, version 2.1, also as published by the Free
 * Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License and the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License
 * and the GNU Lesser General Public License along with this program;
 * if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */
package org.rhq.plugins.jbossas5.test;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.rhq.plugins.jbossas5.connection.ProfileServiceConnection;

/**
 * Runs unit tests that need to run within a JBossAS 5.x instance to communicate with the profile service in-process.
 *
 * @author Ian Springer
 */
public class UnitTestRunner {
    private final Log log = LogFactory.getLog(this.getClass());

    public void runUnitTests(ProfileServiceConnection connection) {
        log.info(">>>>>>>>>>>>>>>>>>>>>>>>>>>> Running unit tests...");
        TxConnectionFactoryTest txConnectionFactoryTest = new TxConnectionFactoryTest(connection);
        try {
            txConnectionFactoryTest.testCreateMandatoryPropertiesNull();
        } catch (Throwable t) {
            log.error("Test failed.", t);
        }
        try {
            txConnectionFactoryTest.testCreateMandatoryPropertyInnerValuesNull();
        } catch (Throwable t) {
            log.error("Test failed.", t);
        }
        try {
            txConnectionFactoryTest.testCreateMandatoryPropertyValuesNull();
        } catch (Throwable t) {
            log.error("Test failed.", t);
        }
        try {
            txConnectionFactoryTest.testCreateNonMandatoryPropertiesNull();
        } catch (Throwable t) {
            log.error("Test failed.", t);
        }
        try {
            txConnectionFactoryTest.testCreateNonMandatoryPropertyInnerValuesNull();
        } catch (Throwable t) {
            log.error("Test failed.", t);
        }
        try {
            txConnectionFactoryTest.testCreateNonMandatoryPropertyValuesNull();
        } catch (Throwable t) {
            log.error("Test failed.", t);
        }
        try {
            txConnectionFactoryTest.testCreateValid();
        } catch (Throwable t) {
            log.error("Test failed.", t);
        }
    }
}
