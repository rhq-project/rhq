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

import java.util.Map;

import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.form.DynamicForm;
import com.smartgwt.client.widgets.form.fields.SelectItem;
import com.smartgwt.client.widgets.form.fields.TextItem;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.core.domain.configuration.definition.ConfigurationTemplate;
import org.rhq.enterprise.gui.coregui.client.components.wizard.AbstractWizardStep;

/**
 * @author Greg Hinkle
 */
public class ConfigurationTemplateStep extends AbstractWizardStep {

    private DynamicForm form;
    private ResourceFactoryCreateWizard wizard;
    private Map<String, ConfigurationTemplate> templates;

    public ConfigurationTemplateStep(ResourceFactoryCreateWizard wizard) {
        this.wizard = wizard;
    }

    public Canvas getCanvas() {
        if (form == null) {

            form = getDynamicForm();

            TextItem nameItem = new TextItem("resourceName", "Resource Name");
            nameItem.setRequired(true);

            SelectItem templateSelect = new SelectItem("template", "Template");

            ConfigurationDefinition definition = wizard.getConfigurationDefinition();

            templates = definition.getTemplates();

            if (templates != null && !templates.isEmpty()) {
                templateSelect.setValueMap(templates.keySet().toArray(new String[templates.size()]));
            } else {
                templateSelect.setDisabled(true);
            }

            form.setItems(nameItem, templateSelect);

        }
        return form;
    }

    public boolean nextPage() {
        return form.validate();
    }

    public String getName() {
        return "New Resource";
    }

    public Configuration getConfiguration() {
        String template = form.getValueAsString("template");
        if (template == null) {
            template = "default";
        }
        return templates.get(template).createConfiguration();
    }

    public String getResourceName() {
        return form.getValueAsString("resourceName");
    }
}
