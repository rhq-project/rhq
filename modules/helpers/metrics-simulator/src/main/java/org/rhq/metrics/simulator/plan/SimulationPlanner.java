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

import java.io.File;
import java.net.InetAddress;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser.Feature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.joda.time.Minutes;
import org.joda.time.Seconds;

import org.rhq.metrics.simulator.MinutesDateTimeService;
import org.rhq.metrics.simulator.SecondsDateTimeService;
import org.rhq.server.metrics.DateTimeService;
import org.rhq.server.metrics.MetricsConfiguration;

/**
 * @author John Sanda
 */
public class SimulationPlanner {

    public SimulationPlan create(File jsonFile) throws Exception {
        JsonFactory jsonFactory = new JsonFactory();
        jsonFactory.enable(Feature.ALLOW_COMMENTS);
        ObjectMapper mapper = new ObjectMapper(jsonFactory);
        JsonNode root = mapper.readTree(jsonFile);
        SimulationPlan simulation = new SimulationPlan();

        simulation.setIntervalType(SimulationPlan.IntervalType.fromText(getString(root.get("intervalType"), "minutes")));
        DateTimeService dateTimeService;

        switch (simulation.getIntervalType()) {
        case SECONDS:
            simulation.setCollectionInterval(getLong(root.get("collectionInterval"), 20L));
            simulation.setAggregationInterval(getLong(root.get("aggregationInterval"), 2500L));
            simulation.setMetricsServerConfiguration(createSecondsConfiguration());
            simulation.setMetricsReportInterval(getInt(root.get("metricsReportInterval"), 30));
            simulation.setSimulationRate(1440);
            dateTimeService = new SecondsDateTimeService();
            break;
        case MINUTES:
            simulation.setCollectionInterval(getLong(root.get("collectionInterval"), 1250L));
            simulation.setAggregationInterval(getLong(root.get("aggregationInterval"), 150000L));
            simulation.setMetricsServerConfiguration(createMinutesConfiguration());
            simulation.setMetricsReportInterval(getInt(root.get("metricsReportInterval"), 180));
            simulation.setDateTimeService(new MinutesDateTimeService());
            simulation.setSimulationRate(2400);
            dateTimeService = new MinutesDateTimeService();
            break;
        default:  // HOURS
            simulation.setCollectionInterval(getLong(root.get("collectionInterval"), 30000L));
            simulation.setAggregationInterval(3600000L);
            simulation.setMetricsReportInterval(getInt(root.get("metricsReportInterval"), 1200));
            simulation.setSimulationRate(1000);
            dateTimeService = new DateTimeService();
        }

        dateTimeService.setConfiguration(simulation.getMetricsServerConfiguration());
        simulation.setDateTimeService(dateTimeService);

        simulation.setNumMeasurementCollectors(getInt(root.get("numMeasurementCollectors"), 5));
        simulation.setNumReaders(getInt(root.get("numReaders"), 1));
        simulation.setReaderThreadPoolSize(getInt(root.get("readerThreadPoolSize"), 1));
        simulation.setSimulationTime(getInt(root.get("simulationTime"), 10));
        simulation.setBatchSize(getInt(root.get("batchSize"), 5000));

        String[] nodes;
        if (root.get("nodes") == null || root.get("nodes").size() == 0) {
            nodes = new String[] {InetAddress.getLocalHost().getHostAddress()};
        } else {
            nodes = new String[root.get("nodes").size()];
            int i = 0;
            for (JsonNode node : root.get("nodes")) {
                nodes[i++] = node.asText();
            }
        }
        simulation.setNodes(nodes);

        simulation.setCqlPort(getInt(root.get("cqlPort"), 9142));
        simulation.setAggregationBatchSize(getInt(root.get("aggregationBatchSize"), 250));
        simulation.setAggregationType(SimulationPlan.AggregationType.fromText(getString(root.get("aggregationType"),
            "sync")));
        simulation.setAggregationEnabled(getBoolean(root.get("aggregationEnabled"), true));

        simulation.setSimulationType(SimulationPlan.SimulationType.fromText(getString(root.get("simulationType"),
            "threaded")));

        return simulation;
    }

    private MetricsConfiguration createMinutesConfiguration() {
        MetricsConfiguration configuration = new MetricsConfiguration();
        configuration.setRawTTL(Minutes.minutes(168).toStandardSeconds().getSeconds());
        configuration.setRawRetention(Minutes.minutes(168).toStandardDuration());
        configuration.setRawTimeSliceDuration(Seconds.seconds(150).toStandardDuration());

        configuration.setOneHourTTL(Minutes.minutes(336).toStandardSeconds().getSeconds());
        configuration.setOneHourRetention(Minutes.minutes(336));
        configuration.setOneHourTimeSliceDuration(Minutes.minutes(15).toStandardDuration());

        configuration.setSixHourTTL(Minutes.minutes(744).toStandardSeconds().getSeconds());
        configuration.setSixHourRetention(Minutes.minutes(744).toStandardSeconds());
        configuration.setSixHourTimeSliceDuration(Minutes.minutes(60).toStandardDuration());

        configuration.setTwentyFourHourTTL(Minutes.minutes(8928).toStandardSeconds().getSeconds());
        configuration.setTwentyFourHourRetention(Minutes.minutes(8928).toStandardSeconds());

        return configuration;
    }

    private MetricsConfiguration createSecondsConfiguration() {
        MetricsConfiguration configuration = new MetricsConfiguration();
        configuration.setRawTTL(420);
        configuration.setRawRetention(Seconds.seconds(420).toStandardDuration());
        configuration.setRawTimeSliceDuration(Seconds.seconds(2).toStandardDuration().plus(500));

        configuration.setOneHourTTL(Seconds.seconds(840).getSeconds());
        configuration.setOneHourRetention(Seconds.seconds(840));
        configuration.setOneHourTimeSliceDuration(Seconds.seconds(15).toStandardDuration());

        configuration.setSixHourTTL(Seconds.seconds(1860).getSeconds());
        configuration.setSixHourRetention(Seconds.seconds(1860));
        configuration.setSixHourTimeSliceDuration(Seconds.seconds(60).toStandardDuration());

        configuration.setTwentyFourHourTTL(Minutes.minutes(365).toStandardSeconds().getSeconds());
        configuration.setTwentyFourHourRetention(Minutes.minutes(365));

        return configuration;
    }

    private String getString(JsonNode node, String defaultValue) {
        if (node == null) {
            return defaultValue;
        }
        return node.asText();
    }

    private long getLong(JsonNode node, long defaultValue) {
        if (node == null) {
            return defaultValue;
        }
        return node.longValue();
    }

    private int getInt(JsonNode node, int defaultValue) {
        if (node == null) {
            return defaultValue;
        }
        return node.intValue();
    }

    private boolean getBoolean(JsonNode node, boolean defaultValue) {
        if (node == null) {
            return defaultValue;
        }
        return node.booleanValue();
    }

}
