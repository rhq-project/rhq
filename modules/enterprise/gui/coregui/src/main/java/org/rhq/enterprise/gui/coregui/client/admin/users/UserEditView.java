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

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.authz.Role;
import org.rhq.enterprise.gui.coregui.client.components.HeaderLabel;

import com.smartgwt.client.data.Record;
import com.smartgwt.client.types.DSOperationType;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.Label;
import com.smartgwt.client.widgets.form.DynamicForm;
import com.smartgwt.client.widgets.form.fields.ResetItem;
import com.smartgwt.client.widgets.form.fields.SectionItem;
import com.smartgwt.client.widgets.form.fields.SubmitItem;
import com.smartgwt.client.widgets.form.fields.TextItem;
import com.smartgwt.client.widgets.form.fields.events.ClickEvent;
import com.smartgwt.client.widgets.form.fields.events.ClickHandler;
import com.smartgwt.client.widgets.grid.ListGridRecord;
import com.smartgwt.client.widgets.layout.VLayout;

import java.util.Set;

/**
 * @author Greg Hinkle
 */
public class UserEditView extends VLayout {


    private Label message = new Label("Select a user to edit...");

    private SubjectRolesEditorItem subjectRolesEditorItem ;

    private VLayout editCanvas;
    private HeaderLabel editLabel;
    private DynamicForm form;

    private UsersDataSource dataSource;

    private Subject subject;

    @Override
    protected void onInit() {
        super.onInit();

        setWidth100();
        setHeight100();

        buildSubjectEditor();
        form.hide();

        addMember(message);
        addMember(form);

    }

    private Canvas buildSubjectEditor() {
        form = new DynamicForm();
        form.setWidth100();

        SectionItem userEditSection = new SectionItem("userEditSection", "Edit User");


//        TextItem firstName = new TextItem("firstName", "First Name");
//
//        TextItem lastName = new TextItem("lastName", "Last Name");
//
//        TextItem email = new TextItem("email", "Email Address");
//
//
//        BooleanItem enabled = new BooleanItem();
//        enabled.setName("enabled");
//        enabled.setTitle("Enabled");
//
//        TextItem username = new TextItem("username", "Username");
//
//        TextItem phone = new TextItem("phone", "Phone");


//        form.setField//s(userEditSection);


        dataSource = UsersDataSource.getInstance();

        form.setUseAllDataSourceFields(true);
        form.setDataSource(dataSource);


        subjectRolesEditorItem = new SubjectRolesEditorItem("rolesEditor","Assigned Roles");


        SubmitItem saveButton = new SubmitItem("save", "Save");

        saveButton.addClickHandler(new ClickHandler() {
            public void onClick(ClickEvent clickEvent) {
                form.saveData();
                System.out.println("Save is done");
            }
        });

        TextItem departmentItem = new TextItem("department");

        ResetItem resetButton = new ResetItem("reset", "Reset");

        form.setItems(departmentItem, subjectRolesEditorItem, saveButton, resetButton);

        return form;


    }


    public void editRecord(Record record) {
        form.getDataSource().getField("username").setCanEdit(false);

        subjectRolesEditorItem.setSubject((Subject) record.getAttributeAsObject("entity"));
        subjectRolesEditorItem.setRoles((Set<Role>) record.getAttributeAsObject("roles"));

        try {
            form.editRecord(record);
        } catch (Throwable t) {
            t.printStackTrace();
        }
        message.hide();
        form.show();
        form.setSaveOperationType(DSOperationType.UPDATE);

        markForRedraw();
    }

    public void editNone() {
        message.show();
        form.hide();
        markForRedraw();
    }

    public void editNew() {
        form.getDataSource().getField("username").setCanEdit(true);

        ListGridRecord r = dataSource.copyValues(subject);
        editRecord(r);
        form.setSaveOperationType(DSOperationType.ADD);

    }


}
