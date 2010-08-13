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

import org.rhq.core.domain.alert.AlertDefinition;

/**
 * @author John Mazzitelli
 */
public class NotificationsAlertDefinitionForm extends DynamicForm implements EditAlertDefinitionForm {

    private AlertDefinition alertDefinition;

    public NotificationsAlertDefinitionForm() {
        this(null);
    }

    public NotificationsAlertDefinitionForm(AlertDefinition alertDefinition) {
        this.alertDefinition = alertDefinition;
    }

    @Override
    protected void onDraw() {
        super.onDraw();

        // TODO only build form if we didn't do it yet
        if (true) {
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

        if (alertDef == null) {
            clearFormValues();
        } else {
            // TODO set values of the components
        }

        markForRedraw();
    }

    @Override
    public void makeEditable() {
        // TODO Auto-generated method stub
    }

    @Override
    public void makeViewOnly() {
        // TODO Auto-generated method stub
    }

    @Override
    public void saveAlertDefinition() {
        // TODO Auto-generated method stub
    }

    @Override
    public void clearFormValues() {
        // TODO component.clearValue();
    }

    private void buildForm() {
        // TODO build components
        // TODO setFields(components);
    }
}
