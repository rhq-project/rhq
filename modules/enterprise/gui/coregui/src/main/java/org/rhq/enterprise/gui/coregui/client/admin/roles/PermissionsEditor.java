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
package org.rhq.enterprise.gui.coregui.client.admin.roles;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;

import com.google.gwt.core.client.JavaScriptObject;
import com.smartgwt.client.core.RefDataClass;
import com.smartgwt.client.data.Record;
import com.smartgwt.client.types.Alignment;
import com.smartgwt.client.types.Autofit;
import com.smartgwt.client.types.ListGridEditEvent;
import com.smartgwt.client.types.ListGridFieldType;
import com.smartgwt.client.util.JSOHelper;
import com.smartgwt.client.widgets.Label;
import com.smartgwt.client.widgets.form.fields.CheckboxItem;
import com.smartgwt.client.widgets.grid.ListGrid;
import com.smartgwt.client.widgets.grid.ListGridField;
import com.smartgwt.client.widgets.grid.ListGridRecord;
import com.smartgwt.client.widgets.grid.events.ChangedEvent;
import com.smartgwt.client.widgets.grid.events.ChangedHandler;
import com.smartgwt.client.widgets.grid.events.RecordClickEvent;
import com.smartgwt.client.widgets.grid.events.RecordClickHandler;
import com.smartgwt.client.widgets.layout.VLayout;

import org.rhq.core.domain.authz.Permission;
import org.rhq.core.domain.authz.Role;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.Messages;
import org.rhq.enterprise.gui.coregui.client.util.message.Message;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableListGrid;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableVStack;

/**
 * An editor for editing the set of RHQ {@link Permission permission}s associated with an RHQ {@link Role role}.
 *
 * @author Ian Springer
 */
public class PermissionsEditor extends LocatableVStack {

    private static Messages MSG = CoreGUI.getMessages();

    private ListGrid globalPermissionsGrid;
    private ListGrid resourcePermissionsGrid;
    private Set<Permission> selectedPermissions;
    private RoleEditView roleEditView;
    private boolean isReadOnly;
    private Object originalValue;

    public PermissionsEditor(RoleEditView roleEditView, boolean isReadOnly) {
        super(roleEditView.extendLocatorId("Permissions"));

        this.roleEditView = roleEditView;
        this.isReadOnly = isReadOnly;
        this.selectedPermissions = new HashSet<Permission>();

        setWidth("95%");
        setHeight100();
        
        VLayout spacer = createVerticalSpacer(13);
        addMember(spacer);

        Label globalPermissionsHeader = new Label("<h4>" + MSG.view_adminRoles_permissions_globalPermissions()
            + "</h4>");
        globalPermissionsHeader.setHeight(17);
        addMember(globalPermissionsHeader);

        this.globalPermissionsGrid = createGlobalPermissionsGrid();
        addMember(this.globalPermissionsGrid);

        spacer = createVerticalSpacer(13);
        addMember(spacer);

        Label resourcePermissionsHeader = new Label("<h4>" + MSG.view_adminRoles_permissions_resourcePermissions()
            + "</h4>");
        resourcePermissionsHeader.setHeight(17);
        addMember(resourcePermissionsHeader);

        this.resourcePermissionsGrid = createResourcePermissionsGrid();
        addMember(this.resourcePermissionsGrid);
    }

    public void reset() {
        //setValue(this.originalValue);
        redraw();
    }

    @Override
    public void redraw() {
        this.selectedPermissions = getValueAsPermissionSet();

        // Update the value of the authorized fields in each row of the grids.

        ListGridRecord[] globalPermissionRecords = this.globalPermissionsGrid.getRecords();
        for (ListGridRecord record : globalPermissionRecords) {
            String permissionName = record.getAttribute("name");
            Permission permission = Permission.valueOf(permissionName);
            record.setAttribute("authorized", this.selectedPermissions.contains(permission));
        }

        ListGridRecord[] resourcePermissionRecords = this.resourcePermissionsGrid.getRecords();
        for (ListGridRecord record : resourcePermissionRecords) {
            String readPermissionName = record.getAttribute("readName");
            Permission readPermission = Permission.valueOf(readPermissionName);
            record.setAttribute("readAuthorized", this.selectedPermissions.contains(readPermission));

            String writePermissionName = record.getAttribute("writeName");
            Permission writePermission = Permission.valueOf(writePermissionName);
            record.setAttribute("writeAuthorized", this.selectedPermissions.contains(writePermission));
        }

        markForRedraw();
    }

    private Set<Permission> getValueAsPermissionSet() {
        Object nativeArray = this.roleEditView.getForm().getValue(RolesDataSource.Field.PERMISSIONS);
        if (this.originalValue == null) {
            this.originalValue = nativeArray;
        }
        ListGridRecord[] permissionRecords = convertToListGridRecordArray((JavaScriptObject)nativeArray);
        return RolesDataSource.toPermissionSet(permissionRecords);
    }

    private ListGrid createGlobalPermissionsGrid() {
        ListGrid grid = createPermissionsGrid("GlobalPermissions");

        // TODO: Add table title.

        ListGridField iconField = createIconField();

        ListGridField displayNameField = new ListGridField("displayName", MSG.common_title_name(), 130);

        ListGridField descriptionField = new ListGridField("description", MSG.common_title_description());
        descriptionField.setWrap(true);

        final ListGridField authorizedField = createAuthorizedField("authorized",
            MSG.view_adminRoles_permissions_isAuthorized(), "name", grid);

        grid.setFields(iconField, displayNameField, authorizedField, descriptionField);

        List<ListGridRecord> records = new ArrayList<ListGridRecord>();
        ListGridRecord record = createGlobalPermissionRecord(MSG.view_adminRoles_permissions_perm_manageSecurity(),
            "global/Locked", Permission.MANAGE_SECURITY,
            MSG.view_adminRoles_permissions_permDesc_manageSecurity());
        records.add(record);
        record = createGlobalPermissionRecord(MSG.view_adminRoles_permissions_perm_manageInventory(),
            "subsystems/inventory/Inventory", Permission.MANAGE_INVENTORY,
            MSG.view_adminRoles_permissions_permDesc_manageInventory());
        records.add(record);
        record = createGlobalPermissionRecord(MSG.view_adminRoles_permissions_perm_manageSettings(),
            "subsystems/configure/Configure", Permission.MANAGE_SETTINGS,
            MSG.view_adminRoles_permissions_permDesc_manageSettings());
        records.add(record);
        record = createGlobalPermissionRecord(MSG.view_adminRoles_permissions_perm_manageBundles(),
            "subsystems/bundle/Bundle", Permission.MANAGE_BUNDLE,
            MSG.view_adminRoles_permissions_permDesc_manageBundles());
        records.add(record);

        grid.setData(records.toArray(new ListGridRecord[records.size()]));

        return grid;
    }

    private ListGrid createResourcePermissionsGrid() {
        ListGrid grid = createPermissionsGrid("ResourcePermissions");
        // TODO: Add table title.

        ListGridField iconField = createIconField();

        ListGridField displayNameField = new ListGridField("displayName", MSG.common_title_name(), 130);

        ListGridField descriptionField = new ListGridField("description", MSG.common_title_description());
        descriptionField.setWrap(true);

        ListGridField readField = createAuthorizedField("readAuthorized", MSG.view_adminRoles_permissions_isRead(),
            "readName", grid);
        ListGridField writeField = createAuthorizedField("writeAuthorized", MSG.view_adminRoles_permissions_isWrite(),
            "writeName", grid);

        grid.setFields(iconField, displayNameField, readField, writeField, descriptionField);

        List<ListGridRecord> records = new ArrayList<ListGridRecord>();

        ListGridRecord record = createResourcePermissionRecord(MSG.view_adminRoles_permissions_perm_inventory(),
            "subsystems/inventory/Inventory",
            Permission.VIEW_RESOURCE,
            MSG.view_adminRoles_permissions_permReadDesc_inventory(),
            Permission.MODIFY_RESOURCE,
            MSG.view_adminRoles_permissions_permWriteDesc_inventory());
        records.add(record);

        record = createResourcePermissionRecord(MSG.view_adminRoles_permissions_perm_manageMeasurements(),
            "subsystems/monitor/Monitor",
            Permission.VIEW_RESOURCE,
            MSG.view_adminRoles_permissions_permReadDesc_manageMeasurements(),
            Permission.MANAGE_MEASUREMENTS,
            MSG.view_adminRoles_permissions_permWriteDesc_manageMeasurements());
        records.add(record);

        record = createResourcePermissionRecord(MSG.view_adminRoles_permissions_perm_manageAlerts(),
            "subsystems/alert/Alerts",
            Permission.VIEW_RESOURCE,
            MSG.view_adminRoles_permissions_permReadDesc_manageAlerts(),
            Permission.MANAGE_ALERTS,
            MSG.view_adminRoles_permissions_permWriteDesc_manageAlerts());
        records.add(record);

        record = createResourcePermissionRecord(MSG.view_adminRoles_permissions_perm_configure(),
            "subsystems/configure/Configure",
            Permission.CONFIGURE_READ,
            MSG.view_adminRoles_permissions_permReadDesc_configure(),
            Permission.CONFIGURE_WRITE,
            MSG.view_adminRoles_permissions_permWriteDesc_configure());
        records.add(record);

        record = createResourcePermissionRecord(MSG.view_adminRoles_permissions_perm_control(),
            "subsystems/control/Operation",
            Permission.VIEW_RESOURCE,
            MSG.view_adminRoles_permissions_permReadDesc_control(),
            Permission.CONTROL,
            MSG.view_adminRoles_permissions_permWriteDesc_control());
        records.add(record);

        record = createResourcePermissionRecord(MSG.view_adminRoles_permissions_perm_manageEvents(),
            "subsystems/event/Events",
            Permission.VIEW_RESOURCE,
            MSG.view_adminRoles_permissions_permReadDesc_manageEvents(),
            Permission.MANAGE_EVENTS,
            MSG.view_adminRoles_permissions_permWriteDesc_manageEvents());
        records.add(record);

        record = createResourcePermissionRecord(MSG.view_adminRoles_permissions_perm_manageContent(),
            "subsystems/content/Content",
            Permission.VIEW_RESOURCE,
            MSG.view_adminRoles_permissions_permReadDesc_manageContent(),
            Permission.MANAGE_CONTENT,
            MSG.view_adminRoles_permissions_permWriteDesc_manageContent());
        records.add(record);

        record = createResourcePermissionRecord(MSG.view_adminRoles_permissions_perm_createChildResources(),
            "subsystems/inventory/Inventory",
            Permission.VIEW_RESOURCE,
            MSG.view_adminRoles_permissions_permReadDesc_createChildResources(),
            Permission.CREATE_CHILD_RESOURCES,
            MSG.view_adminRoles_permissions_permWriteDesc_createChildResources());
        records.add(record);

        record = createResourcePermissionRecord(MSG.view_adminRoles_permissions_perm_deleteChildResources(),
            "subsystems/inventory/Inventory",
            Permission.VIEW_RESOURCE,
            MSG.view_adminRoles_permissions_permReadDesc_deleteChildResources(),
            Permission.DELETE_RESOURCE,
            MSG.view_adminRoles_permissions_permWriteDesc_deleteChildResources());
        records.add(record);

        grid.setData(records.toArray(new ListGridRecord[records.size()]));

        return grid;
    }

    private ListGridField createIconField() {
        ListGridField iconField = new ListGridField("icon", "&nbsp;", 28);
        iconField.setShowDefaultContextMenu(false);
        iconField.setCanSort(false);
        iconField.setAlign(Alignment.CENTER);
        iconField.setType(ListGridFieldType.IMAGE);
        iconField.setImageURLSuffix("_16.png");
        iconField.setImageWidth(16);
        iconField.setImageHeight(16);
        return iconField;
    }

    private LocatableListGrid createPermissionsGrid(String id) {
        LocatableListGrid grid = new LocatableListGrid(extendLocatorId(id));

        grid.setAutoFitData(Autofit.BOTH);
        grid.setWrapCells(true);
        grid.setFixedRecordHeights(false);        

        return grid;
    }
    
    private ListGridField createAuthorizedField(String name, String title, final String nameField, final ListGrid grid) {
        final ListGridField authorizedField = new ListGridField(name, title, 65);

        // Show images rather than true/false.
        authorizedField.setType(ListGridFieldType.IMAGE);
        authorizedField.setImageSize(11);
        LinkedHashMap<String, String> valueMap = new LinkedHashMap<String, String>(2);
        valueMap.put(Boolean.TRUE.toString(), "global/permission_enabled_11.png");
        valueMap.put(Boolean.FALSE.toString(), "global/permission_disabled_11.png");
        authorizedField.setValueMap(valueMap);

        if (!this.isReadOnly) {
            authorizedField.setCanEdit(true);
            grid.setEditEvent(ListGridEditEvent.CLICK);
            CheckboxItem editor = new CheckboxItem();
            authorizedField.setEditorType(editor);
            final Record[] recordBeingEdited = {null};
            authorizedField.addRecordClickHandler(new RecordClickHandler() {
                public void onRecordClick(RecordClickEvent event) {
                    recordBeingEdited[0] = event.getRecord();
                }
            });
            authorizedField.addChangedHandler(new ChangedHandler() {
                public void onChanged(ChangedEvent event) {
                    Boolean authorized = (Boolean)event.getValue();
                    int recordNum = event.getRowNum();
                    ListGridRecord record = grid.getRecord(recordNum);
                    String permissionName = record.getAttribute(nameField);
                    Permission permission = Permission.valueOf(permissionName);
                    if (permission == Permission.VIEW_RESOURCE) {
                        event.getItem().setValue(true);
                        event.getItem().disable();
                        String permissionDisplayName = record.getAttribute("displayName");
                        Message message = new Message(
                            MSG.view_adminRoles_permissions_readAccessImplied(permissionDisplayName),
                            Message.Severity.Warning);
                        CoreGUI.getMessageCenter().notify(message);                                                    
                    } else {
                        updatePermissions(authorized, permission);

                        // Let our parent role editor know the permissions have been changed, so it can update the
                        // enablement of its Save and Reset buttons.
                        org.rhq.enterprise.gui.coregui.client.admin.roles.PermissionsEditor.this.roleEditView.onItemChanged();
                    }
                }
            });            
        }

        return authorizedField;
    }

    private void updatePermissions(Boolean authorized, Permission permission) {
        boolean redrawRequired = false;
        if (authorized) {
            this.selectedPermissions.add(permission);
            if (permission == Permission.MANAGE_SECURITY) {
                // MANAGE_SECURITY implies all other perms.
                this.selectedPermissions.addAll(EnumSet.allOf(Permission.class));
                redrawRequired = true;
            } else if (permission == Permission.MANAGE_INVENTORY) {
                // MANAGE_INVENTORY implies all Resource perms.
                this.selectedPermissions.addAll(Permission.RESOURCE_ALL);
                redrawRequired = true;
            } else if (permission == Permission.CONFIGURE_WRITE) {
                // CONFIGURE_WRITE implies CONFIGURE_READ.
                this.selectedPermissions.add(Permission.CONFIGURE_READ);
                redrawRequired = true;
            }            
        } else {
            this.selectedPermissions.remove(permission);
            if (permission == Permission.CONFIGURE_READ) {
                // Lack of CONFIGURE_READ implies lack of CONFIGURE_WRITE.
                this.selectedPermissions.remove(Permission.CONFIGURE_WRITE);
                redrawRequired = true;
            }
        }
        
        ListGridRecord[] permissionRecords = RolesDataSource.toRecordArray(this.selectedPermissions);
        this.roleEditView.getForm().setValue(RolesDataSource.Field.PERMISSIONS, permissionRecords);

        if (redrawRequired) {
            redraw();
        }
    }

    private ListGridRecord createGlobalPermissionRecord(String displayName, String icon, Permission globalPermission,
                                                        String description) {
        ListGridRecord record = new ListGridRecord();
        record.setAttribute("displayName", displayName);
        record.setAttribute("icon", icon);
        record.setAttribute("name", globalPermission.name());
        record.setAttribute("description", description);
        record.setAttribute("authorized", this.selectedPermissions.contains(globalPermission));

        return record;
    }

    private ListGridRecord createResourcePermissionRecord(String displayName, String icon, Permission readPermission,
                                                           String readDescription, Permission writePermission,
                                                           String writeDescription) {
        ListGridRecord record = new ListGridRecord();
        record.setAttribute("displayName", displayName);
        record.setAttribute("icon", icon);
        record.setAttribute("readName", readPermission.name());
        record.setAttribute("readAuthorized", this.selectedPermissions.contains(readPermission));
        record.setAttribute("description", "<b>" + MSG.view_adminRoles_permissions_read() + "</b> " + readDescription
            + "<br/><b>" + MSG.view_adminRoles_permissions_write() + "</b> " + writeDescription);
        record.setAttribute("writeName", writePermission.name());
        record.setAttribute("writeAuthorized", this.selectedPermissions.contains(writePermission));

        return record;
    }

    public Set<Permission> getPermissions() {
        return this.selectedPermissions;
    }

    private static ListGridRecord[] convertToListGridRecordArray(JavaScriptObject jsObject) {
        if (jsObject == null) {
            return new ListGridRecord[0];
        }
        JavaScriptObject[] jsArray = JSOHelper.toArray(jsObject);
        ListGridRecord[] records = new ListGridRecord[jsArray.length];
        for (int i = 0; i < jsArray.length; i++) {
            JavaScriptObject jsArrayItem = jsArray[i];
            ListGridRecord record = (ListGridRecord) RefDataClass.getRef(jsArrayItem);
            if (record == null) {
                record = new ListGridRecord(jsArrayItem);
            }
            records[i] = record;
        }
        return records;
    }

    private VLayout createVerticalSpacer(int height) {
        VLayout spacer = new VLayout();
        spacer.setHeight(height);
        return spacer;
    }

}
