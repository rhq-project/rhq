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
package org.rhq.coregui.client.admin.users;

import java.util.ArrayList;
import java.util.List;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.types.SelectionStyle;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.grid.ListGridField;
import com.smartgwt.client.widgets.grid.ListGridRecord;
import com.smartgwt.client.widgets.grid.events.CellClickEvent;
import com.smartgwt.client.widgets.grid.events.CellClickHandler;

import org.rhq.core.domain.authz.Permission;
import org.rhq.coregui.client.CoreGUI;
import org.rhq.coregui.client.IconEnum;
import org.rhq.coregui.client.admin.AdministrationView;
import org.rhq.coregui.client.components.table.EscapedHtmlCellFormatter;
import org.rhq.coregui.client.components.table.TableAction;
import org.rhq.coregui.client.components.table.TableSection;
import org.rhq.coregui.client.components.table.Table.TableActionInfo.ButtonColor;
import org.rhq.coregui.client.components.view.HasViewName;
import org.rhq.coregui.client.components.view.ViewName;
import org.rhq.coregui.client.gwt.GWTServiceLookup;
import org.rhq.coregui.client.gwt.SubjectGWTServiceAsync;
import org.rhq.coregui.client.util.message.Message;

/**
 * A table that lists all users and provides the ability to view or edit details of users, delete users, or create new
 * users. For the logged in user to view or edit a user other than themselves, they must possess the
 * {@link Permission#MANAGE_SECURITY MANAGE_SECURITY} permission. Furthermore, a user without MANAGE_SECURITY cannot
 * update their assigned roles. 
 *
 * @author Greg Hinkle
 * @author Ian Springer
 */
public class UsersView extends TableSection<UsersDataSource> implements HasViewName {

    public static final ViewName VIEW_ID = new ViewName("Users", MSG.common_title_users(), IconEnum.USERS);

    public static final String VIEW_PATH = AdministrationView.VIEW_ID + "/"
        + AdministrationView.SECTION_SECURITY_VIEW_ID + "/" + VIEW_ID;

    private boolean hasManageSecurity;

    public UsersView(boolean hasManageSecurity) {
        super(null);

        final UsersDataSource dataSource = UsersDataSource.getInstance();

        setDataSource(dataSource);
        setEscapeHtmlInDetailsLinkColumn(true);

        this.hasManageSecurity = hasManageSecurity;
    }

    @Override
    protected void configureTable() {
        updateSelectionStyle();
        getListGrid().addCellClickHandler(new CellClickHandler() {
            public void onCellClick(CellClickEvent event) {
                updateSelectionStyle();
            }
        });

        List<ListGridField> fields = createFields();
        setListGridFields(fields.toArray(new ListGridField[fields.size()]));

        addTableAction(MSG.common_button_new(), ButtonColor.BLUE, createNewAction());
        addTableAction(MSG.common_button_delete(), getDeleteConfirmMessage(), ButtonColor.RED, createDeleteAction());

        super.configureTable();
    }

    @Override
    public void refresh() {
        super.refresh();

        updateSelectionStyle();
    }

    private void updateSelectionStyle() {
        if (!hasManageSecurity) {
            getListGrid().deselectAllRecords();
        }
        SelectionStyle selectionStyle = hasManageSecurity ? getDefaultSelectionStyle() : SelectionStyle.NONE;
        getListGrid().setSelectionType(selectionStyle);
    }

    private List<ListGridField> createFields() {
        List<ListGridField> fields = new ArrayList<ListGridField>();

        ListGridField nameField = new ListGridField(UsersDataSource.Field.NAME, 150);
        fields.add(nameField);

        ListGridField activeField = new ListGridField(UsersDataSource.Field.FACTIVE, 90);
        fields.add(activeField);

        ListGridField ldapField = new ListGridField(UsersDataSource.Field.LDAP, 90);
        fields.add(ldapField);

        ListGridField firstNameField = new ListGridField(UsersDataSource.Field.FIRST_NAME, 150);
        firstNameField.setCellFormatter(new EscapedHtmlCellFormatter());
        fields.add(firstNameField);

        ListGridField lastNameField = new ListGridField(UsersDataSource.Field.LAST_NAME, 150);
        lastNameField.setCellFormatter(new EscapedHtmlCellFormatter());
        fields.add(lastNameField);

        ListGridField departmentField = new ListGridField(UsersDataSource.Field.DEPARTMENT, 150);
        departmentField.setCellFormatter(new EscapedHtmlCellFormatter());
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

    private TableAction createDeleteAction() {
        return new TableAction() {
            public boolean isEnabled(ListGridRecord[] selection) {
                if (!hasManageSecurity) {
                    return false;
                }

                int count = selection.length;
                if (count == 0) {
                    return false;
                }

                for (ListGridRecord record : selection) {
                    int subjectId = record.getAttributeAsInt(UsersDataSource.Field.ID);
                    if (UsersDataSource.isSystemSubjectId(subjectId)) {
                        // The superuser and rhqadmin users cannot be deleted.
                        return false;
                    }
                }
                return true;
            }

            public void executeAction(ListGridRecord[] selection, Object actionValue) {
                if (selection == null || selection.length == 0) {
                    return;
                }

                final ArrayList<String> doomedNames = new ArrayList<String>(selection.length);
                final int[] doomedIds = new int[selection.length];
                int i = 0;
                for (ListGridRecord record : selection) {
                    doomedNames.add(record.getAttribute(UsersDataSource.Field.NAME));
                    doomedIds[i++] = record.getAttributeAsInt(UsersDataSource.Field.ID);
                }

                SubjectGWTServiceAsync subjectService = GWTServiceLookup.getSubjectService();
                subjectService.deleteSubjects(doomedIds, new AsyncCallback<Void>() {
                    public void onFailure(Throwable caught) {
                        String message = MSG.dataSource_users_deleteFailed(doomedNames.toString());
                        CoreGUI.getErrorHandler().handleError(message, caught);
                        UsersView.this.refresh();
                    }

                    public void onSuccess(Void result) {
                        Message message = new Message(MSG.dataSource_users_delete(doomedNames.toString()));
                        CoreGUI.getMessageCenter().notify(message);
                        UsersView.this.refresh();
                    }
                });
            }
        };
    }

    @Override
    public Canvas getDetailsView(Integer subjectId) {
        return new UserEditView(subjectId);
    }

    @Override
    protected String getDeleteConfirmMessage() {
        return MSG.common_msg_deleteConfirm(MSG.common_label_users());
    }

    @Override
    public ViewName getViewName() {
        return VIEW_ID;
    }
}
