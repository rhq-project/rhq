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
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotSame;

import javax.jms.Queue;
import javax.jms.QueueConnection;
import javax.jms.QueueConnectionFactory;
import javax.jms.QueueSender;
import javax.jms.QueueSession;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.naming.InitialContext;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertyList;
import org.rhq.core.domain.configuration.PropertyMap;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.pluginapi.operation.OperationResult;
import org.rhq.plugins.jbossas5.test.util.AppServerUtils;
import org.testng.annotations.BeforeGroups;
import org.testng.annotations.Test;

/**
 * 
 * @author Lukas Krejci
 */
@Test(groups = {"as5-plugin", "as5-plugin-ejb3", "as5-plugin-ejb3-mdb"})
public class Ejb3MDBResourceTest extends AbstractEjb3ResourceTest {

    private static final int MESSAGES_SENT = 10;
    private static final String QUEUE_NAME = "queue/tutorial/example";
    private static final String MDB_NAME = "ExampleMDB";
    
    @BeforeGroups(groups = "as5-plugin-ejb3", dependsOnMethods="deployTestJars")
    public void setup() {
        try {
            System.out.println("Sending " + MESSAGES_SENT + " messages to " + QUEUE_NAME);
            
            InitialContext ctx = AppServerUtils.getAppServerInitialContext();

            QueueConnectionFactory factory = (QueueConnectionFactory) ctx.lookup("ConnectionFactory");
            Queue queue = (Queue) ctx.lookup(QUEUE_NAME);

            QueueConnection connection = factory.createQueueConnection();
            connection.start();

            QueueSession session = connection.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);

            QueueSender sender = session.createSender(queue);
            
            TextMessage message = session.createTextMessage();

            for (int i = 0; i < MESSAGES_SENT; ++i) {
                message.setText("Message no. " + i);
                sender.send(message);
            }
            
            sender.close();
            session.close();
            connection.close();

            System.out.println("Giving some time for the messages to arrive...");
            Thread.sleep(2000);
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

    @Override
    protected void validateOperationResult(String name, OperationResult result, Resource resource) {
        if ("viewInvocationStats".equals(name) && resource.getResourceKey().equals(MDB_NAME)) {
            //the method was invoked ten times in the setup. we should see it in the results here...
            Configuration resultValueConfig = result.getComplexResults();

            assertNotNull(resultValueConfig, "viewInvocationStats results shouldn't be null.");

            PropertyList propertyList = resultValueConfig.getList("methods");

            assertNotNull(propertyList, "viewInvocationStats must include a \"methods\" property list.");
            assertNotSame(propertyList.getList().size(), 0,
                "the viewInvocationStats should contain at least one method statistics");

            PropertyMap methodStat = (PropertyMap) propertyList.getList().iterator().next();

            assertEquals(methodStat.getSimpleValue("methodName", null), "onMessage",
                "Couldn't find method stats for the tested method.");

            PropertySimple count = methodStat.getSimple("count");
            assertNotNull(count, "Could get to the 'count' method stat property. This should not happen.");

            assertEquals(count.getIntegerValue(), Integer.valueOf(MESSAGES_SENT),
                "The tested method call count doesn't match.");

            assertNotNull(methodStat.getSimple("totalTime"),
                "Couldn't find 'totalTime' method stat. This should not happen.");
            assertNotNull(methodStat.getSimple("minTime"),
                "Couldn't find 'minTime' method stat. This should not happen.");
            assertNotNull(methodStat.getSimple("maxTime"),
                "Couldn't find 'maxTime' method stat. This should not happen.");
        } else {
            super.validateOperationResult(name, result, resource);
        }
    }

    
}
