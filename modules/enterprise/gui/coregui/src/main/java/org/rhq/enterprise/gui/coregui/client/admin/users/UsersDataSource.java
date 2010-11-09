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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.data.DSRequest;
import com.smartgwt.client.data.DSResponse;
import com.smartgwt.client.data.DataSourceField;
import com.smartgwt.client.data.Record;
import com.smartgwt.client.data.fields.DataSourceIntegerField;
import com.smartgwt.client.data.fields.DataSourcePasswordField;
import com.smartgwt.client.data.fields.DataSourceTextField;
import com.smartgwt.client.rpc.RPCResponse;
import com.smartgwt.client.widgets.form.validator.LengthRangeValidator;
import com.smartgwt.client.widgets.form.validator.MatchesFieldValidator;
import com.smartgwt.client.widgets.form.validator.RegExpValidator;
import com.smartgwt.client.widgets.grid.ListGridRecord;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.authz.Role;
import org.rhq.core.domain.criteria.SubjectCriteria;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.gwt.SubjectGWTServiceAsync;
import org.rhq.enterprise.gui.coregui.client.util.RPCDataSource;
import org.rhq.enterprise.gui.coregui.client.util.message.Message;

/**
 * @author Greg Hinkle
 * @author Ian Springer
 */
public class UsersDataSource extends RPCDataSource<Subject> {

    private static UsersDataSource INSTANCE;
    private static final String EMAIL_ADDRESS_REGEXP = "^([a-zA-Z0-9_.\\-+])+@(([a-zA-Z0-9\\-])+\\.)+[a-zA-Z0-9]{2,4}$";

    public static abstract class Field {
        public static final String ID = "id";
        public static final String NAME = "name";
        public static final String FIRST_NAME = "firstName";
        public static final String LAST_NAME = "lastName";
        public static final String FACTIVE = "factive";
        public static final String FSYSTEM = "fsystem";
        public static final String DEPARTMENT = "department";
        public static final String PHONE_NUMBER = "phoneNumber";
        public static final String EMAIL_ADDRESS = "emailAddress";
        public static final String ROLES = "roles";

        // auth-related fields
        public static final String HAS_PRINCIPAL = "hasPrincipal";
        public static final String PASSWORD = "password";
        public static final String PASSWORD_VERIFY = "passwordVerify";
    }

    private final SubjectGWTServiceAsync subjectService = GWTServiceLookup.getSubjectService();

    public static UsersDataSource getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new UsersDataSource();
        }
        return INSTANCE;
    }

    public UsersDataSource() {
        super();
        List<DataSourceField> fields = addDataSourceFields();
        addFields(fields);
    }

    @Override
    protected List<DataSourceField> addDataSourceFields() {
        List<DataSourceField> fields = super.addDataSourceFields();

        DataSourceField idDataField = new DataSourceIntegerField(Field.ID, "ID");
        idDataField.setPrimaryKey(true);
        idDataField.setCanEdit(false);
        fields.add(idDataField);

        DataSourceTextField usernameField = createTextField(Field.NAME, "User Name", 3, 100, true);
        fields.add(usernameField);

        DataSourceTextField hasPrincipalField = createBooleanField(Field.HAS_PRINCIPAL, "LDAP Auth?", true);
        hasPrincipalField.setCanEdit(false);
        fields.add(hasPrincipalField);
        
        // TODO: Should the password always be required? Probably not for LDAP users...
        DataSourcePasswordField passwordField = new DataSourcePasswordField(Field.PASSWORD, "Password", 100, true);
        LengthRangeValidator passwordValidator = new LengthRangeValidator();
        passwordValidator.setMin(6);
        passwordValidator.setMax(100);
        passwordField.setValidators(passwordValidator);
        fields.add(passwordField);

        DataSourcePasswordField passwordVerifyField = new DataSourcePasswordField(Field.PASSWORD_VERIFY, "Verify Password", 100,
            true);
        MatchesFieldValidator passwordsEqualValidator = new MatchesFieldValidator();
        passwordsEqualValidator.setOtherField(Field.PASSWORD);
        passwordsEqualValidator.setErrorMessage("Passwords do not match.");
        passwordVerifyField.setValidators(passwordsEqualValidator);
        fields.add(passwordVerifyField);

        DataSourceTextField firstNameField = createTextField(Field.FIRST_NAME, "First Name", null, 100, true);
        fields.add(firstNameField);

        DataSourceTextField lastNameField = createTextField(Field.LAST_NAME, "Last Name", null, 100, true);
        fields.add(lastNameField);

        DataSourceTextField emailAddressField = createTextField(Field.EMAIL_ADDRESS, "Email Address", null, 100, true);
        fields.add(emailAddressField);
        RegExpValidator emailAddressValidator = new RegExpValidator(EMAIL_ADDRESS_REGEXP);
        emailAddressValidator.setErrorMessage("Invalid email address.");
        emailAddressField.setValidators(emailAddressValidator);

        DataSourceTextField phoneNumberField = createTextField(Field.PHONE_NUMBER, "Phone Number", null, 100, false);
        fields.add(phoneNumberField);

        DataSourceTextField departmentField = createTextField(Field.DEPARTMENT, "Department", null, 100, false);
        fields.add(departmentField);

        DataSourceTextField enabledField = createBooleanField(Field.FACTIVE, "Login Enabled?", true);
        fields.add(enabledField);

        return fields;
    }

    public void executeFetch(final DSRequest request, final DSResponse response) {
        SubjectCriteria criteria = new SubjectCriteria();
        criteria.setPageControl(getPageControl(request));
        criteria.addFilterFsystem(false); // filter out the overlord
        criteria.fetchRoles(true);

        subjectService.findSubjectsByCriteria(criteria, new AsyncCallback<PageList<Subject>>() {
            public void onFailure(Throwable caught) {
                CoreGUI.getErrorHandler().handleError("Failed to fetch users.", caught);
                response.setStatus(RPCResponse.STATUS_FAILURE);
                processResponse(request.getRequestId(), response);
            }

            public void onSuccess(PageList<Subject> result) {
                populateSuccessResponse(result, response);
                processResponse(request.getRequestId(), response);
            }
        });
    }

    @Override
    protected void executeAdd(final DSRequest request, final DSResponse response) {
        JavaScriptObject data = request.getData();
        final ListGridRecord rec = new ListGridRecord(data);
        final Subject newSubject = copyValues(rec);

        subjectService.createSubject(newSubject, new AsyncCallback<Subject>() {
            public void onFailure(Throwable caught) {
                // TODO better exceptions so we can set the right validation errors
                Map<String, String> errors = new HashMap<String, String>();
                errors.put("name", "A user with this name already exists.");
                response.setErrors(errors);
                //                CoreGUI.getErrorHandler().handleError("Failed to create role",caught);
                response.setStatus(RPCResponse.STATUS_VALIDATION_ERROR);
                processResponse(request.getRequestId(), response);
            }

            public void onSuccess(final Subject result) {
                String password = rec.getAttribute("password");
                subjectService.createPrincipal(newSubject.getName(), password, new AsyncCallback<Void>() {
                    public void onFailure(Throwable caught) {
                        CoreGUI.getErrorHandler()
                            .handleError("Subject created, but failed to create principal", caught);
                    }

                    public void onSuccess(Void nothing) {
                        CoreGUI.getMessageCenter().notify(
                            new Message("Created User [" + newSubject.getName() + "]", Message.Severity.Info));
                        response.setData(new Record[] { copyValues(result) });
                        processResponse(request.getRequestId(), response);
                    }
                });
            }
        });

    }

    @Override
    protected void executeUpdate(final DSRequest request, final DSResponse response) {
        final ListGridRecord record = getEditedRecord(request);
        final Subject updatedSubject = copyValues(record);
        subjectService.updateSubject(updatedSubject, new AsyncCallback<Subject>() {
            public void onFailure(Throwable caught) {
                CoreGUI.getErrorHandler().handleError("Failed to update user.", caught);
            }

            public void onSuccess(final Subject result) {

                String password = record.getAttributeAsString("password");
                if (password != null) {
                    subjectService.changePassword(updatedSubject.getName(), password, new AsyncCallback<Void>() {
                        public void onFailure(Throwable caught) {
                            CoreGUI.getErrorHandler().handleError("Failed to update user's password.", caught);
                        }

                        public void onSuccess(Void nothing) {
                            CoreGUI.getMessageCenter().notify(
                                new Message("User updated and password changed.", Message.Severity.Info));
                            response.setData(new Record[] { copyValues(result) });
                            processResponse(request.getRequestId(), response);

                        }
                    });
                } else {
                    CoreGUI.getMessageCenter().notify(
                        new Message("User [" + result.getName() + "] updated.", Message.Severity.Info));
                    response.setData(new Record[] { copyValues(result) });
                    processResponse(request.getRequestId(), response);
                }
            }
        });
    }

    @Override
    protected void executeRemove(final DSRequest request, final DSResponse response) {
        JavaScriptObject data = request.getData();
        final ListGridRecord rec = new ListGridRecord(data);
        final Subject subjectToDelete = copyValues(rec);

        subjectService.deleteSubjects(new int[] { subjectToDelete.getId() }, new AsyncCallback<Void>() {
            public void onFailure(Throwable caught) {
                CoreGUI.getErrorHandler().handleError("Failed to delete user.", caught);
            }

            public void onSuccess(Void result) {
                CoreGUI.getMessageCenter().notify(
                    new Message("User [" + subjectToDelete.getName() + "] deleted.", Message.Severity.Info));
                response.setData(new Record[] { rec });
                processResponse(request.getRequestId(), response);
            }
        });

    }

    @SuppressWarnings("unchecked")
    public Subject copyValues(ListGridRecord from) {
        Subject to = new Subject();
        
        to.setId(from.getAttributeAsInt(Field.ID));
        to.setName(from.getAttributeAsString(Field.NAME));
        to.setFirstName(from.getAttributeAsString(Field.FIRST_NAME));
        to.setLastName(from.getAttributeAsString(Field.LAST_NAME));
        to.setFactive(from.getAttributeAsBoolean(Field.FACTIVE));
        to.setFsystem(from.getAttributeAsBoolean(Field.FSYSTEM));
        to.setDepartment(from.getAttributeAsString(Field.DEPARTMENT));
        to.setPhoneNumber(from.getAttributeAsString(Field.PHONE_NUMBER));
        to.setEmailAddress(from.getAttributeAsString(Field.EMAIL_ADDRESS));

        to.setRoles((Set<Role>) from.getAttributeAsObject(Field.ROLES));

        return to;
    }

    public ListGridRecord copyValues(Subject from) {
        ListGridRecord to = new ListGridRecord();

        to.setAttribute(Field.ID, from.getId());
        to.setAttribute(Field.NAME, from.getName());
        to.setAttribute(Field.FIRST_NAME, from.getFirstName());
        to.setAttribute(Field.LAST_NAME, from.getLastName());
        to.setAttribute(Field.FACTIVE, from.getFactive());
        to.setAttribute(Field.FSYSTEM, from.getFsystem());
        to.setAttribute(Field.DEPARTMENT, from.getDepartment());
        to.setAttribute(Field.PHONE_NUMBER, from.getPhoneNumber());
        to.setAttribute(Field.EMAIL_ADDRESS, from.getEmailAddress());

        to.setAttribute(Field.ROLES, from.getRoles());

        // TODO ...
        to.setAttribute(Field.HAS_PRINCIPAL, true);
        to.setAttribute(Field.PASSWORD, "********");
        to.setAttribute(Field.PASSWORD_VERIFY, "********");

        to.setAttribute("entity", from);

        return to;
    }

}
