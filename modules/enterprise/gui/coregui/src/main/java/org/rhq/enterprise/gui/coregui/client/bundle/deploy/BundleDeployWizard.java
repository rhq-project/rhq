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
import org.rhq.core.domain.bundle.BundleVersion;
import org.rhq.enterprise.gui.coregui.client.components.wizard.WizardStep;

public class BundleDeployWizard extends AbstractBundleDeployWizard {

    public BundleDeployWizard() {
        List<WizardStep> steps = init();
        steps.add(new SelectDestinationStep(this));
        steps.add(new SelectBundleStep(this));
        steps.add(new SelectBundleVersionStep(this));
        steps.add(new GetDeploymentInfoStep(this));
        steps.add(new GetDeploymentConfigStep(this));
        steps.add(new DeployStep(this));
    }

    public BundleDeployWizard(Integer platformGroupId) {
        this.setPlatformGroupId(platformGroupId);

        List<WizardStep> steps = init();
        if (null == this.getPlatformGroupId()) {
            steps.add(new SelectDestinationStep(this));
        }
        steps.add(new SelectBundleStep(this));
        steps.add(new SelectBundleVersionStep(this));
        steps.add(new GetDeploymentInfoStep(this));
        steps.add(new GetDeploymentConfigStep(this));
        steps.add(new DeployStep(this));
    }

    public BundleDeployWizard(Integer platformGroupId, Integer bundleId) {
        this.setPlatformGroupId(platformGroupId);
        this.setBundleId(bundleId);

        List<WizardStep> steps = init();
        if (null == this.getPlatformGroupId()) {
            steps.add(new SelectDestinationStep(this));
        }
        if (null == this.getBundleId()) {
            steps.add(new SelectBundleStep(this));
        }
        steps.add(new SelectBundleVersionStep(this));
        steps.add(new GetDeploymentInfoStep(this));
        steps.add(new GetDeploymentConfigStep(this));
        steps.add(new DeployStep(this));
    }

    public BundleDeployWizard(Integer platformGroupId, Integer bundleId, BundleVersion bundleVersion) {
        this.setPlatformGroupId(platformGroupId);
        this.setBundleId(bundleId);
        this.setBundleVersion(bundleVersion);

        List<WizardStep> steps = init();
        if (null == this.getPlatformGroupId()) {
            steps.add(new SelectDestinationStep(this));
        }
        if (null == this.getBundleId()) {
            steps.add(new SelectBundleStep(this));
        }
        if (null == this.getBundleVersion()) {
            steps.add(new SelectBundleVersionStep(this));
        }
        steps.add(new GetDeploymentInfoStep(this));
        steps.add(new GetDeploymentConfigStep(this));
        steps.add(new DeployStep(this));
    }

    public BundleDeployWizard(BundleDeployment bundleDeployment) {
        List<WizardStep> steps = init();
        this.setBundleId(bundleDeployment.getBundleVersion().getBundle().getId());
        this.setBundleVersion(bundleDeployment.getBundleVersion());
        this.setBundleDeployment(bundleDeployment);
        setNewDefinition(Boolean.FALSE);

        steps.add(new SelectDestinationStep(this));
        steps.add(new GetDeploymentConfigStep(this));
        steps.add(new DeployStep(this));
    }

    public BundleDeployWizard(Integer platformGroupId, BundleDeployment bundleDeployment) {
        List<WizardStep> steps = init();
        this.setBundleId(bundleDeployment.getBundleVersion().getBundle().getId());
        this.setBundleVersion(bundleDeployment.getBundleVersion());
        this.setBundleDeployment(bundleDeployment);
        setNewDefinition(Boolean.FALSE);

        if (null == this.getPlatformGroupId()) {
            steps.add(new SelectDestinationStep(this));
        }
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
        if (bd != null && isNewDefinition()) {
            // the user must have created it already after verification step, delete it
            // TODO
        }
    }

}
