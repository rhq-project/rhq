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

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.domain.alert.AlertDefinition;
import org.rhq.core.domain.event.Event;
import org.rhq.core.domain.event.EventSource;
import org.rhq.core.domain.measurement.Availability;
import org.rhq.core.domain.measurement.MeasurementData;
import org.rhq.core.domain.measurement.composite.MeasurementBaselineComposite;
import org.rhq.core.domain.operation.OperationHistory;
import org.rhq.core.domain.resource.Resource;

/**
 * @author Joseph Marques
 */

@Stateless
@TransactionAttribute(TransactionAttributeType.SUPPORTS)
public class AlertConditionCacheManagerBean implements AlertConditionCacheManagerLocal {
    @SuppressWarnings("unused")
    private static final Log log = LogFactory.getLog(AlertConditionCacheManagerBean.class);

    public AlertConditionCacheStats checkConditions(MeasurementData... measurementData) {
        AlertConditionCacheStats stats;
        stats = AlertConditionCache.getInstance().checkConditions(measurementData);
        return stats;
    }

    public AlertConditionCacheStats checkConditions(OperationHistory operationHistory) {
        AlertConditionCacheStats stats;
        stats = AlertConditionCache.getInstance().checkConditions(operationHistory);
        return stats;
    }

    public AlertConditionCacheStats checkConditions(Availability... availabilities) {
        AlertConditionCacheStats stats;
        stats = AlertConditionCache.getInstance().checkConditions(availabilities);
        return stats;
    }

    public AlertConditionCacheStats checkConditions(EventSource source, Event... events) {
        AlertConditionCacheStats stats;
        stats = AlertConditionCache.getInstance().checkConditions(source, events);
        return stats;
    }

    public AlertConditionCacheStats updateConditions(Resource deletedResource) {
        AlertConditionCacheStats stats;
        stats = AlertConditionCache.getInstance().updateConditions(deletedResource);
        return stats;
    }

    // this could potentially take really long, but we don't need to be in a transactional scope anyway
    public AlertConditionCacheStats updateConditions(List<MeasurementBaselineComposite> measurementBaselines) {
        AlertConditionCacheStats stats;
        stats = AlertConditionCache.getInstance().updateConditions(measurementBaselines);
        return stats;
    }

    public AlertConditionCacheStats updateConditions(AlertDefinition alertDefinition,
        AlertDefinitionEvent alertDefinitionEvent) {
        AlertConditionCacheStats stats;
        stats = AlertConditionCache.getInstance().updateConditions(alertDefinition, alertDefinitionEvent);
        return stats;
    }

    public boolean isCacheValid() {
        boolean valid;
        valid = AlertConditionCache.getInstance().isCacheValid();
        return valid;
    }

    public String[] getCacheNames() {
        String[] names;
        names = AlertConditionCache.getInstance().getCacheNames();
        return names;
    }

    public void printCache(String cacheName) {
        AlertConditionCache.getInstance().printCache(cacheName);
    }

    public void printAllCaches() {
        AlertConditionCache.getInstance().printAllCaches();
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void reloadCachesForAgent(int agentId) {
        AlertConditionCache.getInstance().reloadCachesForAgent(agentId);
    }
}