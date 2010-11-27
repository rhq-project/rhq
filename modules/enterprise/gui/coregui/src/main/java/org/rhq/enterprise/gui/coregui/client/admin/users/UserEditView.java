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
import com.smartgwt.client.data.Record;
import com.smartgwt.client.types.Alignment;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.form.fields.CanvasItem;
import com.smartgwt.client.widgets.form.fields.FormItem;
import com.smartgwt.client.widgets.form.fields.PasswordItem;
import com.smartgwt.client.widgets.form.fields.RadioGroupItem;
import com.smartgwt.client.widgets.form.fields.StaticTextItem;
import com.smartgwt.client.widgets.form.fields.TextItem;
import com.smartgwt.client.widgets.form.fields.events.ChangedEvent;
import com.smartgwt.client.widgets.form.fields.events.ChangedHandler;
import com.smartgwt.client.widgets.grid.ListGridRecord;

import org.rhq.core.domain.auth.Principal;
import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.authz.Permission;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.UserSessionManager;
import org.rhq.enterprise.gui.coregui.client.ViewPath;
import org.rhq.enterprise.gui.coregui.client.components.form.AbstractRecordEditor;
import org.rhq.enterprise.gui.coregui.client.components.form.EnhancedDynamicForm;
import org.rhq.enterprise.gui.coregui.client.components.selector.AssignedItemsChangedEvent;
import org.rhq.enterprise.gui.coregui.client.components.selector.AssignedItemsChangedHandler;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.util.message.Message;

/**
 * A form for viewing and/or editing an RHQ user (i.e. a {@link Subject}, and optionally an associated
 * {@link Principal}).
 *
 * @author Ian Springer
 */
public class UserEditView extends AbstractRecordEditor<UsersDataSource> {

    private static final String HEADER_ICON = "global/User_24.png";
    private static final int SUBJECT_ID_RHQADMIN = 2;

    private CanvasItem rolesItem;
    private SubjectRoleSelector roleSelector;

    private boolean hasManageSecurityPermission;

    public UserEditView(String locatorId, int subjectId) {
        super(locatorId, new UsersDataSource(), subjectId, MSG.common_label_user(), HEADER_ICON);
    }

    @Override
    public void renderView(ViewPath viewPath) {
        super.renderView(viewPath);

        GWTServiceLookup.getAuthorizationService().getExplicitGlobalPermissions(new AsyncCallback<Set<Permission>>() {
            @Override
            public void onSuccess(Set<Permission> result) {
                UserEditView.this.hasManageSecurityPermission = result.contains(Permission.MANAGE_SECURITY);
                Subject sessionSubject = UserSessionManager.getSessionSubject();
                boolean isEditingSelf = (sessionSubject.getId() == getRecordId());
                boolean isReadOnly = (!UserEditView.this.hasManageSecurityPermission && !isEditingSelf);
                init(isReadOnly);
            }

            @Override
            public void onFailure(Throwable caught) {
                CoreGUI.getMessageCenter().notify(
                    new Message(MSG.util_userPerm_loadFailGlobal(), caught, Message.Severity.Error, EnumSet
                        .of(Message.Option.BackgroundJobResult)));
            }
        });
    }

    @Override
    protected Record createNewRecord() {
        Subject subject = new Subject();
        subject.setFactive(true);
        @SuppressWarnings( { "UnnecessaryLocalVariable" })
        Record userRecord = UsersDataSource.getInstance().copyValues(subject, false);
        return userRecord;
    }

    @Override
    protected void editRecord(Record record) {
        super.editRecord(record);

        // Don't allow the rhqadmin account to be disabled.
        if (getRecordId() == SUBJECT_ID_RHQADMIN) {
            FormItem activeField = getForm().getField(UsersDataSource.Field.FACTIVE);
            activeField.disable();
        }

        // A user can always view their own assigned roles, but only users with MANAGE_SECURITY can view or update
        // other users' assigned roles.
        Subject whoami = UserSessionManager.getSessionSubject();
        String username = record.getAttribute(UsersDataSource.Field.NAME);
        if (this.hasManageSecurityPermission || whoami.getName().equals(username)) {
            Record[] roleRecords = record.getAttributeAsRecordArray(UsersDataSource.Field.ROLES);
            ListGridRecord[] roleListGridRecords = toListGridRecordArray(roleRecords);

            boolean isReadOnly = areRolesReadOnly(record);

            roleSelector = new SubjectRoleSelector(this.extendLocatorId("Roles"), roleListGridRecords, isReadOnly);
            roleSelector.setWidth100();
            roleSelector.setAlign(Alignment.LEFT);
            roleSelector.addAssignedItemsChangedHandler(new AssignedItemsChangedHandler() {
                public void onSelectionChanged(AssignedItemsChangedEvent event) {
                    onItemChanged();
                }
            });
            this.rolesItem.setCanvas(this.roleSelector);
        }
    }

    //
    // In general, a user with MANAGE_SECURITY can update assigned roles, with two exceptions:
    //
    //    1) an LDAP user's assigned roles cannot be modified except when mapping LDAP groups to LDAP roles,
    //       which is not done via this view.
    //    2) rhqadmin's roles cannot be changed - the superuser role is all rhqadmin should ever need.
    //
    private boolean areRolesReadOnly(Record record) {
        boolean isLdap = Boolean.valueOf(record.getAttribute(UsersDataSource.Field.LDAP));
        return (!hasManageSecurityPermission || (getRecordId() == SUBJECT_ID_RHQADMIN) || isLdap);
    }

    @Override
    protected List<FormItem> createFormItems(EnhancedDynamicForm form) {
        List<FormItem> items = new ArrayList<FormItem>();

        // Username field should be editable when creating a new user, but should be read-only for existing users.
        if (form.isNewRecord()) {
            TextItem nameItem = new TextItem(UsersDataSource.Field.NAME);
            items.add(nameItem);
        } else {
            StaticTextItem nameItem = new StaticTextItem(UsersDataSource.Field.NAME);
            items.add(nameItem);
        }

        RadioGroupItem isLdapItem = new RadioGroupItem(UsersDataSource.Field.LDAP);
        isLdapItem.setVertical(false);
        items.add(isLdapItem);

        boolean isLdap = Boolean.valueOf(isLdapItem.getValueAsString());

        // Only display the password fields for non-LDAP users (i.e. users that have an associated RHQ Principal).
        if (!isLdap) {
            PasswordItem passwordItem = new PasswordItem(UsersDataSource.Field.PASSWORD);
            items.add(passwordItem);

            final PasswordItem verifyPasswordItem = new PasswordItem(UsersDataSource.Field.PASSWORD_VERIFY);
            final boolean[] initialPasswordChange = { true };
            passwordItem.addChangedHandler(new ChangedHandler() {
                public void onChanged(ChangedEvent event) {
                    if (initialPasswordChange[0]) {
                        verifyPasswordItem.clearValue();
                        initialPasswordChange[0] = false;
                    }
                }
            });

            items.add(verifyPasswordItem);
        }

        TextItem firstNameItem = new TextItem(UsersDataSource.Field.FIRST_NAME);
        items.add(firstNameItem);

        TextItem lastNameItem = new TextItem(UsersDataSource.Field.LAST_NAME);
        items.add(lastNameItem);

        TextItem emailAddressItem = new TextItem(UsersDataSource.Field.EMAIL_ADDRESS);
        items.add(emailAddressItem);

        TextItem phoneNumberItem = new TextItem(UsersDataSource.Field.PHONE_NUMBER);
        items.add(phoneNumberItem);

        TextItem departmentItem = new TextItem(UsersDataSource.Field.DEPARTMENT);
        items.add(departmentItem);

        RadioGroupItem activeItem = new RadioGroupItem(UsersDataSource.Field.FACTIVE);
        activeItem.setVertical(false);
        items.add(activeItem);

        this.rolesItem = new CanvasItem(UsersDataSource.Field.ROLES);
        this.rolesItem.setShowTitle(false);
        this.rolesItem.setColSpan(form.getNumCols());
        this.rolesItem.setCanvas(new Canvas());
        items.add(this.rolesItem);

        return items;
    }

    @Override
    protected void save() {
        ListGridRecord[] roleRecords = this.roleSelector.getAssignedGrid().getRecords();
        getForm().setValue(UsersDataSource.Field.ROLES, roleRecords);

        // Submit the form values to the datasource.
        super.save();
    }

    @Override
    protected void reset() {
        super.reset();
        this.roleSelector.reset();
    }

}
