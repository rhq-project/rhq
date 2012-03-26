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
package org.rhq.core.domain.resource.composite;

import static org.rhq.core.domain.measurement.AvailabilityType.DISABLED;
import static org.rhq.core.domain.measurement.AvailabilityType.DOWN;
import static org.rhq.core.domain.measurement.AvailabilityType.UNKNOWN;
import static org.rhq.core.domain.measurement.AvailabilityType.UP;

import java.util.ArrayList;
import java.util.List;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import org.rhq.core.domain.measurement.Availability;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.domain.resource.Resource;

@Test
public class ResourceAvailabilitySummaryTest {

    private Resource res = new Resource(1);

    public void testLastChange() {
        List<Availability> avails = list(UP, 10, DOWN, 20, UNKNOWN, 30, DISABLED, 40);
        assert new ResourceAvailabilitySummary(avails).getLastChange().getTime() == 40000L;
        avails = list(UNKNOWN, 0);
        assert new ResourceAvailabilitySummary(avails).getLastChange().getTime() == 0L;
        avails = list(UNKNOWN, 1);
        assert new ResourceAvailabilitySummary(avails).getLastChange().getTime() == 1000L;
        avails = list(UP, 5);
        assert new ResourceAvailabilitySummary(avails).getLastChange().getTime() == 5000L;
    }

    public void testCurrent() {
        List<Availability> avails = list(UP, 10, DOWN, 20, UNKNOWN, 30, DISABLED, 40);
        assert new ResourceAvailabilitySummary(avails).getCurrent() == DISABLED;
        avails = list(UP, 10, DOWN, 20, UP, 30, DOWN, 40);
        assert new ResourceAvailabilitySummary(avails).getCurrent() == DOWN;
        avails = list(UNKNOWN, 0);
        assert new ResourceAvailabilitySummary(avails).getCurrent() == UNKNOWN;
        avails = list(UNKNOWN, 1);
        assert new ResourceAvailabilitySummary(avails).getCurrent() == UNKNOWN;
        avails = list(UP, 5);
        assert new ResourceAvailabilitySummary(avails).getCurrent() == UP;
        avails = list(UP, 5, DISABLED, 10);
        assert new ResourceAvailabilitySummary(avails).getCurrent() == DISABLED;
    }

    public void testFailures() {
        List<Availability> avails = list(UNKNOWN, 0);
        assert new ResourceAvailabilitySummary(avails).getFailures() == 0;
        avails = list(UP, 10);
        assert new ResourceAvailabilitySummary(avails).getFailures() == 0;
        avails = list(DOWN, 10);
        assert new ResourceAvailabilitySummary(avails).getFailures() == 1;
        avails = list(DISABLED, 10);
        assert new ResourceAvailabilitySummary(avails).getFailures() == 0;
        avails = list(UNKNOWN, 0, UP, 10, DOWN, 20, DISABLED, 30);
        assert new ResourceAvailabilitySummary(avails).getFailures() == 1;
        avails = list(UNKNOWN, 0, UP, 10, DOWN, 20, UP, 30, DOWN, 40);
        assert new ResourceAvailabilitySummary(avails).getFailures() == 2;
        avails = list(UNKNOWN, 0, UP, 10, DOWN, 20, UP, 30, DOWN, 40, UP, 50);
        assert new ResourceAvailabilitySummary(avails).getFailures() == 2;
        avails = list(UNKNOWN, 0, DISABLED, 10);
        assert new ResourceAvailabilitySummary(avails).getFailures() == 0;

        // try it with the first unknown range starting at non-zero time
        avails = list(UNKNOWN, 1, UP, 10, DOWN, 20, DISABLED, 30);
        assert new ResourceAvailabilitySummary(avails).getFailures() == 1;
        avails = list(UNKNOWN, 1, UP, 10, DOWN, 20, UP, 30, DOWN, 40);
        assert new ResourceAvailabilitySummary(avails).getFailures() == 2;
        avails = list(UNKNOWN, 1, UP, 10, DOWN, 20, UP, 30, DOWN, 40, UP, 50);
        assert new ResourceAvailabilitySummary(avails).getFailures() == 2;
        avails = list(UNKNOWN, 1, DISABLED, 10);
        assert new ResourceAvailabilitySummary(avails).getFailures() == 0;
    }

    public void testDisabled() {
        List<Availability> avails = list(UNKNOWN, 0);
        assert new ResourceAvailabilitySummary(avails).getDisabled() == 0;
        avails = list(UP, 10);
        assert new ResourceAvailabilitySummary(avails).getDisabled() == 0;
        avails = list(DOWN, 10);
        assert new ResourceAvailabilitySummary(avails).getDisabled() == 0;
        avails = list(DISABLED, 10);
        assert new ResourceAvailabilitySummary(avails).getDisabled() == 1;
        avails = list(UNKNOWN, 0, UP, 10, DOWN, 20, DISABLED, 30);
        assert new ResourceAvailabilitySummary(avails).getDisabled() == 1;
        avails = list(UNKNOWN, 0, UP, 10, DOWN, 20, UP, 30, DOWN, 40);
        assert new ResourceAvailabilitySummary(avails).getDisabled() == 0;
        avails = list(UNKNOWN, 0, UP, 10, DISABLED, 20, UP, 30, DISABLED, 40, UP, 50);
        assert new ResourceAvailabilitySummary(avails).getDisabled() == 2;

        // try it with the first unknown range starting at non-zero time
        avails = list(UNKNOWN, 1, UP, 10, DOWN, 20, DISABLED, 30);
        assert new ResourceAvailabilitySummary(avails).getDisabled() == 1;
        avails = list(UNKNOWN, 1, UP, 10, DOWN, 20, UP, 30, DOWN, 40);
        assert new ResourceAvailabilitySummary(avails).getDisabled() == 0;
        avails = list(UNKNOWN, 1, UP, 10, DISABLED, 20, UP, 30, DISABLED, 40, UP, 50);
        assert new ResourceAvailabilitySummary(avails).getDisabled() == 2;
    }

    public void testDownTime() throws InterruptedException {
        List<Availability> avails;

        avails = list(UNKNOWN, 0);
        assert new ResourceAvailabilitySummary(avails).getDownTime() == 0;
        avails = list(UNKNOWN, 1);
        assert new ResourceAvailabilitySummary(avails).getDownTime() == 0;
        avails = list(UP, 600);
        assert new ResourceAvailabilitySummary(avails).getDownTime() == 0;
        avails = list(DISABLED, 600);
        assert new ResourceAvailabilitySummary(avails).getDownTime() == 0;
        avails = list(UNKNOWN, 0, UP, 600, DISABLED, 1200);
        assert new ResourceAvailabilitySummary(avails).getDownTime() == 0;
        avails = list(UNKNOWN, 0, UP, 100, DOWN, 300, DISABLED, 800);
        assert new ResourceAvailabilitySummary(avails).getDownTime() == 500000L; // from 300 to 800
        avails = list(UNKNOWN, 0, UP, 200, DOWN, 400, UP, 800, DOWN, 1600, UP, 3200);
        assert new ResourceAvailabilitySummary(avails).getDownTime() == 2000000L; // 400-800 then 1600-3200 (400+1600=2000s)

        // try it with the first unknown range starting at non-zero time
        avails = list(UNKNOWN, 1, UP, 600, DISABLED, 1200);
        assert new ResourceAvailabilitySummary(avails).getDownTime() == 0;
        avails = list(UNKNOWN, 1, UP, 100, DOWN, 300, DISABLED, 800);
        assert new ResourceAvailabilitySummary(avails).getDownTime() == 500000L; // from 300 to 800
        avails = list(UNKNOWN, 1, UP, 200, DOWN, 400, UP, 800, DOWN, 1600, UP, 3200);
        assert new ResourceAvailabilitySummary(avails).getDownTime() == 2000000L; // 400-800 then 1600-3200 (400+1600=2000s)

        avails = list(DOWN, getPastTime(600));
        assertApproximate(600000L, new ResourceAvailabilitySummary(avails).getDownTime());

        long t1 = getPastTime(3200);
        avails = list(UNKNOWN, 0, UP, t1, DOWN, t1 + 400, UP, t1 + 800, DOWN, t1 + 1600);
        Thread.sleep(1000);
        assertApproximate(2001000L, new ResourceAvailabilitySummary(avails).getDownTime()); //(t1+400)-(t1+800) then t1+1600-now (400+1600+1s sleep)
    }

    public void testUpTime() throws InterruptedException {
        List<Availability> avails;

        avails = list(UNKNOWN, 0);
        assert new ResourceAvailabilitySummary(avails).getUpTime() == 0;
        avails = list(UNKNOWN, 1);
        assert new ResourceAvailabilitySummary(avails).getUpTime() == 0;
        avails = list(DOWN, 600);
        assert new ResourceAvailabilitySummary(avails).getUpTime() == 0;
        avails = list(DISABLED, 600);
        assert new ResourceAvailabilitySummary(avails).getUpTime() == 0;
        avails = list(UNKNOWN, 0, DOWN, 600, DISABLED, 1200);
        assert new ResourceAvailabilitySummary(avails).getUpTime() == 0;
        avails = list(UNKNOWN, 0, DOWN, 100, UP, 300, DISABLED, 800);
        assert new ResourceAvailabilitySummary(avails).getUpTime() == 500000L; // from 300 to 800
        avails = list(UNKNOWN, 0, DOWN, 200, UP, 400, DOWN, 800, UP, 1600, DOWN, 3200);
        assert new ResourceAvailabilitySummary(avails).getUpTime() == 2000000L; // 400-800 then 1600-3200 (400+1600=2000s)

        // try it with the first unknown range starting at non-zero time
        avails = list(UNKNOWN, 1, DOWN, 600, DISABLED, 1200);
        assert new ResourceAvailabilitySummary(avails).getUpTime() == 0;
        avails = list(UNKNOWN, 1, DOWN, 100, UP, 300, DISABLED, 800);
        assert new ResourceAvailabilitySummary(avails).getUpTime() == 500000L; // from 300 to 800
        avails = list(UNKNOWN, 1, DOWN, 200, UP, 400, DOWN, 800, UP, 1600, DOWN, 3200);
        assert new ResourceAvailabilitySummary(avails).getUpTime() == 2000000L; // 400-800 then 1600-3200 (400+1600=2000s)

        avails = list(UP, getPastTime(600));
        assertApproximate(600000L, new ResourceAvailabilitySummary(avails).getUpTime());

        long t1 = getPastTime(3200);
        avails = list(UNKNOWN, 0, DOWN, t1, UP, t1 + 400, DOWN, t1 + 800, UP, t1 + 1600);
        Thread.sleep(1000);
        assertApproximate(2001000L, new ResourceAvailabilitySummary(avails).getUpTime()); //(t1+400)-(t1+800) then t1+1600-now (400+1600+1s sleep)
    }

    public void testDisabledTime() throws InterruptedException {
        List<Availability> avails;

        avails = list(UNKNOWN, 0);
        assert new ResourceAvailabilitySummary(avails).getDisabledTime() == 0;
        avails = list(UNKNOWN, 1);
        assert new ResourceAvailabilitySummary(avails).getDisabledTime() == 0;
        avails = list(DOWN, 600);
        assert new ResourceAvailabilitySummary(avails).getDisabledTime() == 0;
        avails = list(UP, 600);
        assert new ResourceAvailabilitySummary(avails).getDisabledTime() == 0;
        avails = list(UNKNOWN, 0, DOWN, 600, UP, 1200);
        assert new ResourceAvailabilitySummary(avails).getDisabledTime() == 0;
        avails = list(UNKNOWN, 0, DOWN, 100, DISABLED, 300, UP, 800);
        assert new ResourceAvailabilitySummary(avails).getDisabledTime() == 500000L; // from 300 to 800
        avails = list(UNKNOWN, 0, DOWN, 200, DISABLED, 400, DOWN, 800, DISABLED, 1600, UP, 3200);
        assert new ResourceAvailabilitySummary(avails).getDisabledTime() == 2000000L; // 400-800 then 1600-3200 (400+1600=2000s)

        // try it with the first unknown range starting at non-zero time
        avails = list(UNKNOWN, 1, DOWN, 600, UP, 1200);
        assert new ResourceAvailabilitySummary(avails).getDisabledTime() == 0;
        avails = list(UNKNOWN, 1, DOWN, 100, DISABLED, 300, UP, 800);
        assert new ResourceAvailabilitySummary(avails).getDisabledTime() == 500000L; // from 300 to 800
        avails = list(UNKNOWN, 1, DOWN, 200, DISABLED, 400, DOWN, 800, DISABLED, 1600, UP, 3200);
        assert new ResourceAvailabilitySummary(avails).getDisabledTime() == 2000000L; // 400-800 then 1600-3200 (400+1600=2000s)

        avails = list(DISABLED, getPastTime(600));
        assertApproximate(600000L, new ResourceAvailabilitySummary(avails).getDisabledTime());

        long t1 = getPastTime(3200);
        avails = list(UNKNOWN, 0, DOWN, t1, DISABLED, t1 + 400, UP, t1 + 800, DISABLED, t1 + 1600);
        Thread.sleep(1000);
        assertApproximate(2001000L, new ResourceAvailabilitySummary(avails).getDisabledTime()); //(t1+400)-(t1+800) then t1+1600-now (400+1600+1s sleep)
    }

    public void testUnknownTime() throws InterruptedException {
        List<Availability> avails;

        avails = list(UNKNOWN, 0);
        assert new ResourceAvailabilitySummary(avails).getUnknownTime() == 0; // initial unknown range starting at 0 is ignored
        avails = list(DISABLED, 600);
        assert new ResourceAvailabilitySummary(avails).getUnknownTime() == 0;
        avails = list(DOWN, 600);
        assert new ResourceAvailabilitySummary(avails).getUnknownTime() == 0;
        avails = list(UP, 600);
        assert new ResourceAvailabilitySummary(avails).getUnknownTime() == 0;
        avails = list(DISABLED, 0, DOWN, 600, UP, 1200);
        assert new ResourceAvailabilitySummary(avails).getUnknownTime() == 0;
        avails = list(UNKNOWN, 1, DOWN, 100, UNKNOWN, 300, UP, 800);
        assert new ResourceAvailabilitySummary(avails).getUnknownTime() == 599000L; // from 1-100 then 300 to 800
        avails = list(UNKNOWN, 0, DOWN, 100, UNKNOWN, 300, UP, 800);
        assert new ResourceAvailabilitySummary(avails).getUnknownTime() == 500000L; // the initial range starting from 0 is ignored; 300 to 800 only
        avails = list(UP, 100, DOWN, 200, UNKNOWN, 400, DOWN, 800, UNKNOWN, 1600, UP, 3200);
        assert new ResourceAvailabilitySummary(avails).getUnknownTime() == 2000000L; // 400-800 then 1600-3200 (400+1600=2000s)

        avails = list(UNKNOWN, getPastTime(600));
        assertApproximate(600000L, new ResourceAvailabilitySummary(avails).getUnknownTime());

        long t1 = getPastTime(3200);
        avails = list(UP, 100, DOWN, t1, UNKNOWN, t1 + 400, UP, t1 + 800, UNKNOWN, t1 + 1600);
        Thread.sleep(1000);
        assertApproximate(2001000L, new ResourceAvailabilitySummary(avails).getUnknownTime()); //(t1+400)-(t1+800) then t1+1600-now (400+1600+1s sleep)
    }

    public void testKnownTime() throws InterruptedException {
        List<Availability> avails;

        avails = list(UNKNOWN, 0);
        assert new ResourceAvailabilitySummary(avails).getKnownTime() == 0;
        avails = list(UNKNOWN, 1);
        assert new ResourceAvailabilitySummary(avails).getKnownTime() == 0;
        avails = list(DOWN, 600, UNKNOWN, 1000);
        assert new ResourceAvailabilitySummary(avails).getKnownTime() == 400000L;
        avails = list(UP, 600, UNKNOWN, 1000);
        assert new ResourceAvailabilitySummary(avails).getKnownTime() == 400000L;
        avails = list(DISABLED, 0, DOWN, 100, UNKNOWN, 300, UP, 800, UNKNOWN, 1000);
        assert new ResourceAvailabilitySummary(avails).getKnownTime() == 500000L; // from 0-100, 100-300, 800-1000

        avails = list(UP, getPastTime(600));
        assertApproximate(600000L, new ResourceAvailabilitySummary(avails).getKnownTime());
        avails = list(DOWN, getPastTime(600));
        assertApproximate(600000L, new ResourceAvailabilitySummary(avails).getKnownTime());
        avails = list(DISABLED, getPastTime(600));
        assertApproximate(600000L, new ResourceAvailabilitySummary(avails).getKnownTime());
    }

    public void testUpPercentage() {
        List<Availability> avails;
        avails = list(UNKNOWN, 0);
        assert new ResourceAvailabilitySummary(avails).getUpPercentage() == 0;
        avails = list(UNKNOWN, 1);
        assert new ResourceAvailabilitySummary(avails).getUpPercentage() == 0;
        avails = list(DOWN, 600);
        assert new ResourceAvailabilitySummary(avails).getUpPercentage() == 0;
        avails = list(DISABLED, 600);
        assert new ResourceAvailabilitySummary(avails).getUpPercentage() == 0;
        avails = list(UP, 600);
        AssertJUnit.assertEquals(1.0, new ResourceAvailabilitySummary(avails).getUpPercentage(), 0.001);

        long t1 = getPastTime(1000);
        avails = list(UNKNOWN, 0, UP, t1, DOWN, t1 + 200, DISABLED, t1 + 500);
        AssertJUnit.assertEquals(0.2, new ResourceAvailabilitySummary(avails).getUpPercentage(), 0.001);

        // try it with the first unknown range starting at non-zero time
        t1 = getPastTime(1000);
        avails = list(UNKNOWN, 1, UP, t1, DOWN, t1 + 200, DISABLED, t1 + 500);
        AssertJUnit.assertEquals(0.2, new ResourceAvailabilitySummary(avails).getUpPercentage(), 0.001);
    }

    public void testDownPercentage() {
        List<Availability> avails;
        avails = list(UNKNOWN, 0);
        assert new ResourceAvailabilitySummary(avails).getDownPercentage() == 0;
        avails = list(UNKNOWN, 1);
        assert new ResourceAvailabilitySummary(avails).getDownPercentage() == 0;
        avails = list(UP, 600);
        assert new ResourceAvailabilitySummary(avails).getDownPercentage() == 0;
        avails = list(DISABLED, 600);
        assert new ResourceAvailabilitySummary(avails).getDownPercentage() == 0;
        avails = list(DOWN, 600);
        AssertJUnit.assertEquals(1.0, new ResourceAvailabilitySummary(avails).getDownPercentage(), 0.001);

        long t1 = getPastTime(1000);
        avails = list(UNKNOWN, 0, UP, t1, DOWN, t1 + 200, DISABLED, t1 + 500);
        AssertJUnit.assertEquals(0.3, new ResourceAvailabilitySummary(avails).getDownPercentage(), 0.001);

        // try it with the first unknown range starting at non-zero time
        t1 = getPastTime(1000);
        avails = list(UNKNOWN, 1, UP, t1, DOWN, t1 + 200, DISABLED, t1 + 500);
        AssertJUnit.assertEquals(0.3, new ResourceAvailabilitySummary(avails).getDownPercentage(), 0.001);
    }

    public void testDisabledPercentage() {
        List<Availability> avails;
        avails = list(UNKNOWN, 0);
        assert new ResourceAvailabilitySummary(avails).getDisabledPercentage() == 0;
        avails = list(UNKNOWN, 1);
        assert new ResourceAvailabilitySummary(avails).getDisabledPercentage() == 0;
        avails = list(UP, 600);
        assert new ResourceAvailabilitySummary(avails).getDisabledPercentage() == 0;
        avails = list(DOWN, 600);
        assert new ResourceAvailabilitySummary(avails).getDisabledPercentage() == 0;
        avails = list(DISABLED, 600);
        AssertJUnit.assertEquals(1.0, new ResourceAvailabilitySummary(avails).getDisabledPercentage(), 0.001);

        long t1 = getPastTime(1000);
        avails = list(UNKNOWN, 0, UP, t1, DISABLED, t1 + 200, DOWN, t1 + 500);
        AssertJUnit.assertEquals(0.3, new ResourceAvailabilitySummary(avails).getDisabledPercentage(), 0.001);

        // try it with the first unknown range starting at non-zero time
        t1 = getPastTime(1000);
        avails = list(UNKNOWN, 1, UP, t1, DISABLED, t1 + 200, DOWN, t1 + 500);
        AssertJUnit.assertEquals(0.3, new ResourceAvailabilitySummary(avails).getDisabledPercentage(), 0.001);
    }

    public void testMTBF() {
        List<Availability> avails;
        avails = list(UNKNOWN, 0);
        assert new ResourceAvailabilitySummary(avails).getMTBF() == 0;
        avails = list(UNKNOWN, 1);
        assert new ResourceAvailabilitySummary(avails).getMTBF() == 0;
        avails = list(UP, 600);
        assert new ResourceAvailabilitySummary(avails).getMTBF() == 0;
        avails = list(DISABLED, 600);
        assert new ResourceAvailabilitySummary(avails).getMTBF() == 0;
        avails = list(DOWN, getPastTime(600));
        assert new ResourceAvailabilitySummary(avails).getMTBF() == 0; // there isn't a subsequent UP, so we can't get MTBF yet

        avails = list(DOWN, 600, UP, 1000);
        assert new ResourceAvailabilitySummary(avails).getMTBF() == 0; // there hasn't been two failures, so we can't get MTBF yet
        avails = list(UP, 600, DOWN, 1000);
        assert new ResourceAvailabilitySummary(avails).getMTBF() == 0; // there hasn't been two failures, so we can't get MTBF yet
        avails = list(DISABLED, 600, UP, 1000, DOWN, 1200);
        assert new ResourceAvailabilitySummary(avails).getMTBF() == 0; // DISABLED is not considered DOWN
        avails = list(UNKNOWN, 0, UP, 1000, DOWN, 1200);
        assert new ResourceAvailabilitySummary(avails).getMTBF() == 0; // UNKNOWN is not considered DOWN
        avails = list(UNKNOWN, 1, UP, 1000, DOWN, 1200);
        assert new ResourceAvailabilitySummary(avails).getMTBF() == 0; // UNKNOWN is not considered DOWN

        // MTBF is simply the time between failures (i.e. mean time of being UP)

        //   UP          __________
        //      <-1000 ->|<-4000->|<-5000->
        // DOWN _________|  !!!!  |________
        //     -9000   -8000    -4000     now
        long t1 = getPastTime(9000);
        avails = list(DOWN, t1, UP, t1 + 1000, DOWN, t1 + 5000);
        assert 4000 * 1000L == new ResourceAvailabilitySummary(avails).getMTBF();
        t1 = getPastTime(9000);
        avails = list(UNKNOWN, 0, DOWN, t1, UP, t1 + 1000, DOWN, t1 + 5000);
        assert 4000 * 1000L == new ResourceAvailabilitySummary(avails).getMTBF();
        avails = list(UNKNOWN, 1, DOWN, t1, UP, t1 + 1000, DOWN, t1 + 5000); // non-zero initial unknown start time
        assert 4000 * 1000L == new ResourceAvailabilitySummary(avails).getMTBF();

        //   UP          __________        __________
        //      <-1000 ->|<-4000->|<-1000->|<-3000->
        // DOWN _________|  !!!!  |________|
        //     -9000   -8000    -4000    -3000     now
        t1 = getPastTime(9000);
        avails = list(UNKNOWN, 0, DOWN, t1, UP, t1 + 1000, DOWN, t1 + 5000, UP, t1 + 6000);
        assert 4000 * 1000L == new ResourceAvailabilitySummary(avails).getMTBF(); // do not count the current UP period

        //   UP          __________        __________
        //      <-1000 ->|<-4000->|<-1000->|<-3000->|
        // DOWN _________|  !!!!  |________|  !!!!  |_
        //     -9000   -8000    -4000    -3000     now
        t1 = getPastTime(9000);
        avails = list(UNKNOWN, 0, DOWN, t1, UP, t1 + 1000, DOWN, t1 + 5000, UP, t1 + 6000, DOWN, t1 + 9000);
        assert ((4000 + 3000) * 1000L) / 2 == new ResourceAvailabilitySummary(avails).getMTBF();

        // make sure DISABLED and UNKNOWN don't count in the calculations
        avails = list(UNKNOWN, 0, DOWN, 50, UP, 200, DISABLED, 300, UNKNOWN, 450, DOWN, 550);
        assert 100 * 1000L == new ResourceAvailabilitySummary(avails).getMTBF(); // only UP for 100
        avails = list(DOWN, 50, UP, 200, DISABLED, 300, UP, 400, UNKNOWN, 450, DOWN, 550);
        assert 150 * 1000L == new ResourceAvailabilitySummary(avails).getMTBF(); // first UP=100, second UP=50 but only two failures
        avails = list(DOWN, 50, UP, 200, DISABLED, 300, DOWN, 350, UP, 400, UNKNOWN, 450, DOWN, 550);
        assert 75 * 1000L == new ResourceAvailabilitySummary(avails).getMTBF(); // first UP=100, second UP=50 with three failures

        // just a bunch of down-up periods
        avails = list(DOWN, 100, UP, 200, DOWN, 400, UP, 500, DOWN, 700, UP, 800, DOWN, 1100, UP, 1200, DOWN, 1500);
        assert 250 * 1000L == new ResourceAvailabilitySummary(avails).getMTBF();
    }

    public void testMTTR() {
        List<Availability> avails;
        avails = list(UNKNOWN, 0);
        assert new ResourceAvailabilitySummary(avails).getMTTR() == 0;
        avails = list(UNKNOWN, 1);
        assert new ResourceAvailabilitySummary(avails).getMTTR() == 0;
        avails = list(UP, 600);
        assert new ResourceAvailabilitySummary(avails).getMTTR() == 0;
        avails = list(DISABLED, 600);
        assert new ResourceAvailabilitySummary(avails).getMTTR() == 0;
        avails = list(DOWN, getPastTime(600));
        assert new ResourceAvailabilitySummary(avails).getMTTR() == 0; // there isn't a subsequent UP, so there isn't a recovery yet!

        avails = list(DOWN, 600, UP, 1000);
        assert 400 * 1000L == new ResourceAvailabilitySummary(avails).getMTTR(); // we assume we were UP prior to the first DOWN
        avails = list(UP, 10000, DOWN, getPastTime(600));
        assert new ResourceAvailabilitySummary(avails).getMTTR() == 0; // we can't get MTTR because we haven't recovered at least once yet
        avails = list(UP, 100, DISABLED, 500, UP, 1000, DOWN, getPastTime(600));
        assert new ResourceAvailabilitySummary(avails).getMTBF() == 0; // DISABLED is not considered DOWN - we don't have a real recovery yet
        avails = list(UP, 100, UNKNOWN, 500, UP, 1000, DOWN, getPastTime(600));
        assert new ResourceAvailabilitySummary(avails).getMTBF() == 0; // UNKNOWN is not considered DOWN - we don't have a real recovery yet

        // MTTR is simply the time during failures (i.e. mean time of being DOWN)

        //   UP _________   !!!!   ________
        //      <-1000 ->|<-4000->|<-5000->
        // DOWN          |________|
        //     -9000   -8000    -4000     now
        long t1 = getPastTime(9000);
        avails = list(UP, t1, DOWN, t1 + 1000, UP, t1 + 5000);
        assert 4000 * 1000L == new ResourceAvailabilitySummary(avails).getMTTR();
        t1 = getPastTime(9000);
        avails = list(UNKNOWN, 0, UP, t1, DOWN, t1 + 1000, UP, t1 + 5000);
        assert 4000 * 1000L == new ResourceAvailabilitySummary(avails).getMTTR();
        avails = list(UNKNOWN, 1, UP, t1, DOWN, t1 + 1000, UP, t1 + 5000); // non-zero initial unknown start time
        assert 4000 * 1000L == new ResourceAvailabilitySummary(avails).getMTTR();

        //   UP _________   !!!!   ________
        //      <-1000 ->|<-4000->|<-1000->|<-3000->
        // DOWN          |________|        |________
        //     -9000   -8000    -4000    -3000     now
        t1 = getPastTime(9000);
        avails = list(UNKNOWN, 0, UP, t1, DOWN, t1 + 1000, UP, t1 + 5000, DOWN, t1 + 6000);
        assert 4000 * 1000L == new ResourceAvailabilitySummary(avails).getMTTR(); // do not count the current UP period

        //   UP _________   !!!!   ________   !!!!   _
        //      <-1000 ->|<-4000->|<-1000->|<-3000->|
        // DOWN          |________|        |________| 
        //     -9000   -8000    -4000    -3000     now
        t1 = getPastTime(9000);
        avails = list(UNKNOWN, 0, UP, t1, DOWN, t1 + 1000, UP, t1 + 5000, DOWN, t1 + 6000, UP, t1 + 9000);
        assert ((4000 + 3000) * 1000L) / 2 == new ResourceAvailabilitySummary(avails).getMTTR();

        // make sure DISABLED and UNKNOWN don't count in the calculations
        avails = list(UNKNOWN, 0, UP, 50, DOWN, 200, DISABLED, 300, UNKNOWN, 450, UP, 550);
        assert 100 * 1000L == new ResourceAvailabilitySummary(avails).getMTTR(); // only DOWN for 100
        avails = list(DOWN, 50, UP, 200, DISABLED, 300, UP, 400, UNKNOWN, 450, DOWN, 550);
        assert 150 * 1000L == new ResourceAvailabilitySummary(avails).getMTTR(); // two failures but only the first is used in the calculation
        avails = list(DOWN, 50, UP, 200, DISABLED, 300, DOWN, 350, UP, 400, UNKNOWN, 450, DOWN, 550);
        assert ((150 + 50) * 1000L) / 2 == new ResourceAvailabilitySummary(avails).getMTTR(); // three failures but only the first two are used

        // just a bunch of down-up periods
        avails = list(DOWN, 100, UP, 200, DOWN, 400, UP, 500, DOWN, 700, UP, 800, DOWN, 1100, UP, 1200, DOWN, 1500, UP,
            2000);
        assert ((100 + 100 + 100 + 100 + 500) * 1000L) / 5 == new ResourceAvailabilitySummary(avails).getMTTR();
    }

    /**
     * Pass in a series of availability type/start time pairs. The start time
     * must be in seconds - this method will convert that to a proper date.
     */
    private List<Availability> list(Object... objs) {
        Availability previousAvail = null;
        List<Availability> a = new ArrayList<Availability>();
        for (int i = 0; i < objs.length; i = i + 2) {
            Long startTime = new Long(((Number) objs[i + 1]).longValue() * 1000L);
            Availability newAvail = new Availability(res, startTime, (AvailabilityType) objs[i]);
            if (previousAvail != null) {
                previousAvail.setEndTime(startTime);
            }
            previousAvail = newAvail;
            a.add(newAvail);
        }

        return a;
    }

    private long getPastTime(int secondsInThePast) {
        return (System.currentTimeMillis() / 1000L) - secondsInThePast;
    }

    private void assertApproximate(long expected, long actual) {
        if (actual != expected) {
            long allowedError = 1500; // System.currentMillis is used; allow tests to be slow but times should still be within 1.5s
            if (actual < (expected - allowedError)) {
                assert false : "actual [" + actual + "] is too low to match the expected [" + expected + "]";
            } else if (actual > (expected + allowedError)) {
                assert false : "actual [" + actual + "] is too high to match the expected [" + expected + "]";
            }
        }
    }
}
