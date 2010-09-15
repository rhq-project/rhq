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

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.grid.CellFormatter;
import com.smartgwt.client.widgets.grid.ListGrid;
import com.smartgwt.client.widgets.grid.ListGridRecord;

import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.components.table.Table;
import org.rhq.enterprise.gui.coregui.client.components.table.TableAction;
import org.rhq.enterprise.gui.coregui.client.components.table.TableSection;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.gwt.ResourceGroupGWTServiceAsync;
import org.rhq.enterprise.gui.coregui.client.util.TableUtility;
import org.rhq.enterprise.gui.coregui.client.util.message.Message;
import org.rhq.enterprise.gui.coregui.client.util.message.Message.Severity;

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

        addTableAction(extendLocatorId("Delete"), "Delete", Table.SelectionEnablement.ANY, null, new TableAction() {
            public void executeAction(ListGridRecord[] selection) {
                final int[] groupDefinitionIds = TableUtility.getIds(selection);
                ResourceGroupGWTServiceAsync groupManager = GWTServiceLookup.getResourceGroupService();
                groupManager.deleteGroupDefinitions(groupDefinitionIds, new AsyncCallback<Void>() {
                    @Override
                    public void onSuccess(Void result) {
                        CoreGUI.getMessageCenter().notify(
                            new Message("Successfully deleted " + groupDefinitionIds.length + " group definitions",
                                Severity.Info));
                        GroupDefinitionListView.this.refresh();
                    }

                    @Override
                    public void onFailure(Throwable caught) {
                        CoreGUI.getErrorHandler().handleError("Failed to delete selected group definitions", caught);
                    }
                });
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