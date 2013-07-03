/*
 * RHQ Management Platform
 * Copyright (C) 2005-2013 Red Hat, Inc.
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
 * along with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA
 */

package org.rhq.enterprise.server.alert.engine.mbean;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ejb.ConcurrencyManagement;
import javax.ejb.ConcurrencyManagementType;
import javax.ejb.LocalBean;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.management.MBeanServer;
import javax.management.MBeanServerInvocationHandler;
import javax.management.ObjectName;

import org.rhq.core.util.ObjectNameFactory;
import org.rhq.enterprise.server.alert.engine.internal.AlertConditionCacheCoordinator;
import org.rhq.enterprise.server.util.JMXUtil;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * An MBean that exposes various structures contained with the AlertConditionCache
 * 
 * @author Joseph Marques
 */
@Singleton
@Startup
@LocalBean
@ConcurrencyManagement(ConcurrencyManagementType.BEAN)
@TransactionAttribute(TransactionAttributeType.SUPPORTS)
public class AlertConditionCacheMonitor implements AlertConditionCacheMonitorMBean {
    private static final ObjectName OBJECT_NAME = ObjectNameFactory.create("rhq:service=AlertConditionCacheMonitor");

    public AtomicInteger availabilityCacheElementMatches = new AtomicInteger();
    public AtomicInteger availabilityDurationCacheElementMatches = new AtomicInteger();
    public AtomicInteger eventCacheElementMatches = new AtomicInteger();
    public AtomicInteger measurementCacheElementMatches = new AtomicInteger();
    public AtomicInteger resourceConfigurationCacheElementMatches = new AtomicInteger();
    public AtomicInteger operationCacheElementMatches = new AtomicInteger();
    public AtomicInteger calltimeCacheElementMatches = new AtomicInteger();
    public AtomicInteger driftCacheElementMatches = new AtomicInteger();
    public AtomicInteger totalCacheElementMatches = new AtomicInteger();

    public AtomicLong availabilityProcessingTime = new AtomicLong();
    public AtomicLong availabilityDurationProcessingTime = new AtomicLong();
    public AtomicLong eventProcessingTime = new AtomicLong();
    public AtomicLong measurementProcessingTime = new AtomicLong();
    public AtomicLong resourceConfigurationProcessingTime = new AtomicLong();
    public AtomicLong operationProcessingTime = new AtomicLong();
    public AtomicLong calltimeProcessingTime = new AtomicLong();
    public AtomicLong driftProcessingTime = new AtomicLong();
    public AtomicLong totalProcessingTime = new AtomicLong();

    private static MBeanServer mbeanServer;
    private static ObjectName objectName;

    private static AlertConditionCacheMonitorMBean proxy;

    public static AlertConditionCacheMonitorMBean getMBean() {
        if (proxy == null) {
            if (objectName != null) {
                proxy = (AlertConditionCacheMonitorMBean) MBeanServerInvocationHandler.newProxyInstance(mbeanServer,
                    objectName, AlertConditionCacheMonitorMBean.class, false);
            } else {
                // create a local object
                proxy = new AlertConditionCacheMonitor();
            }
        }

        return proxy;
    }

    public int getAvailabilityCacheElementCount() {
        return AlertConditionCacheCoordinator.getInstance().getCacheSize(
            AlertConditionCacheCoordinator.Cache.AvailabilityCache);
    }

    public int getAvailabilityDurationCacheElementCount() {
        return AlertConditionCacheCoordinator.getInstance().getCacheSize(
            AlertConditionCacheCoordinator.Cache.AvailabilityDurationCache);
    }

    public int getEventCacheElementCount() {
        return AlertConditionCacheCoordinator.getInstance().getCacheSize(
            AlertConditionCacheCoordinator.Cache.EventsCache);
    }

    public int getMeasurementCacheElementCount() {
        return AlertConditionCacheCoordinator.getInstance().getCacheSize(
            AlertConditionCacheCoordinator.Cache.MeasurementDataCache)
            + AlertConditionCacheCoordinator.getInstance().getCacheSize(
                AlertConditionCacheCoordinator.Cache.MeasurementTraitCache);
    }

    public int getResourceConfigurationCacheElementCount() {
        return AlertConditionCacheCoordinator.getInstance().getCacheSize(
            AlertConditionCacheCoordinator.Cache.ResourceConfigurationCache);
    }

    public int getOperationCacheElementCount() {
        return AlertConditionCacheCoordinator.getInstance().getCacheSize(
            AlertConditionCacheCoordinator.Cache.ResourceOperationCache);
    }

    public int getCallTimeCacheElementCount() {
        return AlertConditionCacheCoordinator.getInstance().getCacheSize(
            AlertConditionCacheCoordinator.Cache.CallTimeDataCache);
    }

    public int getDriftCacheElementCount() {
        return AlertConditionCacheCoordinator.getInstance().getCacheSize(
            AlertConditionCacheCoordinator.Cache.DriftCache);
    }

    /**
     * Takes all the counts from {@link #getCacheCounts()} and returns the sum.
     */
    public int getTotalCacheElementCount() {
        Map<String, Integer> map = getCacheCounts();
        int total = 0;
        for (Integer count : map.values()) {
            total += count;
        }
        return total;
    }

    public Map<String, Integer> getCacheCounts() {
        return AlertConditionCacheCoordinator.getInstance().getCacheCounts();
    }

    public int getAvailabilityCacheElementMatches() {
        return availabilityCacheElementMatches.get();
    }

    public int getAvailabilityDurationCacheElementMatches() {
        return availabilityDurationCacheElementMatches.get();
    }

    public int getEventCacheElementMatches() {
        return eventCacheElementMatches.get();
    }

    public int getMeasurementCacheElementMatches() {
        return measurementCacheElementMatches.get();
    }

    public int getResourceConfigurationCacheElementMatches() {
        return resourceConfigurationCacheElementMatches.get();
    }

    public int getOperationCacheElementMatches() {
        return operationCacheElementMatches.get();
    }

    public int getCallTimeCacheElementMatches() {
        return calltimeCacheElementMatches.get();
    }

    public int getDriftCacheElementMatches() {
        return driftCacheElementMatches.get();
    }

    public int getTotalCacheElementMatches() {
        return totalCacheElementMatches.get();
    }

    public void incrementAvailabilityCacheElementMatches(int matches) {
        availabilityCacheElementMatches.addAndGet(matches);
        totalCacheElementMatches.addAndGet(matches);
    }

    public void incrementAvailabilityDurationCacheElementMatches(int matches) {
        availabilityDurationCacheElementMatches.addAndGet(matches);
        totalCacheElementMatches.addAndGet(matches);
    }

    public void incrementEventCacheElementMatches(int matches) {
        eventCacheElementMatches.addAndGet(matches);
        totalCacheElementMatches.addAndGet(matches);
    }

    public void incrementResourceConfigurationCacheElementMatches(int matches) {
        resourceConfigurationCacheElementMatches.addAndGet(matches);
        totalCacheElementMatches.addAndGet(matches);
    }

    public void incrementMeasurementCacheElementMatches(int matches) {
        measurementCacheElementMatches.addAndGet(matches);
        totalCacheElementMatches.addAndGet(matches);
    }

    public void incrementOperationCacheElementMatches(int matches) {
        operationCacheElementMatches.addAndGet(matches);
        totalCacheElementMatches.addAndGet(matches);
    }

    public void incrementCallTimeCacheElementMatches(int matches) {
        calltimeCacheElementMatches.addAndGet(matches);
        totalCacheElementMatches.addAndGet(matches);
    }

    public void incrementDriftCacheElementMatches(int matches) {
        driftCacheElementMatches.addAndGet(matches);
        totalCacheElementMatches.addAndGet(matches);
    }

    public long getAvailabilityProcessingTime() {
        return availabilityProcessingTime.get();
    }

    public long getAvailabilityDurationProcessingTime() {
        return availabilityDurationProcessingTime.get();
    }

    public long getEventProcessingTime() {
        return eventProcessingTime.get();
    }

    public long getMeasurementProcessingTime() {
        return measurementProcessingTime.get();
    }

    public long getOperationProcessingTime() {
        return operationProcessingTime.get();
    }

    public long getCallTimeProcessingTime() {
        return calltimeProcessingTime.get();
    }

    public long getDriftProcessingTime() {
        return driftProcessingTime.get();
    }

    public long getTotalProcessingTime() {
        return totalProcessingTime.get();
    }

    public void incrementAvailabilityProcessingTime(long moreMillis) {
        availabilityProcessingTime.addAndGet(moreMillis);
        totalProcessingTime.addAndGet(moreMillis);
    }

    public void incrementAvailabilityDurationProcessingTime(long moreMillis) {
        availabilityDurationProcessingTime.addAndGet(moreMillis);
        totalProcessingTime.addAndGet(moreMillis);
    }

    public void incrementEventProcessingTime(long moreMillis) {
        eventProcessingTime.addAndGet(moreMillis);
        totalProcessingTime.addAndGet(moreMillis);
    }

    public void incrementResourceConfigurationProcessingTime(long moreMillis) {
        resourceConfigurationProcessingTime.addAndGet(moreMillis);
        totalProcessingTime.addAndGet(moreMillis);
    }

    public void incrementMeasurementProcessingTime(long moreMillis) {
        measurementProcessingTime.addAndGet(moreMillis);
        totalProcessingTime.addAndGet(moreMillis);
    }

    public void incrementOperationProcessingTime(long moreMillis) {
        operationProcessingTime.addAndGet(moreMillis);
        totalProcessingTime.addAndGet(moreMillis);
    }

    public void incrementCallTimeProcessingTime(long moreMillis) {
        calltimeProcessingTime.addAndGet(moreMillis);
        totalProcessingTime.addAndGet(moreMillis);
    }

    public void incrementDriftProcessingTime(long moreMillis) {
        driftProcessingTime.addAndGet(moreMillis);
        totalProcessingTime.addAndGet(moreMillis);
    }

    public void reloadCaches() {
        LookupUtil.getAlertConditionCacheManager().reloadAllCaches();
    }

    @PostConstruct
    private void init() {
        JMXUtil.registerMBean(this, OBJECT_NAME);
        mbeanServer = JMXUtil.getPlatformMBeanServer();
        objectName = OBJECT_NAME;
    }

    @PreDestroy
    private void destroy() {
        mbeanServer = null;
        objectName = null;
        JMXUtil.unregisterMBeanQuietly(OBJECT_NAME);
    }

}
