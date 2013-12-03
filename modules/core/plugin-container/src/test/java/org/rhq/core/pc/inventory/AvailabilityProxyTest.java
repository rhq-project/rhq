/*
 * RHQ Management Platform
 * Copyright (C) 2005-2013 Red Hat, Inc.
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
 * along with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA
 */
package org.rhq.core.pc.inventory;

import static org.rhq.core.domain.measurement.AvailabilityType.DOWN;
import static org.rhq.core.domain.measurement.AvailabilityType.UP;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.fail;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.testng.annotations.Test;

import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.pluginapi.availability.AvailabilityFacet;

/**
 * @author Elias Ross
 */
@Test
public class AvailabilityProxyTest implements AvailabilityFacet {

    private final Log LOG = LogFactory.getLog(AvailabilityProxyTest.class);
    private volatile int timeout = 1;
    private AvailabilityType returnedAvail = UP;
    private final ExecutorService executor = Executors.newCachedThreadPool();

    /**
     * Run a test. Note this may not be 100% reliable, as it depends on thread execution to
     * happen according to our sleep schedule...
     */
    public void test() throws InterruptedException {
        TestAvailabilityProxy ap = new TestAvailabilityProxy(this, executor, getClass().getClassLoader());
        LOG.debug("proxy " + ap);

        assertEquals("should be up", UP, ap.getAvailability()); // waits 1ms and returns synchronously
        timeout = 1200;
        assertEquals("should be down", DOWN, ap.getAvailability()); // waits 1s and times out
        Thread.sleep(300); // now waited total of 1s + .3s = 1.3 sec > 1.2s
        assertEquals("should be up now", UP, ap.getAvailability()); // waits 1s and returns last reported value (UP)

        ap.setAsyncTimeout(1020L);
        Thread.sleep(50); // waited 1.050 seconds
        try {
            ap.getAvailability(); // this submits another which we need to let finish
            fail("should timeout 1020, waited 1050");
        } catch (TimeoutException e) {
        }
        // wait for the last submit to return
        Thread.sleep(1210);

        LOG.debug("proxy " + ap);

        // try disabling sync checks
        // - start returning DOWN avail in order to perform a sync disable and then re-enable
        // - go back to default async timeout, we don't want it to trigger anymore
        // short timeout but longer than the sync timeout to force several sync timeouts
        returnedAvail = DOWN;
        ap.setAsyncTimeout(null);
        timeout = 75;
        ap.setSyncTimeout(50L);

        while (!ap.isSyncDisabled()) {
            ap.getAvailability();
            Thread.sleep(50L);
        }

        // go back to returning UP so we can re-enable sync checking
        // make the sync check a half second so we can prove that sync checking is not happening
        returnedAvail = UP;
        timeout = 500;
        ap.setSyncTimeout(500L);
        long start = System.currentTimeMillis();
        assertEquals("should be DOWN", DOWN, ap.getAvailability());
        assert System.currentTimeMillis() - start < 100 : "Should have been fast, returning old avail";
        // wait for the last submit to return
        Thread.sleep(510);

        // check for re-enable sync checks
        assertEquals("should be UP", UP, ap.getAvailability());
        assertEquals("should be enabled", false, ap.isSyncDisabled());
        // wait for the last submit to return
        Thread.sleep(510);

        // test interrupt handling
        LOG.debug("interrupt this thread");
        Thread.currentThread().interrupt();
        assertEquals("cancellation", AvailabilityType.UNKNOWN, ap.getAvailability());
        assertEquals(true, Thread.currentThread().isInterrupted());
    }

    @Override
    public synchronized AvailabilityType getAvailability() {
        try {
            LOG.debug("sleep " + timeout);
            Thread.sleep(timeout);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        LOG.debug("return " + returnedAvail.getName());
        return returnedAvail;
    }

    private class TestAvailabilityProxy extends AvailabilityProxy {

        private Long asyncTimeout = null;
        private Long syncTimeout = null;

        public TestAvailabilityProxy(AvailabilityFacet resourceComponent, ExecutorService executor,
            ClassLoader classLoader) {
            super(resourceComponent, executor, classLoader);
        }

        @Override
        public AvailabilityType getAvailability() {
            // System.out.println("DevDebug  0 [" + System.currentTimeMillis() + "] getAvail() timeout=[" + timeout + "], syncTimeout=[" + syncTimeout + "], asyncTimeout=[" + asyncTimeout + "]");
            return super.getAvailability();
        }

        public void setAsyncTimeout(Long asyncTimeout) {
            this.asyncTimeout = asyncTimeout;
        }

        public void setSyncTimeout(Long syncTimeout) {
            this.syncTimeout = syncTimeout;
        }

        @Override
        protected long getSyncTimeout() {
            return null == syncTimeout ? super.getSyncTimeout() : syncTimeout;
        }

        @Override
        protected long getAsyncTimeout() {
            return null == asyncTimeout ? super.getAsyncTimeout() : asyncTimeout;
        }
    }
}
