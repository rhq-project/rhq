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

import org.rhq.core.domain.alert.Alert;
import org.rhq.core.domain.resource.InventoryStatus;
import org.rhq.enterprise.server.alert.AlertConditionManagerLocal;
import org.rhq.enterprise.server.alert.AlertManagerLocal;
import org.rhq.enterprise.server.alert.CachedConditionManagerLocal;
import org.rhq.enterprise.server.alert.engine.jms.model.AbstractAlertConditionMessage;
import org.rhq.enterprise.server.cloud.instance.CacheConsistencyManagerLocal;
import org.rhq.enterprise.server.util.concurrent.AlertSerializer;

/**
 * Use the default message provider
 *
 * @author Joseph Marques
 */
@MessageDriven(activationConfig = {
    @ActivationConfigProperty(propertyName = "destinationType", propertyValue = "javax.jms.Queue"),
    @ActivationConfigProperty(propertyName = "destination", propertyValue = "java:/queue/AlertConditionQueue"),
    @ActivationConfigProperty(propertyName = "acknowledgeMode", propertyValue = "Auto-acknowledge"),
    @ActivationConfigProperty(propertyName = "subscriptionDurability", propertyValue = "NonDurable") })
public class AlertConditionConsumerBean implements MessageListener {

    private final Log log = LogFactory.getLog(AlertConditionConsumerBean.class);

    @EJB
    private AlertManagerLocal alertManager;
    @EJB
    private AlertConditionManagerLocal alertConditionManager;
    @EJB
    private CachedConditionManagerLocal cachedConditionManager;
    @EJB
    private CacheConsistencyManagerLocal cacheConsistencyManager;

    @Override
    public void onMessage(Message message) {
        AbstractAlertConditionMessage conditionMessage = null;

        try {
            ObjectMessage objectMessage = (ObjectMessage) message;
            conditionMessage = (AbstractAlertConditionMessage) objectMessage.getObject();
        } catch (Throwable t) {
            log.error("Error getting content of jms message", t);
            return;
        }

        Integer definitionId = null;
        try {
            if (log.isDebugEnabled()) {
                log.debug("Received message: " + conditionMessage);
            }

            int alertConditionId = conditionMessage.getAlertConditionId();
            InventoryStatus status = alertConditionManager.getResourceStatusByConditionIdNewTx(alertConditionId);
            if (status != InventoryStatus.COMMITTED) {
                if (log.isDebugEnabled()) {
                    log.debug("Resource for AlertCondition[id=" + alertConditionId
                        + "] is no longer COMMITTED, status was '" + status + "'; this message will be discarded");
                }
                return;
            }

            definitionId = alertConditionManager.getAlertDefinitionByConditionIdNewTx(alertConditionId);
            if (definitionId == null) {
                log.info("AlertCondition[id=" + alertConditionId
                    + "] has been removed after it was triggered; this message will be discarded");
                return;
            }

            AlertSerializer.getSingleton().lock(definitionId);

            /*
             * must be executed in a new, nested transaction so that by it completes and unlocks, the next thread
             * will see all of its results.
             */
            Alert newAlert = cachedConditionManager.processCachedConditionMessageNewTx(conditionMessage, definitionId);

            /*
             * In general it's not required to reload the caches directly. Changes made via the AlertDefinitionManager
             * will update the cache indirectly via the status fields on the server (for the global cache) and
             * owning agent (for the agent cache) and the periodic job that checks it.  But, for recovery alert
             * handling (see BZ 1003132) the delay of up to 30s is unacceptably long and can cause recovery to be
             * missed.  There may be non-recovery issues like this as well. So, when any alert is fired, for the server
             * in question, perform an immediate cache reload check. This ensures the recovery semantics are quickly
             * put in place, minimizing the window of vulnerability.  Note that other HA nodes will be updated via the
             * scheduled check, which should be fine, as that is mainly to handle an agent failover use case.
             *
             * Note that we must do this *after* the alert firing transaction completes.
             *
             * As of 4.10 we've moved the alert notification handling out of the alert firing transaction and
             * after the cache reload.  This ensures that the cache, and in particular, recovery alert defs are
             * updated before executing notifications that could initiate recovery processing (like an automated
             * restart of a down resource).  It also makes the alert firing transaction more lean.
             */
            if (null != newAlert) {
                log.debug("Checking for cache reload due to alert firing");
                cacheConsistencyManager.reloadServerCacheIfNeededNSTx();

                //  the alert is already persisted, now process notifications
                alertManager.sendAlertNotificationsNSTx(newAlert);
            }

        } catch (Throwable t) {
            log.error("Error handling " + conditionMessage + " - " + t.toString());
        } finally {
            try {
                if (definitionId != null) {
                    AlertSerializer.getSingleton().unlock(definitionId);
                }
            } catch (Throwable t) {
            }
        }
    }
}