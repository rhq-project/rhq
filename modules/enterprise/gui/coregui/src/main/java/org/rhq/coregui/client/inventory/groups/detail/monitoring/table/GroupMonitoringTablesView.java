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

package org.rhq.coregui.client.inventory.groups.detail.monitoring.table;

import org.rhq.core.domain.resource.group.composite.ResourceGroupComposite;
import org.rhq.coregui.client.RefreshableView;
import org.rhq.coregui.client.util.enhanced.EnhancedVLayout;

/**
 *
 *
 * @author Lukas Krejci
 */
public class GroupMonitoringTablesView extends EnhancedVLayout implements RefreshableView {

    private GroupMeasurementTableView metrics;
    private GroupMembersHealthView memberHealth;

    public GroupMonitoringTablesView(ResourceGroupComposite groupComposite) {
        super();

        metrics = new GroupMeasurementTableView(groupComposite);
        memberHealth = new GroupMembersHealthView(groupComposite, false);
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
