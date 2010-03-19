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

public class CreateConfigStep implements WizardStep {

    private final BundleDeployWizard wizard;
    // private final BundleGWTServiceAsync bundleServer = GWTServiceLookup.getBundleService();
    private ConfigurationEditor editor;

    public CreateConfigStep(BundleDeployWizard bundleCreationWizard) {
        this.wizard = bundleCreationWizard;
    }

    public String getName() {
        return "Set Deployment Configuration";
    }

    public Canvas getCanvas() {
        if (editor == null) {
            ConfigurationDefinition configDef = wizard.getBundleVersion().getConfigurationDefinition();
            Configuration startingConfig = null;
            if (null != wizard.getTemplate()) {
                startingConfig = wizard.getTemplate().getConfiguration();
            }

            editor = new ConfigurationEditor(configDef, startingConfig);
        }

        return editor;
    }

    public boolean nextPage() {
        wizard.setConfig(editor.getConfiguration());
        return true;
    }

    public boolean isPreviousEnabled() {
        return true;
    }

    public boolean isNextEnabled() {
        return true;
    }
}
