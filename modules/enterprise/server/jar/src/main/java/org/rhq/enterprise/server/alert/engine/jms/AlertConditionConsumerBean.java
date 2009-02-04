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
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.ObjectMessage;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

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
public class AlertConditionConsumerBean implements MessageListener {

    private final Log log = LogFactory.getLog(AlertConditionConsumerBean.class);

    @EJB
    private AlertConditionManagerLocal alertConditionManager;
    @EJB
    private CachedConditionManagerLocal cachedConditionManager;

    public void onMessage(Message message) {
        AbstractAlertConditionMessage conditionMessage = null;

        try {
            ObjectMessage objectMessage = (ObjectMessage) message;
            conditionMessage = (AbstractAlertConditionMessage) objectMessage.getObject();
        } catch (Throwable t) {
            log.error("Error getting content of jms message", t);
            return;
        }

        AlertDefinition definition = null;
        try {
            if (log.isDebugEnabled())
                log.debug("Received message: " + conditionMessage);

            int alertConditionId = conditionMessage.getAlertConditionId();
            definition = alertConditionManager.getAlertDefinitionByConditionIdInNewTransaction(alertConditionId);
            if (definition == null) {
                log.info("AlertCondition[id=" + alertConditionId
                    + "] has been removed after it was triggered; this message will be discarded");
                return;
            }

            AlertSerializer.getSingleton().lock(definition.getId());

            /*
             * must be executed in a new, nested transaction so that by it
             * completes and unlocks, the next thread will see all of its results
             */
            cachedConditionManager.processCachedConditionMessage(conditionMessage, definition);
        } catch (Throwable t) {
            log.error("Error handling " + conditionMessage + " - " + t.toString());
        } finally {
            try {
                if (definition != null) {
                    AlertSerializer.getSingleton().unlock(definition.getId());
                }
            } catch (Throwable t) {
            }
        }
    }
}