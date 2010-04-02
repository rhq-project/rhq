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
package org.rhq.enterprise.gui.coregui.client.bundle.deploy;

import java.util.HashSet;

import com.smartgwt.client.util.SC;
import com.smartgwt.client.widgets.Canvas;

import org.rhq.enterprise.gui.coregui.client.components.wizard.WizardStep;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.selection.AbstractSelector;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.selection.ResourceGroupSelector;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.selection.ResourceSelector;

public class DeployTargetStep implements WizardStep {

    private final BundleDeployWizard wizard;

    private AbstractSelector selector;

    public DeployTargetStep(BundleDeployWizard bundleCreationWizard) {
        this.wizard = bundleCreationWizard;
    }

    public String getName() {
        return this.wizard.isResourceDeploy() ? "Select Target Resource" : "Select Target Group";
    }

    public Canvas getCanvas() {
        if ((null == selector) || isMismatch()) {
            selector = this.wizard.isResourceDeploy() ? new ResourceSelector() : new ResourceGroupSelector();
        }

        return selector;
    }

    private boolean isMismatch() {
        return ((selector instanceof ResourceSelector && !this.wizard.isResourceDeploy()) || (selector instanceof ResourceGroupSelector && this.wizard
            .isResourceDeploy()));
    }

    public boolean nextPage() {
        HashSet<Integer> selection = selector.getSelection();
        if (selection.size() != 1) {
            SC.warn("Select only a single target resource for deployment. Use group deploy for multiple targets.");
            return false;
        }

        wizard.setDeployTargetId(selection.iterator().next());
        return true;
    }
}
