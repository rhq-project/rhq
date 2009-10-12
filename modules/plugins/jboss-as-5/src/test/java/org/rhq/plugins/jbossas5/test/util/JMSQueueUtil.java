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

package org.rhq.plugins.jbossas5.test.util;

import javax.jms.Queue;
import javax.jms.QueueConnection;
import javax.jms.QueueConnectionFactory;
import javax.jms.QueueSender;
import javax.jms.QueueSession;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.naming.InitialContext;

/**
 * 
 * @author Lukas Krejci
 */
public class JMSQueueUtil {

    private JMSQueueUtil() {
        
    }
    
    public static void sendMessages(String queueName, int nofMessages) throws Exception {        
        System.out.println("Sending " + nofMessages + " messages to " + queueName);
        
        InitialContext ctx = AppServerUtils.getAppServerInitialContext();

        QueueConnectionFactory factory = (QueueConnectionFactory) ctx.lookup("ConnectionFactory");
        Queue queue = (Queue) ctx.lookup(queueName);

        QueueConnection connection = factory.createQueueConnection();
        connection.start();

        QueueSession session = connection.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);

        QueueSender sender = session.createSender(queue);
        
        TextMessage message = session.createTextMessage();

        for (int i = 0; i < nofMessages; ++i) {
            message.setText("Message no. " + i);
            sender.send(message);
        }
        
        sender.close();
        session.close();
        connection.close();
        
        System.out.println("Giving some time for the messages to arrive...");
        Thread.sleep(2000);
    }
}
