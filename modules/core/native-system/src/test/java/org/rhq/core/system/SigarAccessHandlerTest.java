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

package org.rhq.core.system;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import java.lang.reflect.Method;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.hyperic.sigar.Sigar;
import org.hyperic.sigar.SigarProxy;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * @author Thomas Segismont
 */
@Test(singleThreaded = true)
public class SigarAccessHandlerTest {
    private static final int THREAD_POOL_SIZE = 100;

    private Method getSwapMethod;
    private Method getFileSystemMapMethod;
    private Sigar sigarMock;
    private ExecutorService executorService;
    private SigarAccessHandler sigarAccessHandler;

    @BeforeClass
    private void setFields() throws Exception {
        getSwapMethod = SigarProxy.class.getMethod("getSwap", new Class[0]);
        getFileSystemMapMethod = SigarProxy.class.getMethod("getFileSystemMap", new Class[0]);
        sigarMock = mock(Sigar.class);
        // Wait 15 seconds on call to getSwap
        when(sigarMock.getSwap()).then(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                Thread.sleep(1000 * 15);
                return null;
            }
        });
    }

    @BeforeMethod
    public void setUp() throws Exception {
        sigarAccessHandler = new SigarAccessHandler(new SigarAccessHandler.SigarFactory() {
            @Override
            public Sigar createSigarInstance() {
                return sigarMock;
            }
        });
        executorService = Executors.newFixedThreadPool(THREAD_POOL_SIZE);
    }

    @AfterMethod
    public void tearDown() throws Exception {
        sigarAccessHandler.close();
        executorService.shutdownNow();

    }

    @Test(timeOut = 1000 * 30)
    public void testOnDemandSigarInstanceCreation() throws Throwable {
        // Start concurrent invocations of the long running getSwap method
        for (int i = 0; i < 10; i++) {
            executorService.submit(new Runnable() {
                @Override
                public void run() {
                    try {
                        sigarAccessHandler.invoke(null, getSwapMethod, new Object[0]);
                    } catch (Throwable ignore) {
                    }
                }
            });
        }
        Thread.sleep(1000 * 8);

        assertEquals(sigarAccessHandler.localSigarInstancesCount(), 9);
    }

    @Test(timeOut = 1000 * 30)
    public void testSigarInstanceDestruction() throws Throwable {
        // Start concurrent invocations of the short running getFileSystemMapMethod method and an invocation of the long
        // running getSwap method
        for (int i = 0; i < 10; i++) {
            if (i == 0) {
                executorService.submit(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            sigarAccessHandler.invoke(null, getSwapMethod, new Object[0]);
                        } catch (Throwable ignore) {
                        }
                    }
                });
            } else {
                executorService.submit(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            sigarAccessHandler.invoke(null, getFileSystemMapMethod, new Object[0]);
                        } catch (Throwable ignore) {
                        }
                    }
                });
            }
        }
        Thread.sleep(1000 * 8);

        assertEquals(sigarAccessHandler.localSigarInstancesCount(), 0);
    }

    @Test(timeOut = 1000 * 30)
    public void testMaxSigarInstanceCreation() throws Throwable {
        // Start concurrent invocations of the long running getSwap method
        List<Future<Throwable>> futures = new LinkedList<Future<Throwable>>();
        for (int i = 0; i < 60; i++) {
            futures.add(executorService.submit(new Callable<Throwable>() {
                @Override
                public Throwable call() throws Exception {
                    try {
                        sigarAccessHandler.invoke(null, getSwapMethod, new Object[0]);
                    } catch (Throwable throwable) {
                        return throwable;
                    }
                    return null;
                }
            }));
        }
        Thread.sleep(1000 * 8);

        assertEquals(sigarAccessHandler.localSigarInstancesCount(), 50);
        int failedCallsCount = 0;
        for (Future<Throwable> future : futures) {
            Throwable throwable = future.get();
            if (throwable != null) {
                failedCallsCount++;
                assertTrue(throwable instanceof RuntimeException);
                assertEquals(throwable.getMessage(), "Too many Sigar instances created");
            }
        }
        assertEquals(failedCallsCount, 9);
    }
}
