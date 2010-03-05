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

import com.smartgwt.client.data.DataSource;
import com.smartgwt.client.data.SortSpecifier;
import com.smartgwt.client.types.Autofit;
import com.smartgwt.client.types.SelectionAppearance;
import com.smartgwt.client.types.SelectionStyle;
import com.smartgwt.client.types.SortDirection;
import com.smartgwt.client.types.VisibilityMode;
import com.smartgwt.client.util.BooleanCallback;
import com.smartgwt.client.util.SC;
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
import com.smartgwt.client.widgets.layout.SectionStack;
import com.smartgwt.client.widgets.layout.SectionStackSection;
import com.smartgwt.client.widgets.layout.VLayout;
import com.smartgwt.client.widgets.toolbar.ToolStrip;
import org.rhq.core.domain.criteria.AlertCriteria;
import org.rhq.enterprise.gui.coregui.client.admin.roles.RoleEditView;

/**
 * A view that displays a paginated table of fired {@link org.rhq.core.domain.alert.Alert alert}s, along with the
 * ability to filter or sort those alerts, click on an alert to view details about that alert's definition, or delete
 * selected alerts.
 *
 * @author Ian Springer
 */
public abstract class AbstractAlertsView extends SectionStack {
    private static final SortSpecifier[] INITIAL_SORT_SPECIFIERS = new SortSpecifier[] {
        new SortSpecifier(AlertCriteria.SORT_FIELD_CTIME, SortDirection.DESCENDING),
        new SortSpecifier(AlertCriteria.SORT_FIELD_NAME, SortDirection.ASCENDING)
    };

    private ListGrid listGrid;
    private IButton removeButton;
    private Label tableInfo;
    private AbstractAlertDataSource dataSource;
        
    public AbstractAlertsView() {
    }

    @Override
    protected void onInit() {
        super.onInit();

        setVisibilityMode(VisibilityMode.MULTIPLE);
        setWidth100();
        setHeight100();

        VLayout gridHolder = new VLayout();
        gridHolder.setWidth100();
        gridHolder.setHeight100();

        this.listGrid = new ListGrid();

        // Appearance settings.
        this.listGrid.setWidth100();
        this.listGrid.setHeight100();
        this.listGrid.setAutoFitData(Autofit.HORIZONTAL);
        this.listGrid.setAlternateRecordStyles(true);
        this.listGrid.setSelectionType(SelectionStyle.SIMPLE);
        this.listGrid.setSelectionAppearance(SelectionAppearance.CHECKBOX);
        //this.listGrid.setShowFilterEditor(true);

        // Data settings.
        this.dataSource = (AbstractAlertDataSource) createDataSource();
        this.listGrid.setDataSource(createDataSource());
        this.listGrid.setUseAllDataSourceFields(true);
        this.listGrid.setAutoFetchData(true);
        this.listGrid.setInitialSort(INITIAL_SORT_SPECIFIERS);

        gridHolder.addMember(this.listGrid);

        ToolStrip toolStrip = new ToolStrip();
        toolStrip.setMembersMargin(15);

        this.removeButton = new IButton("Delete");
        this.removeButton.setDisabled(true);
        this.removeButton.addClickHandler(new ClickHandler() {
            public void onClick(ClickEvent clickEvent) {
                SC.confirm("Are you sure you want to delete the " + AbstractAlertsView.this.listGrid.getSelection().length
                        + " selected alerts?",
                        new BooleanCallback() {
                            public void execute(Boolean confirmed) {
                                if (confirmed) {
                                    //listGrid.removeSelectedData();
                                    dataSource.deleteAlerts(AbstractAlertsView.this);
                                }
                            }
                        }
                );
            }
        });
        toolStrip.addMember(this.removeButton);
        toolStrip.addMember(new LayoutSpacer());

        this.tableInfo = new Label();
        this.tableInfo.setWrap(false);
        toolStrip.addMember(this.tableInfo);

        gridHolder.addMember(toolStrip);

        this.listGrid.addDataArrivedHandler(new DataArrivedHandler() {
            public void onDataArrived(DataArrivedEvent event) {
                updateFooter();
            }
        });
        this.listGrid.addSelectionChangedHandler(new SelectionChangedHandler() {
            public void onSelectionChanged(SelectionEvent event) {
                updateFooter();
            }
        });        

        SectionStackSection topSection = new SectionStackSection("Alerts");
        topSection.setExpanded(true);
        topSection.setItems(gridHolder);

        addSection(topSection);

        final RoleEditView roleEditor = new RoleEditView();

        final SectionStackSection detailSection = new SectionStackSection("Selected Alert");
        detailSection.setItems(roleEditor);
        addSection(detailSection);

        this.listGrid.addSelectionChangedHandler(new SelectionChangedHandler() {
            public void onSelectionChanged(SelectionEvent selectionEvent) {
                if (selectionEvent.getState()) {
                    expandSection(1);
                    roleEditor.editRecord(selectionEvent.getRecord());
                } else
                    collapseSection(1);
                    roleEditor.editNone();
            }
        });
    }

    protected abstract DataSource createDataSource();

    ListGrid getListGrid() {
        return this.listGrid;
    }

    void reloadData() {
        this.tableInfo.setContents("");
        this.listGrid.invalidateCache();
        //this.listGrid.markForRedraw();        
    }

    private void updateFooter() {
        String label = "Total: " + this.listGrid.getTotalRows();
        if (this.listGrid.anySelected()) {
            label += " (" + this.listGrid.getSelection().length + " selected)";
        }
        this.tableInfo.setContents(label);
        this.removeButton.setDisabled(!listGrid.anySelected());
    }
}