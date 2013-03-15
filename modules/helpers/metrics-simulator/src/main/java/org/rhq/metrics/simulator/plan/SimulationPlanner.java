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

        simulation.setCollectionInterval(getLong(root.get("collectionInterval"), 500L));
        simulation.setAggregationInterval(getLong(root.get("aggregationInterval"), 1000L));
        simulation.setThreadPoolSize(getInt(root.get("threadPoolSize"), 7));
        simulation.setNumMeasurementCollectors(getInt(root.get("numMeasurementCollectors"), 5));
        simulation.setSimulationTime(getInt(root.get("simulationTime"), 10));

        JsonNode schedules = root.get("schedules");
        if (schedules.isArray()) {
            for (JsonNode node : schedules) {
                simulation.addScheduleSet(new ScheduleGroup(getInt(node.get("count"), 2500),
                    getLong(node.get("interval"), 500L)));
            }
        } else {
            simulation.addScheduleSet(new ScheduleGroup(getInt(schedules.get("count"), 2500),
                getLong(schedules.get("interval"), 500L)));
        }

        MetricsConfiguration serverConfiguration = createDefaultMetricsConfiguration();
        simulation.setMetricsServerConfiguration(serverConfiguration);

        JsonNode ttlNodes = root.get("ttl");
        if (ttlNodes != null) {
            for (JsonNode node : ttlNodes) {
                String tableName = node.get("table").asText();
                if (!tableName.isEmpty()) {
                    MetricsTable table = getTable(tableName);
                    JsonNode ttlNode = node.get("value");
                    if (ttlNode != null) {
                        setTTLAndRetention(table, ttlNode.asInt(), serverConfiguration);
                    }
                }
            }
        }

        JsonNode timeSliceNode = root.get("timeSliceDuration");
        if (timeSliceNode != null) {
            String units = timeSliceNode.get("units").asText();
            if (units.isEmpty()) {
                units = "minutes";
            }
            for (JsonNode node : timeSliceNode.get("values")) {
                JsonNode valueNode = node.get("value");
                JsonNode tableNode = node.get("table");
                if (!(tableNode == null || valueNode == null)) {
                    Duration duration = getDuration(units, valueNode.asInt());
                    MetricsTable table = getTable(tableNode.asText());
                    setTimeSliceDuration(table, duration, serverConfiguration);
                }
            }
        }

        ClusterConfig clusterConfig = new ClusterConfig();
        JsonNode clusterConfigNode = root.get("cluster");
        if (clusterConfigNode != null) {
            clusterConfig.setEmbedded(clusterConfigNode.get("embedded").asBoolean(true));

            JsonNode clusterDirNode = clusterConfigNode.get("clusterDir");
            if (clusterDirNode != null) {
                clusterConfig.setClusterDir(clusterDirNode.asText());
            }

            JsonNode heapSizeNode = clusterConfigNode.get("heapSize");
            if (heapSizeNode != null) {
                clusterConfig.setHeapSize(heapSizeNode.asText());
            }

            JsonNode heapNewSizeNode = clusterConfigNode.get("heapNewSize");
            if (heapNewSizeNode != null) {
                clusterConfig.setHeapNewSize(heapNewSizeNode.asText());
            }

            clusterConfig.setNumNodes(getInt(clusterConfigNode.get("numNodes"), 2));
        }
        simulation.setClusterConfig(clusterConfig);

        return simulation;
    }

    private MetricsConfiguration createDefaultMetricsConfiguration() {
        MetricsConfiguration configuration = new MetricsConfiguration();
        configuration.setRawTTL(180);
        configuration.setRawRetention(Seconds.seconds(180).toStandardDuration());
        configuration.setRawTimeSliceDuration(Minutes.ONE.toStandardDuration());

        configuration.setOneHourTTL(360);
        configuration.setOneHourRetention(Seconds.seconds(360));
        configuration.setOneHourTimeSliceDuration(Minutes.minutes(6).toStandardDuration());

        configuration.setSixHourTTL(540);
        configuration.setSixHourRetention(Seconds.seconds(540));
        configuration.setSixHourTimeSliceDuration(Minutes.minutes(24).toStandardDuration());

        configuration.setTwentyFourHourTTL(720);
        configuration.setTwentyFourHourRetention(Seconds.seconds(720));

        return configuration;
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
                configuration.setOneHourRetention(Seconds.seconds(ttl));
                break;
            case SIX_HOUR:
                configuration.setSixHourTTL(ttl);
                configuration.setSixHourRetention(Seconds.seconds(ttl));
                break;
            default:
                configuration.setTwentyFourHourTTL(ttl);
                configuration.setTwentyFourHourRetention(Seconds.seconds(ttl));
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
