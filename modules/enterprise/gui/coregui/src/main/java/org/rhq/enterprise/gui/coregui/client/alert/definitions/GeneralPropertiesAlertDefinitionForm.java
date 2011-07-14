/*
 * RHQ Management Platform
 * Copyright (C) 2005-2010 Red Hat, Inc.
 * All rights reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2, as
 * published by the Free Software Foundation, and/or the GNU Lesser
 * General Public License, version 2.1, also as published by the Free
 * Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License and the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License
 * and the GNU Lesser General Public License along with this program;
 * if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */

package org.rhq.enterprise.gui.coregui.client.alert.definitions;

import java.util.LinkedHashMap;

import com.smartgwt.client.widgets.form.fields.RadioGroupItem;
import com.smartgwt.client.widgets.form.fields.SelectItem;
import com.smartgwt.client.widgets.form.fields.StaticTextItem;

import org.rhq.core.domain.alert.AlertDefinition;
import org.rhq.core.domain.alert.AlertPriority;
import org.rhq.enterprise.gui.coregui.client.ImageManager;
import org.rhq.enterprise.gui.coregui.client.components.form.SimpleEditableTextAreaFormItem;
import org.rhq.enterprise.gui.coregui.client.components.form.SimpleEditableFormItem;
import org.rhq.enterprise.gui.coregui.client.components.form.StringLengthValidator;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableDynamicForm;

/**
 * @author John Mazzitelli
 */
public class GeneralPropertiesAlertDefinitionForm extends LocatableDynamicForm implements EditAlertDefinitionForm {

    private AlertDefinition alertDefinition;

    private SimpleEditableFormItem nameField;
    private SimpleEditableTextAreaFormItem descriptionField;
    private SelectItem prioritySelection;
    private RadioGroupItem enabledSelection;
    private RadioGroupItem readOnlySelection;

    private StaticTextItem priorityStatic;
    private StaticTextItem enabledStatic;
    private StaticTextItem readOnlyStatic;

    private boolean formBuilt = false;

    public GeneralPropertiesAlertDefinitionForm(String locatorId) {
        this(locatorId, null);
    }

    public GeneralPropertiesAlertDefinitionForm(String locatorId, AlertDefinition alertDefinition) {
        super(locatorId);
        this.alertDefinition = alertDefinition;
    }

    @Override
    protected void onInit() {
        super.onInit();

        if (!formBuilt) {
            buildForm();
            setAlertDefinition(alertDefinition);
            makeViewOnly();
        }
    }

    @Override
    public AlertDefinition getAlertDefinition() {
        return alertDefinition;
    }

    @Override
    public void setAlertDefinition(AlertDefinition alertDef) {
        alertDefinition = alertDef;

        buildForm();

        if (alertDef == null) {
            clearFormValues();
        } else {
            nameField.setValue(alertDef.getName());

            descriptionField.setValue(alertDef.getDescription());

            prioritySelection.setValue(alertDef.getPriority().name());
            priorityStatic.setValue(alertDef.getPriority().name());

            enabledSelection.setValue(alertDef.getEnabled() ? "yes" : "no");
            enabledStatic.setValue(alertDef.getEnabled() ? MSG.common_val_yes() : MSG.common_val_no());

            readOnlySelection.setValue(alertDef.isReadOnly() ? "yes" : "no");
            readOnlyStatic.setValue(alertDef.isReadOnly() ? MSG.common_val_yes() : MSG.common_val_no());
        }

        markForRedraw();
    }

    @Override
    public void makeEditable() {
        nameField.switchToEditMode();

        descriptionField.switchToEditMode();

        prioritySelection.show();
        priorityStatic.hide();

        enabledSelection.show();
        enabledStatic.hide();

        readOnlySelection.show();
        readOnlyStatic.hide();

        if (alertDefinition != null) {
            Integer parentId = alertDefinition.getParentId();
            if ((parentId == null || parentId.intValue() == 0) && (alertDefinition.getGroupAlertDefinition() == null)) {
                readOnlySelection.hide();
            } else {
                readOnlySelection.show();
            }
        } else {
            readOnlySelection.hide();
        }
        readOnlyStatic.hide();

        markForRedraw();
    }

    @Override
    public void makeViewOnly() {
        nameField.switchToStaticMode();

        descriptionField.switchToStaticMode();

        prioritySelection.hide();
        priorityStatic.show();

        enabledSelection.hide();
        enabledStatic.show();

        readOnlySelection.hide();
        readOnlyStatic.show();

        if (alertDefinition != null) {
            Integer parentId = alertDefinition.getParentId();
            if ((parentId == null || parentId.intValue() == 0) && (alertDefinition.getGroupAlertDefinition() == null)) {
                readOnlyStatic.hide();
            } else {
                readOnlyStatic.show();
            }
        } else {
            readOnlyStatic.hide();
        }
        readOnlySelection.hide();

        markForRedraw();
    }

    @Override
    public void saveAlertDefinition() {
        alertDefinition.setName(nameField.getValue().toString());
        Object description = descriptionField.getValue();
        if (null != description) {
            alertDefinition.setDescription(description.toString());
        }

        String prioritySelected = prioritySelection.getValue().toString();
        alertDefinition.setPriority(AlertPriority.valueOf(prioritySelected));

        alertDefinition.setEnabled("yes".equals(enabledSelection.getValue()));
        alertDefinition.setReadOnly("yes".equals(readOnlySelection.getValue()));
    }

    @Override
    public void clearFormValues() {
        nameField.clearValue();
        descriptionField.clearValue();
        prioritySelection.clearValue();
        enabledSelection.clearValue();
        readOnlySelection.clearValue();

        priorityStatic.clearValue();
        enabledStatic.clearValue();
        readOnlyStatic.clearValue();

        markForRedraw();
    }

    private void buildForm() {
        if (!formBuilt) {
            nameField = new SimpleEditableFormItem("name", MSG.common_title_name());
            nameField.setWidth(300);
            StringLengthValidator notEmptyValidator = new StringLengthValidator(1, null, false);
            // ensure these are executed in a validate() override
            nameField.setValidators(notEmptyValidator);

            descriptionField = new SimpleEditableTextAreaFormItem("description", MSG.common_title_description());
            descriptionField.setWidth(300);
            descriptionField.setDefaultValue("");

            prioritySelection = new SelectItem("priority", MSG.view_alerts_field_priority());
            LinkedHashMap<String, String> priorities = new LinkedHashMap<String, String>(3);
            priorities.put(AlertPriority.HIGH.name(), MSG.common_alert_high());
            priorities.put(AlertPriority.MEDIUM.name(), MSG.common_alert_medium());
            priorities.put(AlertPriority.LOW.name(), MSG.common_alert_low());
            LinkedHashMap<String, String> priorityIcons = new LinkedHashMap<String, String>(3);
            priorityIcons.put(AlertPriority.HIGH.name(), ImageManager.getAlertIcon(AlertPriority.HIGH));
            priorityIcons.put(AlertPriority.MEDIUM.name(), ImageManager.getAlertIcon(AlertPriority.MEDIUM));
            priorityIcons.put(AlertPriority.LOW.name(), ImageManager.getAlertIcon(AlertPriority.LOW));
            prioritySelection.setValueMap(priorities);
            prioritySelection.setValueIcons(priorityIcons);
            prioritySelection.setDefaultValue(AlertPriority.MEDIUM.name());
            priorityStatic = new StaticTextItem("priorityStatic", MSG.view_alerts_field_priority());
            priorityStatic.setValueIcons(priorityIcons);

            enabledSelection = new RadioGroupItem("enabled", MSG.view_alerts_field_enabled());
            LinkedHashMap<String, String> enabledYesNo = new LinkedHashMap<String, String>(2);
            enabledYesNo.put("yes", MSG.common_val_yes());
            enabledYesNo.put("no", MSG.common_val_no());
            enabledSelection.setValueMap(enabledYesNo);
            enabledSelection.setDefaultValue("yes");
            enabledStatic = new StaticTextItem("enabledStatic", MSG.view_alerts_field_enabled());

            readOnlySelection = new RadioGroupItem("readOnly", MSG.view_alerts_field_protected());
            LinkedHashMap<String, String> readOnlyYesNo = new LinkedHashMap<String, String>(2);
            readOnlyYesNo.put("yes", MSG.common_val_yes());
            readOnlyYesNo.put("no", MSG.common_val_no());
            readOnlySelection.setValueMap(readOnlyYesNo);
            readOnlySelection.setDefaultValue("yes");
            readOnlySelection.setPrompt(MSG.view_alerts_field_protected_tooltip());
            readOnlyStatic = new StaticTextItem("readOnlyStatic", MSG.view_alerts_field_protected());

            setFields(nameField, descriptionField, prioritySelection, priorityStatic, enabledSelection, enabledStatic,
                readOnlySelection, readOnlyStatic);

            formBuilt = true;
        }
    }

    /**
     * Must explicitly force outer-form validatation of EditibleFormItems.   
     */
    @Override
    public boolean validate() {
        return super.validate() && nameField.validate();
    }

}
