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
package org.rhq.enterprise.gui.coregui.client.inventory.resource.type;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.data.Record;
import com.smartgwt.client.widgets.tree.TreeGrid;
import com.smartgwt.client.widgets.tree.TreeGridField;
import com.smartgwt.client.widgets.tree.TreeNode;

import org.rhq.core.domain.criteria.ResourceTypeCriteria;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.gwt.ResourceTypeGWTServiceAsync;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableVLayout;

/**
 * @author Greg Hinkle
 */
public class PluginTypeTreeView extends LocatableVLayout {

    private ResourceTypeGWTServiceAsync resourceTypeService = GWTServiceLookup.getResourceTypeGWTService();

    public PluginTypeTreeView(String locatorId) {
        super(locatorId);
        setWidth100();
        setHeight100();
    }

    @Override
    protected void onDraw() {
        super.onDraw();

        final TreeGrid treeGrid = new CustomResourceTypeTreeGrid();

        treeGrid.setHeight100();

        treeGrid.setTitle(MSG.view_type_resourceTypes());
        treeGrid.setAnimateFolders(false);
        treeGrid.setResizeFieldsInRealTime(true);

        final TreeGridField name, plugin, category;
        name = new TreeGridField("name");
        plugin = new TreeGridField("plugin");
        category = new TreeGridField("category");

        treeGrid.setFields(name, plugin, category);

        addMember(treeGrid);

        ResourceTypeCriteria criteria = new ResourceTypeCriteria();
        criteria.fetchParentResourceTypes(true);
        criteria.setPageControl(PageControl.getUnlimitedInstance());

        resourceTypeService.findResourceTypesByCriteria(criteria, new AsyncCallback<PageList<ResourceType>>() {
            public void onFailure(Throwable caught) {
                CoreGUI.getErrorHandler().handleError("Failed to load", caught);
            }

            public void onSuccess(PageList<ResourceType> result) {

                treeGrid.getTree().linkNodes(ResourceTypePluginTreeDataSource.buildNodes(result));

            }
        });
    }

    public static class CustomResourceTypeTreeGrid extends TreeGrid {
        @Override
        protected String getIcon(Record record, boolean defaultState) {

            if (record instanceof TreeNode) {
                boolean open = getTree().isOpen((TreeNode) record);

                if (record instanceof ResourceTypePluginTreeDataSource.ResourceTypeTreeNode) {
                    ResourceType resourceType = ((ResourceTypePluginTreeDataSource.ResourceTypeTreeNode) record)
                        .getResourceType();

                    switch (resourceType.getCategory()) {
                    case PLATFORM:
                        return "types/Platform_up_16.png";
                    case SERVER:
                        return "types/Server_up_16.png";
                    case SERVICE:
                        return "types/Service_up_16.png";
                    }
                } else if (record instanceof ResourceTypePluginTreeDataSource.PluginTreeNode) {
                    return "types/plugin_16.png";
                }
            }
            return null;
        }
    }
}
