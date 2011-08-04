/*
 * RHQ Management Platform
 * Copyright (C) 2005-2008 Red Hat, Inc.
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
package org.rhq.enterprise.server.resource;

import javax.ejb.EJB;
import javax.ejb.Stateless;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.resource.InventoryStatus;
import org.rhq.core.domain.resource.InventorySummary;
import org.rhq.enterprise.server.measurement.MeasurementScheduleManagerLocal;
import org.rhq.enterprise.server.resource.group.ResourceGroupManagerLocal;
import org.rhq.enterprise.server.resource.group.definition.GroupDefinitionManagerLocal;

/**
 * The boss for "high-level" operations involving {@link org.rhq.core.domain.resource.Resource}s and/or
 * {@link org.rhq.core.domain.resource.group.Group}s.
 *
 * @author Ian Springer
 */
@Stateless
public class ResourceBossBean implements ResourceBossLocal {
    @EJB
    private ResourceManagerLocal resourceManager;

    @EJB
    private ResourceGroupManagerLocal groupManager;

    @EJB
    private MeasurementScheduleManagerLocal scheduleManager;

    @EJB
    private GroupDefinitionManagerLocal groupDefinitionManager;

    public InventorySummary getInventorySummary(Subject user) {
        InventorySummary summary = new InventorySummary();

        int[] categoryCounts = resourceManager.getResourceCountSummary(user, InventoryStatus.COMMITTED);
        summary.setPlatformCount(categoryCounts[0]);
        summary.setServerCount(categoryCounts[1]);
        summary.setServiceCount(categoryCounts[2]);

        categoryCounts = groupManager.getResourceGroupCountSummary(user);
        summary.setMixedGroupCount(categoryCounts[0]);
        summary.setCompatibleGroupCount(categoryCounts[1]);

        summary.setGroupDefinitionCount(groupDefinitionManager.getGroupDefinitionCount(user));

        summary.setScheduledMeasurementsPerMinute(scheduleManager.getScheduledMeasurementsPerMinute());

        return summary;
    }
}