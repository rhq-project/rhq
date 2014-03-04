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
package org.rhq.coregui.client.inventory.groups.wizard;

import java.util.ArrayList;
import java.util.List;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.widgets.IButton;

import org.rhq.core.domain.resource.group.ResourceGroup;
import org.rhq.coregui.client.CoreGUI;
import org.rhq.coregui.client.components.wizard.AbstractWizard;
import org.rhq.coregui.client.components.wizard.WizardStep;
import org.rhq.coregui.client.gwt.GWTServiceLookup;
import org.rhq.coregui.client.gwt.ResourceGroupGWTServiceAsync;
import org.rhq.coregui.client.util.message.Message;

/**
 * A wizard for creating a new Resource group. This is an abstract base class that
 * subclasses can use to perform group creation.
 *
 * @author Greg Hinkle
 * @author John Mazzitelli
 */
public abstract class AbstractGroupCreateWizard extends AbstractWizard {
    protected GroupCreateStep createStep;
    protected GroupMembersStep memberStep;

    public AbstractGroupCreateWizard() {
        List<WizardStep> steps = new ArrayList<WizardStep>();
        steps.add(createStep = new GroupCreateStep());
        steps.add(memberStep = new GroupMembersStep(this));
        setSteps(steps);
    }

    public String getWindowTitle() {
        return MSG.view_groupCreateWizard_windowTitle();
    }

    public String getTitle() {
        return MSG.view_groupCreateWizard_title();
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
        return createGroup(createStep.getGroup(), memberStep.getSelectedResourceIds());
    }

    public boolean createGroup(final ResourceGroup newGroupToCreate, final int[] selectedGroupMembersResourceIds) {

        ResourceGroupGWTServiceAsync groupService = GWTServiceLookup.getResourceGroupService();

        groupService.createResourceGroup(newGroupToCreate, selectedGroupMembersResourceIds,
            new AsyncCallback<ResourceGroup>() {
                public void onFailure(Throwable caught) {
                    String msg = caught.getMessage();
                    CoreGUI.getErrorHandler().handleError(
                        MSG.view_groupCreateWizard_createFailure(newGroupToCreate.getName(), msg), caught);
                }

                public void onSuccess(ResourceGroup result) {
                    String conciseMessage = MSG.view_groupCreateWizard_createSuccessful_concise(result.getName());
                    String detailedMessage = MSG.view_groupCreateWizard_createSuccessful_full(result.getGroupCategory()
                        .name().toLowerCase(), result.getName(), String.valueOf(selectedGroupMembersResourceIds.length));
                    CoreGUI.getMessageCenter().notify(new Message(conciseMessage, detailedMessage));
                    groupCreateCallback(result);
                }
            });

        return true;
    }

    /**
     * Subclasses can override this in order to perform additional tasks once the group is created.
     * Use this to do things like refresh other parts of the UI that need to see the new group
     * that was created.
     *
     * @param group the new group that was created
     */
    protected void groupCreateCallback(ResourceGroup group) {
        return; // no-op - subclasses can override this method to do things
    }

}
