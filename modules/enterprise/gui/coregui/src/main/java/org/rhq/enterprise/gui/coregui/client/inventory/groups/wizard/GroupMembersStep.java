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

import java.util.Set;

import com.smartgwt.client.widgets.Canvas;

import org.rhq.enterprise.gui.coregui.client.components.wizard.AbstractWizardStep;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.selection.ResourceSelector;
import org.rhq.enterprise.gui.coregui.client.util.selenium.Locatable;

/**
 * @author Greg Hinkle
 */
public class GroupMembersStep extends AbstractWizardStep {

    private GroupCreateWizard wizard = null;
    private ResourceSelector selector = null;

    public GroupMembersStep(GroupCreateWizard wizard) {
        this.wizard = wizard;
    }

    public Canvas getCanvas(Locatable parent) {
        if (selector == null) {
            if (parent != null) {
                selector = new ResourceSelector(parent.extendLocatorId("GroupMembers"));
            } else {
                selector = new ResourceSelector("GroupMembers");
            }
        }
        return selector;
    }

    public boolean nextPage() {
        wizard.createGroup();
        return true; // last step
    }

    public String getName() {
        return MSG.view_groupCreateWizard_membersStepName();
    }

    public int[] getSelectedResourceIds() {
        Set<Integer> selectedIds = selector.getSelection();
        int[] selection = new int[selectedIds.size()];
        int i = 0;
        for (Integer id : selectedIds) {
            selection[i++] = id;
        }
        return selection;
    }
}
