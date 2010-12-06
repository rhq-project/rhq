/*
 * RHQ Management Platform
 * Copyright (C) 2005-2010 Red Hat, Inc.
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

package org.rhq.enterprise.gui.coregui.client.inventory.resource.selection;

import org.rhq.core.domain.resource.ResourceType;

/**
 * This forces the user to only be able to select a single resource.
 * 
 * @author John Mazzitelli
 */
public class SingleResourceSelector extends ResourceSelector {

    public SingleResourceSelector(String locatorId) {
        super(locatorId);
    }

    public SingleResourceSelector(String locatorId, ResourceType resourceTypeFilter, boolean forceResourceTypeFilter) {
        super(locatorId, resourceTypeFilter, forceResourceTypeFilter);
    }

    @Override
    protected void updateButtonEnablement() {
        addButton.setDisabled(!availableGrid.anySelected() || availableGrid.getTotalRows() == 0
            || assignedGrid.getTotalRows() > 0);
        removeButton.setDisabled(!assignedGrid.anySelected());
        addAllButton.hide();
        removeAllButton.hide();
    }
}
