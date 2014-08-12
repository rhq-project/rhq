/*
 * RHQ Management Platform
 * Copyright (C) 2005-2014 Red Hat, Inc.
 * All rights reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2, as
 * published by the Free Software Foundation, and/or the GNU Lesser
 * General Public License, version 2.1, also as published by the Free
 * Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License and the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License
 * and the GNU Lesser General Public License along with this program;
 * if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */
package org.rhq.coregui.client.inventory.resource.detail.monitoring;

import java.util.Iterator;
import java.util.List;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.data.DSRequest;
import com.smartgwt.client.data.DSResponse;
import com.smartgwt.client.data.DataSourceField;
import com.smartgwt.client.data.Record;
import com.smartgwt.client.data.fields.DataSourceIntegerField;
import com.smartgwt.client.data.fields.DataSourceTextField;
import com.smartgwt.client.widgets.grid.ListGridRecord;

import org.rhq.core.domain.criteria.Criteria;
import org.rhq.core.domain.criteria.MeasurementDefinitionCriteria;
import org.rhq.core.domain.criteria.ResourceCriteria;
import org.rhq.core.domain.criteria.ResourceGroupCriteria;
import org.rhq.core.domain.measurement.MeasurementDefinition;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.group.ResourceGroup;
import org.rhq.core.domain.util.PageList;
import org.rhq.core.domain.util.PageOrdering;
import org.rhq.coregui.client.CoreGUI;
import org.rhq.coregui.client.gwt.GWTServiceLookup;
import org.rhq.coregui.client.util.RPCDataSource;

/**
 * @author Greg Hinkle
 */
public class ResourceScheduledMetricDatasource extends RPCDataSource<MeasurementDefinition, Criteria> {

    public ResourceScheduledMetricDatasource() {
        List<DataSourceField> fields = addDataSourceFields();
        addFields(fields);
    }

    @Override
    protected List<DataSourceField> addDataSourceFields() {
        List<DataSourceField> fields = super.addDataSourceFields();

        DataSourceIntegerField id = new DataSourceIntegerField("id", MSG.common_title_id());
        id.setPrimaryKey(true);
        fields.add(id);

        DataSourceTextField name = new DataSourceTextField("name", MSG.common_title_name());
        fields.add(name);

        DataSourceTextField displayName = new DataSourceTextField("displayName", MSG.common_title_display_name());
        fields.add(displayName);

        DataSourceTextField description = new DataSourceTextField("description", MSG.common_title_description());
        fields.add(description);

        DataSourceTextField units = new DataSourceTextField("units", MSG.common_title_units());
        fields.add(units);

        DataSourceTextField numericType = new DataSourceTextField("numericType", MSG.common_title_numeric_type());
        fields.add(numericType);

        DataSourceTextField category = new DataSourceTextField("category", MSG.common_title_category());
        fields.add(category);

        return fields;
    }

    @Override
    protected void executeFetch(final DSRequest request, final DSResponse response, final Criteria unused) {

        // due to the conditional below which determines what kind of criteria to use, this datasource
        // doesn't rely on getFetchCriteria and the passed in criteria object. It is up to this method to
        // correctly prepare the criteria we build here (e.g. set the page control properly).
        if (request.getCriteria().getValues().containsKey("id")) {
            MeasurementDefinitionCriteria criteria = new MeasurementDefinitionCriteria();
            criteria.addFilterId(request.getCriteria().getAttributeAsInt("id"));
            criteria.setPageControl(getPageControl(request));

            GWTServiceLookup.getMeasurementDataService().findMeasurementDefinitionsByCriteria(criteria,
                new AsyncCallback<PageList<MeasurementDefinition>>() {
                    @Override
                    public void onFailure(Throwable caught) {
                        CoreGUI.getErrorHandler().handleError(MSG.dataSource_definitions_loadFailed(), caught);
                    }

                    @Override
                    public void onSuccess(PageList<MeasurementDefinition> result) {
                        response.setData(buildRecords(result));
                        setPagingInfo(response, result);
                        processResponse(request.getRequestId(), response);
                    }
                });

        } else if (request.getCriteria().getValues().containsKey("resourceId")) {
            ResourceCriteria rCriteria = new ResourceCriteria();
            rCriteria.addFilterId(request.getCriteria().getAttributeAsInt("resourceId"));
            rCriteria.fetchResourceType(true);

            GWTServiceLookup.getResourceService().findResourcesByCriteria(rCriteria,
                new AsyncCallback<PageList<Resource>>() {
                    public void onFailure(Throwable caught) {
                        CoreGUI.getErrorHandler().handleError(MSG.dataSource_schedules_loadFailed(), caught);
                    }

                    public void onSuccess(PageList<Resource> result) {
                        MeasurementDefinitionCriteria mdCriteria = new MeasurementDefinitionCriteria();
                        mdCriteria.addFilterResourceTypeId(result.get(0).getResourceType().getId());
                        mdCriteria.addSortDisplayName(PageOrdering.ASC);

                        GWTServiceLookup.getMeasurementDataService().findMeasurementDefinitionsByCriteria(mdCriteria,
                            new AsyncCallback<PageList<MeasurementDefinition>>() {
                                @Override
                                public void onFailure(Throwable caught) {
                                    CoreGUI.getErrorHandler()
                                        .handleError(MSG.dataSource_schedules_loadFailed(), caught);
                                }

                                @Override
                                public void onSuccess(PageList<MeasurementDefinition> result) {
                                    response.setData(buildRecords(filter(result)));
                                    setPagingInfo(response, result);
                                    processResponse(request.getRequestId(), response);
                                }
                            });
                    }
                });

        } else if (request.getCriteria().getValues().containsKey("resourceGroupId")) {
            ResourceGroupCriteria rgCriteria = new ResourceGroupCriteria();
            rgCriteria.addFilterId(request.getCriteria().getAttributeAsInt("resourceGroupId"));
            rgCriteria.fetchResourceType(true);

            GWTServiceLookup.getResourceGroupService().findResourceGroupsByCriteria(rgCriteria,
                new AsyncCallback<PageList<ResourceGroup>>() {
                    public void onFailure(Throwable caught) {
                        CoreGUI.getErrorHandler().handleError(MSG.dataSource_schedules_loadFailed(), caught);
                    }

                    public void onSuccess(PageList<ResourceGroup> result) {
                        MeasurementDefinitionCriteria mdCriteria = new MeasurementDefinitionCriteria();
                        mdCriteria.addFilterResourceTypeId(result.get(0).getResourceType().getId());
                        mdCriteria.addSortDisplayName(PageOrdering.ASC);

                        GWTServiceLookup.getMeasurementDataService().findMeasurementDefinitionsByCriteria(mdCriteria,
                            new AsyncCallback<PageList<MeasurementDefinition>>() {
                                @Override
                                public void onFailure(Throwable caught) {
                                    CoreGUI.getErrorHandler()
                                        .handleError(MSG.dataSource_schedules_loadFailed(), caught);
                                }

                                @Override
                                public void onSuccess(PageList<MeasurementDefinition> result) {
                                    response.setData(buildRecords(filter(result)));
                                    setPagingInfo(response, result);
                                    processResponse(request.getRequestId(), response);
                                }
                            });
                    }
                });

        } else {
            processResponse(request.getRequestId(), response);
        }
    }

    private List<MeasurementDefinition> filter(List<MeasurementDefinition> mds) {
        for (Iterator<MeasurementDefinition> i = mds.iterator(); i.hasNext();) {
            MeasurementDefinition md = i.next();
            // don't include the special avail metric
            if (md.getDisplayName().toLowerCase().equals("availability")) {
                i.remove();
                break;
            }
        }
        return mds;
    }

    @Override
    protected Criteria getFetchCriteria(DSRequest request) {
        // our executeFetch does some special conditional checking to determine what kind of criteria to use.
        // because of this, we don't explicitly use this method to get the criteria for this datasource, just return null
        return null;
    }

    @Override
    public MeasurementDefinition copyValues(Record from) {
        return null; // TODO: Implement this method.
    }

    @Override
    public ListGridRecord copyValues(MeasurementDefinition from) {
        ListGridRecord record = new ListGridRecord();

        record.setAttribute("id", from.getId());
        record.setAttribute("name", from.getName());
        record.setAttribute("displayName", from.getDisplayName());
        record.setAttribute("description", from.getDescription());
        record.setAttribute("units", from.getUnits().name());
        record.setAttribute("numericType", from.getNumericType().name());
        record.setAttribute("category", from.getCategory().name());
        return record;
    }
}
