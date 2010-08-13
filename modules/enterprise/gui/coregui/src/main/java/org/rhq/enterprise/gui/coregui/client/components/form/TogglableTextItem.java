/*
 * RHQ Management Platform
 * Copyright (C) 2010 Red Hat, Inc.
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

import com.google.gwt.core.client.JavaScriptObject;
import com.smartgwt.client.widgets.form.fields.TextItem;

import java.util.ArrayList;
import java.util.List;

/**
 * TODO
 *
 * @author Ian Springer
 */
public class TogglableTextItem extends TextItem {
    List<ValueUpdatedHandler> valueUpdatedHandlers = new ArrayList<ValueUpdatedHandler>();

    public TogglableTextItem() {
    }

    public TogglableTextItem(JavaScriptObject jsObj) {
        super(jsObj);
    }

    public TogglableTextItem(String name) {
        super(name);
    }

    public TogglableTextItem(String name, String title) {
        super(name, title);
    }

    public void addValueUpdatedHandler(ValueUpdatedHandler handler) {
        this.valueUpdatedHandlers.add(handler);
    }

    public List<ValueUpdatedHandler> getValueUpdatedHandlers() {
        return valueUpdatedHandlers;
    }
}
