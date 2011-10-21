/*
 * RHQ Management Platform
 * Copyright (C) 2005-2011 Red Hat, Inc.
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
import java.util.List;
import java.util.Set;

import com.allen_sauer.gwt.log.client.Log;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.data.DSRequest;
import com.smartgwt.client.data.Record;
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
import org.rhq.core.domain.common.composite.SystemSetting;
import org.rhq.core.domain.common.composite.SystemSettings;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.PermissionsLoadedListener;
import org.rhq.enterprise.gui.coregui.client.PermissionsLoader;
import org.rhq.enterprise.gui.coregui.client.UserSessionManager;
import org.rhq.enterprise.gui.coregui.client.ViewPath;
import org.rhq.enterprise.gui.coregui.client.components.form.AbstractRecordEditor;
import org.rhq.enterprise.gui.coregui.client.components.form.EnhancedDynamicForm;
import org.rhq.enterprise.gui.coregui.client.components.selector.AssignedItemsChangedEvent;
import org.rhq.enterprise.gui.coregui.client.components.selector.AssignedItemsChangedHandler;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;

/**
 * A form for viewing and/or editing an RHQ user (i.e. a {@link Subject}, and if the user is authenticated via RHQ and
 * not LDAP, the password of the associated {@link Principal}).
 *
 * @author Ian Springer
 */
public class UserEditView extends AbstractRecordEditor<UsersDataSource> {

    private static final String HEADER_ICON = "global/User_24.png";
    private static final int SUBJECT_ID_RHQADMIN = 2;

    private SubjectRoleSelector roleSelector;

    private boolean loggedInUserHasManageSecurityPermission;
    private boolean ldapAuthorizationEnabled;

    public UserEditView(String locatorId, int subjectId) {
        super(locatorId, new UsersDataSource(), subjectId, MSG.common_label_user(), HEADER_ICON);
    }

    @Override
    public void renderView(ViewPath viewPath) {
        super.renderView(viewPath);

        // Step 1 of async init: load current user's global permissions.
        new PermissionsLoader().loadExplicitGlobalPermissions(new PermissionsLoadedListener() {
            @Override
            public void onPermissionsLoaded(Set<Permission> permissions) {
                if (permissions == null) {
                    // TODO: i18n
                    CoreGUI.getErrorHandler().handleError("Failed to load global permissions for current user. Perhaps the Server is down.");
                    return;
                }

                UserEditView.this.loggedInUserHasManageSecurityPermission = permissions.contains(Permission.MANAGE_SECURITY);
                Subject sessionSubject = UserSessionManager.getSessionSubject();
                boolean isEditingSelf = (sessionSubject.getId() == getRecordId());
                final boolean isReadOnly = (!UserEditView.this.loggedInUserHasManageSecurityPermission && !isEditingSelf);

                // Step 2 of async init: check if LDAP authz is enabled in system settings.
                GWTServiceLookup.getSystemService().isLdapAuthorizationEnabled(new AsyncCallback<Boolean>() {
                    public void onFailure(Throwable caught) {
                        // TODO: i18n
                        CoreGUI.getErrorHandler()
                            .handleError(
                                "Failed to determine if LDAP authorization is enabled. Perhaps the Server is down.",
                                caught);
                    }

                    public void onSuccess(Boolean ldapAuthz) {
                        UserEditView.this.ldapAuthorizationEnabled = ldapAuthz;
                        Log.debug("LDAP authorization is " + ((ldapAuthorizationEnabled) ? "" : "not ") + "enabled.");

                        // Step 3 of async init: call super.init() to draw the editor.
                        UserEditView.this.init(isReadOnly);
                    }
                });
            }
        });
    }

    @Override
    protected Record createNewRecord() {
        Subject subject = new Subject();
        subject.setFactive(true);
        @SuppressWarnings( { "UnnecessaryLocalVariable" })
        Record userRecord = UsersDataSource.getInstance().copyUserValues(subject, false);
        return userRecord;
    }

    @Override
    protected void editRecord(Record record) {
        super.editRecord(record);

        Subject sessionSubject = UserSessionManager.getSessionSubject();
        boolean userBeingEditedIsLoggedInUser = (getRecordId() == sessionSubject.getId());

        // A user can always view their own assigned roles, but only users with MANAGE_SECURITY can view or update
        // other users' assigned roles.
        if (this.loggedInUserHasManageSecurityPermission || userBeingEditedIsLoggedInUser) {
            Record[] roleRecords = record.getAttributeAsRecordArray(UsersDataSource.Field.ROLES);
            ListGridRecord[] roleListGridRecords = toListGridRecordArray(roleRecords);
            boolean rolesAreReadOnly = areRolesReadOnly(record);

            this.roleSelector = new SubjectRoleSelector(this.extendLocatorId("Roles"), roleListGridRecords,
                rolesAreReadOnly);
            this.roleSelector.addAssignedItemsChangedHandler(new AssignedItemsChangedHandler() {
                public void onSelectionChanged(AssignedItemsChangedEvent event) {
                    UserEditView.this.onItemChanged();
                }
            });
            getContentPane().addMember(this.roleSelector);
        }
    }

    //
    // In general, a user with MANAGE_SECURITY can update assigned roles, with two exceptions:
    //
    //    1) if LDAP authorization is enabled, an LDAP-authenticated user's assigned roles cannot be modified directly;
    //       instead an "LDAP role" is automatically assigned to the user if the user is a member of one or more of the
    //       LDAP groups associated with that role; a user with MANAGE_SECURITY can assign LDAP groups to an LDAP role
    //       by editing the role
    //    2) rhqadmin's roles cannot be changed - the superuser role is all rhqadmin should ever need.
    //
    private boolean areRolesReadOnly(Record record) {
        if (!this.loggedInUserHasManageSecurityPermission) {
            return true;
        }
        boolean isLdapAuthenticatedUser = Boolean.valueOf(record.getAttribute(UsersDataSource.Field.LDAP));
        return (getRecordId() == SUBJECT_ID_RHQADMIN) || (isLdapAuthenticatedUser && this.ldapAuthorizationEnabled);
    }

    @Override
    protected List<FormItem> createFormItems(EnhancedDynamicForm form) {
        List<FormItem> items = new ArrayList<FormItem>();

        // Username field should be editable when creating a new user, but should be read-only for existing users.
        FormItem nameItem;
        if (isNewRecord()) {
            nameItem = new TextItem(UsersDataSource.Field.NAME);
        } else {
            nameItem = new StaticTextItem(UsersDataSource.Field.NAME);
            ((StaticTextItem)nameItem).setOutputAsHTML(true);
        }
        items.add(nameItem);

        StaticTextItem isLdapItem = new StaticTextItem(UsersDataSource.Field.LDAP);
        items.add(isLdapItem);
        boolean isLdapAuthenticatedUser = Boolean.valueOf(form.getValueAsString(UsersDataSource.Field.LDAP));

        // Only display the password fields for non-LDAP users (i.e. users that have an associated RHQ Principal).
        if (!this.isReadOnly() && !isLdapAuthenticatedUser) {
            PasswordItem passwordItem = new PasswordItem(UsersDataSource.Field.PASSWORD);
            passwordItem.setShowTitle(true);
            items.add(passwordItem);

            final PasswordItem verifyPasswordItem = new PasswordItem(UsersDataSource.Field.PASSWORD_VERIFY);
            verifyPasswordItem.setShowTitle(true);
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
        firstNameItem.setShowTitle(true);
        firstNameItem.setAttribute(EnhancedDynamicForm.OUTPUT_AS_HTML_ATTRIBUTE, true);
        items.add(firstNameItem);

        TextItem lastNameItem = new TextItem(UsersDataSource.Field.LAST_NAME);
        lastNameItem.setShowTitle(true);
        lastNameItem.setAttribute(EnhancedDynamicForm.OUTPUT_AS_HTML_ATTRIBUTE, true);
        items.add(lastNameItem);

        TextItem emailAddressItem = new TextItem(UsersDataSource.Field.EMAIL_ADDRESS);
        emailAddressItem.setShowTitle(true);
        emailAddressItem.setAttribute(EnhancedDynamicForm.OUTPUT_AS_HTML_ATTRIBUTE, true);
        items.add(emailAddressItem);

        TextItem phoneNumberItem = new TextItem(UsersDataSource.Field.PHONE_NUMBER);
        phoneNumberItem.setAttribute(EnhancedDynamicForm.OUTPUT_AS_HTML_ATTRIBUTE, true);
        items.add(phoneNumberItem);

        TextItem departmentItem = new TextItem(UsersDataSource.Field.DEPARTMENT);
        departmentItem.setAttribute(EnhancedDynamicForm.OUTPUT_AS_HTML_ATTRIBUTE, true);
        items.add(departmentItem);

        boolean userBeingEditedIsRhqadmin = (getRecordId() == SUBJECT_ID_RHQADMIN);
        FormItem activeItem;
        if (!this.loggedInUserHasManageSecurityPermission || userBeingEditedIsRhqadmin) {
            activeItem = new StaticTextItem(UsersDataSource.Field.FACTIVE);
        } else {
            RadioGroupItem activeRadioGroupItem = new RadioGroupItem(UsersDataSource.Field.FACTIVE);
            activeRadioGroupItem.setVertical(false);
            activeItem = activeRadioGroupItem;
        }
        items.add(activeItem);

        return items;
    }

    @Override
    protected void save(DSRequest requestProperties) {
        // Grab the currently assigned roles from the selector and stick them into the corresponding canvas
        // item on the form, so when the form is saved, they'll get submitted along with the rest of the simple fields
        // to the datasource's add or update methods.
        if (roleSelector != null) {
            ListGridRecord[] roleRecords = this.roleSelector.getSelectedRecords();
            getForm().setValue(UsersDataSource.Field.ROLES, roleRecords);
        }

        // Submit the form values to the datasource.
        super.save(requestProperties);
    }

    @Override
    protected void reset() {
        super.reset();

        if (this.roleSelector != null) {
            this.roleSelector.reset();
        }
    }

}
