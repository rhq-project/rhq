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
package org.rhq.enterprise.gui.coregui.client.inventory.resource.factory;

import com.smartgwt.client.widgets.Canvas;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.enterprise.gui.coregui.client.components.HeaderLabel;
import org.rhq.enterprise.gui.coregui.client.components.configuration.ConfigurationEditor;
import org.rhq.enterprise.gui.coregui.client.components.wizard.AbstractWizardStep;
import org.rhq.enterprise.gui.coregui.client.util.selenium.Locatable;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableVLayout;

/**
 * @author Jay Shaughnessy
 * @author Greg Hinkle
 */
public class ResourceFactoryConfigurationStep extends AbstractWizardStep {

    private boolean noConfigurationNeeded = false; // if true, it has been determined the user doesn't have to set any config
    private ConfigurationEditor editor;
    AbstractResourceFactoryWizard wizard;

    public ResourceFactoryConfigurationStep(AbstractResourceFactoryWizard wizard) {
        this.wizard = wizard;
    }

    public Canvas getCanvas(Locatable parent) {
        if (editor == null) {

            ConfigurationDefinition def = wizard.getNewResourceConfigurationDefinition();
            if (def != null) {
                Configuration startingConfig = wizard.getNewResourceStartingConfiguration();
                if (parent != null) {
                    editor = new ConfigurationEditor(parent.extendLocatorId("ResourceFactoryConfig"), def,
                        startingConfig);
                } else {
                    editor = new ConfigurationEditor("ResourceFactoryConfig", def, startingConfig);
                }
            } else {
                // there is no configuration to edit, just return a static message indicating that there is nothing to do
                noConfigurationNeeded = true;
                LocatableVLayout layout = new LocatableVLayout("noConfigMsgLayout");
                layout.setMargin(Integer.valueOf(20));
                layout.setWidth100();
                layout.setHeight(10);
                HeaderLabel label = new HeaderLabel(MSG.widget_resourceFactoryWizard_editConfigStep_nothingToDo());
                label.setWidth100();
                layout.addMember(label);
                return layout;
            }
        }
        return editor;
    }

    public boolean nextPage() {
        if (noConfigurationNeeded == true || (editor != null && editor.validate())) {
            wizard.setNewResourceConfiguration((noConfigurationNeeded) ? null : editor.getConfiguration());
            wizard.execute();
            return true;
        }

        return false;
    }

    public String getName() {
        return MSG.widget_resourceFactoryWizard_editConfigStepName();
    }
}