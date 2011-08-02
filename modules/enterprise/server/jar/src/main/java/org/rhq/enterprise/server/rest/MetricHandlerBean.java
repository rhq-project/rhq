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

import java.util.List;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.interceptor.Interceptors;

import org.rhq.core.domain.measurement.DataType;
import org.rhq.core.domain.measurement.MeasurementSchedule;
import org.rhq.core.domain.measurement.composite.MeasurementDataNumericHighLowComposite;
import org.rhq.enterprise.server.rest.domain.MetricAggregate;
import org.rhq.enterprise.server.measurement.MeasurementAggregate;
import org.rhq.enterprise.server.measurement.MeasurementDataManagerLocal;
import org.rhq.enterprise.server.measurement.MeasurementScheduleManagerLocal;

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
    private static final long EIGHT_HOURS = 8 * 3600L * 1000L;

    @Override
    public MetricAggregate getMetricData(int scheduleId, long startTime, long endTime,
                                         int dataPoints) {

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
        MetricAggregate res = new MetricAggregate(aggr.getMin(),aggr.getAvg(),aggr.getMax());

        int definitionId = schedule.getDefinition().getId();
        List<List<MeasurementDataNumericHighLowComposite>> listList = dataManager.findDataForResource(caller,
                schedule.getResource().getId(), new int[]{definitionId}, startTime, endTime, dataPoints);

        if (!listList.isEmpty()) {
            List<MeasurementDataNumericHighLowComposite> list = listList.get(0);
            for (MeasurementDataNumericHighLowComposite c : list) {
                MetricAggregate.DataPoint dp = new MetricAggregate.DataPoint(c.getTimestamp(),c.getValue(),c.getHighValue(),c.getLowValue());
                res.addDataPoint(dp);
            }
        }


        return res;
    }
}
