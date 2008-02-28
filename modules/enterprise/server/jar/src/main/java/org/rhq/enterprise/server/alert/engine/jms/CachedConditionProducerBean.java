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
package org.rhq.enterprise.server.alert.engine.jms;

import javax.annotation.Resource;
import javax.ejb.Stateless;
import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.JMSException;
import javax.jms.MessageProducer;
import javax.jms.ObjectMessage;
import javax.jms.Queue;
import javax.jms.Session;

import org.rhq.enterprise.server.alert.engine.jms.model.ActiveAlertConditionMessage;
import org.rhq.enterprise.server.alert.engine.jms.model.InactiveAlertConditionMessage;
import org.rhq.enterprise.server.alert.engine.jms.model.OutOfBoundsConditionMessage;
import org.rhq.enterprise.server.alert.engine.model.AbstractCacheElement;

/**
 * A convenience class that will be used by the AlertConditionCacheManager to send messages to a JMS queue for
 * processing. These messages reference alert conditions that have triggered (become true). The contents will be picked
 * at on the other end of the non-durable queue by the consumer, which will then create and persist the appropriate log
 * message.
 */
@Stateless
public class CachedConditionProducerBean implements CachedConditionProducerLocal {
    /*
     * Get the transactional connection factory
     */
    @Resource(mappedName = "java:/JmsXA")
    private ConnectionFactory factory;

    @Resource(mappedName = "queue/AlertConditionQueue")
    private Queue alertConditionQueue;

    @Resource(mappedName = "queue/OutOfBoundsConditionQueue")
    private Queue outOfBoundsConditionQueue;

    public <T extends AbstractCacheElement<S>, S> void sendActivateAlertConditionMessage(int alertConditionId, S value,
        long timestamp) throws JMSException {
        Connection connection = factory.createConnection();
        Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
        MessageProducer sender = session.createProducer(alertConditionQueue);

        /*
         * The triggered alert condition gets stored as a string anyway, so until this is made more flexible we'll just
         * send the string representation of the value for the AbstractCacheElement in the JMS message
         */
        ActiveAlertConditionMessage conditionMessage = new ActiveAlertConditionMessage(alertConditionId, value
            .toString(), timestamp);

        ObjectMessage message = session.createObjectMessage(conditionMessage);

        sender.send(message);

        connection.close();
    }

    public void sendDeactivateAlertConditionMessage(int alertConditionId, long timestamp) throws JMSException {
        Connection connection = factory.createConnection();
        Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
        MessageProducer sender = session.createProducer(alertConditionQueue);

        // missing "value" element in the message is a negative event
        InactiveAlertConditionMessage conditionMessage = new InactiveAlertConditionMessage(alertConditionId, timestamp);

        ObjectMessage message = session.createObjectMessage(conditionMessage);

        sender.send(message);

        connection.close();
    }

    public void sendOutOfBoundsConditionMessage(int scheduleId, Double value, long timestamp) throws JMSException {
        Connection connection = factory.createConnection();
        Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
        MessageProducer sender = session.createProducer(outOfBoundsConditionQueue);

        /*
         * The triggered alert condition gets stored as a string anyway, so until this is made more flexible we'll just
         * send the string representation of the value for the AbstractCacheElement in the JMS message
         */
        OutOfBoundsConditionMessage conditionMessage = new OutOfBoundsConditionMessage(scheduleId, value, timestamp);

        ObjectMessage message = session.createObjectMessage(conditionMessage);

        sender.send(message);

        connection.close();
    }
}