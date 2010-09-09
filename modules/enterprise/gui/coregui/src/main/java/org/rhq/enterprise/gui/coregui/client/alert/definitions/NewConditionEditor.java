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

import com.smartgwt.client.types.Alignment;
import com.smartgwt.client.util.SC;
import com.smartgwt.client.widgets.form.fields.ButtonItem;
import com.smartgwt.client.widgets.form.fields.SelectItem;
import com.smartgwt.client.widgets.form.fields.events.ClickEvent;
import com.smartgwt.client.widgets.form.fields.events.ClickHandler;

import org.rhq.core.domain.alert.AlertCondition;
import org.rhq.core.domain.alert.AlertConditionCategory;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableDynamicForm;

/**
 * @author John Mazzitelli
 */
public class NewConditionEditor extends LocatableDynamicForm {

    private HashSet<AlertCondition> conditions;
    private SelectItem conditionTypeSelectItem;

    public NewConditionEditor(String locatorId, HashSet<AlertCondition> conditions) {
        super(locatorId);
        this.conditions = conditions;
    }

    @Override
    protected void onInit() {
        super.onInit();

        conditionTypeSelectItem = new SelectItem("conditionType", "Condition Type");
        LinkedHashMap<String, String> condTypes = new LinkedHashMap<String, String>(7);
        condTypes.put(AlertConditionCategory.THRESHOLD.name(), "Measurement Absolute Value Threshold");
        condTypes.put(AlertConditionCategory.BASELINE.name(), "Measurement Baseline Threshold");
        condTypes.put(AlertConditionCategory.CHANGE.name(), "Measurement Value Change");
        condTypes.put(AlertConditionCategory.TRAIT.name(), "Trait Value Change");
        condTypes.put(AlertConditionCategory.AVAILABILITY.name(), "Availability Change");
        condTypes.put(AlertConditionCategory.CONTROL.name(), "Operation Execution");
        condTypes.put(AlertConditionCategory.EVENT.name(), "Event Detection");
        conditionTypeSelectItem.setValueMap(condTypes);
        conditionTypeSelectItem.setDefaultValue(AlertConditionCategory.THRESHOLD.name());
        conditionTypeSelectItem.setWrapTitle(false);

        ButtonItem ok = new ButtonItem("buttonItem", "OK");
        ok.setColSpan(2);
        ok.setAlign(Alignment.CENTER);
        ok.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                // TODO
                SC.say("Not yet implemented");
            }
        });

        setFields(conditionTypeSelectItem, ok);
    };
}
