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

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.data.DSCallback;
import com.smartgwt.client.data.DSRequest;
import com.smartgwt.client.data.DSResponse;
import com.smartgwt.client.data.Record;
import com.smartgwt.client.types.DSOperationType;
import com.smartgwt.client.types.Overflow;
import com.smartgwt.client.types.TitleOrientation;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.Label;
import com.smartgwt.client.widgets.Window;
import com.smartgwt.client.widgets.form.DynamicForm;
import com.smartgwt.client.widgets.form.events.SubmitValuesEvent;
import com.smartgwt.client.widgets.form.events.SubmitValuesHandler;
import com.smartgwt.client.widgets.form.fields.CanvasItem;
import com.smartgwt.client.widgets.form.fields.ResetItem;
import com.smartgwt.client.widgets.form.fields.SubmitItem;
import com.smartgwt.client.widgets.form.fields.TextItem;
import com.smartgwt.client.widgets.form.fields.events.ClickEvent;
import com.smartgwt.client.widgets.form.fields.events.ClickHandler;
import com.smartgwt.client.widgets.grid.ListGridRecord;
import com.smartgwt.client.widgets.layout.VLayout;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.authz.Permission;
import org.rhq.core.domain.authz.Role;
import org.rhq.core.domain.resource.group.ResourceGroup;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.components.HeaderLabel;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.selection.ResourceGroupSelector;
import org.rhq.enterprise.gui.coregui.client.util.message.Message;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableDynamicForm;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableVLayout;

/**
 * @author Greg Hinkle
 */
public class RoleEditView extends LocatableVLayout {

    private Role roleBeingEdited;

    private Label message = new Label("Select a role to edit...");

    private VLayout editCanvas;
    private HeaderLabel editLabel;
    private DynamicForm form;
    private PermissionEditorView permissionEditorItem;

    private CanvasItem groupSelectorItem;
    private ResourceGroupSelector groupSelector;

    private CanvasItem subjectSelectorItem;
    private RoleSubjectSelector subjectSelector;

    private Window editorWindow;

    public RoleEditView(String locatorId) {
        super(locatorId);
        setPadding(10);
        setOverflow(Overflow.AUTO);

        addMember(message);

        addMember(buildRoleForm());

        editCanvas.hide();
    }

    private Canvas buildRoleForm() {

        this.editCanvas = new VLayout();
        this.editCanvas.setWidth100();
        this.editCanvas.setHeight100();

        editLabel = new HeaderLabel("Create User");
        // TODO create header css style and set

        editCanvas.addMember(editLabel);

        form = new LocatableDynamicForm(extendLocatorId("RoleForm"));
        form.setWidth100();

        form.setDataSource(RolesDataSource.getInstance());

        TextItem idItem = new TextItem("id", "Id");

        TextItem nameItem = new TextItem("name", "Name");

        permissionEditorItem = new PermissionEditorView("permissionEditor", "Permissions");
        permissionEditorItem.setShowTitle(false);
        permissionEditorItem.setColSpan(2);

        groupSelectorItem = new CanvasItem("groupSelectionCanvas", "Assigned Resource Groups");
        groupSelectorItem.setCanvas(new Label("loading...")); //new RoleResourceGroupSelector(null));
        groupSelectorItem.setTitleOrientation(TitleOrientation.TOP);
        groupSelectorItem.setColSpan(2);

        subjectSelectorItem = new CanvasItem("subjectSelectionCanvas", "Assigned Subjects");
        subjectSelectorItem.setCanvas(new Label("loading...")); //new RoleSubjectSelector(null));
        subjectSelectorItem.setTitleOrientation(TitleOrientation.TOP);
        subjectSelectorItem.setColSpan(2);

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

        form.setItems(idItem, nameItem, permissionEditorItem, groupSelectorItem, subjectSelectorItem, saveButton,
            new ResetItem("reset", "Reset"));

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

            groupSelector = new RoleResourceGroupSelector("RoleEditor-Groups", (Collection<ResourceGroup>) record
                .getAttributeAsObject("resourceGroups"));
            groupSelectorItem.setCanvas(groupSelector);

            subjectSelector = new RoleSubjectSelector("RoleEditor-Subjects", (Collection<Subject>) record
                .getAttributeAsObject("subjects"));
            subjectSelectorItem.setCanvas(subjectSelector);

        } catch (Throwable t) {
            t.printStackTrace();
        }
        //        markForRedraw();
    }

    public void editNone() {
        message.show();
        editCanvas.hide();

        //        markForRedraw();
    }

    public void editNew() {
        ListGridRecord r = RolesDataSource.getInstance().copyValues(new Role());
        editRecord(r);
        form.setSaveOperationType(DSOperationType.ADD);

        editLabel.setContents("Create Role");

        editorWindow = new Window();
        editorWindow.setTitle("Create Role");
        editorWindow.setWidth(800);
        editorWindow.setHeight(800);
        editorWindow.setIsModal(true);
        editorWindow.setShowModalMask(true);
        editorWindow.setCanDragResize(true);
        editorWindow.centerInPage();
        editorWindow.addItem(this);
        editorWindow.show();

    }

    public void save() {

        System.out.println("Saving role");
        form.saveData(new DSCallback() {
            public void execute(DSResponse dsResponse, Object o, DSRequest dsRequest) {
                HashSet<Integer> selection = groupSelector.getSelection();
                int[] groupIds = new int[selection.size()];
                int i = 0;
                for (Integer id : selection) {
                    groupIds[i++] = id;
                }

                int roleId;
                if (roleBeingEdited != null && roleBeingEdited.getId() != null) {
                    roleId = roleBeingEdited.getId();
                } else {
                    // new role
                    roleId = Integer.parseInt(new ListGridRecord(dsRequest.getData()).getAttribute("id"));
                }

                GWTServiceLookup.getRoleService().setAssignedResourceGroups(roleId, groupIds,
                    new AsyncCallback<Void>() {
                        public void onFailure(Throwable caught) {
                            CoreGUI.getErrorHandler().handleError("Failed to update role's assigned groups", caught);
                        }

                        public void onSuccess(Void result) {
                            CoreGUI.getMessageCenter().notify(
                                new Message("Updated assigned groups", Message.Severity.Info));
                        }
                    });

                HashSet<Integer> selectedSubjects = subjectSelector.getSelection();
                int[] subjectIds = new int[selectedSubjects.size()];
                i = 0;
                for (Integer id : selectedSubjects) {
                    subjectIds[i++] = id;
                }

                GWTServiceLookup.getRoleService().setAssignedSubjects(roleId, subjectIds, new AsyncCallback<Void>() {
                    public void onFailure(Throwable caught) {
                        CoreGUI.getErrorHandler().handleError("Failed to update role's assigned subjects", caught);
                    }

                    public void onSuccess(Void result) {
                        CoreGUI.getMessageCenter().notify(
                            new Message("Updated role assigned subjects", Message.Severity.Info));
                    }
                });

                if (editorWindow != null) {
                    editorWindow.destroy();
                }
            }
        });
    }

    public DynamicForm getForm() {
        return form;
    }
}
