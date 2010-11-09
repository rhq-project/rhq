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

import java.util.Set;

import com.google.gwt.user.client.History;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.data.DSCallback;
import com.smartgwt.client.data.DSRequest;
import com.smartgwt.client.data.DSResponse;
import com.smartgwt.client.data.Record;
import com.smartgwt.client.types.Alignment;
import com.smartgwt.client.types.DSOperationType;
import com.smartgwt.client.types.Overflow;
import com.smartgwt.client.types.TitleOrientation;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.IButton;
import com.smartgwt.client.widgets.Label;
import com.smartgwt.client.widgets.form.DynamicForm;
import com.smartgwt.client.widgets.form.events.ItemChangedEvent;
import com.smartgwt.client.widgets.form.events.ItemChangedHandler;
import com.smartgwt.client.widgets.form.fields.CanvasItem;
import com.smartgwt.client.widgets.grid.ListGridRecord;
import com.smartgwt.client.widgets.layout.HLayout;
import com.smartgwt.client.widgets.layout.VLayout;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.authz.Role;
import org.rhq.core.domain.criteria.SubjectCriteria;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.gui.coregui.client.BookmarkableView;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.ViewId;
import org.rhq.enterprise.gui.coregui.client.ViewPath;
import org.rhq.enterprise.gui.coregui.client.components.form.EnhancedDynamicForm;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.util.message.Message;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableIButton;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableVLayout;

/**
 * @author Greg Hinkle
 * @author Ian Springer
 */
public class UserEditView extends LocatableVLayout implements BookmarkableView {

    private Label message = new Label("Loading...");

    private VLayout editCanvas;
    private DynamicForm form;

    private UsersDataSource dataSource;

    private CanvasItem roleSelectionItem;
    private SubjectRoleSelector roleSelector;

    public UserEditView(String locatorId) {
        super(locatorId);

        dataSource = UsersDataSource.getInstance();

        setOverflow(Overflow.AUTO);

        buildSubjectEditor();
        editCanvas.hide();

        addMember(message);
        addMember(editCanvas);
    }

    private Canvas buildSubjectEditor() {
        form = new EnhancedDynamicForm(this.getLocatorId());
        form.setDataSource(dataSource);
        form.setUseAllDataSourceFields(true);

        this.roleSelectionItem = new CanvasItem("selectRoles", "Assigned Roles");
        this.roleSelectionItem.setCanvas(new Canvas());
        this.roleSelectionItem.setTitleOrientation(TitleOrientation.TOP);
        this.roleSelectionItem.setColSpan(form.getNumCols());

        final IButton saveButton = new LocatableIButton(this.extendLocatorId("Save"), "Save");
        saveButton.setDisabled(true);
        saveButton.addClickHandler(new com.smartgwt.client.widgets.events.ClickHandler() {
            public void onClick(com.smartgwt.client.widgets.events.ClickEvent clickEvent) {
                save();
            }
        });

        final IButton resetButton = new LocatableIButton(this.extendLocatorId("Reset"), "Reset");
        resetButton.setDisabled(true);
        resetButton.addClickHandler(new com.smartgwt.client.widgets.events.ClickHandler() {
            public void onClick(com.smartgwt.client.widgets.events.ClickEvent clickEvent) {
                form.reset();
                resetButton.disable();
            }
        });

        form.addItemChangedHandler(new ItemChangedHandler() {
            public void onItemChanged(ItemChangedEvent event) {
                saveButton.setDisabled(!form.validate());
                resetButton.enable();
            }
        });

        IButton cancelButton = new LocatableIButton(this.extendLocatorId("Cancel"), "Cancel");
        cancelButton.addClickHandler(new com.smartgwt.client.widgets.events.ClickHandler() {
            public void onClick(com.smartgwt.client.widgets.events.ClickEvent clickEvent) {
                History.back();
            }
        });

        HLayout buttonLayout = new HLayout(10);
        buttonLayout.setAlign(Alignment.CENTER);
        buttonLayout.addMember(saveButton);
        buttonLayout.addMember(resetButton);
        buttonLayout.addMember(cancelButton);

        form.setItems(roleSelectionItem);

        editCanvas = new VLayout();

        editCanvas.addMember(form);
        editCanvas.addMember(buttonLayout);

        return editCanvas;
    }

    public void save() {
        final Set<Integer> roles = roleSelector.getSelection();

        // The form.saveData() call triggers either UsersDataSource.executeAdd() to create the new Subject,
        // or executeUpdate() if saving changes to an existing Subject. On success we need to perform the
        // subsequent role assignment, so set this callback on completion.                 
        form.saveData(new DSCallback() {
            public void execute(DSResponse dsResponse, Object o, DSRequest dsRequest) {

                int subjectId = Integer.parseInt(new ListGridRecord(dsRequest.getData()).getAttribute("id"));

                int[] roleIds = new int[roles.size()];
                int i = 0;
                for (Integer id : roles) {
                    roleIds[i++] = id;
                }

                GWTServiceLookup.getRoleService().setAssignedRolesForSubject(subjectId, roleIds,
                    new AsyncCallback<Void>() {
                        public void onFailure(Throwable caught) {
                            CoreGUI.getErrorHandler().handleError("Failed to save user role assignments.", caught);
                            History.back();
                        }

                        public void onSuccess(Void result) {
                            CoreGUI.getMessageCenter().notify(
                                new Message("Succesfully saved user role assignments.", Message.Severity.Info));
                            History.back();
                        }
                    });
            }
        });
    }

    @SuppressWarnings("unchecked")
    public void editRecord(Record record) {
        roleSelector = new SubjectRoleSelector(this.extendLocatorId("Roles"), (Set<Role>) record
            .getAttributeAsObject("roles"));
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

    private void editNewInternal() {
        Subject subject = new Subject();
        subject.setFactive(true);
        ListGridRecord record = dataSource.copyValues(subject);
        editRecord(record);

        // This tells form.saveData() to call UsersDataSource.executeAdd() on the new Subject's ListGridRecord
        form.setSaveOperationType(DSOperationType.ADD);
    }

    public static void editNew(String locatorId) {
        UserEditView editView = new UserEditView(locatorId);
        editView.editNewInternal();
    }

    private void editSubject(final ViewId current) {

        final int id = Integer.valueOf(current.getBreadcrumbs().get(0).getName());

        if (id > 0) {
            SubjectCriteria criteria = new SubjectCriteria();
            criteria.addFilterId(id);
            criteria.fetchRoles(true);
            criteria.fetchConfiguration(true);

            GWTServiceLookup.getSubjectService().findSubjectsByCriteria(criteria,
                new AsyncCallback<PageList<Subject>>() {
                    @Override
                    public void onFailure(Throwable caught) {
                        CoreGUI.getErrorHandler().handleError("Failed to load subject for editing", caught);
                    }

                    @Override
                    public void onSuccess(PageList<Subject> result) {
                        Subject subject = result.get(0);
                        Record record = new UsersDataSource().copyValues(subject);
                        editRecord(record);

                        current.getBreadcrumbs().get(0).setDisplayName("Editing: " + subject.getName());
                        CoreGUI.refreshBreadCrumbTrail();
                    }
                });
        } else {
            editNewInternal();
            current.getBreadcrumbs().get(0).setDisplayName("New User");
            CoreGUI.refreshBreadCrumbTrail();
        }
    }

    @Override
    public void renderView(ViewPath viewPath) {
        int userId = viewPath.getCurrentAsInt();

        editSubject(viewPath.getCurrent());
    }
}
