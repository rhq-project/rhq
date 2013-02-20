/*
 * RHQ Management Platform
 * Copyright (C) 2005-2011 Red Hat, Inc.
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

package org.rhq.enterprise.gui.coregui.client.inventory.groups.detail.monitoring.table;

import org.rhq.core.domain.resource.group.composite.ResourceGroupComposite;
import org.rhq.enterprise.gui.coregui.client.RefreshableView;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableVLayout;

/**
 * 
 *
 * @author Lukas Krejci
 */
public class GroupMonitoringTablesView extends LocatableVLayout implements RefreshableView {

    private GroupMeasurementTableView metrics;
    private GroupMembersHealthView memberHealth;
    
    /**
     * @param locatorId
     */
    public GroupMonitoringTablesView(String locatorId, ResourceGroupComposite groupComposite) {
        super();
        
        metrics = new GroupMeasurementTableView(extendLocatorId("ViewMetrics"), groupComposite, groupComposite.getResourceGroup().getId());
        memberHealth = new GroupMembersHealthView(extendLocatorId("ViewHealth"), groupComposite.getResourceGroup().getId(), false);
        addMember(metrics);
        addMember(memberHealth);
    }

    public void refresh() {
        metrics.refresh();
        //this method is called when saving the changed metric range on the measurement table. we therefore don't necessarily
        //need to refresh the members.
        //memberHealth.refresh();
    }
}
