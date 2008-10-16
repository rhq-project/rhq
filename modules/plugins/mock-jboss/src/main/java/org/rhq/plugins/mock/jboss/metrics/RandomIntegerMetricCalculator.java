/*
 * JBoss, a division of Red Hat.
 * Copyright 2007, Red Hat Middleware, LLC. All rights reserved.
 */

package org.rhq.plugins.mock.jboss.metrics;

import org.rhq.core.domain.measurement.MeasurementData;
import org.rhq.core.domain.measurement.MeasurementDataNumeric;
import org.rhq.core.domain.measurement.MeasurementScheduleRequest;
import org.rhq.plugins.mock.jboss.scenario.MetricRandomInteger;

/**
 * Measurement calculator implementation that will generate a random number (within a configurable range) on each call.
 *
 * @author Jason Dobies
 */
public class RandomIntegerMetricCalculator implements MetricValueCalculator {
    // Attributes  --------------------------------------------

    private int minimumValue;
    private int maximumValue;

    // Constructors  --------------------------------------------

    public RandomIntegerMetricCalculator(MetricRandomInteger policy) {
        minimumValue = policy.getMinimumValue();
        maximumValue = policy.getMaximumValue();
    }

    // MetricValueCalculator  --------------------------------------------

    public MeasurementData nextValue(MeasurementScheduleRequest request) {
        int value = minimumValue + (int) (Math.random() * (maximumValue - minimumValue));
        return new MeasurementDataNumeric(request, new Double(value));
    }
}
