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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;

import com.smartgwt.client.types.Alignment;
import com.smartgwt.client.types.TitleOrientation;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.form.fields.FormItem;
import com.smartgwt.client.widgets.form.fields.RadioGroupItem;
import com.smartgwt.client.widgets.form.fields.SelectItem;
import com.smartgwt.client.widgets.form.fields.SpacerItem;
import com.smartgwt.client.widgets.form.fields.StaticTextItem;
import com.smartgwt.client.widgets.form.fields.events.ChangedEvent;
import com.smartgwt.client.widgets.form.fields.events.ChangedHandler;

import org.rhq.core.domain.drift.DriftDefinitionComparator;
import org.rhq.core.domain.drift.DriftDefinitionTemplate;
import org.rhq.core.domain.drift.DriftDefinitionComparator.CompareMode;
import org.rhq.enterprise.gui.coregui.client.components.wizard.AbstractWizardStep;
import org.rhq.enterprise.gui.coregui.client.util.FormUtility;
import org.rhq.enterprise.gui.coregui.client.util.selenium.Locatable;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableDynamicForm;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableVLayout;

/**
 * @author Jay Shaughnessy
 */
public class DriftPinTemplateWizardInfoStep extends AbstractWizardStep {

    static private final String CREATE_TEMPLATE = "create";
    static private final String SELECT_TEMPLATE = "select";

    private LocatableVLayout canvas;
    private LocatableDynamicForm radioForm;
    private AbstractDriftPinTemplateWizard wizard;

    private RadioGroupItem radioGroupItem;

    private LocatableDynamicForm selectTemplateForm;
    StaticTextItem selectTemplateDescriptionItem = new StaticTextItem("Description", MSG.common_title_description());
    SelectItem selectTemplateItem = new SelectItem("Template", MSG.view_drift_wizard_addDef_templatePrompt());

    public DriftPinTemplateWizardInfoStep(AbstractDriftPinTemplateWizard wizard) {
        this.wizard = wizard;
    }

    public Canvas getCanvas(Locatable parent) {
        if (null == canvas) {
            if (parent != null) {
                canvas = new LocatableVLayout(parent.extendLocatorId("DriftPinTemplateInfo"));
            } else {
                canvas = new LocatableVLayout("DriftPinTemplateInfo");
            }

            canvas.setWidth100();

            radioForm = new LocatableDynamicForm(canvas.extendLocatorId("Radio"));
            radioForm.setNumCols(1);
            radioForm.setWidth100();

            List<FormItem> formItems = new ArrayList<FormItem>();

            LinkedHashMap<String, String> radioGroupValues = new LinkedHashMap<String, String>();
            radioGroupValues.put(CREATE_TEMPLATE, "Pin to New Template (derived from the snapshot's Drift Definition)");
            radioGroupValues.put(SELECT_TEMPLATE, "Pin to an Existing Template");

            radioGroupItem = new RadioGroupItem("options", "Template Selection"); // TODO I18N
            radioGroupItem.setRequired(true);
            radioGroupItem.setAlign(Alignment.LEFT);
            radioGroupItem.setTitleOrientation(TitleOrientation.TOP);
            radioGroupItem.setValueMap(radioGroupValues);
            radioGroupItem.setValue(CREATE_TEMPLATE);
            wizard.setCreateTemplate(true);

            radioGroupItem.addChangedHandler(new ChangedHandler() {
                public void onChanged(ChangedEvent event) {
                    wizard.setCreateTemplate(CREATE_TEMPLATE.equals(event.getValue()));

                    if (wizard.isCreateTemplate()) {
                        selectTemplateForm.hide();
                        wizard.getView().getNextButton().setTitle(MSG.common_button_next());

                    } else {
                        selectTemplateForm.show();
                        wizard.getView().getNextButton().setTitle(MSG.common_button_finish());
                    }
                    radioForm.markForRedraw();
                }
            });
            formItems.add(radioGroupItem);

            formItems.add(new SpacerItem());

            radioForm.setItems(formItems.toArray(new FormItem[formItems.size()]));
            canvas.addMember(radioForm);

            selectTemplateForm = new LocatableDynamicForm(radioForm.extendLocatorId("SelectForm"));
            selectTemplateForm.setNumCols(1);
            selectTemplateForm.setIsGroup(true);
            selectTemplateForm.setGroupTitle("Existing Templates");
            selectTemplateForm.setPadding(10);
            //selectTemplateForm.setMargin(10);
            selectTemplateForm.hide();

            formItems.clear();

            selectTemplateDescriptionItem.setTitleOrientation(TitleOrientation.TOP);
            selectTemplateDescriptionItem.setAlign(Alignment.LEFT);
            selectTemplateDescriptionItem.setWidth(300);
            formItems.add(selectTemplateDescriptionItem);

            SpacerItem spacerItem = new SpacerItem();
            spacerItem.setHeight(10);
            formItems.add(spacerItem);

            selectTemplateItem.setTitleOrientation(TitleOrientation.TOP);
            selectTemplateItem.setAlign(Alignment.LEFT);
            selectTemplateItem.setWidth(300);
            selectTemplateItem.setRequired(true);
            FormUtility.addContextualHelp(selectTemplateItem, MSG.view_drift_wizard_pinTemplate_infoStepHelp());

            Set<DriftDefinitionTemplate> templates = wizard.getResourceType().getDriftDefinitionTemplates();
            final HashMap<String, DriftDefinitionTemplate> templatesMap = new HashMap<String, DriftDefinitionTemplate>(
                templates.size());
            if (!templates.isEmpty()) {
                // Only use templates that have the same base dir and filters as the definition from which
                // this snapshot is coming.  Otherwise the file set does not map.
                DriftDefinitionComparator ddc = new DriftDefinitionComparator(
                    CompareMode.BOTH_BASE_INFO_AND_DIRECTORY_SPECIFICATIONS);
                for (DriftDefinitionTemplate template : templates) {

                    if (0 == ddc.compare(template.getTemplateDefinition(), wizard.getSnapshotDriftDef())) {
                        templatesMap.put(template.getName(), template);
                    }
                }
            } else {
                // there should be at least one template for any resource type that supports drift monitoring
                throw new IllegalStateException(
                    "At least one drift definition template should exist for the resource type");
            }

            Set<String> templatesMapKeySet = templatesMap.keySet();
            String[] templatesMapKeySetArray = templatesMapKeySet.toArray(new String[templatesMap.size()]);
            selectTemplateItem.setValueMap(templatesMapKeySetArray);
            selectTemplateItem.addChangedHandler(new ChangedHandler() {
                public void onChanged(ChangedEvent event) {
                    if (null == event || "".equals(event.getValue())) {
                        return;
                    }

                    setSelectedTemplate((String) event.getValue(), templatesMap);
                }
            });
            formItems.add(selectTemplateItem);
            selectTemplateForm.setItems(formItems.toArray(new FormItem[formItems.size()]));
            canvas.addMember(selectTemplateForm);

            // set value to first in list  
            selectTemplateItem.setValue(templatesMapKeySetArray[0]);
            setSelectedTemplate(templatesMapKeySetArray[0], templatesMap);
        }

        return canvas;
    }

    private void setSelectedTemplate(String key, final HashMap<String, DriftDefinitionTemplate> templatesMap) {

        DriftDefinitionTemplate selectedTemplate = templatesMap.get(key);
        wizard.setSelectedTemplate(selectedTemplate);
        String description = selectedTemplate.getDescription();
        description = (null == description) ? MSG.common_val_none() : description;
        selectTemplateForm.getItem("Description").setValue(description);
    }

    public boolean nextPage() {
        if (!radioForm.validate()) {
            return false;
        }

        return true;
    }

    public String getName() {
        return MSG.view_drift_wizard_pinTemplate_infoStepName();
    }
}
