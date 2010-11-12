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

import com.google.gwt.user.client.History;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.data.Criteria;
import com.smartgwt.client.data.DSCallback;
import com.smartgwt.client.data.DSRequest;
import com.smartgwt.client.data.DSResponse;
import com.smartgwt.client.data.Record;
import com.smartgwt.client.types.Alignment;
import com.smartgwt.client.types.DSOperationType;
import com.smartgwt.client.types.Overflow;
import com.smartgwt.client.widgets.IButton;
import com.smartgwt.client.widgets.Label;
import com.smartgwt.client.widgets.form.DynamicForm;
import com.smartgwt.client.widgets.form.events.ItemChangedEvent;
import com.smartgwt.client.widgets.form.events.ItemChangedHandler;
import com.smartgwt.client.widgets.form.fields.FormItem;
import com.smartgwt.client.widgets.form.fields.HiddenItem;
import com.smartgwt.client.widgets.form.fields.PasswordItem;
import com.smartgwt.client.widgets.form.fields.RadioGroupItem;
import com.smartgwt.client.widgets.form.fields.StaticTextItem;
import com.smartgwt.client.widgets.form.fields.TextItem;
import com.smartgwt.client.widgets.form.fields.events.ChangedEvent;
import com.smartgwt.client.widgets.form.fields.events.ChangedHandler;
import com.smartgwt.client.widgets.grid.ListGridRecord;
import com.smartgwt.client.widgets.layout.HLayout;
import com.smartgwt.client.widgets.layout.VLayout;

import org.rhq.core.domain.auth.Principal;
import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.authz.Permission;
import org.rhq.enterprise.gui.coregui.client.BookmarkableView;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.DetailsView;
import org.rhq.enterprise.gui.coregui.client.UserSessionManager;
import org.rhq.enterprise.gui.coregui.client.ViewPath;
import org.rhq.enterprise.gui.coregui.client.components.TitleBar;
import org.rhq.enterprise.gui.coregui.client.components.form.EnhancedDynamicForm;
import org.rhq.enterprise.gui.coregui.client.components.selector.AssignedItemsChangedEvent;
import org.rhq.enterprise.gui.coregui.client.components.selector.AssignedItemsChangedHandler;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableIButton;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableVLayout;

/**
 * A form for viewing and/or editing an RHQ user (i.e. a {@link Subject}, and optionally an associated
 * {@link Principal}).
 *
 * @author Ian Springer
 * @author Greg Hinkle
 */
public class UserEditView extends LocatableVLayout implements BookmarkableView, DetailsView {

    private static final Label LOADING_LABEL = new Label("Loading...");
    private static final String HEADER_ICON = "global/User_24.png";
    private static final int SUBJECT_ID_RHQADMIN = 2;

    private int subjectId;
    private TitleBar titleBar;
    private VLayout editCanvas;
    private DynamicForm form;

    private UsersDataSource dataSource;

    private SubjectRoleSelector roleSelector;
    private IButton saveButton;
    private IButton resetButton;

    private boolean isReadOnly;
    private HLayout roleSelectionPane;
    private boolean hasManageSecurityPermission;

    public UserEditView(String locatorId, int subjectId) {
        super(locatorId);
        setPadding(7);

        // Define member variables.
        this.subjectId = subjectId;
        this.dataSource = UsersDataSource.getInstance();

        // Set properties for this VLayout.
        setOverflow(Overflow.AUTO);

        // Display a "Loading..." label at the top of the view to keep the user informed.
        addMember(LOADING_LABEL);

        loadGlobalPermissions();
    }

    private void loadGlobalPermissions() {
        GWTServiceLookup.getAuthorizationService().getExplicitGlobalPermissions(new AsyncCallback<Set<Permission>>() {
            public void onFailure(Throwable caught) {
                CoreGUI.getErrorHandler().handleError("Could not determine user's global permissions - assuming none.",
                    caught);
                Set<Permission> globalPermissions = EnumSet.noneOf(Permission.class);
                init(globalPermissions);
            }

            public void onSuccess(Set<Permission> globalPermissions) {
                init(globalPermissions);
            }
        });
    }

    private void init(Set<Permission> globalPermissions) {
        // Initialize member vars.
        Subject sessionSubject = UserSessionManager.getSessionSubject();
        boolean isEditingSelf = ((sessionSubject.getId() != subjectId));
        this.hasManageSecurityPermission = globalPermissions.contains(Permission.MANAGE_SECURITY);
        this.isReadOnly = (!hasManageSecurityPermission && !isEditingSelf);

        // Add remaining child widgets.
        this.titleBar = new TitleBar(null, HEADER_ICON);
        addMember(this.titleBar);        
        this.editCanvas = buildSubjectEditor();
        this.editCanvas.hide();
        addMember(this.editCanvas);
    }

    private VLayout buildSubjectEditor() {
        form = new EnhancedDynamicForm(this.getLocatorId(), this.isReadOnly);
        form.setDataSource(dataSource);

        List<FormItem> items = new ArrayList<FormItem>();                

        // Username field should be editable when creating a new user, but should be read-only for existing users.
        if (this.subjectId == 0) {
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
            final boolean[] initialPasswordChange = {true};
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

        HiddenItem rolesItem = new HiddenItem(UsersDataSource.Field.ROLES);
        items.add(rolesItem);

        this.roleSelectionPane = new HLayout();

        this.form.setItems(items.toArray(new FormItem[items.size()]));

        form.addItemChangedHandler(new ItemChangedHandler() {
            public void onItemChanged(ItemChangedEvent event) {
                UserEditView.this.onItemChanged();
            }
        });

        VLayout editCanvas = new VLayout();
        editCanvas.addMember(form);
        editCanvas.addMember(roleSelectionPane);

        if (!isReadOnly) {
            VLayout verticalSpacer = new VLayout();
            verticalSpacer.setHeight(12);
            editCanvas.addMember(verticalSpacer);

            HLayout buttonLayout = new HLayout(10);

            saveButton = new LocatableIButton(this.extendLocatorId("Save"), "Save");
            saveButton.setDisabled(true);
            saveButton.addClickHandler(new com.smartgwt.client.widgets.events.ClickHandler() {
                public void onClick(com.smartgwt.client.widgets.events.ClickEvent clickEvent) {
                    save();
                }
            });

            resetButton = new LocatableIButton(this.extendLocatorId("Reset"), "Reset");
            resetButton.setDisabled(true);
            resetButton.addClickHandler(new com.smartgwt.client.widgets.events.ClickHandler() {
                public void onClick(com.smartgwt.client.widgets.events.ClickEvent clickEvent) {
                    form.reset();
                    roleSelector.reset();
                    resetButton.disable();
                }
            });

            IButton cancelButton = new LocatableIButton(this.extendLocatorId("Cancel"), "Cancel");
            cancelButton.addClickHandler(new com.smartgwt.client.widgets.events.ClickHandler() {
                public void onClick(com.smartgwt.client.widgets.events.ClickEvent clickEvent) {
                    History.back();
                }
            });

            buttonLayout.addMember(saveButton);
            buttonLayout.addMember(resetButton);
            buttonLayout.addMember(cancelButton);

            editCanvas.addMember(buttonLayout);
        }

        return editCanvas;
    }

    private void onItemChanged() {
        // The below is a workaround for the fact that calling form.validate() causes the focus to change to the
        // last invalid field, if one or more fields is invalid.
        FormItem focusItem = form.getFocusItem();
        Boolean isValid = form.validate();
        if (focusItem != null) {
            form.focusInItem(focusItem);
        }

        // If we're in editable mode, update the button enablement.
        if (!isReadOnly) {
            saveButton.setDisabled(!isValid);
            resetButton.setDisabled(false);
        }
    }

    public void save() {
        ListGridRecord[] roleRecords = roleSelector.getAssignedGrid().getSelection();
        form.setValue(UsersDataSource.Field.ROLES, roleRecords);
        this.form.saveData();
    }

    @SuppressWarnings("unchecked")
    private void editRecord(final Record subjectRecord) {
        int subjectId = subjectRecord.getAttributeAsInt(UsersDataSource.Field.ID);
        Record[] roleRecords = subjectRecord.getAttributeAsRecordArray(UsersDataSource.Field.ROLES);
        ListGridRecord[] roleListGridRecords = new ListGridRecord[roleRecords.length];
        for (int i = 0, roleRecordsLength = roleRecords.length; i < roleRecordsLength; i++) {
            Record roleRecord = roleRecords[i];
            roleListGridRecords[i] = (ListGridRecord)roleRecord;
        }

        // A user can always view their own assigned roles, but only users with MANAGE_SECURITY can view or update
        // other users' assigned roles.
        Subject whoami = UserSessionManager.getSessionSubject();
        String username = subjectRecord.getAttribute(UsersDataSource.Field.NAME);
        if (this.hasManageSecurityPermission || whoami.getName().equals(username)) {
            boolean isLdap = Boolean.valueOf(subjectRecord.getAttribute(UsersDataSource.Field.LDAP));
            // In general, a user with MANAGE_SECURITY can update assigned roles, with two exceptions:
            //    1) rhqadmin's roles cannot be changed - the superuser role is all rhqadmin should ever need.
            //    2) an LDAP user's assigned roles cannot be modified except when mapping LDAP groups to LDAP roles,
            //       which is not done via this view.
            boolean isReadOnly = !hasManageSecurityPermission || (subjectId == SUBJECT_ID_RHQADMIN || isLdap);
            roleSelector = new SubjectRoleSelector(this.extendLocatorId("Roles"), roleListGridRecords, isReadOnly);
            roleSelector.setWidth100();
            roleSelector.setAlign(Alignment.LEFT);
            roleSelector.addAssignedItemsChangedHandler(new AssignedItemsChangedHandler() {
                public void onSelectionChanged(AssignedItemsChangedEvent event) {
                    onItemChanged();
                }
            });
            roleSelectionPane.addMember(roleSelector);
        }

        form.editRecord(subjectRecord);
        form.setSaveOperationType((subjectId == 0) ? DSOperationType.ADD : DSOperationType.UPDATE);

        LOADING_LABEL.hide();
        editCanvas.show();
        markForRedraw();
    }

    private void editNewSubject() {
        this.titleBar.setTitle("New User");

        Subject subject = new Subject();
        subject.setFactive(true);
        ListGridRecord record = dataSource.copyValues(subject);
        editRecord(record);

        // This tells form.saveData() to call UsersDataSource.executeAdd() on the new Subject's ListGridRecord
        form.setSaveOperationType(DSOperationType.ADD);
    }

    private void editExistingSubject(final int subjectId) {
        Criteria criteria = new Criteria(UsersDataSource.Field.ID, String.valueOf(subjectId));
        new UsersDataSource().fetchData(criteria, new DSCallback() {
            public void execute(DSResponse response, Object rawData, DSRequest request) {
                if (response.getStatus() == DSResponse.STATUS_SUCCESS) {
                    Record[] userRecords = response.getData();
                    Record userRecord = userRecords[0];
                    editRecord(userRecord);
                    // Perform up front validation for existing users.
                    // NOTE: We do *not* do this for new users, since we expect most of the required fields to be blank.
                    form.validate();

                    // Don't allow the rhqadmin account to be disabled.
                    if (subjectId == SUBJECT_ID_RHQADMIN) {
                        FormItem activeField = form.getField(UsersDataSource.Field.FACTIVE);
                        activeField.disable();
                    }

                    String username = userRecord.getAttribute(UsersDataSource.Field.NAME);
                    UserEditView.this.titleBar.setTitle("User '" + username + "'");
                }
            }
        });
    }

    @Override
    public boolean isEditable() {
        return (!this.isReadOnly);
    }

    @Override
    public void renderView(ViewPath viewPath) {
        int subjectId = viewPath.getCurrentAsInt();
        if (subjectId == 0) {
            editNewSubject();
        } else {
            editExistingSubject(subjectId);
        }
    }
    
}
