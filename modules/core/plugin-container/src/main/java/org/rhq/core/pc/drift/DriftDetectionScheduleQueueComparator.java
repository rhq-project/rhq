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

/**
 * Comparator for a {@link java.util.PriorityQueue} of {@link DriftDetectionSchedule} instances. Disabled schedules go
 * at the end of the queue, enabled schedules are ordered by nextScan property.
 *
 * @author Thomas Segismont
 */
class DriftDetectionScheduleQueueComparator implements java.util.Comparator<DriftDetectionSchedule> {
    @Override
    public int compare(DriftDetectionSchedule schedule1, DriftDetectionSchedule schedule2) {
        boolean enabled1 = schedule1.getDriftDefinition().isEnabled();
        boolean enabled2 = schedule2.getDriftDefinition().isEnabled();
        int diff = (enabled1 == enabled2) ? 0 : (enabled2 ? 1 : -1);
        if (diff == 0) {
            long nextScan1 = schedule1.getNextScan();
            long nextScan2 = schedule2.getNextScan();
            diff = (nextScan1 == nextScan2) ? 0 : (nextScan1 > nextScan2 ? 1 : -1);
        }
        return diff;
    }
}
