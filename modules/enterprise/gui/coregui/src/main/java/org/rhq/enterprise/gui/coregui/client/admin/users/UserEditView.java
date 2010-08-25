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

import java.util.HashSet;
import java.util.Set;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.data.DSCallback;
import com.smartgwt.client.data.DSRequest;
import com.smartgwt.client.data.DSResponse;
import com.smartgwt.client.data.Record;
import com.smartgwt.client.types.Alignment;
import com.smartgwt.client.types.DSOperationType;
import com.smartgwt.client.types.TitleOrientation;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.IButton;
import com.smartgwt.client.widgets.Label;
import com.smartgwt.client.widgets.Window;
import com.smartgwt.client.widgets.form.DynamicForm;
import com.smartgwt.client.widgets.form.fields.CanvasItem;
import com.smartgwt.client.widgets.form.fields.SectionItem;
import com.smartgwt.client.widgets.form.fields.TextItem;
import com.smartgwt.client.widgets.grid.ListGridRecord;
import com.smartgwt.client.widgets.layout.HLayout;
import com.smartgwt.client.widgets.layout.VLayout;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.authz.Role;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.components.HeaderLabel;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.util.message.Message;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableVLayout;

/**
 * @author Greg Hinkle
 */
public class UserEditView extends LocatableVLayout {

    private Label message = new Label("Select a user to edit...");

    //    private SubjectRolesEditorItem subjectRolesEditorItem ;

    private VLayout editCanvas;
    private HeaderLabel editLabel;
    private DynamicForm form;

    CanvasItem roleSelectionItem;

    private UsersDataSource dataSource;

    private Subject subject;

    private Window editorWindow;
    private SubjectRoleSelector roleSelector;

    public UserEditView(String locatorId) {
        super(locatorId);

        dataSource = UsersDataSource.getInstance();

        setWidth100();
        setHeight100();

        buildSubjectEditor();
        editCanvas.hide();

        addMember(message);
        addMember(editCanvas);

    }

    private Canvas buildSubjectEditor() {
        form = new DynamicForm();
        form.setWidth100();

        form.setHiliteRequiredFields(true);
        form.setRequiredTitleSuffix("* :");

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

        form.setUseAllDataSourceFields(true);
        form.setDataSource(dataSource);

        this.roleSelectionItem = new CanvasItem("selectRoles", "Select Roles");
        this.roleSelectionItem.setTitleOrientation(TitleOrientation.TOP);
        this.roleSelectionItem.setColSpan(2);
        //        roleSelectionItem.setCanvas(new SubjectRoleSelector(null));

        TextItem departmentItem = new TextItem("department");
        departmentItem.setRequired(false);

        IButton saveButton = new IButton("Save");
        saveButton.addClickHandler(new com.smartgwt.client.widgets.events.ClickHandler() {
            public void onClick(com.smartgwt.client.widgets.events.ClickEvent clickEvent) {
                if (form.validate()) {
                    save();
                    if (editorWindow != null) {
                        editorWindow.destroy();
                        CoreGUI.refresh();
                    }
                }
            }
        });

        IButton resetButton = new IButton("Reset");
        resetButton.addClickHandler(new com.smartgwt.client.widgets.events.ClickHandler() {
            public void onClick(com.smartgwt.client.widgets.events.ClickEvent clickEvent) {
                form.reset();
            }
        });

        IButton cancelButton = new IButton("Cancel");
        cancelButton.addClickHandler(new com.smartgwt.client.widgets.events.ClickHandler() {
            public void onClick(com.smartgwt.client.widgets.events.ClickEvent clickEvent) {
                if (editorWindow != null) {
                    editorWindow.destroy();
                } else {
                    form.reset();
                }
            }
        });

        HLayout buttonLayout = new HLayout(10);
        buttonLayout.setAlign(Alignment.CENTER);
        buttonLayout.addMember(saveButton);
        buttonLayout.addMember(resetButton);
        buttonLayout.addMember(cancelButton);

        form.setItems(departmentItem, roleSelectionItem);

        editCanvas = new VLayout();

        editCanvas.addMember(form);
        editCanvas.addMember(buttonLayout);

        return editCanvas;

    }

    private void save() {
        final HashSet<Integer> roles = roleSelector.getSelection();
        form.saveData(new DSCallback() {
            public void execute(DSResponse dsResponse, Object o, DSRequest dsRequest) {

                int subjectId = Integer.parseInt(new ListGridRecord(dsRequest.getData()).getAttribute("id"));

                int[] roleIds = new int[roles.size()];
                int i = 0;
                for (Integer id : roles) {
                    roleIds[i++] = id;
                }

                GWTServiceLookup.getRoleService().setAssignedSubjectRoles(subjectId, roleIds,
                    new AsyncCallback<Void>() {
                        public void onFailure(Throwable caught) {
                            CoreGUI.getErrorHandler().handleError("Failed to set subject role assignments.", caught);
                        }

                        public void onSuccess(Void result) {
                            CoreGUI.getMessageCenter().notify(
                                new Message("Succesfully saved new user roles.", Message.Severity.Info));
                        }
                    });

            }
        });
    }

    public void editRecord(Record record) {

        //        form.getDataSource().getField("username").setCanEdit(true );

        roleSelector = new SubjectRoleSelector("UserEditor-Roles", (Set<Role>) record.getAttributeAsObject("roles"));
        roleSelectionItem.setCanvas(roleSelector);

        try {
            form.editRecord(record);
        } catch (Throwable t) {
            t.printStackTrace();
        }
        message.hide();
        editCanvas.show();
        form.setSaveOperationType(DSOperationType.UPDATE);

        markForRedraw();
    }

    public void editNone() {
        message.show();
        editCanvas.hide();
        markForRedraw();
    }

    public void editNewInternal() {
        subject = new Subject();
        ListGridRecord r = dataSource.copyValues(subject);
        editRecord(r);
        //        form.getDataSource().getField("username").setCanEdit(false);
        form.setSaveOperationType(DSOperationType.ADD);

        editorWindow = new Window();
        editorWindow.setTitle("Create User");
        editorWindow.setWidth(800);
        editorWindow.setHeight(800);
        editorWindow.setIsModal(true);
        editorWindow.setShowModalMask(true);
        editorWindow.setCanDragResize(true);
        editorWindow.centerInPage();
        editorWindow.addItem(this);
        editorWindow.show();

    }
}
