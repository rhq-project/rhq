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
import static org.rhq.coregui.client.inventory.resource.ResourceDataSourceField.TYPE_ID;
import static org.rhq.coregui.client.inventory.resource.ResourceDataSourceField.VERSION;

import java.util.ArrayList;
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
import org.rhq.core.domain.resource.InventoryStatus;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceCategory;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.core.domain.util.ResourceTypeUtility;
import org.rhq.coregui.client.CoreGUI;
import org.rhq.coregui.client.ImageManager;
import org.rhq.coregui.client.gwt.GWTServiceLookup;
import org.rhq.coregui.client.gwt.ResourceGWTServiceAsync;
import org.rhq.coregui.client.inventory.resource.type.ResourceTypeRepository;
import org.rhq.coregui.client.inventory.resource.type.ResourceTypeRepository.TypesLoadedCallback;
import org.rhq.coregui.client.util.Log;
import org.rhq.coregui.client.util.RPCDataSource;
import org.rhq.coregui.client.util.message.Message;

/**
 * @author Greg Hinkle
 */
public class ResourceDatasource extends RPCDataSource<Resource, ResourceCriteria> {

    public static final String FILTER_GROUP_ID = "groupId";
    public static final String FILTER_RESOURCE_IDS = "resourceIds";
    public static final String FILTER_PARENT_CATEGORY = "parentCategory";

    private static ResourceDatasource INSTANCE;

    private ResourceGWTServiceAsync resourceService = GWTServiceLookup.getResourceService();

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

        return addResourceDatasourceFields(fields);
    }

    public static List<DataSourceField> addResourceDatasourceFields(List<DataSourceField> fields) {
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

        DataSourceTextField keyDataField = new DataSourceTextField(KEY.propertyName(), KEY.title(), 200);
        keyDataField.setCanEdit(false);
        keyDataField.setDetail(true);
        fields.add(keyDataField);

        DataSourceTextField descriptionDataField = new DataSourceTextField(DESCRIPTION.propertyName(),
            DESCRIPTION.title());
        descriptionDataField.setCanEdit(false);
        fields.add(descriptionDataField);

        DataSourceTextField locationDataField = new DataSourceTextField(LOCATION.propertyName(), LOCATION.title());
        locationDataField.setCanEdit(false);
        locationDataField.setDetail(true);
        fields.add(locationDataField);

        DataSourceTextField typeNameDataField = new DataSourceTextField(TYPE.propertyName(), TYPE.title());
        fields.add(typeNameDataField);

        DataSourceTextField pluginNameDataField = new DataSourceTextField(PLUGIN.propertyName(), PLUGIN.title());
        pluginNameDataField.setDetail(true);
        fields.add(pluginNameDataField);

        DataSourceTextField versionDataField = new DataSourceTextField(VERSION.propertyName(), VERSION.title());
        fields.add(versionDataField);

        DataSourceTextField categoryDataField = new DataSourceTextField(CATEGORY.propertyName(), CATEGORY.title());
        // The icon field will show the category, no need to make the category field visible by default.
        categoryDataField.setDetail(true);
        fields.add(categoryDataField);

        DataSourceImageField availabilityDataField = new DataSourceImageField(AVAILABILITY.propertyName(),
            AVAILABILITY.title(), 20);
        availabilityDataField.setCanEdit(false);
        fields.add(availabilityDataField);

        DataSourceTextField inventoryStatusDataField = new DataSourceTextField(INVENTORY_STATUS.propertyName(),
            INVENTORY_STATUS.title());
        inventoryStatusDataField.setDetail(true);
        fields.add(inventoryStatusDataField);
        
        DataSourceTextField ctimeDataField = new DataSourceTextField(CTIME.propertyName(), CTIME.title());
        ctimeDataField.setDetail(true);
        fields.add(ctimeDataField);

        DataSourceTextField itimeDataField = new DataSourceTextField(ITIME.propertyName(), ITIME.title());
        itimeDataField.setDetail(true);
        fields.add(itimeDataField);

        DataSourceTextField mtimeDataField = new DataSourceTextField(MTIME.propertyName(), MTIME.title());
        mtimeDataField.setDetail(true);
        fields.add(mtimeDataField);

        DataSourceTextField modifiedByDataField = new DataSourceTextField(MODIFIER.propertyName(), MODIFIER.title());
        modifiedByDataField.setDetail(true);
        fields.add(modifiedByDataField);

        return fields;
    }

    public void executeFetch(final DSRequest request, final DSResponse response, final ResourceCriteria criteria) {
        resourceService.findResourcesByCriteria(criteria, new AsyncCallback<PageList<Resource>>() {
            public void onFailure(Throwable caught) {
                if (caught.getMessage().contains("SearchExpressionException")) {
                    Message message = new Message("Invalid search expression.", Message.Severity.Error);
                    CoreGUI.getMessageCenter().notify(message);
                } else if (caught.getMessage().contains("PageList was passed an empty collection")) {
                    // Because of bug 773626
                    Log.warn(caught.getMessage());
                    criteria.setPageControl(new PageControl(0, getDataPageSize()));
                    executeFetch(request, response, criteria);
                    return;
                } else {
                    CoreGUI.getErrorHandler().handleError(MSG.view_inventory_resources_loadFailed(), caught);
                }
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
        HashSet<String> ancestries = new HashSet<String>();
        for (Resource resource : result) {
            ResourceType type = resource.getResourceType();
            if (type != null) {
                typesSet.add(type.getId());
            }
            ancestries.add(resource.getAncestry());
        }

        // In addition to the types of the result resources, get the types of their ancestry
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
                        record.setAttribute(TYPE_ID.propertyName(), type.getId());
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

        printRequestCriteria(request);
        criteria.addFilterId(getFilter(request, "id", Integer.class));
        criteria.addFilterParentResourceId(getFilter(request, "parentId", Integer.class));
        criteria.addFilterCurrentAvailability(getFilter(request, AVAILABILITY.propertyName(), AvailabilityType.class));
        criteria.addFilterResourceCategories(getArrayFilter(request, CATEGORY.propertyName(), ResourceCategory.class));
        criteria.addFilterIds(getArrayFilter(request, FILTER_RESOURCE_IDS, Integer.class));
        criteria.addFilterExplicitGroupIds(getFilter(request, FILTER_GROUP_ID, Integer.class));
        criteria.addFilterName(getFilter(request, NAME.propertyName(), String.class));
        criteria.addFilterResourceTypeId(getFilter(request, TYPE.propertyName(), Integer.class));
        criteria.addFilterPluginName(getFilter(request, PLUGIN.propertyName(), String.class));
        criteria.addFilterTagNamespace(getFilter(request, "tagNamespace", String.class));
        criteria.addFilterTagSemantic(getFilter(request, "tagSemantic", String.class));
        criteria.addFilterTagName(getFilter(request, "tagName", String.class));
        criteria.addFilterVersion(getFilter(request, "version", String.class));
        criteria.addFilterParentResourceCategory(getFilter(request, FILTER_PARENT_CATEGORY, ResourceCategory.class));

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

        //@todo: Remove me when finished debugging search expression
        Log.debug(" *** ResourceCriteria Search String: " + getFilter(request, "search", String.class));
        criteria.setSearchExpression(getFilter(request, "search", String.class));

        // filter out unsortable fields (i.e. fields sorted client-side only)
        PageControl pageControl = getPageControl(request);
        pageControl.removeOrderingField(AncestryUtil.RESOURCE_ANCESTRY);
        criteria.setPageControl(pageControl);

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
        record.setAttribute(KEY.propertyName(), from.getResourceKey());
        record.setAttribute(DESCRIPTION.propertyName(), from.getDescription());
        record.setAttribute(LOCATION.propertyName(), from.getLocation());
        record.setAttribute(TYPE.propertyName(), from.getResourceType().getId());
        record.setAttribute(PLUGIN.propertyName(), from.getResourceType().getPlugin());
        record.setAttribute(VERSION.propertyName(), from.getVersion());
        record.setAttribute(CATEGORY.propertyName(), from.getResourceType().getCategory().name());
        record.setAttribute("icon", ImageManager.getResourceIcon(from.getResourceType().getCategory(), from
            .getCurrentAvailability().getAvailabilityType()));
        record.setAttribute(AVAILABILITY.propertyName(),
            ImageManager.getAvailabilityIconFromAvailType(from.getCurrentAvailability().getAvailabilityType()));
        record.setAttribute(CTIME.propertyName(), from.getCtime());
        record.setAttribute(ITIME.propertyName(), from.getItime());
        record.setAttribute(MTIME.propertyName(), from.getMtime());
        record.setAttribute(MODIFIER.propertyName(), from.getModifiedBy());
        record.setAttribute(INVENTORY_STATUS.propertyName(), from.getInventoryStatus().name());

        record.setAttribute(AncestryUtil.RESOURCE_ANCESTRY, from.getAncestry());
        record.setAttribute(AncestryUtil.RESOURCE_TYPE_ID, from.getResourceType().getId());

        return record;
    }

    public ResourceGWTServiceAsync getResourceService() {
        return resourceService;
    }
}
