/*
 *
 *  * RHQ Management Platform
 *  * Copyright (C) 2005-2012 Red Hat, Inc.
 *  * All rights reserved.
 *  *
 *  * This program is free software; you can redistribute it and/or modify
 *  * it under the terms of the GNU General Public License, version 2, as
 *  * published by the Free Software Foundation, and/or the GNU Lesser
 *  * General Public License, version 2.1, also as published by the Free
 *  * Software Foundation.
 *  *
 *  * This program is distributed in the hope that it will be useful,
 *  * but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  * GNU General Public License and the GNU Lesser General Public License
 *  * for more details.
 *  *
 *  * You should have received a copy of the GNU General Public License
 *  * and the GNU Lesser General Public License along with this program;
 *  * if not, write to the Free Software Foundation, Inc.,
 *  * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 *
 */

package org.rhq.server.metrics;

import static org.testng.Assert.fail;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CountDownLatch;

import com.google.common.base.Throwables;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.testng.annotations.Test;

import org.rhq.core.domain.measurement.MeasurementDataNumeric;

public class TimeoutTest extends CassandraIntegrationTest {

    private final Log log = LogFactory.getLog(TimeoutTest.class);

    @Test
    public void generateTimeout() throws Exception {
        MetricsConfiguration configuration = new MetricsConfiguration();

        MetricsServer metricsServer = new MetricsServer();
        metricsServer.setSession(session);
        metricsServer.setConfiguration(configuration);

        DateTimeService dateTimeService = new DateTimeService();
        dateTimeService.setConfiguration(configuration);
        metricsServer.setDateTimeService(dateTimeService);

        MetricsDAO dao = new MetricsDAO(session, configuration);
        metricsServer.setDAO(dao);

        long time = hour0().getMillis();
        Set<MeasurementDataNumeric> data = new HashSet<MeasurementDataNumeric>();
        for (int i = 0; i < 10000; ++i) {
            data.add(new MeasurementDataNumeric(time, i, (double) i));
        }

        WaitForRawInserts waitForRawInserts = new WaitForRawInserts(data.size());
        long start = System.currentTimeMillis();
        metricsServer.addNumericData(data, waitForRawInserts);

        waitForRawInserts.await("Failed to insert raw metrics");
        long end = System.currentTimeMillis();

        log.info("Inserted " + data.size() + " raw metrics in " + (end - start) + " ms");
    }

    private static class WaitForRawInserts implements RawDataInsertedCallback {

        private final Log log = LogFactory.getLog(WaitForRawInserts.class);

        private CountDownLatch latch;

        private Throwable throwable;

        public WaitForRawInserts(int numInserts) {
            latch = new CountDownLatch(numInserts);
        }

        @Override
        public void onFinish() {
        }

        @Override
        public void onSuccess(MeasurementDataNumeric measurementDataNumeric) {
            latch.countDown();
        }

        @Override
        public void onFailure(Throwable throwable) {
            latch.countDown();
            this.throwable = throwable;
            log.error("An async operation failed", throwable);
        }

        public void await(String errorMsg) throws InterruptedException {
            latch.await();
            if (throwable != null) {
                fail(errorMsg, Throwables.getRootCause(throwable));
            }
        }
    }

}
