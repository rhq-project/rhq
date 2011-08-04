/*
 * RHQ Management Platform
 * Copyright (C) 2010-2011 Red Hat, Inc.
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

import java.util.LinkedHashMap;

import com.smartgwt.client.types.Overflow;
import com.smartgwt.client.widgets.events.ClickEvent;
import com.smartgwt.client.widgets.events.ClickHandler;
import com.smartgwt.client.widgets.form.DynamicForm;
import com.smartgwt.client.widgets.form.fields.IntegerItem;
import com.smartgwt.client.widgets.form.fields.SelectItem;
import com.smartgwt.client.widgets.form.fields.events.ChangedEvent;
import com.smartgwt.client.widgets.form.fields.events.ChangedHandler;
import com.smartgwt.client.widgets.form.validator.IntegerRangeValidator;
import com.smartgwt.client.widgets.grid.ListGrid;

import org.rhq.enterprise.gui.coregui.client.components.table.TableWidget;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableButton;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableDynamicForm;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableHLayout;

/**
 * Widget for updating the collection intervals for the selected metrics. It displays two form fields for
 * entering the interval and the interval's units (seconds, minutes, or hours), as well as a Set button
 * for submitting the update.
 *
 * @author Ian Springer
 */
public class UpdateCollectionIntervalWidget extends LocatableHLayout implements TableWidget {

    private static final String ITEM_INTERVAL = "interval";
    private static final String ITEM_UNITS = "units";

    private static final String UNITS_SECONDS = "s";
    private static final String UNITS_MINUTES = "m";
    private static final String UNITS_HOURS = "h";

    // Maps values to labels for the units select list.
    private static final LinkedHashMap<String, String> VALUE_MAP = new LinkedHashMap<String, String>();
    static {
        VALUE_MAP.put(UNITS_SECONDS, MSG.common_label_seconds());
        VALUE_MAP.put(UNITS_MINUTES, MSG.common_label_minutes());
        VALUE_MAP.put(UNITS_HOURS, MSG.common_label_hours());
    }

    private AbstractMeasurementScheduleListView schedulesView;
    private DynamicForm form;
    private LocatableButton setButton;

    public UpdateCollectionIntervalWidget(String locatorId, AbstractMeasurementScheduleListView schedulesView) {
        super(locatorId);
        this.schedulesView = schedulesView;
    }

    @Override
    protected void onDraw() {
        super.onDraw();

        this.form = new LocatableDynamicForm(this.getLocatorId());
        this.form.setNumCols(3);

        final IntegerItem intervalItem = new IntegerItem();
        intervalItem.setWrapTitle(false);
        intervalItem.setWidth(75);
        intervalItem.setName(ITEM_INTERVAL);
        intervalItem.setTitle(MSG.view_inventory_collectionInterval());
        final IntegerRangeValidator integerRangeValidator = new IntegerRangeValidator();
        integerRangeValidator.setMin(1);
        integerRangeValidator.setMax(10000000); // avoids exceptions if someone enters really large nums; no one needs to go higher anyway
        intervalItem.setValidators(integerRangeValidator);
        intervalItem.addChangedHandler(new ChangedHandler() {
            public void onChanged(ChangedEvent changedEvent) {
                refresh(UpdateCollectionIntervalWidget.this.schedulesView.getListGrid());
            }
        });

        // Specify a null title so no label is rendered to the left of the combo box.
        SelectItem unitsItem = new SelectItem(ITEM_UNITS, null);
        unitsItem.setValueMap(VALUE_MAP);
        unitsItem.setDefaultValue(UNITS_MINUTES);
        unitsItem.setShowTitle(false);
        unitsItem.setWidth(125);
        unitsItem.addChangedHandler(new ChangedHandler() {
            @Override
            public void onChanged(ChangedEvent event) {
                String value = (String) event.getValue();
                if (value.equals(UNITS_SECONDS)) {
                    integerRangeValidator.setMin(30);
                } else {
                    integerRangeValidator.setMin(1);
                }
                refresh(UpdateCollectionIntervalWidget.this.schedulesView.getListGrid());
            }
        });

        if (!schedulesView.hasManageMeasurementsPermission()) {
            intervalItem.setDisabled(true);
            unitsItem.setDisabled(true);
        }

        this.form.setFields(intervalItem, unitsItem);
        addMember(this.form);

        this.setButton = new LocatableButton(this.extendLocatorId("Set"), MSG.common_button_set());
        this.setButton.setDisabled(true);
        this.setButton.addClickHandler(new ClickHandler() {
            public void onClick(ClickEvent clickEvent) {
                form.validate();
                UpdateCollectionIntervalWidget.this.schedulesView.getDataSource().updateSchedules(
                    UpdateCollectionIntervalWidget.this.schedulesView, getInterval());
            }
        });
        this.setButton.setOverflow(Overflow.VISIBLE);
        this.setButton.setMargin(3);
        addMember(this.setButton);
    }

    @Override
    public void refresh(ListGrid listGrid) {
        if (isDrawn()) {
            boolean isValid = this.form.validate();
            int count = listGrid.getSelection().length;
            Long interval = getInterval();
            this.setButton.setDisabled(!isValid || count == 0 || interval == null);
        } else {
            markForRedraw();
        }
    }

    private Long getInterval() {
        IntegerItem item = (IntegerItem) this.form.getItem(ITEM_INTERVAL);
        if (item.getValue() == null || !item.validate()) {
            return null;
        }
        String stringValue = this.form.getValueAsString(ITEM_INTERVAL);
        long longValue = Long.valueOf(stringValue);

        String units = this.form.getValueAsString(ITEM_UNITS);
        longValue *= 1000;
        if (units.equals(UNITS_MINUTES)) {
            longValue *= 60;
        } else if (units.equals(UNITS_HOURS)) {
            longValue *= 60 * 60;
        }
        return longValue;
    }

}
