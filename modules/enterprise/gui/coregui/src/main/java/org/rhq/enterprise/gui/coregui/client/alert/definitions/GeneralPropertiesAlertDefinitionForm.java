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

import com.smartgwt.client.widgets.form.DynamicForm;
import com.smartgwt.client.widgets.form.fields.RadioGroupItem;
import com.smartgwt.client.widgets.form.fields.SelectItem;
import com.smartgwt.client.widgets.form.fields.StaticTextItem;
import com.smartgwt.client.widgets.form.fields.TextAreaItem;
import com.smartgwt.client.widgets.form.fields.TextItem;

import org.rhq.core.domain.alert.AlertDefinition;
import org.rhq.core.domain.alert.AlertPriority;

/**
 * @author John Mazzitelli
 */
public class GeneralPropertiesAlertDefinitionForm extends DynamicForm implements EditAlertDefinitionForm {

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

    public GeneralPropertiesAlertDefinitionForm() {
        this(null);
    }

    public GeneralPropertiesAlertDefinitionForm(AlertDefinition alertDefinition) {
        this.alertDefinition = alertDefinition;
    }

    @Override
    protected void onDraw() {
        super.onDraw();

        if (!formBuilt) {
            buildForm();
            setAlertDefinition(alertDefinition);
            makeViewOnly();
        }
    }

    @Override
    public AlertDefinition getAlertDefinition() {
        return this.alertDefinition;
    }

    @Override
    public void setAlertDefinition(AlertDefinition alertDef) {
        this.alertDefinition = alertDef;

        buildForm();

        if (alertDef == null) {
            clearFormValues();
        } else {
            nameTextField.setValue(alertDef.getName());
            nameStatic.setValue(alertDef.getName());

            descriptionTextField.setValue(alertDef.getDescription());
            descriptionStatic.setValue(alertDef.getDescription());

            prioritySelection.setValue(alertDef.getPriority().getDisplayName());
            priorityStatic.setValue(alertDef.getPriority().getDisplayName());

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

        Integer parentId = this.alertDefinition.getParentId();
        if ((parentId == null || parentId.intValue() == 0) && (this.alertDefinition.getGroupAlertDefinition() == null)) {
            readOnlySelection.hide();
        } else {
            readOnlySelection.show();
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

        Integer parentId = this.alertDefinition.getParentId();
        if ((parentId == null || parentId.intValue() == 0) && (this.alertDefinition.getGroupAlertDefinition() == null)) {
            readOnlyStatic.hide();
        } else {
            readOnlyStatic.show();
        }
        readOnlySelection.hide();

        markForRedraw();
    }

    @Override
    public void saveAlertDefinition() {
        // TODO Auto-generated method stub
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
            nameStatic = new StaticTextItem("nameStatic", "Name");

            descriptionTextField = new TextAreaItem("description", "Description");
            descriptionTextField.setWidth(300);
            descriptionStatic = new StaticTextItem("descriptionStatic", "Description");

            prioritySelection = new SelectItem("priority", "Priority");
            prioritySelection.setValueMap(AlertPriority.HIGH.getDisplayName(), AlertPriority.MEDIUM.getDisplayName(),
                AlertPriority.LOW.getDisplayName());
            prioritySelection.setDefaultValue(AlertPriority.MEDIUM.getDisplayName());
            priorityStatic = new StaticTextItem("priorityStatic", "Priority");

            enabledSelection = new RadioGroupItem("enabled", "Enabled");
            enabledSelection.setValueMap("Yes", "No");
            enabledSelection.setDefaultValue("Yes");
            enabledStatic = new StaticTextItem("enabledStatic", "Enabled");

            readOnlySelection = new RadioGroupItem("readOnly", "Read Only");
            readOnlySelection.setValueMap("Yes", "No");
            readOnlySelection.setDefaultValue("Yes");
            readOnlySelection.setPrompt("If true, the parent definition will not override this alert definition");
            readOnlyStatic = new StaticTextItem("readOnlyStatic", "Read Only");

            setFields(nameTextField, nameStatic, descriptionTextField, descriptionStatic, prioritySelection,
                priorityStatic, enabledSelection, enabledStatic, readOnlySelection, readOnlyStatic);

            formBuilt = true;
        }

    }
}
