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

import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.grid.ListGridRecord;

import org.rhq.enterprise.gui.coregui.client.components.table.Table;
import org.rhq.enterprise.gui.coregui.client.components.table.TableAction;

/**
 * @author Greg Hinkle
 */
public class UsersView extends Table {

    public UsersView(String locatorId) {
        super(locatorId, "Users View");

        final UsersDataSource datasource = UsersDataSource.getInstance();

        setDataSource(datasource);
    }

    @Override
    protected void configureTable() {

        getListGrid().getField("id").setWidth(55);
        getListGrid().getField("name").setWidth(100);

        addTableAction(extendLocatorId("Delete"), "Delete", Table.SelectionEnablement.ANY,
            "Are you sure you want to delete # users?", new TableAction() {
                public void executeAction(ListGridRecord[] selection) {
                    getListGrid().removeSelectedData();
                }
            });

        addTableAction(extendLocatorId("New"), "New", new TableAction() {
            public void executeAction(ListGridRecord[] selection) {
                createUser();
            }
        });
    }

    public void createUser() {
        UserEditView editView = new UserEditView(extendLocatorId("Edit"));
        editView.editNewInternal();
    }

    public Canvas getDetailsView(int id) {
        final UserEditView userEditor = new UserEditView(extendLocatorId("Detail"));

        return userEditor;
    }
}
