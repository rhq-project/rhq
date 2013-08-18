/*
 * RHQ Management Platform
 * Copyright 2011, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
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
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.testng.PowerMockObjectFactory;
import org.testng.Assert;
import org.testng.IObjectFactory;
import org.testng.annotations.ObjectFactory;
import org.testng.annotations.Test;

import org.rhq.core.domain.measurement.MeasurementBaseline;
import org.rhq.server.metrics.domain.AggregateSimpleNumericMetric;
import org.rhq.server.metrics.domain.AggregateType;

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

        when(mockMetricsDAO.findAggregatedSimpleOneHourMetric(eq(1), eq(0), eq(1))).thenReturn(
            new ArrayList<AggregateSimpleNumericMetric>());

        int expectedScheduleId = 2567;

        //create object to test and inject required dependencies
        MetricsBaselineCalculator objectUnderTest = new MetricsBaselineCalculator(new MetricsDAO(mockSession,
            metricsConfiguration));

        //run code under test
        List<MeasurementBaseline> result = objectUnderTest.calculateBaselines(Arrays.asList(expectedScheduleId), 0, 1);

        //verify the results (Assert and mock verification)
        assertEquals(result.size(), 0, "No baselines expected");

        verify(mockMetricsDAO, times(1)).findAggregatedSimpleOneHourMetric(any(Integer.class), any(Integer.class),
            any(Integer.class));
        verifyNoMoreInteractions(mockMetricsDAO);
    }

    @Test
    public void randomDataTest() throws Exception {
        //generate random data
        Random random = new Random();
        List<AggregateSimpleNumericMetric> randomData = new ArrayList<AggregateSimpleNumericMetric>();

        for (int i = 0; i < 123; i++) {
            randomData.add(new AggregateSimpleNumericMetric(1, random.nextDouble() * 1000, AggregateType.AVG));
            randomData.add(new AggregateSimpleNumericMetric(1, random.nextDouble() * 1000, AggregateType.MAX));
            randomData.add(new AggregateSimpleNumericMetric(1, random.nextDouble() * 1000, AggregateType.MIN));
        }

        double average = 0;
        for (AggregateSimpleNumericMetric metric : randomData) {
            if (AggregateType.AVG.equals(metric.getType())) {
                average += metric.getValue();
            }
        }
        average = average / 123;

        double expectedMax = Double.MIN_VALUE;
        for (AggregateSimpleNumericMetric metric : randomData) {
            if (AggregateType.MAX.equals(metric.getType()) && expectedMax < metric.getValue()) {
                expectedMax = metric.getValue();
            }
        }

        double expectedMin = Double.MAX_VALUE;
        for (AggregateSimpleNumericMetric metric : randomData) {
            if (AggregateType.MIN.equals(metric.getType()) && expectedMin > metric.getValue()) {
                expectedMin = metric.getValue();
            }
        }

        int expectedScheduleId = 1567;
        long expectedStartTime = 135;
        long expectedEndTime = 246;
        long beforeComputeTime = System.currentTimeMillis();

        //tell the method story as it happens: mock dependencies and configure
        //those dependencies to get the method under test to completion.
        StorageSession mockSession = mock(StorageSession.class);
        MetricsDAO mockMetricsDAO = mock(MetricsDAO.class);
        PowerMockito.whenNew(MetricsDAO.class).withParameterTypes(StorageSession.class, MetricsConfiguration.class)
            .withArguments(eq(mockSession), eq(metricsConfiguration)).thenReturn(mockMetricsDAO);

        when(
            mockMetricsDAO.findAggregatedSimpleOneHourMetric(eq(expectedScheduleId), eq(expectedStartTime),
                eq(expectedEndTime))).thenReturn(randomData);

        //create object to test and inject required dependencies
        MetricsBaselineCalculator objectUnderTest = new MetricsBaselineCalculator(new MetricsDAO(mockSession,
            metricsConfiguration));

        //run code under test
        List<MeasurementBaseline> result = objectUnderTest.calculateBaselines(Arrays.asList(expectedScheduleId),
            expectedStartTime, expectedEndTime);

        //verify the results (Assert and mock verification)
        Assert.assertEquals(result.size(), 1);

        MeasurementBaseline baselineResult = result.get(0);
        Assert.assertEquals(baselineResult.getMean(), average, TEST_PRECISION);
        Assert.assertEquals(baselineResult.getMax(), expectedMax, TEST_PRECISION);
        Assert.assertEquals(baselineResult.getMin(), expectedMin, TEST_PRECISION);
        Assert.assertEquals(baselineResult.getScheduleId(), expectedScheduleId);
        if (baselineResult.getComputeTime().getTime() > System.currentTimeMillis()) {
            Assert.fail("Back compute time, the computation was forward dated.");
        }
        if (baselineResult.getComputeTime().getTime() < beforeComputeTime) {
            Assert.fail("Back compute time, the computation was backdated.");
        }

        verify(mockMetricsDAO, times(1)).findAggregatedSimpleOneHourMetric(eq(expectedScheduleId),
            eq(expectedStartTime), eq(expectedEndTime));
        verifyNoMoreInteractions(mockMetricsDAO);
    }

    @Test
    public void noMinMaxDataTest() throws Exception {
        //generate random data
        Random random = new Random();
        List<AggregateSimpleNumericMetric> randomData = new ArrayList<AggregateSimpleNumericMetric>();

        for (int i = 0; i < 123; i++) {
            randomData.add(new AggregateSimpleNumericMetric(1, random.nextDouble() * 1000, AggregateType.AVG));
        }

        double average = 0;
        for (AggregateSimpleNumericMetric metric : randomData) {
            average += metric.getValue();
        }
        average = average / 123;

        double expectedMinMax = Double.NaN;
        int expectedScheduleId = 567;
        long expectedStartTime = 135;
        long expectedEndTime = 246;

        //tell the method story as it happens: mock dependencies and configure
        //those dependencies to get the method under test to completion.
        StorageSession mockSession = mock(StorageSession.class);
        MetricsDAO mockMetricsDAO = mock(MetricsDAO.class);
        PowerMockito.whenNew(MetricsDAO.class).withParameterTypes(StorageSession.class, MetricsConfiguration.class)
            .withArguments(eq(mockSession), eq(metricsConfiguration)).thenReturn(mockMetricsDAO);

        when(
            mockMetricsDAO.findAggregatedSimpleOneHourMetric(eq(expectedScheduleId), eq(expectedStartTime),
                eq(expectedEndTime))).thenReturn(randomData);

        //create object to test and inject required dependencies
        MetricsBaselineCalculator objectUnderTest = new MetricsBaselineCalculator(new MetricsDAO(mockSession,
            metricsConfiguration));

        //run code under test
        List<MeasurementBaseline> result = objectUnderTest.calculateBaselines(Arrays.asList(expectedScheduleId),
            expectedStartTime, expectedEndTime);

        //verify the results (Assert and mock verification)
        Assert.assertEquals(result.size(), 1);

        MeasurementBaseline baselineResult = result.get(0);
        Assert.assertEquals(baselineResult.getMean(), average, TEST_PRECISION);
        Assert.assertEquals(baselineResult.getMax(), expectedMinMax);
        Assert.assertEquals(baselineResult.getMin(), expectedMinMax);
        Assert.assertEquals(baselineResult.getScheduleId(), expectedScheduleId);

        verify(mockMetricsDAO, times(1)).findAggregatedSimpleOneHourMetric(eq(expectedScheduleId),
            eq(expectedStartTime), eq(expectedEndTime));
        verifyNoMoreInteractions(mockMetricsDAO);
    }
}
