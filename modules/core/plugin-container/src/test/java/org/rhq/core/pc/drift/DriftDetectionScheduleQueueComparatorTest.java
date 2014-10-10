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

package org.rhq.core.pc.drift;

import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Random;

import org.testng.annotations.Test;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.drift.DriftDefinition;

public class DriftDetectionScheduleQueueComparatorTest {
    private static final int DISABLED_COUNT = 100;
    private static final int TOTAL = DISABLED_COUNT * 20;

    @Test
    public void testPriority() {
        Collection<DriftDetectionSchedule> sampleSchedules = createSampleSchedules();

        PriorityQueue<DriftDetectionSchedule> priorityQueue = new PriorityQueue<DriftDetectionSchedule>(
            sampleSchedules.size(), new DriftDetectionScheduleQueueComparator());
        priorityQueue.addAll(sampleSchedules);

        DriftDetectionSchedule previousSchedule = priorityQueue.poll();
        assertNotNull(previousSchedule);

        DriftDetectionSchedule schedule;
        boolean foundDisabled = !previousSchedule.getDriftDefinition().isEnabled();
        while ((schedule = priorityQueue.poll()) != null) {
            boolean enabled = schedule.getDriftDefinition().isEnabled();
            if (foundDisabled && enabled) {
                fail("All disabled schedule should be at the end of queue");
            }
            foundDisabled = !enabled;
            if (!foundDisabled) {
                assertTrue(schedule.getNextScan() < previousSchedule.getNextScan(), "getNextScan priority failure");
            }
            previousSchedule = schedule;
        }
    }

    private static Collection<DriftDetectionSchedule> createSampleSchedules() {
        Random random = new Random();
        List<DriftDetectionSchedule> schedules = new ArrayList<DriftDetectionSchedule>(TOTAL);
        for (int i = 0; i < TOTAL; i++) {
            schedules.add(newScheduleInstance(random.nextLong(), i > DISABLED_COUNT));
        }
        Collections.shuffle(schedules);
        return schedules;
    }

    private static DriftDetectionSchedule newScheduleInstance(long interval, boolean enabled) {
        DriftDefinition definition = new DriftDefinition(new Configuration());
        definition.setEnabled(enabled);
        definition.setInterval(interval);
        DriftDetectionSchedule driftDetectionSchedule = new DriftDetectionSchedule(-1, definition);
        driftDetectionSchedule.updateShedule();
        return driftDetectionSchedule;
    }

}
