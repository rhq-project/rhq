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
package org.rhq.enterprise.gui.coregui.client.inventory.resource;

import org.rhq.core.domain.criteria.ResourceCriteria;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.gwt.ResourceGWTServiceAsync;
import org.rhq.enterprise.gui.coregui.client.util.RPCDataSource;

import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.data.DSRequest;
import com.smartgwt.client.data.DSResponse;
import com.smartgwt.client.data.DataSourceField;
import com.smartgwt.client.data.fields.DataSourceImageField;
import com.smartgwt.client.data.fields.DataSourceIntegerField;
import com.smartgwt.client.data.fields.DataSourceTextField;
import com.smartgwt.client.rpc.RPCResponse;
import com.smartgwt.client.widgets.grid.ListGridRecord;


/**
 * @author Greg Hinkle
 */
public class ResourceDatasource extends RPCDataSource {

    private String query;

    private ResourceGWTServiceAsync resourceService = GWTServiceLookup.getResourceService();


    public ResourceDatasource() {

        DataSourceField idDataField = new DataSourceIntegerField("id", "ID", 20);
        idDataField.setPrimaryKey(true);
        
        DataSourceTextField nameDataField = new DataSourceTextField("name", "Name", 200);
        nameDataField.setCanEdit(false);

        DataSourceTextField descriptionDataField = new DataSourceTextField("description", "Description");
        descriptionDataField.setCanEdit(false);

        DataSourceImageField availabilityDataField = new DataSourceImageField("currentAvailability", "Availability", 20);
        availabilityDataField.setCanEdit(false);

        setFields(idDataField, nameDataField, descriptionDataField, availabilityDataField);
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }


    public void executeFetch(final String requestId, final DSRequest request, final DSResponse response) {
        final long start = System.currentTimeMillis();

        ResourceCriteria criteria = new ResourceCriteria();
        criteria.setPageControl(getPageControl(request));
        criteria.addFilterName(query);


        resourceService.findResourcesByCriteria(criteria, new AsyncCallback<PageList<Resource>>() {
            public void onFailure(Throwable caught) {
                Window.alert("Failed to load " + caught.getMessage());
                System.err.println("Failed to fetch Resource Data");
                response.setStatus(RPCResponse.STATUS_FAILURE);
                processResponse(requestId, response);                
            }

            public void onSuccess(PageList<Resource> result) {

                System.out.println("Data retrieved in: " + (System.currentTimeMillis() - start));

                ListGridRecord[] records = new ListGridRecord[result.size()];
                for (int x=0; x<result.size(); x++) {
                    Resource res = result.get(x);
                    ListGridRecord record = new ListGridRecord();
                    record.setAttribute("resouce",res);
                    record.setAttribute("id",res.getId());
                    record.setAttribute("name",res.getName());
                    record.setAttribute("description",res.getDescription());
                    record.setAttribute("currentAvailability",
                            res.getCurrentAvailability().getAvailabilityType() == AvailabilityType.UP
                            ? "/images/icons/availability_green_16.png" 
                            : "/images/icons/availability_red_16.png");
                    records[x] = record;
                }

                response.setData(records);
                response.setTotalRows(result.getTotalSize());	// for paging to work we have to specify size of full result set
                processResponse(requestId, response);
            }
        });
    }
}