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
package org.rhq.enterprise.gui.coregui.client.components.measurement;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;

import com.smartgwt.client.widgets.form.fields.ButtonItem;
import com.smartgwt.client.widgets.form.fields.CheckboxItem;
import com.smartgwt.client.widgets.form.fields.DateTimeItem;
import com.smartgwt.client.widgets.form.fields.SelectItem;
import com.smartgwt.client.widgets.form.fields.events.ChangeEvent;
import com.smartgwt.client.widgets.form.fields.events.ChangeHandler;
import com.smartgwt.client.widgets.form.fields.events.ClickEvent;
import com.smartgwt.client.widgets.form.fields.events.ClickHandler;
import com.smartgwt.client.widgets.grid.ListGrid;

import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.components.table.TableWidget;
import org.rhq.enterprise.gui.coregui.client.util.MeasurementUtility;
import org.rhq.enterprise.gui.coregui.client.util.message.Message;
import org.rhq.enterprise.gui.coregui.client.util.message.Message.Severity;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableDynamicForm;

/**
 * @author Simeon Pinder
 * @author Greg Hinkle
 */
public abstract class AbstractMeasurementRangeEditor extends LocatableDynamicForm implements TableWidget {

    //keyed map of translated date units Ex. minutes,hours,days
    protected static LinkedHashMap<String, String> lastUnits;
    //array of values available for displaying/selecting 'last N hours|minutes|days'.
    protected static String[] lastValues;

    protected boolean advanced;
    private ButtonItem advancedSimpleButton;
    protected SelectItem simpleLastValuesItem;
    protected SelectItem simpleLastUnitsItem;
    protected DateTimeItem advancedStartItem;
    protected DateTimeItem advancedEndItem;
    private boolean displaySetButton = true;
    private boolean displayEnableButton = false;
    private boolean displayRangeItemGrouping = false;
    protected CheckboxItem enableRangeItem;

    private ButtonItem setButton;
    public static String ENABLE_RANGE_ITEM = "ENABLE_RANGE_ITEM";
    public static String ADVANCED_BUTTON_ITEM = "advanced";
    public static String SIMPLE_VALUE_ITEM = "lastValues";
    public static String SIMPLE_UNIT_ITEM = "lastUnits";
    public static String ADVANCED_START_ITEM = "start";
    public static String ADVANCED_END_ITEM = "end";
    public static String SET_ITEM = "set";

    static {
        lastUnits = new LinkedHashMap<String, String>(3);
        lastUnits.put(String.valueOf(MeasurementUtility.UNIT_MINUTES), MSG.common_label_minutes());
        lastUnits.put(String.valueOf(MeasurementUtility.UNIT_HOURS), MSG.common_label_hours());
        lastUnits.put(String.valueOf(MeasurementUtility.UNIT_DAYS), MSG.common_label_days());

        lastValues = new String[] { "4", "8", "12", "24", "30", "36", "48", "60", "90", "120" };
    }

    public AbstractMeasurementRangeEditor(String locatorId) {
        super(locatorId);
        setNumCols(12);
        setWrapItemTitles(false);
    }

    /**
     * Returns the current range that is persisted. Note this may NOT be the begin and end times
     * as shown in the UI if the user changed the values but did not hit the set button.
     * @return begin/end epoch times in a list
     */
    public abstract List<Long> getBeginEndTimes();

    public abstract MetricRangePreferences getMetricRangePreferences();

    public abstract void setMetricRangeProperties(MetricRangePreferences prefs);

    @Override
    protected void onInit() {
        super.onInit();
        if (isDisplayRangeItemGrouping()) {
            setIsGroup(true);
            setGroupTitle("Filter by: Time");
        }
        enableRangeItem = new CheckboxItem(ENABLE_RANGE_ITEM, "");
        enableRangeItem.setStartRow(true);
        enableRangeItem.setShowTitle(false);
        enableRangeItem.setShowLabel(false);
        enableRangeItem.addChangeHandler(new ChangeHandler() {
            @Override
            public void onChange(ChangeEvent event) {
                boolean enableRange = Boolean.valueOf(event.getItem().getValue() + "");
                enableMeasurementRange(enableRange);
            }
        });

        //combobox of last items
        simpleLastValuesItem = new SelectItem(SIMPLE_VALUE_ITEM, MSG.view_measureRange_last());
        simpleLastValuesItem.setStartRow(false);
        simpleLastValuesItem.setEndRow(false);
        simpleLastValuesItem.setValueMap(lastValues);
        simpleLastValuesItem.setWidth("*");
        simpleLastValuesItem.setRedrawOnChange(true);

        //combobox of units of time
        simpleLastUnitsItem = new SelectItem(SIMPLE_UNIT_ITEM);
        simpleLastUnitsItem.setStartRow(false);
        simpleLastUnitsItem.setEndRow(false);
        simpleLastUnitsItem.setValueMap(lastUnits);
        simpleLastUnitsItem.setShowTitle(false);
        simpleLastUnitsItem.setWidth("*");
        simpleLastUnitsItem.setRedrawOnChange(true);

        //time range start from
        advancedStartItem = new DateTimeItem(ADVANCED_START_ITEM, MSG.view_measureRange_start());
        advancedStartItem.setStartRow(false);
        advancedStartItem.setEndRow(false);

        //time range end
        advancedEndItem = new DateTimeItem(ADVANCED_END_ITEM, MSG.common_title_end());
        advancedEndItem.setStartRow(false);
        advancedEndItem.setEndRow(false);

        setButton = new ButtonItem(SET_ITEM, MSG.common_button_set());
        setButton.setStartRow(false);
        setButton.setEndRow(false);
        setButton.setShowTitle(false);
        setButton.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                MetricRangePreferences prefs = new MetricRangePreferences();
                prefs.explicitBeginEnd = advanced;
                if (advanced) {
                    prefs.begin = advancedStartItem.getValueAsDate().getTime();
                    prefs.end = advancedEndItem.getValueAsDate().getTime();
                } else {
                    prefs.lastN = Integer.valueOf(simpleLastValuesItem.getValueAsString());
                    prefs.unit = Integer.valueOf(simpleLastUnitsItem.getValueAsString());
                }
                setMetricRangeProperties(prefs);
            }
        });

        advancedSimpleButton = new ButtonItem(ADVANCED_BUTTON_ITEM, MSG.common_button_advanced());
        advancedSimpleButton.setStartRow(false);
        advancedSimpleButton.setEndRow(false);
        advancedSimpleButton.setShowTitle(false);
        advancedSimpleButton.addClickHandler(new ClickHandler() {
            public void onClick(ClickEvent clickEvent) {
                advanced = !advanced;
                update();
            }
        });

        MetricRangePreferences metricRangePrefs = getMetricRangePreferences();
        if (metricRangePrefs != null) {
            advanced = (metricRangePrefs.explicitBeginEnd);
        }

        if (displaySetButton) {
            setItems(simpleLastValuesItem, simpleLastUnitsItem, advancedStartItem, advancedEndItem, setButton,
                advancedSimpleButton);
        } else {//not displaying Set button
            if (displayEnableButton) {
                setItems(enableRangeItem, simpleLastValuesItem, simpleLastUnitsItem, advancedStartItem,
                    advancedEndItem, advancedSimpleButton);
                setNumCols(9);//extend to encompass expanded
            } else {
                setItems(simpleLastValuesItem, simpleLastUnitsItem, advancedStartItem, advancedEndItem,
                    advancedSimpleButton);
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
            showItem("start");
            showItem("end");
            hideItem("lastValues");
            hideItem("lastUnits");
        } else {
            advancedSimpleButton.setTitle(MSG.common_button_advanced());
            hideItem("start");
            hideItem("end");
            showItem("lastValues");
            showItem("lastUnits");
        }

        // populate the fields - first with defaults in case we have no prefs, then with the appropriate prefs
        assignDefaultsToSimpleItems();
        assignDefaultsToAdvancedItems();

        try {
            MetricRangePreferences metricRangePrefs = getMetricRangePreferences();
            if (metricRangePrefs.explicitBeginEnd) {
                if (metricRangePrefs.begin != null && metricRangePrefs.end != null) {
                    advancedStartItem.setValue(new Date(metricRangePrefs.begin.longValue()));
                    advancedEndItem.setValue(new Date(metricRangePrefs.end.longValue()));
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
        public Long begin;
        public Long end;

        /**
         * Returns a two element <code>List</code> of <code>Long</code> objects representing the begin and end times (in
         * milliseconds since the epoch) of the time frame.
         **/
        public ArrayList<Long> getBeginEndTimes() {
            if (explicitBeginEnd) {
                ArrayList<Long> times = new ArrayList<Long>(2);
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
}
