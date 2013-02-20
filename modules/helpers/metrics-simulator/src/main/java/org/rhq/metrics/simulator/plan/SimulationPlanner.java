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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.joda.time.Days;
import org.joda.time.Duration;
import org.joda.time.Hours;
import org.joda.time.Minutes;
import org.joda.time.Seconds;

import org.rhq.server.metrics.MetricsConfiguration;
import org.rhq.server.metrics.domain.MetricsTable;

/**
 * @author John Sanda
 */
public class SimulationPlanner {

    public SimulationPlan create(File jsonFile) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(jsonFile);
        SimulationPlan simulation = new SimulationPlan();

        simulation.setCollectionInterval(root.get("collectionInterval").asLong());
        simulation.setAggregationInterval(root.get("aggregationInterval").asLong());
        simulation.setThreadPoolSize(root.get("threadPoolSize").asInt());
        simulation.setNumMeasurementCollectors(root.get("numMeasurementCollectors").asInt());
        simulation.setSimulationTime(root.get("simulationTime").asInt());

        for (JsonNode node : root.get("schedules")) {
            simulation.addScheduleSet(new ScheduleGroup(node.get("count").asInt(), node.get("interval").asLong()));
        }

        MetricsConfiguration serverConfiguration = new MetricsConfiguration();
        simulation.setMetricsServerConfiguration(serverConfiguration);

        for (JsonNode node : root.get("ttl")) {
            MetricsTable table = getTable(node.get("table").asText());
            int ttl = node.get("value").asInt();
            setTTLAndRetention(table, ttl, serverConfiguration);
        }

        JsonNode timeSliceNode = root.get("timeSliceDuration");
        String units = timeSliceNode.get("units").asText();
        for (JsonNode node : timeSliceNode.get("values")) {
            Duration duration = getDuration(units, node.get("value").asInt());
            MetricsTable table = getTable(node.get("table").asText());
            setTimeSliceDuration(table, duration, serverConfiguration);
        }

        ClusterConfig clusterConfig = new ClusterConfig();
        JsonNode clusterConfigNode = root.get("cluster");
        clusterConfig.setEmbedded(clusterConfigNode.get("embedded").asBoolean());
        clusterConfig.setClusterDir(clusterConfigNode.get("clusterDir").asText());
        clusterConfig.setHeapSize(clusterConfigNode.get("heapSize").asText());
        clusterConfig.setHeapNewSize(clusterConfigNode.get("heapNewSize").asText());
        clusterConfig.setNumNodes(clusterConfigNode.get("numNodes").asInt());
        simulation.setClusterConfig(clusterConfig);

        return simulation;
    }

    private MetricsTable getTable(String name) {
        if (name.equals(MetricsTable.RAW.getTableName())) {
            return MetricsTable.RAW;
        } else if (name.equals(MetricsTable.ONE_HOUR.getTableName())) {
            return MetricsTable.ONE_HOUR;
        } else if (name.equals(MetricsTable.SIX_HOUR.getTableName())) {
            return MetricsTable.SIX_HOUR;
        } else if (name.equals(MetricsTable.TWENTY_FOUR_HOUR.getTableName())) {
            return MetricsTable.TWENTY_FOUR_HOUR;
        } else {
            throw new IllegalArgumentException(name + " is not a valid metrics table name");
        }
    }

    private void setTTLAndRetention(MetricsTable table, int ttl, MetricsConfiguration configuration) {
        switch (table) {
            case RAW:
                configuration.setRawTTL(ttl);
                configuration.setRawRetention(Seconds.seconds(ttl).toStandardDuration());
                break;
            case ONE_HOUR:
                configuration.setOneHourTTL(ttl);
                configuration.setOneHourRetention(Seconds.seconds(ttl).toStandardDuration());
                break;
            case SIX_HOUR:
                configuration.setSixHourTTL(ttl);
                configuration.setSixHourRetention(Seconds.seconds(ttl).toStandardDuration());
                break;
            default:
                configuration.setTwentyFourHourTTL(ttl);
                configuration.setTwentyFourHourRetention(Seconds.seconds(ttl).toStandardDuration());
                break;
        }
    }

    private Duration getDuration(String units, int value) {
        if (units.equals("seconds")) {
            return Seconds.seconds(value).toStandardDuration();
        } else if (units.equals("minutes")) {
            return Minutes.minutes(value).toStandardDuration();
        } else if (units.equals("hours")) {
            return Hours.hours(value).toStandardDuration();

        } else if (units.equals("days")) {
            return Days.days(value).toStandardDuration();
        } else {
            throw new IllegalArgumentException(units + " is not a valid value for the units property.");
        }
    }

    private void setTimeSliceDuration(MetricsTable table, Duration duration, MetricsConfiguration configuration) {
        switch (table) {
            case RAW:
                configuration.setRawTimeSliceDuration(duration);
                break;
            case ONE_HOUR:
                configuration.setOneHourTimeSliceDuration(duration);
                break;
            case SIX_HOUR:
                configuration.setSixHourTimeSliceDuration(duration);
                break;
            default:
                // do nothing
        }
    }

}
