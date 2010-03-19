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

import com.smartgwt.client.widgets.IButton;
import com.smartgwt.client.widgets.events.ClickEvent;
import com.smartgwt.client.widgets.events.ClickHandler;

import org.rhq.core.domain.bundle.Bundle;
import org.rhq.core.domain.bundle.BundleDeployDefinition;
import org.rhq.core.domain.bundle.BundleVersion;
import org.rhq.enterprise.gui.coregui.client.components.wizard.WizardStep;

public class BundleDeployWizard extends AbstractBundleDeployWizard {

    public BundleDeployWizard() {
        List<WizardStep> steps = init();

        steps.add(new SelectBundleStep(this));
        steps.add(new SelectBundleVersionStep(this));
        steps.add(new BundleDeployDefinitionInfoStep(this));
        steps.add(new SelectTemplateStep(this));
        steps.add(new CreateConfigStep(this));
        steps.add(new DeployNowStep(this));
        steps.add(new SelectPlatformsStep(this));
        steps.add(new DeployStep(this));
    }

    public BundleDeployWizard(Bundle bundle) {
        List<WizardStep> steps = init();
        setBundle(bundle);

        steps.add(new SelectBundleVersionStep(this));
        steps.add(new BundleDeployDefinitionInfoStep(this));
        steps.add(new SelectTemplateStep(this));
        steps.add(new CreateConfigStep(this));
        steps.add(new DeployNowStep(this));
        steps.add(new SelectPlatformsStep(this));
        steps.add(new DeployStep(this));
    }

    public BundleDeployWizard(Bundle bundle, BundleVersion bundleVersion) {
        List<WizardStep> steps = init();
        setBundle(bundle);
        setBundleVersion(bundleVersion);

        steps.add(new BundleDeployDefinitionInfoStep(this));
        steps.add(new SelectTemplateStep(this));
        steps.add(new CreateConfigStep(this));
        steps.add(new DeployNowStep(this));
        steps.add(new SelectPlatformsStep(this));
        steps.add(new DeployStep(this));
    }

    public BundleDeployWizard(Bundle bundle, BundleVersion bundleVersion, BundleDeployDefinition bundleDeployDefinition) {
        List<WizardStep> steps = init();
        setBundle(bundle);
        setBundleVersion(bundleVersion);
        setBundleDeployDefinition(bundleDeployDefinition);
        setNewDefinition(Boolean.FALSE);

        steps.add(new SelectTemplateStep(this));
        steps.add(new CreateConfigStep(this));
        steps.add(new DeployNowStep(this));
        steps.add(new SelectPlatformsStep(this));
        steps.add(new DeployStep(this));
    }

    private List<WizardStep> init() {
        setWindowTitle("Bundle Deployment Wizard");
        setTitle("Bundle Deployment");

        ArrayList<WizardStep> steps = new ArrayList<WizardStep>();
        setSteps(steps);
        return steps;
    }

    @Override
    public void startBundleWizard() {
        super.startBundleWizard();
        IButton cancelButton = getView().getCancelButton();
        cancelButton.addClickHandler(new ClickHandler() {
            public void onClick(ClickEvent clickEvent) {
                BundleDeployDefinition bdd = getBundleDeployDefinition();
                if (bdd != null && isNewDefinition()) {
                    // the user must have created it already after verification step, delete it
                }
            }
        });
    }

}
