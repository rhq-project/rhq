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

import com.smartgwt.client.widgets.form.fields.RadioGroupItem;
import com.smartgwt.client.widgets.form.fields.SelectItem;

import org.rhq.core.domain.alert.AlertDefinition;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableDynamicForm;

/**
 * @author John Mazzitelli
 */
public class RecoveryAlertDefinitionForm extends LocatableDynamicForm implements EditAlertDefinitionForm {

    private AlertDefinition alertDefinition;

    private SelectItem recoverAlertSelection;
    private RadioGroupItem disableWhenFiredSelection;

    private boolean formBuilt = false;

    public RecoveryAlertDefinitionForm(String locatorId) {
        this(locatorId, null);
    }

    public RecoveryAlertDefinitionForm(String locatorId, AlertDefinition alertDefinition) {
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
            disableWhenFiredSelection.setValue(alertDef.getWillRecover() ? "Yes" : "No");
        }

        markForRedraw();
    }

    @Override
    public void makeEditable() {
        // TODO Auto-generated method stub

        markForRedraw();
    }

    @Override
    public void makeViewOnly() {
        // TODO Auto-generated method stub

        markForRedraw();
    }

    @Override
    public void saveAlertDefinition() {
        // TODO Auto-generated method stub
    }

    @Override
    public void clearFormValues() {
        recoverAlertSelection.clearValue();
        disableWhenFiredSelection.clearValue();

        markForRedraw();
    }

    private void buildForm() {
        if (!formBuilt) {
            recoverAlertSelection = new SelectItem("recoveryAlert", "Recover Alert");
            recoverAlertSelection.setValueMap("Select...");
            recoverAlertSelection.setDefaultValue("Select...");
            // TODO: call into server and get the menu list of all alerts that we can recover

            disableWhenFiredSelection = new RadioGroupItem("disableWhenFired", "Disable When Fired");
            disableWhenFiredSelection.setValueMap("Yes", "No");
            disableWhenFiredSelection.setDefaultValue("Yes");

            setFields(recoverAlertSelection, disableWhenFiredSelection);

            formBuilt = true;
        }
    }
}
