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
package org.rhq.enterprise.gui.coregui.client.bundle.create;

import java.util.ArrayList;

import org.rhq.enterprise.gui.coregui.client.components.wizard.WizardStep;

public class BundleCreateWizard extends AbstractBundleCreateWizard {

    public BundleCreateWizard() {
        setWindowTitle(MSG.view_bundle_createWizard_windowTitle());
        setTitle(MSG.view_bundle_createWizard_title());

        ArrayList<WizardStep> steps = new ArrayList<WizardStep>();
        steps.add(new BundleUploadDistroFileStep(this));
        steps.add(new BundleUploadDataStep(this));
        steps.add(new BundleSummaryStep(this));
        setSteps(steps);
    }
}
