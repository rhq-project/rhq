/*
 * RHQ Management Platform
 * Copyright (C) 2010 Red Hat, Inc.
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

import com.smartgwt.client.widgets.IButton;
import com.smartgwt.client.widgets.events.ClickEvent;
import com.smartgwt.client.widgets.events.ClickHandler;
import com.smartgwt.client.widgets.form.DynamicForm;
import com.smartgwt.client.widgets.form.fields.FormItem;
import com.smartgwt.client.widgets.form.fields.IntegerItem;
import com.smartgwt.client.widgets.form.fields.SelectItem;
import com.smartgwt.client.widgets.form.fields.events.ChangedEvent;
import com.smartgwt.client.widgets.form.fields.events.ChangedHandler;
import com.smartgwt.client.widgets.form.validator.IntegerRangeValidator;
import com.smartgwt.client.widgets.grid.ListGrid;
import com.smartgwt.client.widgets.layout.VLayout;

import org.rhq.enterprise.gui.coregui.client.components.table.TableWidget;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableDynamicForm;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableHLayout;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableIButton;

/**
 * Widget for updating the collection intervals for the selected metrics. It displays two form fields for
 * entering the interval and the interval's units (seconds, minutes, or hours), as well as a Set button
 * for submitting the update.
 *
 * @author Ian Springer
 */
public class UpdateCollectionIntervalWidget extends LocatableHLayout implements TableWidget {
    private static final String SECONDS = "s";
    private static final String MINUTES = "m";
    private static final String HOURS = "h";

    private AbstractMeasurementScheduleListView schedulesView;
    private DynamicForm form;
    private IButton setButton;

    public UpdateCollectionIntervalWidget(String locatorId, AbstractMeasurementScheduleListView schedulesView) {
        super(locatorId);
        this.schedulesView = schedulesView;
    }

    @Override
    protected void onDraw() {
        super.onDraw();

        VLayout spacer = new VLayout();
        spacer.setWidth(20);
        addMember(spacer);

        this.form = new LocatableDynamicForm(this.getLocatorId());
        this.form.setNumCols(3);
        IntegerItem intervalItem = new IntegerItem();
        intervalItem.setName("interval");
        intervalItem.setTitle("Collection Interval");
        IntegerRangeValidator integerRangeValidator = new IntegerRangeValidator();
        integerRangeValidator.setMin(1);
        intervalItem.setValidators(integerRangeValidator);
        intervalItem.setValidateOnChange(true);
        intervalItem.addChangedHandler(new ChangedHandler() {
            public void onChanged(ChangedEvent changedEvent) {
                refresh(UpdateCollectionIntervalWidget.this.schedulesView.getListGrid());
            }
        });
        // Specify a null title so no label is rendered to the left of the combo box.
        SelectItem unitsItem = new SelectItem("units", null);
        LinkedHashMap<String, String> map = new LinkedHashMap<String, String>(3);
        map.put(SECONDS, "seconds");
        map.put(MINUTES, "minutes");
        map.put(HOURS, "hours");
        unitsItem.setValueMap(map);
        unitsItem.setDefaultValue(SECONDS);
        unitsItem.setShowTitle(false);
        this.form.setFields(intervalItem, unitsItem);
        addMember(this.form);

        this.setButton = new LocatableIButton(this.extendLocatorId("Set"), "Set");
        this.setButton.setDisabled(true);
        this.setButton.addClickHandler(new ClickHandler() {
            public void onClick(ClickEvent clickEvent) {
                form.validate();
                UpdateCollectionIntervalWidget.this.schedulesView.getDataSource().updateSchedules(
                    UpdateCollectionIntervalWidget.this.schedulesView, getInterval());
            }
        });
        addMember(this.setButton);
    }

    @Override
    public void refresh(ListGrid listGrid) {
        if (isDrawn()) {
            int count = listGrid.getSelection().length;
            Long interval = getInterval();
            this.setButton.setDisabled(count == 0 || interval == null);
        } else {
            markForRedraw();
        }
    }

    private Long getInterval() {
        FormItem item = this.form.getItem("interval");
        if (item.getValue() == null || !item.validate()) {
            return null;
        }
        String stringValue = this.form.getValueAsString("interval");
        long value = Long.valueOf(stringValue.trim());
        String units = this.form.getValueAsString("units");
        value *= 1000;
        if (units.equals(MINUTES)) {
            value *= 60;
        } else if (units.equals(HOURS)) {
            value *= 60 * 60;
        }
        return value;
    }
}
