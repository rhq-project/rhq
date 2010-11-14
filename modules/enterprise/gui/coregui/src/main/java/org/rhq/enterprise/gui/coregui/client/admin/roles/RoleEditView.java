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
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.data.Record;
import com.smartgwt.client.types.TitleOrientation;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.Label;
import com.smartgwt.client.widgets.form.fields.CanvasItem;
import com.smartgwt.client.widgets.form.fields.FormItem;
import com.smartgwt.client.widgets.form.fields.TextItem;
import com.smartgwt.client.widgets.grid.ListGridRecord;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.authz.Permission;
import org.rhq.core.domain.authz.Role;
import org.rhq.core.domain.resource.group.LdapGroup;
import org.rhq.enterprise.gui.coregui.client.BookmarkableView;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.UserSessionManager;
import org.rhq.enterprise.gui.coregui.client.ViewPath;
import org.rhq.enterprise.gui.coregui.client.components.form.AbstractRecordEditor;
import org.rhq.enterprise.gui.coregui.client.components.selector.AssignedItemsChangedEvent;
import org.rhq.enterprise.gui.coregui.client.components.selector.AssignedItemsChangedHandler;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.selection.ResourceGroupSelector;

/**
 * A form for viewing and/or editing an RHQ {@link Role role}.
 *
 * @author Ian Springer
 */
public class RoleEditView extends AbstractRecordEditor<RolesDataSource> implements BookmarkableView {

    private static final String DATA_TYPE_NAME = "role";
    private static final String HEADER_ICON = "global/Role_16.png";

    private PermissionEditorView permissionEditorItem;

    private CanvasItem resourceGroupsItem;
    private ResourceGroupSelector groupSelector;

    private CanvasItem subjectsItem;
    private RoleSubjectSelector subjectSelector;

    private CanvasItem ldapGroupsItem;
    private RoleLdapGroupSelector ldapGroupSelector;

    private boolean hasManageSecurityPermission;
    private boolean isLdapConfigured;

    public RoleEditView(String locatorId, int roleId) {
        super(locatorId, new RolesDataSource(), roleId, DATA_TYPE_NAME, HEADER_ICON);
    }

    @Override
    public void renderView(ViewPath viewPath) {
        GWTServiceLookup.getAuthorizationService().getExplicitGlobalPermissions(new AsyncCallback<Set<Permission>>() {
            public void onFailure(Throwable caught) {
                CoreGUI.getErrorHandler().handleError("Could not determine your global permissions - assuming none.",
                    caught);
                RoleEditView.this.hasManageSecurityPermission = false;
                checkIfLdapConfigured();
            }

            public void onSuccess(Set<Permission> globalPermissions) {
                RoleEditView.this.hasManageSecurityPermission = globalPermissions.contains(Permission.MANAGE_SECURITY);
                checkIfLdapConfigured();
            }
        });
    }

    private void checkIfLdapConfigured() {
        GWTServiceLookup.getLdapService().checkLdapConfiguredStatus(new AsyncCallback<Boolean>() {
            public void onSuccess(Boolean isLdapConfigured) {
                RoleEditView.this.isLdapConfigured = isLdapConfigured;
                fetchAvailableLdapGroups();
            }

            public void onFailure(Throwable caught) {
                CoreGUI.getErrorHandler().handleError("Failed to determine if LDAP configured - assuming not.", caught);
                RoleEditView.this.isLdapConfigured = false;
                fetchAvailableLdapGroups();
            }
        });
    }

    private void fetchAvailableLdapGroups() {
        final boolean isReadOnly = (!this.hasManageSecurityPermission);
        if (this.isLdapConfigured) {
            final Set<LdapGroup> availableLdapGroups = null;
            GWTServiceLookup.getLdapService().findAvailableGroups(
                new AsyncCallback<Set<Map<String, String>>>() {
                    public void onFailure(Throwable caught) {
                        CoreGUI.getErrorHandler().handleError("Failed to retrieve available LDAP groups - assuming none.",
                            caught);
                        Set<LdapGroup> availableLdapGroups = Collections.emptySet();
                        init(isReadOnly);
                    }

                    public void onSuccess(Set<Map<String, String>> result) {
                        // Get assigned LDAP groups.
                        Set<LdapGroup> availableLdapGroups = RoleLdapGroupSelector.convertToCollection(result);
                        // Update record with both objects.
                        //record.setAttribute("ldapGroupsAvailable", availableGroups);
                        init(isReadOnly);
                    }
                });
        } else {
            // LDAP not configured - we won't display an LDAP group selector.
            // TODO: Should we display a message stating that LDAP is not configured with a link to Admin Settings view?
            Set<LdapGroup> availableLdapGroups = Collections.emptySet();
            init(isReadOnly);
        }
    }

    protected Record createNewRecord() {
        Role newRole = new Role();
        return getDataSource().copyValues(newRole);
    }

    @Override
    protected void editRecord(Record record) {        
        // A user can always view their own assigned roles, but only users with MANAGE_SECURITY can view or update
        // other users' assigned roles.
        Subject whoami = UserSessionManager.getSessionSubject();
        // TODO: Make call to Server to see if logged in user is a member of this role.

        if (this.hasManageSecurityPermission) {
            // Create the selectors and add them to the corresponding canvas items on the form.

            Record[] groupRecords = record.getAttributeAsRecordArray(RolesDataSource.Field.RESOURCE_GROUPS);
            ListGridRecord[] groupListGridRecords = toListGridRecordArray(groupRecords);
            //boolean areGroupsReadOnly = areGroupsReadOnly(record); // TODO
            this.groupSelector = new RoleResourceGroupSelector(this.extendLocatorId("Groups"), groupListGridRecords);
            this.groupSelector.addAssignedItemsChangedHandler(new AssignedItemsChangedHandler() {
                public void onSelectionChanged(AssignedItemsChangedEvent event) {
                    onItemChanged();
                }
            });
            this.resourceGroupsItem.setCanvas(this.groupSelector);

            Record[] subjectRecords = record.getAttributeAsRecordArray(RolesDataSource.Field.SUBJECTS);
            ListGridRecord[] subjectListGridRecords = toListGridRecordArray(subjectRecords);
            //boolean areSubjectsReadOnly = areSubjectsReadOnly(record); // TODO
            this.subjectSelector = new RoleSubjectSelector(this.extendLocatorId("Subjects"), subjectListGridRecords);
            this.subjectSelector.addAssignedItemsChangedHandler(new AssignedItemsChangedHandler() {
                public void onSelectionChanged(AssignedItemsChangedEvent event) {
                    onItemChanged();
                }
            });
            this.subjectsItem.setCanvas(this.subjectSelector);

            if (this.isLdapConfigured) {
                Record[] ldapGroupRecords = record.getAttributeAsRecordArray(RolesDataSource.Field.LDAP_GROUPS);
                ListGridRecord[] ldapGroupListGridRecords = toListGridRecordArray(ldapGroupRecords);
                Integer roleId = record.getAttributeAsInt(RolesDataSource.Field.ID);
                //boolean areLdapGroupsReadOnly = areLdapGroupsReadOnly(record); // TODO
                this.ldapGroupSelector = new RoleLdapGroupSelector(this.extendLocatorId("LdapGroups"), roleId);
                this.ldapGroupSelector.addAssignedItemsChangedHandler(new AssignedItemsChangedHandler() {
                    public void onSelectionChanged(AssignedItemsChangedEvent event) {
                        onItemChanged();
                    }
                });
                this.ldapGroupsItem.setCanvas(this.ldapGroupSelector);
            } else {
                Label label = new Label("<b>NOTE:</b> The LDAP security integration is not configured. To configure " +
                                        "it, go to <a href='#Administration/Configuration/SystemSettings'>System " +
                                        "Settings</a>.");
                label.setWidth100();
                label.setPadding(6);
                this.ldapGroupsItem.setCanvas(label);
            }

            // TODO: Fix this.
            //Set<Permission> permissions = (Set<Permission>) record.getAttributeAsObject(RolesDataSource.Field.PERMISSIONS);
            Set<Permission> permissions = Collections.emptySet();
            this.permissionEditorItem.setPermissions(permissions);
        }

        super.editRecord(record);
    }

    @Override
    protected List<FormItem> createFormItems(boolean newUser) {
        List<FormItem> items = new ArrayList<FormItem>();

        TextItem nameItem = new TextItem(RolesDataSource.Field.NAME, "Name");
        items.add(nameItem);

        TextItem descriptionItem = new TextItem(RolesDataSource.Field.DESCRIPTION, "Description");
        descriptionItem.setColSpan(getForm().getNumCols());
        items.add(descriptionItem);

        permissionEditorItem = new PermissionEditorView(this.getLocatorId(), "permissionsEditor", "Permissions", this);
        permissionEditorItem.setShowTitle(false);
        permissionEditorItem.setColSpan(getForm().getNumCols());
        items.add(permissionEditorItem);

        resourceGroupsItem = new CanvasItem(RolesDataSource.Field.RESOURCE_GROUPS, "Assigned Resource Groups");
        resourceGroupsItem.setShowTitle(false);
        resourceGroupsItem.setTitleOrientation(TitleOrientation.TOP);
        resourceGroupsItem.setColSpan(getForm().getNumCols());
        resourceGroupsItem.setCanvas(new Canvas());
        items.add(resourceGroupsItem);

        subjectsItem = new CanvasItem(RolesDataSource.Field.SUBJECTS, "Assigned Subjects");
        subjectsItem.setShowTitle(false);
        subjectsItem.setTitleOrientation(TitleOrientation.TOP);
        subjectsItem.setColSpan(getForm().getNumCols());
        subjectsItem.setCanvas(new Canvas());
        items.add(subjectsItem);

        ldapGroupsItem = new CanvasItem(RolesDataSource.Field.LDAP_GROUPS, "LDAP Groups");
        ldapGroupsItem.setShowTitle(false);
        ldapGroupsItem.setTitleOrientation(TitleOrientation.TOP);
        ldapGroupsItem.setColSpan(getForm().getNumCols());
        ldapGroupsItem.setCanvas(new Canvas());
        items.add(ldapGroupsItem);

        return items;
    }

    @Override
    protected void save() {
        // Grab the currently assigned sets from each of the selectors and stick them into the corresponding canvas
        // items on the form, so when the form is saved, they'll get submitted along with the rest of the simple fields
        // to the datasource's add or update methods .
        ListGridRecord[] groupRecords = this.groupSelector.getAssignedGrid().getSelection();
        getForm().setValue(RolesDataSource.Field.RESOURCE_GROUPS, groupRecords);

        ListGridRecord[] subjectRecords = this.subjectSelector.getAssignedGrid().getSelection();
        getForm().setValue(RolesDataSource.Field.SUBJECTS, subjectRecords);

        ListGridRecord[] ldapGroupRecords = this.ldapGroupSelector.getAssignedGrid().getSelection();
        getForm().setValue(RolesDataSource.Field.LDAP_GROUPS, ldapGroupRecords);

        // Submit the form values to the datasource.
        super.save();
    }

    @Override
    protected void reset() {
        super.reset();
        this.groupSelector.reset();
        this.subjectSelector.reset();
        this.ldapGroupSelector.reset();
    }
}
