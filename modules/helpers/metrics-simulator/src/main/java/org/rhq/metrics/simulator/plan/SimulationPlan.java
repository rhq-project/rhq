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

import org.rhq.server.metrics.DateTimeService;
import org.rhq.server.metrics.MetricsConfiguration;

/**
 * @author John Sanda
 */
public class SimulationPlan {

    public static enum IntervalType {
        SECONDS("seconds"), MINUTES("minutes"), HOURS("hours");

        private final String text;

        IntervalType(String text) {
            this.text = text;
        }

        public static IntervalType fromText(String text) {
            if (text.equals("seconds")) {
                return SECONDS;
            }
            if (text.equals("minutes")) {
                return MINUTES;
            }
            if (text.equals("hours")) {
                return HOURS;
            }
            throw new IllegalArgumentException(text + " is not a valid interval type");
        }
    }

    public static enum AggregationType {
        SYNC("sync"), ASYNC("async");

        private final String text;

        AggregationType(String text) {
            this.text = text;
        }

        public static AggregationType fromText(String text) {
            if (text.equals("sync")) {
                return SYNC;
            }
            if (text.equals("async")) {
                return ASYNC;
            }
            throw new IllegalArgumentException(text + " is not a valid aggregation type");
        }
    }

    public static enum SimulationType {
        THREADED("threaded"), SEQUENTIAL("sequential");

        private final String text;

        SimulationType(String text) {
            this.text = text;
        }

        public static SimulationType fromText(String text) {
            if (text.equals("threaded") || text.equals("thread")) {
                return THREADED;
            }
            if (text.equals("sequential") || text.equals("seq")) {
                return SEQUENTIAL;
            }
            throw new IllegalArgumentException(text + " is not a valid simulation type");
        }
    }

    private long collectionInterval;

    private long aggregationInterval;

    private MetricsConfiguration metricsServerConfiguration;

    private int numMeasurementCollectors;

    private int simulationTime;

    private String[] nodes;

    private int cqlPort;

    private int batchSize;

    private int metricsReportInterval;

    private IntervalType intervalType;

    private DateTimeService dateTimeService;

    private int numReaders;

    private long readInterval;

    private long simulationRate;

    private int aggregationBatchSize;

    private boolean aggregationEnabled = true;

    private AggregationType aggregationType;

    private SimulationType simulationType;

    private int readerThreadPoolSize = 1;

    public int getReaderThreadPoolSize() {
        return readerThreadPoolSize;
    }

    public void setReaderThreadPoolSize(int readerThreadPoolSize) {
        this.readerThreadPoolSize = readerThreadPoolSize;
    }

    public DateTimeService getDateTimeService() {
        return dateTimeService;
    }

    public void setDateTimeService(DateTimeService dateTimeService) {
        this.dateTimeService = dateTimeService;
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

    public int getNumMeasurementCollectors() {
        return numMeasurementCollectors;
    }

    public void setNumMeasurementCollectors(int numMeasurementCollectors) {
        this.numMeasurementCollectors = numMeasurementCollectors;
    }

    public int getSimulationTime() {
        return simulationTime;
    }

    public void setSimulationTime(int simulationTime) {
        this.simulationTime = simulationTime;
    }

    public String[] getNodes() {
        return nodes;
    }

    public void setNodes(String[] nodes) {
        this.nodes = nodes;
    }

    public int getCqlPort() {
        return cqlPort;
    }

    public void setCqlPort(int cqlPort) {
        this.cqlPort = cqlPort;
    }

    public int getBatchSize() {
        return batchSize;
    }

    public void setBatchSize(int batchSize) {
        this.batchSize = batchSize;
    }

    public int getMetricsReportInterval() {
        return metricsReportInterval;
    }

    public void setMetricsReportInterval(int metricsReportInterval) {
        this.metricsReportInterval = metricsReportInterval;
    }

    public IntervalType getIntervalType() {
        return intervalType;
    }

    public void setIntervalType(IntervalType intervalType) {
        this.intervalType = intervalType;
    }

    public int getNumReaders() {
        return numReaders;
    }

    public void setNumReaders(int numReaders) {
        this.numReaders = numReaders;
    }

    public long getReadInterval() {
        return readInterval;
    }

    public void setReadInterval(long readInterval) {
        this.readInterval = readInterval;
    }

    public long getSimulationRate() {
        return simulationRate;
    }

    public void setSimulationRate(long simulationRate) {
        this.simulationRate = simulationRate;
    }

    public int getAggregationBatchSize() {
        return aggregationBatchSize;
    }

    public void setAggregationBatchSize(int aggregationBatchSize) {
        this.aggregationBatchSize = aggregationBatchSize;
    }

    public AggregationType getAggregationType() {
        return aggregationType;
    }

    public void setAggregationType(AggregationType aggregationType) {
        this.aggregationType = aggregationType;
    }

    public SimulationType getSimulationType() {
        return simulationType;
    }

    public void setSimulationType(SimulationType simulationType) {
        this.simulationType = simulationType;
    }

    public boolean isAggregationEnabled() {
        return aggregationEnabled;
    }

    public void setAggregationEnabled(boolean aggregationEnabled) {
        this.aggregationEnabled = aggregationEnabled;
    }
}
