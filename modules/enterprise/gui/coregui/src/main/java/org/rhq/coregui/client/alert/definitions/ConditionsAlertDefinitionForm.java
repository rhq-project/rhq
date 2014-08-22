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

package org.rhq.coregui.client.alert.definitions;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;

import com.smartgwt.client.widgets.form.DynamicForm;
import com.smartgwt.client.widgets.form.fields.SelectItem;
import com.smartgwt.client.widgets.form.fields.StaticTextItem;
import com.smartgwt.client.widgets.form.fields.events.ChangeEvent;
import com.smartgwt.client.widgets.form.fields.events.ChangeHandler;

import org.rhq.core.domain.alert.AlertCondition;
import org.rhq.core.domain.alert.AlertDefinition;
import org.rhq.core.domain.alert.BooleanExpression;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.coregui.client.CoreGUI;
import org.rhq.coregui.client.util.enhanced.EnhancedVLayout;
import org.rhq.coregui.client.util.message.Message;
import org.rhq.coregui.client.util.message.Message.Severity;

/**
 * @author John Mazzitelli
 */
public class ConditionsAlertDefinitionForm extends EnhancedVLayout implements EditAlertDefinitionForm {

    private final ResourceType resourceType;
    private AlertDefinition alertDefinition;

    private SelectItem conditionExpression; // this is the GWT menu where the user selects ALL or ANY conjunction
    private ConditionsEditor conditionsEditor;

    private StaticTextItem conditionExpressionStatic;

    private boolean formBuilt = false;
    private boolean updated;

    public ConditionsAlertDefinitionForm(ResourceType resourceType) {
        this(resourceType, null);
    }

    public ConditionsAlertDefinitionForm(ResourceType resourceType, AlertDefinition alertDefinition) {
        super();
        this.resourceType = resourceType;
        this.alertDefinition = alertDefinition;
        this.updated = false;
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
    
    public Map<Integer, AlertCondition> getModifiedConditions() {
        return conditionsEditor.getModifiedConditions();
    }
    
    @Override
    public boolean isResetMatching() {
        return updated || conditionsEditor.isUpdated() || conditionsEditor.isConditionInternallyUpdated();
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
        updated = false;

        conditionExpression.hide();
        conditionsEditor.setEditable(false);
        conditionsEditor.setUpdated(false);

        conditionExpressionStatic.show();

        markForRedraw();
    }

    @Override
    public void saveAlertDefinition() {
        updated = false;

        String condExpr = conditionExpression.getValue().toString();
        alertDefinition.setConditionExpression(BooleanExpression.valueOf(condExpr));

        HashSet<AlertCondition> conditions = conditionsEditor.getConditions();
        alertDefinition.setConditions(conditions);

        conditionsEditor.setUpdated(false);
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

            DynamicForm conditionExpressionForm;
            conditionExpressionForm = new DynamicForm();
            conditionExpression = new SelectItem("conditionExpression",
                MSG.view_alert_common_tab_conditions_expression());
            LinkedHashMap<String, String> condExprs = new LinkedHashMap<String, String>(2);
            condExprs.put(BooleanExpression.ANY.name(), BooleanExpression.ANY.toString());
            condExprs.put(BooleanExpression.ALL.name(), BooleanExpression.ALL.toString());
            conditionExpression.setValueMap(condExprs);
            conditionExpression.setDefaultValue(BooleanExpression.ANY.name());
            conditionExpression.setWrapTitle(false);
            conditionExpression.setHoverWidth(300);
            conditionExpression.setTooltip(MSG.view_alert_common_tab_conditions_expression_tooltip());

            conditionExpressionStatic = new StaticTextItem("conditionExpressionStatic",
                MSG.view_alert_common_tab_conditions_expression());
            conditionExpressionStatic.setWrapTitle(false);

            conditionExpressionForm.setFields(conditionExpression, conditionExpressionStatic);

            conditionsEditor = new ConditionsEditor(conditionExpression, resourceType, null);

            conditionExpression.addChangeHandler(new ChangeHandler() {
                @Override
                public void onChange(ChangeEvent event) {
                    updated = true;

                    // Find out if this is using the ALL conjunction - if it is, we can't have more than one conditional use the same metric.
                    // If we do, immediately abort and warn the user. See BZ 737565
                    if ((BooleanExpression.ALL.name().equals(event.getValue().toString()))
                        && (resourceType != null && resourceType.getMetricDefinitions() != null)) {

                        HashSet<AlertCondition> conditions = conditionsEditor.getConditions();
                        Map<Integer, String> metricIdsUsed = new HashMap<Integer, String>();
                        for (AlertCondition condition : conditions) {
                            if (condition.getMeasurementDefinition() != null) {
                                Integer id = Integer.valueOf(condition.getMeasurementDefinition().getId());
                                if (metricIdsUsed.containsKey(id)) {
                                    String msg = MSG
                                        .view_alert_definition_condition_editor_metricswarning(metricIdsUsed.get(id));
                                    Message warning = new Message(msg, Severity.Warning, EnumSet
                                        .of(Message.Option.Transient));
                                    CoreGUI.getMessageCenter().notify(warning);
                                    event.cancel(); // multiple conditions used the same metric with ALL conjunction, this doesn't work - abort (BZ 737565)
                                    break;
                                }
                                metricIdsUsed.put(id, condition.getMeasurementDefinition().getDisplayName());
                            }
                        }
                    }
                    return;
                }
            });

            setMembers(conditionExpressionForm, conditionsEditor);
            formBuilt = true;
        }
    }
}
