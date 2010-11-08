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
import org.rhq.enterprise.gui.coregui.client.components.table.AbstractTableAction;
import org.rhq.enterprise.gui.coregui.client.components.table.TableAction;
import org.rhq.enterprise.gui.coregui.client.components.table.TableSection;

/**
 * A table that lists all roles and provides the ability to view details of or delete those roles and to create new
 * roles. 
 *
 * @author Greg Hinkle
 */
public class RolesView extends TableSection implements BookmarkableView {
    public static final String VIEW_ID = "Roles";
    
    private static final int ID_SUPERUSER = 1;
    private static final int ID_ALL_RESOURCES = 2;

    private static final String HEADER_ICON = "global/Role_24.png";

    public RolesView(String locatorId) {
        super(locatorId, "Roles");

        final RolesDataSource datasource = RolesDataSource.getInstance();
        setDataSource(datasource);
        setHeaderIcon(HEADER_ICON);
    }

    @Override
    protected void configureTable() {
        super.configureTable();

        addTableAction(extendLocatorId("Delete"), "Delete",
            "Are you sure you want to delete # roles?", new TableAction() {
                public boolean isEnabled(ListGridRecord[] selection) {
                    int count = selection.length;
                    if (count == 0) {
                        return false;
                    }

                    for (ListGridRecord record : selection) {
                        int id = record.getAttributeAsInt("id");
                        if (id == ID_SUPERUSER || id == ID_ALL_RESOURCES) {
                            // The superuser and all-resources roles cannot be deleted.
                            return false;
                        }
                    }
                    return true;
                }

                public void executeAction(ListGridRecord[] selection) {
                    getListGrid().removeSelectedData();
                }
            });

        addTableAction(extendLocatorId("New"), "New", new AbstractTableAction() {
            public void executeAction(ListGridRecord[] selection) {
                newDetails();
            }
        });
    }

    @Override
    public Canvas getDetailsView(int id) {
        RoleEditView editor = new RoleEditView(extendLocatorId("Detail"));

        return editor;
    }
}