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

import org.rhq.core.domain.bundle.Bundle;
import org.rhq.enterprise.gui.coregui.client.bundle.BundleSelector;
import org.rhq.enterprise.gui.coregui.client.components.wizard.WizardStep;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.selection.AbstractSelector;

public class SelectBundleStep implements WizardStep {

    private final BundleDeployWizard wizard;

    private AbstractSelector<Bundle> selector;

    public SelectBundleStep(BundleDeployWizard wizard) {
        this.wizard = wizard;
    }

    public String getName() {
        return "Select Deployment Bundle";
    }

    public Canvas getCanvas() {
        this.selector = new BundleSelector();
        return this.selector;
    }

    public boolean nextPage() {
        HashSet<Integer> selection = this.selector.getSelection();
        if (selection.size() != 1) {
            SC.warn("Select only a single bundle for deployment.");
            return false;
        }

        this.wizard.setBundleId(selection.iterator().next());
        return true;
    }
}
