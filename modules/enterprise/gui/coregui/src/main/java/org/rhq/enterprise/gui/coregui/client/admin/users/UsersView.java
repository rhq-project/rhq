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
package org.rhq.enterprise.gui.coregui.client.admin.users;

import com.smartgwt.client.types.Autofit;
import com.smartgwt.client.types.ListGridFieldType;
import com.smartgwt.client.util.BooleanCallback;
import com.smartgwt.client.util.SC;
import com.smartgwt.client.widgets.IButton;
import com.smartgwt.client.widgets.Label;
import com.smartgwt.client.widgets.Window;
import com.smartgwt.client.widgets.events.ClickEvent;
import com.smartgwt.client.widgets.events.ClickHandler;
import com.smartgwt.client.widgets.grid.ListGrid;
import com.smartgwt.client.widgets.grid.ListGridField;
import com.smartgwt.client.widgets.grid.events.SelectionChangedHandler;
import com.smartgwt.client.widgets.grid.events.SelectionEvent;
import com.smartgwt.client.widgets.layout.LayoutSpacer;
import com.smartgwt.client.widgets.layout.SectionStackSection;
import com.smartgwt.client.widgets.layout.VLayout;
import com.smartgwt.client.widgets.toolbar.ToolStrip;

/**
 * @author Greg Hinkle
 */
public class UsersView extends VLayout {

    public UsersView() {
        super();
        setWidth100();
        setHeight100();
    }

    @Override
    protected void onInit() {
        super.onInit();


        final UsersDataSource datasource = UsersDataSource.getInstance();

        VLayout gridHolder = new VLayout();
        gridHolder.setWidth100();
        gridHolder.setHeight("50%");
        gridHolder.setShowResizeBar(true);

        final ListGrid listGrid = new ListGrid();
        listGrid.setWidth100();
        listGrid.setHeight100();
        listGrid.setDataSource(datasource);
        listGrid.setAutoFetchData(true);
        listGrid.setAutoFitData(Autofit.HORIZONTAL);
//        listGrid.setAutoFitData(Autofit.HORIZONTAL);
        listGrid.setAlternateRecordStyles(true);
//        listGrid.setAutoFitData(Autofit.HORIZONTAL);
//        listGrid.setSelectionType(SelectionStyle.SIMPLE);
//        listGrid.setSelectionAppearance(SelectionAppearance.CHECKBOX);

        listGrid.setShowFilterEditor(true);

        ListGridField idField = new ListGridField("id", "Id", 55);
        idField.setType(ListGridFieldType.INTEGER);

        ListGridField nameField = new ListGridField("username", "Name", 100);

        ListGridField emailField = new ListGridField("email", "Email Address");

        listGrid.setFields(idField, nameField, emailField);


        gridHolder.addMember(listGrid);

        ToolStrip toolStrip = new ToolStrip();
        toolStrip.setMembersMargin(15);

        final IButton removeButton = new IButton("Remove");
        removeButton.setDisabled(true);
        removeButton.addClickHandler(new ClickHandler() {
            public void onClick(ClickEvent clickEvent) {
                SC.confirm("Are you sure you want to delete " + listGrid.getSelection().length + " users?",
                        new BooleanCallback() {
                            public void execute(Boolean aBoolean) {
                                // todo
                            }
                        }
                );
            }
        });


        final Label tableInfo = new Label("Total: " + listGrid.getTotalRows());
        tableInfo.setWrap(false);


        final IButton addButton = new IButton("Add Role");
        addButton.addClickHandler(new ClickHandler() {
            public void onClick(ClickEvent clickEvent) {
                createUser();
            }
        });


        toolStrip.addMember(removeButton);
        toolStrip.addMember(addButton);
        toolStrip.addMember(new LayoutSpacer());
        toolStrip.addMember(tableInfo);

        gridHolder.addMember(toolStrip);


        final UserEditView userEditor = new UserEditView();


        listGrid.addSelectionChangedHandler(new SelectionChangedHandler() {
            public void onSelectionChanged(SelectionEvent selectionEvent) {
                int selectedCount = ((ListGrid) selectionEvent.getSource()).getSelection().length;
                tableInfo.setContents("Total: " + listGrid.getTotalRows() + " (" + selectedCount + " selected)");
                removeButton.setDisabled(selectedCount == 0);
            }
        });

        final SectionStackSection detailsSection = new SectionStackSection("Details");
        detailsSection.setItems(new Label("Select a user to edit..."));
        detailsSection.setExpanded(false);

        listGrid.addSelectionChangedHandler(new SelectionChangedHandler() {
            public void onSelectionChanged(SelectionEvent selectionEvent) {
                if (selectionEvent.getState()) {
                    userEditor.editRecord(selectionEvent.getRecord());
                } else {
                    userEditor.editNone();
                }
            }
        });


        addMember(gridHolder);
        addMember(userEditor);
    }


    public void createUser() {

        UserEditView editView = new UserEditView();

        final Window roleEditor = new Window();
        roleEditor.setTitle("Create User");
        roleEditor.setWidth(800);
        roleEditor.setHeight(800);
        roleEditor.setIsModal(true);
        roleEditor.setShowModalMask(true);
        roleEditor.setCanDragResize(true);
        roleEditor.centerInPage();
        roleEditor.addItem(editView);
        roleEditor.show();

        editView.editNew();
    }
}
