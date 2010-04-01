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

import java.util.LinkedHashMap;
import java.util.Map;

import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.form.DynamicForm;
import com.smartgwt.client.widgets.form.fields.SelectItem;
import com.smartgwt.client.widgets.form.fields.events.ChangedEvent;
import com.smartgwt.client.widgets.form.fields.events.ChangedHandler;

import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.core.domain.configuration.definition.ConfigurationTemplate;
import org.rhq.enterprise.gui.coregui.client.components.wizard.WizardStep;

public class SelectTemplateStep implements WizardStep {

    private final BundleDeployWizard wizard;
    private DynamicForm form;

    private SelectItem selectTemplateItem = null;
    private LinkedHashMap<String, String> selectTemplateValues = new LinkedHashMap<String, String>();
    private ConfigurationDefinition definition = null;
    private Map<String, ConfigurationTemplate> templates = null;

    public SelectTemplateStep(BundleDeployWizard bundleDeployWizard) {
        this.wizard = bundleDeployWizard;
    }

    public String getName() {
        return "Select Configuration Template";
    }

    public Canvas getCanvas() {
        if (form == null) {
            form = new DynamicForm();
            form.setWidth100();
            form.setNumCols(2);
            form.setColWidths("50%", "*");

            definition = wizard.getBundleVersion().getConfigurationDefinition();
            templates = definition.getTemplates();

            if (templates != null && !templates.isEmpty()) {
                for (String templateName : templates.keySet()) {
                    selectTemplateValues.put(templateName, templateName);
                }

                selectTemplateItem = new SelectItem("selectTemplate", "Configuration Template");
                selectTemplateItem.setValueMap(selectTemplateValues);
                selectTemplateItem.addChangedHandler(new ChangedHandler() {
                    public void onChanged(ChangedEvent event) {
                        wizard.setTemplate((ConfigurationTemplate) templates.get(event.getValue()));
                    }
                });
            } else {
                selectTemplateItem = new SelectItem("selectTemplate", "No Configuration Templates Defined");
                selectTemplateItem.setDisabled(true);
            }

            form.setItems(selectTemplateItem);
        }

        this.wizard.getView().getNextButton().setDisabled(false);
        return form;
    }

    public boolean nextPage() {
        return form.validate();
    }
}
