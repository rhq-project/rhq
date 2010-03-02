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

import org.rhq.core.domain.authz.Permission;
import org.rhq.core.domain.authz.Role;
import org.rhq.core.domain.resource.group.ResourceGroup;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.gui.coregui.client.components.HeaderLabel;

import com.smartgwt.client.data.Criteria;
import com.smartgwt.client.data.DataSource;
import com.smartgwt.client.data.Record;
import com.smartgwt.client.types.DSOperationType;
import com.smartgwt.client.types.Overflow;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.Label;
import com.smartgwt.client.widgets.form.DynamicForm;
import com.smartgwt.client.widgets.form.events.SubmitValuesEvent;
import com.smartgwt.client.widgets.form.events.SubmitValuesHandler;
import com.smartgwt.client.widgets.form.fields.CanvasItem;
import com.smartgwt.client.widgets.form.fields.FormItem;
import com.smartgwt.client.widgets.form.fields.ResetItem;
import com.smartgwt.client.widgets.form.fields.SectionItem;
import com.smartgwt.client.widgets.form.fields.SubmitItem;
import com.smartgwt.client.widgets.form.fields.TextItem;
import com.smartgwt.client.widgets.form.fields.events.ClickEvent;
import com.smartgwt.client.widgets.form.fields.events.ClickHandler;
import com.smartgwt.client.widgets.grid.ListGridRecord;
import com.smartgwt.client.widgets.layout.VLayout;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

/**
 * @author Greg Hinkle
 */
public class RoleEditView extends VLayout {

    private Role roleBeingEdited;


    private Label message = new Label("Select a role to edit...");


    private VLayout editCanvas;
    private HeaderLabel editLabel;
    private DynamicForm form;
    private PermissionEditorView permissionEditorItem;
    private RoleGroupsEditorItem assignedGroupEditorItem;

    public RoleEditView() {
        super();
        setPadding(10);
        setOverflow(Overflow.AUTO);
    }

    @Override
    protected void onInit() {
        super.onInit();


        addMember(message);

        addMember(buildRoleForm());

        editCanvas.hide();
    }

    private Canvas buildRoleForm() {

        this.editCanvas = new VLayout();

        editLabel = new HeaderLabel("Create User");
        // TODO create header css style and set

        editCanvas.addMember(editLabel);

        form = new DynamicForm();
        form.setDataSource(RolesDataSource.getInstance());

        TextItem idItem = new TextItem("id", "Id");

        TextItem nameItem = new TextItem("name", "Name");

        permissionEditorItem = new PermissionEditorView("permissionEditor", "Permissions");
        permissionEditorItem.setShowTitle(false);
        permissionEditorItem.setColSpan(2);



        assignedGroupEditorItem = new RoleGroupsEditorItem("assignedGroups", "Assigned Groups");
        assignedGroupEditorItem.setShowTitle(false);
        assignedGroupEditorItem.setColSpan(2);

        SubmitItem saveButton = new SubmitItem("save", "Save");

        saveButton.addClickHandler(new ClickHandler() {
            public void onClick(ClickEvent clickEvent) {
                save();
                System.out.println("Save is done");
            }
        });

        form.addSubmitValuesHandler(new SubmitValuesHandler() {
            public void onSubmitValues(SubmitValuesEvent submitValuesEvent) {
                Object o = submitValuesEvent.getValues();
                System.out.println("O: " + o);
            }
        });


        form.setItems(
                idItem,
                nameItem,
                permissionEditorItem,
                assignedGroupEditorItem,
                saveButton, new ResetItem("reset", "Reset"));

        editCanvas.addMember(form);

        return editCanvas;
    }

    public void editRecord(Record record) {
        this.roleBeingEdited = (Role) record.getAttributeAsObject("entity");
        message.hide();
        editCanvas.show();
        try {
            editLabel.setContents("Editing Role " + record.getAttribute("name"));
            form.editRecord(record);
            permissionEditorItem.setParentForm(form);
            permissionEditorItem.setPermissions((Set<Permission>) record.getAttributeAsObject("permissions"));
            assignedGroupEditorItem.setGroups((PageList<ResourceGroup>) record.getAttributeAsObject("assignedGroups"));
        } catch (Throwable t) {
            t.printStackTrace();
        }
        markForRedraw();
    }

    public void editNone() {
        message.show();
        editCanvas.hide();

        markForRedraw();
    }

    public void editNew() {
        ListGridRecord r = new ListGridRecord();
        Role newRole = new Role(null); // todo make default constructor public
        RolesDataSource.copyValues(newRole, r);
        editRecord(r);
        form.setSaveOperationType(DSOperationType.ADD);

//
//        HashSet<Permission> permissions = new HashSet<Permission>();
//        HashMap values = new HashMap();
//        values.put("permssions", permissions);
//
//        form.editNewRecord();


        editLabel.setContents("Create Role");
//        permissionEditorItem.setPermissions(permissions);

//        message.hide();
//        editCanvas.show();
    }



    public void save() {
        System.out.println("Saving role");
        form.saveData();
    }


    public DynamicForm getForm() {
        return form;
    }
}
