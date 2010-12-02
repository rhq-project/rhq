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

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.grid.CellFormatter;
import com.smartgwt.client.widgets.grid.ListGridField;
import com.smartgwt.client.widgets.grid.ListGridRecord;

import org.rhq.core.domain.authz.Permission;
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
 * A table that lists all users and provides the ability to view or edit details of users, delete users, or create new
 * users. For the logged in user to view or edit a user other than themselves, they must possess the
 * {@link Permission#MANAGE_SECURITY MANAGE_SECURITY} permission. Furthermore, a user without MANAGE_SECURITY cannot
 * update their assigned roles. 
 *
 * @author Greg Hinkle
 * @author Ian Springer
 */
public class UsersView extends TableSection<UsersDataSource> {

    public static final ViewName VIEW_ID = new ViewName("Users", MSG.view_adminSecurity_users());
    public static final String VIEW_PATH = AdministrationView.VIEW_ID + "/"
        + AdministrationView.SECTION_SECURITY_VIEW_ID + "/" + VIEW_ID;

    private static final String HEADER_ICON = "global/User_24.png";

    private boolean hasManageSecurity;

    public UsersView(String locatorId) {
        super(locatorId, MSG.view_adminSecurity_users());

        final UsersDataSource dataSource = UsersDataSource.getInstance();

        setDataSource(dataSource);
        setHeaderIcon(HEADER_ICON);
    }

    @Override
    protected void configureTable() {
        List<ListGridField> fields = createFields();
        setListGridFields(fields.toArray(new ListGridField[fields.size()]));

        addTableAction(extendLocatorId("Delete"), MSG.common_button_delete(), getDeleteConfirmMessage(),
            createDeleteAction());
        addTableAction(extendLocatorId("New"), MSG.common_button_new(), createNewAction());

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

    private List<ListGridField> createFields() {
        List<ListGridField> fields = new ArrayList<ListGridField>();

        ListGridField nameField = new ListGridField(UsersDataSource.Field.NAME, 150);
        nameField.setCellFormatter(new CellFormatter() {
            public String format(Object value, ListGridRecord record, int i, int i1) {
                Integer subjectId = getId(record);
                String userUrl = LinkManager.getUserLink(subjectId);
                return SeleniumUtility.getLocatableHref(userUrl, value.toString(), null);
            }
        });
        fields.add(nameField);

        ListGridField activeField = new ListGridField(UsersDataSource.Field.FACTIVE, 90);
        fields.add(activeField);

        ListGridField ldapField = new ListGridField(UsersDataSource.Field.LDAP, 90);
        fields.add(ldapField);

        ListGridField firstNameField = new ListGridField(UsersDataSource.Field.FIRST_NAME, 150);
        fields.add(firstNameField);

        ListGridField lastNameField = new ListGridField(UsersDataSource.Field.LAST_NAME, 150);
        fields.add(lastNameField);

        ListGridField departmentField = new ListGridField(UsersDataSource.Field.DEPARTMENT, 150);
        fields.add(departmentField);

        // TODO: instead of fetching roles, use a composite object that will pull the role count across the wire.
        //       this count will not required permission checks at all. 

        /*
            ListGridField rolesField = new ListGridField(UsersDataSource.Field.ROLES, 250);
            rolesField.setCellFormatter(new CellFormatter() {
                public String format(Object value, ListGridRecord record, int rowNum, int colNum) {
                    Record[] roleRecords = record.getAttributeAsRecordArray(UsersDataSource.Field.ROLES);
                    StringBuilder formattedValue = new StringBuilder();
                    for (int i = 0; i < roleRecords.length; i++) {
                        Record roleRecord = roleRecords[i];
                        String roleName = roleRecord.getAttribute(RolesDataSource.Field.NAME);
                        formattedValue.append(roleName);
                        if (i != (roleRecords.length - 1)) {
                            formattedValue.append(", ");
                        }
                    }
                    return formattedValue.toString();
                }
            });
            fields.add(rolesField);
        
        */

        return fields;
    }

    private TableAction createDeleteAction() {
        return new TableAction() {
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
        };
    }

    private TableAction createNewAction() {
        return new TableAction() {
            public boolean isEnabled(ListGridRecord[] selection) {
                return hasManageSecurity;
            }

            public void executeAction(ListGridRecord[] selection, Object actionValue) {
                newDetails();
            }
        };
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
