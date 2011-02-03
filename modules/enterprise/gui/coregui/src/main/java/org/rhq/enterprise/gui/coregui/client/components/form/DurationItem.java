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
package org.rhq.enterprise.gui.coregui.client.components.form;

import com.smartgwt.client.widgets.form.DynamicForm;
import com.smartgwt.client.widgets.form.fields.CanvasItem;
import com.smartgwt.client.widgets.form.fields.ComboBoxItem;
import com.smartgwt.client.widgets.form.fields.IntegerItem;
import com.smartgwt.client.widgets.form.fields.events.ChangedEvent;
import com.smartgwt.client.widgets.form.fields.events.ChangedHandler;
import com.smartgwt.client.widgets.form.validator.IntegerRangeValidator;
import org.rhq.enterprise.gui.coregui.client.util.FormUtility;
import org.rhq.enterprise.gui.coregui.client.util.selenium.Locatable;

import java.util.LinkedHashMap;
import java.util.Set;
import java.util.TreeSet;

/**
 * A form item for entering a time duration - consists of an IntegerItem for entering the amount of time and a
 * ComboBoxItem for entering the time units.
 *
 * @author Ian Springer
 */
public class DurationItem extends CanvasItem {

    private static final String FIELD_VALUE = "value";
    private static final String FIELD_UNITS = "units";

    private final DynamicForm form;
    private TimeUnit defaultUnit;
    private String contextualHelp;
    private boolean supportsIterations;
    private TimeUnit valueUnit;
    private boolean isReadOnly;

    public DurationItem(String name, String title, Set<TimeUnit> supportedUnits, boolean supportsIterations,
                        boolean isReadOnly, Locatable parentWidget) {
        super(name, title);

        this.supportsIterations = supportsIterations;
        this.isReadOnly = isReadOnly;

        this.form = new EnhancedDynamicForm(parentWidget.extendLocatorId(name), false, false);
        this.form.setNumCols(4);
        this.form.setColWidths("140", "90", "105", "*");

        final IntegerItem valueItem = new IntegerItem(FIELD_VALUE, title);
        valueItem.setShowTitle(getShowTitle());
        valueItem.setValue(getValue());
        IntegerRangeValidator integerRangeValidator = new IntegerRangeValidator();
        integerRangeValidator.setMin(1);
        valueItem.setValidators(integerRangeValidator);
        valueItem.setValidateOnChange(getValidateOnChange());
        valueItem.setValidateOnExit(getValidateOnExit());

        valueItem.addChangedHandler(new ChangedHandler() {
            public void onChanged(ChangedEvent event) {
                updateValue();
            }
        });

        ComboBoxItem unitsItem = new ComboBoxItem(FIELD_UNITS);
        unitsItem.setShowTitle(false);

        // TODO: i18n valueMap values
        LinkedHashMap<String, String> valueMap = new LinkedHashMap<String, String>();
        if (supportsIterations) {
            valueMap.put("times", "times");
        }
        if (supportedUnits != null && !supportedUnits.isEmpty()) {
            TreeSet<TimeUnit> sortedSupportedUnits = new TreeSet<TimeUnit>(supportedUnits);
            for (TimeUnit unit : sortedSupportedUnits) {
                valueMap.put(unit.name().toLowerCase(), unit.name().toLowerCase());
            }
            this.valueUnit = sortedSupportedUnits.iterator().next();
            unitsItem.setValueMap(valueMap);
        }

        if (this.defaultUnit != null) {
            unitsItem.setDefaultValue(this.defaultUnit.name().toLowerCase());
        } else {
            unitsItem.setDefaultToFirstOption(true);
        }

        if (this.contextualHelp != null) {
            FormUtility.addContextualHelp(unitsItem, this.contextualHelp);
        }

        unitsItem.addChangedHandler(new ChangedHandler() {
            public void onChanged(ChangedEvent event) {
                updateValue();
            }
        });

        this.form.setFields(valueItem, unitsItem);
        valueItem.setWidth(90);
        unitsItem.setWidth(105);

        setCanvas(this.form);
    }

    public void setValue(Object value) {
        Integer intValue;
        if (value instanceof String) {
            intValue = Integer.valueOf((String)value);
        } else if (value instanceof Integer) {
            intValue = (Integer) value;
        } else {
            intValue = null;
        }

        if (intValue != null) {
            this.form.setValue(FIELD_VALUE, intValue);
        } else {
            this.form.setValue(FIELD_VALUE, (String)null);
        }

        super.setValue(value);
    }

    private void updateValue() {
        Integer convertedValue = null;

        IntegerItem valueItem = (IntegerItem) this.form.getItem(FIELD_VALUE);
        Object value = valueItem.getValue();
        Integer intValue;
        if (value instanceof String) {
            intValue = Integer.valueOf((String)value);
        } else if (value instanceof Integer) {
            intValue = (Integer) value;
        } else {
            intValue = null;
        }

        if (intValue != null) {
            TimeUnit unit = getInputTimeUnit();
            if (unit == null) {
                convertedValue = intValue;
            } else {
                if (unit.compareTo(this.valueUnit) < 0) {
                    throw new IllegalStateException("Input unit is less than target unit.");
                }
                switch (unit) {
                    case MILLISECONDS:
                        switch (this.valueUnit) {
                            case MILLISECONDS:
                                convertedValue = intValue;
                                break;
                        }
                        break;
                    case SECONDS:
                        switch (this.valueUnit) {
                            case SECONDS:
                                convertedValue = intValue;
                                break;
                            case MILLISECONDS:
                                convertedValue = intValue * 1000;
                                break;
                        }
                        break;
                    case MINUTES:
                        switch (this.valueUnit) {
                            case MINUTES:
                                convertedValue = intValue;
                                break;
                            case SECONDS:
                                convertedValue = intValue * 60;
                                break;
                            case MILLISECONDS:
                                convertedValue = intValue * 60 * 1000;
                                break;
                        }
                        break;
                    case HOURS:
                        switch (this.valueUnit) {
                            case HOURS:
                                convertedValue = intValue;
                                break;
                            case MINUTES:
                                convertedValue = intValue * 60;
                                break;
                            case SECONDS:
                                convertedValue = intValue * 60 * 60;
                                break;
                            case MILLISECONDS:
                                convertedValue = intValue * 60 * 60 * 1000;
                                break;
                        }
                        break;
                    case DAYS:
                        switch (this.valueUnit) {
                            case DAYS:
                                convertedValue = intValue;
                                break;
                            case HOURS:
                                convertedValue = intValue * 24;
                                break;
                            case MINUTES:
                                convertedValue = intValue * 24 * 60;
                                break;
                            case SECONDS:
                                convertedValue = intValue * 24 * 60 * 60;
                                break;
                            case MILLISECONDS:
                                convertedValue = intValue * 24 * 60 * 60 * 1000;
                                break;
                        }
                        break;
                    case WEEKS:
                        switch (this.valueUnit) {
                            case WEEKS:
                                convertedValue = intValue;
                                break;
                            case DAYS:
                                convertedValue = intValue * 7;
                                break;
                            case HOURS:
                                convertedValue = intValue * 7 * 24;
                                break;
                            case MINUTES:
                                convertedValue = intValue * 7 * 24 * 60;
                                break;
                            case SECONDS:
                                convertedValue = intValue * 7 * 24 * 60 * 60;
                                break;
                            case MILLISECONDS:
                                convertedValue = intValue * 7 * 24 * 60 * 60 * 1000;
                                break;
                        }
                        break;
                    case MONTHS:
                        switch (this.valueUnit) {
                            case MONTHS:
                                convertedValue = intValue;
                                break;
                            case WEEKS:
                                convertedValue = intValue * 4;
                                break;
                            case DAYS:
                                convertedValue = intValue * 30;
                                break;
                            case HOURS:
                                convertedValue = intValue * 30 * 24;
                                break;
                            case MINUTES:
                                convertedValue = intValue * 30 * 24 * 60;
                                break;
                            case SECONDS:
                                convertedValue = intValue * 30 * 24 * 60 * 60;
                                break;
                            case MILLISECONDS:
                                convertedValue = intValue * 30 * 24 * 60 * 60 * 1000;
                                break;
                        }
                        break;
                    case YEARS:
                        switch (this.valueUnit) {
                            case YEARS:
                                convertedValue = intValue;
                                break;
                            case MONTHS:
                                convertedValue = intValue * 12;
                                break;
                            case WEEKS:
                                convertedValue = intValue * 52;
                                break;
                            case DAYS:
                                convertedValue = intValue * 365;
                                break;
                            case HOURS:
                                convertedValue = intValue * 365 * 24;
                                break;
                            case MINUTES:
                                convertedValue = intValue * 365 * 24 * 60;
                                break;
                            case SECONDS:
                                convertedValue = intValue * 365 * 24 * 60 * 60;
                                break;
                            case MILLISECONDS:
                                convertedValue = intValue * 365 * 24 * 60 * 60 * 1000;
                                break;
                        }
                        break;
                }
            }
        }

        super.setValue(convertedValue);
    }

    private TimeUnit getInputTimeUnit() {
        ComboBoxItem unitsItem = (ComboBoxItem) this.form.getItem(FIELD_UNITS);
        String unitString = unitsItem.getValueAsString(); // this will always be non-null
        TimeUnit unit;
        try {
            unit = TimeUnit.valueOf(unitString.toUpperCase());
        } catch (IllegalArgumentException e) {
            // not a time unit, so unit must be "times" (i.e. iterations)
            unit = null;
        }
        return unit;
    }

    public Integer getValueAsInteger() {
        return (Integer) getValue();
    }

    @Override
    public Boolean validate() {
        return this.form.validate();
    }

    public void setDefaultUnit(TimeUnit defaultUnit) {
        this.defaultUnit = defaultUnit;
    }

    public void setContextualHelp(String contextualHelp) {
        this.contextualHelp = contextualHelp;
    }

    /**
     * Returns the time unit of this item's {@link #getValue() value} - if null, the item's value represents an iteration
     * count, not a time duration.
     *
     * @return the time unit of this item's {@link #getValue() value} - if null, the item's value represents an iteration
     *         count, not a time duration
     */
    public TimeUnit getValueUnit() {
        return this.valueUnit;
    }

    @Override
    public String toString() {
        Object value = form.getValue(FIELD_VALUE);
        String string;
        if (value != null) {
            TimeUnit timeUnit = getInputTimeUnit();
            String unitString = (timeUnit != null) ? timeUnit.name().toLowerCase() : "times";
            string = value + " " + unitString;
        } else {
            string = "";
        }

        return string;
    }

}
