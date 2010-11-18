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
import com.smartgwt.client.widgets.grid.ListGridField;
import com.smartgwt.client.widgets.grid.ListGridRecord;

import org.rhq.enterprise.gui.coregui.client.admin.AdministrationView;
import org.rhq.enterprise.gui.coregui.client.components.table.AbstractTableAction;
import org.rhq.enterprise.gui.coregui.client.components.table.TableAction;
import org.rhq.enterprise.gui.coregui.client.components.table.TableActionEnablement;
import org.rhq.enterprise.gui.coregui.client.components.table.TableSection;
import org.rhq.enterprise.gui.coregui.client.components.view.ViewName;

/**
 * A table that lists all users and provides the ability to view details of or delete those users and to create new
 * users.
 *
 * @author Greg Hinkle
 * @author Ian Springer
 */
public class UsersView extends TableSection {

    public static final ViewName VIEW_ID = new ViewName("Users", MSG.view_adminSecurity_users());
    public static final String VIEW_PATH = AdministrationView.VIEW_ID + "/"
        + AdministrationView.SECTION_SECURITY_VIEW_ID + "/" + VIEW_ID;

    private static final String HEADER_ICON = "global/User_24.png";

    public UsersView(String locatorId) {
        super(locatorId, MSG.view_adminSecurity_users());

        final UsersDataSource dataSource = UsersDataSource.getInstance();

        setDataSource(dataSource);
        setHeaderIcon(HEADER_ICON);
    }

    @Override
    protected void configureTable() {
        getListGrid().setUseAllDataSourceFields(false);

        ListGridField nameField = new ListGridField(UsersDataSource.Field.NAME, 120);

        ListGridField activeField = new ListGridField(UsersDataSource.Field.FACTIVE, 90);

        ListGridField ldapField = new ListGridField(UsersDataSource.Field.LDAP, 90);

        ListGridField firstNameField = new ListGridField(UsersDataSource.Field.FIRST_NAME, 150);

        ListGridField lastNameField = new ListGridField(UsersDataSource.Field.LAST_NAME, 150);

        ListGridField departmentField = new ListGridField(UsersDataSource.Field.DEPARTMENT, 150);

        setListGridFields(nameField, activeField, ldapField, firstNameField, lastNameField, departmentField);

        // TODO: fix msg
        addTableAction(extendLocatorId("Delete"), MSG.common_button_delete(), getDeleteConfirmMessage(),
            new TableAction() {
                public boolean isEnabled(ListGridRecord[] selection) {
                    int count = selection.length;
                    if (count == 0) {
                        return false;
                    }

                    for (ListGridRecord record : selection) {
                        int id = record.getAttributeAsInt(UsersDataSource.Field.ID);
                        if (id == UsersDataSource.ID_OVERLORD || id == UsersDataSource.ID_RHQADMIN) {
                            // The superuser and rhqadmin users cannot be deleted.
                            return false;
                        }
                    }
                    return true;
                }

                public void executeAction(ListGridRecord[] selection, Object actionValue) {
                    deleteSelectedRecords();
                }
            });

        addTableAction(extendLocatorId("New"), MSG.common_button_new(), new AbstractTableAction(
            TableActionEnablement.ALWAYS) {
            public void executeAction(ListGridRecord[] selection, Object actionValue) {
                newDetails();
            }
        });
    }

    public Canvas getDetailsView(int subjectId) {
        return new UserEditView(extendLocatorId("Detail"), subjectId);
    }

    @Override
    protected String getDataTypeName() {
        return MSG.common_label_user();
    }

    @Override
    protected String getDataTypeNamePlural() {
        return MSG.common_label_users();
    }

}
