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
package org.rhq.core.pc.availability;

import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.pluginapi.availability.AvailabilityCollectorRunnable;
import org.rhq.core.pluginapi.availability.AvailabilityFacet;

@Test
public class AvailabilityCollectorTest {

    private AvailabilityCollectorThreadPool threadPool;

    @BeforeTest
    public void beforeTest() {
        threadPool = new AvailabilityCollectorThreadPool();
        threadPool.initialize();
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
            this.threadPool);
        runnable.start();
        Thread.sleep(1000L);
        assert AvailabilityType.UP == runnable.getLastKnownAvailability();

        // availability collector cannot allow for collections faster than 60s. So we can't have tests faster than this.
        // set this if-check to true to fully test the collector (which takes a couple mins of wait time to complete)
        if (System.getProperty("AvailabilityCollectorTest.longtest", "false").equals("true")) {
            avail[0] = AvailabilityType.DOWN;
            System.out.println("~~~~~~~~~~sleeping for 60 secs");
            Thread.sleep(60100L);
            assert AvailabilityType.DOWN == runnable.getLastKnownAvailability() : "Collector should have seen the change";

            runnable.stop();
            avail[0] = AvailabilityType.UP;
            System.out.println("~~~~~~~~~~sleeping for 60 secs");
            Thread.sleep(60100L);
            assert AvailabilityType.DOWN == runnable.getLastKnownAvailability() : "Collector should have stopped and not see the change";
        }
    }

    protected class TestAvailabilityFacet implements AvailabilityFacet {
        private AvailabilityType[] avail;

        public TestAvailabilityFacet(AvailabilityType[] avail) {
            this.avail = avail;
        }

        public AvailabilityType getAvailability() {
            System.out.println("~~~~~~~~~~" + new java.util.Date() + " == " + this.avail[0]);
            return this.avail[0];
        }
    }
}
