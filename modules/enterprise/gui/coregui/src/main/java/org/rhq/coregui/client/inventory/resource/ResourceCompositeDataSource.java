/*
 * RHQ Management Platform
 * Copyright (C) 2005-2011 Red Hat, Inc.
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
package org.rhq.coregui.client.inventory.resource;

import static org.rhq.coregui.client.inventory.resource.ResourceDataSourceField.AVAILABILITY;
import static org.rhq.coregui.client.inventory.resource.ResourceDataSourceField.CATEGORY;
import static org.rhq.coregui.client.inventory.resource.ResourceDataSourceField.CTIME;
import static org.rhq.coregui.client.inventory.resource.ResourceDataSourceField.DESCRIPTION;
import static org.rhq.coregui.client.inventory.resource.ResourceDataSourceField.INVENTORY_STATUS;
import static org.rhq.coregui.client.inventory.resource.ResourceDataSourceField.ITIME;
import static org.rhq.coregui.client.inventory.resource.ResourceDataSourceField.KEY;
import static org.rhq.coregui.client.inventory.resource.ResourceDataSourceField.LOCATION;
import static org.rhq.coregui.client.inventory.resource.ResourceDataSourceField.MODIFIER;
import static org.rhq.coregui.client.inventory.resource.ResourceDataSourceField.MTIME;
import static org.rhq.coregui.client.inventory.resource.ResourceDataSourceField.NAME;
import static org.rhq.coregui.client.inventory.resource.ResourceDataSourceField.PARENT_INVENTORY_STATUS;
import static org.rhq.coregui.client.inventory.resource.ResourceDataSourceField.PLUGIN;
import static org.rhq.coregui.client.inventory.resource.ResourceDataSourceField.TYPE;
import static org.rhq.coregui.client.inventory.resource.ResourceDataSourceField.VERSION;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.data.DSRequest;
import com.smartgwt.client.data.DSResponse;
import com.smartgwt.client.data.DataSourceField;
import com.smartgwt.client.data.Record;
import com.smartgwt.client.rpc.RPCResponse;
import com.smartgwt.client.widgets.grid.ListGridRecord;

import org.rhq.core.domain.criteria.ResourceCriteria;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.domain.resource.InventoryStatus;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceCategory;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.resource.composite.ResourceComposite;
import org.rhq.core.domain.util.PageList;
import org.rhq.core.domain.util.ResourceTypeUtility;
import org.rhq.coregui.client.CoreGUI;
import org.rhq.coregui.client.ImageManager;
import org.rhq.coregui.client.gwt.GWTServiceLookup;
import org.rhq.coregui.client.gwt.ResourceGWTServiceAsync;
import org.rhq.coregui.client.inventory.resource.type.ResourceTypeRepository;
import org.rhq.coregui.client.inventory.resource.type.ResourceTypeRepository.TypesLoadedCallback;
import org.rhq.coregui.client.util.RPCDataSource;
import org.rhq.coregui.client.util.message.Message;

/**
 * A DataSource, basically the same as ResourceDatasource in the fields it defines, but that works with
 * ResourceComposite as opposed to Resource records.  In this way the Records can provide additional info,
 * like the user's resource permissions, for the resources. 
 *  
 * @author Jay Shaughnessy
 */
public class ResourceCompositeDataSource extends RPCDataSource<ResourceComposite, ResourceCriteria> {

    private static ResourceCompositeDataSource INSTANCE;

    private ResourceGWTServiceAsync resourceService = GWTServiceLookup.getResourceService();

    public static ResourceCompositeDataSource getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new ResourceCompositeDataSource();
        }
        return INSTANCE;
    }

    public ResourceCompositeDataSource() {
        super();
        List<DataSourceField> fields = addDataSourceFields();
        addFields(fields);
    }

    // Defined the same way as ResourceDatasource 
    @Override
    protected List<DataSourceField> addDataSourceFields() {
        List<DataSourceField> fields = super.addDataSourceFields();

        return ResourceDatasource.addResourceDatasourceFields(fields);
    }

    public void executeFetch(final DSRequest request, final DSResponse response, final ResourceCriteria criteria) {
        getResourceService().findResourceCompositesByCriteria(criteria,
            new AsyncCallback<PageList<ResourceComposite>>() {
                public void onFailure(Throwable caught) {
                    if (caught.getMessage().contains("SearchExpressionException")) {
                        Message message = new Message("Invalid search expression.", Message.Severity.Error);
                        CoreGUI.getMessageCenter().notify(message);
                    } else {
                        CoreGUI.getErrorHandler().handleError(MSG.view_inventory_resources_loadFailed(), caught);
                    }
                    response.setStatus(RPCResponse.STATUS_FAILURE);
                    processResponse(request.getRequestId(), response);
                }

                public void onSuccess(PageList<ResourceComposite> result) {
                    dataRetrieved(result, response, request);
                }
            });
    }

    protected void dataRetrieved(final PageList<ResourceComposite> result, final DSResponse response,
        final DSRequest request) {
        Set<Integer> typesSet = new HashSet<Integer>();
        Set<String> ancestries = new HashSet<String>();
        List<Resource> resources = new ArrayList<Resource>(result.size());
        for (ResourceComposite resourceComposite : result) {
            Resource resource = resourceComposite.getResource();
            resources.add(resource);
            ResourceType type = resource.getResourceType();
            if (type != null) {
                typesSet.add(type.getId());
            }
            ancestries.add(resource.getAncestry());
        }

        // In addition to the types of the result resources, get the types of their ancestry
        // NOTE: this may be too labor intensive in general, but since this is a singleton I couldn't
        //       make it easily optional.
        typesSet.addAll(AncestryUtil.getAncestryTypeIds(ancestries));

        ResourceTypeRepository typeRepo = ResourceTypeRepository.Cache.getInstance();
        typeRepo.getResourceTypes(typesSet.toArray(new Integer[typesSet.size()]), new TypesLoadedCallback() {
            @Override
            public void onTypesLoaded(Map<Integer, ResourceType> types) {
                // SmartGWT has issues storing a Map as a ListGridRecord attribute. Wrap it in a pojo.
                AncestryUtil.MapWrapper typesWrapper = new AncestryUtil.MapWrapper(types);

                Record[] records = buildRecords(result);
                for (Record record : records) {
                    // replace type id with type name
                    Integer typeId = record.getAttributeAsInt(TYPE.propertyName());
                    ResourceType type = types.get(typeId);
                    if (type != null) {
                        record.setAttribute(TYPE.propertyName(), ResourceTypeUtility.displayName(type));
                    }

                    // To avoid a lot of unnecessary String construction, be lazy about building ancestry hover text.
                    // Store the types map off the records so we can build a detailed hover string as needed.                      
                    record.setAttribute(AncestryUtil.RESOURCE_ANCESTRY_TYPES, typesWrapper);

                    // Build the decoded ancestry Strings now for display
                    record.setAttribute(AncestryUtil.RESOURCE_ANCESTRY_VALUE, AncestryUtil.getAncestryValue(record));
                }
                response.setData(records);
                setPagingInfo(response, result);
                processResponse(request.getRequestId(), response);
            }
        });
    }

    @Override
    protected ResourceCriteria getFetchCriteria(final DSRequest request) {
        ResourceCriteria criteria = new ResourceCriteria();

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

        // we never want to filter on null status - that would return resources for every status (committed, new, deleted, etc).
        // we want to rely on whatever the default setting is for the criteria and only override that if we explicitly have a status to filter.
        InventoryStatus invStatusFilter = getFilter(request, INVENTORY_STATUS.propertyName(), InventoryStatus.class);
        if (invStatusFilter != null) {
            criteria.addFilterInventoryStatus(invStatusFilter);
        }

        InventoryStatus parentInvStatusFilter = getFilter(request, PARENT_INVENTORY_STATUS.propertyName(),
            InventoryStatus.class);
        if (parentInvStatusFilter != null) {
            List<InventoryStatus> statuses = new ArrayList<InventoryStatus>(1);
            statuses.add(parentInvStatusFilter);
            criteria.addFilterParentInventoryStatuses(statuses);
        }

        return criteria;
    }

    @Override
    public ResourceComposite copyValues(Record from) {
        // not very strong...
        return new ResourceComposite(new Resource(from.getAttributeAsInt("id")), AvailabilityType.DOWN);
    }

    @Override
    public ListGridRecord copyValues(ResourceComposite from) {
        Resource res = from.getResource();
        ListGridRecord record = new ListGridRecord();
        record.setAttribute("resourceComposite", from);
        record.setAttribute("resource", res);
        record.setAttribute("id", res.getId());
        record.setAttribute(NAME.propertyName(), res.getName());
        record.setAttribute(KEY.propertyName(), res.getResourceKey());
        record.setAttribute(DESCRIPTION.propertyName(), res.getDescription());
        record.setAttribute(LOCATION.propertyName(), res.getLocation());
        record.setAttribute(TYPE.propertyName(), res.getResourceType().getId());
        record.setAttribute(PLUGIN.propertyName(), res.getResourceType().getPlugin());
        record.setAttribute(VERSION.propertyName(), res.getVersion());
        record.setAttribute(CATEGORY.propertyName(), res.getResourceType().getCategory().name());
        record.setAttribute("icon", ImageManager.getResourceIcon(res.getResourceType().getCategory(), res
            .getCurrentAvailability().getAvailabilityType()));
        record.setAttribute(AVAILABILITY.propertyName(),
            ImageManager.getAvailabilityIconFromAvailType(res.getCurrentAvailability().getAvailabilityType()));
        record.setAttribute(CTIME.propertyName(), res.getCtime());
        record.setAttribute(ITIME.propertyName(), res.getItime());
        record.setAttribute(MTIME.propertyName(), res.getMtime());
        record.setAttribute(MODIFIER.propertyName(), res.getModifiedBy());
        record.setAttribute(INVENTORY_STATUS.propertyName(), res.getInventoryStatus());

        record.setAttribute("resourcePermission", from.getResourcePermission());

        // for ancestry handling       
        record.setAttribute(AncestryUtil.RESOURCE_ANCESTRY, res.getAncestry());
        record.setAttribute(AncestryUtil.RESOURCE_TYPE_ID, res.getResourceType().getId());

        return record;
    }

    public ResourceGWTServiceAsync getResourceService() {
        return resourceService;
    }

}
