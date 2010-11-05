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

package org.rhq.enterprise.gui.coregui.client.inventory.resource.detail.inventory;

import java.util.HashSet;
import java.util.Set;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.widgets.IButton;
import com.smartgwt.client.widgets.events.ClickEvent;
import com.smartgwt.client.widgets.events.ClickHandler;
import com.smartgwt.client.widgets.layout.LayoutSpacer;
import com.smartgwt.client.widgets.toolbar.ToolStrip;

import org.rhq.core.domain.criteria.ResourceCriteria;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.util.message.Message;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableIButton;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableVLayout;

/**
 * @author Jay Shaughnessy
 */
public class ResourceResourceGroupsView extends LocatableVLayout {

    private int resourceId;
    private Resource resource;
    private ResourceResourceGroupSelector selector;

    public ResourceResourceGroupsView(String locatorId, int resourceId) {
        super(locatorId);

        this.resourceId = resourceId;
    }

    @Override
    protected void onDraw() {
        super.onDraw();

        build();
    }

    public void build() {
        ToolStrip toolStrip = new ToolStrip();
        toolStrip.setWidth100();

        toolStrip.addMember(new LayoutSpacer());

        IButton saveButton = new LocatableIButton(this.extendLocatorId("Save"), "Save");
        saveButton.addClickHandler(new ClickHandler() {
            public void onClick(ClickEvent clickEvent) {
                save();
            }
        });

        toolStrip.addMember(saveButton);
        this.addMember(toolStrip);

        ResourceCriteria c = new ResourceCriteria();
        c.addFilterId(this.resourceId);
        c.fetchExplicitGroups(true);
        GWTServiceLookup.getResourceService().findResourcesByCriteria(c, new AsyncCallback<PageList<Resource>>() {
            public void onFailure(Throwable caught) {
                CoreGUI.getErrorHandler().handleError("Failed to fetch Resource's Groups", caught);
            }

            public void onSuccess(PageList<Resource> result) {
                resource = result.get(0);
                ResourceResourceGroupsView.this.selector = new ResourceResourceGroupSelector(
                    ResourceResourceGroupsView.this.getLocatorId(), resource.getExplicitGroups());

                addMember(ResourceResourceGroupsView.this.selector);
            }
        });
    }

    private void save() {
        int[] resourceGroupIds = getSelectedResourceGroupIds();

        GWTServiceLookup.getResourceGroupService().setAssignedResourceGroupsForResource(this.resource.getId(),
            resourceGroupIds, true, new AsyncCallback<Void>() {
                public void onFailure(Throwable caught) {
                    CoreGUI.getErrorHandler().handleError("Failed to update configuration", caught);
                }

                public void onSuccess(Void result) {
                    CoreGUI.getMessageCenter().notify(
                        new Message("Group membership updated for ["
                            + ResourceResourceGroupsView.this.resource.getName() + "]", Message.Severity.Info));
                    CoreGUI.refresh();
                }
            });
    }

    private int[] getSelectedResourceGroupIds() {
        Set<Integer> selectedIds = this.selector.getSelection();
        int[] selection = new int[selectedIds.size()];        
        int i = 0;
        for (Integer id : selectedIds) {
            selection[i++] = id;
        }

        return selection;
    }

}