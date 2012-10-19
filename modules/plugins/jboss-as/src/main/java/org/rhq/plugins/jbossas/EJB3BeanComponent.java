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

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertyList;
import org.rhq.core.domain.configuration.PropertyMap;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.domain.measurement.DataType;
import org.rhq.core.domain.measurement.MeasurementReport;
import org.rhq.core.domain.measurement.MeasurementScheduleRequest;
import org.rhq.core.domain.measurement.calltime.CallTimeData;
import org.rhq.core.domain.measurement.calltime.CallTimeDataValue;
import org.rhq.core.pluginapi.operation.OperationResult;
import org.rhq.plugins.jbossas.util.JBossMBeanUtility;
import org.rhq.plugins.jmx.MBeanResourceComponent;

/**
 * A plugin component for managing an EJB3 session bean.
 *
 * @author Greg Hinkle
 * @author Ian Springer
 */
public class EJB3BeanComponent extends MBeanResourceComponent<JBossASServerComponent> {
    private final Log log = LogFactory.getLog(EJB3BeanComponent.class);

    private Map<Integer, CallTimeData> previousRawCallTimeDatas = new HashMap<Integer, CallTimeData>();

    @Override
    public AvailabilityType getAvailability() {
        return (JBossMBeanUtility.isStarted(getEmsBean(), getResourceContext())) ? AvailabilityType.UP
            : AvailabilityType.DOWN;
    }

    @Override
    public OperationResult invokeOperation(String name, Configuration parameters) throws Exception {
        if ("viewMethodStats".equals(name)) {
            // TODO GH: Ems should be managing the context classloader for the refresh call (also check the operations 1 more time)
            Object invocationStatistics = getInvocationStatistics();

            OperationResult result = new OperationResult();
            PropertyList methodList = new PropertyList("methods");
            result.getComplexResults().put(methodList);

            Map<String, Object> stats = getStats(invocationStatistics);
            for (String methodName : stats.keySet()) {
                Object timeStatistic = stats.get(methodName);

                Long count = (Long) timeStatistic.getClass().getField("count").get(timeStatistic);
                Long minTime = (Long) timeStatistic.getClass().getField("minTime").get(timeStatistic);
                Long maxTime = (Long) timeStatistic.getClass().getField("maxTime").get(timeStatistic);
                Long totalTime = (Long) timeStatistic.getClass().getField("totalTime").get(timeStatistic);

                PropertyMap method = new PropertyMap("method", new PropertySimple("methodName", methodName),
                    new PropertySimple("count", count), new PropertySimple("minTime", minTime), new PropertySimple(
                        "maxTime", maxTime), new PropertySimple("totalTime", totalTime));
                methodList.add(method);
            }

            return result;
        }

        return super.invokeOperation(name, parameters);
    }

    @Override
    public void getValues(MeasurementReport report, Set<MeasurementScheduleRequest> schedules) {
        Set<MeasurementScheduleRequest> numericMetricSchedules = new LinkedHashSet<MeasurementScheduleRequest>();
        for (MeasurementScheduleRequest schedule : schedules) {
            if (schedule.getDataType() == DataType.MEASUREMENT) {
                numericMetricSchedules.add(schedule);
            } else if (schedule.getName().equals("MethodInvocationTime")) {
                Object invocationStatistics;
                try {
                    invocationStatistics = getInvocationStatistics();
                } catch (Exception e) {
                    // This will be fairly common, since only JBossAS 4.2.x provides this operation, so don't log an
                    // error.
                    continue;
                }
                try {
                    long lastResetTime = getLastResetTime(invocationStatistics);
                    Map<String, Object> stats = getStats(invocationStatistics);
                    long collectionTime = System.currentTimeMillis();
                    if (!stats.isEmpty()) {
                        CallTimeData callTimeData = createCallTimeData(schedule, stats, new Date(lastResetTime),
                            new Date(collectionTime));
                        report.addData(callTimeData);
                    }
                } catch (Exception e) {
                    log.error("Failed to retrieve EJB3 call-time data.", e);
                }
            }
        }

        super.getValues(report, numericMetricSchedules);
    }

    private CallTimeData createCallTimeData(MeasurementScheduleRequest schedule, Map<String, Object> stats,
        Date lastResetTime, Date collectionTime) throws Exception {
        CallTimeData previousRawCallTimeData = this.previousRawCallTimeDatas.get(schedule.getScheduleId());
        CallTimeData rawCallTimeData = new CallTimeData(schedule);
        this.previousRawCallTimeDatas.put(schedule.getScheduleId(), rawCallTimeData);
        CallTimeData callTimeData = new CallTimeData(schedule);
        for (String methodName : stats.keySet()) {
            Object timeStatistic = stats.get(methodName);

            long minTime = (Long) timeStatistic.getClass().getField("minTime").get(timeStatistic);
            long maxTime = (Long) timeStatistic.getClass().getField("maxTime").get(timeStatistic);
            long totalTime = (Long) timeStatistic.getClass().getField("totalTime").get(timeStatistic);
            long count = (Long) timeStatistic.getClass().getField("count").get(timeStatistic);

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
                // when the JON server sees the data has the same begin time as
                // previously persisted data, it will replace the previous data with the
                // updated data (which will basically have a later end time)
            }

            callTimeData.addAggregatedCallData(methodName, beginTime, collectionTime, minTime, maxTime, totalTime,
                count);
        }

        return callTimeData;
    }

    // NOTE: Invocation stats were not exposed by EJB3 MBeans in versions of JBoss EJB3 prior to RC9 Patch 1
    //       (see http://jira.jboss.com/jira/browse/EJBTHREE-742 and
    //       http://viewvc.jboss.org/cgi-bin/viewvc.cgi/jbossas?view=rev&revision=57901). The EJB3 builds
    //       bundled with JBossAS 4.2.x are newer than RC9 Patch 1, so invocation stats should always be
    //       available from AS 4.2.x instances.
    private Object getInvocationStatistics() throws Exception {
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(getEmsBean().getClass().getClassLoader());
        // Value is an InvocationStatistics object, but to avoid classloader issues, we don't cast it as such.
        // (see http://anonsvn.jboss.org/repos/jbossas/branches/Branch_4_2/ejb3/src/main/org/jboss/ejb3/statistics/InvocationStatistics.java)
        Object invocationStatistics = null;
        try {
            invocationStatistics = getEmsBean().getAttribute("InvokeStats").refresh();
        } catch (RuntimeException e) {
            String msg = "Failed to retrieve EJB3 invocation stats - perhaps JBoss EJB3 impl version is less than RC9 Patch 1.";
            log.info(msg + " Enable DEBUG logging to see cause.");
            if (log.isDebugEnabled())
                log.debug(msg, e);
            throw new Exception(msg, e);
        } finally {
            Thread.currentThread().setContextClassLoader(cl);
        }
        return invocationStatistics;
    }

    private long getLastResetTime(Object invocationStatistics) throws Exception {
        Field field = invocationStatistics.getClass().getField("lastResetTime");
        return (Long) field.get(invocationStatistics);
    }

    private Map<String, Object> getStats(Object invocationStatistics) throws Exception {
        Method method = invocationStatistics.getClass().getMethod("getStats");
        return (Map<String, Object>) method.invoke(invocationStatistics);
    }
}