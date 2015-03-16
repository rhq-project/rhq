/*
 * RHQ Management Platform
 * Copyright (C) 2005-2014 Red Hat, Inc.
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
 * along with this program; if not, write to the Free Software Foundation, Inc,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA
 */

package org.rhq.enterprise.server.storage;

/**
 * @author John Sanda
 */
public interface StorageClientManagerMBean {

    double getRequestLimit();

    double getRequestLimitTopologyDelta();

    void setRequestWarmupPeriod(int requestWarmupPeriod);

    int getRequestWarmupPeriod();

    void setRequestWarmupCounterMaximum(int requestWarmupCounterMaximum);

    int getRequestWarmupCounterMaximum();

    int getCurrentWarmupTime();

    void setRequestLimitTopologyDelta(double delta);

    long getRequestTimeoutDampening();

    void setRequestTimeoutDampening(long requestTimeoutDampening);

    // Cassandra driver's exposed methods
    int getConnectedToHosts();
    int getKnownHosts();
    int getOpenConnections();
    long getReadRequestTimeouts();
    long getWriteRequestTimeouts();
    long getTotalRequests();

    // Metrics.Errors, not exposing RetryPolicy statistics
    long getRetries();
    long getConnectionErrors();

    // Timers
    double getOneMinuteAvgRate();
    double getFiveMinuteAvgRate();
    double getFifteenMinuteAvgRate();
    double getMeanRate();
    double getMeanLatency();

    // Queue

    int getQueueAvailableCapacity();
}
