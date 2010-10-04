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
package org.rhq.enterprise.gui.coregui.client.components.table;

import java.util.Map;

import com.smartgwt.client.data.Record;
import com.smartgwt.client.widgets.form.fields.FormItem;
import com.smartgwt.client.widgets.grid.ListGrid;
import com.smartgwt.client.widgets.grid.ListGridField;
import com.smartgwt.client.widgets.grid.events.EditorEnterEvent;
import com.smartgwt.client.widgets.grid.events.EditorEnterHandler;
import com.smartgwt.client.widgets.grid.events.RecordClickEvent;
import com.smartgwt.client.widgets.grid.events.RecordClickHandler;


/**
 * A PropertyGrid based on a {@link ListGrid} that automatically creates two
 * fields (one for name column and other for values column) and can display a
 * custom editor for every row
 *
 * @author Dan Mihai Ile
 */
public class PropertyGrid extends ListGrid {
    /**
     * Map to hold the editors
     */
    private Map<String, FormItem> editorsMap;

    /**
     * Hold the name of the attribute on a {@link Record} which contains the
     * editorsMap key
     */
    private String valueFieldAttribute;

    /**
     * Create a new {@link PropertyGrid} with two column fields (first for
     * names, second for values)
     */
    public PropertyGrid() {

        // Create the 2 fields - one for names, other for values.
        ListGridField nameField = new ListGridField();
        nameField.setCanEdit(false);
        ListGridField valueField = new ListGridField();
        valueField.setCanEdit(true);

        setFields(nameField, valueField);

        // listeners to intercept when the user starts to edit data
        addEditorEnterHandler(new EditorEnterHandler() {
            public void onEditorEnter(EditorEnterEvent event) {
                Record record = event.getRecord();
                enableSpecificEditor(record);
            }
        });
        addRecordClickHandler(new RecordClickHandler() {
            public void onRecordClick(RecordClickEvent event) {
                Record record = event.getRecord();
                enableSpecificEditor(record);
            }
        });

    }

    /**
     * The name column field
     *
     * @return
     */
    public ListGridField getNameField() {
        return getField(0);
    }

    /**
     * The values column field
     *
     * @return
     */
    public ListGridField getValuesField() {
        return getField(1);
    }

    /**
     * Set the name of the attribute from where the row will get the editor name
     * to be used on the {@link Map} from where the rows will get its editors
     * <p/>
     * When an edit starts the {@link PropertyGrid} will retrieve the key of the
     * editor form the row's attributes and use that key on the map to get the
     * editor to be used
     *
     * @param attribute the name of the attribute on the rows that contains the key of
     *                  the editor
     * @param map       a map that contains the key of the editor and the editor
     */
    public void setEditorsMap(String attribute, Map<String, FormItem> map) {
        this.valueFieldAttribute = attribute;
        this.editorsMap = map;
    }

    /**
     * Enable the specific editor for the given record
     *
     * @param record the record
     */
    public void enableSpecificEditor(Record record) {
        boolean readOnly = record.getAttributeAsBoolean("readOnly");
        if (editorsMap != null && !readOnly) {
            String attribute = record.getAttribute(valueFieldAttribute);
            if (editorsMap.containsKey(attribute)) {
                FormItem formItem = editorsMap.get(attribute);
				getField(1).setEditorType(formItem);
			}
		}
	}
}