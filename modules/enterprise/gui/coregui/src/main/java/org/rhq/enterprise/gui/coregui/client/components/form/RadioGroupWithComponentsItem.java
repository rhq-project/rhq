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

/**
 * @author Greg Hinkle
 */
public class RadioGroupWithComponentsItem extends CanvasItem {

    private final LinkedHashMap<String, ? extends Canvas> valueMap;
    private final RGWCCanvas canvas;
    private final DynamicForm form;

    public RadioGroupWithComponentsItem(String name, String title, LinkedHashMap<String, ? extends Canvas> valueMap,
        DynamicForm form) {

        super(name, title);
        this.valueMap = valueMap;
        this.form = form;
        this.canvas = new RGWCCanvas();
        setCanvas(this.canvas);
    }

    public class RGWCCanvas extends DynamicForm {
        public RGWCCanvas() {
            setNumCols(3);
        }

        @Override
        protected void onInit() {
            super.onInit();

            ArrayList<FormItem> items = new ArrayList<FormItem>();

            for (final String label : valueMap.keySet()) {
                RadioGroupItem button = new RadioGroupItem(getName(), label);
                button.setShowTitle(false);
                button.setStartRow(true);
                button.setValueMap(label);
                items.add(button);

                Canvas value = valueMap.get(label);
                CanvasItem ci = new CanvasItem();
                ci.setShowTitle(false);
                ci.setCanvas(value);
                items.add(ci);

                button.addChangedHandler(new ChangedHandler() {
                    public void onChanged(ChangedEvent changedEvent) {
                        form.setValue(getName(), label);
                        updateEnablement();
                    }
                });
            }
            setItems(items.toArray(new FormItem[items.size()]));
        }

        public void updateEnablement() {
            String formValue = form.getValueAsString(getName());
            for (String key : valueMap.keySet()) {
                Canvas value = valueMap.get(key);
                if (value != null && value instanceof DynamicForm) {
                    for (FormItem item : ((DynamicForm) value).getFields()) {
                        item.setDisabled(!formValue.equals(key));
                    }
                }
            }
        }
    }

}
