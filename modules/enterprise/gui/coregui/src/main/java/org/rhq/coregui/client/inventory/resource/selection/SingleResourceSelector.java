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

package org.rhq.coregui.client.inventory.resource.selection;

import com.smartgwt.client.types.SelectionStyle;

import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceType;

/**
 * This forces the user to only be able to select a single resource.
 * 
 * @author John Mazzitelli
 */
public class SingleResourceSelector extends ResourceSelector {

    public SingleResourceSelector() {
        super();
    }

    public SingleResourceSelector(ResourceType resourceTypeFilter, boolean forceResourceTypeFilter) {
        super(resourceTypeFilter, forceResourceTypeFilter);
    }

    @Override
    protected void onInit() {
        super.onInit();

        // we only allow a single resource to be selected
        availableGrid.setSelectionType(SelectionStyle.SINGLE);
    }

    @Override
    protected void updateButtonEnablement() {
        addButton.setDisabled(!availableGrid.anySelected() || availableGrid.getTotalRows() == 0
            || assignedGrid.getTotalRows() > 0);
        removeButton.setDisabled(!assignedGrid.anySelected());
        addAllButton.hide();
        removeAllButton.hide();
    }

    @Override
    public void addSelectedRows() {
        // do not allow more than one row into the assigned grid
        if (assignedGrid.getTotalRows() == 0) {
            super.addSelectedRows();
        }
    }
    
    public Resource getSelectedResource() {
        if (assignedGrid.getTotalRows() != 0) {
            return getDataSource().copyValues(assignedGrid.getRecord(0));
        }
        return null;
    }
}
