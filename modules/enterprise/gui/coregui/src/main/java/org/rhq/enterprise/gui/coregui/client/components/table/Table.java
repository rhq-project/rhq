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

import com.smartgwt.client.data.SortSpecifier;
import org.rhq.enterprise.gui.coregui.client.util.RPCDataSource;

import com.smartgwt.client.data.Criteria;
import com.smartgwt.client.types.Autofit;
import com.smartgwt.client.util.BooleanCallback;
import com.smartgwt.client.util.SC;
import com.smartgwt.client.widgets.HTMLFlow;
import com.smartgwt.client.widgets.IButton;
import com.smartgwt.client.widgets.Label;
import com.smartgwt.client.widgets.events.ClickEvent;
import com.smartgwt.client.widgets.events.ClickHandler;
import com.smartgwt.client.widgets.grid.ListGrid;
import com.smartgwt.client.widgets.grid.events.DataArrivedEvent;
import com.smartgwt.client.widgets.grid.events.DataArrivedHandler;
import com.smartgwt.client.widgets.grid.events.SelectionChangedHandler;
import com.smartgwt.client.widgets.grid.events.SelectionEvent;
import com.smartgwt.client.widgets.layout.LayoutSpacer;
import com.smartgwt.client.widgets.layout.VLayout;
import com.smartgwt.client.widgets.toolbar.ToolStrip;

import java.util.ArrayList;

/**
 * @author Greg Hinkle
 */
public class Table extends VLayout {

    private HTMLFlow title;
    private ListGrid listGrid;
    private ToolStrip footer;
    private Label tableInfo;

    public enum SelectionEnablement {
        ANY, SINGLE, MULTIPLE
    };

    private ArrayList<TableActionInfo> tableActions = new ArrayList<TableActionInfo>();

    public Table(String tableTitle) {
        this(tableTitle, null, null);
    }

    public Table(String tableTitle, Criteria criteria) {
        this(tableTitle, criteria, null);
    }

    public Table(String tableTitle, SortSpecifier[] sortSpecifiers) {
        this(tableTitle, null, sortSpecifiers);
    }

    public Table(String tableTitle, Criteria criteria, SortSpecifier[] sortSpecifiers) {
        super();

        setWidth100();
        setHeight100();

        // Title
        title = new HTMLFlow();
        title.setWidth100();
        title.setHeight(35);
        title.setContents(tableTitle);
        title.setPadding(4);
        title.setStyleName("HeaderLabel");

        // Grid
        listGrid = new ListGrid();
        if (criteria != null) {
            listGrid.setInitialCriteria(criteria);
        }
        if (sortSpecifiers != null) {
            listGrid.setInitialSort(sortSpecifiers);
        }
        listGrid.setWidth100();
        listGrid.setHeight100();
        listGrid.setAutoFetchData(true);
        listGrid.setAutoFitData(Autofit.HORIZONTAL);
        listGrid.setAlternateRecordStyles(true);

        // Footer
        footer = new ToolStrip();
        footer.setPadding(5);
        footer.setWidth100();
        footer.setMembersMargin(15);

        tableInfo = new Label("Total: " + listGrid.getTotalRows());
    }


    @Override
    protected void onDraw() {
        super.onDraw();

        addMember(title);
        addMember(listGrid);
        addMember(footer);

        tableInfo.setWrap(false);

        for (final TableActionInfo tableAction : tableActions) {
            IButton button = new IButton(tableAction.title);
            button.setDisabled(true);
            button.addClickHandler(new ClickHandler() {
                public void onClick(ClickEvent clickEvent) {
                    if (tableAction.confirmMessage != null) {
                        SC.ask(tableAction.confirmMessage, new BooleanCallback() {
                            public void execute(Boolean confirmed) {
                                if (confirmed) {
                                    tableAction.action.executeAction(listGrid.getSelection());
                                }
                            }
                        });
                    } else {
                        tableAction.action.executeAction(listGrid.getSelection());
                    }
                }
            });
            tableAction.actionButton = button;
            footer.addMember(button);
        }
        footer.addMember(new LayoutSpacer());
        footer.addMember(tableInfo);


        // Manages enable/disable buttons for the grid
        listGrid.addSelectionChangedHandler(new SelectionChangedHandler() {
            public void onSelectionChanged(SelectionEvent selectionEvent) {
                refreshTableInfo();
            }
        });

        listGrid.addDataArrivedHandler(new DataArrivedHandler() {
            public void onDataArrived(DataArrivedEvent dataArrivedEvent) {
                refreshTableInfo();
            }
        });
    }

    public void setTableTitle(String title) {
        this.title.setContents(title);
    }

    public void setDataSource(RPCDataSource dataSource) {
        listGrid.setDataSource(dataSource);
    }


    public ListGrid getListGrid() {
        return listGrid;
    }


    public void addTableAction(String title, TableAction tableAction) {
        this.addTableAction(title, SelectionEnablement.ANY, null, tableAction);
    }


    public void addTableAction(String title, SelectionEnablement enablement, String confirmation, TableAction tableAction) {
        TableActionInfo info = new TableActionInfo(title, enablement, tableAction);
        info.confirmMessage = confirmation;
        tableActions.add(info);
    }

    private void refreshTableInfo() {
        int count = this.listGrid.getSelection().length;
        for (TableActionInfo tableAction : tableActions) {
            if (count == 0) {
                tableAction.actionButton.setDisabled(true);
            } else if (count == 1) {
                tableAction.actionButton.setDisabled(tableAction.enablement == SelectionEnablement.MULTIPLE);
            } else if (count > 1) {
                tableAction.actionButton.setDisabled(tableAction.enablement == SelectionEnablement.SINGLE);
            }
        }
        this.tableInfo.setContents("Total: " + listGrid.getTotalRows() + " (" + count + " selected)");
    }


    // -------------- Inner utility class -------------

    private static class TableActionInfo {

        public String title;
        public SelectionEnablement enablement;
        TableAction action;
        String confirmMessage;
        IButton actionButton;


        protected TableActionInfo(String title, SelectionEnablement enablement, TableAction action) {
            this.title = title;
            this.enablement = enablement;
            this.action = action;
        }
    }
}
