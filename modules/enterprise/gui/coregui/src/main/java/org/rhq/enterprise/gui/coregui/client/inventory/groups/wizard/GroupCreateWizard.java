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

import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.resource.group.ResourceGroup;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.components.wizard.AbstractWizard;
import org.rhq.enterprise.gui.coregui.client.components.wizard.WizardStep;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.gwt.ResourceGroupGWTServiceAsync;
import org.rhq.enterprise.gui.coregui.client.util.message.Message;

/**
 * @author Greg Hinkle
 */
public class GroupCreateWizard extends AbstractWizard {

    private GroupCreateStep createStep;
    private GroupMembersStep memberStep;

    public GroupCreateWizard() {
        ArrayList<WizardStep> steps = new ArrayList<WizardStep>();

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
        return null; // TODO: Implement this method.
    }

    public List<IButton> getCustomButtons(int step) {
        return null; // TODO: Implement this method.
    }

    /** 
     * @return only the rt id is set
     */
    public ResourceType getCompatibleGroupResourceType() {
        ResourceGroup rg = createStep.getGroup();
        ResourceType rt = null;
        if (null != rg) {
            rt = rg.getResourceType();
        }
        return rt;
    }

    public void cancel() {
        // Nothing to do.  Group is persisted after the "Finish" button.
    }

    public boolean createGroup() {

        ResourceGroupGWTServiceAsync groupService = GWTServiceLookup.getResourceGroupService();

        groupService.createResourceGroup(createStep.getGroup(), memberStep.getSelectedResourceIds(),
            new AsyncCallback<ResourceGroup>() {
                public void onFailure(Throwable caught) {
                    CoreGUI.getErrorHandler().handleError("Failed to create resource group", caught);
                }

                public void onSuccess(ResourceGroup result) {
                    CoreGUI.getMessageCenter().notify(
                        new Message("Saved new group " + result.getName(), Message.Severity.Info));

                    // try and get the new group reflected in the UI.
                    CoreGUI.refresh();
                }
            });

        return true;
    }
}
