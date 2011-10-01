/*
 * RHQ Management Platform
 * Copyright (C) 2011 Red Hat, Inc.
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

package org.rhq.core.pc.drift.sync;

import static org.rhq.core.domain.drift.DriftDefinitionComparator.CompareMode.BOTH_BASE_INFO_AND_DIRECTORY_SPECIFICATIONS;

import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.domain.drift.DriftDefinition;
import org.rhq.core.domain.drift.DriftDefinitionComparator;
import org.rhq.core.pc.drift.DriftDetectionSchedule;
import org.rhq.core.pc.drift.DriftManager;
import org.rhq.core.pc.drift.ScheduleQueue;

/**
 * As its name implies, this class synchronizes drift definitions at runtime. By runtime
 * we mean when {@link DriftManager} is fully initialized. Updating the local inventory is
 * done through DriftManager. When this class determines that a drift definition needs
 * to be purged from the local inventory, it does so by calling DriftManager to unschedule
 * drift detection. Likewise when this class determines that a drift definition needs
 * to be added to the local inventory, it does so by calling DriftManager to schedule
 * detection.
 * <br/><br/>
 * Note that inventory sync happens regularly after the plugin container is initialized.
 * Discovery scans are performed at fixed intervals. The results of a discovery scan are
 * reported to the server, and the server sends back {@link org.rhq.core.domain.discovery.ResourceSyncInfo resource sync info}
 * which is then used to sync with the local inventory.
 */
class RuntimeSynchronizer implements DriftSynchronizer {
    private final Log log = LogFactory.getLog(RuntimeSynchronizer.class);

    private DriftManager driftMgr;

    public RuntimeSynchronizer(DriftManager driftManager) {
        driftMgr = driftManager;
    }

    @Override
    public List<DriftDefinition> getDeletedDefinitions(int resourceId, Set<DriftDefinition> definitionsFromServer) {
        log.debug("Checking for drift definitions that need to be deleted for resource id " + resourceId);
        List<DriftDefinition> deleted = new LinkedList<DriftDefinition>();
        ScheduleQueue queue = driftMgr.getSchedulesQueue();

        for (DriftDetectionSchedule schedule : getSchedulesForResource(resourceId, queue.toArray())) {
            if (!definitionsFromServer.contains(schedule.getDriftDefinition())) {
                if (log.isDebugEnabled()) {
                    log.debug("Detected stale drift definition that needs to be purged - "
                        + toString(resourceId, schedule.getDriftDefinition()));
                }
                deleted.add(schedule.getDriftDefinition());
            }
        }
        return deleted;
    }

    @Override
    public void purgeFromLocalInventory(int resourceId, List<DriftDefinition> definitions) {
        log.debug("Preparing to unschedule drift detection and purge from local inventory drift definitions "
            + "that have been deleted on the server for resource id " + resourceId);

        for (DriftDefinition c : definitions) {
            driftMgr.unscheduleDriftDetection(resourceId, c);
        }
    }

    @Override
    public List<DriftDefinition> getAddedDefinitions(int resourceId, Set<DriftDefinition> definitionsFromServer) {
        log.debug("Checking for drift definitions that need to be added for resource id " + resourceId);

        ScheduleQueue queue = driftMgr.getSchedulesQueue();
        DriftDefinitionComparator comparator = new DriftDefinitionComparator(
            BOTH_BASE_INFO_AND_DIRECTORY_SPECIFICATIONS);
        List<DriftDefinition> added = new LinkedList<DriftDefinition>();

        for (DriftDefinition c : definitionsFromServer) {
            if (!queue.contains(resourceId, c, comparator)) {
                if (log.isDebugEnabled()) {
                    log.debug("Detected new drift definition that needs to be added to local inventory - "
                        + toString(resourceId, c));
                }
                added.add(c);
            }
        }
        return added;
    }

    @Override
    public void addToLocalInventory(int resourceId, List<DriftDefinition> definitions) {
        log.debug("Adding drift definitions to local inventory and creating drift detection schedules for "
            + "resource id " + resourceId);

        for (DriftDefinition c : definitions) {
            if (log.isDebugEnabled()) {
                log.debug("Adding " + toString(resourceId, c) + " to local inventory");
            }
            driftMgr.scheduleDriftDetection(resourceId, c);
        }
    }

    @Override
    public void syncChangeSetContent() {
        driftMgr.scanForContentToResend();
    }

    private String toString(int rid, DriftDefinition c) {
        return "DriftDefinition[id: " + c.getId() + ", name: " + c.getName() + ", resourceId: " + rid + "]";
    }

    private List<DriftDetectionSchedule> getSchedulesForResource(int resourceId, DriftDetectionSchedule[] schedules) {
        List<DriftDetectionSchedule> resourceSchedules = new LinkedList<DriftDetectionSchedule>();
        for (DriftDetectionSchedule s : schedules) {
            if (s.getResourceId() == resourceId) {
                resourceSchedules.add(s);
            }
        }
        return resourceSchedules;
    }
}
