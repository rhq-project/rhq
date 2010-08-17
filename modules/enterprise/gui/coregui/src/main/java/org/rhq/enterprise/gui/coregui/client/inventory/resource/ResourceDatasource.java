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

import static org.rhq.enterprise.gui.coregui.client.inventory.resource.ResourceDataSourceField.AVAILABILITY;
import static org.rhq.enterprise.gui.coregui.client.inventory.resource.ResourceDataSourceField.CATEGORY;
import static org.rhq.enterprise.gui.coregui.client.inventory.resource.ResourceDataSourceField.DESCRIPTION;
import static org.rhq.enterprise.gui.coregui.client.inventory.resource.ResourceDataSourceField.NAME;
import static org.rhq.enterprise.gui.coregui.client.inventory.resource.ResourceDataSourceField.PLUGIN;
import static org.rhq.enterprise.gui.coregui.client.inventory.resource.ResourceDataSourceField.TYPE;

import java.util.List;
import java.util.Map;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.data.DSRequest;
import com.smartgwt.client.data.DSResponse;
import com.smartgwt.client.data.DataSourceField;
import com.smartgwt.client.data.fields.DataSourceImageField;
import com.smartgwt.client.data.fields.DataSourceIntegerField;
import com.smartgwt.client.data.fields.DataSourceTextField;
import com.smartgwt.client.rpc.RPCResponse;
import com.smartgwt.client.widgets.grid.ListGridRecord;

import org.rhq.core.domain.criteria.ResourceCriteria;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceCategory;
import org.rhq.core.domain.tagging.Tag;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.gwt.ResourceGWTServiceAsync;
import org.rhq.enterprise.gui.coregui.client.util.RPCDataSource;
import org.rhq.enterprise.gui.coregui.client.util.message.Message;

/**
 * @author Greg Hinkle
 */
public class ResourceDatasource extends RPCDataSource<Resource> {

    private String query;

    private ResourceGWTServiceAsync resourceService = GWTServiceLookup.getResourceService();

    public ResourceDatasource() {
        super();

        // TODO until http://code.google.com/p/smartgwt/issues/detail?id=490 is fixed always go to the server for data
        this.setAutoCacheAllData(false);
        this.setCacheAllData(false);

        DataSourceField idDataField = new DataSourceIntegerField("id", "ID", 20);
        idDataField.setPrimaryKey(true);

        DataSourceImageField iconField = new DataSourceImageField("icon");
        iconField.setImageURLPrefix("types/");

        DataSourceTextField nameDataField = new DataSourceTextField(NAME.propertyName(), NAME.title(), 200);
        nameDataField.setCanEdit(false);

        DataSourceTextField descriptionDataField = new DataSourceTextField(DESCRIPTION.propertyName(), DESCRIPTION
            .title());
        descriptionDataField.setCanEdit(false);

        DataSourceTextField typeNameDataField = new DataSourceTextField(TYPE.propertyName(), TYPE.title());
        DataSourceTextField pluginNameDataField = new DataSourceTextField(PLUGIN.propertyName(), PLUGIN.title());
        DataSourceTextField categoryDataField = new DataSourceTextField(CATEGORY.propertyName(), CATEGORY.title());

        DataSourceImageField availabilityDataField = new DataSourceImageField(AVAILABILITY.propertyName(), AVAILABILITY
            .title(), 20);

        availabilityDataField.setCanEdit(false);

        setFields(idDataField, iconField, nameDataField, descriptionDataField, typeNameDataField, pluginNameDataField,
            categoryDataField, availabilityDataField);
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public void executeFetch(final DSRequest request, final DSResponse response) {

        ResourceCriteria criteria = getFetchCriteria(request);

        resourceService.findResourcesByCriteria(criteria, new AsyncCallback<PageList<Resource>>() {
            public void onFailure(Throwable caught) {
                CoreGUI.getErrorHandler().handleError("Failed to fetch resource data", caught);
                response.setStatus(RPCResponse.STATUS_FAILURE);
                processResponse(request.getRequestId(), response);
            }

            public void onSuccess(PageList<Resource> result) {

                dataRetrieved(result, response, request);
            }
        });
    }

    @SuppressWarnings("unchecked")
    protected ResourceCriteria getFetchCriteria(final DSRequest request) {

        ResourceCriteria criteria = new ResourceCriteria();
        criteria.setPageControl(getPageControl(request));
        // TODO: This call is broken in 2.2, http://code.google.com/p/smartgwt/issues/detail?id=490
        // when using AdvancedCriteria
        Map<String, Object> criteriaMap = request.getCriteria().getValues();

        if (criteriaMap.get("parentId") != null) {
            criteria.addFilterParentResourceId(Integer.parseInt((String) criteriaMap.get("parentId")));
        }

        if (criteriaMap.get("id") != null) {
            criteria.addFilterId(Integer.parseInt(request.getCriteria().getAttribute("id")));
        }

        if (criteriaMap.get("resourceIds") != null) {
            int[] ids = request.getCriteria().getAttributeAsIntArray("resourceIds");
            Integer[] oids = new Integer[ids.length];
            for (int i = 0; i < ids.length; i++) {
                oids[i] = ids[i++];
            }
            criteria.addFilterIds(oids);
        }

        // Fetch member Resources of the group with the specified id.
        if (criteriaMap.get("groupId") != null) {
            int groupId = Integer.parseInt((String) criteriaMap.get("groupId"));
            criteria.addFilterImplicitGroupIds(groupId);
        }

        if (criteriaMap.get(NAME.propertyName()) != null) {
            criteria.addFilterName((String) criteriaMap.get(NAME.propertyName()));
        }

        if (criteriaMap.get(CATEGORY.propertyName()) != null) {
            criteria.addFilterResourceCategory(ResourceCategory.valueOf(((String) criteriaMap.get(CATEGORY
                .propertyName())).toUpperCase()));
        }

        if (criteriaMap.get(AVAILABILITY.propertyName()) != null) {
            criteria.addFilterCurrentAvailability(AvailabilityType.valueOf(((String) criteriaMap.get(AVAILABILITY
                .propertyName())).toUpperCase()));
        }

        if (criteriaMap.get(TYPE.propertyName()) != null) {
            criteria.addFilterResourceTypeId(Integer.parseInt(((String) criteriaMap.get(TYPE.propertyName()))));
        }

        if (criteriaMap.get(PLUGIN.propertyName()) != null) {
            criteria.addFilterPluginName((String) criteriaMap.get(PLUGIN.propertyName()));
        }

        if (criteriaMap.get("tag") != null) {
            criteria.addFilterTag((Tag) criteriaMap.get("tag"));
        }

        if (criteriaMap.get("tagNamespace") != null) {
            criteria.addFilterTagNamespace((String) criteriaMap.get("tagNamespace"));
        }

        if (criteriaMap.get("tagSemantic") != null) {
            criteria.addFilterTagSemantic((String) criteriaMap.get("tagSemantic"));
        }

        if (criteriaMap.get("tagName") != null) {
            criteria.addFilterTagName((String) criteriaMap.get("tagName"));
        }

        return criteria;
    }

    @Override
    protected void executeRemove(final DSRequest request, final DSResponse response) {
        JavaScriptObject data = request.getData();
        final ListGridRecord rec = new ListGridRecord(data);
        final Resource resourceToDelete = copyValues(rec);

        final int resourceId = resourceToDelete.getId();
        resourceService.uninventoryResources(new int[] { resourceId }, new AsyncCallback<List<Integer>>() {
            public void onFailure(Throwable caught) {
                CoreGUI.getErrorHandler().handleError("Failed to uninventory resource " + resourceId, caught);
                response.setStatus(DSResponse.STATUS_FAILURE);
                processResponse(request.getRequestId(), response);
            }

            public void onSuccess(List<Integer> result) {
                CoreGUI.getMessageCenter().notify(
                    new Message("Resource [" + resourceId + "] successfully uninventoried.", Message.Severity.Info));
                response.setStatus(DSResponse.STATUS_SUCCESS);
                processResponse(request.getRequestId(), response);
            }
        });

    }

    protected void dataRetrieved(PageList<Resource> result, DSResponse response, DSRequest request) {
        ListGridRecord[] records = buildRecords(result);
        response.setData(records);
        response.setTotalRows(result.getTotalSize()); // for paging to work we have to specify size of full result set
        processResponse(request.getRequestId(), response);
    }

    @Override
    public Resource copyValues(ListGridRecord from) {
        return new Resource(from.getAttributeAsInt("id"));
    }

    @Override
    public ListGridRecord copyValues(Resource from) {
        ListGridRecord record = new ListGridRecord();
        record.setAttribute("resource", from);
        record.setAttribute("id", from.getId());
        record.setAttribute(NAME.propertyName(), from.getName());
        record.setAttribute(DESCRIPTION.propertyName(), from.getDescription());
        record.setAttribute(TYPE.propertyName(), from.getResourceType().getId());
        record.setAttribute(PLUGIN.propertyName(), from.getResourceType().getPlugin());
        record.setAttribute(CATEGORY.propertyName(), from.getResourceType().getCategory().getDisplayName());
        record.setAttribute("icon", from.getResourceType().getCategory().getDisplayName() + "_"
            + (from.getCurrentAvailability().getAvailabilityType() == AvailabilityType.UP ? "up" : "down") + "_16.png");

        record
            .setAttribute(
                "currentAvailability",
                from.getCurrentAvailability().getAvailabilityType() == AvailabilityType.UP ? "/images/icons/availability_green_16.png"
                    : "/images/icons/availability_red_16.png");

        return record;
    }
}
