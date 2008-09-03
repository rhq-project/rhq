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

import javax.ejb.ActivationConfigProperty;
import javax.ejb.EJB;
import javax.ejb.MessageDriven;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.ObjectMessage;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.domain.alert.AlertCondition;
import org.rhq.core.domain.alert.AlertDefinition;
import org.rhq.enterprise.server.alert.AlertConditionManagerLocal;
import org.rhq.enterprise.server.alert.CachedConditionManagerLocal;
import org.rhq.enterprise.server.alert.engine.jms.model.AbstractAlertConditionMessage;
import org.rhq.enterprise.server.util.concurrent.AlertSerializer;

/**
 * Use the default message provider
 * 
 * @author Joseph Marques
 */
@MessageDriven(activationConfig = {
    @ActivationConfigProperty(propertyName = "destinationType", propertyValue = "javax.jms.Queue"),
    @ActivationConfigProperty(propertyName = "destination", propertyValue = "queue/AlertConditionQueue"),
    @ActivationConfigProperty(propertyName = "acknowledgeMode", propertyValue = "Auto-acknowledge"),
    @ActivationConfigProperty(propertyName = "subscriptionDurability", propertyValue = "NonDurable") })
public final class AlertConditionConsumerBean implements MessageListener {

    private final Log log = LogFactory.getLog(AlertConditionConsumerBean.class);

    @EJB
    private AlertConditionManagerLocal alertConditionManager;

    @EJB
    private CachedConditionManagerLocal cachedConditionManager;

    public void onMessage(Message message) {
        ObjectMessage objectMessage = (ObjectMessage) message;
        AbstractAlertConditionMessage conditionMessage = null;

        try {
            conditionMessage = (AbstractAlertConditionMessage) objectMessage.getObject();
        } catch (JMSException je) {
            log.error("Error getting content of jms message", je);
        }

        AlertDefinition definition = null;
        try {
            log.debug("Received message: " + conditionMessage);

            int alertConditionId = conditionMessage.getAlertConditionId();
            AlertCondition condition = alertConditionManager.getAlertConditionById(alertConditionId);
            if (condition == null) {
                log.info("AlertCondition[id=" + alertConditionId
                    + "] has been removed after it was triggered; this message will be discarded");
                return;
            }
            definition = condition.getAlertDefinition();

            AlertSerializer.getSingleton().lock(definition.getId());

            /* 
             * must be executed in a new, nested transaction so that by it
             * completes and unlocks, the next thread will see all of its results
             */
            cachedConditionManager.processCachedConditionMessage(conditionMessage, definition);
            //processCachedConditionMessage(conditionMessage, definition);
        } catch (Exception e) {
            log.error("Error handling " + conditionMessage + " - " + e.toString());
        } finally {
            if (definition != null) {
                AlertSerializer.getSingleton().unlock(definition.getId());
            }
        }
    }

    //    private void processCachedConditionMessage(AbstractAlertConditionMessage conditionMessage,
    //        AlertDefinition definition) {
    //        /*
    //         * note that ctime is the time when the condition was known to be true, not the time we're persisting the
    //         * condition log message
    //         */
    //        if (conditionMessage instanceof ActiveAlertConditionMessage) {
    //            ActiveAlertConditionMessage activeConditionMessage = (ActiveAlertConditionMessage) conditionMessage;
    //
    //            alertConditionLogManager.updateUnmatchedLogByAlertConditionId(activeConditionMessage.getAlertConditionId(),
    //                activeConditionMessage.getTimestamp(), activeConditionMessage.getValue());
    //
    //            alertConditionLogManager.checkForCompletedAlertConditionSet(activeConditionMessage.getAlertConditionId());
    //        } else if (conditionMessage instanceof InactiveAlertConditionMessage) {
    //
    //            AlertDampeningEvent event = new AlertDampeningEvent(definition, AlertDampeningEvent.Type.NEGATIVE);
    //            entityManager.persist(event);
    //
    //            alertDampeningManager.processEventType(definition.getId(), AlertDampeningEvent.Type.NEGATIVE);
    //        } else {
    //            log.error("Unsupported message type sent to consumer for processing: "
    //                + conditionMessage.getClass().getSimpleName());
    //        }
    //    }
}