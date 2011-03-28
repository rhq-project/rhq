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

package org.rhq.enterprise.gui.coregui.client.inventory.resource;

import static org.rhq.enterprise.gui.coregui.client.inventory.resource.ResourceDataSourceField.AVAILABILITY;
import static org.rhq.enterprise.gui.coregui.client.inventory.resource.ResourceDataSourceField.CATEGORY;
import static org.rhq.enterprise.gui.coregui.client.inventory.resource.ResourceDataSourceField.DESCRIPTION;
import static org.rhq.enterprise.gui.coregui.client.inventory.resource.ResourceDataSourceField.NAME;
import static org.rhq.enterprise.gui.coregui.client.inventory.resource.ResourceDataSourceField.PLUGIN;
import static org.rhq.enterprise.gui.coregui.client.inventory.resource.ResourceDataSourceField.TYPE;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.data.DSRequest;
import com.smartgwt.client.data.DSResponse;
import com.smartgwt.client.data.DataSourceField;
import com.smartgwt.client.data.Record;
import com.smartgwt.client.rpc.RPCResponse;
import com.smartgwt.client.widgets.grid.ListGridRecord;

import org.rhq.core.domain.criteria.ResourceCriteria;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceCategory;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.resource.composite.ResourceComposite;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.ImageManager;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.gwt.ResourceGWTServiceAsync;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.type.ResourceTypeRepository;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.type.ResourceTypeRepository.TypesLoadedCallback;
import org.rhq.enterprise.gui.coregui.client.util.RPCDataSource;

/**
 * A DataSource basically the same as ResourceDatasource in the fields it defines, but that works with
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
                    CoreGUI.getErrorHandler().handleError(MSG.view_inventory_resources_loadFailed(), caught);
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
        HashSet<Integer> typesSet = new HashSet<Integer>();
        HashSet<String> ancestries = new HashSet<String>();
        ArrayList<Resource> resources = new ArrayList<Resource>(result.size());
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
                // Smartgwt has issues storing a Map as a ListGridRecord attribute. Wrap it in a pojo.                
                AncestryUtil.MapWrapper typesWrapper = new AncestryUtil.MapWrapper(types);

                Record[] records = buildRecords(result);
                for (Record record : records) {
                    // replace type id with type name
                    Integer typeId = record.getAttributeAsInt(TYPE.propertyName());
                    ResourceType type = types.get(typeId);
                    if (type != null) {
                        record.setAttribute(TYPE.propertyName(), type.getName());
                    }

                    // To avoid a lot of unnecessary String construction, be lazy about building ancestry hover text.
                    // Store the types map off the records so we can build a detailed hover string as needed.                      
                    record.setAttribute(AncestryUtil.RESOURCE_ANCESTRY_TYPES, typesWrapper);

                    // Build the decoded ancestry Strings now for display
                    record.setAttribute(AncestryUtil.RESOURCE_ANCESTRY_VALUE, AncestryUtil.getAncestryValue(record));
                }
                response.setData(records);
                response.setTotalRows(result.getTotalSize()); // for paging to work we have to specify size of full result set
                processResponse(request.getRequestId(), response);
            }
        });
    }

    @Override
    protected ResourceCriteria getFetchCriteria(final DSRequest request) {
        ResourceCriteria criteria = new ResourceCriteria();
        criteria.setPageControl(getPageControl(request));

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
        record.setAttribute(DESCRIPTION.propertyName(), res.getDescription());
        record.setAttribute(TYPE.propertyName(), res.getResourceType().getId());
        record.setAttribute(PLUGIN.propertyName(), res.getResourceType().getPlugin());
        record.setAttribute(CATEGORY.propertyName(), res.getResourceType().getCategory().name());
        record.setAttribute("icon", ImageManager.getResourceIcon(res.getResourceType().getCategory(), res
            .getCurrentAvailability().getAvailabilityType() == AvailabilityType.UP));
        record.setAttribute(AVAILABILITY.propertyName(), ImageManager.getAvailabilityIconFromAvailType(res
            .getCurrentAvailability().getAvailabilityType()));

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
