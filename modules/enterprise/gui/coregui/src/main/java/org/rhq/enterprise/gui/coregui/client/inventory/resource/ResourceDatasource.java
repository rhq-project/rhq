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
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.gwt.ResourceGWTServiceAsync;

import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.data.DSRequest;
import com.smartgwt.client.data.DSResponse;
import com.smartgwt.client.data.DataSource;
import com.smartgwt.client.data.DataSourceField;
import com.smartgwt.client.data.fields.DataSourceImageField;
import com.smartgwt.client.data.fields.DataSourceIntegerField;
import com.smartgwt.client.data.fields.DataSourceTextField;
import com.smartgwt.client.rpc.RPCResponse;
import com.smartgwt.client.types.DSDataFormat;
import com.smartgwt.client.types.DSProtocol;
import com.smartgwt.client.widgets.grid.ListGridRecord;


/**
 * @author Greg Hinkle
 */
public class ResourceDatasource extends DataSource {
    private boolean initialized = false;


    private String query;

    private ResourceGWTServiceAsync resourceService = GWTServiceLookup.getResourceService();


    public ResourceDatasource() {
        setClientOnly(false);
        setDataProtocol(DSProtocol.CLIENTCUSTOM);
        setDataFormat(DSDataFormat.CUSTOM);

        DataSourceField idDataField = new DataSourceIntegerField("id", "ID", 20);
        idDataField.setPrimaryKey(true);
        
        DataSourceTextField nameDataField = new DataSourceTextField("name", "Name", 200);
        nameDataField.setCanEdit(false);

        DataSourceTextField descriptionDataField = new DataSourceTextField("description", "Description");
        descriptionDataField.setCanEdit(false);

        DataSourceImageField availabilityDataField = new DataSourceImageField("currentAvailability", "Availability", 20);
        availabilityDataField.setCanEdit(false);

//        nameDataField.setType(FieldType.);

        setFields(idDataField, nameDataField, descriptionDataField, availabilityDataField);
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    @Override
    protected Object transformRequest(DSRequest request) {
        String requestId = request.getRequestId();
        DSResponse response = new DSResponse();
        response.setAttribute("clientContext", request.getAttributeAsObject("clientContext"));
        // Asume success
        response.setStatus(0);
        switch (request.getOperationType()) {
            case ADD:
                //executeAdd(lstRec, true);
                break;
            case FETCH:
                executeFetch(requestId, request, response);
                break;
            case REMOVE:
                //executeRemove(lstRec);
                break;
            case UPDATE:
                //executeAdd(lstRec, false);
                break;

            default:
                break;
        }

        return request.getData();
    }

    public void executeFetch(final String requestId, final DSRequest request, final DSResponse response) {
        final long start = System.currentTimeMillis();


        ResourceCriteria criteria = new ResourceCriteria();
        criteria.addFilterName(query);
        criteria.setPageControl(PageControl.getExplicitPageControl(request.getStartRow(), request.getEndRow() - request.getStartRow()));

//        criteria.addSortAgentName(PageOrdering.ASC);


        resourceService.findResourcesByCriteria(criteria, new AsyncCallback<PageList<Resource>>() {
            public void onFailure(Throwable caught) {
                Window.alert("Failed to load " + caught.getMessage());
                System.err.println("Failed to fetch Resource Data");
                response.setStatus(RPCResponse.STATUS_FAILURE);
                processResponse(requestId, response);                
            }

            public void onSuccess(PageList<Resource> result) {
//                mainPanel.clear();
//
//                FlexTable table = new FlexTable();
//                mainPanel.add(table);
//
//                int r = 0;
//                int c = 0;
//
//                table.setText(r,c++,"id");
//                table.setText(r,c++,"name");
//                table.setText(r,c++,"description");
//                table.setText(r,c++,"type");
//                table.setText(r,c++,"availability");
//
//                mainPanel.add(table);

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


//                for (Resource res : result) {
//                    if (initialized) {
//                        updateData(new ResourceRow(res));
//                    } else {
//                        addData(new ResourceRow(res));
//                    }
//
//                    if (!initialized) initialized = true;
//                    r++;
//                    c=0;
//                    table.setText(r,c++, String.valueOf(res.getId()));
//                    table.setText(r,c++, res.getName());
//                    table.setText(r,c++, res.getDescription());
//                    table.setText(r,c++, String.valueOf(res.getResourceType().getId()));
//                    table.setText(r,c++, String.valueOf(res.getCurrentAvailability().getAvailabilityType().getName()));

//                }
            }
        });
    }

    private static class ResourceRow extends ListGridRecord {

        private Resource resource;

        private ResourceRow(Resource resource) {
            this.resource = resource;
//             DataTools.setProperties

        }


        public Resource getResource() {
            return resource;
        }

        public void setResource(Resource resource) {
            this.resource = resource;
        }

        
    }
}