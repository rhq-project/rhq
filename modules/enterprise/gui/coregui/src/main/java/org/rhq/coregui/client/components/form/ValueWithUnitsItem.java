/*
 * RHQ Management Platform
 * Copyright (C) 2005-2011 Red Hat, Inc.
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
package org.rhq.coregui.client.components.form;

import java.util.LinkedHashMap;
import java.util.Set;

import com.smartgwt.client.widgets.form.DynamicForm;
import com.smartgwt.client.widgets.form.fields.CanvasItem;
import com.smartgwt.client.widgets.form.fields.FormItem;
import com.smartgwt.client.widgets.form.fields.IntegerItem;
import com.smartgwt.client.widgets.form.fields.SelectItem;
import com.smartgwt.client.widgets.form.validator.IntegerRangeValidator;

import org.rhq.core.domain.measurement.MeasurementUnits;
import org.rhq.coregui.client.CoreGUI;
import org.rhq.coregui.client.Messages;
import org.rhq.coregui.client.util.FormUtility;

/**
 * A form item for entering a n arbitrary value - consists of an IntegerItem for entering the value and a
 * ComboBoxItem for entering the units.
 *
 * @author Jirka Kremser
 */
public class ValueWithUnitsItem extends CanvasItem {

    private static final Messages MSG = CoreGUI.getMessages();

    private static final String FIELD_VALUE = "value";
    private static final String FIELD_UNITS = "units";

    private final DynamicForm form;
    private Set<MeasurementUnits> supportedUnits;
    private MeasurementUnits valueUnit;

    public ValueWithUnitsItem(String name, String title, Set<MeasurementUnits> supportedUnits) {
        super(name, title);

        if (supportedUnits != null && !supportedUnits.isEmpty()) {
            this.supportedUnits = supportedUnits;
            if (null == valueUnit) {
                valueUnit = supportedUnits.iterator().next();
            }
        }

        form = new EnhancedDynamicForm(false, false);
        form.setNumCols(2);
        form.setColWidths("126", "60");

        final IntegerItem valueItem = new IntegerItem(FIELD_VALUE, title);
        valueItem.setShowTitle(getShowTitle());
        valueItem.setValue(super.getValue());
        IntegerRangeValidator integerRangeValidator = new IntegerRangeValidator();
        integerRangeValidator.setMin(1);
        integerRangeValidator.setMax(Integer.MAX_VALUE);
        valueItem.setValidators(integerRangeValidator);
        valueItem.setValidateOnChange(true);

        SelectItem unitsItem = new SelectItem(FIELD_UNITS);
        unitsItem.setShowTitle(false);

        LinkedHashMap<String, String> valueMap = new LinkedHashMap<String, String>();
        for (MeasurementUnits unit : supportedUnits) {
            valueMap.put(unit.toString(), unit.toString());
        }
        unitsItem.setValueMap(valueMap);
        unitsItem.setDefaultToFirstOption(true);

        this.form.setFields(valueItem, unitsItem);
        valueItem.setWidth(126);
        unitsItem.setWidth(60);

        setCanvas(form);
    }

    @Override
    public void setValidateOnChange(Boolean validateOnChange) {
        form.setValidateOnChange(validateOnChange);
    }

    @Override
    public void setValidateOnExit(Boolean validateOnExit) {
        form.setValidateOnChange(validateOnExit);
    }

    public void setValue(Integer value, MeasurementUnits unitType) {
        if (!this.supportedUnits.contains(unitType)) {
            throw new IllegalArgumentException(MSG.widget_durationItem_unitTypeNotSupported(unitType.name()));
        }
        if (value != null) {
            form.setValue(FIELD_VALUE, value);
        } else {
            form.setValue(FIELD_VALUE, (String) null);
        }
        this.valueUnit = unitType;
        form.setValue(FIELD_UNITS, valueUnit.toString());

        setValue(value);
    }
    
    @Override
    public Object getValue() {
        return form.getValue(FIELD_VALUE).toString() + form.getValue(FIELD_UNITS).toString();
    }

    @Override
    public Boolean validate() {
        return form.validate();
    }

    public void setContextualHelp(String contextualHelp) {
        if (contextualHelp != null) {
            FormItem item;
            item = form.getItem(FIELD_UNITS);
            FormUtility.addContextualHelp(item, contextualHelp);
        }
    }
}
