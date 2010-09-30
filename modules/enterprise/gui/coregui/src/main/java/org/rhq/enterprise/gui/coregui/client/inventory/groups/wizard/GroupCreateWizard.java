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
package org.rhq.enterprise.gui.coregui.client.inventory.groups.wizard;

import java.util.ArrayList;
import java.util.List;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.widgets.IButton;

import org.rhq.core.domain.resource.group.ResourceGroup;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.LinkManager;
import org.rhq.enterprise.gui.coregui.client.components.wizard.AbstractWizard;
import org.rhq.enterprise.gui.coregui.client.components.wizard.WizardStep;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.gwt.ResourceGroupGWTServiceAsync;
import org.rhq.enterprise.gui.coregui.client.inventory.groups.ResourceGroupListView;
import org.rhq.enterprise.gui.coregui.client.util.message.Message;

/**
 * A wizard for creating a new Resource group.
 *
 * @author Greg Hinkle
 */
public class GroupCreateWizard extends AbstractWizard {
    private ResourceGroupListView resourceGroupListView;

    private GroupCreateStep createStep;
    private GroupMembersStep memberStep;

    public GroupCreateWizard(ResourceGroupListView resourceGroupListView) {
        this.resourceGroupListView = resourceGroupListView;

        List<WizardStep> steps = new ArrayList<WizardStep>();

        steps.add(createStep = new GroupCreateStep());
        steps.add(memberStep = new GroupMembersStep(this));
        setSteps(steps);
    }

    public String getWindowTitle() {
        return "Create Group";
    }

    public String getTitle() {
        return "Create Group";
    }

    public String getSubtitle() {
        return null;
    }

    public List<IButton> getCustomButtons(int step) {
        return null;
    }

    public void cancel() {
        // Nothing to do.  Group is persisted after the "Finish" button.
    }

    public boolean createGroup() {

        ResourceGroupGWTServiceAsync groupService = GWTServiceLookup.getResourceGroupService();

        groupService.createResourceGroup(createStep.getGroup(), memberStep.getSelectedResourceIds(),
            new AsyncCallback<ResourceGroup>() {
                public void onFailure(Throwable caught) {
                    CoreGUI.getErrorHandler().handleError("Failed to create Resource group.", caught);
                }

                public void onSuccess(ResourceGroup result) {
                    String groupUrl = LinkManager.getResourceGroupLink(result.getId());
                    String conciseMessage = "Resource group created. [<a href='" + groupUrl + "'>View Group</a>]";
                    String detailedMessage =
                        "Created new " + result.getGroupCategory().name().toLowerCase() + " Resource group '"
                            + result.getName() + "' with " + memberStep.getSelectedResourceIds().length + " members.";
                    CoreGUI.getMessageCenter().notify(new Message(conciseMessage, detailedMessage));
                    resourceGroupListView.refresh();
                }
            });

        return true;
    }
}
