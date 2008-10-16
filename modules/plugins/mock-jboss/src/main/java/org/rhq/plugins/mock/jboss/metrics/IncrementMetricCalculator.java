/*
 * JBoss, a division of Red Hat.
 * Copyright 2007, Red Hat Middleware, LLC. All rights reserved.
 */

package org.rhq.plugins.mock.jboss.metrics;

import org.rhq.core.domain.measurement.MeasurementData;
import org.rhq.core.domain.measurement.MeasurementDataNumeric;
import org.rhq.core.domain.measurement.MeasurementScheduleRequest;
import org.rhq.plugins.mock.jboss.scenario.MetricIncrement;

/**
 * Measurement calculator implementation that will return an increasing value on subsequent calls. The amount and
 * frequency at which the value increases is configurable.
 *
 * @author Jason Dobies
 */
public class IncrementMetricCalculator implements MetricValueCalculator {
    // Attributes  --------------------------------------------

    private int incrementFrequency;
    private int incrementValue;

    private int currentValue;
    private int numberOfRequests;

    // Constructors  --------------------------------------------

    public IncrementMetricCalculator(MetricIncrement policy) {
        currentValue = policy.getBaseValue();

        incrementFrequency = policy.getIncrementFrequency();
        incrementValue = policy.getIncrementValue();

        numberOfRequests = 0;
    }

    // MetricValueCalculator Implementation  --------------------------------------------

    public MeasurementData nextValue(MeasurementScheduleRequest request) {
        numberOfRequests++;

        // Don't increment on the first call, to allow the base value to be returned initially
        if (numberOfRequests == 1)
            return new MeasurementDataNumeric(request, new Double(currentValue));

        // Only increment according to frequency 
        if (numberOfRequests % incrementFrequency == 0) {
            currentValue += incrementValue;
        }

        return new MeasurementDataNumeric(request, new Double(currentValue));
    }

}
