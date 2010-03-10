/*
 * RHQ Management Platform
 * Copyright (C) 2010 Red Hat, Inc.
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
package org.rhq.enterprise.gui.coregui.client.alert;

import com.smartgwt.client.data.Criteria;
import com.smartgwt.client.data.SortSpecifier;
import com.smartgwt.client.types.SortDirection;
import com.smartgwt.client.widgets.HTMLFlow;
import com.smartgwt.client.widgets.grid.ListGrid;
import com.smartgwt.client.widgets.grid.ListGridRecord;
import com.smartgwt.client.widgets.grid.events.SelectionChangedHandler;
import com.smartgwt.client.widgets.grid.events.SelectionEvent;
import com.smartgwt.client.widgets.layout.HLayout;
import com.smartgwt.client.widgets.layout.VLayout;
import org.rhq.core.domain.criteria.AlertCriteria;
import org.rhq.enterprise.gui.coregui.client.components.table.Table;
import org.rhq.enterprise.gui.coregui.client.components.table.TableAction;

/**
 * A view that displays a paginated table of fired {@link org.rhq.core.domain.alert.Alert alert}s, along with the
 * ability to filter or sort those alerts, click on an alert to view details about that alert's definition, or delete
 * selected alerts.
 *
 * @author Ian Springer
 */
public class AlertsView extends VLayout {
    private static final String TITLE = "Alerts";

    private static final SortSpecifier[] SORT_SPECIFIERS = new SortSpecifier[] {
        new SortSpecifier(AlertCriteria.SORT_FIELD_CTIME, SortDirection.DESCENDING),
        new SortSpecifier(AlertCriteria.SORT_FIELD_NAME, SortDirection.ASCENDING)
    };

    private static final String DELETE_CONFIRM_MESSAGE = "Are you sure you want to delete the selected alert(s)?";

    private Table table;
    private AlertDataSource dataSource;
    private HTMLFlow detailsContent;

    public AlertsView(Criteria criteria, String[] excludedFieldNames) {
        this.table = new Table(TITLE, criteria, SORT_SPECIFIERS, excludedFieldNames);
    }


    @Override
    protected void onInit() {
        super.onInit();
        setWidth100();
        setHeight100();
        setMembersMargin(20);

    }

    @Override
    protected void onDraw() {
        super.onDraw();


        // Add the list table as the top half of the view.
        //Criteria criteria = new Criteria(AlertCriteria.);
        this.table.setHeight("50%");
        ListGrid listGrid = this.table.getListGrid();
        this.dataSource = new AlertDataSource();
        this.table.setDataSource(this.dataSource);

        this.table.addTableAction("Delete", Table.SelectionEnablement.ANY, DELETE_CONFIRM_MESSAGE, new TableAction() {
            public void executeAction(ListGridRecord[] selection) {
                AlertsView.this.dataSource.deleteAlerts(AlertsView.this);
            }
        });
        this.table.addTableAction("Acknowledge", Table.SelectionEnablement.ANY, null, new TableAction() {
            public void executeAction(ListGridRecord[] selection) {
                AlertsView.this.dataSource.acknowledgeAlerts(AlertsView.this);
            }
        });

        listGrid.addSelectionChangedHandler(new SelectionChangedHandler() {
            public void onSelectionChanged(SelectionEvent event) {
                ListGridRecord[] selectedRecords =  AlertsView.this.table.getListGrid().getSelection();
                String contents;
                if (selectedRecords.length == 1) {
                    ListGridRecord record = selectedRecords[0];
                    String name = record.getAttribute("name");
                    String id = record.getAttribute("id");
                    // TODO: Finish this.
                    contents = "Details for alert '" + name + "' with id " + id + "...<h2>TODO</h2>";

                } else {
                    contents = "Select a single alert above to display its details here.";
                }
                AlertsView.this.detailsContent.setContents(contents);
            }
        });

        addMember(this.table);

        // Add the details panel as the bottom half of the view.
        HLayout detailsPane = new HLayout();
        detailsPane.setWidth100();
        detailsPane.setHeight("50%");
        this.detailsContent = new HTMLFlow("Select a single alert above to display its details here.");
        this.detailsContent.setWidth100();
        detailsPane.addMember(this.detailsContent);
        addMember(detailsPane);
    }

    protected Criteria getCriteria() {
        return null;
    }

    ListGrid getListGrid() {
        return this.table.getListGrid();
    }

    public void refresh() {
        this.table.getListGrid().invalidateCache();
        //this.table.getListGrid().markForRedraw();
    }

    public void refresh(Criteria criteria) {
        this.table.refresh(criteria);
        //this.table.getListGrid().markForRedraw();
    }

}