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

package org.rhq.coregui.client.components.form;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.form.DynamicForm;
import com.smartgwt.client.widgets.form.fields.CanvasItem;
import com.smartgwt.client.widgets.form.fields.FormItem;
import com.smartgwt.client.widgets.form.fields.RadioGroupItem;
import com.smartgwt.client.widgets.form.fields.events.ChangedEvent;
import com.smartgwt.client.widgets.form.fields.events.ChangedHandler;

import org.rhq.coregui.client.util.enhanced.EnhancedUtility;

/**
 * TODO
 *
 * @author Greg Hinkle
 * @author Lukas Krejci
 */
public class RadioGroupWithComponentsItem extends CanvasItem {

    private final LinkedHashMap<NameAndTitle, Canvas> valueMap;
    private LinkedHashMap<NameAndTitle, CanvasItem> canvasItems;
    private final RGWCCanvas canvas;
    private final DynamicForm form;
    private String selected;

    public RadioGroupWithComponentsItem(String name, String title, LinkedHashMap<String, ? extends Canvas> valueMap,
        DynamicForm form) {

        super(name, title);

        this.valueMap = new LinkedHashMap<NameAndTitle, Canvas>();
        for (Map.Entry<String, ? extends Canvas> entry : valueMap.entrySet()) {
            this.valueMap.put(new NameAndTitle(entry.getKey()), entry.getValue());
        }

        this.form = form;
        this.canvas = new RGWCCanvas();
        this.selected = null;
        setCanvas(this.canvas);
    }

    public String getSelected() {
        return this.selected;
    }

    public int getSelectedIndex() {
        if (selected == null) {
            return -1;
        }

        int idx = 0;
        for (NameAndTitle t : valueMap.keySet()) {
            if (selected.equals(t.getTitle())) {
                break;
            }
            ++idx;
        }

        return idx;
    }

    public void setSelected(String selected) {
        RadioGroupItem radio = (RadioGroupItem) canvas.getItem(EnhancedUtility.getSafeId(selected));
        if (radio != null) {
            this.selected = selected;
            radio.setValue(selected);
            canvas.updateEnablement();
            form.markForRedraw();
        }
    }

    public Canvas getSelectedComponent() {
        if (null == this.selected) {
            return null;
        }

        return valueMap.get(new NameAndTitle(this.selected));
    }

    private static class NameAndTitle {
        private String name;
        private String title;

        public NameAndTitle(String title) {
            name = EnhancedUtility.getSafeId(title);
            this.title = title;
        }

        public String getName() {
            return name;
        }

        public String getTitle() {
            return title;
        }

        @Override
        public int hashCode() {
            return name.hashCode();
        }

        @Override
        public boolean equals(Object other) {
            if (other == this) {
                return true;
            }

            if (!(other instanceof NameAndTitle)) {
                return false;
            }

            NameAndTitle o = (NameAndTitle) other;

            return name.equals(o.name);
        }
    }

    public class RGWCCanvas extends DynamicForm {

        public RGWCCanvas() {
            super();
            setNumCols(3);
        }

        @Override
        protected void onInit() {
            super.onInit();
            canvasItems = new LinkedHashMap<NameAndTitle, CanvasItem>();
            ArrayList<FormItem> items = new ArrayList<FormItem>();

            for (final NameAndTitle label : valueMap.keySet()) {
                RadioGroupItem button = new RadioGroupItem(label.getName(), label.getTitle());
                button.setShowTitle(false);
                button.setStartRow(true);
                button.setValueMap(label.getTitle());
                items.add(button);

                Canvas value = valueMap.get(label);
                CanvasItem ci = new CanvasItem();
                ci.setShowTitle(false);
                if (value != null) {
                    ci.setCanvas(value);
                }
                ci.setDisabled(true);
                canvasItems.put(label, ci);
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
            for (NameAndTitle key : canvasItems.keySet()) {
                CanvasItem canvasItem = canvasItems.get(key);
                Canvas nestedCanvas = canvasItem.getCanvas();
                boolean disabled = !selected.equals(key.getTitle());
                if (disabled) {
                    canvasItem.disable();
                    clearValues(nestedCanvas);
                    canvas.getItem(key.getName()).clearValue();
                    if (nestedCanvas != null) {
                        nestedCanvas.markForRedraw();
                    }
                } else {
                    canvasItem.enable();
                }
            }

        }

        private void clearValues(Canvas value) {
            if (value != null && value instanceof DynamicForm) {
                for (FormItem item : ((DynamicForm) value).getFields()) {
                    if (item instanceof CanvasItem) {
                        clearValues(((CanvasItem) item).getCanvas());
                    } else {
                        item.clearValue();
                    }
                }
            }
        }
    }

    public void destroyComponents() {
        for (Canvas canvas : valueMap.values()) {
            try {
                canvas.destroy();
            } catch (Throwable t) {
                int i = 0;
            }
        }
    }

}
