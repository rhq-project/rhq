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
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.clientapi.server.drift.DriftServerService;
import org.rhq.core.domain.drift.DriftConfiguration;
import org.rhq.core.domain.drift.DriftConfigurationComparator;
import org.rhq.core.pc.drift.DriftManager;
import org.rhq.core.pc.inventory.InventoryManager;

import static org.rhq.core.domain.drift.DriftConfigurationComparator.CompareMode.BOTH_BASE_INFO_AND_DIRECTORY_SPECIFICATIONS;

/**
 * This class handles syncing drift configurations during inventory sync. This class is
 * intended to server as the public interface (so to speak) to {@link InventoryManager} for
 * syncing drift configurations.
 * <br/><br/>
 * Please review the docs for each of the setter methods to determine which properties
 * should be set before invoking any business logic methods.
 */
public class DriftSyncManager {
    private final Log log = LogFactory.getLog(DriftSyncManager.class);

    private DriftServerService driftServer;

    private DriftManager driftMgr;

    private InventoryManager inventoryMgr;

    private File dataDir;

    /**
     * @param driftServer The interface to the remote server. This property must be set.
     * It is injected to facilitate testing with stubs or mocks.
     */
    public void setDriftServer(DriftServerService driftServer) {
        this.driftServer = driftServer;
    }

    /**
     * @param driftManager The drift manager singleton created by the plugin container.
     * This property must be set even if drift manager is not yet initialized. It is
     * injected to facilitate testing with stubs or mocks.
     */
    public void setDriftManager(DriftManager driftManager) {
        driftMgr = driftManager;
    }

    /**
     * @param inventoryManager The inventory manager singleton created by the plugin
     * container. This property must be set. It is injected to facilitate testing with stubs
     * or mocks.
     */
    public void setInventoryManager(InventoryManager inventoryManager) {
        inventoryMgr = inventoryManager;
    }

    /**
     * @param dataDirectory The plugin container data directory where inventory files are
     * stored. This property must be set. It is injected to facilitate testing.
     */
    public void setDataDirectory(File dataDirectory) {
        dataDir = dataDirectory;
    }

    public void syncConfigsWithServer(Set<Integer> resourceIds) {
        log.info("Starting server sync for drift configurations...");
        long startTime = System.currentTimeMillis();

        Map<Integer, List<DriftConfiguration>> configs = driftServer.getDriftConfigurations(resourceIds);
        DriftConfigurationComparator comparator = new DriftConfigurationComparator(
            BOTH_BASE_INFO_AND_DIRECTORY_SPECIFICATIONS);
        DriftSynchronizerFactory synchronizerFactory = new DriftSynchronizerFactory();
        DriftSynchronizer synchronizer;

        if (isStartUpSync()) {
            synchronizer = synchronizerFactory.getStartUpSynchronizer(inventoryMgr, dataDir);
        } else {
            synchronizer = synchronizerFactory.getRuntimeSynchronizer(driftMgr);
        }

        int totalDeleted = 0;
        int totalAdded = 0;

        for (Integer resourceId : configs.keySet()) {
            Set<DriftConfiguration> resourceConfigsOnServer = new TreeSet<DriftConfiguration>(comparator);
            resourceConfigsOnServer.addAll(configs.get(resourceId));

            List<DriftConfiguration> deletedConfigs = synchronizer.getDeletedConfigurations(resourceId,
                resourceConfigsOnServer);
            totalDeleted += deletedConfigs.size();
            synchronizer.purgeFromLocalInventory(resourceId, deletedConfigs);

            List<DriftConfiguration> addedConfigs = synchronizer.getAddedConfigurations(resourceId,
                resourceConfigsOnServer);
            totalAdded += addedConfigs.size();
            synchronizer.addToLocalInventory(resourceId, addedConfigs);
        }

        long endTime = System.currentTimeMillis();
        if (log.isInfoEnabled()) {
            log.info("Finished server sync for drift configurations. " + totalAdded + " added and " + totalDeleted +
                " deleted in " + (endTime - startTime) + " ms");
        }
    }

    /**
     * This method determines whether or not this is a sync being done at start up (or
     * more precisely during PC initialization). It does by checking to see if
     * DriftManager is initialized. The only time DriftManager should not be initialized
     * is during PC initialization which happens at start up or at runtime when the PC is
     * rebooted.
     *
     * @return true if this is a sync being performed at start up or plugin container
     * initialization.
     */
    private boolean isStartUpSync() {
        return driftMgr == null || !driftMgr.isInitialized();
    }

}
