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
package org.rhq.enterprise.gui.coregui.client.inventory.groups.detail;

import com.smartgwt.client.data.Record;
import com.smartgwt.client.widgets.tree.TreeNode;

import org.rhq.core.domain.resource.ResourceType;
import org.rhq.enterprise.gui.coregui.client.ImageManager;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableTreeGrid;

/**
 * Tree grid used to render the left hand group tree.
 * 
 * @author John Mazzitelli
 */
public class CustomResourceGroupTreeGrid extends LocatableTreeGrid {

    public CustomResourceGroupTreeGrid(String locatorId) {
        super(locatorId);
    }

    @Override
    protected String getIcon(Record record, boolean defaultState) {

        if (record instanceof ResourceGroupTreeView.ResourceGroupEnhancedTreeNode) {
            ResourceGroupTreeView.ResourceGroupEnhancedTreeNode node = (ResourceGroupTreeView.ResourceGroupEnhancedTreeNode) record;

            // allow the nodes to tell us what icon they want
            if (node.getIcon() != null) {
                return node.getIcon();
            }

            if (node.isAutoClusterNode()) {
                ResourceType resourceType = node.getResourceType();
                String icon = ImageManager.getClusteredResourceIcon(resourceType.getCategory());
                return icon;

            } else if (node.isCompatibleGroupTopNode() || node.isAutoGroupNode()) {
                boolean open = getTree().isOpen((TreeNode) record);
                return "resources/folder_group_" + (open ? "opened" : "closed") + ".png";

            }

            // use the default image - which is typically for subcategory nodes
            boolean open = getTree().isOpen((TreeNode) record);
            return "resources/folder_" + (open ? "opened" : "closed") + ".png";
        }

        return null; // should never happen
    }
}
