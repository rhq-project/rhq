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

import com.smartgwt.client.widgets.Canvas;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.enterprise.gui.coregui.client.components.configuration.ConfigurationEditor;
import org.rhq.enterprise.gui.coregui.client.components.wizard.WizardStep;

public class GetDeploymentConfigStep implements WizardStep {

    private final BundleDeployWizard wizard;
    private ConfigurationEditor editor;

    public GetDeploymentConfigStep(BundleDeployWizard bundleCreationWizard) {
        this.wizard = bundleCreationWizard;
    }

    public String getName() {
        return "Set Deployment Configuration";
    }

    public Canvas getCanvas() {
        if (null == editor) {
            ConfigurationDefinition configDef = wizard.getBundleVersion().getConfigurationDefinition();
            Configuration startingConfig = (null != this.wizard.getTemplate()) ? this.wizard.getTemplate()
                .getConfiguration() : new Configuration();

            // if there are no prop defs for this config def then we can skip this step entirely. just
            // set an empty config.
            if (configDef.getPropertyDefinitions().isEmpty()) {
                this.wizard.setConfig(startingConfig);
                this.wizard.getView().incrementStep();
            }

            // otherwise, pop up the config editor to get the needed config
            editor = new ConfigurationEditor(configDef, startingConfig);
        }

        return editor;
    }

    public boolean nextPage() {
        wizard.setConfig(editor.getConfiguration());
        return true;
    }
}
