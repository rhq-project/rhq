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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import com.smartgwt.client.types.DSOperationType;
import com.smartgwt.client.widgets.form.fields.BooleanItem;
import com.smartgwt.client.widgets.form.fields.CanvasItem;
import com.smartgwt.client.widgets.form.fields.CheckboxItem;
import com.smartgwt.client.widgets.form.fields.FormItem;
import com.smartgwt.client.widgets.form.fields.HiddenItem;
import com.smartgwt.client.widgets.form.fields.SpacerItem;
import com.smartgwt.client.widgets.form.fields.StaticTextItem;

import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableDynamicForm;

/**
 * A subclass of SmartGWT's DynamicForm widget that can provides additional layout settings and features.
 *
 * @author Ian Springer
 */
public class EnhancedDynamicForm extends LocatableDynamicForm {

    private static final String FIELD_ID = "id";

    private boolean isReadOnly;

    public EnhancedDynamicForm(String locatorId) {
        this(locatorId, false);
    }

    public EnhancedDynamicForm(String locatorId, boolean readOnly) {
        this(locatorId, readOnly, false);
    }

    public EnhancedDynamicForm(String locatorId, boolean readOnly, boolean isNewRecord) {
        super(locatorId);

        this.isReadOnly = readOnly;
        if (isNewRecord) {
            setSaveOperationType(DSOperationType.ADD);
        }

        // Layout Settings
        //setWidth(640);
        //setWidth100();
        setCellPadding(3);

        // Default to 4 columns, i.e.: itemOneTitle | itemOneValue | itemTwoTitle | itemTwoValue
        setNumCols(4);
        setColWidths(75, 200, 75, 200);
        //setTitleWidth(100);
        setWrapItemTitles(false);

        // Other Display Settings
        if (readOnly) {
            setHiliteRequiredFields(false);
        } else {
            setHiliteRequiredFields(true);
            setRequiredTitleSuffix(" <span class='requiredFieldMarker'>*</span> :");
        }

        // DataSource Settings        
        setUseAllDataSourceFields(false);
        setAutoFetchData(false);

        // Validation Settings        
        setStopOnError(false);
    }

    @Override
    // NOTE: It's important to override setFields(), rather than setItems(), since setItems() is an alias method
    //       which simply delegates to setFields().
    public void setFields(FormItem... items) {
        List<FormItem> itemsList = new ArrayList<FormItem>();
        boolean hasIdField = false;
        for (FormItem item : items) {
            if (item.getName().equals(FIELD_ID)) {
                hasIdField = true;
            }
            if (this.isReadOnly) {
                if ((item instanceof StaticTextItem) || (item instanceof CanvasItem) || (item instanceof SpacerItem)) {
                    // note: EditableFormItem is a subclass of CanvasItem
                    if (item instanceof EditableFormItem) {
                        ((EditableFormItem) item).setReadOnly(true);
                    }
                    itemsList.add(item);
                } else {
                    StaticTextItem staticItem = new StaticTextItem(item.getName(), item.getTitle());
                    Boolean showTitle = (item.getShowTitle() != null) ? item.getShowTitle() : true;
                    staticItem.setShowTitle(showTitle);
                    staticItem.setTooltip(item.getTooltip());
                    staticItem.setValue(item.getDisplayValue());
                    staticItem.setColSpan(item.getAttribute("colSpan"));
                    // TODO: Any other fields we should copy? icons?

                    if ((item instanceof BooleanItem) || (item instanceof CheckboxItem)) {
                        LinkedHashMap<String, String> valueMap = new LinkedHashMap<String, String>();
                        valueMap.put("true", MSG.common_val_yes());
                        valueMap.put("false", MSG.common_val_no());
                        staticItem.setValueMap(valueMap);
                    }

                    itemsList.add(staticItem);
                }
            } else {
                itemsList.add(item);
            }
        }

        // If the dataSource has an "id" field, make sure the form does too.
        if (!hasIdField && getDataSource() != null && getDataSource().getField(FIELD_ID) != null) {
            FormItem idItem;
            if (isNewRecord() != null && isNewRecord() || !CoreGUI.isDebugMode()) {
                idItem = new HiddenItem(FIELD_ID);
            } else {
                idItem = new StaticTextItem(FIELD_ID, MSG.common_title_id());
            }
            itemsList.add(0, idItem);
        }

        for (FormItem item : itemsList) {
            if (item instanceof StaticTextItem) {
                item.setRequired(false);
            } else {
                item.setValidateOnExit(true);
            }

            //item.setWidth("*"); // this causes a JavaScript exception ...  :-(
            item.setWidth(195);
        }

        super.setFields((FormItem[]) itemsList.toArray(new FormItem[itemsList.size()]));
    }

    public boolean isReadOnly() {
        return isReadOnly;
    }

}
