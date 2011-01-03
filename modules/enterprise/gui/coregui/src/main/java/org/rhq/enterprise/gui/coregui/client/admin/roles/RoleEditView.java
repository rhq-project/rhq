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

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.data.Record;
import com.smartgwt.client.types.Overflow;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.Label;
import com.smartgwt.client.widgets.form.fields.FormItem;
import com.smartgwt.client.widgets.form.fields.TextItem;
import com.smartgwt.client.widgets.grid.ListGridRecord;
import com.smartgwt.client.widgets.tab.Tab;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.authz.Permission;
import org.rhq.core.domain.authz.Role;
import org.rhq.core.domain.resource.group.GroupCategory;
import org.rhq.enterprise.gui.coregui.client.BookmarkableView;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.ImageManager;
import org.rhq.enterprise.gui.coregui.client.PermissionsLoadedListener;
import org.rhq.enterprise.gui.coregui.client.PermissionsLoader;
import org.rhq.enterprise.gui.coregui.client.UserSessionManager;
import org.rhq.enterprise.gui.coregui.client.ViewPath;
import org.rhq.enterprise.gui.coregui.client.admin.users.UsersDataSource;
import org.rhq.enterprise.gui.coregui.client.components.form.AbstractRecordEditor;
import org.rhq.enterprise.gui.coregui.client.components.form.EnhancedDynamicForm;
import org.rhq.enterprise.gui.coregui.client.components.selector.AssignedItemsChangedEvent;
import org.rhq.enterprise.gui.coregui.client.components.selector.AssignedItemsChangedHandler;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.selection.ResourceGroupSelector;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableTab;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableTabSet;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableVLayout;

/**
 * A form for viewing and/or editing an RHQ {@link Role role}.
 *
 * @author Ian Springer
 */
public class RoleEditView extends AbstractRecordEditor<RolesDataSource> implements BookmarkableView {
    
    private static final String HEADER_ICON = "global/Role_24.png";

    private LocatableTab permissionsTab;
    private PermissionsEditor permissionsEditor;

    private LocatableTab resourceGroupsTab;
    private ResourceGroupSelector resourceGroupSelector;

    private LocatableTab subjectsTab;
    private RoleSubjectSelector subjectSelector;

    private LocatableTab ldapGroupsTab;
    private RoleLdapGroupSelector ldapGroupSelector;

    private boolean hasManageSecurityPermission;
    private boolean isLdapConfigured;
    private boolean isSystemRole;

    public RoleEditView(String locatorId, int roleId) {
        super(locatorId, new RolesDataSource(), roleId, MSG.common_label_role(), HEADER_ICON);
    }

    @Override
    public void renderView(ViewPath viewPath) {
        super.renderView(viewPath);

        this.isSystemRole = RolesDataSource.isSystemRoleId(getRecordId());

        new PermissionsLoader().loadExplicitGlobalPermissions(new PermissionsLoadedListener() {
            @Override
            public void onPermissionsLoaded(Set<Permission> perms) {
                if (perms != null) {
                    RoleEditView.this.hasManageSecurityPermission = perms.contains(Permission.MANAGE_SECURITY);
                    checkIfLdapConfigured();
                }
            }
        });
    }

    @Override
    protected boolean isFormReadOnly() {
        return (isReadOnly() || this.isSystemRole);
    }

    private void checkIfLdapConfigured() {
        GWTServiceLookup.getLdapService().checkLdapConfiguredStatus(new AsyncCallback<Boolean>() {
            public void onSuccess(Boolean isLdapConfigured) {
                RoleEditView.this.isLdapConfigured = isLdapConfigured;
                init();
            }

            public void onFailure(Throwable caught) {
                CoreGUI.getErrorHandler().handleError(MSG.view_adminRoles_failLdap(), caught);
                RoleEditView.this.isLdapConfigured = false;
                init();
            }
        });
    }

    private void init() {
        final boolean isReadOnly = (!this.hasManageSecurityPermission);
        init(isReadOnly);
    }

    @Override
    protected LocatableVLayout buildContentPane() {
        LocatableVLayout contentPane = new LocatableVLayout(extendLocatorId("Content"));
        contentPane.setWidth100();
        contentPane.setHeight100();
        contentPane.setOverflow(Overflow.AUTO);

        EnhancedDynamicForm form = buildForm();
        setForm(form);

        LocatableVLayout topPane = new LocatableVLayout(extendLocatorId("Top"));
        topPane.setWidth100();
        topPane.setHeight(80);
        topPane.addMember(form);

        contentPane.addMember(topPane);

        LocatableTabSet tabSet = new LocatableTabSet(contentPane.extendLocatorId("TabSet"));
        tabSet.setWidth100();
        tabSet.setHeight100();

        // TODO: Also add these tabs if the session subject is a member of the Role being viewed.
        if (this.hasManageSecurityPermission) {
            this.permissionsTab = buildPermissionsTab(tabSet);
            tabSet.addTab(permissionsTab);

            if (!this.isSystemRole) {
                this.resourceGroupsTab = buildResourceGroupsTab(tabSet);
                tabSet.addTab(resourceGroupsTab);
            }

            this.subjectsTab = buildSubjectsTab(tabSet);
            tabSet.addTab(subjectsTab);

            this.ldapGroupsTab = buildLdapGroupsTab(tabSet);
            tabSet.addTab(ldapGroupsTab);
        }

        contentPane.addMember(tabSet);

        return contentPane;
    }

    private LocatableTab buildPermissionsTab(LocatableTabSet tabSet) {
        LocatableTab tab = new LocatableTab(tabSet.extendLocatorId("Permissions"), MSG.common_title_permissions(),
            "global/Locked_16.png");
        // NOTE: We will set the tab content to the permissions editor later once the Role has been fetched.

        return tab;
    }

    private LocatableTab buildResourceGroupsTab(LocatableTabSet tabSet) {
        LocatableTab tab = new LocatableTab(tabSet.extendLocatorId("ResourceGroups"),
            MSG.common_title_resourceGroups(), ImageManager.getGroupIcon(GroupCategory.MIXED));
        // NOTE: We will set the tab content to the resource group selector later once the Role has been fetched.

        return tab;
    }

    private LocatableTab buildSubjectsTab(LocatableTabSet tabSet) {
        LocatableTab tab = new LocatableTab(tabSet.extendLocatorId("Users"), MSG.common_title_users(),
            "global/User_16.png");
        // NOTE: We will set the tab content to the subject selector later once the Role has been fetched.

        return tab;
    }

    private LocatableTab buildLdapGroupsTab(LocatableTabSet tabSet) {
        LocatableTab tab = new LocatableTab(tabSet.extendLocatorId("LdapGroups"), MSG.common_title_ldapGroups(),
            "global/Role_16.png");
        // NOTE: We will set the tab content to the LDAP group selector later once the Role has been fetched.

        return tab;
    }

    @Override
    protected Record createNewRecord() {
        Role role = new Role();
        @SuppressWarnings( { "UnnecessaryLocalVariable" })
        Record roleRecord = RolesDataSource.getInstance().copyValues(role);
        return roleRecord;
    }

    protected void editRecord(Record record) {
        super.editRecord(record);

        // A user can always view their own assigned roles, but only users with MANAGE_SECURITY can view or update
        // other users' assigned roles.
        Subject sessionSubject = UserSessionManager.getSessionSubject();
        int sessionSubjectId = sessionSubject.getId();
        Record[] subjectRecords = record.getAttributeAsRecordArray(RolesDataSource.Field.SUBJECTS);
        boolean isMemberOfRole = false;
        for (Record subjectRecord : subjectRecords) {
            int subjectId = subjectRecord.getAttributeAsInt(RolesDataSource.Field.ID);
            if (subjectId == sessionSubjectId) {
                isMemberOfRole = true;
            }
        }

        if (this.hasManageSecurityPermission || isMemberOfRole) {
            // Create the permission editor and selectors and add them to the corresponding tabs.

            this.permissionsEditor = new PermissionsEditor(this, !hasManageSecurityPermission ||
                this.isSystemRole);
            updateTab(this.permissionsTab, this.permissionsEditor);

            if (!this.isSystemRole) {
                Record[] groupRecords = record.getAttributeAsRecordArray(RolesDataSource.Field.RESOURCE_GROUPS);
                ListGridRecord[] groupListGridRecords = toListGridRecordArray(groupRecords);
                this.resourceGroupSelector = new RoleResourceGroupSelector(this.extendLocatorId("Groups"), groupListGridRecords,
                    !this.hasManageSecurityPermission);
                this.resourceGroupSelector.addAssignedItemsChangedHandler(new AssignedItemsChangedHandler() {
                    public void onSelectionChanged(AssignedItemsChangedEvent event) {
                        onItemChanged();
                    }
                });
                updateTab(this.resourceGroupsTab, this.resourceGroupSelector);
            }

            ListGridRecord[] subjectListGridRecords = toListGridRecordArray(subjectRecords);
            if (getRecordId() == RolesDataSource.ID_SUPERUSER) {
                // If this is the superuser role, make sure the rhqadmin record is disabled, so it cannot be removed
                // from the role, and filter the overlord record out, so users don't even know it exists.
                List<ListGridRecord> filteredSubjectRecords = new ArrayList<ListGridRecord>();
                for (ListGridRecord subjectListGridRecord : subjectListGridRecords) {
                    int subjectId = subjectListGridRecord.getAttributeAsInt(UsersDataSource.Field.ID);
                    if (subjectId == UsersDataSource.ID_RHQADMIN) {
                        subjectListGridRecord.setEnabled(false);
                    }
                    if (subjectId != UsersDataSource.ID_OVERLORD) {
                        filteredSubjectRecords.add(subjectListGridRecord);
                    }
                }
                subjectListGridRecords = filteredSubjectRecords.toArray(new ListGridRecord[filteredSubjectRecords.size()]);
            }
            this.subjectSelector = new RoleSubjectSelector(this.extendLocatorId("Subjects"), subjectListGridRecords,
                !this.hasManageSecurityPermission);
            this.subjectSelector.addAssignedItemsChangedHandler(new AssignedItemsChangedHandler() {
                public void onSelectionChanged(AssignedItemsChangedEvent event) {
                    onItemChanged();
                }
            });
            updateTab(this.subjectsTab, this.subjectSelector);

            if (this.isLdapConfigured) {
                Record[] ldapGroupRecords = record.getAttributeAsRecordArray(RolesDataSource.Field.LDAP_GROUPS);
                ListGridRecord[] ldapGroupListGridRecords = toListGridRecordArray(ldapGroupRecords);
                this.ldapGroupSelector = new RoleLdapGroupSelector(this.extendLocatorId("LdapGroups"), 
                    ldapGroupListGridRecords, !this.hasManageSecurityPermission);
                this.ldapGroupSelector.addAssignedItemsChangedHandler(new AssignedItemsChangedHandler() {
                    public void onSelectionChanged(AssignedItemsChangedEvent event) {
                        onItemChanged();
                    }
                });
                updateTab(this.ldapGroupsTab, this.ldapGroupSelector);
            } else {
                // LDAP is not configured for this RHQ Server - display a message on the LDAP Groups tab informing the
                // user of this along with a link to the System Settings view.
                Label label = new Label("<b>"
                    + MSG.common_msg_emphasizedNotePrefix()
                    + "</b> "
                    + MSG.view_adminRoles_noLdap("href='#Administration/Configuration/SystemSettings'", MSG
                        .view_adminConfig_systemSettings()));
                label.setWidth100();
                label.setHeight(20);
                label.setPadding(5);
                updateTab(this.ldapGroupsTab, label);
            }
        }

        this.permissionsEditor.redraw();
    }

    @Override
    protected List<FormItem> createFormItems(EnhancedDynamicForm form) {
        List<FormItem> items = new ArrayList<FormItem>();

        TextItem nameItem = new TextItem(RolesDataSource.Field.NAME, MSG.common_title_name());
        items.add(nameItem);

        TextItem descriptionItem = new TextItem(RolesDataSource.Field.DESCRIPTION, MSG.common_title_description());
        descriptionItem.setColSpan(form.getNumCols());
        items.add(descriptionItem);

        return items;
    }

    @Override
    protected void save() {
        // Grab the currently assigned sets from each of the selectors and stick them into the corresponding canvas
        // items on the form, so when the form is saved, they'll get submitted along with the rest of the simple fields
        // to the datasource's add or update methods.
        if (this.resourceGroupSelector != null) {
            ListGridRecord[] resourceGroupRecords = this.resourceGroupSelector.getSelectedRecords();
            getForm().setValue(RolesDataSource.Field.RESOURCE_GROUPS, resourceGroupRecords);
        }

        if (this.subjectSelector != null) {
            ListGridRecord[] subjectRecords = this.subjectSelector.getSelectedRecords();
            getForm().setValue(RolesDataSource.Field.SUBJECTS, subjectRecords);
        }

        if (this.ldapGroupSelector != null) {
            ListGridRecord[] ldapGroupRecords = this.ldapGroupSelector.getSelectedRecords();
            getForm().setValue(RolesDataSource.Field.LDAP_GROUPS, ldapGroupRecords);
        }

        // Submit the form values to the datasource.
        super.save();
    }

    @Override
    protected void reset() {
        super.reset();

        if (this.permissionsEditor != null) {
            this.permissionsEditor.reset();
        }

        if (this.resourceGroupSelector != null) {
            this.resourceGroupSelector.reset();
        }
        if (this.subjectSelector != null) {
            this.subjectSelector.reset();
        }
        if (this.ldapGroupSelector != null) {
            this.ldapGroupSelector.reset();
        }
    }

    private static void updateTab(Tab tab, Canvas content) {
        if (tab == null) {
            throw new IllegalStateException("A null tab was specified.");
        }
        tab.getTabSet().updateTab(tab, content);
    }

}
