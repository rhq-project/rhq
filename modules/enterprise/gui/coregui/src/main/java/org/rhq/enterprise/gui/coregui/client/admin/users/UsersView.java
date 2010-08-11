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

import org.rhq.enterprise.gui.coregui.client.components.table.Table;
import org.rhq.enterprise.gui.coregui.client.components.table.TableAction;

import com.smartgwt.client.types.ListGridFieldType;
import com.smartgwt.client.widgets.Label;
import com.smartgwt.client.widgets.Window;
import com.smartgwt.client.widgets.grid.ListGridField;
import com.smartgwt.client.widgets.grid.ListGridRecord;
import com.smartgwt.client.widgets.grid.events.SelectionChangedHandler;
import com.smartgwt.client.widgets.grid.events.SelectionEvent;
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

        final Table table = new Table("Users");
        table.setHeight("50%");
        table.setShowResizeBar(true);
        table.setDataSource(datasource);


        ListGridField idField = new ListGridField("id", "Id", 55);
        idField.setType(ListGridFieldType.INTEGER);

        ListGridField nameField = new ListGridField("name", "User Name", 100);

        ListGridField emailField = new ListGridField("emailAddress", "Email Address");

        table.getListGrid().setFields(idField, nameField, emailField);

        ToolStrip toolStrip = new ToolStrip();
        toolStrip.setWidth100();
        toolStrip.setMembersMargin(15);

        table.addTableAction("Remove",
                Table.SelectionEnablement.ANY,
                "Are you sure you want to delete # users?",
                new TableAction() {
                    public void executeAction(ListGridRecord[] selection) {
                        table.getListGrid().removeSelectedData();
                    }
                });

        table.addTableAction("Add User",
                new TableAction() {
                    public void executeAction(ListGridRecord[] selection) {
                        createUser();
                    }
                });


        final UserEditView userEditor = new UserEditView();

        final SectionStackSection detailsSection = new SectionStackSection("Details");
        detailsSection.setItems(new Label("Select a user to edit..."));
        detailsSection.setExpanded(false);

        table.getListGrid().addSelectionChangedHandler(new SelectionChangedHandler() {
            public void onSelectionChanged(SelectionEvent selectionEvent) {
                if (selectionEvent.getState()) {
                    userEditor.editRecord(selectionEvent.getRecord());
                } else {
                    userEditor.editNone();
                }
            }
        });


        addMember(table);
        addMember(userEditor);
    }


    public void createUser() {


        UserEditView.editNew();
    }
}
