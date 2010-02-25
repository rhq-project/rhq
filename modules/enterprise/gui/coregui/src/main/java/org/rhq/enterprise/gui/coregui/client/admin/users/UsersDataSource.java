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
import org.rhq.core.domain.criteria.SubjectCriteria;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.gwt.SubjectGWTServiceAsync;
import org.rhq.enterprise.gui.coregui.client.util.RPCDataSource;

import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.data.DSRequest;
import com.smartgwt.client.data.DSResponse;
import com.smartgwt.client.data.DataSourceField;
import com.smartgwt.client.data.fields.DataSourceIntegerField;
import com.smartgwt.client.data.fields.DataSourceTextField;
import com.smartgwt.client.rpc.RPCResponse;
import com.smartgwt.client.widgets.grid.ListGridRecord;

/**
 * @author Greg Hinkle
 */
public class UsersDataSource extends RPCDataSource {

    private static UsersDataSource INSTANCE;

    private SubjectGWTServiceAsync subjectService = GWTServiceLookup.getSubjectService();


    public static UsersDataSource getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new UsersDataSource();
        }
        return INSTANCE;
    }

    
    protected UsersDataSource() {
        super("Users");

        DataSourceField idDataField = new DataSourceIntegerField("id", "ID");
        idDataField.setPrimaryKey(true);

        DataSourceTextField usernameField = new DataSourceTextField("username", "User Name");
        usernameField.setCanEdit(false);

        //DataSourceTextField name = new DataSourceTextField("name", "Name");

        DataSourceTextField firstName = new DataSourceTextField("firstName", "First Name");

        DataSourceTextField lastName = new DataSourceTextField("lastName", "Last Name");


        DataSourceTextField email = new DataSourceTextField("email", "Email Address");


        DataSourceTextField phone = new DataSourceTextField("phoneNumber", "Phone");

        DataSourceTextField department = new DataSourceTextField("department", "Department");

        DataSourceField roles = new DataSourceField();
        roles.setForeignKey("Roles.id");
        roles.setName("roles");
        roles.setMultiple(true);


        setFields(idDataField, usernameField, firstName, lastName, phone, email, department);
    }



    public void executeFetch(final String requestId, final DSRequest request, final DSResponse response) {
        final long start = System.currentTimeMillis();
        
        SubjectCriteria criteria = new SubjectCriteria();
        criteria.setPageControl(getPageControl(request, criteria.getAlias()));

        subjectService.findSubjectsByCriteria(criteria, new AsyncCallback<PageList<Subject>>() {
            public void onFailure(Throwable caught) {
                Window.alert("Failed to load " + caught.getMessage());
                System.err.println("Failed to fetch Resource Data");
                response.setStatus(RPCResponse.STATUS_FAILURE);
                processResponse(requestId, response);
            }

            public void onSuccess(PageList<Subject> result) {
                System.out.println("Data retrieved in: " + (System.currentTimeMillis() - start));

                ListGridRecord[] records = new ListGridRecord[result.size()];
                for (int x=0; x<result.size(); x++) {
                    Subject res = result.get(x);
                    ListGridRecord record = new ListGridRecord();
                    record.setAttribute("id",res.getId());
                    record.setAttribute("username",res.getName());
                    record.setAttribute("name",res.getFirstName() + " " + res.getLastName());
                    record.setAttribute("firstName", res.getFirstName());
                    record.setAttribute("lastName", res.getLastName());
                    record.setAttribute("factive", res.getFactive());
                    record.setAttribute("department", res.getDepartment());
                    record.setAttribute("phoneNumber", res.getPhoneNumber());
                    record.setAttribute("email",res.getEmailAddress());
                    records[x] = record;
                }

                response.setData(records);
                response.setTotalRows(result.getTotalSize());	// for paging to work we have to specify size of full result set
                processResponse(requestId, response);
            }
        });
    }

}
