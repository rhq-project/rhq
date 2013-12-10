/*
 * RHQ Management Platform
 * Copyright (C) 2005-2013 Red Hat, Inc.
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
 * along with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA
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

import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.domain.measurement.DataType;
import org.rhq.core.domain.measurement.MeasurementReport;
import org.rhq.core.domain.measurement.MeasurementScheduleRequest;
import org.rhq.core.domain.measurement.calltime.CallTimeData;
import org.rhq.core.domain.util.OSGiVersion;
import org.rhq.core.pluginapi.inventory.ResourceComponent;
import org.rhq.core.util.stream.StreamUtil;
import org.rhq.modules.plugins.jbossas7.json.Address;
import org.rhq.modules.plugins.jbossas7.json.ReadAttribute;
import org.rhq.modules.plugins.jbossas7.json.Result;

/**
 * A specialization of the AS component for EJB3 runtime beans. This kind of beans can collect method invocation
 * statistics which is what this class handles.
 *
 * @author Lukas Krejci
 * @since 4.10.0
 */
public class Ejb3BeanRuntimeComponent extends BaseComponent<BaseComponent<?>> {

    private static final String METHODS_ATTRIBUTE = "methods";
    private static final int CALLTIME_METRIC_NAME_PREFIX_LENGTH = "__calltime:".length();
    private static final Address RUNTIME_MBEAN_ADDRESS = new Address("core-service=platform-mbean,type=runtime");
    private static final String START_TIME_ATTRIBUTE = "start-time";

    private static final OSGiVersion FIRST_VERSION_SUPPORTING_METHOD_STATS = new OSGiVersion("7.2.1");

    private OSGiVersion asVersion = null;

    private static class StatsRecord implements Serializable {
        private static final long serialVersionUID = 1L;

        long invocations;
        long total;
    }

    private static class Stats implements Serializable {
        private static final long serialVersionUID = 1L;

        //we kept the serialVersionUID=1 even though this field was added. This means that the collection will work
        //even with the persisted data coming from the previous version of the plugin.
        //We need to have special handling in the code that works around the collectionStartTime having 0 value.
        long serverStartTime;
        long collectionTime;
        Map<String, StatsRecord> data;

        static Stats fromMap(Map<String, Map<String, Number>> map, String collectedMetric, long collectionTime,
            long serverStartTime) {

            Stats ret = new Stats();
            ret.serverStartTime = serverStartTime;
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
    public AvailabilityType getAvailability() {
        AvailabilityType avail = super.getAvailability();

        if (avail == AvailabilityType.DOWN) {
            asVersion = null;
        }

        return avail;
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
                    @SuppressWarnings("unchecked")
                    Map<String, Map<String, Number>> allMethodStats = (Map<String, Map<String, Number>>) value;

                    if (allMethodStats.isEmpty()) {
                        continue;
                    }

                    //first we need to know since when the values were collected
                    result = getASConnection().execute(new ReadAttribute(RUNTIME_MBEAN_ADDRESS, START_TIME_ATTRIBUTE));
                    long serverStartTime = (Long) result.getResult();

                    //now process the calltime value
                    String requestedMetric = request.getName().substring(CALLTIME_METRIC_NAME_PREFIX_LENGTH);

                    Stats lastCollection = getLastCallTimeCollection(requestedMetric, allMethodStats, serverStartTime);
                    Stats thisCollection = Stats.fromMap(allMethodStats, requestedMetric, System.currentTimeMillis(),
                        serverStartTime);

                    CallTimeData callTime = new CallTimeData(request);

                    fillCallTimeData(callTime, thisCollection, lastCollection);

                    saveCallTimeCollection(requestedMetric, thisCollection);

                    report.addData(callTime);
                } else {
                    OSGiVersion currentAsVersion = getASVersion();
                    if (currentAsVersion == null) {
                        getLog().warn(
                            "Could not determine the AS version while reporting unexpected result of method"
                                + " stats. Request: " + request);
                    } else if (FIRST_VERSION_SUPPORTING_METHOD_STATS.compareTo(currentAsVersion) <= 0) {
                        getLog().error(
                            "Unexpected type of results when querying method stats for measurement request " + request
                                + ". Expected map but got " + (value == null ? "null" : value.getClass().getName()));
                    }
                }
            }
        }

        super.getValues(report, metricsToPassDown);
    }

    private Stats getLastCallTimeCollection(String requestName, Map<String, Map<String, Number>> fallbackValues,
        long fallbackStartTime) throws IOException {

        File dataFile = new File(context.getResourceDataDirectory(), requestName);
        if (!dataFile.exists()) {
            return Stats.fromMap(fallbackValues, requestName, System.currentTimeMillis(), fallbackStartTime);
        } else {
            ObjectInputStream in = null;
            try {
                in = new ObjectInputStream(new FileInputStream(dataFile));

                Stats stats = (Stats) in.readObject();
                if (stats.serverStartTime == 0) {
                    //we might get serverStartTime == 0 if the datafile comes from the old version of the plugin
                    //in that case just fallback to the old behavior that assumed no server restarts.
                    //After that we save the new version of the stats with the start time remembered and we will
                    //switch to the new correct behavior from the next collection.
                    stats.serverStartTime = fallbackStartTime;
                }

                return stats;
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
     * @param stats the current stats collected from the AS
     * @param previousStats the previously collected stats
     */
    private void fillCallTimeData(CallTimeData callTimeData, Stats stats, Stats previousStats) {
        Date startDate = new Date(previousStats.collectionTime);
        Date endDate = new Date(stats.collectionTime);

        boolean serverRestarted = stats.serverStartTime != previousStats.serverStartTime;

        for(Map.Entry<String, StatsRecord> entry : stats.data.entrySet()) {
            String methodName = entry.getKey();
            StatsRecord thisStatsRecord = entry.getValue();

            long invocations;
            long total;

            if (serverRestarted) {
                // If server restarted, we know the counter goes from 0. Note that in that case we still might have
                // missed some invocations - the ones that happened between the last collection and the server restart.
                // That cannot be avoided though unless the server itself pushes the data to us instead of us pulling
                // the data on a schedule.
                invocations = thisStatsRecord.invocations;
                total = thisStatsRecord.total;
            } else {
                StatsRecord previousStatsRecord = previousStats.data.get(methodName);

                long oldInvocations = previousStatsRecord != null ? previousStatsRecord.invocations : 0;
                invocations = thisStatsRecord.invocations - oldInvocations;

                long oldTotal = previousStatsRecord != null ? previousStatsRecord.total : 0;
                total = thisStatsRecord.total - oldTotal;
            }

            if (invocations == 0) {
                continue;
            }

            //AS doesn't really give us this info...
            double min = (double) total / invocations;
            double max = (double) total / invocations;

            callTimeData.addAggregatedCallData(methodName, startDate, endDate, min, max, total,
                invocations);
        }
    }

    private OSGiVersion getASVersion() {
        if (asVersion == null)  {
            ResourceComponent<?> base = context.getParentResourceComponent();

            while (base != null && base instanceof BaseComponent && !(base instanceof BaseServerComponent)) {
                base = ((BaseComponent<?>)base).context.getParentResourceComponent();
            }

            if (base != null && base instanceof BaseServerComponent) {
                String version = ((BaseServerComponent<?>)base).getReleaseVersion();
                asVersion = new OSGiVersion(version);
            }
        }

        return asVersion;
    }
}
