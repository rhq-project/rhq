/*
 * RHQ Management Platform
 * Copyright (C) 2005-2010 Red Hat, Inc.
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

import java.util.ArrayList;
import java.util.LinkedHashMap;

import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.form.DynamicForm;
import com.smartgwt.client.widgets.form.fields.CanvasItem;
import com.smartgwt.client.widgets.form.fields.FormItem;
import com.smartgwt.client.widgets.form.fields.RadioGroupItem;
import com.smartgwt.client.widgets.form.fields.events.ChangedEvent;
import com.smartgwt.client.widgets.form.fields.events.ChangedHandler;

import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableDynamicForm;

/**
 * @author Greg Hinkle
 */
public class RadioGroupWithComponentsItem extends CanvasItem {

    private final LinkedHashMap<String, ? extends Canvas> valueMap;
    private final RGWCCanvas canvas;
    private final DynamicForm form;
    private String selected;

    public RadioGroupWithComponentsItem(String name, String title, LinkedHashMap<String, ? extends Canvas> valueMap,
        DynamicForm form) {

        super(name, title);
        this.valueMap = valueMap;
        this.form = form;
        // since the name is an internal identifier I think it can be used as the locatorId
        this.canvas = new RGWCCanvas(name);
        this.selected = null;
        setCanvas(this.canvas);
    }

    public String getSelected() {
        return this.selected;
    }

    public Canvas getSelectedComponent() {
        if (null == this.selected) {
            return null;
        }

        return valueMap.get(this.selected);
    }

    public class RGWCCanvas extends LocatableDynamicForm {

        public RGWCCanvas(String locatorId) {
            super(locatorId);
            setNumCols(3);
        }

        @Override
        protected void onInit() {
            super.onInit();

            ArrayList<FormItem> items = new ArrayList<FormItem>();

            for (final String label : valueMap.keySet()) {
                RadioGroupItem button = new RadioGroupItem(label, label);
                button.setShowTitle(false);
                button.setStartRow(true);
                button.setValueMap(label);
                items.add(button);

                Canvas value = valueMap.get(label);
                CanvasItem ci = new CanvasItem();
                ci.setShowTitle(false);
                if (value != null) {
                    ci.setCanvas(value);
                }
                ci.setDisabled(true);
                items.add(ci);

                button.addChangedHandler(new ChangedHandler() {
                    public void onChanged(ChangedEvent changedEvent) {
                        selected = (String) changedEvent.getValue();
                        updateEnablement();
                        form.markForRedraw();
                    }
                });
            }
            this.setItems(items.toArray(new FormItem[items.size()]));
        }

        public void updateEnablement() {

            for (String key : valueMap.keySet()) {
                Canvas value = valueMap.get(key);
                Boolean disabled = !selected.equals(key);
                if (disabled) {
                    canvas.getItem(key).clearValue();
                    canvas.getItem(key).redraw();
                }
                if (value != null && value instanceof DynamicForm) {
                    if (!disabled.equals(value.isDisabled())) {
                        value.setDisabled(disabled);
                        for (FormItem item : ((DynamicForm) value).getFields()) {
                            item.clearValue();
                            item.redraw();
                        }
                        value.markForRedraw();
                    }
                }
            }
        }
    }

}
