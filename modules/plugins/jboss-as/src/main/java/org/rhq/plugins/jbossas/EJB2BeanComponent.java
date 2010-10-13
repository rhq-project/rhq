/*
 * Jopr Management Platform
 * Copyright (C) 2005-2008 Red Hat, Inc.
 * All rights reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2, as
 * published by the Free Software Foundation, and/or the GNU Lesser
 * General Public License, version 2.1, also as published by the Free
 * Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License and the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License
 * and the GNU Lesser General Public License along with this program;
 * if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */
package org.rhq.plugins.jbossas;

import java.lang.reflect.Method;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mc4j.ems.connection.bean.EmsBean;
import org.mc4j.ems.connection.bean.attribute.EmsAttribute;

import org.rhq.core.domain.measurement.DataType;
import org.rhq.core.domain.measurement.MeasurementReport;
import org.rhq.core.domain.measurement.MeasurementScheduleRequest;
import org.rhq.core.domain.measurement.calltime.CallTimeData;
import org.rhq.core.domain.measurement.calltime.CallTimeDataValue;
import org.rhq.plugins.jmx.MBeanResourceComponent;

/**
 * A plugin component for managing an EJB2 session bean.
 *
 * @author Greg Hinkle
 * @author Ian Springer
 * @author John Mazzitelli
 */
public class EJB2BeanComponent extends MBeanResourceComponent<JBossASServerComponent> {
    private final Log log = LogFactory.getLog(EJB2BeanComponent.class);

    private Map<Integer, CallTimeData> previousRawCallTimeDatas = new HashMap<Integer, CallTimeData>();

    @Override
    public void getValues(MeasurementReport report, Set<MeasurementScheduleRequest> schedules) {
        Set<MeasurementScheduleRequest> numericMetricSchedules = new LinkedHashSet<MeasurementScheduleRequest>();
        for (MeasurementScheduleRequest schedule : schedules) {
            if (schedule.getDataType() == DataType.MEASUREMENT) {
                numericMetricSchedules.add(schedule);
            } else if (schedule.getName().equals("MethodInvocationTime")) {
                Object statelessSessionBeanStats; // javax.management.j2ee.statistics.StatelessSessionBeanStats
                try {
                    statelessSessionBeanStats = getStatelessSessionBeanStats();
                } catch (Exception e) {
                    // Unsure if this will ever happen - maybe the JBoss instance doesn't provide this operation?
                    continue;
                }

                try {
                    Map<String, Stat> stats = getStats(statelessSessionBeanStats);
                    Date lastResetTime = getLastResetTime(statelessSessionBeanStats);
                    Date now = new Date(System.currentTimeMillis());
                    if (!stats.isEmpty()) {
                        CallTimeData callTimeData = createCallTimeData(schedule, stats, lastResetTime, now);
                        report.addData(callTimeData);
                    }
                } catch (Exception e) {
                    log.error("Failed to retrieve EJB2 call-time data.", e);
                }
            }
        }

        super.getValues(report, numericMetricSchedules);
    }

    private CallTimeData createCallTimeData(MeasurementScheduleRequest schedule, Map<String, Stat> stats,
        Date lastResetTime, Date collectionTime) throws Exception {

        CallTimeData previousRawCallTimeData = this.previousRawCallTimeDatas.get(schedule.getScheduleId());
        CallTimeData rawCallTimeData = new CallTimeData(schedule);
        this.previousRawCallTimeDatas.put(schedule.getScheduleId(), rawCallTimeData);

        CallTimeData callTimeData = new CallTimeData(schedule);
        for (String methodName : stats.keySet()) {
            Stat timeStatistic = stats.get(methodName);

            long minTime = timeStatistic.min;
            long maxTime = timeStatistic.max;
            long totalTime = timeStatistic.total;
            long count = timeStatistic.count;

            try {
                rawCallTimeData.addAggregatedCallData(methodName, lastResetTime, collectionTime, minTime, maxTime,
                    totalTime, count);
            } catch (IllegalArgumentException iae) {
                // if any issue with the data, log them and continue processing the rest of the report
                log.error(iae);
                continue;
            }

            // Now compute the adjusted data, which is what we will report back to the server.
            CallTimeDataValue previousValue = (previousRawCallTimeData != null) ? previousRawCallTimeData.getValues()
                .get(methodName) : null;
            boolean supercedesPrevious = ((previousValue != null) && (previousValue.getBeginTime() == lastResetTime
                .getTime()));
            Date beginTime = lastResetTime;
            if (supercedesPrevious) {
                // The data for this method hasn't been reset since the last time we collected it.
                long countSincePrevious = count - previousValue.getCount();
                if (countSincePrevious > 0) {
                    // There have been new calls since the last time we collected data
                    // for this method. Adjust the time span to begin at the end of the
                    // time span from the previous collection.
                    beginTime = new Date(previousValue.getEndTime());

                    // Adjust the total and count to reflect the adjusted time span;
                    // do so by subtracting the previous values from the current values.
                    // NOTE: It isn't possible to figure out the minimum and maximum for
                    // the adjusted time span, so just leave them be. If they happen
                    // to have changed since the previous collection, they will be
                    // accurate; otherwise they will not.
                    count = countSincePrevious;
                    totalTime = totalTime - (long) previousValue.getTotal();
                }
                // else, the count hasn't changed, so don't bother adjusting the data;
                // when the server sees the data has the same begin time as
                // previously persisted data, it will replace the previous data with the
                // updated data (which will basically have a later end time)
            }

            callTimeData.addAggregatedCallData(methodName, beginTime, collectionTime, minTime, maxTime, totalTime,
                count);
        }

        return callTimeData;
    }

    private Date getLastResetTime(Object statelessSessionBeanStats) throws Exception {
        // XXX we assume no one will ever execute the Jboss MBean's resetStats
        // XXX we look at a count stat's start time and use that as the last reset time
        Method method = statelessSessionBeanStats.getClass().getMethod("getMethodReadyCount");
        Object methodReadyCountStat = method.invoke(statelessSessionBeanStats);
        long time = (Long) methodReadyCountStat.getClass().getMethod("getStartTime").invoke(methodReadyCountStat);
        return new Date(time);
    }

    private Object getStatelessSessionBeanStats() throws Exception {
        // will actually be a type of: javax.management.j2ee.statistics.StatelessSessionBeanStats
        Object statelessSessionBeanStats = null;

        // accesses the remote MBean to get information on the EJB
        EmsBean emsBean = getEmsBean();

        // use the connection's classloader so we use the appropriate class definitions
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(emsBean.getClass().getClassLoader());

        try {
            EmsAttribute attribute = emsBean.getAttribute("stats");
            statelessSessionBeanStats = attribute.refresh();
        } catch (RuntimeException e) {
            String msg = "Failed to retrieve EJB2 invocation stats.";
            if (log.isDebugEnabled()) {
                log.debug(msg, e);
            } else {
                log.info(msg + " Enable DEBUG logging to see cause.");
            }
            throw new Exception(msg, e);
        } finally {
            Thread.currentThread().setContextClassLoader(cl);
        }
        return statelessSessionBeanStats;
    }

    private Map<String, Stat> getStats(Object statelessSessionBeanStats) throws Exception {

        Map<String, Stat> stats = new HashMap<String, Stat>();

        Method method = statelessSessionBeanStats.getClass().getMethod("getStatistics");
        Object[] jbossStats = (Object[]) method.invoke(statelessSessionBeanStats); // javax.management.j2ee.statistics.Statistic[]

        if (jbossStats != null) {
            for (Object jbossStat : jbossStats) {
                Class<? extends Object> clazz = jbossStat.getClass();
                if (clazz.getSimpleName().contains("TimeStatistic")) {
                    // there appears to be a bug in Jboss stats - startTime always changes when you get
                    // the values and lastSampleTime is always 0.
                    Stat newStat = new Stat();
                    newStat.name = (String) clazz.getMethod("getName").invoke(jbossStat);
                    newStat.count = (Long) clazz.getMethod("getCount").invoke(jbossStat);
                    newStat.min = (Long) clazz.getMethod("getMinTime").invoke(jbossStat);
                    newStat.max = (Long) clazz.getMethod("getMaxTime").invoke(jbossStat);
                    newStat.total = (Long) clazz.getMethod("getTotalTime").invoke(jbossStat);
                    newStat.startTime = (Long) clazz.getMethod("getStartTime").invoke(jbossStat);
                    newStat.lastSampleTime = (Long) clazz.getMethod("getLastSampleTime").invoke(jbossStat);
                    stats.put(newStat.name, newStat);
                }
            }
        }

        return stats;
    }

    class Stat {
        String name;
        long count;
        long min;
        long max;
        long total;
        long startTime;
        long lastSampleTime;
    }
}