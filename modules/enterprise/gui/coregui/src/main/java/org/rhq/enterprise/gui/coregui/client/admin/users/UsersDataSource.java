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
import com.smartgwt.client.data.fields.DataSourceTextField;
import com.smartgwt.client.rpc.RPCResponse;
import com.smartgwt.client.types.FieldType;
import com.smartgwt.client.widgets.form.validator.LengthRangeValidator;
import com.smartgwt.client.widgets.form.validator.MatchesFieldValidator;
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
 */
public class UsersDataSource extends RPCDataSource<Subject> {

    private static UsersDataSource INSTANCE;

    private SubjectGWTServiceAsync subjectService = GWTServiceLookup.getSubjectService();

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

        DataSourceField idDataField = new DataSourceIntegerField("id", "ID");
        idDataField.setPrimaryKey(true);
        idDataField.setCanEdit(false);
        fields.add(idDataField);

        DataSourceTextField usernameField = new DataSourceTextField("name", "User Name", 100, true);
        fields.add(usernameField);

        DataSourceTextField firstName = new DataSourceTextField("firstName", "First Name", 100, true);
        fields.add(firstName);

        DataSourceTextField lastName = new DataSourceTextField("lastName", "Last Name", 100, true);
        fields.add(lastName);

        DataSourceTextField password = new DataSourceTextField("password", "Password", 100, false);
        password.setType(FieldType.PASSWORD);

        LengthRangeValidator passwordValdidator = new LengthRangeValidator();
        passwordValdidator.setMin(6);
        passwordValdidator.setErrorMessage("Password must be at least six characters");
        password.setValidators(passwordValdidator);

        DataSourceTextField passwordVerify = new DataSourceTextField("passwordVerify", "Verify", 100, false);
        passwordVerify.setType(FieldType.PASSWORD);

        MatchesFieldValidator passwordsEqualValidator = new MatchesFieldValidator();
        passwordsEqualValidator.setOtherField("password");
        passwordsEqualValidator.setErrorMessage("Passwords do not match");
        passwordVerify.setValidators(passwordsEqualValidator);
        fields.add(password);

        DataSourceTextField emailAddress = new DataSourceTextField("emailAddress", "Email Address", 100, true);
        fields.add(emailAddress);

        DataSourceTextField phone = new DataSourceTextField("phoneNumber", "Phone", 15, false);
        fields.add(phone);

        DataSourceTextField department = new DataSourceTextField("department", "Department", 100, false);
        fields.add(department);

        DataSourceTextField enabled = new DataSourceTextField("factive", "Enabled");
        enabled.setType(FieldType.BOOLEAN);
        fields.add(enabled);

        return fields;
    }

    public void executeFetch(final DSRequest request, final DSResponse response) {
        SubjectCriteria criteria = new SubjectCriteria();
        criteria.setPageControl(getPageControl(request));
        criteria.fetchRoles(true);

        subjectService.findSubjectsByCriteria(criteria, new AsyncCallback<PageList<Subject>>() {
            public void onFailure(Throwable caught) {
                CoreGUI.getErrorHandler().handleError("Failed to fetch users data", caught);
                response.setStatus(RPCResponse.STATUS_FAILURE);
                processResponse(request.getRequestId(), response);
            }

            public void onSuccess(PageList<Subject> result) {
                response.setData(buildRecords(result));
                response.setTotalRows(result.getTotalSize()); // for paging to work we have to specify size of full result set
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
                CoreGUI.getErrorHandler().handleError("Failed to update subject", caught);
            }

            public void onSuccess(final Subject result) {

                String password = record.getAttributeAsString("password");
                if (password != null) {
                    subjectService.changePassword(updatedSubject.getName(), password, new AsyncCallback<Void>() {
                        public void onFailure(Throwable caught) {
                            CoreGUI.getErrorHandler().handleError("Failed to update subject's password", caught);
                        }

                        public void onSuccess(Void nothing) {
                            CoreGUI.getMessageCenter().notify(
                                new Message("User updated and password changed", Message.Severity.Info));
                            response.setData(new Record[] { copyValues(result) });
                            processResponse(request.getRequestId(), response);

                        }
                    });
                } else {
                    CoreGUI.getMessageCenter().notify(
                        new Message("User [" + result.getName() + "] updated", Message.Severity.Info));
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
                CoreGUI.getErrorHandler().handleError("Failed to delete role", caught);
            }

            public void onSuccess(Void result) {
                CoreGUI.getMessageCenter().notify(
                    new Message("User [" + subjectToDelete.getName() + "] removed", Message.Severity.Info));
                response.setData(new Record[] { rec });
                processResponse(request.getRequestId(), response);
            }
        });

    }

    @SuppressWarnings("unchecked")
    public Subject copyValues(ListGridRecord from) {
        Subject to = new Subject();
        to.setId(from.getAttributeAsInt("id"));
        to.setName(from.getAttributeAsString("name"));
        to.setFirstName(from.getAttributeAsString("firstName"));
        to.setLastName(from.getAttributeAsString("lastName"));
        to.setFactive(from.getAttributeAsBoolean("factive"));
        to.setDepartment(from.getAttributeAsString("department"));
        to.setPhoneNumber(from.getAttributeAsString("phoneNumber"));
        to.setEmailAddress(from.getAttributeAsString("emailAddress"));
        to.setFactive(from.getAttributeAsBoolean("factive"));

        to.setRoles((Set<Role>) from.getAttributeAsObject("roles"));
        return to;
    }

    public ListGridRecord copyValues(Subject from) {
        ListGridRecord to = new ListGridRecord();
        to.setAttribute("id", from.getId());
        to.setAttribute("name", from.getName());
        to.setAttribute("firstName", from.getFirstName());
        to.setAttribute("lastName", from.getLastName());
        to.setAttribute("factive", from.getFactive());
        to.setAttribute("department", from.getDepartment());
        to.setAttribute("phoneNumber", from.getPhoneNumber());
        to.setAttribute("emailAddress", from.getEmailAddress());
        to.setAttribute("factive", from.getFactive());

        to.setAttribute("roles", from.getRoles());

        to.setAttribute("entity", from);
        return to;
    }

}
