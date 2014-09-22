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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import org.rhq.core.domain.measurement.Availability;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.pluginapi.availability.AvailabilityFacet;
import org.rhq.core.pluginapi.inventory.ResourceComponent;

@Test
public class AvailabilityProxyConcurrencyTest implements AvailabilityFacet {

    @BeforeMethod
    @AfterMethod
    public void clearInterrupts() {
        // make sure we don't leave interrupt flag set, if somehow the test set it
        Thread.interrupted();
    }

    private AtomicInteger numberOfFacetCalls = new AtomicInteger(-1);

    public void testConcurrentAvailChecks() throws Exception {
        Thread.interrupted(); // clear any hanging around interrupt status

        final ExecutorService executor = Executors.newCachedThreadPool();
        try {
            // mock out the resource container so calls to the component get our impl of getAvailability()
            ResourceComponent<?> resourceComponent = Mockito.mock(ResourceComponent.class);
            Mockito.when(resourceComponent.getAvailability()).thenAnswer(new Answer<AvailabilityType>() {
                public AvailabilityType answer(InvocationOnMock invocation) throws Throwable {
                    return AvailabilityProxyConcurrencyTest.this.getAvailability();
                }
            });
            final ResourceContainer resourceContainer = Mockito.mock(ResourceContainer.class);
            Mockito.when(resourceContainer.getResourceClassLoader()).thenReturn(getClass().getClassLoader());
            Mockito.when(resourceContainer.getResourceComponent()).thenReturn(resourceComponent);

            // our one proxy we want to call concurrently
            final AvailabilityProxy ap = new TestAvailabilityProxy(resourceContainer);

            // make sure our mock object uses our own thread pool when submitting the task
            Mockito.when(resourceContainer.submitAvailabilityCheck(ap)).thenAnswer(
                new Answer<Future<AvailabilityType>>() {
                    public Future<AvailabilityType> answer(InvocationOnMock invocation) throws Throwable {
                        return executor.submit(ap);
                    }
                });

            // prime the pump by getting the first avail synchronously
            System.out.println("~~~AVAILABILITY PROXY CALL #" + 0 + " at " + new Date());
            AvailabilityType firstAvail = ap.getAvailability();
            assert UP.equals(firstAvail) : "Can't even get our first avail correctly: " + firstAvail;
            Mockito.when(resourceContainer.getAvailability()).thenReturn(
                new Availability(new Resource(1), AvailabilityType.UP)); // last avail is UP and will stay as UP

            // make several calls to availProxy.getAvailability() in quick succession
            final int numCalls = 15;
            final Hashtable<String, AvailabilityType> availResults = new Hashtable<String, AvailabilityType>(numCalls);
            final Hashtable<String, Date> dateResults = new Hashtable<String, Date>(numCalls);
            final Hashtable<String, Throwable> throwableResults = new Hashtable<String, Throwable>(numCalls);

            // this will count how many times the proxy actually calls the facet (i.e. component's) getAvail method
            numberOfFacetCalls.set(0);

            // release the hounds!
            for (int i = 1; i <= numCalls; i++) {
                try {
                    // space out the calls slightly to allow some async invocations to complete, giving us a mix of
                    // sync and async completions
                    try {
                        Thread.sleep(25);
                    } catch (InterruptedException e) {
                        //
                    }

                    System.out.println("~~~AVAILABILITY PROXY CALL #" + i + " at " + new Date());
                    AvailabilityType availCheck = ap.getAvailability();
                    // if the avail check is in progress, defer to our last known avail (which should be UP, due to
                    // our first call, and the simulating mock)
                    availCheck = (availCheck == AvailabilityType.UNKNOWN) ? resourceContainer.getAvailability()
                        .getAvailabilityType() : availCheck;
                    availResults.put("Call-" + i, availCheck);
                } catch (Exception e) {
                    throwableResults.put(Thread.currentThread().getName(), e);
                } finally {
                    dateResults.put("Call-" + i, new Date());
                }
            }

            System.out.println("~~~THREADS FINISHED AT: " + new Date());
            System.out.println("~~~THREAD FINISH TIMES: " + dateResults);
            System.out.println("~~~THREADS WITH EXCEPTIONS: " + throwableResults);

            // now make sure all of them returns UP
            assert availResults.size() == numCalls : "Failed, bad threads: availResults = " + availResults;
            for (AvailabilityType availtype : availResults.values()) {
                assert availtype.equals(UP) : "Failed, bad avail: availResults = " + availResults;
            }

            // make sure we actually tested the code we need to test - we should not be making
            // individual facet calls for each request because of the quick succession of calls
            // and the facet sleeps. The proxy should return the last avail rather
            // than requiring a new facet call, in some cases. The first 3 are always fast (see below
            // impl of facet's getAvailability().
            assert (numberOfFacetCalls.get()) > 3 : numberOfFacetCalls;
            assert (numberOfFacetCalls.get()) < numCalls : numberOfFacetCalls;
        } finally {
            executor.shutdownNow();
        }
    }

    @Override
    public synchronized AvailabilityType getAvailability() {
        final int facetCall = numberOfFacetCalls.incrementAndGet();
        try {
            System.out.println("~~~AVAILABILITY FACET CALL #" + facetCall + " at " + new Date());
            // return quickly for the first request, we want it to establish a lastKnownAvail of UP
            if (facetCall > 0) {
                // make a few fast enough to complete synchronously and other need to finish async
                Thread.sleep((0 == (facetCall % 3)) ? 400 : 10);
            }
        } catch (Exception e) {
            System.out.println("~~~AVAILABILITY SLEEP WAS ABORTED FOR FACET CALL # " + facetCall + ": " + e);
        }
        return UP;
    }

    // for our test, we want to ensure the sync avail check doesn't time out - so increase the timeout limit to a
    // value > numThreads * getAvail's sleep time (at time of writing 15threads * 350ms = 5.25s).
    private class TestAvailabilityProxy extends AvailabilityProxy {
        public TestAvailabilityProxy(ResourceContainer rc) {
            super(rc);
        }

        @Override
        protected long getSyncTimeout() {
            return 250L;
        }
    }
}