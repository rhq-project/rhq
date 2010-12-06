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

import java.util.HashSet;
import java.util.LinkedHashMap;

import com.smartgwt.client.widgets.form.fields.SelectItem;
import com.smartgwt.client.widgets.form.fields.StaticTextItem;

import org.rhq.core.domain.alert.AlertCondition;
import org.rhq.core.domain.alert.AlertDefinition;
import org.rhq.core.domain.alert.BooleanExpression;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableDynamicForm;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableVLayout;

/**
 * @author John Mazzitelli
 */
public class ConditionsAlertDefinitionForm extends LocatableVLayout implements EditAlertDefinitionForm {

    private final ResourceType resourceType;
    private AlertDefinition alertDefinition;

    private SelectItem conditionExpression;
    private ConditionsEditor conditionsEditor;

    private StaticTextItem conditionExpressionStatic;

    private boolean formBuilt = false;

    public ConditionsAlertDefinitionForm(String locatorId, ResourceType resourceType) {
        this(locatorId, resourceType, null);
    }

    public ConditionsAlertDefinitionForm(String locatorId, ResourceType resourceType, AlertDefinition alertDefinition) {
        super(locatorId);
        this.resourceType = resourceType;
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
            conditionsEditor.setConditions(alertDef.getConditions());
            conditionExpressionStatic.setValue(alertDef.getConditionExpression().toString());
        }

        markForRedraw();
    }

    @Override
    public void makeEditable() {
        conditionExpression.show();
        conditionsEditor.setEditable(true);
        conditionExpressionStatic.hide();

        markForRedraw();
    }

    @Override
    public void makeViewOnly() {
        conditionExpression.hide();
        conditionsEditor.setEditable(false);
        conditionExpressionStatic.show();

        markForRedraw();
    }

    @Override
    public void saveAlertDefinition() {
        String condExpr = conditionExpression.getValue().toString();
        alertDefinition.setConditionExpression(BooleanExpression.valueOf(condExpr));

        HashSet<AlertCondition> conditions = conditionsEditor.getConditions();
        alertDefinition.setConditions(conditions);
    }

    @Override
    public void clearFormValues() {
        conditionExpression.clearValue();
        conditionsEditor.setConditions(null);

        conditionExpressionStatic.clearValue();

        markForRedraw();
    }

    private void buildForm() {
        if (!formBuilt) {

            LocatableDynamicForm conditionExpressionForm;
            conditionExpressionForm = new LocatableDynamicForm(this.extendLocatorId("conditionExpressionForm"));
            conditionExpression = new SelectItem("conditionExpression", MSG
                .view_alert_common_tab_conditions_expression());
            LinkedHashMap<String, String> condExprs = new LinkedHashMap<String, String>(2);
            condExprs.put(BooleanExpression.ANY.name(), BooleanExpression.ANY.toString());
            condExprs.put(BooleanExpression.ALL.name(), BooleanExpression.ALL.toString());
            conditionExpression.setValueMap(condExprs);
            conditionExpression.setDefaultValue(BooleanExpression.ANY.name());
            conditionExpression.setWrapTitle(false);
            conditionExpression.setHoverWidth(300);
            conditionExpression.setTooltip(MSG.view_alert_common_tab_conditions_expression_tooltip());

            conditionExpressionStatic = new StaticTextItem("conditionExpressionStatic", MSG
                .view_alert_common_tab_conditions_expression());
            conditionExpressionStatic.setWrapTitle(false);

            conditionExpressionForm.setFields(conditionExpression, conditionExpressionStatic);

            conditionsEditor = new ConditionsEditor(this.extendLocatorId("conditionsEditor"), resourceType, null);

            setMembers(conditionExpressionForm, conditionsEditor);
            formBuilt = true;
        }
    }
}
