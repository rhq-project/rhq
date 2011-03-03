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

import java.util.HashSet;
import java.util.List;
import java.util.Map;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.data.DSRequest;
import com.smartgwt.client.data.DSResponse;
import com.smartgwt.client.data.DataSourceField;
import com.smartgwt.client.data.Record;
import com.smartgwt.client.data.fields.DataSourceImageField;
import com.smartgwt.client.data.fields.DataSourceIntegerField;
import com.smartgwt.client.data.fields.DataSourceTextField;
import com.smartgwt.client.rpc.RPCResponse;
import com.smartgwt.client.widgets.grid.ListGridRecord;

import org.rhq.core.domain.criteria.ResourceCriteria;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceCategory;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.ImageManager;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.gwt.ResourceGWTServiceAsync;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.type.ResourceTypeRepository;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.type.ResourceTypeRepository.TypesLoadedCallback;
import org.rhq.enterprise.gui.coregui.client.util.RPCDataSource;

/**
 * @author Greg Hinkle
 */
public class ResourceDatasource extends RPCDataSource<Resource> {

    public static final String FILTER_GROUP_ID = "groupId";

    private ResourceGWTServiceAsync resourceService = GWTServiceLookup.getResourceService();

    private static ResourceDatasource INSTANCE;

    public static ResourceDatasource getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new ResourceDatasource();
        }
        return INSTANCE;
    }

    public ResourceDatasource() {
        super();
        List<DataSourceField> fields = addDataSourceFields();
        addFields(fields);
    }

    @Override
    protected List<DataSourceField> addDataSourceFields() {
        List<DataSourceField> fields = super.addDataSourceFields();

        DataSourceField idDataField = new DataSourceIntegerField("id", MSG.common_title_id(), 50);
        idDataField.setPrimaryKey(true);
        idDataField.setCanEdit(false);
        fields.add(idDataField);

        DataSourceImageField iconField = new DataSourceImageField("icon", " ");
        iconField.setWidth(25);
        fields.add(iconField);

        DataSourceTextField nameDataField = new DataSourceTextField(NAME.propertyName(), NAME.title(), 200);
        nameDataField.setCanEdit(false);
        fields.add(nameDataField);

        DataSourceTextField descriptionDataField = new DataSourceTextField(DESCRIPTION.propertyName(), DESCRIPTION
            .title());
        descriptionDataField.setCanEdit(false);
        fields.add(descriptionDataField);

        DataSourceTextField typeNameDataField = new DataSourceTextField(TYPE.propertyName(), TYPE.title());
        fields.add(typeNameDataField);

        DataSourceTextField pluginNameDataField = new DataSourceTextField(PLUGIN.propertyName(), PLUGIN.title());
        fields.add(pluginNameDataField);

        DataSourceTextField categoryDataField = new DataSourceTextField(CATEGORY.propertyName(), CATEGORY.title());
        fields.add(categoryDataField);

        DataSourceImageField availabilityDataField = new DataSourceImageField(AVAILABILITY.propertyName(), AVAILABILITY
            .title(), 20);
        availabilityDataField.setCanEdit(false);
        fields.add(availabilityDataField);

        return fields;
    }

    public void executeFetch(final DSRequest request, final DSResponse response) {
        ResourceCriteria criteria = getFetchCriteria(request);

        resourceService.findResourcesByCriteria(criteria, new AsyncCallback<PageList<Resource>>() {
            public void onFailure(Throwable caught) {
                CoreGUI.getErrorHandler().handleError(MSG.view_inventory_resources_loadFailed(), caught);
                response.setStatus(RPCResponse.STATUS_FAILURE);
                processResponse(request.getRequestId(), response);
            }

            public void onSuccess(PageList<Resource> result) {
                dataRetrieved(result, response, request);
            }
        });
    }

    protected void dataRetrieved(final PageList<Resource> result, final DSResponse response, final DSRequest request) {
        HashSet<Integer> typesSet = new HashSet<Integer>();
        for (Resource resource : result) {
            ResourceType type = resource.getResourceType();
            if (type != null) {
                typesSet.add(type.getId());
            }
        }

        ResourceTypeRepository typeRepo = ResourceTypeRepository.Cache.getInstance();
        typeRepo.getResourceTypes(typesSet.toArray(new Integer[typesSet.size()]), new TypesLoadedCallback() {
            @Override
            public void onTypesLoaded(Map<Integer, ResourceType> types) {
                Record[] records = buildRecords(result);
                for (Record record : records) {
                    Integer typeId = record.getAttributeAsInt(TYPE.propertyName());
                    ResourceType type = types.get(typeId);
                    if (type != null) {
                        record.setAttribute(TYPE.propertyName(), type.getName());
                    }
                }
                response.setData(records);
                response.setTotalRows(result.getTotalSize()); // for paging to work we have to specify size of full result set
                processResponse(request.getRequestId(), response);
            }
        });
    }

    protected ResourceCriteria getFetchCriteria(final DSRequest request) {
        ResourceCriteria criteria = new ResourceCriteria();
        criteria.setPageControl(getPageControl(request));
        //printRequestCriteria(request);
        criteria.addFilterId(getFilter(request, "id", Integer.class));
        criteria.addFilterParentResourceId(getFilter(request, "parentId", Integer.class));
        criteria.addFilterCurrentAvailability(getFilter(request, AVAILABILITY.propertyName(), AvailabilityType.class));
        criteria.addFilterResourceCategories(getArrayFilter(request, CATEGORY.propertyName(), ResourceCategory.class));
        criteria.addFilterIds(getArrayFilter(request, "resourceIds", Integer.class));
        criteria.addFilterImplicitGroupIds(getFilter(request, "groupId", Integer.class));
        criteria.addFilterName(getFilter(request, NAME.propertyName(), String.class));
        criteria.addFilterResourceTypeId(getFilter(request, TYPE.propertyName(), Integer.class));
        criteria.addFilterPluginName(getFilter(request, PLUGIN.propertyName(), String.class));
        criteria.addFilterTagNamespace(getFilter(request, "tagNamespace", String.class));
        criteria.addFilterTagSemantic(getFilter(request, "tagSemantic", String.class));
        criteria.addFilterTagName(getFilter(request, "tagName", String.class));
        criteria.addFilterVersion(getFilter(request, "version", String.class));
        criteria.setSearchExpression(getFilter(request, "search", String.class));

        return criteria;
    }

    @Override
    public Resource copyValues(Record from) {
        Resource resource = new Resource();

        resource.setId(from.getAttributeAsInt("id"));
        resource.setUuid(from.getAttributeAsString("uuid"));

        return resource;
    }

    @Override
    public ListGridRecord copyValues(Resource from) {
        ListGridRecord record = new ListGridRecord();

        record.setAttribute("resource", from);
        record.setAttribute("id", from.getId());
        record.setAttribute("uuid", from.getUuid());
        record.setAttribute(NAME.propertyName(), from.getName());
        record.setAttribute(DESCRIPTION.propertyName(), from.getDescription());
        record.setAttribute(TYPE.propertyName(), from.getResourceType().getId());
        record.setAttribute(PLUGIN.propertyName(), from.getResourceType().getPlugin());
        record.setAttribute(CATEGORY.propertyName(), from.getResourceType().getCategory().name());
        record.setAttribute("icon", ImageManager.getResourceIcon(from.getResourceType().getCategory(), from
            .getCurrentAvailability().getAvailabilityType() == AvailabilityType.UP));
        record.setAttribute(AVAILABILITY.propertyName(), ImageManager.getAvailabilityIconFromAvailType(from
            .getCurrentAvailability().getAvailabilityType()));

        return record;
    }

    public ResourceGWTServiceAsync getResourceService() {
        return resourceService;
    }
}
