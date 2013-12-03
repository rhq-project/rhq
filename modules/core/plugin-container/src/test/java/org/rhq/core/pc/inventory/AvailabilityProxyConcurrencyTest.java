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

import static org.rhq.core.domain.measurement.AvailabilityType.UP;

import java.util.Date;
import java.util.Hashtable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.testng.annotations.Test;

import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.pluginapi.availability.AvailabilityFacet;

@Test
public class AvailabilityProxyConcurrencyTest implements AvailabilityFacet {

    private AtomicInteger numberOfFacetCalls = new AtomicInteger(-1);

    public void testConcurrentAvailChecks() throws Exception {
        Thread.interrupted(); // clear any hanging around interrupt status

        ExecutorService executor = Executors.newCachedThreadPool();
        try {
            // our one proxy we want to call concurrently
            final AvailabilityProxy ap = new AvailabilityProxy(this, executor, getClass().getClassLoader());

            // prime the pump by getting the first one without problems
            AvailabilityType firstAvail = ap.getAvailability();
            assert UP.equals(firstAvail) : "Can't even get our first avail correctly: " + firstAvail;

            // create several threads that will concurrently call getAvailability
            final int numThreads = 10;
            final Hashtable<String, AvailabilityType> availResults = new Hashtable<String, AvailabilityType>(numThreads);
            final Hashtable<String, Date> dateResults = new Hashtable<String, Date>(numThreads);
            final Hashtable<String, Throwable> throwableResults = new Hashtable<String, Throwable>(numThreads);
            final CountDownLatch startLatch = new CountDownLatch(1);
            final CountDownLatch endLatch = new CountDownLatch(numThreads);
            final Runnable runnable = new Runnable() {
                public void run() {
                    try {
                        startLatch.await();
                        AvailabilityType availCheck = ap.getAvailability();
                        availResults.put(Thread.currentThread().getName(), availCheck);
                    } catch (Exception e) {
                        throwableResults.put(Thread.currentThread().getName(), e);
                    } finally {
                        dateResults.put(Thread.currentThread().getName(), new Date());
                        endLatch.countDown();
                    }
                }
            };
            numberOfFacetCalls.set(0); // this will count how many times the proxy actually calls the facet getAvail method
            for (int i = 0; i < numThreads; i++) {
                Thread t = new Thread(runnable, "t" + i);
                t.start();
            }

            // release the hounds! then wait for them to all finish
            System.out.println("~~~THREADS STARTED AT: " + new Date());
            startLatch.countDown();
            endLatch.await(10000, TimeUnit.SECONDS); // should never take this long
            System.out.println("~~~THREADS FINISHED AT: " + new Date());
            System.out.println("~~~THREAD FINISH TIMES: " + dateResults);
            System.out.println("~~~THREADS WITH EXCEPTIONS: " + throwableResults);

            // now make sure all of them returns UP
            assert availResults.size() == numThreads : "Failed, bad threads: availResults = " + availResults;
            for (AvailabilityType availtype : availResults.values()) {
                assert availtype.equals(UP) : "Failed, bad avail: availResults = " + availResults;
            }

            // make sure we actually tested the code we need to test - we should not be making
            // individual facet calls for each request because we shotgun the requests so fast,
            // and the facet sleeps so long, that the proxy should return the last avail rather
            // than requiring a new facet call.
            assert (numberOfFacetCalls.get()) < numThreads : numberOfFacetCalls;
        } finally {
            executor.shutdownNow();
        }
    }

    @Override
    public synchronized AvailabilityType getAvailability() {
        try {
            System.out.println("~~~AVAILABILITY FACET CALL #" + numberOfFacetCalls.incrementAndGet());
            Thread.sleep(250); // just make it slow enough so a few proxy calls are done concurrently while this method is running
        } catch (Exception e) {
            System.out.println("~~~AVAILABILITY SLEEP WAS ABORTED: " + e);
        }
        return UP;
    }
}