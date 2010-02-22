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

import org.rhq.core.domain.authz.Role;
import org.rhq.core.domain.criteria.RoleCriteria;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.gwt.RoleGWTServiceAsync;
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
public class RolesDataSource extends RPCDataSource {

    private RoleGWTServiceAsync roleService = GWTServiceLookup.getRoleService();

    private boolean initialized = false;

    private static RolesDataSource INSTANCE;

    public static RolesDataSource getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new RolesDataSource();
        }
        return INSTANCE;
    }


    private String query;

    protected RolesDataSource() {
        super("Roles");


        DataSourceField idDataField = new DataSourceIntegerField("id", "ID");
        idDataField.setPrimaryKey(true);

        DataSourceTextField nameField = new DataSourceTextField("name", "Name");


        setFields(idDataField, nameField);
    }


    public void executeFetch(final String requestId, final DSRequest request, final DSResponse response) {
        final long start = System.currentTimeMillis();


        RoleCriteria criteria = new RoleCriteria();
//        criteria.addFilterName(query);

        if (request.getStartRow() != null && request.getEndRow() != null) {
            criteria.setPageControl(PageControl.getExplicitPageControl(request.getStartRow(), request.getEndRow() - request.getStartRow()));
        } else {
            criteria.setPageControl(PageControl.getSingleRowInstance());
        }

//        criteria.addSortAgentName(PageOrdering.ASC);

        roleService.findRolesByCriteria(criteria, new AsyncCallback<PageList<Role>>() {
            public void onFailure(Throwable caught) {
                Window.alert("Failed to load " + caught.getMessage());
                System.err.println("Failed to fetch Resource Data");
                response.setStatus(RPCResponse.STATUS_FAILURE);
                processResponse(requestId, response);
            }

            public void onSuccess(PageList<Role> result) {

                System.out.println("Data retrieved in: " + (System.currentTimeMillis() - start));

                ListGridRecord[] records = new ListGridRecord[result.size()];
                for (int x = 0; x < result.size(); x++) {
                    Role role = result.get(x);
                    ListGridRecord record = new ListGridRecord();
                    record.setAttribute("id", role.getId());
                    record.setAttribute("name", role.getName());


                    record.setAttribute("permissions", role.getPermissions());

                    records[x] = record;
                }

                response.setData(records);
                response.setTotalRows(result.getTotalSize());    // for paging to work we have to specify size of full result set
                processResponse(requestId, response);

            }
        });
    }

}
