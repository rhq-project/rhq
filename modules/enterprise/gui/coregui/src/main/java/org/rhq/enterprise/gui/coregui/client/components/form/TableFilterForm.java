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

import com.smartgwt.client.widgets.form.DynamicForm;
import com.smartgwt.client.widgets.form.fields.FormItem;
import com.smartgwt.client.widgets.form.fields.SelectItem;
import com.smartgwt.client.widgets.form.fields.TextItem;
import com.smartgwt.client.widgets.form.fields.events.ChangedEvent;
import com.smartgwt.client.widgets.form.fields.events.ChangedHandler;
import com.smartgwt.client.widgets.form.fields.events.KeyPressEvent;
import com.smartgwt.client.widgets.form.fields.events.KeyPressHandler;

import org.rhq.enterprise.gui.coregui.client.components.table.Table;

/**
 * A subclass of SmartGWT's DynamicForm widget that provides a more convenient interface for filtering a {@link Table} 
 * of results.
 *
 * @author Joseph Marques 
 */
public class TableFilterForm extends DynamicForm implements KeyPressHandler, ChangedHandler {

    private Table table;

    public TableFilterForm(Table table) {
        super();
        setWidth100();
        this.table = table;
        this.table.setTableTitle(null);
    }

    @Override
    public void setItems(FormItem... items) {
        super.setItems(items);
        setupFormItems(items);
    }

    private void setupFormItems(FormItem... formItems) {
        for (FormItem nextFormItem : formItems) {
            nextFormItem.setWrapTitle(false);
            nextFormItem.setWidth(300); // wider than default
            if (nextFormItem instanceof TextItem) {
                nextFormItem.addKeyPressHandler(this);
            } else if (nextFormItem instanceof SelectItem) {
                nextFormItem.addChangedHandler(this);
            }
        }
    }

    private void fetchFilteredTableData() {
        table.refresh(getValuesAsCriteria());
    }

    public void onKeyPress(KeyPressEvent event) {
        if (event.getKeyName().equals("Enter") == false) {
            return;
        }
        fetchFilteredTableData();
    }

    public void onChanged(ChangedEvent event) {
        fetchFilteredTableData();
    }

}
