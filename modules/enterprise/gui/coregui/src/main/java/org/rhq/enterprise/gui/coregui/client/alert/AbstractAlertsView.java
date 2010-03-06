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
public abstract class AbstractAlertsView extends VLayout {
    private static final String TABLE_TITLE = "Alerts";

    private static final SortSpecifier[] INITIAL_SORT_SPECIFIERS = new SortSpecifier[] {
        new SortSpecifier(AlertCriteria.SORT_FIELD_CTIME, SortDirection.DESCENDING),
        new SortSpecifier(AlertCriteria.SORT_FIELD_NAME, SortDirection.ASCENDING)
    };

    private static final String DELETE_CONFIRM_MESSAGE = "Are you sure you want to delete the selected alerts?";

    private ListGrid listGrid;
    private AbstractAlertDataSource dataSource;
    private HTMLFlow detailsContent;

    @Override
    protected void onInit() {
        super.onInit();

        setWidth100();
        setHeight100();
        setMembersMargin(20);

        // Add the list table as the top half of the view.
        Table table = new Table(TABLE_TITLE, INITIAL_SORT_SPECIFIERS);
        table.setHeight("50%");
        this.listGrid = table.getListGrid();
        this.dataSource = createDataSource();
        table.setDataSource(dataSource);

        table.addTableAction("Delete", Table.SelectionEnablement.ANY, DELETE_CONFIRM_MESSAGE, new TableAction() {
            public void executeAction(ListGridRecord[] selection) {
                AbstractAlertsView.this.dataSource.deleteAlerts(AbstractAlertsView.this);

            }
        });

        this.listGrid.addSelectionChangedHandler(new SelectionChangedHandler() {
            public void onSelectionChanged(SelectionEvent event) {
                ListGridRecord[] selectedRecords = listGrid.getSelection();
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
                detailsContent.setContents(contents);
            }
        });

        addMember(table);

        // Add the details panel as the bottom half of the view.
        HLayout detailsPane = new HLayout();
        detailsPane.setWidth100();
        detailsPane.setHeight("50%");        
        this.detailsContent = new HTMLFlow("Select a single alert above to display its details here.");
        this.detailsContent.setWidth100();
        detailsPane.addMember(this.detailsContent);
        addMember(detailsPane);
    }

    protected abstract AbstractAlertDataSource createDataSource();

    ListGrid getListGrid() {
        return this.listGrid;
    }
}