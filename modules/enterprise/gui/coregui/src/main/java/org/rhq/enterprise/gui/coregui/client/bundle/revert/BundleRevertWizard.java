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
package org.rhq.enterprise.gui.coregui.client.bundle.revert;

import java.util.ArrayList;
import java.util.List;

import org.rhq.core.domain.bundle.BundleDestination;
import org.rhq.enterprise.gui.coregui.client.components.wizard.WizardStep;

public class BundleRevertWizard extends AbstractBundleRevertWizard {

    // Revert existing destination
    public BundleRevertWizard(BundleDestination destination) {
        if (null == destination) {
            throw new IllegalArgumentException("destination is null");
        }

        this.setDestination(destination);

        List<WizardStep> steps = init();
        steps.add(new ConfirmationStep(this));
        steps.add(new GetRevertInfoStep(this));
        steps.add(new RevertStep(this));
    }

    private List<WizardStep> init() {
        setWindowTitle(MSG.view_bundle_revertWizard_windowTitle());
        setTitle(MSG.view_bundle_revertWizard_title());

        ArrayList<WizardStep> steps = new ArrayList<WizardStep>();
        setSteps(steps);
        return steps;
    }

    public void cancel() {
        // nothing to do, this wizard does not create any entities prior to the scheduleRevert call. The
        // BundleDeployment returned in the revert step is already in progress and must be further handled
        // only by the GUI.
        return;
    }
}
