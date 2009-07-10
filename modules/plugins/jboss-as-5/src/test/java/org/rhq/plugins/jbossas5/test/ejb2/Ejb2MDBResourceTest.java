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

package org.rhq.plugins.jbossas5.test.ejb2;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.fail;

import javax.jms.Queue;
import javax.jms.QueueConnection;
import javax.jms.QueueConnectionFactory;
import javax.jms.QueueSender;
import javax.jms.QueueSession;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.naming.InitialContext;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.plugins.jbossas5.test.util.AppServerUtils;
import org.testng.annotations.BeforeGroups;
import org.testng.annotations.Test;

/**
 * 
 * @author Lukas Krejci
 */
@Test(groups = "as5-plugin")
public class Ejb2MDBResourceTest extends AbstractEjb2ResourceTest {

    private static final int MESSAGES_SENT = 10;

    protected String getResourceTypeName() {
        return "EJB2 Message-Driven Bean";
    }

    @BeforeGroups(groups = "as5-plugin")
    public void setupBean() {
        try {
            InitialContext ctx = AppServerUtils.getAppServerInitialContext();

            QueueConnectionFactory factory = (QueueConnectionFactory) ctx.lookup("ConnectionFactory");
            Queue queue = (Queue) ctx.lookup("queue/A");

            QueueConnection connection = factory.createQueueConnection();
            QueueSession session = connection.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);

            QueueSender sender = session.createSender(queue);

            TextMessage message = session.createTextMessage();

            for (int i = 0; i < MESSAGES_SENT; ++i) {
                message.setText("Message no. " + i);
                sender.send(message);
            }
        } catch (Exception e) {
            fail("Failed to setup Message Driven Bean test", e);
        }
    }

    protected void validateNumericMetricValue(String metricName, Double value) {
        if ("MessageCount".equals(metricName)) {
            assertEquals(value, Double.valueOf(MESSAGES_SENT), "Unexpected message count.");
        } else {
            super.validateNumericMetricValue(metricName, value);
        }
    }

    protected void validateTraitMetricValue(String metricName, String value) {
        super.validateTraitMetricValue(metricName, value);
    }

    protected Configuration getTestResourceConfiguration() {
        //nothing configurable for an MDB
        return new Configuration();
    }

}
