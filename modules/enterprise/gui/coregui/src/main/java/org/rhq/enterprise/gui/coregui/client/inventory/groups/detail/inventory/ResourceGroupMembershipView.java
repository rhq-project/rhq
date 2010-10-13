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

package org.rhq.enterprise.gui.coregui.client.inventory.groups.detail.inventory;

import java.util.HashSet;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.widgets.IButton;
import com.smartgwt.client.widgets.events.ClickEvent;
import com.smartgwt.client.widgets.events.ClickHandler;
import com.smartgwt.client.widgets.layout.LayoutSpacer;
import com.smartgwt.client.widgets.toolbar.ToolStrip;

import org.rhq.core.domain.criteria.ResourceGroupCriteria;
import org.rhq.core.domain.resource.group.GroupCategory;
import org.rhq.core.domain.resource.group.ResourceGroup;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.util.message.Message;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableIButton;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableVLayout;

/**
 * @author Jay Shaughnessy
 */
public class ResourceGroupMembershipView extends LocatableVLayout {

    private int resourceGroupId;
    private ResourceGroup resourceGroup;
    private ResourceGroupResourceSelector selector;

    public ResourceGroupMembershipView(String locatorId, int resourceGroupId) {
        super(locatorId);

        this.resourceGroupId = resourceGroupId;
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

        ResourceGroupCriteria c = new ResourceGroupCriteria();
        c.addFilterId(this.resourceGroupId);
        c.fetchExplicitResources(true);
        c.fetchResourceType(true);
        GWTServiceLookup.getResourceGroupService().findResourceGroupsByCriteria(c,
            new AsyncCallback<PageList<ResourceGroup>>() {
                public void onFailure(Throwable caught) {
                    CoreGUI.getErrorHandler().handleError("Failed to fetch Resource Group", caught);
                }

                public void onSuccess(PageList<ResourceGroup> result) {
                    ResourceGroup group = result.get(0);
                    ResourceGroupMembershipView.this.resourceGroup = group;
                    ResourceGroupMembershipView.this.selector = new ResourceGroupResourceSelector(
                        ResourceGroupMembershipView.this.getLocatorId(),
                        group.getExplicitResources(),
                        (GroupCategory.COMPATIBLE == ResourceGroupMembershipView.this.resourceGroup.getGroupCategory()) ? group
                            .getResourceType()
                            : null, false);

                    addMember(ResourceGroupMembershipView.this.selector);
                }
            });
    }

    private void save() {
        int[] resourceIds = getSelectedResourceIds();

        GWTServiceLookup.getResourceGroupService().setAssignedResources(this.resourceGroup.getId(), resourceIds, true,
            new AsyncCallback<Void>() {
                public void onFailure(Throwable caught) {
                    CoreGUI.getErrorHandler().handleError("Failed to update configuration", caught);
                }

                public void onSuccess(Void result) {
                    CoreGUI.getMessageCenter().notify(
                        new Message("Group membership updated for ["
                            + ResourceGroupMembershipView.this.resourceGroup.getName() + "]", Message.Severity.Info));
                    CoreGUI.refresh();
                }
            });
    }

    private int[] getSelectedResourceIds() {
        int[] selection = new int[this.selector.getSelection().size()];
        HashSet<Integer> selectedIds = this.selector.getSelection();
        int i = 0;
        for (Integer id : selectedIds) {
            selection[i++] = id;
        }

        return selection;
    }

}