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

import java.util.EnumSet;
import java.util.Set;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.grid.CellFormatter;
import com.smartgwt.client.widgets.grid.ListGridField;
import com.smartgwt.client.widgets.grid.ListGridRecord;

import org.rhq.core.domain.authz.Permission;
import org.rhq.enterprise.gui.coregui.client.BookmarkableView;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.LinkManager;
import org.rhq.enterprise.gui.coregui.client.admin.AdministrationView;
import org.rhq.enterprise.gui.coregui.client.components.table.TableAction;
import org.rhq.enterprise.gui.coregui.client.components.table.TableSection;
import org.rhq.enterprise.gui.coregui.client.components.view.ViewName;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.util.message.Message;
import org.rhq.enterprise.gui.coregui.client.util.selenium.SeleniumUtility;

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

    // TODO: We need a 24x24 version of the Role icon.
    private static final String HEADER_ICON = "global/Role_16.png";

    private boolean hasManageSecurity;

    public RolesView(String locatorId) {
        super(locatorId, MSG.view_adminSecurity_roles());

        final RolesDataSource datasource = RolesDataSource.getInstance();
        setDataSource(datasource);
        setHeaderIcon(HEADER_ICON);
    }
    
    @Override
    protected void configureTable() {
        super.configureTable();

        ListGridField nameField = new ListGridField(RolesDataSource.Field.NAME, 150);
        nameField.setCellFormatter(new CellFormatter() {
            public String format(Object value, ListGridRecord record, int i, int i1) {
                Integer roleId = getId(record);
                String roleUrl = LinkManager.getRoleLink(roleId);
                return SeleniumUtility.getLocatableHref(roleUrl, value.toString(), null);
            }
        });

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
                        int id = record.getAttributeAsInt(RolesDataSource.Field.ID);
                        if (id == RolesDataSource.ID_SUPERUSER || id == RolesDataSource.ID_ALL_RESOURCES) {
                            // The superuser and all-resources roles cannot be deleted.
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
    }

    private void fetchManageSecurityPermissionAsync() {
        GWTServiceLookup.getAuthorizationService().getExplicitGlobalPermissions(
            new AsyncCallback<Set<Permission>>() {

                @Override
                public void onSuccess(Set<Permission> result) {
                    hasManageSecurity = result.contains(Permission.MANAGE_SECURITY);
                    refresh();
                }

                @Override
                public void onFailure(Throwable caught) {
                    hasManageSecurity = false;
                    CoreGUI.getMessageCenter().notify(
                        new Message(MSG.util_userPerm_loadFailGlobal(), caught, Message.Severity.Error, EnumSet
                            .of(Message.Option.BackgroundJobResult)));
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