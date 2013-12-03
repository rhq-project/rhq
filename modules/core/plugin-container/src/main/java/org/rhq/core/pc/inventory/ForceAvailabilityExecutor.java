/*
 * RHQ Management Platform
 * Copyright 2012, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
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
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.domain.resource.Resource;

/**
 * A thin subclass that ensures that the avail report generation forces avail checks for all resources.
 *
 * @author Jay Shaughnessy
 */
public class ForceAvailabilityExecutor extends AvailabilityExecutor {

    public ForceAvailabilityExecutor(InventoryManager inventoryManager) {
        super(inventoryManager);
    }

    @Override
    protected void checkInventory(Resource resource, AvailabilityReport availabilityReport,
        AvailabilityType parentAvailType, boolean forceCheck, Scan scan) throws InterruptedException {

        scan.setForced(true);
        super.checkInventory(resource, availabilityReport, parentAvailType, true, scan);
    }
}
