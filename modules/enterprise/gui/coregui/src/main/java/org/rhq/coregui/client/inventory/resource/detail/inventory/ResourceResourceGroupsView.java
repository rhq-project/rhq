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

package org.rhq.coregui.client.inventory.resource.detail.inventory;

import java.util.Set;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.widgets.IButton;
import com.smartgwt.client.widgets.events.ClickEvent;
import com.smartgwt.client.widgets.events.ClickHandler;
import com.smartgwt.client.widgets.toolbar.ToolStrip;

import org.rhq.core.domain.criteria.ResourceCriteria;
import org.rhq.core.domain.resource.Resource;
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
public class ResourceResourceGroupsView extends EnhancedVLayout {

    private int resourceId;
    private Resource resource;
    private ResourceResourceGroupSelector selector;
    private ClickHandler saveButtonHandler;

    public ResourceResourceGroupsView(int resourceId) {
        super();

        this.resourceId = resourceId;
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

    @Override
    protected void onDraw() {
        super.onDraw();

        build();
    }

    public void build() {
        ToolStrip toolStrip = new ToolStrip();
        toolStrip.setWidth100();
        toolStrip.setExtraSpace(10);
        toolStrip.setMembersMargin(5);
        toolStrip.setLayoutMargin(5);

        IButton saveButton = new EnhancedIButton(MSG.common_button_save(), ButtonColor.BLUE);
        saveButton.addClickHandler(new ClickHandler() {
            public void onClick(ClickEvent clickEvent) {
                save();
                if (ResourceResourceGroupsView.this.saveButtonHandler != null) {
                    ResourceResourceGroupsView.this.saveButtonHandler.onClick(clickEvent);
                }
            }
        });

        toolStrip.addMember(saveButton);
        this.addMember(toolStrip);

        ResourceCriteria c = new ResourceCriteria();
        c.addFilterId(this.resourceId);
        c.fetchExplicitGroups(true);
        GWTServiceLookup.getResourceService().findResourcesByCriteria(c, new AsyncCallback<PageList<Resource>>() {
            public void onFailure(Throwable caught) {
                CoreGUI.getErrorHandler().handleError(MSG.view_resourceResourceGroupList_error_fetchFailure(), caught);
            }

            public void onSuccess(PageList<Resource> result) {
                resource = result.get(0);
                ResourceResourceGroupsView.this.selector = new ResourceResourceGroupSelector(resource
                    .getExplicitGroups());

                addMember(ResourceResourceGroupsView.this.selector);
            }
        });
    }

    private void save() {
        int[] resourceGroupIds = getSelectedResourceGroupIds();

        GWTServiceLookup.getResourceGroupService().setAssignedResourceGroupsForResource(this.resource.getId(),
            resourceGroupIds, true, new AsyncCallback<Void>() {
                public void onFailure(Throwable caught) {
                    CoreGUI.getErrorHandler().handleError(MSG.view_resourceResourceGroupList_error_updateFailure(),
                        caught);
                }

                public void onSuccess(Void result) {
                    CoreGUI
                        .getMessageCenter()
                        .notify(
                            new Message(
                                MSG.view_resourceResourceGroupList_message_updateSuccess(ResourceResourceGroupsView.this.resource
                                    .getName()), Message.Severity.Info));
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