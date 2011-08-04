/*
 * RHQ Management Platform
 * Copyright (C) 2005-2011 Red Hat, Inc.
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

import com.smartgwt.client.widgets.form.ColorPicker;
import com.smartgwt.client.widgets.form.events.ColorSelectedEvent;
import com.smartgwt.client.widgets.form.events.ColorSelectedHandler;
import com.smartgwt.client.widgets.form.fields.ButtonItem;
import com.smartgwt.client.widgets.form.fields.events.ClickEvent;
import com.smartgwt.client.widgets.form.fields.events.ClickHandler;

import org.rhq.enterprise.gui.coregui.client.CoreGUI;

/**
 * A button that lets you pick a color. This is different than the smartgwt
 * ColorPickerItem this is just a button - it doesn't let you enter a text
 * string for any color. You can only pick a color from the dialog.
 * 
 * @author John Mazzitelli
 */
public class ColorButtonItem extends ButtonItem {

    // can't figure out how to change the color of this icon
    //private static final String ICON_PATH = "[SKIN]/DynamicForm/PropSheet_ColorPicker_icon.png";

    private String currentColor;
    private ColorSelectedHandler colorSelectedHandler;

    public ColorButtonItem() {
        super();
        prepareWidget();
    }

    public ColorButtonItem(String name) {
        super(name);
        prepareWidget();
    }

    public ColorButtonItem(String name, String title) {
        super(name, title);
        prepareWidget();
    }

    public String getCurrentColor() {
        return this.currentColor;
    }

    public void setCurrentColor(String color) {
        this.currentColor = color;
    }

    public void setColorSelectedHandler(ColorSelectedHandler handler) {
        this.colorSelectedHandler = handler;
    }

    protected void prepareWidget() {
        setPrompt(CoreGUI.getMessages().widget_colorPicker_tooltip());

        addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                ColorPicker picker = new ColorPicker();
                picker.setAllowComplexMode(false);
                if (currentColor != null) {
                    picker.setDefaultColor(currentColor);
                }
                picker.addColorSelectedHandler(new ColorSelectedHandler() {
                    @Override
                    public void onColorSelected(ColorSelectedEvent event) {
                        if (colorSelectedHandler != null) {
                            colorSelectedHandler.onColorSelected(event);
                        }
                        currentColor = event.getColor();
                    }
                });
                picker.show();
            }
        });
    }
}