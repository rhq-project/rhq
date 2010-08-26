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
package org.rhq.enterprise.gui.coregui.client.inventory.resource.detail;

import com.smartgwt.client.data.Record;
import com.smartgwt.client.widgets.tree.TreeNode;

import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.domain.resource.Resource;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableTreeGrid;

/**
 * @author Greg Hinkle
 */
public class CustomResourceTreeGrid extends LocatableTreeGrid {

    public CustomResourceTreeGrid(String locatorId) {
        super(locatorId);
    }

    @Override
    protected String getIcon(Record record, boolean defaultState) {

        if (record instanceof TreeNode) {
            boolean open = getTree().isOpen((TreeNode) record);

            if (record instanceof ResourceTreeDatasource.ResourceTreeNode) {
                Resource resource = ((ResourceTreeDatasource.ResourceTreeNode) record).getResource();

                boolean up = resource.getCurrentAvailability().getAvailabilityType() == AvailabilityType.UP;

                String category = resource.getResourceType().getCategory().getDisplayName();

                return "types/" + category + "_" + (up ? "up" : "down") + "_16.png";

            } else {
                return "resources/folder_group_" + (open ? "opened" : "closed") + ".png";
            }
        }
        return null;
    }

}
