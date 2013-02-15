/*
 *
 *  * RHQ Management Platform
 *  * Copyright (C) 2005-2012 Red Hat, Inc.
 *  * All rights reserved.
 *  *
 *  * This program is free software; you can redistribute it and/or modify
 *  * it under the terms of the GNU General Public License, version 2, as
 *  * published by the Free Software Foundation, and/or the GNU Lesser
 *  * General Public License, version 2.1, also as published by the Free
 *  * Software Foundation.
 *  *
 *  * This program is distributed in the hope that it will be useful,
 *  * but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  * GNU General Public License and the GNU Lesser General Public License
 *  * for more details.
 *  *
 *  * You should have received a copy of the GNU General Public License
 *  * and the GNU Lesser General Public License along with this program;
 *  * if not, write to the Free Software Foundation, Inc.,
 *  * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 *
 */

package org.rhq.metrics.simulator.plan;

import java.util.ArrayList;
import java.util.List;

import org.rhq.server.metrics.MetricsConfiguration;

/**
 * @author John Sanda
 */
public class SimulationPlan {

    private List<ScheduleGroup> scheduleSets = new ArrayList<ScheduleGroup>();

    private int threadPoolSize;

    private long collectionInterval;

    private long aggregationInterval;

    private MetricsConfiguration metricsServerConfiguration;

    public List<ScheduleGroup> getScheduleSets() {
        return scheduleSets;
    }

    public void addScheduleSet(ScheduleGroup scheduleSet) {
        scheduleSets.add(scheduleSet);
    }

    public void setScheduleSets(List<ScheduleGroup> scheduleSets) {
        this.scheduleSets = scheduleSets;
    }

    public int getThreadPoolSize() {
        return threadPoolSize;
    }

    public void setThreadPoolSize(int threadPoolSize) {
        this.threadPoolSize = threadPoolSize;
    }

    public long getCollectionInterval() {
        return collectionInterval;
    }

    public void setCollectionInterval(long collectionInterval) {
        this.collectionInterval = collectionInterval;
    }

    public MetricsConfiguration getMetricsServerConfiguration() {
        return metricsServerConfiguration;
    }

    public void setMetricsServerConfiguration(MetricsConfiguration metricsServerConfiguration) {
        this.metricsServerConfiguration = metricsServerConfiguration;
    }

    public long getAggregationInterval() {
        return aggregationInterval;
    }

    public void setAggregationInterval(long aggregationInterval) {
        this.aggregationInterval = aggregationInterval;
    }
}
