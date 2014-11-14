/*
 * RHQ Management Platform
 * Copyright (C) 2005-2014 Red Hat, Inc.
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.rhq.core.domain.auth.Principal;
import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.authz.Permission;
import org.rhq.coregui.client.CoreGUI;
import org.rhq.coregui.client.PermissionsLoadedListener;
import org.rhq.coregui.client.PermissionsLoader;
import org.rhq.coregui.client.UserSessionManager;
import org.rhq.coregui.client.ViewPath;
import org.rhq.coregui.client.components.form.AbstractRecordEditor;
import org.rhq.coregui.client.components.form.EnhancedDynamicForm;
import org.rhq.coregui.client.components.selector.AssignedItemsChangedEvent;
import org.rhq.coregui.client.components.selector.AssignedItemsChangedHandler;
import org.rhq.coregui.client.gwt.GWTServiceLookup;
import org.rhq.coregui.client.util.Log;
import org.rhq.coregui.client.util.preferences.UserPreferenceNames.UiSubsystem;
import org.rhq.coregui.client.util.preferences.UserPreferences;

import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.data.DSRequest;
import com.smartgwt.client.data.Record;
import com.smartgwt.client.types.Alignment;
import com.smartgwt.client.types.ListGridEditEvent;
import com.smartgwt.client.types.ListGridFieldType;
import com.smartgwt.client.widgets.Label;
import com.smartgwt.client.widgets.form.fields.CheckboxItem;
import com.smartgwt.client.widgets.form.fields.FormItem;
import com.smartgwt.client.widgets.form.fields.PasswordItem;
import com.smartgwt.client.widgets.form.fields.RadioGroupItem;
import com.smartgwt.client.widgets.form.fields.StaticTextItem;
import com.smartgwt.client.widgets.form.fields.TextItem;
import com.smartgwt.client.widgets.form.fields.events.ChangedEvent;
import com.smartgwt.client.widgets.form.fields.events.ChangedHandler;
import com.smartgwt.client.widgets.grid.CellFormatter;
import com.smartgwt.client.widgets.grid.ListGrid;
import com.smartgwt.client.widgets.grid.ListGridField;
import com.smartgwt.client.widgets.grid.ListGridRecord;
import com.smartgwt.client.widgets.grid.events.HeaderClickEvent;
import com.smartgwt.client.widgets.grid.events.HeaderClickHandler;
import com.smartgwt.client.widgets.layout.VLayout;

/**
 * A form for viewing and/or editing an RHQ user (i.e. a {@link Subject}, and if the user is authenticated via RHQ and
 * not LDAP, the password of the associated {@link Principal}).
 *
 * @author Ian Springer
 * @author Jirka Kremser
 */
public class UserEditView extends AbstractRecordEditor<UsersDataSource> {

    private static final String HEADER_ICON = "global/User_24.png";
    private static final int SUBJECT_ID_RHQADMIN = 2;

    private SubjectRoleSelector roleSelector;

    private boolean loggedInUserHasManageSecurityPermission;
    private boolean ldapAuthorizationEnabled;

    private ListGrid uiCustomizationGrid;
    private UserPreferences prefs;
    private List<Integer> initialState;
    private ListGridRecord[] initData;
    private ListGrid uiShowGrid;

    public UserEditView(int subjectId) {
        super(new UsersDataSource(), subjectId, MSG.common_label_user(), HEADER_ICON);
        injectHelperFunctions(this);
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
                    CoreGUI.getErrorHandler().handleError(
                        "Failed to load global permissions for current user. Perhaps the Server is down.");
                    return;
                }

                UserEditView.this.loggedInUserHasManageSecurityPermission = permissions
                    .contains(Permission.MANAGE_SECURITY);
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
        Record userRecord = UsersDataSource.getInstance().copyUserValues(subject, false);
        return userRecord;
    }

    @Override
    protected void editRecord(Record record) {
        super.editRecord(record);

        Subject sessionSubject = UserSessionManager.getSessionSubject();
        boolean userBeingEditedIsLoggedInUser = (getRecordId() == sessionSubject.getId());
        prefs = UserSessionManager.getUserPreferences();

        // A user can always view their own assigned roles, but only users with MANAGE_SECURITY can view or update
        // other users' assigned roles.
        if (this.loggedInUserHasManageSecurityPermission || userBeingEditedIsLoggedInUser) {
            VLayout spacer = null;
            if (userBeingEditedIsLoggedInUser) {
                spacer = new VLayout();
                spacer.setHeight(10);
                getContentPane().addMember(spacer);
                Label showUiSubsystems = new Label("<h4>" + MSG.view_adminUsers_ui_label() + "</h4>");
                showUiSubsystems.setHeight(17);
                getContentPane().addMember(showUiSubsystems);
                uiCustomizationGrid = createUiCustomizationGrid();
                getContentPane().addMember(uiCustomizationGrid);
                spacer = new VLayout();
                spacer.setHeight(10);
                getContentPane().addMember(spacer);
                Label rolesHeader = new Label("<h4>" + MSG.common_title_roles() + "</h4>");
                rolesHeader.setHeight(17);
                getContentPane().addMember(rolesHeader);
            }

            Record[] roleRecords = record.getAttributeAsRecordArray(UsersDataSource.Field.ROLES);
            ListGridRecord[] roleListGridRecords = toListGridRecordArray(roleRecords);
            boolean rolesAreReadOnly = areRolesReadOnly(record);

            this.roleSelector = new SubjectRoleSelector(roleListGridRecords, rolesAreReadOnly);
            this.roleSelector.addAssignedItemsChangedHandler(new AssignedItemsChangedHandler() {
                public void onSelectionChanged(AssignedItemsChangedEvent event) {
                    UserEditView.this.onItemChanged();
                }
            });
            getContentPane().addMember(this.roleSelector);
        }
    }

    private ListGrid createUiCustomizationGrid() {
        uiShowGrid = new ListGrid();
        uiShowGrid.addHeaderClickHandler(new HeaderClickHandler() {
            // when changing the sorting the cells are reformated using all the custom cell formatters,
            // so we need to call the bootstrapSwitch() again. Unfortunately we can't hook to draw() method of ListGrid
            @Override
            public void onHeaderClick(HeaderClickEvent event) {
                new Timer() {
                    @Override
                    public void run() {
                        initSwitches();
                    }
                }.schedule(200);
            }
        });
        uiShowGrid.setStyleName("showSubsystemsGrid");
        ListGridField iconField = new ListGridField("icon", "&nbsp;", 28);
        iconField.setShowDefaultContextMenu(false);
        iconField.setCanSort(false);
        iconField.setAlign(Alignment.CENTER);
        iconField.setType(ListGridFieldType.IMAGE);
        iconField.setImageURLSuffix("_16.png");
        iconField.setImageWidth(16);
        iconField.setImageHeight(16);
        iconField.setCanEdit(false);

        ListGridField displayNameField = new ListGridField("displayName", MSG.common_title_name(), 130);
        displayNameField.setCanEdit(false);

        ListGridField descriptionField = new ListGridField("description", MSG.common_title_description());
        descriptionField.setWrap(true);
        descriptionField.setCanEdit(false);

        final ListGridField showFieldHidden = new ListGridField("showSubsystem", MSG.common_title_show(), 50);
        showFieldHidden.setHidden(true);
        showFieldHidden.setType(ListGridFieldType.IMAGE);
        showFieldHidden.setImageSize(11);
        LinkedHashMap<String, String> valueMap = new LinkedHashMap<String, String>(2);
        valueMap.put(Boolean.TRUE.toString(), "global/permission_enabled_11.png");
        valueMap.put(Boolean.FALSE.toString(), "global/permission_disabled_11.png");
        showFieldHidden.setValueMap(valueMap);
        showFieldHidden.setCanEdit(true);
        CheckboxItem editor = new CheckboxItem();
        showFieldHidden.setEditorType(editor);
        showFieldHidden.addChangedHandler(new com.smartgwt.client.widgets.grid.events.ChangedHandler() {
            @Override
            public void onChanged(com.smartgwt.client.widgets.grid.events.ChangedEvent event) {
                UserEditView.this.onItemChanged();
            }
        });

        final ListGridField showField = new ListGridField("showSubsystemSwitch", MSG.common_title_show(), 100);
        showField.addChangedHandler(new com.smartgwt.client.widgets.grid.events.ChangedHandler() {
            @Override
            public void onChanged(com.smartgwt.client.widgets.grid.events.ChangedEvent event) {
                UserEditView.this.onItemChanged();
            }
        });
        showField.setCellFormatter(new CellFormatter() {
            @Override
            public String format(Object value, ListGridRecord record, int rowNum, int colNum) {
                boolean show = "true".equals(record.getAttributeAsString("showSubsystem").toString());
                
                return "<input type='checkbox' name='rhqSwitch' id='showSubsystem" + rowNum + "' data-on-text='"
                    + MSG.common_title_show() + "' data-off-text='" + MSG.view_adminUsers_ui_hide()
                    + "' onchange='__gwt_onItemChange();' data-size='mini' " + (show ? "checked" : "") + ">";
            }
        });
        showField.setCanEdit(false);
        uiShowGrid.setFields(iconField, displayNameField, showField, descriptionField, showFieldHidden);

        Map<UiSubsystem, Boolean> showSubsystems = prefs.getShowUiSubsystems();
        initializeDefaultState(showSubsystems);

        List<ListGridRecord> records = new ArrayList<ListGridRecord>();
        ListGridRecord record = createShowUiSubsystemRecord(
            MSG.view_admin_content() + " & " + MSG.common_title_bundles(), showSubsystems.get(UiSubsystem.CONTENT),
            "subsystems/content/Content", MSG.view_adminUsers_ui_content());
        records.add(record);
        record = createShowUiSubsystemRecord(MSG.view_reportsTop_title(), showSubsystems.get(UiSubsystem.REPORTS),
            "subsystems/report/Document", MSG.view_adminUsers_ui_reports());
        records.add(record);
        record = createShowUiSubsystemRecord(MSG.view_admin_administration(),
            showSubsystems.get(UiSubsystem.ADMINISTRATION), "types/Service_type", MSG.view_adminUsers_ui_admin());
        records.add(record);
        record = createShowUiSubsystemRecord(MSG.view_tabs_common_events(), showSubsystems.get(UiSubsystem.EVENTS),
            "subsystems/event/Events", MSG.view_adminUsers_ui_events());
        records.add(record);
        record = createShowUiSubsystemRecord(MSG.common_title_operations(), showSubsystems.get(UiSubsystem.OPERATIONS),
            "subsystems/control/Operation", MSG.view_adminUsers_ui_ops());
        records.add(record);
        record = createShowUiSubsystemRecord(MSG.common_title_alerts(), showSubsystems.get(UiSubsystem.ALERTS),
            "subsystems/alert/Alerts", MSG.view_adminUsers_ui_alerts());
        records.add(record);
        record = createShowUiSubsystemRecord(MSG.common_title_configuration(), showSubsystems.get(UiSubsystem.CONFIG),
            "subsystems/configure/Configure", MSG.view_adminUsers_ui_config());
        records.add(record);
        record = createShowUiSubsystemRecord(MSG.view_tabs_common_drift(), showSubsystems.get(UiSubsystem.DRIFT),
            "subsystems/drift/Drift", MSG.view_adminUsers_ui_drift());
        records.add(record);
        initData = records.toArray(new ListGridRecord[records.size()]);
        uiShowGrid.setData(initData);
        uiShowGrid.setCanEdit(true);
        uiShowGrid.setEditEvent(ListGridEditEvent.CLICK);
        return uiShowGrid;
    }

    private native void injectHelperFunctions(UserEditView view) /*-{
        $wnd.__gwt_onItemChange = $entry(function(){
            view.@org.rhq.coregui.client.admin.users.UserEditView::onItemChanged()();
        });
    }-*/;
    
    // 4 (20 * 200ms) seconds should be fine even for very old browsers / slow PCs to render 8 lines in the ListGrid
    private native void initSwitchesUntilItsDone() /*-{
        $wnd.$.getScript("js/bootstrap-switch.min.js", function(){
            $wnd.$.fn.bootstrapSwitch.defaults.handleWidth = '40px';
            $wnd.$.fn.bootstrapSwitch.defaults.labelWidth = '40px';
            var affectedElems = 0;
            $wnd.tryToInitialize = function() {
                $wnd.console.info('Initializing bootstrap-switch...');
                affectedElems = $wnd.$("[name='rhqSwitch']").bootstrapSwitch();
            };
            $wnd.tryToInitialize();
            var attempts = 20;
            do {
                $wnd.setTimeout("tryToInitialize();", 200);
            } while (affectedElems.length != 8 && attempts-- > 0)
        });
    }-*/;
    
    private native void initSwitches() /*-{
        $wnd.$("[name='rhqSwitch']").bootstrapSwitch();
    }-*/;

    private native boolean isSubsystemShown(int id) /*-{
        return $wnd.$('#showSubsystem' + id).is(':checked')
    }-*/;
    
    @Override
    protected void onDraw() {
        super.onDraw();
        // we need to wait for cell formatters to do their work and then run the jsni method on updated DOM
        new Timer() {
            @Override
            public void run() {
                initSwitchesUntilItsDone();
            }
        }.schedule(1000);
    }

    private void initializeDefaultState(Map<UiSubsystem, Boolean> showSubsystems) {
        initialState = new ArrayList<Integer>();
        for (UiSubsystem subsystem : UiSubsystem.values()) {
            initialState.add(showSubsystems.get(subsystem) ? 1 : 0);
        }
    }

    private ListGridRecord createShowUiSubsystemRecord(String displayName, boolean show, String icon, String description) {
        ListGridRecord record = new ListGridRecord();
        record.setAttribute("displayName", displayName);
        record.setAttribute("showSubsystem", show);
        record.setAttribute("showSubsystemSwitch", show ? "checked" : "");
        record.setAttribute("icon", icon);
        record.setAttribute("description", description);
        return record;
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
            ((StaticTextItem) nameItem).setEscapeHTML(true);
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

    private List<Integer> getIntegerList() {
        List<Integer> integerList = new ArrayList<Integer>(8);
        for (int i = 0, length = uiCustomizationGrid.getRecords().length; i < length; i++) {
            integerList.add(isSubsystemShown(i) ? 1 : 0);
        }
        return integerList;
    }

    @Override
    protected void save(final DSRequest requestProperties) {
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
    protected void postSaveAction() {
        List<Integer> currentState = getIntegerList();
        if (!initialState.equals(currentState)) {
            prefs.setShowUiSubsystems(currentState, new AsyncCallback<Subject>() {
                @Override
                public void onFailure(Throwable caught) {
                    CoreGUI.getErrorHandler().handleError("Cannot store preferences", caught);
                }

                @Override
                public void onSuccess(Subject subject) {
                    // do the refresh to reflect the changes to current UI
                    new Timer() {
                        @Override
                        public void run() {
                            Window.Location.reload();
                        }
                    }.schedule(100);
                }
            });
        }
    }
    
    @Override
    protected boolean showResetButton() {
        return false;
    }

//    @Override
//    protected void reset() {
//        super.reset();
//
//        if (this.roleSelector != null) {
//            this.roleSelector.reset();
//        }
//        
//        if (uiShowGrid != null && initData != null && initData.length > 0) {
//            uiShowGrid.setData(initData);
//            new Timer() {
//                @Override
//                public void run() {
//                    initSwitches();
//                }
//            }.schedule(190);
//        }
//    }
}
