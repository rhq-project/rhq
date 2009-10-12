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

package org.rhq.plugins.jbossas5.test.ejb3;

import static org.testng.Assert.fail;

import org.rhq.plugins.jbossas5.test.util.JMSQueueUtil;
import org.testng.annotations.BeforeGroups;
import org.testng.annotations.Test;

/**
 * 
 * @author Lukas Krejci
 */
@Test(groups = {"as5-plugin", "as5-plugin-ejb3", "as5-plugin-ejb3-mdb"})
public class Ejb3MDBResourceTest extends AbstractEjb3MessageDrivenBeanResourceTest {

    private static final int MESSAGES_SENT = 10;
    private static final String QUEUE_NAME = "queue/tutorial/example";
    private static final String MDB_NAME = "ExampleMDB";
    
    @BeforeGroups(groups = "as5-plugin-ejb3", dependsOnMethods="deployTestJars")
    public void setup() {
        try {
            JMSQueueUtil.sendMessages(QUEUE_NAME, MESSAGES_SENT);
        } catch (Exception e) {
            fail("Failed to setup EJB3 Message Driven Bean test", e);
        }
    }
    
    protected String getResourceTypeName() {
        return "EJB3 Message-Driven Bean";
    }

    @Override
    public void testMetrics() throws Exception {
        super.testMetrics();
    }

    @Override
    public void testOperations() throws Exception {
        super.testOperations();
    }

    protected String getTestedBeanName() {
        return MDB_NAME;
    }    
    
    protected int getMessagesSent() {
        return MESSAGES_SENT;
    }
}
