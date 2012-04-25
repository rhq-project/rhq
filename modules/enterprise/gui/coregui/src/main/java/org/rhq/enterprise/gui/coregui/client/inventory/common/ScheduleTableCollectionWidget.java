/*
 * RHQ Management Platform
 * Copyright (C) 2005-2012 Red Hat, Inc.
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
package org.rhq.enterprise.gui.coregui.client.inventory.common;

import com.smartgwt.client.widgets.form.DynamicForm;
import com.smartgwt.client.widgets.form.fields.CanvasItem;
import com.smartgwt.client.widgets.form.fields.SelectItem;
import com.smartgwt.client.widgets.form.fields.SpinnerItem;
import com.smartgwt.client.widgets.form.fields.events.ChangedEvent;
import com.smartgwt.client.widgets.form.fields.events.ChangedHandler;
import com.smartgwt.client.widgets.form.fields.events.ShowValueEvent;
import com.smartgwt.client.widgets.form.fields.events.ShowValueHandler;
import com.smartgwt.client.widgets.form.validator.IntegerRangeValidator;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.Messages;
import org.rhq.enterprise.gui.coregui.client.util.Log;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableDynamicForm;

import java.util.LinkedHashMap;

/**
 * This widget is used inside the ListGrid controls when editing the collection interval
 * in place. It shows up inside the grid cell when you click on it to edit the cell. The
 * control consists of a collection interval spinner for entering the collection interval
 * time period and a unit of measure SelectItem to choose the time measurement units.
 *
 * @author Mike Thompson
 */
public class ScheduleTableCollectionWidget extends CanvasItem {
    private static final Messages MSG = CoreGUI.getMessages();

    private static final String UNITS_SECONDS = "s";
    private static final String UNITS_MINUTES = "m";
    private static final String UNITS_HOURS = "h";

    private DynamicForm form;
    private SpinnerItem collectionIntervalSpinnerItem = new SpinnerItem();
    private IntegerRangeValidator integerRangeValidator = new IntegerRangeValidator();
    private SelectItem unitsItem = new SelectItem();

    // Maps values to labels for the units select list.
    private static final LinkedHashMap<String, String> VALUE_MAP = new LinkedHashMap<String, String>();
    static {
        VALUE_MAP.put(UNITS_SECONDS, MSG.common_unit_seconds());
        VALUE_MAP.put(UNITS_MINUTES, MSG.common_unit_minutes());
        VALUE_MAP.put(UNITS_HOURS, MSG.common_unit_hours());
    }

    private AbstractSchedulesView schedulesView;

    public ScheduleTableCollectionWidget(String locatorId, final AbstractSchedulesView schedulesView) {
        super(locatorId);
        this.schedulesView = schedulesView;

        form = new LocatableDynamicForm("collectionIntervalForm");
        form.setNumCols(2);
        collectionIntervalSpinnerItem.setWidth(50);
        collectionIntervalSpinnerItem.setShowTitle(false);
        integerRangeValidator.setMin(1);
        integerRangeValidator.setMax(10000000); // avoids exceptions if someone enters really large nums; no one needs to go higher anyway
        collectionIntervalSpinnerItem.setValidators(integerRangeValidator);
        collectionIntervalSpinnerItem.addChangedHandler(new ChangedHandler() {
            @Override
            public void onChanged(ChangedEvent changedEvent) {
                Long collectionInterval = Long.valueOf(changedEvent.getItem().getValue() + "");
                Log.debug("Collection interval value Changed to: " + collectionInterval + " " + unitsItem.getValueAsString());
                schedulesView.updateSchedules(convertToMillis(collectionInterval, unitsItem.getValueAsString()));
            }
        });

        // Specify a null title so no label is rendered to the left of the combo box.
        unitsItem.setValueMap(VALUE_MAP);
        unitsItem.setDefaultValue(UNITS_MINUTES);
        unitsItem.setShowTitle(false);
        unitsItem.setWidth(100);
        unitsItem.addChangedHandler(new ChangedHandler() {
            @Override
            public void onChanged(ChangedEvent event) {

                Long collectionInterval = Long.valueOf(collectionIntervalSpinnerItem.getValueAsString());
                Log.debug("Collection interval units changed to: "  + unitsItem.getValueAsString());
                String value = (String) event.getValue();
                if (value.equals(UNITS_SECONDS)) {
                    integerRangeValidator.setMin(30);
                } else {
                    integerRangeValidator.setMin(1);
                }
                schedulesView.updateSchedules(convertToMillis(collectionInterval, unitsItem.getValueAsString()));
            }
        });

        addShowValueHandler(new ShowValueHandler() {
            @Override
            public void onShowValue(ShowValueEvent event) {
                Integer rawValue = (Integer) event.getDataValue();
                convertToProperUnits(rawValue);
            }
        });

        if (!schedulesView.hasWriteAccess()) {
            collectionIntervalSpinnerItem.setDisabled(true);
            unitsItem.setDisabled(true);
        }

        form.setFields(collectionIntervalSpinnerItem, unitsItem);
        setCanvas(form);
    }

    private void convertToProperUnits(long milliseconds){
        if (milliseconds > 1000) {
            long seconds = milliseconds / 1000;
            if (seconds >= 60) {
                long minutes = seconds / 60;
                seconds = seconds % 60;
                if (minutes > 60) {
                    long hours = minutes / 60;
                    collectionIntervalSpinnerItem.setValue(hours);
                    minutes = minutes % 60;
                    unitsItem.setValue(UNITS_HOURS);
                    collectionIntervalSpinnerItem.setStep(1);
                }
                if (minutes != 0) {
                    collectionIntervalSpinnerItem.setValue(minutes);
                    unitsItem.setValue(UNITS_MINUTES);
                    collectionIntervalSpinnerItem.setStep(5);
                }
            }
            if (seconds != 0) {
                unitsItem.setValue(UNITS_SECONDS);
                collectionIntervalSpinnerItem.setValue(seconds);
                collectionIntervalSpinnerItem.setStep(5);
            }
        }
    }

    private long convertToMillis(long collectionInterval, String timeUnitString){
        long millis = 0;

        if(timeUnitString.equals(UNITS_SECONDS)){
            millis =  collectionInterval * 1000;

        }else if(timeUnitString.equals(UNITS_MINUTES)){
            millis =  collectionInterval * (60 * 1000);

        }else if(timeUnitString.equals(UNITS_HOURS)){
            millis =  collectionInterval * (60 * 60 * 1000);
        }
        return millis;

    }


    public Long getInterval() {

        if (collectionIntervalSpinnerItem.getValue() == null || !collectionIntervalSpinnerItem.validate()) {
            return null;
        }
        Long intervalValue = Long.valueOf(collectionIntervalSpinnerItem.getValueAsString());
        return convertToMillis(intervalValue, unitsItem.getValueAsString());
    }

}

