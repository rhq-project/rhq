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

    public GetDeploymentConfigStep(BundleDeployWizard wizard) {
        this.wizard = wizard;
    }

    public String getName() {
        return "Set Deployment Configuration";
    }

    public Canvas getCanvas() {
        if (null == editor) {
            ConfigurationDefinition configDef = this.wizard.getBundleVersion().getConfigurationDefinition();

            // if there are no prop defs for this config def then we can skip this step entirely. just
            // set an empty config.
            if (configDef.getPropertyDefinitions().isEmpty()) {
                this.wizard.setNewDeploymentConfig(new Configuration());
                this.wizard.getView().incrementStep();
            } else {
                // otherwise, pop up the config editor to get the needed config
                Configuration startingConfig = (null == this.wizard.getLiveDeployment()) ? new Configuration()
                    : getNormalizedLiveConfig(configDef);
                editor = new ConfigurationEditor(configDef, startingConfig);
            }
        }

        return editor;
    }

    private Configuration getNormalizedLiveConfig(ConfigurationDefinition configDef) {
        Configuration config = this.wizard.getLiveDeployment().getConfiguration();
        if (null == config) {
            config = new Configuration();
        } else {
            config = config.deepCopy(false);
            //TODO: get access to this method, may need to add slsb call
            //      also, may need to enhance this drop unnecessary config (maybe it can just stay around) 
            //ConfigurationUtility.normalizeConfiguration(config, configDef);
        }

        return config;
    }

    public boolean nextPage() {
        wizard.setNewDeploymentConfig(editor.getConfiguration());
        return true;
    }
}
