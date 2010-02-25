/*
 * RHQ Management Platform
 * Copyright (C) 2010 Red Hat, Inc.
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
package org.rhq.enterprise.gui.coregui.client.alert;

import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.data.DSRequest;
import com.smartgwt.client.data.DSResponse;
import com.smartgwt.client.data.DataSourceField;
import com.smartgwt.client.data.fields.DataSourceIntegerField;
import com.smartgwt.client.data.fields.DataSourceTextField;
import com.smartgwt.client.rpc.RPCResponse;
import com.smartgwt.client.widgets.grid.ListGridRecord;
import org.rhq.core.domain.alert.Alert;
import org.rhq.core.domain.criteria.AlertCriteria;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.gui.coregui.client.gwt.AlertGWTServiceAsync;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.util.RPCDataSource;

import java.util.Date;

/**
 * @author Ian Springer
 */
public class AlertDataSource extends RPCDataSource {
    private static final String NAME = "Alert";
    private static final DateTimeFormat DATE_TIME_FORMAT = DateTimeFormat.getMediumDateTimeFormat();

    private AlertGWTServiceAsync alertService = GWTServiceLookup.getAlertService();

    private static AlertDataSource INSTANCE;

    public static AlertDataSource getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new AlertDataSource();            
        }
        return INSTANCE;
    }

    protected AlertDataSource() {
        super(NAME);

        DataSourceField idDataField = new DataSourceIntegerField("id", "Id");
        idDataField.setPrimaryKey(true);
        idDataField.setHidden(true);

        DataSourceTextField nameField = new DataSourceTextField("name", "Name");

        DataSourceTextField priorityField = new DataSourceTextField("priority", "Priority");

        DataSourceTextField ctimeField = new DataSourceTextField("ctime", "Creation Time");

        setFields(idDataField, nameField, priorityField, ctimeField);
    }

    public void executeFetch(final String requestId, final DSRequest request, final DSResponse response) {
        final long start = System.currentTimeMillis();

        AlertCriteria criteria = new AlertCriteria();
        criteria.fetchAlertDefinition(true);

        PageControl pageControl;
        if (request.getStartRow() != null && request.getEndRow() != null) {
            pageControl = PageControl.getExplicitPageControl(request.getStartRow(),
                    request.getEndRow() - request.getStartRow());
        } else {
            pageControl = PageControl.getSingleRowInstance();
        }
        criteria.setPageControl(pageControl);

        this.alertService.findAlertsByCriteria(criteria, new AsyncCallback<PageList<Alert>>() {
            public void onFailure(Throwable caught) {
                Window.alert("Failed to fetch Alerts - cause: " + caught);
                System.err.println("Failed to fetch Alerts - cause: " + caught);
                response.setStatus(RPCResponse.STATUS_FAILURE);
                processResponse(requestId, response);
            }

            public void onSuccess(PageList<Alert> result) {
                System.out.println(result.size() + " Alerts fetched in: " + (System.currentTimeMillis() - start) + "ms");

                ListGridRecord[] records = new ListGridRecord[result.size()];
                for (int i = 0; i < result.size(); i++) {
                    Alert alert = result.get(i);
                    ListGridRecord record = new ListGridRecord();
                    record.setAttribute("id", alert.getId());
                    record.setAttribute("name", alert.getAlertDefinition().getName());
                    record.setAttribute("priority", alert.getAlertDefinition().getPriority().name());
                    record.setAttribute("ctime", DATE_TIME_FORMAT.format(new Date(alert.getCtime())));
                    records[i] = record;
                }

                response.setData(records);
                response.setTotalRows(result.getTotalSize());    // for paging to work we have to specify size of full result set
                processResponse(requestId, response);
            }
        });
    }
}