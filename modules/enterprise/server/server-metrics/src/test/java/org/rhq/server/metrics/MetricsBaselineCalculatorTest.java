/*
 * RHQ Management Platform
 * Copyright (C) 2005-2014 Red Hat, Inc.
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

package org.rhq.server.metrics;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.testng.PowerMockObjectFactory;
import org.testng.Assert;
import org.testng.IObjectFactory;
import org.testng.annotations.ObjectFactory;
import org.testng.annotations.Test;

import org.rhq.core.domain.measurement.MeasurementBaseline;
import org.rhq.server.metrics.domain.AggregateNumericMetric;
import org.rhq.server.metrics.domain.Bucket;

/**
 * @author Stefan Negrea
 *
 */
@PrepareForTest({ MetricsBaselineCalculator.class })
public class MetricsBaselineCalculatorTest {

    private static final double TEST_PRECISION = Math.pow(10, -10);

    private MetricsConfiguration metricsConfiguration = new MetricsConfiguration();

    @ObjectFactory
    public IObjectFactory getObjectFactory() {
        return new PowerMockObjectFactory();
    }

    @Test
    public void noCalculationTest() throws Exception {
        //tell the method story as it happens: mock dependencies and configure
        //those dependencies to get the method under test to completion.
        StorageSession mockSession = mock(StorageSession.class);
        MetricsDAO mockMetricsDAO = mock(MetricsDAO.class);
        PowerMockito.whenNew(MetricsDAO.class).withParameterTypes(StorageSession.class, MetricsConfiguration.class)
            .withArguments(eq(mockSession), eq(metricsConfiguration)).thenReturn(mockMetricsDAO);

        when(mockMetricsDAO.findAggregateMetrics(eq(1), eq(Bucket.ONE_HOUR), eq(0), eq(1))).thenReturn(
            new ArrayList<AggregateNumericMetric>());

        Set expectedScheduleId = new HashSet(1);
        expectedScheduleId.add(2567);

        //create object to test and inject required dependencies
        MetricsBaselineCalculator objectUnderTest = new MetricsBaselineCalculator(new MetricsDAO(mockSession,
            metricsConfiguration));

        //run code under test
        Map<Integer, MeasurementBaseline> result = objectUnderTest.calculateBaselines(expectedScheduleId, 0, 1);

        //verify the results (Assert and mock verification)
        assertEquals(result.size(), 0, "No baselines expected");

        verify(mockMetricsDAO, times(1)).findAggregateMetrics(any(Integer.class), eq(Bucket.ONE_HOUR),
            any(Integer.class), any(Integer.class));
        verifyNoMoreInteractions(mockMetricsDAO);
    }

    @Test
    public void randomDataTest() throws Exception {
        //generate random data
        Random random = new Random();
        List<AggregateNumericMetric> randomData = new ArrayList<AggregateNumericMetric>();

        for (int i = 0; i < 123; i++) {
            randomData.add(new AggregateNumericMetric(1, Bucket.ONE_HOUR, random.nextDouble() * 1000,
                random.nextDouble() * 1000, random.nextDouble() * 1000, System.currentTimeMillis()));
        }

        ArithmeticMeanCalculator mean = new ArithmeticMeanCalculator();
        double expectedMax = randomData.get(0).getMax();
        double expectedMin = randomData.get(0).getMin();

        for (AggregateNumericMetric metric : randomData) {
            mean.add(metric.getAvg());
            if (metric.getMax() > expectedMax) {
                expectedMax = metric.getMax();
            }
            if (metric.getMin() < expectedMin) {
                expectedMin = metric.getMin();
            }
        }

        int expectedScheduleId = 1567;
        long expectedStartTime = 135;
        long expectedEndTime = 246;
        long beforeComputeTime = System.currentTimeMillis();
        Set expectedScheduleIdSet = new HashSet(1);
        expectedScheduleIdSet.add(expectedScheduleId);

        //tell the method story as it happens: mock dependencies and configure
        //those dependencies to get the method under test to completion.
        StorageSession mockSession = mock(StorageSession.class);
        MetricsDAO mockMetricsDAO = mock(MetricsDAO.class);
        PowerMockito.whenNew(MetricsDAO.class).withParameterTypes(StorageSession.class, MetricsConfiguration.class)
            .withArguments(eq(mockSession), eq(metricsConfiguration)).thenReturn(mockMetricsDAO);

        when(
            mockMetricsDAO.findAggregateMetrics(eq(expectedScheduleId), eq(Bucket.ONE_HOUR), eq(expectedStartTime),
                eq(expectedEndTime))).thenReturn(randomData);

        //create object to test and inject required dependencies
        MetricsBaselineCalculator objectUnderTest = new MetricsBaselineCalculator(new MetricsDAO(mockSession,
            metricsConfiguration));

        //run code under test
        Map<Integer, MeasurementBaseline> result = objectUnderTest.calculateBaselines(expectedScheduleIdSet,
            expectedStartTime, expectedEndTime);

        //verify the results (Assert and mock verification)
        Assert.assertEquals(result.size(), 1);

        MeasurementBaseline baselineResult = result.get(Integer.valueOf(expectedScheduleId));
        Assert.assertEquals(baselineResult.getMean(), mean.getArithmeticMean(), TEST_PRECISION);
        Assert.assertEquals(baselineResult.getMax(), expectedMax, TEST_PRECISION);
        Assert.assertEquals(baselineResult.getMin(), expectedMin, TEST_PRECISION);
        Assert.assertEquals(baselineResult.getScheduleId(), expectedScheduleId);
        if (baselineResult.getComputeTime().getTime() > System.currentTimeMillis()) {
            Assert.fail("Back compute time, the computation was forward dated.");
        }
        if (baselineResult.getComputeTime().getTime() < beforeComputeTime) {
            Assert.fail("Back compute time, the computation was backdated.");
        }

        verify(mockMetricsDAO, times(1)).findAggregateMetrics(eq(expectedScheduleId), eq(Bucket.ONE_HOUR),
            eq(expectedStartTime), eq(expectedEndTime));
        verifyNoMoreInteractions(mockMetricsDAO);
    }

    @Test
    public void noMinMaxDataTest() throws Exception {
        //generate random data
        Random random = new Random();
        List<AggregateNumericMetric> randomData = new ArrayList<AggregateNumericMetric>();

        for (int i = 0; i < 123; i++) {
            double value = random.nextDouble() * 1000;
            randomData.add(new AggregateNumericMetric(1, Bucket.ONE_HOUR, value, value, value,
                System.currentTimeMillis()));
        }

        ArithmeticMeanCalculator mean = new ArithmeticMeanCalculator();
        for (AggregateNumericMetric metric : randomData) {
            mean.add(metric.getAvg());
        }

//        double average = 0;
//        for (AggregateSimpleNumericMetric metric : randomData) {
//            average += metric.getValue();
//        }
//        average = average / 123;

        double expectedMinMax = Double.NaN;
        int expectedScheduleId = 567;
        long expectedStartTime = 135;
        long expectedEndTime = 246;
        Set expectedScheduleIdSet = new HashSet(1);
        expectedScheduleIdSet.add(expectedScheduleId);

        //tell the method story as it happens: mock dependencies and configure
        //those dependencies to get the method under test to completion.
        StorageSession mockSession = mock(StorageSession.class);
        MetricsDAO mockMetricsDAO = mock(MetricsDAO.class);
        PowerMockito.whenNew(MetricsDAO.class).withParameterTypes(StorageSession.class, MetricsConfiguration.class)
            .withArguments(eq(mockSession), eq(metricsConfiguration)).thenReturn(mockMetricsDAO);

        when(
            mockMetricsDAO.findAggregateMetrics(eq(expectedScheduleId), eq(Bucket.ONE_HOUR), eq(expectedStartTime),
                eq(expectedEndTime))).thenReturn(randomData);

        //create object to test and inject required dependencies
        MetricsBaselineCalculator objectUnderTest = new MetricsBaselineCalculator(new MetricsDAO(mockSession,
            metricsConfiguration));

        //run code under test
        Map<Integer, MeasurementBaseline> result = objectUnderTest.calculateBaselines(expectedScheduleIdSet,
            expectedStartTime, expectedEndTime);

        //verify the results (Assert and mock verification)
        Assert.assertEquals(result.size(), 1);

        verify(mockMetricsDAO, times(1)).findAggregateMetrics(eq(expectedScheduleId), eq(Bucket.ONE_HOUR),
            eq(expectedStartTime), eq(expectedEndTime));
        verifyNoMoreInteractions(mockMetricsDAO);
    }
}