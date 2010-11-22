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
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;

import com.google.gwt.core.client.JavaScriptObject;
import com.smartgwt.client.core.RefDataClass;
import com.smartgwt.client.data.Record;
import com.smartgwt.client.types.Alignment;
import com.smartgwt.client.types.ListGridEditEvent;
import com.smartgwt.client.types.ListGridFieldType;
import com.smartgwt.client.util.JSOHelper;
import com.smartgwt.client.widgets.Label;
import com.smartgwt.client.widgets.form.fields.CanvasItem;
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
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableVStack;

/**
 * A complex form item for editing the set of RHQ {@link Permission permission}s associated with an RHQ {@link Role
 * role}.
 *
 * @author Ian Springer
 */
// TODO: i18n
public class PermissionsItem extends CanvasItem {

    private static Messages MSG = CoreGUI.getMessages();

    private Set<Permission> selectedPermissions;

    private RoleEditView roleEditView;
    private boolean isReadOnly;

    public PermissionsItem(RoleEditView roleEditView) {
        super(RolesDataSource.Field.PERMISSIONS, "Permissions");

        this.selectedPermissions = new HashSet<Permission>();
        this.roleEditView = roleEditView;
        this.isReadOnly = this.roleEditView.isReadOnly();
    }

    @Override
    protected PermissionsEditor createCanvas() {
        return new PermissionsEditor();
    }

    @Override
    public PermissionsEditor getCanvas() {
        PermissionsEditor permissionsEditor = (PermissionsEditor)super.getCanvas();
        if (permissionsEditor == null) {
            permissionsEditor = createCanvas();
            setCanvas(permissionsEditor);
        }
        return permissionsEditor;
    }

    @Override
    public void redraw() {
        this.selectedPermissions = getValueAsPermissionSet();

        // Update the value of the authorized fields in each row of the grids.

        ListGridRecord[] globalPermissionRecords = getCanvas().getGlobalPermissionsGrid().getRecords();
        for (ListGridRecord record : globalPermissionRecords) {
            String permissionName = record.getAttribute("name");
            Permission permission = Permission.valueOf(permissionName);
            record.setAttribute("authorized", this.selectedPermissions.contains(permission));
        }

        ListGridRecord[] resourcePermissionRecords = getCanvas().getResourcePermissionsGrid().getRecords();
        for (ListGridRecord record : resourcePermissionRecords) {
            String readPermissionName = record.getAttribute("readName");
            Permission readPermission = Permission.valueOf(readPermissionName);
            record.setAttribute("readAuthorized", this.selectedPermissions.contains(readPermission));

            String writePermissionName = record.getAttribute("writeName");
            Permission writePermission = Permission.valueOf(writePermissionName);
            record.setAttribute("writeAuthorized", this.selectedPermissions.contains(writePermission));
        }

        super.redraw();
    }

    private Set<Permission> getValueAsPermissionSet() {
        Object nativeArray = getValue();
        ListGridRecord[] permissionRecords = convertToListGridRecordArray((JavaScriptObject)nativeArray);
        return RolesDataSource.toPermissionSet(permissionRecords);
    }

    private ListGrid createGlobalPermissionsGrid() {
        ListGrid grid = new ListGrid();
        grid.setHeight(115);
        // TODO: Add table title.

        ListGridField iconField = new ListGridField("icon", "&nbsp;", 40);
        iconField.setShowDefaultContextMenu(false);
        iconField.setAlign(Alignment.CENTER);
        iconField.setType(ListGridFieldType.IMAGE);
        iconField.setImageURLSuffix("_16.png");
        iconField.setImageWidth(16);
        iconField.setImageHeight(16);

        ListGridField nameField = new ListGridField("name");
        nameField.setHidden(true);

        ListGridField displayNameField = new ListGridField("displayName", "Name", 120);

        ListGridField descriptionField = new ListGridField("description", "Description", 450);
        descriptionField.setWrap(true);

        final ListGridField authorizedField = createAuthorizedField("authorized", "Authorized?", "name", grid);

        grid.setFields(iconField, nameField, displayNameField, descriptionField, authorizedField);

        List<ListGridRecord> records = new ArrayList<ListGridRecord>();
        ListGridRecord record = createGlobalPermissionRecord("Manage Security", "global/Locked", Permission.MANAGE_SECURITY,
            "can create, update, or delete users and roles (viewing is implied for everyone)");
        records.add(record);
        record = createGlobalPermissionRecord("Manage Inventory", "subsystems/inventory/Inventory", Permission.MANAGE_INVENTORY,
            "has all Resource permissions, as described below, for all Resources; can create, update, and delete groups; " +
            "and can import auto-discovered or manually discovered Resources");
        records.add(record);
        record = createGlobalPermissionRecord("Manage Settings", "subsystems/configure/Configure", Permission.MANAGE_SETTINGS,
            "can modify the RHQ Server configuration and perform any Server-related functionality");
        records.add(record);
        record = createGlobalPermissionRecord("Manage Bundles", "subsystems/bundle/Bundle", Permission.MANAGE_BUNDLE,
            "can create, update, or delete provisioning bundles (viewing is implied for everyone)");
        records.add(record);

        grid.setData(records.toArray(new ListGridRecord[records.size()]));

        return grid;
    }

    private ListGrid createResourcePermissionsGrid() {
        ListGrid grid = new ListGrid();
        grid.setHeight(290);
        // TODO: Add table title.

        ListGridField iconField = new ListGridField("icon", "&nbsp;", 40);
        iconField.setShowDefaultContextMenu(false);
        iconField.setAlign(Alignment.CENTER);
        iconField.setType(ListGridFieldType.IMAGE);
        iconField.setImageURLSuffix("_16.png");
        iconField.setImageWidth(16);
        iconField.setImageHeight(16);

        ListGridField readNameField = new ListGridField("readName");
        readNameField.setHidden(true);

        ListGridField writeNameField = new ListGridField("writeName");
        writeNameField.setHidden(true);

        ListGridField displayNameField = new ListGridField("displayName", "Name", 120);

        ListGridField descriptionField = new ListGridField("description", "Description", 450);
        descriptionField.setWrap(true);

        ListGridField readField = createAuthorizedField("readAuthorized", "Read?", "readName", grid);
        ListGridField writeField = createAuthorizedField("writeAuthorized", "Write?", "writeName", grid);

        grid.setFields(iconField, readNameField, displayNameField, descriptionField, readField, writeField);

        List<ListGridRecord> records = new ArrayList<ListGridRecord>();

        ListGridRecord record = createResourcePermissionRecord("Inventory", "subsystems/inventory/Inventory",
            Permission.VIEW_RESOURCE,
            "(IMPLIED) view Resource properties (name, description, version, etc.), connection settings, and connection settings history",
            Permission.MODIFY_RESOURCE,
            "update Resource name, version, description, and connection settings; delete connection settings history items");
        records.add(record);

        record = createResourcePermissionRecord("Manage Measurements", "subsystems/monitor/Monitor",
            Permission.VIEW_RESOURCE,
            "(IMPLIED) view metric data and collection schedules",
            Permission.MANAGE_MEASUREMENTS,
            "update metric collection schedules");
        records.add(record);

        record = createResourcePermissionRecord("Manage Alerts", "subsystems/alert/Alerts",
            Permission.VIEW_RESOURCE,
            "(IMPLIED) view alert definitions and alert history",
            Permission.MANAGE_ALERTS,
            "create, update, and delete alert definitions; acknowledge and delete alert history items");
        records.add(record);

        record = createResourcePermissionRecord("Configure", "subsystems/configure/Configure",
            Permission.CONFIGURE_READ,
            "view Resource configuration and Resource configuration revision history",
            Permission.CONFIGURE_WRITE,
            "update Resource configuration; delete Resource configuration revision history items");
        records.add(record);

        record = createResourcePermissionRecord("Control", "subsystems/control/Operation",
            Permission.VIEW_RESOURCE,
            "(IMPLIED) view available operations and operation execution history",
            Permission.CONTROL,
            "execute operations; delete operation execution history items");
        records.add(record);

        record = createResourcePermissionRecord("Manage Events", "subsystems/event/Events",
            Permission.VIEW_RESOURCE,
            "(IMPLIED) view events",
            Permission.MANAGE_EVENTS,
            "delete events");
        records.add(record);

        record = createResourcePermissionRecord("Manage Content", "subsystems/content/Content",
            Permission.VIEW_RESOURCE,
            "(IMPLIED) view installed and available packages; view package installation history",
            Permission.MANAGE_CONTENT,
            "subscribe to content sources; install and uninstall packages");
        records.add(record);

        record = createResourcePermissionRecord("Create Child Resources", "subsystems/inventory/Inventory",
            Permission.VIEW_RESOURCE,
            "(IMPLIED) view child creation history",
            Permission.CREATE_CHILD_RESOURCES,
            "create new child Resources (for child Resources of types that are creatable)");
        records.add(record);

        record = createResourcePermissionRecord("Delete Child Resources", "subsystems/inventory/Inventory",
            Permission.VIEW_RESOURCE,
            "(IMPLIED) view child deletion history",
            Permission.DELETE_RESOURCE,
            "delete child Resources (for child Resources of types that are deletable)");
        records.add(record);

        grid.setData(records.toArray(new ListGridRecord[records.size()]));

        return grid;
    }

    private ListGridField createAuthorizedField(String name, String title, final String nameField, final ListGrid grid) {
        final ListGridField authorizedField = new ListGridField(name, title, 65);

        // Show yes/no rather than true/false.
        LinkedHashMap<String, String> valueMap = new LinkedHashMap<String, String>();
        valueMap.put(Boolean.TRUE.toString(), MSG.common_val_yes_lower());
        valueMap.put(Boolean.FALSE.toString(), MSG.common_val_no_lower());        
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
                        Message message = new Message("Read access for the " + permissionDisplayName
                            + " permission is implied and cannot be disabled.", Message.Severity.Warning);
                        CoreGUI.getMessageCenter().notify(message);                                                    
                    } else {
                        updatePermissions(authorized, permission);

                        // Let our parent role editor know the permissions have been changed, so it can update the
                        // enablement of its Save and Reset buttons.
                        PermissionsItem.this.roleEditView.onItemChanged();
                    }
                }
            });            
        }

        return authorizedField;
    }

    private void updatePermissions(Boolean authorized, Permission permission) {
        if (authorized) {
            this.selectedPermissions.add(permission);
        } else {
            this.selectedPermissions.remove(permission);
        }

        ListGridRecord[] permissionRecords = RolesDataSource.toRecordArray(this.selectedPermissions);
        getForm().setValue(getName(), permissionRecords);
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
        record.setAttribute("description", "<b>Read:</b> " + readDescription + "<br/><b>Write:</b> " + writeDescription);
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

    class PermissionsEditor extends LocatableVStack {
        private ListGrid globalPermissionsGrid;
        private ListGrid resourcePermissionsGrid;

        PermissionsEditor() {
            super(roleEditView.extendLocatorId("Permissions"));
            setWidth100();
            setHeight(500);

            VLayout spacer = createVerticalSpacer(13);
            addMember(spacer);

            Label globalPermissionsHeader = new Label("<h4>Global Permissions</h4>");
            globalPermissionsHeader.setHeight(17);
            addMember(globalPermissionsHeader);

            this.globalPermissionsGrid = createGlobalPermissionsGrid();
            addMember(this.globalPermissionsGrid);

            spacer = createVerticalSpacer(13);
            addMember(spacer);

            Label resourcePermissionsHeader = new Label("<h4>Resource Permissions</h4>");
            resourcePermissionsHeader.setHeight(17);            
            addMember(resourcePermissionsHeader);

            this.resourcePermissionsGrid = createResourcePermissionsGrid();
            addMember(this.resourcePermissionsGrid);
        }

        private VLayout createVerticalSpacer(int height) {
            VLayout spacer = new VLayout();
            spacer.setHeight(height);
            return spacer;
        }

        public ListGrid getGlobalPermissionsGrid() {
            return this.globalPermissionsGrid;
        }

        public ListGrid getResourcePermissionsGrid() {
            return this.resourcePermissionsGrid;
        }
    }

}
