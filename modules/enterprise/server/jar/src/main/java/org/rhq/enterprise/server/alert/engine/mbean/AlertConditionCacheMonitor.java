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
package org.rhq.enterprise.server.alert.engine.mbean;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import javax.management.MBeanRegistration;
import javax.management.MBeanServer;
import javax.management.MBeanServerInvocationHandler;
import javax.management.ObjectName;

import org.rhq.enterprise.server.alert.engine.AlertConditionCache;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * An MBean that exposes various structures contained with the AlertConditionCache
 * 
 * @author Joseph Marques
 */
public class AlertConditionCacheMonitor implements AlertConditionCacheMonitorMBean, MBeanRegistration {

    public AtomicInteger availabilityCacheElementMatches = new AtomicInteger();
    public AtomicInteger eventCacheElementMatches = new AtomicInteger();
    public AtomicInteger measurementCacheElementMatches = new AtomicInteger();
    public AtomicInteger resourceConfigurationCacheElementMatches = new AtomicInteger();
    public AtomicInteger operationCacheElementMatches = new AtomicInteger();
    public AtomicInteger totalCacheElementMatches = new AtomicInteger();

    public AtomicLong availabilityProcessingTime = new AtomicLong();
    public AtomicLong eventProcessingTime = new AtomicLong();
    public AtomicLong measurementProcessingTime = new AtomicLong();
    public AtomicLong resourceConfigurationProcessingTime = new AtomicLong();
    public AtomicLong operationProcessingTime = new AtomicLong();
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
        return AlertConditionCache.getInstance().getCacheSize(AlertConditionCache.CacheName.AvailabilityCache);
    }

    public int getEventCacheElementCount() {
        return AlertConditionCache.getInstance().getCacheSize(AlertConditionCache.CacheName.EventsCache);
    }

    public int getMeasurementCacheElementCount() {
        return AlertConditionCache.getInstance().getCacheSize(AlertConditionCache.CacheName.MeasurementDataCache)
            + AlertConditionCache.getInstance().getCacheSize(AlertConditionCache.CacheName.MeasurementTraitCache);
    }

    public int getResourceConfigurationCacheElementCount() {
        return AlertConditionCache.getInstance().getCacheSize(AlertConditionCache.CacheName.ResourceConfigurationCache);
    }

    public int getOperationCacheElementCount() {
        return AlertConditionCache.getInstance().getCacheSize(AlertConditionCache.CacheName.ResourceOperationCache);
    }

    public int getAvailabilityCacheElementMatches() {
        return availabilityCacheElementMatches.get();
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

    public int getTotalCacheElementMatches() {
        return totalCacheElementMatches.get();
    }

    public void incrementAvailabilityCacheElementMatches(int matches) {
        availabilityCacheElementMatches.addAndGet(matches);
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

    public long getAvailabilityProcessingTime() {
        return availabilityProcessingTime.get();
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

    public long getTotalProcessingTime() {
        return totalProcessingTime.get();
    }

    public void incrementAvailabilityProcessingTime(long moreMillis) {
        availabilityProcessingTime.addAndGet(moreMillis);
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

    public String[] getCacheNames() {
        return LookupUtil.getAlertConditionCacheManager().getCacheNames();
    }

    public void printCache(String cacheName) {
        LookupUtil.getAlertConditionCacheManager().printCache(cacheName);
    }

    public void printAllCaches() {
        LookupUtil.getAlertConditionCacheManager().printAllCaches();
    }

    public ObjectName preRegister(MBeanServer server, ObjectName name) throws Exception {
        objectName = name;
        mbeanServer = server;
        return name;
    }

    public void postRegister(Boolean registrationDone) {
    }

    public void preDeregister() throws Exception {
    }

    public void postDeregister() {
        mbeanServer = null;
    }

}