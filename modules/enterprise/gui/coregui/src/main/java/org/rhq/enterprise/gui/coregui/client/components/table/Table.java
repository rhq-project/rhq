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

import java.util.ArrayList;

import com.smartgwt.client.data.Criteria;
import com.smartgwt.client.data.SortSpecifier;
import com.smartgwt.client.types.Autofit;
import com.smartgwt.client.util.BooleanCallback;
import com.smartgwt.client.util.SC;
import com.smartgwt.client.widgets.HTMLFlow;
import com.smartgwt.client.widgets.IButton;
import com.smartgwt.client.widgets.Label;
import com.smartgwt.client.widgets.events.ClickEvent;
import com.smartgwt.client.widgets.events.ClickHandler;
import com.smartgwt.client.widgets.grid.ListGrid;
import com.smartgwt.client.widgets.grid.ListGridField;
import com.smartgwt.client.widgets.grid.events.DataArrivedEvent;
import com.smartgwt.client.widgets.grid.events.DataArrivedHandler;
import com.smartgwt.client.widgets.grid.events.FieldStateChangedEvent;
import com.smartgwt.client.widgets.grid.events.FieldStateChangedHandler;
import com.smartgwt.client.widgets.grid.events.SelectionChangedHandler;
import com.smartgwt.client.widgets.grid.events.SelectionEvent;
import com.smartgwt.client.widgets.layout.LayoutSpacer;
import com.smartgwt.client.widgets.layout.VLayout;
import com.smartgwt.client.widgets.toolbar.ToolStrip;

import org.rhq.enterprise.gui.coregui.client.util.RPCDataSource;

/**
 * @author Greg Hinkle
 * @author Ian Springer
 */
public class Table extends VLayout {
    private static final SelectionEnablement DEFAULT_SELECTION_ENABLEMENT = SelectionEnablement.ALWAYS;

    private HTMLFlow title;
    private ListGrid listGrid;
    private ToolStrip footer;
    private Label tableInfo;
    private String[] excludedFieldNames;

    /**
     * Specifies how many rows must be selected in order for a {@link TableAction} button to be enabled.
     */
    public enum SelectionEnablement {
        /**
         * Enabled no matter how many rows are selected (zero or more)
         */
        ALWAYS,
        /**
         * One or more rows are selected.
         */
        ANY,
        /**
         * Exactly one row is selected.
         */
        SINGLE,
        /**
         * Two or more rows are selected.
         */
        MULTIPLE
    }

    ;

    private ArrayList<TableActionInfo> tableActions = new ArrayList<TableActionInfo>();

    public Table(String tableTitle) {
        this(tableTitle, null, null, null, true);
    }

    public Table(String tableTitle, Criteria criteria) {
        this(tableTitle, criteria, null, null, true);
    }

    public Table(String tableTitle, SortSpecifier[] sortSpecifiers) {
        this(tableTitle, null, sortSpecifiers, null, true);
    }

    public Table(String tableTitle, boolean autoFetchData) {
        this(tableTitle, null, null, null, autoFetchData);
    }

    public Table(String tableTitle, Criteria criteria, SortSpecifier[] sortSpecifiers, String[] excludedFieldNames) {
        this(tableTitle,criteria,sortSpecifiers,excludedFieldNames,true);
    }
    public Table(String tableTitle, Criteria criteria, SortSpecifier[] sortSpecifiers, String[] excludedFieldNames,
                 boolean autoFetchData) {
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
        listGrid.setAutoFetchData(autoFetchData);
        listGrid.setAutoFitData(Autofit.HORIZONTAL);
        listGrid.setAlternateRecordStyles(true);
        listGrid.setResizeFieldsInRealTime(false);

        // Footer
        footer = new ToolStrip();
        footer.setPadding(5);
        footer.setWidth100();
        footer.setMembersMargin(15);

        tableInfo = new Label("Total: " + listGrid.getTotalRows());

        this.excludedFieldNames = excludedFieldNames;
    }

    @Override
    protected void onDraw() {
        super.onDraw();

        // NOTE: It is essential that we wait to hide any excluded fields until after super.onDraw() is called, since
        //       super.onDraw() is what actually adds the fields to the ListGrid (based on what fields are defined in
        //       the underlying datasource).
        if (this.excludedFieldNames != null) {
            for (String excludedFieldName : excludedFieldNames) {
                this.listGrid.hideField(excludedFieldName);
            }
        }

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

                        String message = tableAction.confirmMessage.replaceAll("\\#", String.valueOf(listGrid.getSelection().length));

                        SC.ask(message, new BooleanCallback() {
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

        IButton refreshButton = new IButton("Refresh");
        refreshButton.addClickHandler(new ClickHandler() {
            public void onClick(ClickEvent clickEvent) {
                listGrid.invalidateCache();
            }
        });
        footer.addMember(refreshButton);

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
                fieldSizes.clear();
                totalWidth = 0;
            }
        });


        // TODO GH: This doesn't yet work as desired to force the fields to fit to the table when you resize one of them.
        if (false) {  // If Force Fit
            listGrid.addDataArrivedHandler(new DataArrivedHandler() {
                public void onDataArrived(DataArrivedEvent dataArrivedEvent) {
                    for (ListGridField f : listGrid.getFields()) {

                        int size = listGrid.getFieldWidth(f.getName());
                        fieldSizes.add(size);
                        totalWidth += size;
                    }
                }
            });

            listGrid.addFieldStateChangedHandler(new FieldStateChangedHandler() {
                public void onFieldStateChanged(FieldStateChangedEvent fieldStateChangedEvent) {

                    if (autoSizing) {
                        return;
                    }
                    autoSizing = true;

                    ArrayList<Integer> newSizes = new ArrayList<Integer>();
                    int total = 0;
                    int resizeCol = 0;
                    int i = 0;
                    for (ListGridField f : listGrid.getFields()) {
                        int size = listGrid.getFieldWidth(f.getName());
                        newSizes.add(size);
                        total += size;
                        if (fieldSizes.get(i) != size) {
                            resizeCol = i;
                        }
                        i++;
                        System.out.println("Field " + f.getName() + " width: " + listGrid.getFieldWidth(f.getName()));
                    }

                    int diff = totalWidth - total;
                    int fieldsLeft = listGrid.getFields().length - resizeCol - 1;

                    if (fieldsLeft > 0) {
                        int perFieldSizeDiff = diff / fieldsLeft;
                        for (int j = resizeCol + 1; j < listGrid.getFields().length; j++) {
                            listGrid.resizeField(j, fieldSizes.get(j) + perFieldSizeDiff);
                        }
                    }

                    fieldSizes = newSizes;
                    markForRedraw();

                    autoSizing = false;
                }
            });
        }
    }

    private int totalWidth;
    private ArrayList<Integer> fieldSizes = new ArrayList<Integer>();
    private boolean autoSizing = false;


    public void refresh(Criteria criteria) {
        this.listGrid.setCriteria(criteria);
        this.listGrid.markForRedraw();
    }

    public void setTableTitle(String title) {
        this.title.setContents(title);
    }

    public void setDataSource(RPCDataSource dataSource) {
        listGrid.setDataSource(dataSource);
    }

    public RPCDataSource getDataSource() {
        return (RPCDataSource) listGrid.getDataSource();
    }

    public ListGrid getListGrid() {
        return listGrid;
    }

    public void addTableAction(String title, TableAction tableAction) {
        this.addTableAction(title, null, null, tableAction);
    }

    public void addTableAction(String title, SelectionEnablement enablement, String confirmation, TableAction tableAction) {
        if (enablement == null) {
            enablement = DEFAULT_SELECTION_ENABLEMENT;
        }
        TableActionInfo info = new TableActionInfo(title, enablement, tableAction);
        info.confirmMessage = confirmation;
        tableActions.add(info);
    }

    private void refreshTableInfo() {
        int count = this.listGrid.getSelection().length;
        for (TableActionInfo tableAction : tableActions) {
            boolean enabled;
            switch (tableAction.enablement) {
                case ALWAYS:
                    enabled = true;
                    break;
                case ANY:
                    enabled = (count >= 1);
                    break;
                case SINGLE:
                    enabled = (count == 1);
                    break;
                case MULTIPLE:
                    enabled = (count > 1);
                    break;
                default:
                    throw new IllegalStateException("Unhandled SelectionEnablement: " + tableAction.enablement.name());
            }
            tableAction.actionButton.setDisabled(!enabled);
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
