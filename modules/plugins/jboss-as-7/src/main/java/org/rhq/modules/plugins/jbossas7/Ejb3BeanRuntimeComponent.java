/*
 * RHQ Management Platform
 * Copyright (C) 2013 Red Hat, Inc.
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

package org.rhq.modules.plugins.jbossas7;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.rhq.core.domain.measurement.DataType;
import org.rhq.core.domain.measurement.MeasurementReport;
import org.rhq.core.domain.measurement.MeasurementScheduleRequest;
import org.rhq.core.domain.measurement.calltime.CallTimeData;
import org.rhq.core.pluginapi.inventory.ResourceComponent;
import org.rhq.core.util.stream.StreamUtil;
import org.rhq.modules.plugins.jbossas7.json.ReadAttribute;
import org.rhq.modules.plugins.jbossas7.json.Result;

/**
 * A specialization of the AS component for EJB3 runtime beans. This kind of beans can collect method invocation
 * statistics which is what this class handles.
 *
 * @author Lukas Krejci
 * @since 4.9
 */
public class Ejb3BeanRuntimeComponent extends BaseComponent<ResourceComponent<?>> {

    private static final String METHODS_ATTRIBUTE = "methods";
    private static final int CALLTIME_METRIC_NAME_PREFIX_LENGTH = "__calltime:".length();

    private static class StatsRecord implements Serializable {
        private static final long serialVersionUID = 1L;

        long invocations;
        long total;
    }

    private static class Stats implements Serializable {
        private static final long serialVersionUID = 1L;

        long collectionTime;
        Map<String, StatsRecord> data;

        static Stats fromMap(Map<String, Map<String, Number>> map, String collectedMetric, long collectionTime) {
            Stats ret = new Stats();
            ret.collectionTime = collectionTime;
            ret.data = new HashMap<String, StatsRecord>(map.size());

            for(Map.Entry<String, Map<String, Number>> entry : map.entrySet()) {
                StatsRecord rec = new StatsRecord();
                String methodName = entry.getKey();

                rec.invocations = entry.getValue().get("invocations").longValue();
                rec.total = entry.getValue().get(collectedMetric).longValue();

                ret.data.put(methodName, rec);
            }

            return ret;
        }
    }

    @Override
    public void getValues(MeasurementReport report, Set<MeasurementScheduleRequest> metrics) throws Exception {
        //we'll handling the rest of the metrics using the super method, but we may leave out some of the requests
        //if we handle them here. Right now, just use the obtained set. We only create a copy of the (unmodifiable) set
        //of requests if necessary.
        Set<MeasurementScheduleRequest> metricsToPassDown = metrics;

        for(MeasurementScheduleRequest request : metrics) {
            if (request.getDataType() == DataType.CALLTIME) {
                //make a copy to pass down to super class if necessary
                if (metricsToPassDown == metrics) {
                    metricsToPassDown = new HashSet<MeasurementScheduleRequest>(metrics);
                }

                metricsToPassDown.remove(request);

                //handle this ourselves
                //the name of the metric is actually the name of the stat collected for each method. we then provide
                //the calltime data for each method.

                Result result = getASConnection().execute(new ReadAttribute(address, METHODS_ATTRIBUTE));
                Object value = result.getResult();
                if (value instanceof Map) {
                    String requestedMetric = request.getName().substring(CALLTIME_METRIC_NAME_PREFIX_LENGTH);

                    @SuppressWarnings("unchecked")
                    Map<String, Map<String, Number>> allMethodStats = (Map<String, Map<String, Number>>) value;

                    Stats lastCollection = getLastCallTimeCollection(requestedMetric, allMethodStats);
                    Stats thisCollection = Stats.fromMap(allMethodStats, requestedMetric, System.currentTimeMillis());

                    CallTimeData callTime = new CallTimeData(request);

                    fillCallTimeData(callTime, requestedMetric, thisCollection, lastCollection);

                    saveCallTimeCollection(requestedMetric, thisCollection);

                    report.addData(callTime);
                } else {
                    log.error("Unexpected type of results when querying method stats");
                }
            }
        }

        super.getValues(report, metricsToPassDown);
    }

    private Stats getLastCallTimeCollection(String requestName, Map<String, Map<String, Number>> fallbackValues) throws IOException {
        File dataFile = new File(context.getResourceDataDirectory(), requestName);
        if (!dataFile.exists()) {
            return Stats.fromMap(fallbackValues, requestName, System.currentTimeMillis());
        } else {
            ObjectInputStream in = null;
            try {
                in = new ObjectInputStream(new FileInputStream(dataFile));

                return (Stats) in.readObject();
            } catch (IOException e) {
                throw new IOException("Couldn't read the stored calltime data from file " + dataFile + ".", e);
            } catch (ClassNotFoundException e) {
                throw new IllegalStateException("Couldn't find plugin API classes. This is serious!", e);
            } finally {
                StreamUtil.safeClose(in);
            }
        }
    }

    private void saveCallTimeCollection(String requestName, Stats stats) throws IOException {
        File dataFile = new File(context.getResourceDataDirectory(), requestName);

        ObjectOutputStream out = null;
        try {
            out = new ObjectOutputStream(new FileOutputStream(dataFile));
            out.writeObject(stats);
        } catch (IOException e) {
            throw new IOException("Couldn't write the last collected calltime data to file " + dataFile + ".", e);
        } finally {
            StreamUtil.safeClose(out);
        }
    }

    /**
     * Given the current and previous stats collected from the AS, this fills in the provided calltime data record with
     * the differential values from the last time to this time.
     *
     * @param callTimeData the calltime record to fill in
     * @param requestName the name of the metric being collected
     * @param stats the current stats collected from the AS
     * @param previousStats the previously collected stats
     */
    private void fillCallTimeData(CallTimeData callTimeData, String requestName, Stats stats, Stats previousStats) {
        Date startDate = new Date(previousStats.collectionTime);
        Date endDate = new Date(stats.collectionTime);

        for(Map.Entry<String, StatsRecord> entry : stats.data.entrySet()) {
            String methodName = entry.getKey();
            StatsRecord thisStatsRecord = entry.getValue();

            StatsRecord previousStatsRecord = previousStats.data.get(methodName);

            long oldInvocations = previousStatsRecord != null ? previousStatsRecord.invocations : 0;
            long invocations = thisStatsRecord.invocations - oldInvocations;
            if (invocations == 0) {
                continue;
            }

            long oldTotal = previousStatsRecord != null ? previousStatsRecord.total : 0;
            long total = thisStatsRecord.total - oldTotal;

            //AS doesn't really give us this info...
            double min = (double) total / invocations;
            double max = (double) total / invocations;

            callTimeData.addAggregatedCallData(methodName, startDate, endDate, min, max, total,
                invocations);
        }

    }
}
