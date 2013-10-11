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

import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.Set;
import java.util.TreeSet;

import com.smartgwt.client.widgets.form.DynamicForm;
import com.smartgwt.client.widgets.form.fields.CanvasItem;
import com.smartgwt.client.widgets.form.fields.FormItem;
import com.smartgwt.client.widgets.form.fields.IntegerItem;
import com.smartgwt.client.widgets.form.fields.SelectItem;
import com.smartgwt.client.widgets.form.fields.StaticTextItem;
import com.smartgwt.client.widgets.form.fields.events.ChangedEvent;
import com.smartgwt.client.widgets.form.fields.events.ChangedHandler;
import com.smartgwt.client.widgets.form.validator.IntegerRangeValidator;

import org.rhq.core.domain.measurement.MeasurementUnits;
import org.rhq.core.domain.measurement.composite.MeasurementNumericValueAndUnits;
import org.rhq.coregui.client.CoreGUI;
import org.rhq.coregui.client.Messages;
import org.rhq.coregui.client.util.FormUtility;
import org.rhq.coregui.client.util.MeasurementConverterClient;
import org.rhq.coregui.client.util.TypeConversionUtility;

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

    private static final long SECOND_IN_MILLIS = 1000L;
    private static final long MINUTE_IN_MILLIS = 60 * SECOND_IN_MILLIS;
    private static final long HOUR_IN_MILLIS = 60 * MINUTE_IN_MILLIS;
    private static final long DAY_IN_MILLIS = 24 * HOUR_IN_MILLIS;
    private static final long WEEK_IN_MILLIS = 7 * DAY_IN_MILLIS;
    private static final long MONTH_IN_MILLIS = 30 * DAY_IN_MILLIS;
    private static final long YEAR_IN_MILLIS = 365 * DAY_IN_MILLIS;

    private final DynamicForm form;
    private TimeUnit defaultTimeUnit;
    private Set<UnitType> supportedUnitTypes;
    private TimeUnit valueUnit;
    private boolean isReadOnly;
    private UnitType unitType;

    /**
     * @param name
     * @param title
     * @param supportedUnits when specified, the Set's most granular TimeUnit will be the valueUnit ({@link #getValueUnit()})
     * @param supportsIterations
     * @param isReadOnly
     * @param parentWidget
     */
    public DurationItem(String name, String title, TreeSet<TimeUnit> supportedUnits, boolean supportsIterations,
        boolean isReadOnly) {

        this(name, title, (null != supportedUnits && !supportedUnits.isEmpty()) ? supportedUnits.iterator().next()
            : null, supportedUnits, supportsIterations, isReadOnly);
    }

    /**
     * @param name
     * @param title
     * @param valueUnit the TimeUnit for to the item value ({@link #getValueUnit()}). If null the default is used.
     * If provided will override the default.  The default is the supportedUnit Set's  most granular TimeUnit ({@link #getValueUnit()}).
     * @param supportedUnits
     * @param supportsIterations
     * @param isReadOnly
     * @param parentWidget
     */
    public DurationItem(String name, String title, TimeUnit valueUnit, TreeSet<TimeUnit> supportedUnits,
        boolean supportsIterations, boolean isReadOnly) {
        super(name, title);

        this.valueUnit = valueUnit;
        this.supportedUnitTypes = EnumSet.noneOf(UnitType.class);
        if (supportedUnits != null && !supportedUnits.isEmpty()) {
            this.supportedUnitTypes.add(UnitType.TIME);
            if (null == this.valueUnit) {
                this.valueUnit = supportedUnits.iterator().next();
            }
        }
        if (supportsIterations) {
            this.supportedUnitTypes.add(UnitType.ITERATIONS);
        }

        this.isReadOnly = isReadOnly;

        this.form = new EnhancedDynamicForm(false, false);

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
            integerRangeValidator.setMax(Integer.MAX_VALUE);
            valueItem.setValidators(integerRangeValidator);
            valueItem.setValidateOnChange(true);

            valueItem.addChangedHandler(new ChangedHandler() {
                public void onChanged(ChangedEvent event) {
                    updateValue();
                }
            });

            SelectItem unitsItem = new SelectItem(FIELD_UNITS);
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

    @Override
    public void setValidateOnChange(Boolean validateOnChange) {
        form.setValidateOnChange(validateOnChange);
    }

    @Override
    public void setValidateOnExit(Boolean validateOnExit) {
        form.setValidateOnChange(validateOnExit);
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
                this.form.setValue(FIELD_VALUE, (String) null);
            }
            this.form.setValue(FIELD_UNITS, unitString);
        }

        setValue(value);
    }

    /**
     * Sets human readable time representation to the form's field "value"
     *
     * @param longValue the time period representation in milliseconds
     */
    public void setAndFormatValue(long longValue) {
        if (longValue < 0) {
            throw new IllegalArgumentException("negative time period " + longValue);
        }
        if (isReadOnly) {
            String formattedOutput = formatMilliseconds(longValue);
            this.unitType = UnitType.TIME;
            this.form.setValue(FIELD_VALUE, formattedOutput);
            setValue(formattedOutput);
        } else {
            MeasurementNumericValueAndUnits valueWithUnits;
            if (longValue % HOUR_IN_MILLIS == 0) {
                valueWithUnits = MeasurementConverterClient.fit((double) longValue, MeasurementUnits.MILLISECONDS,
                    MeasurementUnits.HOURS, MeasurementUnits.HOURS);
            } else {
                valueWithUnits = MeasurementConverterClient.fit((double) longValue, MeasurementUnits.MILLISECONDS,
                    MeasurementUnits.MINUTES, MeasurementUnits.MINUTES);
            }
            SelectItem unitsItem = (SelectItem) this.form.getItem(FIELD_UNITS);
            this.form.setValue(FIELD_VALUE, valueWithUnits.getValue().intValue());
            unitsItem.setValue(valueWithUnits.getUnits().name().toLowerCase());
            updateValue();
        }
    }

    /**
     * formatMilliseconds(10 * WEEK_IN_MILLIS + 2 * SECOND_IN_MILLIS) = 2 months 2 weeks 2 seconds
     *
     * @param longValue
     * @return formatted string with time period representation
     */
    private String formatMilliseconds(long longValue) {
        if (longValue < 0) {
            throw new IllegalArgumentException("negative time period " + longValue);
        }
        String formattedOutput = null;
        long wholeUnits = 0;
        if (longValue < SECOND_IN_MILLIS) { //ms
            return getTimeValue(longValue, TimeUnit.MILLISECONDS);
        } else if (longValue < MINUTE_IN_MILLIS) { //s
            wholeUnits = longValue / SECOND_IN_MILLIS;
            formattedOutput = getTimeValue(wholeUnits, TimeUnit.SECONDS);
            return formattedOutput
                + ((wholeUnits * SECOND_IN_MILLIS < longValue) ? " "
                    + formatMilliseconds(longValue - wholeUnits * SECOND_IN_MILLIS) : "");
        } else if (longValue < HOUR_IN_MILLIS) { //m
            wholeUnits = longValue / MINUTE_IN_MILLIS;
            formattedOutput = getTimeValue(wholeUnits, TimeUnit.MINUTES);
            return formattedOutput
                + ((wholeUnits * MINUTE_IN_MILLIS < longValue) ? " "
                    + formatMilliseconds(longValue - wholeUnits * MINUTE_IN_MILLIS) : "");
        } else if (longValue < DAY_IN_MILLIS) { //h
            wholeUnits = longValue / HOUR_IN_MILLIS;
            formattedOutput = getTimeValue(wholeUnits, TimeUnit.HOURS);
            return formattedOutput
                + ((wholeUnits * HOUR_IN_MILLIS < longValue) ? " "
                    + formatMilliseconds(longValue - wholeUnits * HOUR_IN_MILLIS) : "");
        } else if (longValue < WEEK_IN_MILLIS) { //d
            wholeUnits = longValue / DAY_IN_MILLIS;
            formattedOutput = getTimeValue(wholeUnits, TimeUnit.DAYS);
            return formattedOutput
                + ((wholeUnits * DAY_IN_MILLIS < longValue) ? " "
                    + formatMilliseconds(longValue - wholeUnits * DAY_IN_MILLIS) : "");
        } else if (longValue < MONTH_IN_MILLIS) { //w
            wholeUnits = longValue / WEEK_IN_MILLIS;
            formattedOutput = getTimeValue(wholeUnits, TimeUnit.WEEKS);
            return formattedOutput
                + ((wholeUnits * WEEK_IN_MILLIS < longValue) ? " "
                    + formatMilliseconds(longValue - wholeUnits * WEEK_IN_MILLIS) : "");
        } else if (longValue < YEAR_IN_MILLIS) { //M
            wholeUnits = longValue / MONTH_IN_MILLIS;
            formattedOutput = getTimeValue(wholeUnits, TimeUnit.MONTHS);
            return formattedOutput
                + ((wholeUnits * MONTH_IN_MILLIS < longValue) ? " "
                    + formatMilliseconds(longValue - wholeUnits * MONTH_IN_MILLIS) : "");
        } else if (longValue >= YEAR_IN_MILLIS) { //y
            wholeUnits = longValue / YEAR_IN_MILLIS;
            formattedOutput = getTimeValue(wholeUnits, TimeUnit.YEARS);
            return formattedOutput
                + ((wholeUnits * YEAR_IN_MILLIS < longValue) ? " "
                    + formatMilliseconds(longValue - wholeUnits * YEAR_IN_MILLIS) : "");
        } else {
            return "";
        }
    }

    private String getTimeValue(long value, TimeUnit valueUnit) {
        return value + " " + valueUnit.getDisplayName();
    }

    private void updateValue() {
        Long value = calculateValue();
        setValue(value);
    }

    private Long calculateValue() {
        IntegerItem valueItem = (IntegerItem) this.form.getItem(FIELD_VALUE);
        Object value = valueItem.getValue();
        Long integerValue = null;
        try {
            integerValue = TypeConversionUtility.toLong(value);
        } catch (NumberFormatException e) {
            return null;
        }
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
                        convertedValue = integerValue * SECOND_IN_MILLIS;
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
                        convertedValue = integerValue * MINUTE_IN_MILLIS;
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
                        convertedValue = integerValue * HOUR_IN_MILLIS;
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
                        convertedValue = integerValue * DAY_IN_MILLIS;
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
                        convertedValue = integerValue * WEEK_IN_MILLIS;
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
                        convertedValue = integerValue * MONTH_IN_MILLIS;
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
                        convertedValue = integerValue * YEAR_IN_MILLIS;
                        break;
                    }
                    break;
                }
            }
        }
        return convertedValue;
    }

    private TimeUnit getInputTimeUnit() {
        SelectItem unitsItem = (SelectItem) this.form.getItem(FIELD_UNITS);
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
