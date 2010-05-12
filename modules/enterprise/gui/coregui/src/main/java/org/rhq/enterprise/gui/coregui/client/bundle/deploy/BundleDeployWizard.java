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

import java.util.ArrayList;
import java.util.List;

import org.rhq.core.domain.bundle.BundleDeployment;
import org.rhq.enterprise.gui.coregui.client.components.wizard.WizardStep;

public class BundleDeployWizard extends AbstractBundleDeployWizard {

    // New Deployment, No Bundle Selected
    public BundleDeployWizard() {
        this.setNewDestination(true);

        List<WizardStep> steps = init();
        steps.add(new SelectBundleStep(this));
        steps.add(new GetDestinationStep(this));
        steps.add(new SelectBundleVersionStep(this));
        steps.add(new GetDeploymentInfoStep(this));
        steps.add(new GetDeploymentConfigStep(this));
        steps.add(new DeployStep(this));
    }

    // New Deployment, Seeded with Bundle
    public BundleDeployWizard(Integer bundleId) {
        if (null == bundleId) {
            throw new IllegalArgumentException("bundleId is null");
        }

        this.setNewDestination(true);
        this.setBundleId(bundleId);

        List<WizardStep> steps = init();
        steps.add(new GetDestinationStep(this));
        steps.add(new SelectBundleVersionStep(this));
        steps.add(new GetDeploymentInfoStep(this));
        steps.add(new GetDeploymentConfigStep(this));
        steps.add(new DeployStep(this));
    }

    // Redeploy
    public BundleDeployWizard(Integer bundleId, Integer destinationId) {
        if (null == destinationId) {
            throw new IllegalArgumentException("destinationId is null");
        }

        this.setBundleId(bundleId);
        this.setDestinationId(destinationId);

        List<WizardStep> steps = init();
        steps.add(new SelectBundleVersionStep(this));
        steps.add(new GetDeploymentInfoStep(this));
        steps.add(new GetDeploymentConfigStep(this));
        steps.add(new DeployStep(this));
    }

    private List<WizardStep> init() {
        setWindowTitle("Bundle Deployment Wizard");
        setTitle("Bundle Deployment");

        ArrayList<WizardStep> steps = new ArrayList<WizardStep>();
        setSteps(steps);
        return steps;
    }

    public void cancel() {
        BundleDeployment bd = getBundleDeployment();
        if (bd != null && isNewDestination()) {
            // the user must have created it already after verification step, delete it
            // TODO
        }
    }

}
