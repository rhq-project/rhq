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

import java.util.ArrayList;
import java.util.List;

import com.google.gwt.user.client.History;
import com.google.gwt.user.client.rpc.AsyncCallback;
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
import com.smartgwt.client.widgets.form.fields.FormItem;
import com.smartgwt.client.widgets.form.fields.HiddenItem;
import com.smartgwt.client.widgets.form.fields.PasswordItem;
import com.smartgwt.client.widgets.form.fields.RadioGroupItem;
import com.smartgwt.client.widgets.form.fields.TextItem;
import com.smartgwt.client.widgets.grid.ListGridRecord;
import com.smartgwt.client.widgets.layout.HLayout;
import com.smartgwt.client.widgets.layout.VLayout;

import org.rhq.core.domain.auth.Principal;
import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.criteria.SubjectCriteria;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.gui.coregui.client.BookmarkableView;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.DetailsView;
import org.rhq.enterprise.gui.coregui.client.ViewPath;
import org.rhq.enterprise.gui.coregui.client.components.form.EnhancedDynamicForm;
import org.rhq.enterprise.gui.coregui.client.components.selector.AssignedItemsChangedEvent;
import org.rhq.enterprise.gui.coregui.client.components.selector.AssignedItemsChangedHandler;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableIButton;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableVLayout;

/**
 * A form for viewing and/or editing an RHQ user (i.e. a {@link Subject}, and optionally an associated
 * {@link Principal}).
 *
 * @author Greg Hinkle
 * @author Ian Springer
 */
public class UserEditView extends LocatableVLayout implements BookmarkableView, DetailsView {
    
    private Label message = new Label("Loading...");

    private VLayout editCanvas;
    private DynamicForm form;

    private UsersDataSource dataSource;

    private SubjectRoleSelector roleSelector;
    private IButton saveButton;
    private IButton resetButton;

    private boolean isReadOnly;
    private HLayout roleSelectionPane;

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

        List<FormItem> items = new ArrayList<FormItem>();                

        TextItem nameItem = new TextItem(UsersDataSource.Field.NAME);
        nameItem.setWidth(200);
        items.add(nameItem);

        PasswordItem passwordItem = new PasswordItem(UsersDataSource.Field.PASSWORD);
        passwordItem.setWidth(200);
        items.add(passwordItem);

        PasswordItem verifyPasswordItem = new PasswordItem(UsersDataSource.Field.PASSWORD_VERIFY);
        verifyPasswordItem.setWidth(200);
        items.add(verifyPasswordItem);

        TextItem firstNameItem = new TextItem(UsersDataSource.Field.FIRST_NAME);
        firstNameItem.setWidth(200);
        items.add(firstNameItem);

        TextItem lastNameItem = new TextItem(UsersDataSource.Field.LAST_NAME);
        lastNameItem.setWidth(200);
        items.add(lastNameItem);

        TextItem emailAddressItem = new TextItem(UsersDataSource.Field.EMAIL_ADDRESS);
        emailAddressItem.setWidth(200);
        items.add(emailAddressItem);

        TextItem phoneNumberItem = new TextItem(UsersDataSource.Field.PHONE_NUMBER);
        phoneNumberItem.setWidth(200);
        items.add(phoneNumberItem);

        TextItem departmentItem = new TextItem(UsersDataSource.Field.DEPARTMENT);
        departmentItem.setWidth(200);
        items.add(departmentItem);

        RadioGroupItem activeItem = new RadioGroupItem(UsersDataSource.Field.FACTIVE);
        activeItem.setVertical(false);
        items.add(activeItem);

        HiddenItem rolesItem = new HiddenItem(UsersDataSource.Field.ROLES);
        items.add(rolesItem);

        this.roleSelectionPane = new HLayout();

        this.form.setItems(items.toArray(new FormItem[items.size()]));

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
        editCanvas.addMember(roleSelectionPane);
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
        ListGridRecord[] roleRecords = roleSelector.getAssignedGrid().getSelection();
        form.setValue(UsersDataSource.Field.ROLES, roleRecords);
        this.form.saveData();
    }

    @SuppressWarnings("unchecked")
    private void editRecord(final Record subjectRecord) {
        int subjectId = subjectRecord.getAttributeAsInt(UsersDataSource.Field.ID);
        Record[] roleRecords = subjectRecord.getAttributeAsRecordArray(UsersDataSource.Field.ROLES);
        ListGridRecord[] roleListGridRecords = new ListGridRecord[roleRecords.length];
        for (int i = 0, roleRecordsLength = roleRecords.length; i < roleRecordsLength; i++) {
            Record roleRecord = roleRecords[i];
            roleListGridRecords[i] = (ListGridRecord)roleRecord;
        }
        boolean isReadOnly = (this.isReadOnly || subjectId == 2);
        roleSelector = new SubjectRoleSelector(this.extendLocatorId("Roles"), roleListGridRecords, isReadOnly);
        roleSelector.setWidth100();
        roleSelector.setAlign(Alignment.LEFT);
        roleSelector.addAssignedItemsChangedHandler(new AssignedItemsChangedHandler() {
            public void onSelectionChanged(AssignedItemsChangedEvent event) {
                onItemChanged();
            }
        });
        roleSelectionPane.addMember(roleSelector);

        try {
            form.editRecord(subjectRecord);
        } catch (Throwable t) {
            t.printStackTrace();
        }
        form.setSaveOperationType((subjectId == 0) ? DSOperationType.ADD : DSOperationType.UPDATE);

        message.hide();
        editCanvas.show();
        markForRedraw();
    }

    private void editNewSubject() {
        setTitle("New User");

        Subject subject = new Subject();
        subject.setFactive(true);
        ListGridRecord record = dataSource.copyValues(subject);
        editRecord(record);

        // This tells form.saveData() to call UsersDataSource.executeAdd() on the new Subject's ListGridRecord
        form.setSaveOperationType(DSOperationType.ADD);
    }

    private void editExistingSubject(final int subjectId) {
        SubjectCriteria criteria = new SubjectCriteria();
        criteria.addFilterId(subjectId);
        criteria.fetchRoles(true);
        criteria.fetchConfiguration(true);

        GWTServiceLookup.getSubjectService().findSubjectsByCriteria(criteria,
            new AsyncCallback<PageList<Subject>>() {
                @Override
                public void onFailure(Throwable caught) {
                    CoreGUI.getErrorHandler().handleError("Failed to load subject for editing.", caught);
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

                    setTitle("User '" + subject.getName() + "'");
                }
            });
    }

    @Override
    public boolean isEditable() {
        return (!this.isReadOnly);
    }

    @Override
    public void renderView(ViewPath viewPath) {
        int subjectId = viewPath.getCurrentAsInt();
        if (subjectId == 0) {
            editNewSubject();
        } else {
            editExistingSubject(subjectId);
        }
    }
}
