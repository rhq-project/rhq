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

import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.grid.CellFormatter;
import com.smartgwt.client.widgets.grid.ListGrid;
import com.smartgwt.client.widgets.grid.ListGridRecord;

import org.rhq.enterprise.gui.coregui.client.components.table.Table;
import org.rhq.enterprise.gui.coregui.client.components.table.TableAction;
import org.rhq.enterprise.gui.coregui.client.components.table.TableSection;

/**
 * @author Greg Hinkle
 * @author Joseph Marques
 */
public class GroupDefinitionListView extends TableSection {

    public GroupDefinitionListView(String locatorId, String headerIcon) {
        super(locatorId, "Group Definitions");

        setHeaderIcon(headerIcon);

        setDataSource(new GroupDefinitionDataSource());
    }

    @Override
    protected void configureTable() {
        super.configureTable();

        ListGrid grid = getListGrid();

        grid.getField("nextCalculationTime").setCellFormatter(new CellFormatter() {
            public String format(Object value, ListGridRecord record, int rowNum, int colNum) {
                if ("0".equals(value.toString())) {
                    return "N/A";
                }
                return value.toString();
            }
        });

        addTableAction(extendLocatorId("New"), "New", Table.SelectionEnablement.ALWAYS, null, new TableAction() {
            public void executeAction(ListGridRecord[] selection) {
                newDetails();
            }
        });
    }

    @Override
    public Canvas getDetailsView(int id) {
        final SingleGroupDefinitionView singleGroupDefinitionView = new SingleGroupDefinitionView(this
            .extendLocatorId("Empty"));
        return singleGroupDefinitionView;
    }

}