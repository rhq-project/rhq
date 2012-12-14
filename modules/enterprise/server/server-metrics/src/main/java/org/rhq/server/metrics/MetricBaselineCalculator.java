/*
 *
 * RHQ Management Platform
 * Copyright (C) 2005-2012 Red Hat, Inc.
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
 *
 */
package org.rhq.server.metrics;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.rhq.core.domain.measurement.MeasurementBaseline;

/**
 * @author Stefan Negrea
 */
public class MetricBaselineCalculator {

    public List<MeasurementBaseline> calculateBaselines(List<Integer> scheduleIds, long startTime, long endTime) {
        List<MeasurementBaseline> calculatedBaselines = new ArrayList<MeasurementBaseline>();

        MeasurementBaseline measurementBaseline;
        for (Integer scheduleId : scheduleIds) {
            //TODO: do processing to find the actual baseline for this schedule id
            //!use data from Cassandra!

            //for now just return a values
            measurementBaseline = this.generateRandomBaseline(scheduleId);

            if (measurementBaseline != null) {
                calculatedBaselines.add(measurementBaseline);
            }
        }

        return calculatedBaselines;
    }

    private MeasurementBaseline generateRandomBaseline(Integer scheduleId) {
        Random random = new Random(12345);

        MeasurementBaseline randomBaseline = new MeasurementBaseline();
        randomBaseline.setMax(random.nextDouble() * 1000);
        randomBaseline.setMin(random.nextDouble() * 1000);
        randomBaseline.setMean(random.nextDouble() * 1000);

        randomBaseline.setScheduleId(scheduleId);

        return randomBaseline;
    }

    private MeasurementBaseline calculateBaseline(Integer scheduleId) {
        String NATIVE_QUERY_CALC_FIRST_AUTOBASELINE_POSTGRES = "" //
            + "    INSERT INTO RHQ_MEASUREMENT_BLINE ( id, BL_MIN, BL_MAX, BL_MEAN, BL_COMPUTE_TIME, SCHEDULE_ID ) " //
            + "         SELECT nextval('RHQ_MEASUREMENT_BLINE_ID_SEQ'), " //
            + "                MIN(data1h.minvalue) AS bline_min, " //
            + "                MAX(data1h.maxvalue) AS bline_max, " //
            + "                AVG(data1h.value) AS bline_mean, " //
            + "                ? AS bline_ts, " // ?1=computeTime
            + "                data1h.SCHEDULE_ID AS bline_sched_id " //
            + "           FROM RHQ_MEASUREMENT_DATA_NUM_1H data1h  " // baselines are 1H data statistics
            + "     INNER JOIN RHQ_MEASUREMENT_SCHED sched  " // baselines are aggregates of schedules
            + "             ON data1h.SCHEDULE_ID = sched.id  " //
            + "     INNER JOIN RHQ_MEASUREMENT_DEF def " // only compute off of dynamic types
            + "             ON sched.definition = def.id " //
            + "LEFT OUTER JOIN RHQ_MEASUREMENT_BLINE bline " // we want null entries on purpose
            + "             ON sched.id = bline.SCHEDULE_ID  " //
            + "          WHERE ( def.numeric_type = 0 ) " // only dynamics (NumericType.DYNAMIC)
            + "            AND ( bline.id IS NULL ) " // no baseline means it was deleted or never calculated
            + "            AND ( data1h.TIME_STAMP BETWEEN ? AND ? ) " // ?2=startTime, ?3=endTime
            + "       GROUP BY data1h.SCHEDULE_ID " // baselines are aggregates per schedule
            // but only calculate baselines for schedules where we have data that fills (startTime, endTime)
            + "         HAVING data1h.SCHEDULE_ID in ( SELECT distinct (mdata.SCHEDULE_ID) "
            + "                                          FROM RHQ_MEASUREMENT_DATA_NUM_1H mdata  " //
            + "                                         WHERE mdata.TIME_STAMP <= ? ) " // ?4=startTime
            + "          LIMIT 100000 "; // batch at most 100K inserts at a time to shrink the xtn size
        return null;
    }
}
