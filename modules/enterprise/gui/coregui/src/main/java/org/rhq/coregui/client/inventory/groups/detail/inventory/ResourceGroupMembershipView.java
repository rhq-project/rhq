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
package org.rhq.coregui.client.inventory.groups.detail.inventory;

import java.util.Set;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.types.Alignment;
import com.smartgwt.client.widgets.IButton;
import com.smartgwt.client.widgets.events.ClickEvent;
import com.smartgwt.client.widgets.events.ClickHandler;
import com.smartgwt.client.widgets.toolbar.ToolStrip;

import org.rhq.core.domain.criteria.ResourceGroupCriteria;
import org.rhq.core.domain.resource.group.GroupCategory;
import org.rhq.core.domain.resource.group.ResourceGroup;
import org.rhq.core.domain.util.PageList;
import org.rhq.coregui.client.CoreGUI;
import org.rhq.coregui.client.gwt.GWTServiceLookup;
import org.rhq.coregui.client.util.enhanced.EnhancedIButton;
import org.rhq.coregui.client.util.enhanced.EnhancedVLayout;
import org.rhq.coregui.client.util.enhanced.EnhancedIButton.ButtonColor;
import org.rhq.coregui.client.util.message.Message;

/**
 * @author Jay Shaughnessy
 */
public class ResourceGroupMembershipView extends EnhancedVLayout {

    private int resourceGroupId;
    private ResourceGroup resourceGroup;
    private ResourceGroupResourceSelector selector;
    private ClickHandler saveButtonHandler;
    private ClickHandler cancelButtonHandler;

    public ResourceGroupMembershipView(int resourceGroupId) {
        super();

        this.resourceGroupId = resourceGroupId;
    }

    /**
     * Allows an external component to hook into the save button. The given
     * handler will be invoked when the save button is pressed. If <code>null</code>
     * is given, then no external handler will be called.
     * 
     * @param saveButtonHandler
     */
    public void setSaveButtonHandler(ClickHandler saveButtonHandler) {
        this.saveButtonHandler = saveButtonHandler;
    }

    /**
     * Allows an external component to hook into the cancel button. The given
     * handler will be invoked when the cancel button is pressed. If <code>null</code>
     * is given, then no external handler will be called.
     * 
     * @param cancelButtonHandler
     */
    public void setCancelButtonHandler(ClickHandler cancelButtonHandler) {
        this.cancelButtonHandler = cancelButtonHandler;
    }

    @Override
    protected void onDraw() {
        super.onDraw();

        build();
    }

    public void build() {

        final ToolStrip toolStrip = new ToolStrip();
        toolStrip.setWidth100();
        //toolStrip.setExtraSpace(10);
        toolStrip.setMembersMargin(5);
        toolStrip.setLayoutMargin(5);
        toolStrip.setAlign(Alignment.RIGHT);

        IButton cancelButton = new EnhancedIButton(MSG.common_button_cancel());
        cancelButton.addClickHandler(new ClickHandler() {
            public void onClick(ClickEvent clickEvent) {
                if (ResourceGroupMembershipView.this.cancelButtonHandler != null) {
                    ResourceGroupMembershipView.this.cancelButtonHandler.onClick(clickEvent);
                }
                destroy();
            }
        });
        toolStrip.addMember(cancelButton);

        IButton saveButton = new EnhancedIButton(MSG.common_button_save(), ButtonColor.BLUE);
        saveButton.addClickHandler(new ClickHandler() {
            public void onClick(ClickEvent clickEvent) {
                save();
                if (ResourceGroupMembershipView.this.saveButtonHandler != null) {
                    ResourceGroupMembershipView.this.saveButtonHandler.onClick(clickEvent);
                }
            }
        });
        toolStrip.addMember(saveButton);

        ResourceGroupCriteria c = new ResourceGroupCriteria();
        c.addFilterId(this.resourceGroupId);
        c.fetchExplicitResources(true);
        c.fetchResourceType(true);
        GWTServiceLookup.getResourceGroupService().findResourceGroupsByCriteria(c,
            new AsyncCallback<PageList<ResourceGroup>>() {
                public void onFailure(Throwable caught) {
                    CoreGUI.getErrorHandler().handleError(MSG.view_group_membership_failFetch(), caught);
                }

                public void onSuccess(PageList<ResourceGroup> result) {
                    ResourceGroup group = result.get(0);
                    ResourceGroupMembershipView.this.resourceGroup = group;
                    ResourceGroupMembershipView.this.selector = new ResourceGroupResourceSelector(
                        group.getExplicitResources(),
                        (GroupCategory.COMPATIBLE == ResourceGroupMembershipView.this.resourceGroup.getGroupCategory()) ? group
                            .getResourceType() : null, false);

                    addMember(ResourceGroupMembershipView.this.selector);

                    addMember(toolStrip);

                    markForRedraw();
                }
            });
    }

    private void save() {
        int[] resourceIds = getSelectedResourceIds();

        GWTServiceLookup.getResourceGroupService().setAssignedResources(this.resourceGroup.getId(), resourceIds, true,
            new AsyncCallback<Void>() {
                public void onFailure(Throwable caught) {
                    CoreGUI.getErrorHandler()
                        .handleError(
                            MSG.view_group_membership_saveFailure(ResourceGroupMembershipView.this.resourceGroup
                                .getName()), caught);
                }

                public void onSuccess(Void result) {
                    CoreGUI.getMessageCenter().notify(
                        new Message(MSG
                            .view_group_membership_saveSuccessful(ResourceGroupMembershipView.this.resourceGroup
                                .getName()), Message.Severity.Info));
                    CoreGUI.refresh();
                }
            });
    }

    private int[] getSelectedResourceIds() {
        Set<Integer> selectedIds = this.selector.getSelection();
        int[] selection = new int[selectedIds.size()];
        int i = 0;
        for (Integer id : selectedIds) {
            selection[i++] = id;
        }

        return selection;
    }

}