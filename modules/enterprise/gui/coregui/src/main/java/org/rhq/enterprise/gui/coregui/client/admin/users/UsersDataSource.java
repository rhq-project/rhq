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

import java.util.Collections;
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
import com.smartgwt.client.data.RecordList;
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
import org.rhq.enterprise.gui.coregui.client.admin.roles.RolesDataSource;
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
    private static final String MASKED_PASSWORD_VALUE = "XXXXXXXX";

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
        hasPrincipalField.setCanEdit(false); // read-only
        fields.add(hasPrincipalField);

        DataSourcePasswordField passwordField = new DataSourcePasswordField(Field.PASSWORD, "Password", 100, true);
        LengthRangeValidator passwordValidator = new LengthRangeValidator();
        passwordValidator.setMin(6);
        passwordValidator.setMax(100);
        passwordField.setValidators(passwordValidator);
        fields.add(passwordField);

        DataSourcePasswordField passwordVerifyField = new DataSourcePasswordField(Field.PASSWORD_VERIFY,
            "Verify Password", 100, true);
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
    protected void executeAdd(final Record newRecord, final DSRequest request, final DSResponse response) {
        final Subject newSubject = copyValues(newRecord);

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
                String password = newRecord.getAttribute(Field.PASSWORD);
                subjectService.createPrincipal(newSubject.getName(), password, new AsyncCallback<Void>() {
                    public void onFailure(Throwable caught) {
                        CoreGUI.getErrorHandler()
                            .handleError("Subject created, but failed to create principal.", caught);
                    }

                    public void onSuccess(Void nothing) {
                        response.setData(new Record[] { copyValues(result) });
                        processResponse(request.getRequestId(), response);

                        UsersView.setMessage(new Message("Created User [" + newSubject.getName() + "].",
                            Message.Severity.Info));

                        CoreGUI.goToView(UsersView.VIEW_PATH);
                    }
                });

                // TODO: Update roles (currently requires separate SLSB call).
            }
        });

    }

    @Override
    protected void executeUpdate(Record updatedUserRecord, final DSRequest request, final DSResponse response) {
        Subject updatedSubject = copyValues(updatedUserRecord);

        final int subjectId = updatedSubject.getId();
        final AddOrUpdateResults results = new AddOrUpdateResults();
        results.subjectId = subjectId;

        final String updatedPassword = updatedUserRecord.getAttributeAsString(Field.PASSWORD);
        boolean passwordWasUpdated = !MASKED_PASSWORD_VALUE.equals(updatedPassword);
        if (!passwordWasUpdated) {
            results.passwordProcessed = true;
        }

        Record currentSubjectRecord = request.getOldValues();
        Record[] currentRoleRecords = currentSubjectRecord.getAttributeAsRecordArray(Field.ROLES);
        Set<Role> currentRoles = RolesDataSource.getInstance().buildDataObjects(currentRoleRecords);
        Record[] updatedRoleRecords = updatedUserRecord.getAttributeAsRecordArray(Field.ROLES);
        Set<Role> updatedRoles = RolesDataSource.getInstance().buildDataObjects(updatedRoleRecords);
        boolean rolesWereUpdated = (currentRoles != null && !currentRoles.equals(updatedRoles)) ||
            (currentRoles == null && updatedRoles != null);
        if (!rolesWereUpdated) {
            results.rolesProcessed = true;
        }

        subjectService.updateSubject(updatedSubject, new AsyncCallback<Subject>() {
            public void onFailure(Throwable caught) {
                results.subjectProcessed = true;
                results.subjectError = caught;
                populateAndProcessResponseIfComplete(results, request, response);
            }

            public void onSuccess(final Subject updatedSubject) {
                results.subjectProcessed = true;
                results.updatedSubject = updatedSubject;
                populateAndProcessResponseIfComplete(results, request, response);
            }
        });

        if (passwordWasUpdated) {
            subjectService.changePassword(updatedSubject.getName(), updatedPassword, new AsyncCallback<Void>() {
                public void onFailure(Throwable caught) {
                    results.passwordProcessed = true;
                    results.passwordError = caught;
                    populateAndProcessResponseIfComplete(results, request, response);
                }

                public void onSuccess(Void nothing) {
                    results.passwordProcessed = true;
                    results.updatedPassword = updatedPassword;
                    populateAndProcessResponseIfComplete(results, request, response);
                }
            });
        }

        if (rolesWereUpdated) {
            if (updatedRoles == null) {
                updatedRoles = Collections.emptySet();
            }
            int[] updatedRoleIds = new int[updatedRoles.size()];
            int index = 0;
            for (Role updatedRole : updatedRoles) {
                updatedRoleIds[index++] = updatedRole.getId();
            }
            final Set<Role> finalUpdatedRoles = updatedRoles;
            GWTServiceLookup.getRoleService().setAssignedRolesForSubject(subjectId, updatedRoleIds,
                new AsyncCallback<Void>() {
                    public void onFailure(Throwable caught) {
                        results.rolesProcessed = true;
                        results.rolesError = caught;
                        populateAndProcessResponseIfComplete(results, request, response);
                    }

                    public void onSuccess(Void result) {
                        results.rolesProcessed = true;
                        results.updatedRoles = finalUpdatedRoles;
                        populateAndProcessResponseIfComplete(results, request, response);
                    }
                });
        }
    }

    private void populateAndProcessResponseIfComplete(AddOrUpdateResults results, DSRequest request, DSResponse response) {
        if (results.isComplete()) {
            Record updatedRecord = null;
            if (results.updatedSubject != null) {
                updatedRecord = copyValues(results.updatedSubject);
            } else {
                if (results.updatedPassword != null || results.updatedRoles != null) {
                    RecordList recordList = response.getDataAsRecordList();
                    updatedRecord = recordList.find(Field.ID, results.subjectId);
                }
                if (results.subjectError != null) {
                    CoreGUI.getErrorHandler().handleError("Failed to update subject for user with id ["
                        + results.subjectId + "]." , results.subjectError);
                }
            }

            if (results.updatedPassword != null) {
                updatedRecord.setAttribute(Field.PASSWORD, results.updatedPassword);
            } else if (results.passwordError != null) {
                CoreGUI.getErrorHandler().handleError("Failed to update password for user with id [" + results.subjectId
                    + "]." , results.passwordError);
            }

            if (results.updatedRoles != null) {
                updatedRecord.setAttribute(Field.ROLES, results.updatedRoles);
            } else if (results.rolesError != null) {
                CoreGUI.getErrorHandler().handleError("Failed to update assigned roles for user with id ["
                    + results.subjectId + "].", results.rolesError);
            }

            if (updatedRecord != null) {
                String username = updatedRecord.getAttribute(Field.NAME);
                UsersView.setMessage(new Message("User updated.", "User [" + username + "] updated.",
                    Message.Severity.Info));
                response.setData(new Record[] {updatedRecord});
                processResponse(request.getRequestId(), response);
            }

            CoreGUI.goToView(UsersView.VIEW_PATH);
        }
    }

    class AddOrUpdateResults {
        int subjectId;

        boolean subjectProcessed;
        boolean passwordProcessed;
        boolean rolesProcessed;

        Subject updatedSubject;
        String updatedPassword;
        Set<Role> updatedRoles;

        Throwable subjectError;
        Throwable passwordError;
        Throwable rolesError;

        boolean isComplete() {
            return (subjectProcessed && passwordProcessed && rolesProcessed);
        }
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
    public Subject copyValues(Record from) {
        Subject to = new Subject();
        
        to.setId(from.getAttributeAsInt(Field.ID));
        to.setName(from.getAttributeAsString(Field.NAME));
        to.setFirstName(from.getAttributeAsString(Field.FIRST_NAME));
        to.setLastName(from.getAttributeAsString(Field.LAST_NAME));
        to.setFactive(Boolean.valueOf(from.getAttributeAsString(Field.FACTIVE)));
        to.setFsystem(Boolean.valueOf(from.getAttributeAsString(Field.FSYSTEM)));
        to.setDepartment(from.getAttributeAsString(Field.DEPARTMENT));
        to.setPhoneNumber(from.getAttributeAsString(Field.PHONE_NUMBER));
        to.setEmailAddress(from.getAttributeAsString(Field.EMAIL_ADDRESS));

        Record[] roleRecords = from.getAttributeAsRecordArray(Field.ROLES);
        to.setRoles(RolesDataSource.getInstance().buildDataObjects(roleRecords));

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

        ListGridRecord[] roleRecords = RolesDataSource.getInstance().buildRecords(from.getRoles());
        to.setAttribute(Field.ROLES, roleRecords);

        // TODO ...
        to.setAttribute(Field.HAS_PRINCIPAL, true);
        to.setAttribute(Field.PASSWORD, MASKED_PASSWORD_VALUE);
        to.setAttribute(Field.PASSWORD_VERIFY, MASKED_PASSWORD_VALUE);

        return to;
    }

}
