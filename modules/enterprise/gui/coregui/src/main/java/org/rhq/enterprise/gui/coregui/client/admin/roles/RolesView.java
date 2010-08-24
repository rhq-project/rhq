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

import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.grid.ListGridRecord;

import org.rhq.enterprise.gui.coregui.client.BookmarkableView;
import org.rhq.enterprise.gui.coregui.client.ViewPath;
import org.rhq.enterprise.gui.coregui.client.components.table.Table;
import org.rhq.enterprise.gui.coregui.client.components.table.TableAction;
import org.rhq.enterprise.gui.coregui.client.components.table.TableSection;

/**
 * @author Greg Hinkle
 */
public class RolesView extends TableSection implements BookmarkableView {


    public RolesView() {
        super("Roles");
        final RolesDataSource datasource = RolesDataSource.getInstance();
        setDataSource(datasource);
    }

    @Override
    protected void configureTable() {
        super.configureTable();


        addTableAction("Remove",
                Table.SelectionEnablement.ANY,
                "Are you sure you want to delete # roles?",
                new TableAction() {
                    public void executeAction(ListGridRecord[] selection) {
                        getListGrid().removeSelectedData();
                    }
                });

        addTableAction("Add Role",
                new TableAction() {
                    public void executeAction(ListGridRecord[] selection) {
                        createRole();
                    }
                });

    }


    public void createRole() {

        RoleEditView editView = new RoleEditView();

        editView.editNew();
    }

    @Override
    public Canvas getDetailsView(int id) {
        RoleEditView editor = new RoleEditView();

        return editor;
    }

    public void renderView(ViewPath viewPath) {

        System.out.println("Display role list");

    }
}