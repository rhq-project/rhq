/*
 * RHQ Management Platform
 * Copyright (C) 2005-2011 Red Hat, Inc.
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
package org.rhq.enterprise.server.rest;

import java.util.ArrayList;
import java.util.List;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.interceptor.Interceptors;

import org.rhq.core.domain.measurement.DataType;
import org.rhq.core.domain.measurement.MeasurementDefinition;
import org.rhq.core.domain.measurement.MeasurementSchedule;
import org.rhq.core.domain.measurement.composite.MeasurementDataNumericHighLowComposite;
import org.rhq.enterprise.server.resource.ResourceManagerLocal;
import org.rhq.enterprise.server.rest.domain.MetricAggregate;
import org.rhq.enterprise.server.measurement.MeasurementAggregate;
import org.rhq.enterprise.server.measurement.MeasurementDataManagerLocal;
import org.rhq.enterprise.server.measurement.MeasurementScheduleManagerLocal;
import org.rhq.enterprise.server.rest.domain.MetricSchedule;

/**
 * Deal with metrics
 * @author Heiko W. Rupp
 */
@Interceptors(SetCallerInterceptor.class)
@Stateless
public class MetricHandlerBean  extends AbstractRestBean implements MetricHandlerLocal {

    @EJB
    MeasurementDataManagerLocal dataManager;
    @EJB
    MeasurementScheduleManagerLocal scheduleManager;
    @EJB
    ResourceManagerLocal resMgr;

    private static final long EIGHT_HOURS = 8 * 3600L * 1000L;

    @Override
    public MetricAggregate getMetricData(int scheduleId, long startTime, long endTime,
                                         int dataPoints,boolean hideEmpty) {

        if (dataPoints<0)
            throw new IllegalArgumentException("dataPoints must be >0 ");

        if (startTime==0) {
            endTime = System.currentTimeMillis();
            startTime = endTime - EIGHT_HOURS;
        }


        MeasurementSchedule schedule = scheduleManager.getScheduleById(caller,scheduleId);
        if (schedule.getDefinition().getDataType()!= DataType.MEASUREMENT)
            throw new IllegalArgumentException("Schedule [" + scheduleId + "] is not a (numerical) metric");

        MeasurementAggregate aggr = dataManager.getAggregate(caller, scheduleId, startTime, endTime);
        MetricAggregate res = new MetricAggregate(scheduleId, aggr.getMin(),aggr.getAvg(),aggr.getMax());

        int definitionId = schedule.getDefinition().getId();
        List<List<MeasurementDataNumericHighLowComposite>> listList = dataManager.findDataForResource(caller,
                schedule.getResource().getId(), new int[]{definitionId}, startTime, endTime, dataPoints);

        long minTime=Long.MAX_VALUE;
        long maxTime=0;

        if (!listList.isEmpty()) {
            List<MeasurementDataNumericHighLowComposite> list = listList.get(0);
            for (MeasurementDataNumericHighLowComposite c : list) {
                long timestamp = c.getTimestamp();
                if (!Double.isNaN(c.getValue()) || !hideEmpty) {
                    MetricAggregate.DataPoint dp = new MetricAggregate.DataPoint(timestamp,c.getValue(),c.getHighValue(),c.getLowValue());
                    res.addDataPoint(dp);
                }
                if (timestamp <minTime)
                    minTime= timestamp;
                if (timestamp >maxTime)
                    maxTime= timestamp;
            }
            res.setNumDataPoints(list.size());
        }
        res.setMaxTimeStamp(maxTime);
        res.setMinTimeStamp(minTime);


        return res;
    }

    public String getMetricDataHtml(int scheduleId,
                                    long startTime,
                                    long endTime,
                                    int dataPoints,
                                    boolean hideEmpty) {
        MetricAggregate agg = getMetricData(scheduleId,startTime,endTime,dataPoints,hideEmpty);
        return renderTemplate("metricData", agg);
    }

    public MetricSchedule getSchedule(int scheduleId) {

        MeasurementSchedule schedule = scheduleManager.getScheduleById(caller,scheduleId);
        if (schedule==null)
            throw new StuffNotFoundException("Schedule with id " + scheduleId);
        MeasurementDefinition definition = schedule.getDefinition();
        MetricSchedule ms = new MetricSchedule(schedule.getId(), definition.getName(), definition.getDisplayName(),
                schedule.isEnabled(),schedule.getInterval(), definition.getUnits().toString(),
                definition.getDataType().toString());

        return ms;
    }

    @Override
    public String getScheduleHtml(int scheduleId) {
        MetricSchedule ms = getSchedule(scheduleId);
        return renderTemplate("metricSchedule", ms);
    }

    @Override
    public List<MetricAggregate> getAggregatesForResource( int resourceId) {

        List<MeasurementSchedule> schedules = scheduleManager.findSchedulesForResourceAndType(caller,
                resourceId, DataType.MEASUREMENT, null,false);
        List<MetricAggregate> ret = new ArrayList<MetricAggregate>(schedules.size());

        long now = System.currentTimeMillis();
        long then = now - EIGHT_HOURS;

        for (MeasurementSchedule schedule: schedules) {
            MeasurementAggregate aggr = dataManager.getAggregate(caller,schedule.getId(),then,now);
            MetricAggregate res = new MetricAggregate(schedule.getId(), aggr.getMin(),aggr.getAvg(),aggr.getMax());
            ret.add(res);
        }
        return ret;

    }

    @Override
    public MetricSchedule updateSchedule(int scheduleId, MetricSchedule in) {
        if (in==null)
            throw new StuffNotFoundException("Input is null"); // TODO other type of exception

        if (in.getScheduleId()==null)
            throw new StuffNotFoundException("Invalid input data");

        MeasurementSchedule schedule = scheduleManager.getScheduleById(caller, scheduleId);
        if (schedule==null)
            throw new StuffNotFoundException("Schedule with id " + scheduleId);

        schedule.setEnabled(in.getEnabled());
        schedule.setInterval(in.getCollectionInterval());

        scheduleManager.updateSchedule(caller, schedule);

        schedule = scheduleManager.getScheduleById(caller,scheduleId);

        MetricSchedule ret = new MetricSchedule(scheduleId,in.getScheduleName(),in.getDisplayName(),schedule.isEnabled(),schedule.getInterval(),in.getUnit(),in.getType());

        return ret;
    }
}
