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
package org.rhq.coregui.client.admin.users;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.data.DSRequest;
import com.smartgwt.client.data.DSResponse;
import com.smartgwt.client.data.DataSourceField;
import com.smartgwt.client.data.Record;
import com.smartgwt.client.data.fields.DataSourceIntegerField;
import com.smartgwt.client.data.fields.DataSourcePasswordField;
import com.smartgwt.client.data.fields.DataSourceTextField;
import com.smartgwt.client.types.FieldType;
import com.smartgwt.client.widgets.form.validator.LengthRangeValidator;
import com.smartgwt.client.widgets.form.validator.MatchesFieldValidator;
import com.smartgwt.client.widgets.form.validator.RegExpValidator;
import com.smartgwt.client.widgets.grid.ListGridRecord;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.criteria.SubjectCriteria;
import org.rhq.core.domain.util.PageList;
import org.rhq.coregui.client.admin.roles.RolesDataSource;
import org.rhq.coregui.client.gwt.GWTServiceLookup;
import org.rhq.coregui.client.gwt.SubjectGWTServiceAsync;
import org.rhq.coregui.client.util.RPCDataSource;
import org.rhq.coregui.client.util.validator.EmailValidator;

/**
 * A DataSource for RHQ {@link Subject user}s.
 *
 * @author Greg Hinkle
 * @author Ian Springer
 */
public class UsersDataSource extends RPCDataSource<Subject, SubjectCriteria> {

    private static UsersDataSource INSTANCE;

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
        public static final String LDAP = "ldap";
        public static final String PASSWORD = "password";
        public static final String PASSWORD_VERIFY = "passwordVerify";
    }

    public static final int ID_OVERLORD = 1;
    public static final int ID_RHQADMIN = 2;

    private final SubjectGWTServiceAsync subjectService = GWTServiceLookup.getSubjectService();

    public static UsersDataSource getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new UsersDataSource();
        }
        return INSTANCE;
    }

    public static boolean isSystemSubjectId(int subjectId) {
        return (subjectId == ID_OVERLORD || subjectId == ID_RHQADMIN);
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

        DataSourceTextField usernameField = createTextField(Field.NAME, MSG.common_title_username(), 3, 100, true);
        // Don't allow characters that could be used in HTML intended for an XSS attack.
        RegExpValidator regExpValidator = new RegExpValidator("[^&<]*");
        usernameField.setValidators(regExpValidator);
        fields.add(usernameField);

        DataSourceTextField ldapField = createBooleanField(Field.LDAP, MSG.dataSource_users_field_ldap(), true);
        ldapField.setCanEdit(false); // read-only
        fields.add(ldapField);

        DataSourcePasswordField passwordField = new DataSourcePasswordField(Field.PASSWORD,
            MSG.common_title_password(), 100, true);
        LengthRangeValidator passwordValidator = new LengthRangeValidator();
        passwordValidator.setMin(6);
        passwordValidator.setMax(100);
        passwordField.setValidators(passwordValidator);
        fields.add(passwordField);

        DataSourcePasswordField passwordVerifyField = new DataSourcePasswordField(Field.PASSWORD_VERIFY,
            MSG.dataSource_users_field_passwordVerify(), 100, true);
        MatchesFieldValidator passwordsEqualValidator = new MatchesFieldValidator();
        passwordsEqualValidator.setOtherField(Field.PASSWORD);
        passwordsEqualValidator.setErrorMessage(MSG.dataSource_users_passwordsDoNotMatch());
        passwordVerifyField.setValidators(passwordsEqualValidator);
        fields.add(passwordVerifyField);

        DataSourceTextField firstNameField = createTextField(Field.FIRST_NAME, MSG.dataSource_users_field_firstName(),
            null, 100, true);
        fields.add(firstNameField);

        DataSourceTextField lastNameField = createTextField(Field.LAST_NAME, MSG.dataSource_users_field_lastName(),
            null, 100, true);
        fields.add(lastNameField);

        DataSourceTextField emailAddressField = createTextField(Field.EMAIL_ADDRESS,
            MSG.dataSource_users_field_emailAddress(), null, 100, true);
        fields.add(emailAddressField);
        EmailValidator emailAddressValidator = new EmailValidator();
        emailAddressValidator.setErrorMessage(MSG.dataSource_users_invalidEmailAddress());
        emailAddressField.setValidators(emailAddressValidator);

        DataSourceTextField phoneNumberField = createTextField(Field.PHONE_NUMBER,
            MSG.dataSource_users_field_phoneNumber(), null, 100, false);
        fields.add(phoneNumberField);

        DataSourceTextField departmentField = createTextField(Field.DEPARTMENT,
            MSG.dataSource_users_field_department(), null, 100, false);
        fields.add(departmentField);

        DataSourceTextField enabledField = createBooleanField(Field.FACTIVE, MSG.dataSource_users_field_factive(), true);
        fields.add(enabledField);

        DataSourceField rolesField = new DataSourceField(Field.ROLES, FieldType.ANY, "Roles");
        fields.add(rolesField);

        return fields;
    }

    @Override
    public void executeFetch(final DSRequest request, final DSResponse response, final SubjectCriteria criteria) {

        subjectService.findSubjectsByCriteria(criteria, new AsyncCallback<PageList<Subject>>() {
            public void onFailure(Throwable caught) {
                String message = "Failed to fetch user(s).";
                sendFailureResponse(request, response, message, caught);
            }

            public void onSuccess(final PageList<Subject> fetchedSubjects) {
                final PageList<Record> userRecordsPageList = new PageList<Record>(fetchedSubjects.getPageControl());
                userRecordsPageList.setTotalSize(fetchedSubjects.getTotalSize());
                userRecordsPageList.setUnbounded(fetchedSubjects.isUnbounded());
                final boolean[] failed = { false };
                for (int i = 0, fetchedSubjectsSize = fetchedSubjects.size(); i < fetchedSubjectsSize; i++) {
                    if (failed[0]) {
                        break;
                    }
                    final Subject fetchedSubject = fetchedSubjects.get(i);
                    final String username = fetchedSubject.getName();
                    subjectService.isUserWithPrincipal(username, new AsyncCallback<Boolean>() {
                        public void onFailure(Throwable caught) {
                            failed[0] = true;
                            String message = "Failed to check if user [" + username + "] is an LDAP user.";
                            sendFailureResponse(request, response, message, caught);
                        }

                        public void onSuccess(Boolean hasPrincipal) {
                            boolean isLdap = (!hasPrincipal);
                            Record userRecord = copyUserValues(fetchedSubject, isLdap);
                            userRecordsPageList.add(userRecord);
                            if (userRecordsPageList.size() == fetchedSubjects.size()) {
                                sendSuccessResponseRecords(request, response, userRecordsPageList);
                            }
                        }
                    });
                }
            }
        });
    }

    @Override
    protected void executeAdd(final Record recordToAdd, final DSRequest request, final DSResponse response) {
        final Subject newSubject = copyValues(recordToAdd);
        String password = recordToAdd.getAttribute(Field.PASSWORD);

        subjectService.createSubject(newSubject, password, new AsyncCallback<Subject>() {
            public void onFailure(Throwable caught) {
                // TODO: Throw more specific SLSB exceptions so we can set the right validation errors.
                String message = caught.getMessage();
                if (message != null && message.contains("javax.persistence.EntityExistsException")) {
                    Map<String, String> errorMessages = new HashMap<String, String>();
                    errorMessages.put(Field.NAME,
                        MSG.view_adminUsers_failCreateUserWithExistingName(newSubject.getName()));
                    sendValidationErrorResponse(request, response, errorMessages);
                } else {
                    throw new RuntimeException(caught);
                }
            }

            public void onSuccess(final Subject createdSubject) {
                Record createdUserRecord = copyUserValues(createdSubject, false);
                sendSuccessResponse(request, response, createdUserRecord);
            }
        });
    }

    @Override
    protected void executeUpdate(final Record editedUserRecord, Record oldUserRecord, final DSRequest request,
        final DSResponse response) {
        Subject editedSubject = copyValues(editedUserRecord);
        final String username = editedSubject.getName();

        final String editedPassword = editedUserRecord.getAttributeAsString(Field.PASSWORD);
        boolean passwordWasEdited = !MASKED_PASSWORD_VALUE.equals(editedPassword);
        String newPassword = (passwordWasEdited) ? editedPassword : null;

        subjectService.updateSubject(editedSubject, newPassword, new AsyncCallback<Subject>() {
            public void onFailure(Throwable caught) {
                String message = "Failed to update user [" + username + "].";
                sendFailureResponse(request, response, message, caught);
            }

            public void onSuccess(final Subject updatedSubject) {
                sendSuccessResponse(request, response, editedUserRecord);
            }
        });
    }

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

    public Record copyUserValues(Subject subject, boolean isLdap) {
        ListGridRecord targetRecord = copyValues(subject);

        targetRecord.setAttribute(Field.LDAP, isLdap);

        // Leave the password fields blank if username is null (i.e. it's a new user).
        if (subject.getName() != null) {
            targetRecord.setAttribute(Field.PASSWORD, MASKED_PASSWORD_VALUE);
            targetRecord.setAttribute(Field.PASSWORD_VERIFY, MASKED_PASSWORD_VALUE);
        }

        return targetRecord;
    }

    public ListGridRecord copyValues(Subject from) {
        return copyValues(from, true);
    }

    @Override
    public ListGridRecord copyValues(Subject from, boolean cascade) {
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

        if (cascade) {
            ListGridRecord[] roleRecords = RolesDataSource.getInstance().buildRecords(from.getRoles(), false);
            to.setAttribute(Field.ROLES, roleRecords);
        }

        return to;
    }

    @Override
    protected SubjectCriteria getFetchCriteria(DSRequest request) {
        SubjectCriteria criteria = new SubjectCriteria();

        // Filtering
        Integer subjectId = getFilter(request, Field.ID, Integer.class);
        criteria.addFilterId(subjectId);
        // Always filter out the overlord - mortal users need not know the overlord even exists.
        criteria.addFilterFsystem(false);

        // Fetching
        if (subjectId != null) {
            // If we're fetching a single Subject, then fetch the related Set of Roles.
            criteria.fetchRoles(true);
        }

        // TODO: For the list view, use a composite object that will pull the role
        //       count across the wire.  this count will not require permission checks at all.

        return criteria;
    }

    @Override
    protected String getSortFieldForColumn(String columnName) {

        // this is a calculated field, can't perform server-side sort
        if (Field.LDAP.equals(columnName)) {
            return null;
        }

        return super.getSortFieldForColumn(columnName);
    }

}
