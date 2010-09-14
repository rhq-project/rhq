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
import com.smartgwt.client.widgets.form.fields.TextAreaItem;
import com.smartgwt.client.widgets.form.fields.TextItem;

import org.rhq.core.domain.alert.AlertDefinition;
import org.rhq.core.domain.alert.AlertPriority;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableDynamicForm;

/**
 * @author John Mazzitelli
 */
public class GeneralPropertiesAlertDefinitionForm extends LocatableDynamicForm implements EditAlertDefinitionForm {

    private AlertDefinition alertDefinition;

    private TextItem nameTextField;
    private TextAreaItem descriptionTextField;
    private SelectItem prioritySelection;
    private RadioGroupItem enabledSelection;
    private RadioGroupItem readOnlySelection;

    private StaticTextItem nameStatic;
    private StaticTextItem descriptionStatic;
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
            nameTextField.setValue(alertDef.getName());
            nameStatic.setValue(alertDef.getName());

            descriptionTextField.setValue(alertDef.getDescription());
            descriptionStatic.setValue(alertDef.getDescription());

            prioritySelection.setValue(alertDef.getPriority().name());
            priorityStatic.setValue(alertDef.getPriority().name());

            enabledSelection.setValue(alertDef.getEnabled() ? "Yes" : "No");
            enabledStatic.setValue(alertDef.getEnabled() ? "Yes" : "No");

            readOnlySelection.setValue(alertDef.isReadOnly() ? "Yes" : "No");
            readOnlyStatic.setValue(alertDef.isReadOnly() ? "Yes" : "No");
        }

        markForRedraw();
    }

    @Override
    public void makeEditable() {
        nameTextField.show();
        nameStatic.hide();

        descriptionTextField.show();
        descriptionStatic.hide();

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
        nameTextField.hide();
        nameStatic.show();

        descriptionTextField.hide();
        descriptionStatic.show();

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
        alertDefinition.setName(nameTextField.getValue().toString());
        alertDefinition.setDescription(descriptionTextField.getValue().toString());

        String prioritySelected = prioritySelection.getValue().toString();
        alertDefinition.setPriority(AlertPriority.valueOf(prioritySelected));

        alertDefinition.setEnabled("Yes".equals(enabledSelection.getValue()));
        alertDefinition.setReadOnly("Yes".equals(readOnlySelection.getValue()));
    }

    @Override
    public void clearFormValues() {
        nameTextField.clearValue();
        descriptionTextField.clearValue();
        prioritySelection.clearValue();
        enabledSelection.clearValue();
        readOnlySelection.clearValue();

        nameStatic.clearValue();
        descriptionStatic.clearValue();
        priorityStatic.clearValue();
        enabledStatic.clearValue();
        readOnlyStatic.clearValue();

        markForRedraw();
    }

    private void buildForm() {
        if (!formBuilt) {
            nameTextField = new TextItem("name", "Name");
            nameTextField.setWidth(300);
            nameTextField.setDefaultValue("");
            nameStatic = new StaticTextItem("nameStatic", "Name");

            descriptionTextField = new TextAreaItem("description", "Description");
            descriptionTextField.setWidth(300);
            descriptionTextField.setDefaultValue("");
            descriptionStatic = new StaticTextItem("descriptionStatic", "Description");

            prioritySelection = new SelectItem("priority", "Priority");
            LinkedHashMap<String, String> priorities = new LinkedHashMap<String, String>(3);
            priorities.put(AlertPriority.HIGH.name(), AlertPriority.HIGH.name());
            priorities.put(AlertPriority.MEDIUM.name(), AlertPriority.MEDIUM.name());
            priorities.put(AlertPriority.LOW.name(), AlertPriority.LOW.name());
            LinkedHashMap<String, String> priorityIcons = new LinkedHashMap<String, String>(3);
            priorityIcons.put(AlertPriority.HIGH.name(), "/images/icons/Flag_red_16.png");
            priorityIcons.put(AlertPriority.MEDIUM.name(), "/images/icons/Flag_yellow_16.png");
            priorityIcons.put(AlertPriority.LOW.name(), "/images/icons/Flag_blue_16.png");
            prioritySelection.setValueMap(priorities);
            prioritySelection.setValueIcons(priorityIcons);
            prioritySelection.setDefaultValue(AlertPriority.MEDIUM.name());
            priorityStatic = new StaticTextItem("priorityStatic", "Priority");
            priorityStatic.setValueIcons(priorityIcons);

            enabledSelection = new RadioGroupItem("enabled", "Enabled");
            enabledSelection.setValueMap("Yes", "No");
            enabledSelection.setDefaultValue("Yes");
            enabledStatic = new StaticTextItem("enabledStatic", "Enabled");

            readOnlySelection = new RadioGroupItem("readOnly", "Protected");
            readOnlySelection.setValueMap("Yes", "No");
            readOnlySelection.setDefaultValue("Yes");
            readOnlySelection
                .setPrompt("If true, this definition is protected from being changed by the parent definition. In other words, the parent definition settings will not override this definition.");
            readOnlyStatic = new StaticTextItem("readOnlyStatic", "Protected");

            setFields(nameTextField, nameStatic, descriptionTextField, descriptionStatic, prioritySelection,
                priorityStatic, enabledSelection, enabledStatic, readOnlySelection, readOnlyStatic);

            formBuilt = true;
        }

    }
}
