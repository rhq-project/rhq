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

import java.util.Arrays;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;

import com.smartgwt.client.widgets.form.fields.ButtonItem;
import com.smartgwt.client.widgets.form.fields.DateTimeItem;
import com.smartgwt.client.widgets.form.fields.SelectItem;
import com.smartgwt.client.widgets.form.fields.events.ClickEvent;
import com.smartgwt.client.widgets.form.fields.events.ClickHandler;
import com.smartgwt.client.widgets.grid.ListGrid;

import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.UserSessionManager;
import org.rhq.enterprise.gui.coregui.client.components.table.TableWidget;
import org.rhq.enterprise.gui.coregui.client.util.MeasurementUtility;
import org.rhq.enterprise.gui.coregui.client.util.message.Message;
import org.rhq.enterprise.gui.coregui.client.util.message.Message.Severity;
import org.rhq.enterprise.gui.coregui.client.util.preferences.MeasurementUserPreferences;
import org.rhq.enterprise.gui.coregui.client.util.preferences.MeasurementUserPreferences.MetricRangePreferences;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableDynamicForm;

/**
 * @author Greg Hinkle
 */
public class MeasurementRangeEditor extends LocatableDynamicForm implements TableWidget {

    private static LinkedHashMap<String, String> lastUnits;
    private static String[] lastValues;

    private MeasurementUserPreferences measurementUserPrefs;

    private boolean advanced;
    private ButtonItem advancedSimpleButton;
    private SelectItem simpleLastValuesItem;
    private SelectItem simpleLastUnitsItem;
    private DateTimeItem advancedStartItem;
    private DateTimeItem advancedEndItem;
    private ButtonItem setButton;

    static {
        lastUnits = new LinkedHashMap<String, String>(3);
        lastUnits.put(String.valueOf(MeasurementUtility.UNIT_MINUTES), MSG.common_label_minutes());
        lastUnits.put(String.valueOf(MeasurementUtility.UNIT_HOURS), MSG.common_label_hours());
        lastUnits.put(String.valueOf(MeasurementUtility.UNIT_DAYS), MSG.common_label_days());

        lastValues = new String[] { "4", "8", "12", "24", "30", "36", "48", "60", "90", "120" };
    }

    public MeasurementRangeEditor(String locatorId) {
        super(locatorId);
        setNumCols(12);
        setWrapItemTitles(false);

        measurementUserPrefs = new MeasurementUserPreferences(UserSessionManager.getUserPreferences());
    }

    /**
     * Returns the current range that is persisted. Note this may NOT be the begin and end times
     * as shown in the UI if the user changed the values but did not hit the set button.
     * @return begin/end epoch times in a list 
     */
    public List<Long> getBeginEndTimes() {
        return measurementUserPrefs.getMetricRangePreferences().getBeginEndTimes();
    }

    @Override
    protected void onInit() {
        super.onInit();

        simpleLastValuesItem = new SelectItem("lastValues", MSG.view_measureRange_last());
        simpleLastValuesItem.setStartRow(false);
        simpleLastValuesItem.setEndRow(false);
        simpleLastValuesItem.setValueMap(lastValues);
        simpleLastValuesItem.setWidth("*");
        simpleLastValuesItem.setRedrawOnChange(true);

        simpleLastUnitsItem = new SelectItem("lastUnits");
        simpleLastUnitsItem.setStartRow(false);
        simpleLastUnitsItem.setEndRow(false);
        simpleLastUnitsItem.setValueMap(lastUnits);
        simpleLastUnitsItem.setShowTitle(false);
        simpleLastUnitsItem.setWidth("*");
        simpleLastUnitsItem.setRedrawOnChange(true);

        advancedStartItem = new DateTimeItem("start", MSG.view_measureRange_start());
        advancedStartItem.setStartRow(false);
        advancedStartItem.setEndRow(false);

        advancedEndItem = new DateTimeItem("end", MSG.common_title_end());
        advancedEndItem.setStartRow(false);
        advancedEndItem.setEndRow(false);

        setButton = new ButtonItem("set", MSG.common_button_set());
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
                measurementUserPrefs.setMetricRangePreferences(prefs);
            }
        });

        advancedSimpleButton = new ButtonItem("advanced", MSG.common_button_advanced());
        advancedSimpleButton.setStartRow(false);
        advancedSimpleButton.setEndRow(false);
        advancedSimpleButton.setShowTitle(false);
        advancedSimpleButton.addClickHandler(new ClickHandler() {
            public void onClick(ClickEvent clickEvent) {
                advanced = !advanced;
                update();
            }
        });

        MetricRangePreferences metricRangePrefs = measurementUserPrefs.getMetricRangePreferences();
        advanced = (metricRangePrefs.explicitBeginEnd);

        setItems(simpleLastValuesItem, simpleLastUnitsItem, advancedStartItem, advancedEndItem, setButton,
            advancedSimpleButton);
    }

    private void assignDefaultsToAdvancedItems() {
        advancedStartItem.setValue(new Date(System.currentTimeMillis() - (1000L * 60 * 60 * 8)));
        advancedEndItem.setValue(new Date());
    }

    private void assignDefaultsToSimpleItems() {
        simpleLastValuesItem.setValue("8");
        simpleLastUnitsItem.setValue(String.valueOf(MeasurementUtility.UNIT_HOURS));
    }

    private void update() {
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
            MetricRangePreferences metricRangePrefs = measurementUserPrefs.getMetricRangePreferences();
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
}
