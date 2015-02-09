/*
 * RHQ Management Platform
 * Copyright (C) 2005-2014 Red Hat, Inc.
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

package org.rhq.modules.plugins.jbossas7;

import static org.testng.Assert.assertEquals;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

/**
 * @author Thomas Segismont
 */
@Test(timeOut = 5 * 60 * 1000)
public class PluginStatsTest {

    private PluginStats pluginStats;
    private ExecutorService executorService;
    private int generatorLoop;

    @BeforeTest
    public void setup() throws Exception {
        Constructor<PluginStats> constructor = PluginStats.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        pluginStats = constructor.newInstance();
        executorService = Executors.newCachedThreadPool();
        generatorLoop = 3 * 1000 * 1000; // 3 million loops
    }

    @Test
    public void testMaxRequestTime() throws Exception {
        List<Callable<Long>> generators = new ArrayList<Callable<Long>>();
        for (int i = 0; i < 4; i++) {
            generators.add(newRequestTimeGenerator());
        }
        List<Future<Long>> maxByGeneratorFutures = executorService.invokeAll(generators);
        List<Long> maxByGenerators = new ArrayList<Long>();
        for (Future<Long> maxByGeneratorFuture : maxByGeneratorFutures) {
            maxByGenerators.add(maxByGeneratorFuture.get(5, TimeUnit.MINUTES));
        }
        Collections.sort(maxByGenerators, Collections.reverseOrder());
        Long expectedMaxTime = maxByGenerators.get(0);
        assertEquals(expectedMaxTime, Long.valueOf(pluginStats.getMaxTime()), maxByGenerators.toString());
        assertEquals(0, pluginStats.getMaxTime(), "max time stat should have been reset to 0");
    }

    private Callable<Long> newRequestTimeGenerator() {
        return new Callable<Long>() {
            @Override
            public Long call() throws Exception {
                long max = 0;
                Random random = new Random();
                for (int i = 0; i < generatorLoop; i++) {
                    long time = Math.abs(random.nextInt());
                    if (max < time) {
                        max = time;
                    }
                    pluginStats.addRequestTime(time);
                }
                return max;
            }
        };
    }

    @AfterTest
    public void tearDown() {
        executorService.shutdown();
    }

}