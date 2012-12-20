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

/**
 * @author Stefan Negrea
 */
public class MetricBaselineCalculator {

    private MetricsDAO metricsDAO;

    public MetricBaselineCalculator(Session session) {
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
            double min = metrics.get(0).getMin();
            double max = metrics.get(0).getMax();
            double average = 0;

            for (AggregatedNumericMetric entry : metrics) {
                if (entry.getMax() > max) {
                    max = entry.getMax();
                } else if (entry.getMin() < min) {
                    min = entry.getMin();
                }

                average += entry.getAvg();
            }

            average = average / (double) metrics.size();

            MeasurementBaseline baseline = new MeasurementBaseline();
            baseline.setMax(max);
            baseline.setMin(min);
            baseline.setMean(average);
            baseline.setScheduleId(scheduleId);

            return baseline;
        }

        return null;
    }
}
