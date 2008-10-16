/*
 * JBoss, a division of Red Hat.
 * Copyright 2007, Red Hat Middleware, LLC. All rights reserved.
 */

package org.rhq.plugins.mock.jboss.metrics.test;

import org.testng.annotations.Test;

import org.rhq.core.domain.measurement.DataType;
import org.rhq.core.domain.measurement.MeasurementData;
import org.rhq.core.domain.measurement.MeasurementDataNumeric;
import org.rhq.core.domain.measurement.MeasurementScheduleRequest;
import org.rhq.plugins.mock.jboss.metrics.IncrementMetricCalculator;
import org.rhq.plugins.mock.jboss.metrics.RandomDoubleMetricCalculator;
import org.rhq.plugins.mock.jboss.metrics.RandomIntegerMetricCalculator;
import org.rhq.plugins.mock.jboss.metrics.StaticMetricCalculator;
import org.rhq.plugins.mock.jboss.scenario.MetricIncrement;
import org.rhq.plugins.mock.jboss.scenario.MetricRandomDouble;
import org.rhq.plugins.mock.jboss.scenario.MetricRandomInteger;
import org.rhq.plugins.mock.jboss.scenario.MetricStatic;

/**
 * @author Jason Dobies
 */
public class MetricValueCalculatorTest {
    // Test Cases  --------------------------------------------

    private static MeasurementScheduleRequest TEST_REQUEST = new MeasurementScheduleRequest(0, "test", 0, true,
        DataType.MEASUREMENT);

    @Test
    public void staticTest() {
        MetricStatic policy = new MetricStatic();
        policy.setValue(42.1);

        StaticMetricCalculator calculator = new StaticMetricCalculator(policy);

        MeasurementData data;

        data = calculator.nextValue(TEST_REQUEST);
        assert data.getValue().equals(42.1) : "Incorrect value returned from first call";

        data = calculator.nextValue(TEST_REQUEST);
        assert data.getValue().equals(42.1) : "Incorrect value returned from second call";
    }

    @Test
    public void randomDoubleTest() {
        MetricRandomDouble policy = new MetricRandomDouble();
        policy.setMinimumValue(20.5);
        policy.setMaximumValue(35.2);

        RandomDoubleMetricCalculator calculator = new RandomDoubleMetricCalculator(policy);

        MeasurementDataNumeric data;

        data = (MeasurementDataNumeric) calculator.nextValue(TEST_REQUEST);
        assert data.getValue() >= 20.5 && data.getValue() <= 35.2 : "Value was not within the random range limits";

        data = (MeasurementDataNumeric) calculator.nextValue(TEST_REQUEST);
        assert data.getValue() >= 20.5 && data.getValue() <= 35.2 : "Value was not within the random range limits";
    }

    @Test
    public void randomIntegerTest() {
        MetricRandomInteger policy = new MetricRandomInteger();
        policy.setMinimumValue(20);
        policy.setMaximumValue(35);

        RandomIntegerMetricCalculator calculator = new RandomIntegerMetricCalculator(policy);

        MeasurementDataNumeric data;

        data = (MeasurementDataNumeric) calculator.nextValue(TEST_REQUEST);
        assert data.getValue() >= 20 && data.getValue() <= 35 : "Value was not within the random range limits";

        data = (MeasurementDataNumeric) calculator.nextValue(TEST_REQUEST);
        assert data.getValue() >= 20 && data.getValue() <= 35 : "Value was not within the random range limits";
    }

    @Test
    public void incrementTestDefaultFrequency() {
        MetricIncrement policy = new MetricIncrement();
        policy.setBaseValue(5);
        policy.setIncrementValue(3);

        IncrementMetricCalculator calculator = new IncrementMetricCalculator(policy);

        MeasurementDataNumeric data;

        data = (MeasurementDataNumeric) calculator.nextValue(TEST_REQUEST);
        assert data.getValue() == 5 : "Incorrect value on iteration 1";

        data = (MeasurementDataNumeric) calculator.nextValue(TEST_REQUEST);
        assert data.getValue() == 8 : "Incorrect value on iteration 2";

        data = (MeasurementDataNumeric) calculator.nextValue(TEST_REQUEST);
        assert data.getValue() == 11 : "Incorrect value on iteration 3";
    }

    @Test
    public void incrementTestOverriddenFrequency() {
        MetricIncrement policy = new MetricIncrement();
        policy.setBaseValue(5);
        policy.setIncrementValue(3);
        policy.setIncrementFrequency(3);

        IncrementMetricCalculator calculator = new IncrementMetricCalculator(policy);

        MeasurementDataNumeric data;

        data = (MeasurementDataNumeric) calculator.nextValue(TEST_REQUEST);
        assert data.getValue() == 5 : "Incorrect value on iteration 1";

        data = (MeasurementDataNumeric) calculator.nextValue(TEST_REQUEST);
        assert data.getValue() == 5 : "Incorrect value on iteration 2";
        data = (MeasurementDataNumeric) calculator.nextValue(TEST_REQUEST);
        assert data.getValue() == 8 : "Incorrect value on iteration 3";

        data = (MeasurementDataNumeric) calculator.nextValue(TEST_REQUEST);
        assert data.getValue() == 8 : "Incorrect value on iteration 4";

        data = (MeasurementDataNumeric) calculator.nextValue(TEST_REQUEST);
        assert data.getValue() == 8 : "Incorrect value on iteration 5";

        data = (MeasurementDataNumeric) calculator.nextValue(TEST_REQUEST);
        assert data.getValue() == 11 : "Incorrect value on iteration 6";

        data = (MeasurementDataNumeric) calculator.nextValue(TEST_REQUEST);
        assert data.getValue() == 11 : "Incorrect value on iteration 7";

        data = (MeasurementDataNumeric) calculator.nextValue(TEST_REQUEST);
        assert data.getValue() == 11 : "Incorrect value on iteration 8";
    }
}
