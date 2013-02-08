 /*
 * RHQ Management Platform
 * Copyright (C) 2005-2013 Red Hat, Inc.
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
package org.rhq.core.util;

import org.testng.annotations.Test;

@Test
public class StopWatchTest {
    public void testQuick() {
        StopWatch sw = new StopWatch();
        try {
            Thread.sleep(1100);
        } catch (InterruptedException e) {
        }
        assert sw.getElapsed() > 1000L;
        assert sw.reset() > 1000L : "reset should have returned the elapse time";
        assert sw.getElapsed() < 300L : "timer should have been reset";
    }

    public void testMarkers() {
        StopWatch sw = new StopWatch();

        // B time
        sw.markTimeBegin("B");
        try {
            Thread.sleep(400);
        } catch (InterruptedException e) {
        }
        sw.markTimeEnd("B");

        // unmarked
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
        }

        // A time
        sw.markTimeBegin("A");
        try {
            Thread.sleep(600);
        } catch (InterruptedException e) {
        }
        sw.markTimeEnd("A");
        System.out.println(sw.toString());
    }
}