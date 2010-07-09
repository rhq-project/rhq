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
import org.rhq.enterprise.gui.coregui.client.components.configuration.ConfigurationEditor;
import org.rhq.enterprise.gui.coregui.client.components.wizard.AbstractWizardStep;

/**
 * @author Greg Hinkle
 */
public class ConfigurationStep extends AbstractWizardStep {

    private ConfigurationEditor editor;
    ResourceFactoryCreateWizard wizard;

    public ConfigurationStep(ResourceFactoryCreateWizard wizard) {
        this.wizard = wizard;
    }

    public Canvas getCanvas() {
        if (editor == null) {

            ConfigurationDefinition def = wizard.getConfigurationDefinition();
            Configuration startingConfig = wizard.getConfiguration();

            editor = new ConfigurationEditor(def, startingConfig);
        }
        return editor;
    }

    public boolean nextPage() {
        return true;
    }

    public String getName() {
        return "Edit Configuration";
    }

    public Configuration getConfiguration() {
        return editor.getConfiguration();
    }
}