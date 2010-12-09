/*
 * RHQ Management Platform
 * Copyright (C) 2005-2010 Red Hat, Inc.
 * All rights reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2, as
 * published by the Free Software Foundation, and/or the GNU Lesser
 * General Public License, version 2.1, also as published by the Free
 * Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License and the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License
 * and the GNU Lesser General Public License along with this program;
 * if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */
package org.rhq.enterprise.gui.coregui.client.inventory.groups.detail;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.widgets.tree.TreeNode;

import org.rhq.core.domain.criteria.ResourceGroupCriteria;
import org.rhq.core.domain.resource.group.ClusterKey;
import org.rhq.core.domain.resource.group.ResourceGroup;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;

/**
 * @author Greg Hinkle
 */
public class ResourceGroupTreeContextMenu extends ResourceGroupContextMenu {

    public ResourceGroupTreeContextMenu(String locatorId) {
        super(locatorId);
    }

    public void showContextMenu(TreeNode node) {

        final ClusterKey clusterKey = (ClusterKey) node.getAttributeAsObject("key");
        if (clusterKey != null) {
            GWTServiceLookup.getClusterService().createAutoClusterBackingGroup(clusterKey, true,
                new AsyncCallback<ResourceGroup>() {
                    @Override
                    public void onFailure(Throwable caught) {
                        CoreGUI.getErrorHandler().handleError(
                            "Failed to create or update auto-cluster group: " + clusterKey, caught);
                    }

                    @Override
                    public void onSuccess(ResourceGroup result) {
                        showContextMenu(result);
                    }
                });
        } else {
            ResourceGroupCriteria criteria = new ResourceGroupCriteria();
            criteria.addFilterId(Integer.parseInt(node.getAttribute("id")));
            GWTServiceLookup.getResourceGroupService().findResourceGroupsByCriteria(criteria,
                new AsyncCallback<PageList<ResourceGroup>>() {
                    @Override
                    public void onFailure(Throwable caught) {
                        CoreGUI.getErrorHandler()
                            .handleError(MSG.view_tree_common_contextMenu_loadFail_group(), caught);
                    }

                    @Override
                    public void onSuccess(PageList<ResourceGroup> result) {
                        showContextMenu(result.get(0));
                    }
                });
        }
    }
}
