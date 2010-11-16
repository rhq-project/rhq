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

import java.util.Date;
import java.util.LinkedHashMap;

import com.smartgwt.client.widgets.form.fields.ButtonItem;
import com.smartgwt.client.widgets.form.fields.DateTimeItem;
import com.smartgwt.client.widgets.form.fields.SelectItem;
import com.smartgwt.client.widgets.form.fields.StaticTextItem;
import com.smartgwt.client.widgets.form.fields.events.ClickEvent;
import com.smartgwt.client.widgets.form.fields.events.ClickHandler;

import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableDynamicForm;

/**
 * @author Greg Hinkle
 */
public class MeasurementRangeEditor extends LocatableDynamicForm {

    private static LinkedHashMap<String, String> lastValues;

    private boolean advanced = false;
    private ButtonItem advancedButton;

    static {
        lastValues = new LinkedHashMap<String, String>();
        lastValues.put("10", MSG.view_measureRange_minutes("10"));
        lastValues.put("30", MSG.view_measureRange_minutes("30"));
        lastValues.put("60", MSG.view_measureRange_hour());
        lastValues.put("120", MSG.view_measureRange_hours("2"));
        lastValues.put("240", MSG.view_measureRange_hours("4"));
        lastValues.put("480", MSG.view_measureRange_hours("8"));
        lastValues.put("720", MSG.view_measureRange_hours("12"));
        lastValues.put("1440", MSG.view_measureRange_day());
        lastValues.put("2880", MSG.view_measureRange_days("2"));
        lastValues.put("10080", MSG.view_measureRange_days("7"));

    }

    public MeasurementRangeEditor(String locatorId) {
        super(locatorId);

        setNumCols(10);
        setHeight(40);
        setWrapItemTitles(false);
        setWidth(250);
    }

    @Override
    protected void onDraw() {
        super.onDraw();

        StaticTextItem title = new StaticTextItem("title", MSG.view_measureRange_range());

        SelectItem timeItem = new SelectItem("last", MSG.view_measureRange_last());
        timeItem.setValueMap(lastValues);

        DateTimeItem startItem = new DateTimeItem("start", MSG.common_title_start());
        startItem.setValue(new Date(System.currentTimeMillis() - (1000L * 60 * 60 * 24)));

        DateTimeItem endItem = new DateTimeItem("end", MSG.common_title_end());
        endItem.setValue(new Date());

        final StaticTextItem display = new StaticTextItem("display");
        display.setShowTitle(false);

        advancedButton = new ButtonItem("advanced", MSG.common_button_advanced());
        advancedButton.setShowTitle(false);
        advancedButton.setStartRow(false);
        advancedButton.setEndRow(false);
        advancedButton.addClickHandler(new ClickHandler() {
            public void onClick(ClickEvent clickEvent) {
                advanced = !advanced;

                update();

            }
        });

        setItems(title, advancedButton, timeItem, startItem, endItem);

        update();
    }

    private void update() {
        if (advanced) {
            advancedButton.setTitle(MSG.view_measureRange_simple());
            hideItem("last");
            showItem("start");
            showItem("end");
        } else {
            advancedButton.setTitle(MSG.common_button_advanced());
            hideItem("start");
            hideItem("end");
            showItem("last");
        }

    }
}
