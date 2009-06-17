/*
* Jopr Management Platform
* Copyright (C) 2005-2009 Red Hat, Inc.
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
package org.rhq.plugins.jbossas5;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.managed.api.ComponentType;
import org.jboss.managed.api.ManagedComponent;
import org.jboss.managed.api.ManagedOperation;
import org.jboss.managed.api.ManagedProperty;
import org.jboss.metatype.api.values.CompositeValue;
import org.jboss.metatype.api.values.SimpleValue;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertyList;
import org.rhq.core.domain.configuration.PropertyMap;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.measurement.MeasurementReport;
import org.rhq.core.domain.measurement.MeasurementScheduleRequest;
import org.rhq.core.domain.measurement.calltime.CallTimeData;
import org.rhq.core.pluginapi.operation.OperationFacet;
import org.rhq.core.pluginapi.operation.OperationResult;
import org.rhq.plugins.jbossas5.util.Ejb2BeanUtils;

/**
 * 
 * @author Lukas Krejci
 * @author Ian Springer
 */
public class Ejb2BeanComponent extends ManagedComponentComponent implements OperationFacet {
    private final Log log = LogFactory.getLog(this.getClass());

    private static final ComponentType mdbComponentType = new ComponentType("EJB", "MDB");

    @Override
    protected ManagedComponent getManagedComponent() {
        if (mdbComponentType.equals(getComponentType())) {
            try {
                Set<ManagedComponent> mdbs = getConnection().getManagementView().getComponentsForType(mdbComponentType);

                for (ManagedComponent mdb : mdbs) {
                    if (getComponentName().equals(Ejb2BeanUtils.getUniqueBeanIdentificator(mdb))) {
                        return mdb;
                    }
                }
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        } else {
            return super.getManagedComponent();
        }

        return null;
    }

    @Override
    public void getValues(MeasurementReport report, Set<MeasurementScheduleRequest> requests) throws Exception {
        Set<MeasurementScheduleRequest> remainingRequests = new LinkedHashSet();
        for (MeasurementScheduleRequest request : requests) {
            String metricName = request.getName();
            try {
                if (metricName.equals("methodInvocationTime")) {
                    // Convert the method stats CompositeValues into nice strongly typed objects.
                    InvocationStats invocationStats = getInvocationStats();
                    if (!invocationStats.methodStats.isEmpty()) {
                        CallTimeData callTimeData = createCallTimeData(request, invocationStats);
                        report.addData(callTimeData);
                        resetInvocationStats();
                    }
                } else {
                    remainingRequests.add(request);
                }
            } catch (Exception e) {
                // Don't let one bad apple spoil the barrel.
                log.error("Failed to collect metric '" + metricName + "' for " + getResourceDescription() + ".", e);
            }
        }
        // Let our superclass handle any metrics we didn't collect.
        super.getValues(report, remainingRequests);
    }

    @Override
    public OperationResult invokeOperation(String name, Configuration parameters) throws Exception {
        OperationResult result;
        if ("methodStats".equals(name)) {
            result = new OperationResult();
            PropertyList methodList = new PropertyList("methods");
            result.getComplexResults().put(methodList);

            // Convert the invocation stats CompositeValues into a nice strongly typed object.
            List<MethodStats> allMethodStats = getInvocationStats().methodStats;

            for (MethodStats methodStats : allMethodStats) {
                PropertyMap method = new PropertyMap("method", new PropertySimple("methodName", methodStats.name),
                    new PropertySimple("count", methodStats.count), new PropertySimple("minTime", methodStats.minTime),
                    new PropertySimple("maxTime", methodStats.maxTime), new PropertySimple("totalTime",
                        methodStats.totalTime));
                methodList.add(method);
            }
        } else {
            result = super.invokeOperation(name, parameters);
        }
        return result;
    }

    private InvocationStats getInvocationStats() {
        InvocationStats invocationStats = new InvocationStats();
        List<MethodStats> allMethodStats = new ArrayList<MethodStats>();
        ManagedProperty detypedInvokedStatsProp = this.getManagedComponent().getProperty("DetypedInvocationStatistics");
        invocationStats.endTime = System.currentTimeMillis();
        CompositeValue detypedInvokeStatsMetaValue = (CompositeValue) detypedInvokedStatsProp.getValue();
        CompositeValue allMethodStatsMetaValue = (CompositeValue) detypedInvokeStatsMetaValue.get("methodStats");
        Set<String> methodNames = allMethodStatsMetaValue.getMetaType().keySet();
        for (String methodName : methodNames) {
            CompositeValue methodStatsMetaValue = (CompositeValue) allMethodStatsMetaValue.get(methodName);
            MethodStats methodStats = new MethodStats();
            methodStats.name = methodName;
            methodStats.count = Long.parseLong(((SimpleValue) methodStatsMetaValue.get("count")).getValue().toString());
            methodStats.totalTime = Long.parseLong(((SimpleValue) methodStatsMetaValue.get("totalTime")).getValue()
                .toString());
            methodStats.minTime = Long.parseLong(((SimpleValue) methodStatsMetaValue.get("minTime")).getValue()
                .toString());
            methodStats.maxTime = Long.parseLong(((SimpleValue) methodStatsMetaValue.get("maxTime")).getValue()
                .toString());
            allMethodStats.add(methodStats);
        }
        invocationStats.methodStats = allMethodStats;

        SimpleValue lastResetTimeMetaValue = (SimpleValue) ((CompositeValue) detypedInvokedStatsProp.getValue())
            .get("lastResetTime");
        invocationStats.beginTime = Long.valueOf(lastResetTimeMetaValue.getValue().toString()); // TODO: handle null value?

        return invocationStats;
    }

    private CallTimeData createCallTimeData(MeasurementScheduleRequest schedule, InvocationStats invocationStats)
        throws Exception {
        CallTimeData callTimeData = new CallTimeData(schedule);
        Date beginDate = new Date(invocationStats.beginTime);
        Date endDate = new Date(invocationStats.endTime);
        for (MethodStats methodStats : invocationStats.methodStats) {
            callTimeData.addAggregatedCallData(methodStats.name, beginDate, endDate, methodStats.minTime,
                methodStats.maxTime, methodStats.totalTime, methodStats.count);
        }
        return callTimeData;
    }

    private void resetInvocationStats() {
        Set<ManagedOperation> operations = getManagedComponent().getOperations();
        for (ManagedOperation operation : operations) {
            if (operation.getName().equals("resetInvocationStats")) {
                operation.invoke();
                break;
            }
        }
    }

    class InvocationStats {
        List<MethodStats> methodStats;
        long beginTime;
        long endTime;
    }

    class MethodStats {
        String name;
        long count;
        long minTime;
        long maxTime;
        long totalTime;
    }
}
