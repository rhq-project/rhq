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
package org.rhq.enterprise.gui.coregui.client.inventory.resource.detail.operation.create;

import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.HTMLFlow;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.core.domain.operation.OperationDefinition;
import org.rhq.enterprise.gui.coregui.client.components.configuration.ConfigurationEditor;
import org.rhq.enterprise.gui.coregui.client.components.wizard.AbstractWizardStep;

/**
 * @author Greg Hinkle
 */
public class OperationParametersStep extends AbstractWizardStep {

    private OperationDefinition definition;

    private ConfigurationEditor configurationEditor;

    public OperationParametersStep(OperationDefinition operationDefinition) {
        super();
        this.definition = operationDefinition;
    }

    public Canvas getCanvas() {
        if (definition.getParametersConfigurationDefinition() != null) {
            if (configurationEditor == null) {
                ConfigurationDefinition configurationDefinition = definition.getParametersConfigurationDefinition();
                Configuration defaultConfiguration = configurationDefinition.getDefaultTemplate() != null ? configurationDefinition
                    .getDefaultTemplate().createConfiguration()
                    : new Configuration();
                configurationEditor = new ConfigurationEditor("OperationParams", configurationDefinition,
                    defaultConfiguration);
            }
            return configurationEditor;
        } else {
            return new HTMLFlow("This operation does not take any parameters.");
        }
    }

    public boolean nextPage() {
        return true; // TODO: Implement this method.
    }

    public String getName() {
        return "Operation Parameters";
    }

    public Configuration getParameterConfiguration() {
        return configurationEditor != null ? configurationEditor.getConfiguration() : null;
    }
}
