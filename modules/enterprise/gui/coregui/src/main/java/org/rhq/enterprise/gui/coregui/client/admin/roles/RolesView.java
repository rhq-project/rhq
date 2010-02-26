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
package org.rhq.enterprise.gui.coregui.client.admin.roles;

import org.rhq.enterprise.gui.coregui.client.Presenter;
import org.rhq.enterprise.gui.coregui.client.admin.users.UsersDataSource;
import org.rhq.enterprise.gui.coregui.client.places.Place;

import com.smartgwt.client.docs.CheckboxField;
import com.smartgwt.client.types.Autofit;
import com.smartgwt.client.types.ListGridFieldType;
import com.smartgwt.client.types.Overflow;
import com.smartgwt.client.types.SelectionAppearance;
import com.smartgwt.client.types.SelectionStyle;
import com.smartgwt.client.types.VisibilityMode;
import com.smartgwt.client.util.BooleanCallback;
import com.smartgwt.client.util.SC;
import com.smartgwt.client.widgets.IButton;
import com.smartgwt.client.widgets.Label;
import com.smartgwt.client.widgets.events.ClickEvent;
import com.smartgwt.client.widgets.events.ClickHandler;
import com.smartgwt.client.widgets.form.fields.CheckboxItem;
import com.smartgwt.client.widgets.grid.ListGrid;
import com.smartgwt.client.widgets.grid.ListGridField;
import com.smartgwt.client.widgets.grid.events.SelectionChangedHandler;
import com.smartgwt.client.widgets.grid.events.SelectionEvent;
import com.smartgwt.client.widgets.layout.LayoutSpacer;
import com.smartgwt.client.widgets.layout.SectionStack;
import com.smartgwt.client.widgets.layout.SectionStackSection;
import com.smartgwt.client.widgets.layout.VLayout;
import com.smartgwt.client.widgets.toolbar.ToolStrip;

import java.util.List;

/**
 * @author Greg Hinkle
 */
public class RolesView extends VLayout implements Presenter {


    @Override
    protected void onInit() {
        super.onInit();

        setWidth100();
        setHeight100();

        final RolesDataSource datasource = RolesDataSource.getInstance();

        VLayout gridHolder = new VLayout();
        gridHolder.setWidth100();
        gridHolder.setHeight("50%");
        gridHolder.setShowResizeBar(true);
        gridHolder.setResizeBarTarget("next");


        final ListGrid listGrid = new ListGrid();
        listGrid.setWidth100();
        listGrid.setHeight100();
        listGrid.setDataSource(datasource);
        listGrid.setAutoFetchData(true);
        listGrid.setAutoFitData(Autofit.HORIZONTAL);
        listGrid.setAlternateRecordStyles(true);
//        listGrid.setSelectionType(SelectionStyle.SIMPLE);
//        listGrid.setSelectionAppearance(SelectionAppearance.CHECKBOX);

        listGrid.setShowFilterEditor(true);
//        listGrid.setUseAllDataSourceFields(true);


        ListGridField idField = new ListGridField("id", "Id", 55);
        idField.setType(ListGridFieldType.INTEGER);


        ListGridField nameField = new ListGridField("username", "Name");

        listGrid.setFields(idField, nameField);


        gridHolder.addMember(listGrid);

        ToolStrip toolStrip = new ToolStrip();
        toolStrip.setMembersMargin(15);

        final IButton removeButton = new IButton("Remove");
        removeButton.setDisabled(true);
        removeButton.addClickHandler(new ClickHandler() {
            public void onClick(ClickEvent clickEvent) {
                SC.confirm("Are you sure you want to delete " + listGrid.getSelection().length + " resources?",
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


        addMember(gridHolder);

        final RoleEditView roleEditor = new RoleEditView();
        roleEditor.setOverflow(Overflow.AUTO);
        addMember(roleEditor);


        listGrid.addSelectionChangedHandler(new SelectionChangedHandler() {
            public void onSelectionChanged(SelectionEvent selectionEvent) {
                if (selectionEvent.getState()) {
                    roleEditor.editRecord(selectionEvent.getRecord());
                } else {
                    roleEditor.editNone();
                }
            }
        });


    }

    public boolean fireDisplay(Place place, List<Place> children) {
        if (!getPlace().equals(place)) {
            return false;
        }


        return true;
    }

    public Place getPlace() {
        return new Place("Roles", "Manage Roles");
    }
}