/*
 * RHQ Management Platform
 * Copyright (C) 2005-2010 Red Hat, Inc.
 * All rights reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */
package org.rhq.coregui.client.components.measurement;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.smartgwt.client.types.Alignment;
import com.smartgwt.client.types.DateDisplayFormat;
import com.smartgwt.client.widgets.form.DynamicForm;
import com.smartgwt.client.widgets.form.fields.ButtonItem;
import com.smartgwt.client.widgets.form.fields.CheckboxItem;
import com.smartgwt.client.widgets.form.fields.DateTimeItem;
import com.smartgwt.client.widgets.form.fields.SelectItem;
import com.smartgwt.client.widgets.form.fields.SpacerItem;
import com.smartgwt.client.widgets.form.fields.events.ChangeEvent;
import com.smartgwt.client.widgets.form.fields.events.ChangeHandler;
import com.smartgwt.client.widgets.form.fields.events.ClickEvent;
import com.smartgwt.client.widgets.form.fields.events.ClickHandler;
import com.smartgwt.client.widgets.form.validator.CustomValidator;
import com.smartgwt.client.widgets.grid.ListGrid;

import org.rhq.core.domain.measurement.util.Moment;
import org.rhq.coregui.client.CoreGUI;
import org.rhq.coregui.client.Messages;
import org.rhq.coregui.client.components.table.TableWidget;
import org.rhq.coregui.client.util.MeasurementUtility;
import org.rhq.coregui.client.util.message.Message;
import org.rhq.coregui.client.util.message.Message.Severity;

/**
 * @author Simeon Pinder
 * @author Greg Hinkle
 */
public abstract class AbstractMeasurementRangeEditor extends DynamicForm implements TableWidget {

    protected static final Messages MSG = CoreGUI.getMessages();

    //keyed map of translated date units Ex. minutes,hours,days
    protected static LinkedHashMap<String, String> lastUnits;
    //array of values available for displaying/selecting 'last N hours|minutes|days'.
    protected static final String[] lastValues;

    protected boolean advanced;
    private ButtonItem advancedSimpleButton;
    protected SelectItem simpleLastValuesItem;
    protected SelectItem simpleLastUnitsItem;
    protected DateTimeItem advancedStartItem;
    protected DateTimeItem advancedEndItem;
    private boolean displaySetButton = true;
    private boolean displayEnableButton = false;
    private boolean displayRangeItemGrouping = false;
    private boolean displayCheckboxLabel = false;
    protected CheckboxItem enableRangeItem;
    private SpacerItem space;

    private ButtonItem setButton;
    public static final String ENABLE_RANGE_ITEM = "ENABLE_RANGE_ITEM";
    public static final String ADVANCED_BUTTON_ITEM = "advanced";
    public static final String SIMPLE_VALUE_ITEM = "lastValues";
    public static final String SIMPLE_UNIT_ITEM = "lastUnits";
    public static final String ADVANCED_START_ITEM = "start";
    public static final String ADVANCED_END_ITEM = "end";
    public static final String SET_ITEM = "set";

    static {
        Messages MSG = CoreGUI.getMessages();

        lastUnits = new LinkedHashMap<String, String>(3);
        lastUnits.put(String.valueOf(MeasurementUtility.UNIT_MINUTES), MSG.common_unit_minutes());
        lastUnits.put(String.valueOf(MeasurementUtility.UNIT_HOURS), MSG.common_unit_hours());
        lastUnits.put(String.valueOf(MeasurementUtility.UNIT_DAYS), MSG.common_unit_days());

        lastValues = new String[] { "4", "8", "12", "24", "30", "36", "48", "60", "90", "120" };
    }

    public AbstractMeasurementRangeEditor() {
        super();
        setNumCols(10);
        setWrapItemTitles(false);
        setAlign(Alignment.LEFT);
    }

    /**
     * Returns the current range that is persisted. Note this may NOT be the begin and end times
     * as shown in the UI if the user changed the values but did not hit the set button.
     * @return begin/end epoch times in a list
     */
    public abstract List<Moment> getBeginEndTimes();

    public abstract MetricRangePreferences getMetricRangePreferences();

    public abstract void setMetricRangeProperties(MetricRangePreferences prefs);

    @Override
    protected void onInit() {
        super.onInit();
        if (isDisplayRangeItemGrouping()) {
            setIsGroup(true);
            setGroupTitle("Filter by: Time");
        }
        enableRangeItem = new CheckboxItem(ENABLE_RANGE_ITEM, "Filter By Time:");
        enableRangeItem.setShowTitle(isDisplayCheckboxLabel());
        enableRangeItem.setShowLabel(isDisplayCheckboxLabel());
        enableRangeItem.addChangeHandler(new ChangeHandler() {
            @Override
            public void onChange(ChangeEvent event) {
                boolean enableRange = Boolean.valueOf(event.getItem().getValue() + "");
                enableMeasurementRange(enableRange);
            }
        });

        //combobox of last items
        simpleLastValuesItem = new SelectItem(SIMPLE_VALUE_ITEM, MSG.view_measureRange_last());
        simpleLastValuesItem.setValueMap(lastValues);
        simpleLastValuesItem.setTitleAlign(Alignment.LEFT);

        //combobox of units of time
        simpleLastUnitsItem = new SelectItem(SIMPLE_UNIT_ITEM);
        simpleLastUnitsItem.setValueMap(lastUnits);
        simpleLastUnitsItem.setShowTitle(false);

        //time range start from
        advancedStartItem = new DateTimeItem(ADVANCED_START_ITEM, MSG.view_measureRange_start());
        advancedStartItem.setTitleAlign(Alignment.LEFT);
        advancedStartItem.setUseMask(true);
        advancedStartItem.setDisplayFormat(DateDisplayFormat.TOUSSHORTDATE);

        //time range end
        advancedEndItem = new DateTimeItem(ADVANCED_END_ITEM, MSG.common_title_end());
        advancedEndItem.setUseMask(true);
        advancedEndItem.setDisplayFormat(DateDisplayFormat.TOUSSHORTDATE);

        //time validator, start should be before end
        CustomValidator timeValidator = new CustomValidator() {
            protected boolean condition(Object value) {
                return advancedEndItem.getValueAsDate().after(advancedStartItem.getValueAsDate());
            }
        };
        timeValidator.setErrorMessage(MSG.view_measureTable_startBeforeEnd());
        advancedStartItem.setValidators(timeValidator);

        setButton = new ButtonItem(SET_ITEM, MSG.common_button_set());
        setButton.setStartRow(false);
        setButton.setEndRow(false);
        setButton.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                MetricRangePreferences prefs = new MetricRangePreferences();
                prefs.explicitBeginEnd = advanced;
                if (advanced) {
                    try {
                        if (validate()) {
                            prefs.begin = new Moment(advancedStartItem.getValueAsDate());
                            prefs.end = new Moment(advancedEndItem.getValueAsDate());
                            if (null != prefs.begin && null != prefs.end
                                && prefs.begin.toDate().before(prefs.end.toDate())) {
                                CoreGUI.getMessageCenter().notify(new Message(MSG.view_measureTable_startBeforeEnd()));
                            } else {
                                setMetricRangeProperties(prefs);
                            }
                        }
                    } catch (Exception ex) {
                        // some of the digits are not filled correctly
                        Map<String, String> errors = new HashMap<String, String>();
                        errors.put(ADVANCED_END_ITEM, "MM/DD/YYYY HH:MM");
                        errors.put(ADVANCED_START_ITEM, "MM/DD/YYYY HH:MM");
                        setErrors(errors, true);
                    }
                } else {
                    prefs.lastN = Integer.valueOf(simpleLastValuesItem.getValueAsString());
                    prefs.unit = Integer.valueOf(simpleLastUnitsItem.getValueAsString());
                    setMetricRangeProperties(prefs);
                }
            }

        });

        advancedSimpleButton = new ButtonItem(ADVANCED_BUTTON_ITEM, MSG.common_button_advanced());
        advancedSimpleButton.setStartRow(false);
        advancedSimpleButton.setEndRow(false);
        advancedSimpleButton.addClickHandler(new ClickHandler() {
            public void onClick(ClickEvent clickEvent) {
                advanced = !advanced;
                update();
            }
        });

        space = new SpacerItem();
        space.setWidth(300);

        MetricRangePreferences metricRangePrefs = getMetricRangePreferences();
        if (metricRangePrefs != null) {
            advanced = (metricRangePrefs.explicitBeginEnd);
        }

        if (displaySetButton) {
            setItems(simpleLastValuesItem, simpleLastUnitsItem, advancedStartItem, advancedEndItem, setButton,
                advancedSimpleButton, space);
        } else {//not displaying Set button
            if (displayEnableButton) {
                setItems(enableRangeItem, simpleLastValuesItem, simpleLastUnitsItem, advancedStartItem,
                    advancedEndItem, advancedSimpleButton, space);
            } else {
                setItems(simpleLastValuesItem, simpleLastUnitsItem, advancedStartItem, advancedEndItem,
                    advancedSimpleButton, space);
            }
        }
        update();
    }

    private void assignDefaultsToAdvancedItems() {
        advancedStartItem.setValue(new Date(System.currentTimeMillis() - (1000L * 60 * 60 * 8)));
        advancedEndItem.setValue(new Date());
    }

    private void assignDefaultsToSimpleItems() {
        simpleLastValuesItem.setValue("8");
        simpleLastUnitsItem.setValue(String.valueOf(MeasurementUtility.UNIT_HOURS));
    }

    protected void enableMeasurementRange(boolean enableRange) {
        if (enableRange) {
            simpleLastValuesItem.disable();
            simpleLastUnitsItem.disable();
            advancedStartItem.disable();
            advancedEndItem.disable();
            advancedSimpleButton.disable();
        } else {
            simpleLastValuesItem.enable();
            simpleLastUnitsItem.enable();
            advancedStartItem.enable();
            advancedEndItem.enable();
            advancedSimpleButton.enable();
            markForRedraw();
        }
    }

    protected void update() {
        if (advanced) {
            advancedSimpleButton.setTitle(MSG.view_measureRange_simple());
            showItem(ADVANCED_START_ITEM);
            showItem(ADVANCED_END_ITEM);
            hideItem(SIMPLE_VALUE_ITEM);
            hideItem(SIMPLE_UNIT_ITEM);
        } else {
            advancedSimpleButton.setTitle(MSG.common_button_advanced());
            hideItem(ADVANCED_START_ITEM);
            hideItem(ADVANCED_END_ITEM);
            showItem(SIMPLE_VALUE_ITEM);
            showItem(SIMPLE_UNIT_ITEM);
        }

        // populate the fields - first with defaults in case we have no prefs, then with the appropriate prefs
        assignDefaultsToSimpleItems();
        assignDefaultsToAdvancedItems();

        try {
            MetricRangePreferences metricRangePrefs = getMetricRangePreferences();
            if (metricRangePrefs.explicitBeginEnd) {
                if (metricRangePrefs.begin != null && metricRangePrefs.end != null) {
                    advancedStartItem.setValue(metricRangePrefs.begin.toDate());
                    advancedEndItem.setValue(metricRangePrefs.end.toDate());
                }
            } else {
                if (lastUnits.containsKey(String.valueOf(metricRangePrefs.unit))) {
                    simpleLastUnitsItem.setValue(String.valueOf(metricRangePrefs.unit));
                }
                if (Arrays.asList(lastValues).contains(String.valueOf(metricRangePrefs.lastN))) {
                    simpleLastValuesItem.setValue(String.valueOf(metricRangePrefs.lastN));
                }
            }
        } catch (Exception e) {
            // in case any odd errors occur (like bad format of preference strings), just fill in some defaults
            CoreGUI.getMessageCenter().notify(
                new Message("Failed to get range user preferences, using defaults", e, Severity.Warning));
            assignDefaultsToSimpleItems();
            assignDefaultsToAdvancedItems();
        }

        markForRedraw();
    }

    @Override
    public void refresh(ListGrid listGrid) {
        update();
    }

    public static class MetricRangePreferences {
        // if readOnly is true, then the beginning and ending range dates are specified with explicit dates
        // if readOnly is false, then the time is relative to NOW and is specified as <lastN> units of <unit> time
        public boolean explicitBeginEnd;

        // simple, when readOnly is false
        public int lastN;
        public int unit; // see MeasurementUtility.UNIT_xxx

        // advanced, when readOnly is true
        public Moment begin;
        public Moment end;

        /**
         * Returns a two element <code>List</code> of <code>Long</code> objects representing the begin and end times (in
         * milliseconds since the epoch) of the time frame.
         **/
        public ArrayList<Moment> getBeginEndTimes() {
            if (explicitBeginEnd) {
                ArrayList<Moment> times = new ArrayList<Moment>(2);
                times.add(begin);
                times.add(end);
                return times;
            } else {
                return MeasurementUtility.calculateTimeFrame(lastN, unit);
            }
        }

        public String toString() {
            return (explicitBeginEnd) ? "[begin=" + begin + end + ",end=" + end + "]" : "[lastN=" + lastN + ",unit="
                + unit + "]";
        }
    }

    /**
     * Returns the SetButton so you can set a click handler from a dialog box
     * @return ButtonItem setButton
     */
    public ButtonItem getSetButton() {
        return setButton;
    }

    public boolean isDisplaySetButton() {
        return displaySetButton;
    }

    public void setDisplaySetButton(boolean displaySetButton) {
        this.displaySetButton = displaySetButton;
    }

    public boolean isDisplayEnableButton() {
        return displayEnableButton;
    }

    public void setDisplayEnableButton(boolean displayEnableButton) {
        this.displayEnableButton = displayEnableButton;
    }

    public boolean isAdvanced() {
        return advanced;
    }

    public void setAdvanced(boolean advanced) {
        this.advanced = advanced;
    }

    public boolean isDisplayRangeItemGrouping() {
        return displayRangeItemGrouping;
    }

    public void setDisplayRangeItemGrouping(boolean displayRangeItemGrouping) {
        this.displayRangeItemGrouping = displayRangeItemGrouping;
    }

    public boolean isDisplayCheckboxLabel() {
        return displayCheckboxLabel;
    }

    public void setDisplayCheckboxLabel(boolean displayCheckboxLabel) {
        this.displayCheckboxLabel = displayCheckboxLabel;
    }
}
