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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.joda.time.Minutes;
import org.joda.time.Seconds;

import org.rhq.server.metrics.MetricsConfiguration;

/**
 * @author John Sanda
 */
public class SimulationPlanner {

    public SimulationPlan create(File jsonFile) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(jsonFile);
        SimulationPlan simulation = new SimulationPlan();

        simulation.setCollectionInterval(getLong(root.get("collectionInterval"), 1250L));
        simulation.setAggregationInterval(getLong(root.get("aggregationInterval"), 150000L));  // 2.5 minutes
        simulation.setNumMeasurementCollectors(getInt(root.get("numMeasurementCollectors"), 5));
        simulation.setSimulationTime(getInt(root.get("simulationTime"), 10));
        simulation.setThreadPoolSize(getInt(root.get("threadPoolSize"), simulation.getNumMeasurementCollectors() + 2));
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

        MetricsConfiguration serverConfiguration = createDefaultMetricsConfiguration();
        simulation.setMetricsServerConfiguration(serverConfiguration);

        return simulation;
    }

    private MetricsConfiguration createDefaultMetricsConfiguration() {

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

}
