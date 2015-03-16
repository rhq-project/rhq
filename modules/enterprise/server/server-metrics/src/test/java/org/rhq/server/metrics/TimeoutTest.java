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

import org.rhq.core.domain.measurement.MeasurementDataNumeric;

public class TimeoutTest extends CassandraIntegrationTest {

    private final Log log = LogFactory.getLog(TimeoutTest.class);

//    @Test
    public void generateTimeout() throws Exception {
        MetricsConfiguration configuration = new MetricsConfiguration();

        MetricsServer metricsServer = new MetricsServer();
        metricsServer.setConfiguration(configuration);

        DateTimeService dateTimeService = new DateTimeService();
        dateTimeService.setConfiguration(configuration);
        metricsServer.setDateTimeService(dateTimeService);

        MetricsDAO dao = new MetricsDAO(new StorageSession(session), configuration);
        metricsServer.setDAO(dao);

        long time = hour0().getMillis();
        Set<MeasurementDataNumeric> data1 = new HashSet<MeasurementDataNumeric>();
        for (int i = 0; i < 50000; ++i) {
            data1.add(new MeasurementDataNumeric(time, i, (double) i));
        }

//        Set<MeasurementDataNumeric> data2 = new HashSet<MeasurementDataNumeric>();
//        for (int i = 0; i < 50000; ++i) {
//            data2.add(new MeasurementDataNumeric(time, i + data1.size(), (double) i));
//        }

        WaitForRawInserts waitForRawInserts1 = new WaitForRawInserts(data1.size());
//        WaitForRawInserts waitForRawInserts2 = new WaitForRawInserts(data2.size());
        long start = System.currentTimeMillis();
        metricsServer.addNumericData(data1, waitForRawInserts1);
        waitForRawInserts1.await("Failed to insert raw metrics");

//        log.info("Sleep before second round of inserts...");
//        Thread.sleep(3000);
//
//        metricsServer.addNumericData(data2, waitForRawInserts2);
        long end = System.currentTimeMillis();

        log.info("Inserted " + data1.size() + " raw metrics in " + (end - start) + " ms");
    }
}
