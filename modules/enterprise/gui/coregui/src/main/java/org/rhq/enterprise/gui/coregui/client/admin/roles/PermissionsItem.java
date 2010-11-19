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
import java.util.List;
import java.util.Set;

import com.google.gwt.core.client.JavaScriptObject;
import com.smartgwt.client.core.RefDataClass;
import com.smartgwt.client.data.Record;
import com.smartgwt.client.types.Alignment;
import com.smartgwt.client.types.ListGridFieldType;
import com.smartgwt.client.util.JSOHelper;
import com.smartgwt.client.widgets.form.fields.CanvasItem;
import com.smartgwt.client.widgets.grid.ListGrid;
import com.smartgwt.client.widgets.grid.ListGridField;
import com.smartgwt.client.widgets.grid.ListGridRecord;
import com.smartgwt.client.widgets.grid.events.ChangeEvent;
import com.smartgwt.client.widgets.grid.events.ChangeHandler;
import com.smartgwt.client.widgets.grid.events.RecordClickEvent;
import com.smartgwt.client.widgets.grid.events.RecordClickHandler;

import org.rhq.core.domain.authz.Permission;
import org.rhq.core.domain.authz.Role;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.Messages;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableVLayout;

/**
 * A complex form item for editing the set of RHQ {@link Permission permission}s associated with an RHQ
 * {@link Role role}.
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

        // Update the value of the authorized field for each row of the table.
        ListGridRecord[] records = getCanvas().getGlobalPermissionsGrid().getRecords();
        for (ListGridRecord record : records) {
            String permissionName = record.getAttribute("name");
            Permission permission = Permission.valueOf(permissionName);
            record.setAttribute("authorized", this.selectedPermissions.contains(permission));
        }

        super.redraw();
    }

    private Set<Permission> getValueAsPermissionSet() {
        Object nativeArray = getValue();
        ListGridRecord[] permissionRecords = convertToListGridRecordArray((JavaScriptObject)nativeArray);
        return RolesDataSource.toPermissionSet(permissionRecords);
    }

    private ListGrid buildGlobalPermissionsGrid() {
        ListGrid globalPermissionsGrid = new ListGrid();
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

        ListGridField descriptionField = new ListGridField("description", "Description", 370);
        descriptionField.setWrap(true);

        final ListGridField authorizedField = new ListGridField("authorized", "Authorized?", 65);
        authorizedField.setType(ListGridFieldType.BOOLEAN);
        if (!this.isReadOnly) {
            authorizedField.setCanEdit(true);
            final Record[] recordBeingEdited = {null};
            authorizedField.addRecordClickHandler(new RecordClickHandler() {
                public void onRecordClick(RecordClickEvent event) {
                    recordBeingEdited[0] = event.getRecord();
                }
            });
            authorizedField.addChangeHandler(new ChangeHandler() {
                public void onChange(ChangeEvent event) {
                    Boolean authorized = (Boolean)event.getValue();
                    String permissionName = recordBeingEdited[0].getAttribute("name");
                    Permission permission = Permission.valueOf(permissionName);
                    updatePermissions(authorized, permission);

                    // Let our parent role editor know the permissions have been changed, so it can update the
                    // enablement of its Save and Reset buttons.
                    PermissionsItem.this.roleEditView.onItemChanged();
                }
            });
        }

        globalPermissionsGrid.setFields(iconField, nameField, displayNameField, descriptionField, authorizedField);

        List<ListGridRecord> records = new ArrayList<ListGridRecord>();
        ListGridRecord record = createSimplePermissionRecord("Manage Security", "global/Locked", Permission.MANAGE_SECURITY,
            "can create, update, or delete users and roles (viewing is implied for everyone)");
        records.add(record);
        record = createSimplePermissionRecord("Manage Inventory", "subsystems/inventory/Inventory", Permission.MANAGE_INVENTORY,
            "has all Resource permissions, as described below, for all Resources; can create, update, and delete groups; " +
            "and can import auto-discovered or manually discovered Resources");
        records.add(record);
        record = createSimplePermissionRecord("Manage Settings", "subsystems/configure/Configure", Permission.MANAGE_SETTINGS,
            "can modify the RHQ Server configuration and perform any Server-related functionality");
        records.add(record);
        record = createSimplePermissionRecord("Manage Bundles", "subsystems/bundle/Bundle", Permission.MANAGE_BUNDLE,
            "can create, update, or delete provisioning bundles (viewing is implied for everyone)");
        records.add(record);

        globalPermissionsGrid.setData(records.toArray(new ListGridRecord[records.size()]));

        return globalPermissionsGrid;
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

    private ListGridRecord createSimplePermissionRecord(String displayName, String icon, Permission globalPermission,
                                                        String description) {
        ListGridRecord record = new ListGridRecord();
        record.setAttribute("displayName", displayName);
        record.setAttribute("icon", icon);
        record.setAttribute("name", globalPermission.name());
        record.setAttribute("description", description);
        record.setAttribute("authorized", this.selectedPermissions.contains(globalPermission));
        return record;
    }

    private ListGridRecord createReadWritePermissionRecord(String displayName, Permission readPermission,
                                                           String readDescription, Permission writePermission,
                                                           String writeDescription) {
        ListGridRecord record = new ListGridRecord();
        record.setAttribute("displayName", displayName);
        record.setAttribute("readName", readPermission.name());
        record.setAttribute("readAuthorized", this.selectedPermissions.contains(readPermission));
        record.setAttribute("readDescription", readDescription);
        record.setAttribute("writeName", writePermission.name());
        record.setAttribute("writeAuthorized", this.selectedPermissions.contains(writePermission));
        record.setAttribute("writeDescription", writeDescription);
        return record;
    }

    public Set<Permission> getPermissions() {
        return this.selectedPermissions;
    }

    private static ListGridRecord[] convertToListGridRecordArray(JavaScriptObject nativeArray) {
        if (nativeArray == null) {
            return new ListGridRecord[]{};
        }
        JavaScriptObject[] componentsj = JSOHelper.toArray(nativeArray);
        ListGridRecord[] objects = new ListGridRecord[componentsj.length];
        for (int i = 0; i < componentsj.length; i++) {
            JavaScriptObject componentJS = componentsj[i];
            ListGridRecord obj = (ListGridRecord) RefDataClass.getRef(componentJS);
            if (obj == null) obj = new ListGridRecord(componentJS);
            objects[i] = obj;
        }
        return objects;
    }

    class PermissionsEditor extends LocatableVLayout {
        private ListGrid globalPermissionsGrid;

        PermissionsEditor() {
            super(roleEditView.extendLocatorId("Permissions"));
            setWidth100();
            setHeight(350);
            this.globalPermissionsGrid = buildGlobalPermissionsGrid();
            addMember(globalPermissionsGrid);
        }

        public ListGrid getGlobalPermissionsGrid() {
            return globalPermissionsGrid;
        }
    }

}
