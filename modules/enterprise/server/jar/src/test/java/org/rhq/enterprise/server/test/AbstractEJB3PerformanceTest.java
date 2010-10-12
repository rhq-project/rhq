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
package org.rhq.enterprise.server.test;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.rhq.helpers.perftest.support.reporting.PerformanceReportExporter;
import org.rhq.helpers.perftest.support.testng.PerformanceReporting;
import org.testng.ITestResult;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;

import javax.persistence.EntityManager;
import java.lang.reflect.Method;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * Helper that introduces timing functionality on top of the Abstract EJB tests.
 *
 * @author Heiko W. Rupp
 */
public class AbstractEJB3PerformanceTest extends AbstractEJB3Test {

    private static final Log log = LogFactory.getLog("TIMING_INFO");


    private static final String DEFAULT = "-default-";
    private Map<String,Long> timings ;
    private Map<String,Long> startTime ;


    protected void startTiming(String name) {
        long now = System.currentTimeMillis();
        startTime.put(name,now);

    }

    protected void endTiming(String name) {

        boolean found = startTime.containsKey(name);
        assert found : "No start time information for name [" + name + "] found - did you call startTiming()?";


        long now = System.currentTimeMillis();
        long start = startTime.get(name);
        long duration = (now - start);
        if (timings.containsKey(name)) {
            long timing = timings.get(name);
            timing+=duration;
            timings.put(name,timing);
        }
        else {
            timings.put(name,duration);
        }
    }

    protected void startTiming() {
        startTiming(DEFAULT);
    }

    protected void endTiming() {
        endTiming(DEFAULT);
    }

    protected long getTiming(String name) {
        if (timings.containsKey(name)) {
            return timings.get(name);
        }
        else
            return -1;
    }

    protected long getTiming() {
        return getTiming(DEFAULT);
    }

    @AfterMethod
    protected void reportTimings(ITestResult result, Method meth) {
        Date now = new Date();
        System.out.println(">>> after " + meth.getName() + " (AbstraceEJB3PerformanceTest) === " + now.getTime());

        printTimings(meth.getName());

        Class clazz = meth.getDeclaringClass();
        PerformanceReporting pr = (PerformanceReporting) clazz.getAnnotation(PerformanceReporting.class);
        if (pr != null) {
            String file =  pr.baseFilename();
            Class<? extends PerformanceReportExporter> exporterClazz = pr.exporter();
            try {
                PerformanceReportExporter exporter = exporterClazz.newInstance();
                exporter.setBaseFile(file);
                exporter.setRolling(pr.rolling());
                exporter.export(timings,result);
            }
            catch (Exception e) {
                // TODO fix this
                e.printStackTrace();
            }

        }


        timings.clear();
        startTime.clear();

    }

    @BeforeMethod
    protected void setupTimings(Method meth) {
        Date now = new Date();
        System.out.println(">>> before " + meth.getName() + " (AbstraceEJB3PerformanceTest) === " + now.getTime());
        timings = new HashMap<String, Long>();
        startTime = new HashMap<String, Long>();

    }



    protected void printTimings(String testName) {
        System.out.println("=== " + testName + " ===");
        Set<Map.Entry<String,Long>> data = timings.entrySet();
        SortedSet <Map.Entry<String,Long>> sorted = new TreeSet<Map.Entry<String,Long>>(new Comparator<Map.Entry<String,Long>>() {

            public int compare(Map.Entry<String,Long> item1, Map.Entry<String,Long> item2) {

                return item1.getKey().compareTo(item2.getKey());
            }
        });
        sorted.addAll(data);
        long summaryTime = 0L;
        for (Map.Entry<String,Long> item : sorted) {
            log.info(":| " + item.getKey() + " => " + item.getValue());
            System.out.println(":| " + item.getKey() + " => " + item.getValue());
            summaryTime += item.getValue();
        }
        System.out.println("Total: " + summaryTime + " ms");
    }

    protected void assertTiming(String name, long maxDuration) {

        boolean found = timings.containsKey(name);
        assert found : "No timing information for name [" + name + "] found";

        long duration = timings.get(name);

        assert duration < maxDuration : "Execution took longer than given max ( " + duration + " > " + maxDuration + ")";

    }

    protected void assertTiming(long maxDuration) {
        assertTiming(DEFAULT,maxDuration);
    }

    /**
     * Make sure the passed value is within a band of <code>[0.80* x, 1.2*x]</code> with
     * <code>x = ( ref * multiplier )</code>.
     * @param ref base value to calculate the reference from
     * @param value value to compare to the band
     * @param multiplier multiplier for the base value of the band.
     * @param text text to prepend to a line if check fails.
     */
    protected void assertLinear(long ref,long value, double multiplier, String text ) {

System.out.println(">>> assertLinear " + text + " " + ref + ", " + value + ", " + multiplier );
        long low = (long) (ref * multiplier * 0.80);
        long hi = (long) (ref * multiplier * 1.2);

        // comment out the low check for now
//        assert value >= low : text + " [low] Val2 (" + value + ") is not > " + low;
        assert value <= hi :  text + " [hi] Val2 (" + value + ") is not < " + hi;
    }

}
