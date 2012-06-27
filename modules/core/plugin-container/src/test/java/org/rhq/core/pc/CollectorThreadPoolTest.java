/*
 * RHQ Management Platform
 * Copyright (C) 2005-2008 Red Hat, Inc.
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
 */
package org.rhq.core.pc;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.domain.measurement.DataType;
import org.rhq.core.domain.measurement.MeasurementDataNumeric;
import org.rhq.core.domain.measurement.MeasurementDataTrait;
import org.rhq.core.domain.measurement.MeasurementReport;
import org.rhq.core.domain.measurement.MeasurementScheduleRequest;
import org.rhq.core.domain.measurement.calltime.CallTimeData;
import org.rhq.core.pluginapi.availability.AvailabilityCollectorRunnable;
import org.rhq.core.pluginapi.availability.AvailabilityFacet;
import org.rhq.core.pluginapi.measurement.MeasurementCollectorRunnable;
import org.rhq.core.pluginapi.measurement.MeasurementFacet;

@Test
public class CollectorThreadPoolTest {

    protected final Log log = LogFactory.getLog(getClass());
    private CollectorThreadPool threadPool;

    @BeforeTest
    public void beforeTest() {
        threadPool = new CollectorThreadPool();
    }

    @AfterTest
    public void afterTest() {
        threadPool.shutdown();
        threadPool = null;
    }

    public void testCollector() throws Exception {

        AvailabilityType[] avail = new AvailabilityType[] { AvailabilityType.UP };
        TestAvailabilityFacet component = new TestAvailabilityFacet(avail);
        AvailabilityCollectorRunnable runnable = new AvailabilityCollectorRunnable(component, 60000L, null,
            this.threadPool.getExecutor());
        runnable.start();
        Thread.sleep(1000L);
        assert AvailabilityType.UP == runnable.getLastKnownAvailability();

        // availability collector cannot allow for collections faster than 60s. So we can't have tests faster than this.
        // set this if-check to true to fully test the collector (which takes a couple mins of wait time to complete)
        if (System.getProperty("AvailabilityCollectorTest.longtest", "false").equals("true")) {
            avail[0] = AvailabilityType.DOWN;
            log("~~~~~~~~~~sleeping for 60 secs");
            Thread.sleep(60100L);
            assert AvailabilityType.DOWN == runnable.getLastKnownAvailability() : "Collector should have seen the change";

            runnable.stop();
            avail[0] = AvailabilityType.UP;
            log("~~~~~~~~~~sleeping for 60 secs");
            Thread.sleep(60100L);
            assert AvailabilityType.DOWN == runnable.getLastKnownAvailability() : "Collector should have stopped and not see the change";
        }
    }

    private void log(String string) {
        log.info(string);
    }

    public void testMeasurement() throws Exception {
        log("testMeasurement");
        TestMeasumentFacet component = new TestMeasumentFacet();
        // 0L means do the initial collection immediately with no delay - so our test can run fast 
        MeasurementCollectorRunnable runnable = new MeasurementCollectorRunnable(component, 0L, 500L, null,
                this.threadPool.getExecutor());
        Set<MeasurementScheduleRequest> metrics = new HashSet<MeasurementScheduleRequest>();
        metrics.add(new MeasurementScheduleRequest(0, "name", 0, true, DataType.TRAIT));
        MeasurementReport report = new MeasurementReport();
        runnable.getLastValues(report, metrics);
        assert 0 == report.getCollectionTime();
        assert report.getTraitData().isEmpty();

        runnable.start();

        log("sleeping");
        Thread.sleep(1000L); // just give it some time to do its thing

        report = new MeasurementReport();
        runnable.getLastValues(report, metrics);
        assert 42 == report.getCollectionTime();
        assert !report.getTraitData().isEmpty();

        runnable.stop();
    }

    public void testMultipleMeasurements() throws Exception {
        log("testMultipleMeasurements");

        TestMultipleMeasumentFacet component = new TestMultipleMeasumentFacet();

        Set<MeasurementScheduleRequest> onlyNumericMetric = new HashSet<MeasurementScheduleRequest>();
        onlyNumericMetric.add(component.getNumericMetricSchedule());

        Set<MeasurementScheduleRequest> onlyTraitMetric = new HashSet<MeasurementScheduleRequest>();
        onlyTraitMetric.add(component.getTraitMetricSchedule());

        Set<MeasurementScheduleRequest> onlyCalltimeMetric = new HashSet<MeasurementScheduleRequest>();
        onlyCalltimeMetric.add(component.getCalltimeMetricSchedule());

        Set<MeasurementScheduleRequest> allMetrics = new HashSet<MeasurementScheduleRequest>();
        allMetrics.addAll(onlyNumericMetric);
        allMetrics.addAll(onlyTraitMetric);
        allMetrics.addAll(onlyCalltimeMetric);

        MeasurementCollectorRunnable runnable = new MeasurementCollectorRunnable(component, 0L, 123L, null,
            this.threadPool.getExecutor());

        MeasurementReport report = new MeasurementReport();
        runnable.getLastValues(report, onlyNumericMetric); // we only ask for the numeric metric, not all
        // we haven't started the runnable yet, so nothing is collected yet
        assert 0 == report.getCollectionTime();
        assert report.getNumericData().isEmpty();
        assert report.getTraitData().isEmpty();
        assert report.getCallTimeData().isEmpty();

        runnable.start();
        Thread.sleep(1000L);

        report = new MeasurementReport();
        runnable.getLastValues(report, allMetrics); // even though we are asking for all, last time we only collected the numeric metric
        assert 1000 == report.getCollectionTime();
        assert !report.getNumericData().isEmpty();
        assert report.getTraitData().isEmpty() : "should not have collected the trait data yet";
        assert report.getCallTimeData().isEmpty() : "should not have collected the calltime data yet";

        // let's create another runnable so we can speed up the test (using a 0 initial delay)
        runnable.stop();
        component = new TestMultipleMeasumentFacet();
        runnable = new MeasurementCollectorRunnable(component, 0L, 123L, null, this.threadPool.getExecutor());

        report = new MeasurementReport();
        runnable.getLastValues(report, allMetrics); // prime the pump by asking for all metrics
        // we haven't started the runnable yet, so nothing is collected yet
        assert 0 == report.getCollectionTime();
        assert report.getNumericData().isEmpty();
        assert report.getTraitData().isEmpty();
        assert report.getCallTimeData().isEmpty();

        runnable.start();
        Thread.sleep(1000L);

        // now we only ask for individual metrics, not all of them at once
        report = new MeasurementReport();
        runnable.getLastValues(report, onlyNumericMetric);
        assert 1000 == report.getCollectionTime();
        assert report.getNumericData().size() == 1;
        assert report.getTraitData().isEmpty() : "we didn't ask for the trait data";
        assert report.getCallTimeData().isEmpty() : "we didn't ask for the calltime data";

        // that numeric data should be gone now - if we ask for it again, we shouldn't get it
        report = new MeasurementReport();
        runnable.getLastValues(report, onlyNumericMetric);
        assert 1000 == report.getCollectionTime();
        assert report.getNumericData().isEmpty() : "the numeric data should have already been retrieved and removed";

        // now ask for the trait data
        report = new MeasurementReport();
        runnable.getLastValues(report, onlyTraitMetric);
        assert 1000 == report.getCollectionTime();
        assert report.getNumericData().isEmpty() : "we didn't ask for the numeric data";
        assert report.getTraitData().size() == 1;
        assert report.getCallTimeData().isEmpty() : "we didn't ask for the calltime data";
        report = new MeasurementReport();
        runnable.getLastValues(report, onlyTraitMetric);
        assert 1000 == report.getCollectionTime();
        assert report.getTraitData().isEmpty() : "the trait data should have already been retrieved and removed";


        // now ask for the calltime data
        report = new MeasurementReport();
        runnable.getLastValues(report, onlyCalltimeMetric);
        assert 1000 == report.getCollectionTime();
        assert report.getNumericData().isEmpty() : "we didn't ask for the numeric data";
        assert report.getTraitData().isEmpty() : "we didn't ask for the trait data";
        assert report.getCallTimeData().size() == 1;
        report = new MeasurementReport();
        runnable.getLastValues(report, onlyCalltimeMetric);
        assert 1000 == report.getCollectionTime();
        assert report.getCallTimeData().isEmpty() : "the calltime data should have already been retrieved and removed";

        runnable.stop();
    }

    @Test(enabled = false)
    // TODO re-enable this test - it passes
    public void testMultipleCollectionPeriods() throws Exception {
        TestMultipleMeasumentFacet component = new TestMultipleMeasumentFacet();
        Set<MeasurementScheduleRequest> allMetrics = new HashSet<MeasurementScheduleRequest>();
        allMetrics.add(component.getNumericMetricSchedule());
        allMetrics.add(component.getTraitMetricSchedule());
        allMetrics.add(component.getCalltimeMetricSchedule());

        // 123 is too small - the minimum is 60000 so the runnable will bump up our interval to that min value
        MeasurementCollectorRunnable runnable = new MeasurementCollectorRunnable(component, 0L, 123L, null,
            this.threadPool.getExecutor());

        runnable.getLastValues(new MeasurementReport(), allMetrics); // prime the pump so we begin collecting everything immediately
        runnable.start();
        Thread.sleep(1000L);

        MeasurementReport report = new MeasurementReport();
        runnable.getLastValues(report, allMetrics);
        assert 1000L == report.getCollectionTime();
        assert report.getCallTimeData().size() == 1;
        assert report.getNumericData().size() == 1;
        MeasurementDataNumeric nextNumeric = report.getNumericData().iterator().next();
        assert nextNumeric.getValue() == (double) 1;
        assert report.getTraitData().size() == 1;
        MeasurementDataTrait nextTrait = report.getTraitData().iterator().next();
        assert nextTrait.getValue().equals("1");

        // wait for the next collection interval to occur
        // don't wait forever, if this fails, we need a way to break the loop.  Don't want longer than 75 seconds
        int loopCount = 0;
        report = new MeasurementReport();
        report.setCollectionTime(1000L);
        System.out.print("CollectorThreadPoolTest.testMultipleCollectionPeriods() waiting");
        while (loopCount++ < 75 && report.getCollectionTime() == 1000L) {
            System.out.print('.');
            Thread.sleep(1000L);
            runnable.getLastValues(report, allMetrics);
        }
        System.out.println("done.");

        assert 1001L == report.getCollectionTime();
        assert report.getCallTimeData().size() == 1 : report.getCallTimeData();
        assert report.getNumericData().size() == 1 : report.getNumericData();
        nextNumeric = report.getNumericData().iterator().next();
        assert nextNumeric.getValue() == (double) 2 : nextNumeric;
        assert report.getTraitData().size() == 1 : report.getTraitData();
        nextTrait = report.getTraitData().iterator().next();
        assert nextTrait.getValue().equals("2") : nextTrait;
    }

    public void testLotsOfDataMeasurements() throws Exception {
        TestLotsOfDataMeasurementFacet component = new TestLotsOfDataMeasurementFacet();
        Set<MeasurementScheduleRequest> allMetrics = new HashSet<MeasurementScheduleRequest>();
        allMetrics.add(component.getNumericMetricSchedule());
        allMetrics.add(component.getTraitMetricSchedule());
        allMetrics.add(component.getCalltimeMetricSchedule());

        MeasurementCollectorRunnable runnable = new MeasurementCollectorRunnable(component, 0L, 123L, null,
            this.threadPool.getExecutor());

        runnable.getLastValues(new MeasurementReport(), allMetrics); // prime the pump so we begin collecting everything immediately
        runnable.start();
        Thread.sleep(1000L);

        // this tests to make sure we can have multiple data points for a single metric
        MeasurementReport report = new MeasurementReport();
        runnable.getLastValues(report, allMetrics);
        assert 1000L == report.getCollectionTime();
        assert report.getCallTimeData().size() == 1;
        CallTimeData nextCalltime = report.getCallTimeData().iterator().next();
        assert nextCalltime.getValues().size() == 2;
        assert report.getNumericData().size() == 3;
        MeasurementDataNumeric nextNumeric = report.getNumericData().iterator().next();
        assert nextNumeric.getValue() == (double) 1;
        assert report.getTraitData().size() == 3;
        MeasurementDataTrait nextTrait = report.getTraitData().iterator().next();
        assert nextTrait.getValue().equals("1");

        // make sure all the data has been flushed now
        report = new MeasurementReport();
        runnable.getLastValues(report, allMetrics);
        assert 1000L == report.getCollectionTime();
        assert report.getNumericData().isEmpty();
        assert report.getTraitData().isEmpty();
        assert report.getCallTimeData().isEmpty();
    }

    protected class TestAvailabilityFacet implements AvailabilityFacet {
        private AvailabilityType[] avail;

        public TestAvailabilityFacet(AvailabilityType[] avail) {
            this.avail = avail;
        }

        public AvailabilityType getAvailability() {
            log("~~~~~~~~~~" + new java.util.Date() + " == " + this.avail[0]);
            return this.avail[0];
        }
    }

    protected class TestMeasumentFacet implements MeasurementFacet {

        @Override
        public void getValues(MeasurementReport report,
                Set<MeasurementScheduleRequest> metrics) throws Exception {
            log("TestMeasumentFacet.getValues " + metrics);
            report.setCollectionTime(42L);
            for (MeasurementScheduleRequest request : metrics) {
                report.addData(new MeasurementDataTrait(0, request, "good times"));
            }
        }

    }

    protected class TestMultipleMeasumentFacet implements MeasurementFacet {
        public static final String NUMERIC_METRIC_NAME = "numericMetric";
        public static final String TRAIT_METRIC_NAME = "traitMetric";

        public MeasurementScheduleRequest getNumericMetricSchedule() {
            return new MeasurementScheduleRequest(1, NUMERIC_METRIC_NAME, 30000L, true, DataType.MEASUREMENT);
        }

        public MeasurementScheduleRequest getTraitMetricSchedule() {
            return new MeasurementScheduleRequest(2, TRAIT_METRIC_NAME, 30000L, true, DataType.TRAIT);
        }

        public MeasurementScheduleRequest getCalltimeMetricSchedule() {
            return new MeasurementScheduleRequest(3, "calltime", 30000L, true, DataType.CALLTIME);
        }

        public long collectionTime = 999;
        public int counter = 0;

        @Override
        public void getValues(MeasurementReport report, Set<MeasurementScheduleRequest> metrics) throws Exception {
            counter++;
            collectionTime++;
            log("TestMultipleMeasumentFacet.getValues [" + counter + "]: " + metrics);
            report.setCollectionTime(collectionTime);
            for (MeasurementScheduleRequest request : metrics) {
                if (request.getDataType() == DataType.CALLTIME) {
                    CallTimeData data = new CallTimeData(request);
                    data.addCallData("dest1", new Date(collectionTime), (long) counter);
                    report.addData(data);
                } else if (request.getName().equals(NUMERIC_METRIC_NAME)) {
                    report.addData(new MeasurementDataNumeric(collectionTime, request, (double) counter));
                } else if (request.getName().equals(TRAIT_METRIC_NAME)) {
                    report.addData(new MeasurementDataTrait(collectionTime, request, "" + counter));
                } else {
                    log.info("bad test - unknown metric: " + request);
                    throw new IllegalStateException("bad test - unknown metric: " + request);
                }
            }
        }
    }

    protected class TestLotsOfDataMeasurementFacet extends TestMultipleMeasumentFacet {
        @Override
        public void getValues(MeasurementReport report, Set<MeasurementScheduleRequest> metrics) throws Exception {
            counter++;
            collectionTime++;
            log("TestLotsOfDataMeasurementFacet.getValues [" + counter + "]: " + metrics);
            report.setCollectionTime(collectionTime);
            for (MeasurementScheduleRequest request : metrics) {
                if (request.getDataType() == DataType.CALLTIME) {
                    CallTimeData data = new CallTimeData(request);
                    data.addCallData("dest1", new Date(collectionTime - 900), (long) counter);
                    data.addCallData("dest2", new Date(collectionTime), (long) counter);
                    report.addData(data);
                } else if (request.getName().equals(NUMERIC_METRIC_NAME)) {
                    report.addData(new MeasurementDataNumeric(collectionTime - 900, request, (double) counter));
                    report.addData(new MeasurementDataNumeric(collectionTime - 800, request, (double) counter));
                    report.addData(new MeasurementDataNumeric(collectionTime, request, (double) counter));
                } else if (request.getName().equals(TRAIT_METRIC_NAME)) {
                    report.addData(new MeasurementDataTrait(collectionTime - 900, request, "" + counter));
                    report.addData(new MeasurementDataTrait(collectionTime - 800, request, "" + counter));
                    report.addData(new MeasurementDataTrait(collectionTime, request, "" + counter));
                } else {
                    log.info("bad test - unknown metric: " + request);
                    throw new IllegalStateException("bad test - unknown metric: " + request);
                }
            }
        }
    }
}
