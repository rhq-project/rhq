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
package org.rhq.enterprise.gui.coregui.client.inventory.groups.definitions;

import com.smartgwt.client.types.ListGridFieldType;
import com.smartgwt.client.types.SelectionAppearance;
import com.smartgwt.client.types.SelectionStyle;
import com.smartgwt.client.widgets.grid.CellFormatter;
import com.smartgwt.client.widgets.grid.ListGridField;
import com.smartgwt.client.widgets.grid.ListGridRecord;
import com.smartgwt.client.widgets.layout.VLayout;

import org.rhq.enterprise.gui.coregui.client.components.table.Table;

/**
 * @author Greg Hinkle
 */
public class GroupDefinitionListView extends VLayout {

    public GroupDefinitionListView() {

        setWidth100();
        setHeight100();

        final GroupDefinitionDataSource datasource = new GroupDefinitionDataSource();

        Table table = new Table("Group Definitions");
        table.setDataSource(datasource);

        table.getListGrid().setSelectionType(SelectionStyle.SIMPLE);
        table.getListGrid().setSelectionAppearance(SelectionAppearance.CHECKBOX);

        ListGridField idField = new ListGridField("id", "Id", 55);
        idField.setType(ListGridFieldType.INTEGER);
        ListGridField nameField = new ListGridField("name", "Name", 250);
        nameField.setCellFormatter(new CellFormatter() {
            public String format(Object o, ListGridRecord listGridRecord, int i, int i1) {
                return "<a href=\"#ResourceGroupDefinition/" + listGridRecord.getAttribute("id") + "\">" + o + "</a>";
            }
        });

        //ListGridField descriptionField = new ListGridField("description", "Description");
        ////        ListGridField typeNameField = new ListGridField("typeName", "Type", 130);
        ////        ListGridField pluginNameField = new ListGridField("pluginName", "Plugin", 100);
        ////        ListGridField categoryField = new ListGridField("category", "Category", 60);
        ////
        ////        ListGridField availabilityField = new ListGridField("currentAvailability", "Availability", 55);
        //
        //        availabilityField.setAlign(Alignment.CENTER);
        //        listGrid.setFields(idField, nameField, descriptionField, typeNameField, pluginNameField, categoryField, availabilityField);

        addMember(table);

    }

}