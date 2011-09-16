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

import java.io.File;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.domain.drift.DriftConfiguration;
import org.rhq.core.pc.inventory.InventoryManager;
import org.rhq.core.pc.inventory.ResourceContainer;

import static org.rhq.core.util.file.FileUtil.purge;

/**
 * As its name implies, this class synchronizes drift configurations at start up or any
 * time when the plugin container is not fully initialized. If the plugin container is not
 * fully initialized, then that means {@link org.rhq.core.pc.drift.DriftManager DriftManager}
 * is not available to call into to schedule or unschedule drift detection.
 * StartupSynchronizer therefore deals only with the drift configurations attached to
 * {@link ResourceContainer} objects. As part of its initialization DriftManager
 * creates or recreates detection schedules from the configurations attached to the
 * resource containers.
 */
class StartupSynchronizer implements DriftSynchronizer {
    private final Log log = LogFactory.getLog(StartupSynchronizer.class);

    private InventoryManager inventoryMgr;

    private File snapshotsDir;

    public StartupSynchronizer(InventoryManager inventoryManager, File dataDirectory) {
        inventoryMgr = inventoryManager;
        snapshotsDir = new File(dataDirectory, "changesets");
    }

    @Override
    public List<DriftConfiguration> getDeletedConfigurations(int resourceId,
        Set<DriftConfiguration> configurationsFromServer) {
        log.debug("Checking for drift configurations that need to be deleted for resource id " + resourceId);
        List<DriftConfiguration> deleted = new LinkedList<DriftConfiguration>();
        ResourceContainer container = inventoryMgr.getResourceContainer(resourceId);

        for (DriftConfiguration c : container.getDriftConfigurations()) {
            if (!configurationsFromServer.contains(c)) {
                if (log.isDebugEnabled()) {
                    log.debug("Detected stale drift configuration that needs to be purged - " +
                        toString(resourceId, c));
                }
                deleted.add(c);
            }
        }

        return deleted;
    }

    @Override
    public void purgeFromLocalInventory(int resourceId, List<DriftConfiguration> configurations) {
        log.debug("Preparing to purge from local inventory drift configurations that have been deleted on the server "
            + "for resource id " + resourceId);
        ResourceContainer container = inventoryMgr.getResourceContainer(resourceId);
        File resourceSnapshotsDir = new File(snapshotsDir, Integer.toString(resourceId));

        for (DriftConfiguration c : configurations) {
            if (log.isDebugEnabled()) {
                log.debug("Purging " + toString(resourceId, c) + " from local inventory");
            }
            container.removeDriftConfiguration(c);
            File snapshotDir = new File(resourceSnapshotsDir, c.getName());
            if (snapshotDir.exists()) {
                if (log.isDebugEnabled()) {
                    log.debug("Puring snapshot directory " + snapshotDir.getPath());
                }
                purge(snapshotDir, true);
            }
        }
    }

    @Override
    public List<DriftConfiguration> getAddedConfigurations(int resourceId,
        Set<DriftConfiguration> configurationsFromServer) {
        log.debug("Checking for drift configurations that need to be added for resource id " + resourceId);

        ResourceContainer container = inventoryMgr.getResourceContainer(resourceId);
        List<DriftConfiguration> added = new LinkedList<DriftConfiguration>();


        for (DriftConfiguration c : configurationsFromServer) {
            if (!container.containsDriftConfiguration(c)) {
                if (log.isDebugEnabled()) {
                    log.debug("Detected new drift configuration that needs to be added to local inventory - " +
                        toString(resourceId, c));
                }
                added.add(c);
            }
        }
        return added;
    }

    @Override
    public void addToLocalInventory(int resourceId, List<DriftConfiguration> configurations) {
        log.debug("Adding drift configurations to local inventory for resource id " + resourceId);
        ResourceContainer container = inventoryMgr.getResourceContainer(resourceId);

        for (DriftConfiguration c : configurations) {
            if (log.isDebugEnabled()) {
                log.debug("Adding " + toString(resourceId, c) + " to local inventory");
            }
            container.addDriftConfiguration(c);
        }
    }

    private String toString(int rid, DriftConfiguration c) {
        return "DriftConfiguration[id: " + c.getId() + ", name: " + c.getName() + ", resourceId: " + rid + "]";
    }
}
