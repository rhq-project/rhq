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
package org.rhq.enterprise.server.alert.engine;

import java.util.List;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.domain.configuration.ResourceConfigurationUpdate;
import org.rhq.core.domain.event.Event;
import org.rhq.core.domain.event.EventSource;
import org.rhq.core.domain.measurement.Availability;
import org.rhq.core.domain.measurement.MeasurementData;
import org.rhq.core.domain.operation.OperationHistory;
import org.rhq.core.domain.resource.Agent;
import org.rhq.enterprise.server.alert.engine.internal.AlertConditionCacheCoordinator;
import org.rhq.enterprise.server.cloud.instance.ServerManagerLocal;

/**
 * @author Joseph Marques
 */

@Stateless
@TransactionAttribute(TransactionAttributeType.SUPPORTS)
public class AlertConditionCacheManagerBean implements AlertConditionCacheManagerLocal {
    @SuppressWarnings("unused")
    private static final Log log = LogFactory.getLog(AlertConditionCacheManagerBean.class);

    @EJB
    private ServerManagerLocal serverManager;

    @EJB
    private AlertConditionCacheManagerLocal alertConditionCacheManager;

    public AlertConditionCacheStats checkConditions(MeasurementData... measurementData) {
        AlertConditionCacheStats stats;
        stats = AlertConditionCacheCoordinator.getInstance().checkConditions(measurementData);
        return stats;
    }

    public AlertConditionCacheStats checkConditions(OperationHistory operationHistory) {
        AlertConditionCacheStats stats;
        stats = AlertConditionCacheCoordinator.getInstance().checkConditions(operationHistory);
        return stats;
    }

    public AlertConditionCacheStats checkConditions(Availability... availabilities) {
        AlertConditionCacheStats stats;
        stats = AlertConditionCacheCoordinator.getInstance().checkConditions(availabilities);
        return stats;
    }

    public AlertConditionCacheStats checkConditions(EventSource source, Event... events) {
        AlertConditionCacheStats stats;
        stats = AlertConditionCacheCoordinator.getInstance().checkConditions(source, events);
        return stats;
    }

    public AlertConditionCacheStats checkConditions(ResourceConfigurationUpdate update) {
        AlertConditionCacheStats stats;
        stats = AlertConditionCacheCoordinator.getInstance().checkConditions(update);
        return stats;
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void reloadCachesForAgent(int agentId) {
        AlertConditionCacheCoordinator.getInstance().reloadCachesForAgent(agentId);
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void reloadGlobalCache() {
        AlertConditionCacheCoordinator.getInstance().reloadGlobalCache();
    }

    public void reloadAllCaches() {
        alertConditionCacheManager.reloadGlobalCache();
        List<Agent> agents = serverManager.getAgents();
        for (Agent agent : agents) {
            alertConditionCacheManager.reloadCachesForAgent(agent.getId());
        }
    }

}