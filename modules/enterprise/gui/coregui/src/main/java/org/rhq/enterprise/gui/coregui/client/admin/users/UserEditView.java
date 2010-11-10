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
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.IButton;
import com.smartgwt.client.widgets.Label;
import com.smartgwt.client.widgets.form.DynamicForm;
import com.smartgwt.client.widgets.form.events.ItemChangedEvent;
import com.smartgwt.client.widgets.form.events.ItemChangedHandler;
import com.smartgwt.client.widgets.form.fields.CanvasItem;
import com.smartgwt.client.widgets.form.fields.FormItem;
import com.smartgwt.client.widgets.form.fields.PasswordItem;
import com.smartgwt.client.widgets.form.fields.RadioGroupItem;
import com.smartgwt.client.widgets.form.fields.RowSpacerItem;
import com.smartgwt.client.widgets.form.fields.TextItem;
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
import org.rhq.enterprise.gui.coregui.client.components.selector.AssignedItemsChangedEvent;
import org.rhq.enterprise.gui.coregui.client.components.selector.AssignedItemsChangedHandler;
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
    private IButton saveButton;
    private IButton resetButton;

    private boolean isReadOnly;

    public UserEditView(String locatorId) {
        this(locatorId, false);
    }

    public UserEditView(String locatorId, boolean isReadOnly) {
        super(locatorId);

        this.dataSource = UsersDataSource.getInstance();

        setOverflow(Overflow.AUTO);

        buildSubjectEditor();
        this.editCanvas.hide();

        addMember(this.message);
        addMember(this.editCanvas);

        this.isReadOnly = isReadOnly;
    }

    private Canvas buildSubjectEditor() {
        form = new EnhancedDynamicForm(this.getLocatorId());
        form.setDataSource(dataSource);                

        TextItem nameItem = new TextItem(UsersDataSource.Field.NAME);
        nameItem.setWidth(175);

        PasswordItem passwordItem = new PasswordItem(UsersDataSource.Field.PASSWORD);        

        PasswordItem verifyPasswordItem = new PasswordItem(UsersDataSource.Field.PASSWORD_VERIFY);

        TextItem firstNameItem = new TextItem(UsersDataSource.Field.FIRST_NAME);

        TextItem lastNameItem = new TextItem(UsersDataSource.Field.LAST_NAME);

        TextItem emailAddressItem = new TextItem(UsersDataSource.Field.EMAIL_ADDRESS);

        TextItem phoneNumberItem = new TextItem(UsersDataSource.Field.PHONE_NUMBER);

        TextItem departmentItem = new TextItem(UsersDataSource.Field.DEPARTMENT);

        RadioGroupItem activeItem = new RadioGroupItem(UsersDataSource.Field.FACTIVE);
        activeItem.setVertical(false);

        RowSpacerItem spacerItem = new RowSpacerItem();
        spacerItem.setStartRow(false);

        this.roleSelectionItem = new CanvasItem("selectRoles");
        this.roleSelectionItem.setCanvas(new Canvas());
        this.roleSelectionItem.setColSpan(2);
        this.roleSelectionItem.setShowTitle(false);

        form.setItems(nameItem, passwordItem, verifyPasswordItem, firstNameItem, lastNameItem, emailAddressItem,
            phoneNumberItem, departmentItem, activeItem, spacerItem, roleSelectionItem);

        saveButton = new LocatableIButton(this.extendLocatorId("Save"), "Save");
        saveButton.setDisabled(true);
        saveButton.addClickHandler(new com.smartgwt.client.widgets.events.ClickHandler() {
            public void onClick(com.smartgwt.client.widgets.events.ClickEvent clickEvent) {
                save();
            }
        });

        resetButton = new LocatableIButton(this.extendLocatorId("Reset"), "Reset");
        resetButton.setDisabled(true);
        resetButton.addClickHandler(new com.smartgwt.client.widgets.events.ClickHandler() {
            public void onClick(com.smartgwt.client.widgets.events.ClickEvent clickEvent) {
                form.reset();
                roleSelector.reset();
                resetButton.disable();
            }
        });

        form.addItemChangedHandler(new ItemChangedHandler() {
            public void onItemChanged(ItemChangedEvent event) {
                UserEditView.this.onItemChanged();
            }
        });

        IButton cancelButton = new LocatableIButton(this.extendLocatorId("Cancel"), "Cancel");
        cancelButton.addClickHandler(new com.smartgwt.client.widgets.events.ClickHandler() {
            public void onClick(com.smartgwt.client.widgets.events.ClickEvent clickEvent) {
                History.back();
            }
        });

        HLayout buttonLayout = new HLayout(10);
        buttonLayout.setAlign(Alignment.LEFT);
        buttonLayout.addMember(saveButton);
        buttonLayout.addMember(resetButton);
        buttonLayout.addMember(cancelButton);

        editCanvas = new VLayout();

        editCanvas.addMember(form);
        editCanvas.addMember(buttonLayout);

        return editCanvas;
    }

    private void onItemChanged() {
        // The below is a workaround for the fact that calling form.validate() causes the focus to change to the
        // last invalid field, if one or more fields is invalid.
        FormItem focusItem = form.getFocusItem();
        Boolean isValid = form.validate();
        if (focusItem != null) {
            form.focusInItem(focusItem);
        }
        
        saveButton.setDisabled(!isValid);
        resetButton.setDisabled(false);
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
        int subjectId = record.getAttributeAsInt(UsersDataSource.Field.ID);
        roleSelector = new SubjectRoleSelector(this.extendLocatorId("Roles"), (Set<Role>) record
            .getAttributeAsObject("roles"), this.isReadOnly || subjectId == 2);
        roleSelector.setAlign(Alignment.LEFT);
        roleSelector.addAssignedItemsChangedHandler(new AssignedItemsChangedHandler() {
            public void onSelectionChanged(AssignedItemsChangedEvent event) {
                onItemChanged();
            }
        });
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
                        // Perform up front validation for existing users.
                        // NOTE: We do *not* do this for new users, since we expect most of the required fields to be blank.
                        form.validate();

                        // Don't allow the rhqadmin account to be disabled.
                        if (subject.getId() == 2) {
                            FormItem activeField = form.getField(UsersDataSource.Field.FACTIVE);
                            activeField.disable();
                        }

                        // TODO: Set view header instead, since we no longer display bread crumbs.
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
