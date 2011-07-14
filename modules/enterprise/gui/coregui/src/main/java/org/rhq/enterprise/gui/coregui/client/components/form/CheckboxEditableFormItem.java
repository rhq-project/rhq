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

import com.smartgwt.client.widgets.form.fields.CheckboxItem;
import com.smartgwt.client.widgets.form.fields.FormItem;

public class CheckboxEditableFormItem extends EditableFormItem {
    private boolean yesNoFlag = true;

    public CheckboxEditableFormItem() {
        super();
    }

    public CheckboxEditableFormItem(String name, String title) {
        super(name, title);
    }

    public CheckboxEditableFormItem(String name, String title, ValueEditedHandler handler) {
        super(name, title, handler);
    }

    /**
     * If you want the static item to show "yes" for true and "no" for false, set this
     * flag to true. If you want the static item to show the actual boolean
     * value ("true" or "false"), this flag should be set to false.
     * 
     * @param yesNoFlag true if yes/no should be shown in the static text (default is true)
     */
    public void setYesNo(boolean yesNoFlag) {
        this.yesNoFlag = yesNoFlag;
    }

    @Override
    protected FormItem instantiateEditFormItem() {
        CheckboxItem item = new CheckboxItem();
        item.setTitle(" ");
        return item;
    }

    @Override
    protected Object setEditItemValue(FormItem editItem, Object value) {
        boolean convertedValue; // note: this MUST be of primitive boolean type, not Boolean

        if (value == null) {
            value = Boolean.FALSE;
        }
        if (value instanceof Boolean) {
            convertedValue = ((Boolean) value).booleanValue();
        } else {
            String str = value.toString();
            if (str.equalsIgnoreCase(MSG.common_val_yes_lower())) {
                convertedValue = true;
            } else if (str.equalsIgnoreCase(MSG.common_val_no_lower())) {
                convertedValue = false;
            } else {
                convertedValue = Boolean.parseBoolean(str);
            }
        }

        editItem.setValue(convertedValue); // this doesn't work if convertedValue is of type java.lang.Boolean
        return convertedValue;
    }

    @Override
    protected Object setStaticItemValue(FormItem staticItem, Object value) {
        String convertedValue;

        if (value == null) {
            convertedValue = "";
        }

        if (value instanceof Boolean) {
            if (this.yesNoFlag) {
                if (((Boolean) value).booleanValue()) {
                    convertedValue = MSG.common_val_yes();
                } else {
                    convertedValue = MSG.common_val_no();
                }
            } else {
                convertedValue = value.toString();
            }
        } else {
            convertedValue = value.toString();
        }

        staticItem.setValue(convertedValue);
        return convertedValue;
    }
}
