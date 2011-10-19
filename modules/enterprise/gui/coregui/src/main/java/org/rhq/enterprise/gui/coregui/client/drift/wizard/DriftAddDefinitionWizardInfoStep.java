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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.smartgwt.client.types.Alignment;
import com.smartgwt.client.types.TitleOrientation;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.form.fields.FormItem;
import com.smartgwt.client.widgets.form.fields.SelectItem;
import com.smartgwt.client.widgets.form.fields.SpacerItem;
import com.smartgwt.client.widgets.form.fields.StaticTextItem;
import com.smartgwt.client.widgets.form.fields.events.ChangedEvent;
import com.smartgwt.client.widgets.form.fields.events.ChangedHandler;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.definition.ConfigurationTemplate;
import org.rhq.core.domain.drift.DriftDefinitionTemplate;
import org.rhq.enterprise.gui.coregui.client.components.wizard.AbstractWizardStep;
import org.rhq.enterprise.gui.coregui.client.util.FormUtility;
import org.rhq.enterprise.gui.coregui.client.util.selenium.Locatable;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableDynamicForm;

/**
 * @author Jay Shaughnessy
 */
public class DriftAddDefinitionWizardInfoStep extends AbstractWizardStep {

    private LocatableDynamicForm form;
    private AbstractDriftAddDefinitionWizard wizard;
    private Map<String, ConfigurationTemplate> templates;

    public DriftAddDefinitionWizardInfoStep(AbstractDriftAddDefinitionWizard wizard) {
        this.wizard = wizard;
    }

    public Canvas getCanvas(Locatable parent) {
        if (form == null) {

            if (parent != null) {
                form = new LocatableDynamicForm(parent.extendLocatorId("DriftAddDefInfo"));
            } else {
                form = new LocatableDynamicForm("DriftAddDefInfo");
            }
            form.setNumCols(1);
            List<FormItem> formItems = new ArrayList<FormItem>(2);

            StaticTextItem descriptionItem = new StaticTextItem("Description", MSG.common_title_description());
            descriptionItem.setTitleOrientation(TitleOrientation.TOP);
            descriptionItem.setAlign(Alignment.LEFT);
            descriptionItem.setWidth(300);
            formItems.add(descriptionItem);

            SpacerItem spacerItem = new SpacerItem("Spacer");
            formItems.add(spacerItem);

            SelectItem templateSelectItem = new SelectItem("Template", MSG.view_drift_wizard_addDef_templatePrompt());
            templateSelectItem.setTitleOrientation(TitleOrientation.TOP);
            templateSelectItem.setAlign(Alignment.LEFT);
            templateSelectItem.setWidth(300);
            templateSelectItem.setRequired(true);
            switch (wizard.getEntityContext().getType()) {
            case SubsystemView:
                FormUtility.addContextualHelp(templateSelectItem, MSG.view_drift_wizard_addTemplate_infoStepHelp());
                break;

            default:
                FormUtility.addContextualHelp(templateSelectItem, MSG.view_drift_wizard_addDef_infoStepHelp());
            }

            Set<DriftDefinitionTemplate> templates = wizard.getType().getDriftDefinitionTemplates();
            final HashMap<String, DriftDefinitionTemplate> templatesMap = new HashMap<String, DriftDefinitionTemplate>(
                templates.size());
            if (!templates.isEmpty()) {
                for (DriftDefinitionTemplate template : templates) {
                    templatesMap.put(template.getName(), template);
                }
            } else {
                // there should be at least one template for any resource type that supports drift monitoring
                throw new IllegalStateException(
                    "At least one drift definition template should exist for the resource type");
            }

            Set<String> templatesMapKeySet = templatesMap.keySet();
            String[] templatesMapKeySetArray = templatesMapKeySet.toArray(new String[templatesMap.size()]);
            templateSelectItem.setValueMap(templatesMapKeySetArray);
            templateSelectItem.addChangedHandler(new ChangedHandler() {
                public void onChanged(ChangedEvent event) {
                    if (null == event || "".equals(event.getValue())) {
                        return;
                    }

                    setSelectedTemplate((String) event.getValue(), templatesMap);
                }
            });
            formItems.add(templateSelectItem);

            form.setItems(formItems.toArray(new FormItem[formItems.size()]));

            // set value to first in list  
            templateSelectItem.setValue(templatesMapKeySetArray[0]);
            setSelectedTemplate(templatesMapKeySetArray[0], templatesMap);
        }

        return form;
    }

    private void setSelectedTemplate(String key, final HashMap<String, DriftDefinitionTemplate> templatesMap) {

        DriftDefinitionTemplate selectedTemplate = templatesMap.get(key);
        wizard.setSelectedTemplate(selectedTemplate);
        wizard.setNewStartingConfiguration(selectedTemplate.createConfiguration());
        String description = selectedTemplate.getDescription();
        description = (null == description) ? MSG.common_val_none() : description;
        form.getItem("Description").setValue(description);
    }

    public boolean nextPage() {
        return form.validate();
    }

    public String getName() {
        switch (wizard.getEntityContext().getType()) {
        case SubsystemView:
            return MSG.view_drift_wizard_addTemplate_infoStepName();

        default:
            return MSG.view_drift_wizard_addDef_infoStepName();
        }
    }

    public Configuration getStartingConfiguration() {
        String template = form.getValueAsString("template");
        return templates.get(template).createConfiguration();
    }
}
