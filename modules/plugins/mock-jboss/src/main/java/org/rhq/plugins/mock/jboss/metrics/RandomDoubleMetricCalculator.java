/*
 * JBoss, a division of Red Hat.
 * Copyright 2007, Red Hat Middleware, LLC. All rights reserved.
 */

package org.rhq.plugins.mock.jboss.metrics;

import org.rhq.core.domain.measurement.MeasurementData;
import org.rhq.core.domain.measurement.MeasurementDataNumeric;
import org.rhq.core.domain.measurement.MeasurementScheduleRequest;
import org.rhq.plugins.mock.jboss.scenario.MetricRandomDouble;

/**
 * Measurement calculator implementation that will generate a random number (within a configurable range) on each call.
 *
 * @author Jason Dobies
 */
public class RandomDoubleMetricCalculator implements MetricValueCalculator {
    // Attributes --------------------------------------------

    private double minimumValue;
    private double maximumValue;

    // Constructors  --------------------------------------------

    public RandomDoubleMetricCalculator(MetricRandomDouble policy) {
        minimumValue = policy.getMinimumValue();
        maximumValue = policy.getMaximumValue();
    }

    // MetricValueCalculator Implementation  --------------------------------------------

    public MeasurementData nextValue(MeasurementScheduleRequest request) {
        double value = minimumValue + (Math.random() * (maximumValue - minimumValue));
        return new MeasurementDataNumeric(request, value);
    }
}
