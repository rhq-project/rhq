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
import com.smartgwt.client.widgets.form.fields.FormItem;
import com.smartgwt.client.widgets.form.fields.IntegerItem;
import com.smartgwt.client.widgets.form.fields.StaticTextItem;
import com.smartgwt.client.widgets.form.fields.events.ChangedEvent;
import com.smartgwt.client.widgets.form.fields.events.ChangedHandler;
import com.smartgwt.client.widgets.form.validator.IntegerRangeValidator;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.Messages;
import org.rhq.enterprise.gui.coregui.client.util.FormUtility;
import org.rhq.enterprise.gui.coregui.client.util.TypeConversionUtility;
import org.rhq.enterprise.gui.coregui.client.util.selenium.Locatable;

import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.Set;
import java.util.TreeSet;

/**
 * A form item for entering a duration - consists of an IntegerItem for entering the amount of time and a
 * ComboBoxItem for entering the duration units.
 *
 * @author Ian Springer
 */
public class DurationItem extends CanvasItem {

    private static final Messages MSG = CoreGUI.getMessages();

    private static final String FIELD_VALUE = "value";
    private static final String FIELD_UNITS = "units";

    private final DynamicForm form;
    private TimeUnit defaultTimeUnit;
    private Set<UnitType> supportedUnitTypes;
    private TimeUnit valueUnit;
    private boolean isReadOnly;
    private UnitType unitType;

    public DurationItem(String name, String title, TreeSet<TimeUnit> supportedUnits, boolean supportsIterations,
                        boolean isReadOnly, Locatable parentWidget) {
        super(name, title);

        this.supportedUnitTypes = EnumSet.noneOf(UnitType.class);
        if (supportedUnits != null && !supportedUnits.isEmpty()) {
            this.supportedUnitTypes.add(UnitType.TIME);
            this.valueUnit = supportedUnits.iterator().next();
        }
        if (supportsIterations) {
            this.supportedUnitTypes.add(UnitType.ITERATIONS);
        }

        this.isReadOnly = isReadOnly;

        this.form = new EnhancedDynamicForm(parentWidget.extendLocatorId(name), false, false);

        if (this.isReadOnly) {
            this.form.setNumCols(2);
            this.form.setColWidths("140", "160");

            StaticTextItem staticTextItem = new StaticTextItem(FIELD_VALUE, title);
            staticTextItem.setShowTitle(getShowTitle());

            this.form.setFields(staticTextItem);
        } else {
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

            LinkedHashMap<String, String> valueMap = new LinkedHashMap<String, String>();
            if (this.supportedUnitTypes.contains(UnitType.ITERATIONS)) {
                valueMap.put("times", MSG.common_unit_times());
            }
            if (this.supportedUnitTypes.contains(UnitType.TIME)) {
                for (TimeUnit unit : supportedUnits) {
                    valueMap.put(unit.name().toLowerCase(), unit.getDisplayName());
                }
            }
            unitsItem.setValueMap(valueMap);

            if (this.defaultTimeUnit != null) {
                unitsItem.setDefaultValue(this.defaultTimeUnit.name().toLowerCase());
            } else {
                unitsItem.setDefaultToFirstOption(true);
            }

            unitsItem.addChangedHandler(new ChangedHandler() {
                public void onChanged(ChangedEvent event) {
                    updateValue();
                }
            });

            this.form.setFields(valueItem, unitsItem);
            valueItem.setWidth(90);
            unitsItem.setWidth(105);
        }

        setCanvas(this.form);
    }

    public void setValue(Integer value, UnitType unitType) {
        if (!this.supportedUnitTypes.contains(unitType)) {
            throw new IllegalArgumentException(MSG.widget_durationItem_unitTypeNotSupported(unitType.name()));
        }
        this.unitType = unitType;

        String unitString = null;
        switch (unitType) {
            case TIME:
                unitString = this.valueUnit.getDisplayName();
                break;
            case ITERATIONS:
                unitString = MSG.common_unit_times();
        }

        if (this.isReadOnly) {
            String stringValue;
            if (value == null) {
                stringValue = "";
            } else {
                stringValue = value + " " + unitString;
            }
            this.form.setValue(FIELD_VALUE, stringValue);
        } else {
            if (value != null) {
                this.form.setValue(FIELD_VALUE, value);
            } else {
                this.form.setValue(FIELD_VALUE, (String)null);
            }
            this.form.setValue(FIELD_UNITS, unitString);
        }

        setValue(value);
    }

    private void updateValue() {
        Long value = calculateValue();
        setValue(value);
    }

    private Long calculateValue() {
        IntegerItem valueItem = (IntegerItem) this.form.getItem(FIELD_VALUE);
        Object value = valueItem.getValue();
        Long integerValue = TypeConversionUtility.toLong(value);
        Long convertedValue = null;
        if (integerValue != null) {
            TimeUnit unit = getInputTimeUnit();
            if (unit == null) {
                this.unitType = UnitType.ITERATIONS;
                convertedValue = integerValue;
            } else {
                this.unitType = UnitType.TIME;
                if (unit.compareTo(this.valueUnit) < 0) {
                    throw new IllegalStateException(MSG.widget_durationItem_inputUnitLessThanTargetUnit());
                }
                switch (unit) {
                    case MILLISECONDS:
                        switch (this.valueUnit) {
                            case MILLISECONDS:
                                convertedValue = integerValue;
                                break;
                        }
                        break;
                    case SECONDS:
                        switch (this.valueUnit) {
                            case SECONDS:
                                convertedValue = integerValue;
                                break;
                            case MILLISECONDS:
                                convertedValue = integerValue * 1000;
                                break;
                        }
                        break;
                    case MINUTES:
                        switch (this.valueUnit) {
                            case MINUTES:
                                convertedValue = integerValue;
                                break;
                            case SECONDS:
                                convertedValue = integerValue * 60;
                                break;
                            case MILLISECONDS:
                                convertedValue = integerValue * 60 * 1000;
                                break;
                        }
                        break;
                    case HOURS:
                        switch (this.valueUnit) {
                            case HOURS:
                                convertedValue = integerValue;
                                break;
                            case MINUTES:
                                convertedValue = integerValue * 60;
                                break;
                            case SECONDS:
                                convertedValue = integerValue * 60 * 60;
                                break;
                            case MILLISECONDS:
                                convertedValue = integerValue * 60 * 60 * 1000;
                                break;
                        }
                        break;
                    case DAYS:
                        switch (this.valueUnit) {
                            case DAYS:
                                convertedValue = integerValue;
                                break;
                            case HOURS:
                                convertedValue = integerValue * 24;
                                break;
                            case MINUTES:
                                convertedValue = integerValue * 24 * 60;
                                break;
                            case SECONDS:
                                convertedValue = integerValue * 24 * 60 * 60;
                                break;
                            case MILLISECONDS:
                                convertedValue = integerValue * 24 * 60 * 60 * 1000;
                                break;
                        }
                        break;
                    case WEEKS:
                        switch (this.valueUnit) {
                            case WEEKS:
                                convertedValue = integerValue;
                                break;
                            case DAYS:
                                convertedValue = integerValue * 7;
                                break;
                            case HOURS:
                                convertedValue = integerValue * 7 * 24;
                                break;
                            case MINUTES:
                                convertedValue = integerValue * 7 * 24 * 60;
                                break;
                            case SECONDS:
                                convertedValue = integerValue * 7 * 24 * 60 * 60;
                                break;
                            case MILLISECONDS:
                                convertedValue = integerValue * 7 * 24 * 60 * 60 * 1000;
                                break;
                        }
                        break;
                    case MONTHS:
                        switch (this.valueUnit) {
                            case MONTHS:
                                convertedValue = integerValue;
                                break;
                            case WEEKS:
                                convertedValue = integerValue * 4;
                                break;
                            case DAYS:
                                convertedValue = integerValue * 30;
                                break;
                            case HOURS:
                                convertedValue = integerValue * 30 * 24;
                                break;
                            case MINUTES:
                                convertedValue = integerValue * 30 * 24 * 60;
                                break;
                            case SECONDS:
                                convertedValue = integerValue * 30 * 24 * 60 * 60;
                                break;
                            case MILLISECONDS:
                                convertedValue = integerValue * 30 * 24 * 60 * 60 * 1000;
                                break;
                        }
                        break;
                    case YEARS:
                        switch (this.valueUnit) {
                            case YEARS:
                                convertedValue = integerValue;
                                break;
                            case MONTHS:
                                convertedValue = integerValue * 12;
                                break;
                            case WEEKS:
                                convertedValue = integerValue * 52;
                                break;
                            case DAYS:
                                convertedValue = integerValue * 365;
                                break;
                            case HOURS:
                                convertedValue = integerValue * 365 * 24;
                                break;
                            case MINUTES:
                                convertedValue = integerValue * 365 * 24 * 60;
                                break;
                            case SECONDS:
                                convertedValue = integerValue * 365 * 24 * 60 * 60;
                                break;
                            case MILLISECONDS:
                                convertedValue = integerValue * 365 * 24 * 60 * 60 * 1000;
                                break;
                        }
                        break;
                }
            }
        }
        return convertedValue;
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

    public UnitType getUnitType() {
        return unitType;
    }

    public Integer getValueAsInteger() {
        return TypeConversionUtility.toInteger(getValue());
    }

    public Long getValueAsLong() {
        return TypeConversionUtility.toLong(getValue());
    }

    @Override
    public Boolean validate() {
        return this.form.validate();
    }

    public void setDefaultTimeUnit(TimeUnit defaultTimeUnit) {
        this.defaultTimeUnit = defaultTimeUnit;
    }

    public void setContextualHelp(String contextualHelp) {
        if (contextualHelp != null) {
            FormItem item;
            if (this.isReadOnly) {
                item = this.form.getItem(FIELD_VALUE);
            } else {
                item = this.form.getItem(FIELD_UNITS);
            }
            FormUtility.addContextualHelp(item, contextualHelp);
        }
    }

    /**
     * If this item's {@link #getValue() value} represents a time duration, returns the value's time unit; otherwise
     * returns null.
     *
     * @return if this item's {@link #getValue() value} represents a time duration, returns the value's time unit; otherwise
     *         returns null
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
            String unitString = (timeUnit != null) ? timeUnit.name().toLowerCase() : MSG.common_unit_times();
            string = value + " " + unitString;
        } else {
            string = "";
        }

        return string;
    }

}
