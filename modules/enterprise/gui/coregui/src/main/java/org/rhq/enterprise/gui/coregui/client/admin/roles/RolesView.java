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

import java.util.Set;

import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.grid.ListGridField;
import com.smartgwt.client.widgets.grid.ListGridRecord;

import org.rhq.core.domain.authz.Permission;
import org.rhq.enterprise.gui.coregui.client.BookmarkableView;
import org.rhq.enterprise.gui.coregui.client.PermissionsLoadedListener;
import org.rhq.enterprise.gui.coregui.client.PermissionsLoader;
import org.rhq.enterprise.gui.coregui.client.admin.AdministrationView;
import org.rhq.enterprise.gui.coregui.client.components.table.TableAction;
import org.rhq.enterprise.gui.coregui.client.components.table.TableSection;
import org.rhq.enterprise.gui.coregui.client.components.view.ViewName;

/**
 * A table that lists all roles and provides the ability to view details of or delete those roles and to create new
 * roles. 
 *
 * @author Greg Hinkle
 * @author Ian Springer
 */
public class RolesView extends TableSection<RolesDataSource> implements BookmarkableView {

    public static final ViewName VIEW_ID = new ViewName("Roles", MSG.view_adminSecurity_roles());
    public static final String VIEW_PATH = AdministrationView.VIEW_ID + "/"
        + AdministrationView.SECTION_SECURITY_VIEW_ID + "/" + VIEW_ID;

    private static final String HEADER_ICON = "global/Role_24.png";

    private boolean hasManageSecurity;

    public RolesView(String locatorId) {
        super(locatorId, MSG.view_adminSecurity_roles());

        final RolesDataSource datasource = RolesDataSource.getInstance();
        setDataSource(datasource);
        setHeaderIcon(HEADER_ICON);
    }

    @Override
    protected void configureTable() {
        ListGridField nameField = new ListGridField(RolesDataSource.Field.NAME, 150);

        ListGridField descriptionField = new ListGridField(RolesDataSource.Field.DESCRIPTION);

        setListGridFields(nameField, descriptionField);

        addTableAction(extendLocatorId("Delete"), MSG.common_button_delete(), getDeleteConfirmMessage(),
            new TableAction() {
                public boolean isEnabled(ListGridRecord[] selection) {
                    int count = selection.length;
                    if (count == 0) {
                        return false;
                    }

                    for (ListGridRecord record : selection) {
                        int roleId = record.getAttributeAsInt(RolesDataSource.Field.ID);
                        if (RolesDataSource.isSystemRoleId(roleId)) {
                            // The superuser role cannot be deleted.
                            return false;
                        }
                    }
                    return true;
                }

                public void executeAction(ListGridRecord[] selection, Object actionValue) {
                    deleteSelectedRecords();
                }
            });

        addTableAction(extendLocatorId("New"), MSG.common_button_new(), new TableAction() {
            public boolean isEnabled(ListGridRecord[] selection) {
                return hasManageSecurity;
            }

            public void executeAction(ListGridRecord[] selection, Object actionValue) {
                newDetails();
            }
        });

        fetchManageSecurityPermissionAsync();

        super.configureTable();
    }

    private void fetchManageSecurityPermissionAsync() {
        new PermissionsLoader().loadExplicitGlobalPermissions(new PermissionsLoadedListener() {
            @Override
            public void onPermissionsLoaded(Set<Permission> permissions) {
                if (permissions != null) {
                    hasManageSecurity = permissions.contains(Permission.MANAGE_SECURITY);
                    refresh();
                } else {
                    hasManageSecurity = false;
                }
            }
        });
    }

    @Override
    public Canvas getDetailsView(int roleId) {
        return new RoleEditView(extendLocatorId("Detail"), roleId);
    }

    @Override
    protected String getDataTypeName() {
        return MSG.common_label_role();
    }

    @Override
    protected String getDataTypeNamePlural() {
        return MSG.common_label_roles();
    }

}