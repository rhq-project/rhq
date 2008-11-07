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

/**
 * An MBean that exposes various structures contained with the AlertConditionCache
 * 
 * @author Joseph Marques
 */
public interface AlertConditionCacheMonitorMBean {

    /*
     * for travel time
     */
    public long getTotalProcessingTime();

    public long getAvailabilityProcessingTime();

    public long getMeasurementProcessingTime();

    public long getEventProcessingTime();

    public long getOperationProcessingTime();

    public void incrementAvailabilityProcessingTime(long moreMillis);

    public void incrementMeasurementProcessingTime(long moreMillis);

    public void incrementEventProcessingTime(long moreMillis);

    public void incrementResourceConfigurationProcessingTime(long moreMillis);

    public void incrementOperationProcessingTime(long moreMillis);

    /*
     * for current congestion
     */
    public int getTotalCacheElementCount();

    public int getAvailabilityCacheElementCount();

    public int getMeasurementCacheElementCount();

    public int getEventCacheElementCount();

    public int getResourceConfigurationCacheElementCount();

    public int getOperationCacheElementCount();

    public void resetAvailabilityCacheElementCounts();

    public void resetEventCacheElementCounts();

    public void resetMeasurementCacheElementCounts();

    public void resetResourceConfigurationCacheElementCounts();

    public void resetOperationCacheElementCounts();

    public void resetAllCacheElementCounts();

    public void incrementAvailabilityCacheElementCount(int count);

    public void incrementMeasurementCacheElementCount(int count);

    public void incrementEventCacheElementCount(int count);

    public void incrementResourceConfigurationCacheElementCount(int count);

    public void incrementOperationCacheElementCount(int count);

    /*
     * for out-bound traffic
     */
    public int getTotalCacheElementMatches();

    public int getAvailabilityCacheElementMatches();

    public int getMeasurementCacheElementMatches();

    public int getEventCacheElementMatches();

    public int getResourceConfigurationCacheElementMatches();

    public int getOperationCacheElementMatches();

    public void incrementAvailabilityCacheElementMatches(int matches);

    public void incrementMeasurementCacheElementMatches(int matches);

    public void incrementEventCacheElementMatches(int matches);

    public void incrementResourceConfigurationCacheElementMatches(int matches);

    public void incrementOperationCacheElementMatches(int matches);

    /*
     * cache contents
     */
    public String[] getCacheNames();

    public void printCache(String cacheName);

    public void printAllCaches();
}