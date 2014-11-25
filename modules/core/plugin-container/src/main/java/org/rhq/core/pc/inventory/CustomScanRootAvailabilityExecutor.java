/*
 * RHQ Management Platform
 * Copyright (C) 2013 Red Hat, Inc.
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

package org.rhq.core.pc.inventory;

import org.rhq.core.domain.discovery.AvailabilityReport;
import org.rhq.core.domain.measurement.MeasurementScheduleRequest;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceCategory;

/**
 * A thin wrapper around the {@link AvailabilityExecutor} to enable performing availability scans rooted at resources
 * other than the platform.
 *
 * @author Lukas Krejci
 * @since 4.9
 */
public class CustomScanRootAvailabilityExecutor extends AvailabilityExecutor {

    private final Resource scanRoot;
    private final boolean forceScanForRoot;

    /**
     * @param inventoryManager the inventory manager to use
     * @param scanRoot         the root of the availability scan
     * @param forceScanForRoot if true, the avail check for the root resource is always performed regardless of
     *                         the availability schedule for it
     */
    public CustomScanRootAvailabilityExecutor(InventoryManager inventoryManager, Resource scanRoot,
        boolean forceScanForRoot) {
        super(inventoryManager);
        this.scanRoot = scanRoot;
        this.forceScanForRoot = forceScanForRoot;
    }

    @Override
    protected void startScan(Resource ignored, AvailabilityReport availabilityReport, boolean changesOnly) {
        ResourceContainer resourceContainer = null;
        if (forceScanForRoot) {
            resourceContainer = inventoryManager.getResourceContainer(scanRoot);

            //if we can't the resource container, let's just not bother with the scan at all
            if (resourceContainer == null) {
                return;
            }


            // We really want the check to happen ;)
            // We can force that by setting the avail schedule time to past.
            // We don't do that for a platform resource though, because its schedule is handled differently (and it
            // always returns "UP" anyway).
            if (resourceContainer.getResourceContext().getResourceType().getCategory() != ResourceCategory.PLATFORM) {
                resourceContainer.setAvailabilityScheduleTime(System.currentTimeMillis() - 10);
            }
        }

        super.startScan(scanRoot, availabilityReport, changesOnly);
        // if we happened to set availabilityScheduleTime for a resource having disabled availability schedule
        // we need to reset it back to 0 (both isEnabled=true and availabilityScheduleTime=0 indicate, not to do 
        // real avail check on resource, but rather defer to parent's availability)
        if (forceScanForRoot && resourceContainer != null) {
            MeasurementScheduleRequest availSchedule = resourceContainer.getAvailabilitySchedule();
            if (availSchedule != null && !availSchedule.isEnabled()) {
                resourceContainer.setAvailabilityScheduleTime(0L);
            }
        }
    }
}
