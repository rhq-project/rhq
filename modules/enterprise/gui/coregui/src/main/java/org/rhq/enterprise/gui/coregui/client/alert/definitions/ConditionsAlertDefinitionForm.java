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

import com.smartgwt.client.widgets.form.DynamicForm;
import com.smartgwt.client.widgets.form.fields.SelectItem;
import com.smartgwt.client.widgets.form.fields.StaticTextItem;

import org.rhq.core.domain.alert.AlertDefinition;
import org.rhq.core.domain.alert.BooleanExpression;

/**
 * @author John Mazzitelli
 */
public class ConditionsAlertDefinitionForm extends DynamicForm implements EditAlertDefinitionForm {

    private AlertDefinition alertDefinition;

    private SelectItem conditionExpression;

    private StaticTextItem conditionExpressionStatic;

    private boolean formBuilt = false;

    public ConditionsAlertDefinitionForm() {
        this(null);
    }

    public ConditionsAlertDefinitionForm(AlertDefinition alertDefinition) {
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
            conditionExpression.setValue(alertDef.getConditionExpression().name());
            conditionExpressionStatic.setValue(alertDef.getConditionExpression().toString());
        }

        markForRedraw();
    }

    @Override
    public void makeEditable() {
        conditionExpression.show();
        conditionExpressionStatic.hide();

        markForRedraw();
    }

    @Override
    public void makeViewOnly() {
        conditionExpression.hide();
        conditionExpressionStatic.show();

        markForRedraw();
    }

    @Override
    public void saveAlertDefinition() {
        String condExpr = conditionExpression.getValue().toString();
        alertDefinition.setConditionExpression(BooleanExpression.valueOf(condExpr));
    }

    @Override
    public void clearFormValues() {
        conditionExpression.clearValue();

        conditionExpressionStatic.clearValue();

        markForRedraw();
    }

    private void buildForm() {
        if (!formBuilt) {

            conditionExpression = new SelectItem("conditionExpression", "Fire alert when");
            LinkedHashMap<String, String> condExprs = new LinkedHashMap<String, String>(2);
            condExprs.put(BooleanExpression.ALL.name(), BooleanExpression.ALL.toString());
            condExprs.put(BooleanExpression.ANY.name(), BooleanExpression.ANY.toString());
            conditionExpression.setValueMap(condExprs);
            conditionExpression.setDefaultValue(BooleanExpression.ALL.name());
            conditionExpressionStatic = new StaticTextItem("conditionExpressionStatic", "Fire alert when");

            setFields(conditionExpression, conditionExpressionStatic);

            formBuilt = true;
        }
    }
}
