/*
 * RHQ Management Platform
 * Copyright (C) 2011 Red Hat, Inc.
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
package org.rhq.enterprise.gui.coregui.client.drift.wizard;

import com.smartgwt.client.widgets.Canvas;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.core.domain.drift.DriftConfigurationDefinition;
import org.rhq.enterprise.gui.coregui.client.components.configuration.ConfigurationEditor;
import org.rhq.enterprise.gui.coregui.client.components.wizard.AbstractWizardStep;
import org.rhq.enterprise.gui.coregui.client.util.selenium.Locatable;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableVLayout;

/**
 * @author Jay Shaughnessy
 */
public class DriftAddConfigWizardConfigStep extends AbstractWizardStep {

    private LocatableVLayout vLayout;
    private ConfigurationEditor editor;
    AbstractDriftAddConfigWizard wizard;

    public DriftAddConfigWizardConfigStep(AbstractDriftAddConfigWizard wizard) {
        this.wizard = wizard;
    }

    public Canvas getCanvas(Locatable parent) {
        // This VLayout isn't really necessary at the moment, but provides for easier expansion if we add more items
        if (vLayout == null) {
            String locatorId = (null == parent) ? "DriftConfig" : parent.extendLocatorId("DriftConfig");
            vLayout = new LocatableVLayout(locatorId);

            ConfigurationDefinition def = DriftConfigurationDefinition.getInstance();
            Configuration startingConfig = wizard.getNewStartingConfiguration();
            editor = new ConfigurationEditor(vLayout.extendLocatorId("Editor"), def, startingConfig);
            vLayout.addMember(editor);
        }

        return vLayout;
    }

    public boolean nextPage() {
        if (editor != null && editor.validate()) {
            wizard.setNewConfiguration(editor.getConfiguration());
            wizard.execute();
            return true;
        }

        return false;
    }

    public String getName() {
        return MSG.widget_resourceFactoryWizard_editConfigStepName();
    }
}