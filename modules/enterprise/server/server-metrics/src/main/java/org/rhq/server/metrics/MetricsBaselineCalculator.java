/*
 *
 * RHQ Management Platform
 * Copyright (C) 2005-2014 Red Hat, Inc.
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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.domain.measurement.MeasurementBaseline;
import org.rhq.server.metrics.domain.AggregateNumericMetric;
import org.rhq.server.metrics.domain.Bucket;

/**
 * @author Stefan Negrea
 */
public class MetricsBaselineCalculator {

    private final Log log = LogFactory.getLog(MetricsBaselineCalculator.class);

    private MetricsDAO metricsDAO;

    public MetricsBaselineCalculator(MetricsDAO metricsDAO) {
        this.metricsDAO = metricsDAO;
    }

    public Map<Integer, MeasurementBaseline> calculateBaselines(Set<Integer> scheduleIds, long startTime, long endTime) {
        Map<Integer, MeasurementBaseline> calculatedBaselines = new HashMap<Integer, MeasurementBaseline>();

        MeasurementBaseline measurementBaseline;
        for (Integer scheduleId : scheduleIds) {
            measurementBaseline = this.calculateBaseline(scheduleId, startTime, endTime);
            if (measurementBaseline != null) {
                calculatedBaselines.put(scheduleId, measurementBaseline);
            }
        }

        return calculatedBaselines;
    }

    private MeasurementBaseline calculateBaseline(Integer schedule, long startTime, long endTime) {
        List<AggregateNumericMetric> metrics = metricsDAO.findAggregateMetrics(schedule, Bucket.ONE_HOUR, startTime,
            endTime);
//        Iterable<AggregateSimpleNumericMetric> metrics = this.metricsDAO.findAggregatedSimpleOneHourMetric(schedule,
//            startTime, endTime);
        if (metrics.isEmpty()) {
            return null;
        }

        ArithmeticMeanCalculator mean = new ArithmeticMeanCalculator();
        Double max = metrics.get(0).getMax();
        Double min = metrics.get(0).getMin();

        for (AggregateNumericMetric metric : metrics) {
            mean.add(metric.getAvg());
            if (metric.getMax() > max) {
                max = metric.getMax();
            }
            if (metric.getMin() < min) {
                min = metric.getMin();
            }
        }

        MeasurementBaseline baseline = new MeasurementBaseline();
        baseline.setMax(max);
        baseline.setMin(min);
        baseline.setMean(mean.getArithmeticMean());
        baseline.setScheduleId(schedule);

        if (log.isDebugEnabled()) {
            log.debug("Calculated baseline: " + baseline.toString());
        }

        return baseline;

//        if (metrics != null && metrics.iterator() != null && metrics.iterator().hasNext()) {
//            ArithmeticMeanCalculator mean = new ArithmeticMeanCalculator();
//
//            double max = Double.NaN;
//            for (AggregateSimpleNumericMetric entry : metrics) {
//                if (AggregateType.MAX.equals(entry.getType())) {
//                    max = entry.getValue();
//                    break;
//                }
//            }
//
//            double min = Double.NaN;
//            for (AggregateSimpleNumericMetric entry : metrics) {
//                if (AggregateType.MIN.equals(entry.getType())) {
//                    min = entry.getValue();
//                    break;
//                }
//            }
//
//            for (AggregateSimpleNumericMetric entry : metrics) {
//                if (AggregateType.AVG.equals(entry.getType())) {
//                    mean.add(entry.getValue());
//                } else if (AggregateType.MAX.equals(entry.getType())) {
//                    if (max < entry.getValue()) {
//                        max = entry.getValue();
//                    }
//                } else if (AggregateType.MIN.equals(entry.getType())) {
//                    if (min > entry.getValue()) {
//                        min = entry.getValue();
//                    }
//                }
//            }
//
//            if (Double.isNaN(max) || Double.isNaN(min) || Double.isNaN(mean.getArithmeticMean())) {
//                //There is not enough data retrieved from the storage cluster to complete the
//                //baseline calculation for this schedule.
//                //
//                //It is expected that at least one min, one max, and one average
//                //are available and also these are valid numbers.
//                return null;
//            }
//
//            MeasurementBaseline baseline = new MeasurementBaseline();
//            baseline.setMax(max);
//            baseline.setMin(min);
//            baseline.setMean(mean.getArithmeticMean());
//            baseline.setScheduleId(schedule);
//
//            if (log.isDebugEnabled()) {
//                log.debug("Calculated baseline: " + baseline.toString());
//            }
//
//            return baseline;
//        }
//
//        return null;
    }
}