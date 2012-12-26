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

import com.datastax.driver.core.Session;

import org.rhq.core.domain.measurement.MeasurementBaseline;
import org.rhq.core.domain.util.PageOrdering;

/**
 * @author Stefan Negrea
 */
public class MetricsBaselineCalculator {

    private MetricsDAO metricsDAO;

    public MetricsBaselineCalculator(Session session) {
        this.metricsDAO = new MetricsDAO(session);
    }

    public List<MeasurementBaseline> calculateBaselines(List<Integer> scheduleIds, long startTime, long endTime) {
        List<MeasurementBaseline> calculatedBaselines = new ArrayList<MeasurementBaseline>();

        MeasurementBaseline measurementBaseline;
        for (Integer scheduleId : scheduleIds) {
            measurementBaseline = this.calculateBaseline(scheduleId, startTime, endTime);
            if (measurementBaseline != null) {
                calculatedBaselines.add(measurementBaseline);
            }
        }

        return calculatedBaselines;
    }

    private MeasurementBaseline calculateBaseline(Integer scheduleId, long startTime, long endTime) {
        List<AggregatedNumericMetric> metrics = this.metricsDAO.findAggregateMetrics(MetricsTable.ONE_HOUR, scheduleId, startTime, endTime);

        if (metrics.size() != 0) {
            ArithmeticMeanCalculator mean = new ArithmeticMeanCalculator();

            for (AggregatedNumericMetric entry : metrics) {
                mean.add(entry.getAvg());
            }

            double min = 0;
            List<Double> results = this.metricsDAO.findAggregateSimpleMetric(MetricsTable.ONE_HOUR, AggregateType.MIN,
                scheduleId, startTime, endTime, PageOrdering.ASC, 1);
            if(results.size() != 0){
                min = results.get(0);
            }

            double max = 0;
            results = this.metricsDAO.findAggregateSimpleMetric(MetricsTable.ONE_HOUR, AggregateType.MAX, scheduleId,
                startTime, endTime, PageOrdering.DESC, 1);
            if (results.size() != 0) {
                max = results.get(0);
            }

            MeasurementBaseline baseline = new MeasurementBaseline();
            baseline.setMax(max);
            baseline.setMin(min);
            baseline.setMean(mean.getArithmeticMean());
            baseline.setScheduleId(scheduleId);

            return baseline;
        }

        return null;
    }
}
