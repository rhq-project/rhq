/*
 * RHQ Management Platform
 * Copyright (C) 2005-2010 Red Hat, Inc.
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
package org.rhq.enterprise.server.performance.test;

import org.rhq.enterprise.server.test.AbstractEJB3PerformanceTest;
import org.rhq.helpers.perftest.support.testng.DatabaseSetupInterceptor;
import org.rhq.helpers.perftest.support.testng.DatabaseState;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

/**
 * Performance test the availabilities subsystem
 *
 * @author Heiko W. Rupp
 * @author Lukas Krejci
 */
@Test(groups = "PERF")
@Listeners({ DatabaseSetupInterceptor.class })
//@JdbcConnectionProviderMethod("getConnection") //defined in AbstractEJB3Test
public class AvailabilityInsertPurgeTest extends AbstractEJB3PerformanceTest {

    @DatabaseState(url = "perftest/AvailabilityInsertPurgeTest-testOne-data.xml.zip", dbVersion="2.94")
    public void testOne() throws Exception {
        startTiming();

        Thread.sleep(1234);

        endTiming();

        commitTimings();

    }
}
