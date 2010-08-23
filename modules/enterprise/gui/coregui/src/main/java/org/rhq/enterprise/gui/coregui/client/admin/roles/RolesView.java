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

import com.smartgwt.client.types.ListGridFieldType;
import com.smartgwt.client.types.Overflow;
import com.smartgwt.client.widgets.grid.ListGridField;
import com.smartgwt.client.widgets.grid.ListGridRecord;
import com.smartgwt.client.widgets.grid.events.SelectionChangedHandler;
import com.smartgwt.client.widgets.grid.events.SelectionEvent;

import org.rhq.enterprise.gui.coregui.client.BookmarkableView;
import org.rhq.enterprise.gui.coregui.client.ViewPath;
import org.rhq.enterprise.gui.coregui.client.components.table.Table;
import org.rhq.enterprise.gui.coregui.client.components.table.TableAction;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableVLayout;

/**
 * @author Greg Hinkle
 */
public class RolesView extends LocatableVLayout implements BookmarkableView {

    public RolesView(String locatorId) {
        super(locatorId);
    }

    @Override
    protected void onInit() {
        super.onInit();

        setWidth100();
        setHeight100();

        final RolesDataSource datasource = RolesDataSource.getInstance();

        final Table table = new Table(getLocatorId(), "Roles");
        table.setHeight("50%");
        table.setShowResizeBar(true);
        table.setResizeBarTarget("next");
        table.setDataSource(datasource);

        ListGridField idField = new ListGridField("id", "Id", 55);
        idField.setType(ListGridFieldType.INTEGER);

        ListGridField nameField = new ListGridField("name", "Name");

        table.getListGrid().setFields(idField, nameField);

        table.addTableAction("RemoveRole", "Remove", Table.SelectionEnablement.ANY,
            "Are you sure you want to delete # roles?", new TableAction() {
                public void executeAction(ListGridRecord[] selection) {
                    table.getListGrid().removeSelectedData();
                }
            });

        table.addTableAction("AddRole", "Add Role", new TableAction() {
            public void executeAction(ListGridRecord[] selection) {
                createRole();
            }
        });

        addMember(table);

        final RoleEditView roleEditor = new RoleEditView();
        roleEditor.setOverflow(Overflow.AUTO);
        addMember(roleEditor);

        table.getListGrid().addSelectionChangedHandler(new SelectionChangedHandler() {
            public void onSelectionChanged(SelectionEvent selectionEvent) {
                if (selectionEvent.getState()) {
                    roleEditor.editRecord(selectionEvent.getRecord());
                } else {
                    roleEditor.editNone();
                }
            }
        });
    }

    public void createRole() {

        RoleEditView editView = new RoleEditView();

        editView.editNew();
    }

    public void renderView(ViewPath viewPath) {

        System.out.println("Display role list");

    }
}