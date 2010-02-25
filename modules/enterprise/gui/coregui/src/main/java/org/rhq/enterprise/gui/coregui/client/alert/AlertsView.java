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

import com.smartgwt.client.types.Autofit;
import com.smartgwt.client.types.SelectionAppearance;
import com.smartgwt.client.types.SelectionStyle;
import com.smartgwt.client.types.VisibilityMode;
import com.smartgwt.client.util.BooleanCallback;
import com.smartgwt.client.util.SC;
import com.smartgwt.client.widgets.IButton;
import com.smartgwt.client.widgets.Label;
import com.smartgwt.client.widgets.events.ClickEvent;
import com.smartgwt.client.widgets.events.ClickHandler;
import com.smartgwt.client.widgets.grid.ListGrid;
import com.smartgwt.client.widgets.grid.events.SelectionChangedHandler;
import com.smartgwt.client.widgets.grid.events.SelectionEvent;
import com.smartgwt.client.widgets.layout.LayoutSpacer;
import com.smartgwt.client.widgets.layout.SectionStack;
import com.smartgwt.client.widgets.layout.SectionStackSection;
import com.smartgwt.client.widgets.layout.VLayout;
import com.smartgwt.client.widgets.toolbar.ToolStrip;
import org.rhq.enterprise.gui.coregui.client.admin.roles.RoleEditView;

/**
 * A view that displays a paginated table of fired {@link org.rhq.core.domain.alert.Alert alert}s, along with the
 * ability to filter or sort those alerts, click on an alert to view details about that alert, or delete selected
 * alerts.
 *
 * @author Ian Springer
 */
public class AlertsView extends SectionStack {

    @Override
    protected void onInit() {
        super.onInit();

        setVisibilityMode(VisibilityMode.MULTIPLE);
        setWidth100();
        setHeight100();

        final AlertDataSource dataSource = AlertDataSource.getInstance();

        VLayout gridHolder = new VLayout();
        gridHolder.setWidth100();
        gridHolder.setHeight100();

        final ListGrid listGrid = new ListGrid();
        listGrid.setWidth100();
        listGrid.setHeight100();
        listGrid.setDataSource(dataSource);
        listGrid.setAutoFetchData(true);
        listGrid.setAutoFitData(Autofit.HORIZONTAL);
        listGrid.setAlternateRecordStyles(true);
        listGrid.setSelectionType(SelectionStyle.SIMPLE);
        listGrid.setSelectionAppearance(SelectionAppearance.CHECKBOX);

        listGrid.setShowFilterEditor(true);

        listGrid.setUseAllDataSourceFields(true);

        gridHolder.addMember(listGrid);

        ToolStrip toolStrip = new ToolStrip();
        toolStrip.setMembersMargin(15);

        final IButton removeButton = new IButton("Delete");
        removeButton.setDisabled(true);
        removeButton.addClickHandler(new ClickHandler() {
            public void onClick(ClickEvent clickEvent) {
                SC.confirm("Are you sure you want to delete " + listGrid.getSelection().length + " alerts?",
                        new BooleanCallback() {
                            public void execute(Boolean aBoolean) {

                            }
                        }
                );
            }
        });

        final Label tableInfo = new Label("Total: " + listGrid.getTotalRows());
        tableInfo.setWrap(false);

        toolStrip.addMember(removeButton);
        toolStrip.addMember(new LayoutSpacer());
        toolStrip.addMember(tableInfo);

        gridHolder.addMember(toolStrip);

        listGrid.addSelectionChangedHandler(new SelectionChangedHandler() {
            public void onSelectionChanged(SelectionEvent selectionEvent) {
                int selectedCount = ((ListGrid) selectionEvent.getSource()).getSelection().length;
                tableInfo.setContents("Total: " + listGrid.getTotalRows() + " (" + selectedCount + " selected)");
                removeButton.setDisabled(selectedCount == 0);
            }
        });

        SectionStackSection topSection = new SectionStackSection("Alerts");
        topSection.setExpanded(true);
        topSection.setItems(gridHolder);

        addSection(topSection);

        final RoleEditView roleEditor = new RoleEditView();

        final SectionStackSection detailSection = new SectionStackSection("Selected Role");
        detailSection.setItems(roleEditor);
        addSection(detailSection);

        listGrid.addSelectionChangedHandler(new SelectionChangedHandler() {
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
}