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

import java.util.List;
import java.util.Set;

import org.rhq.core.domain.drift.DriftConfiguration;

/**
 * A DriftSynchronizer is responsible for sycning {@link DriftConfiguration}s in the
 * server's with those in the local inventory.
 */
public interface DriftSynchronizer {

    /**
     * Determines which drift configurations for a resource have been deleted on the server
     * and need to be purged from the local inventory. This method should not make any
     * changes to the local inventory.
     *
     * @param resourceId
     * @param configurationsFromServer A set of drift configurations belonging to the
     * resource with the specified id. The set uses a {@link org.rhq.core.domain.drift.DriftConfigurationComparator DriftConfigurationComparator}
     * with the compare mode set to {@link org.rhq.core.domain.drift.DriftConfigurationComparator.CompareMode#BOTH_BASE_INFO_AND_DIRECTORY_SPECIFICATIONS BOTH_BASE_INFO_AND_DIRECTORY_SPECIFICATIONS}.
     * @return A list of drift configurations that need to be purged from the local inventory.
     */
    List<DriftConfiguration> getDeletedConfigurations(int resourceId,
        Set<DriftConfiguration> configurationsFromServer);


    /**
     * Removes the drift configurations from local inventory. Implementations are responsible
     * for deciding how that is to be done. For example, if the plugin container is not
     * fully initialized, then purging will involve removing configurations from the
     * {@link org.rhq.core.pc.inventory.ResourceContainer ResourceContainer}. But if the
     * plugin container is initialized, then drift detection will have to be unscheduled.
     *
     * @param resourceId
     * @param configurations The drift configurations to purge from local inventory
     */
    void purgeFromLocalInventory(int resourceId, List<DriftConfiguration> configurations);

    /**
     * Determines which drift configurations for a resource have been added on the server
     * and need to be added to the local inventory. This method should not make any changes
     * to the local inventory.
     *
     * @param resourceId
     * @param configurationsFromServer A set of drift configurations belonging to the
     * resource with the specified id. The set uses a {@link org.rhq.core.domain.drift.DriftConfigurationComparator DriftConfigurationComparator}
     * with the compare mode set to {@link org.rhq.core.domain.drift.DriftConfigurationComparator.CompareMode#BOTH_BASE_INFO_AND_DIRECTORY_SPECIFICATIONS BOTH_BASE_INFO_AND_DIRECTORY_SPECIFICATIONS}.
     * @return A list of drift configurations that need to be purged from the local inventory.
     */
    List<DriftConfiguration> getAddedConfigurations(int resourceId, Set<DriftConfiguration> configurationsFromServer);

    /**
     * Adds the drift configurations to the local inventory. Implementations are responsible
     * for deciding how that is to be done. For example, if the plugin container is not
     * fully initialized, then adding a configuration will involve adding it to the
     * {@link org.rhq.core.pc.inventory.ResourceContainer ResourceContainer}. But if the
     * plugin container is initialized, drift detection will have to be scheduled.
     *
     * @param resourceId
     * @param configurations The drift configurations to add to the local inventory.
     */
    void addToLocalInventory(int resourceId, List<DriftConfiguration> configurations);
}
