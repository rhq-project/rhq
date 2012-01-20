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

import java.util.HashSet;
import java.util.Set;

import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.domain.measurement.DataType;
import org.rhq.core.domain.measurement.MeasurementDataTrait;
import org.rhq.core.domain.measurement.MeasurementReport;
import org.rhq.core.domain.measurement.MeasurementScheduleRequest;
import org.rhq.core.pc.CollectorThreadPool;
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
        MeasurementCollectorRunnable runnable = new MeasurementCollectorRunnable(component, 500L, null,
                this.threadPool.getExecutor());
        runnable.start();
        Set<MeasurementScheduleRequest> metrics = new HashSet();
        metrics.add(new MeasurementScheduleRequest(0, "name", 0, true, DataType.TRAIT));
        MeasurementReport report = new MeasurementReport();
        runnable.getLastValues(report, metrics);
        assert 0 == report.getCollectionTime();
        assert report.getTraitData().isEmpty();

        log("sleeping");
        Thread.sleep(1000L);

        report = new MeasurementReport();
        runnable.getLastValues(report, metrics);
        assert 42 == report.getCollectionTime();
        assert !report.getTraitData().isEmpty();
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
            log("getValues " + metrics);
            report.setCollectionTime(42L);
            for (MeasurementScheduleRequest request : metrics) {
                report.addData(new MeasurementDataTrait(0, request, "good times"));
            }
        }

    }
}
