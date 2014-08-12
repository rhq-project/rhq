/*
 * RHQ Management Platform
 * Copyright (C) 2005-2014 Red Hat, Inc.
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
package org.rhq.coregui.client.inventory.groups;

import static org.rhq.coregui.client.inventory.groups.ResourceGroupDataSourceField.CATEGORY;
import static org.rhq.coregui.client.inventory.groups.ResourceGroupDataSourceField.DESCRIPTION;
import static org.rhq.coregui.client.inventory.groups.ResourceGroupDataSourceField.NAME;
import static org.rhq.coregui.client.inventory.groups.ResourceGroupDataSourceField.PLUGIN;
import static org.rhq.coregui.client.inventory.groups.ResourceGroupDataSourceField.TYPE;

import java.util.List;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.data.DSRequest;
import com.smartgwt.client.data.DSResponse;
import com.smartgwt.client.data.DataSourceField;
import com.smartgwt.client.data.Record;
import com.smartgwt.client.data.fields.DataSourceIntegerField;
import com.smartgwt.client.data.fields.DataSourceTextField;
import com.smartgwt.client.rpc.RPCResponse;
import com.smartgwt.client.widgets.grid.ListGridRecord;

import org.rhq.core.domain.criteria.ResourceGroupCriteria;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.resource.group.GroupCategory;
import org.rhq.core.domain.resource.group.ResourceGroup;
import org.rhq.core.domain.util.PageList;
import org.rhq.coregui.client.CoreGUI;
import org.rhq.coregui.client.gwt.GWTServiceLookup;
import org.rhq.coregui.client.gwt.ResourceGroupGWTServiceAsync;
import org.rhq.coregui.client.util.RPCDataSource;

/**
 * @author Greg Hinkle
 */
public class ResourceGroupsDataSource extends RPCDataSource<ResourceGroup, ResourceGroupCriteria> {

    public static final String FILTER_GROUP_IDS = "resourceGroupIds";

    private ResourceGroupGWTServiceAsync groupService = GWTServiceLookup.getResourceGroupService();

    private static ResourceGroupsDataSource INSTANCE;

    public static ResourceGroupsDataSource getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new ResourceGroupsDataSource();
        }
        return INSTANCE;
    }

    public ResourceGroupsDataSource() {
        super();
        List<DataSourceField> fields = addDataSourceFields();
        addFields(fields);
    }

    @Override
    protected List<DataSourceField> addDataSourceFields() {
        List<DataSourceField> fields = super.addDataSourceFields();

        DataSourceField idDataField = new DataSourceIntegerField("id", MSG.common_title_id(), 20);
        idDataField.setPrimaryKey(true);
        fields.add(idDataField);

        DataSourceTextField nameDataField = new DataSourceTextField(NAME.propertyName(), NAME.title(), 200);
        fields.add(nameDataField);

        DataSourceTextField descriptionDataField = new DataSourceTextField(DESCRIPTION.propertyName(),
            DESCRIPTION.title());
        fields.add(descriptionDataField);

        DataSourceTextField typeNameDataField = new DataSourceTextField(TYPE.propertyName(), TYPE.title());
        fields.add(typeNameDataField);

        DataSourceTextField pluginNameDataField = new DataSourceTextField(PLUGIN.propertyName(), PLUGIN.title());
        fields.add(pluginNameDataField);

        DataSourceTextField categoryDataField = new DataSourceTextField(CATEGORY.propertyName(), CATEGORY.title());
        fields.add(categoryDataField);

        return fields;
    }

    @Override
    public void executeFetch(final DSRequest request, final DSResponse response, final ResourceGroupCriteria criteria) {
        groupService.findResourceGroupsByCriteria(criteria, new AsyncCallback<PageList<ResourceGroup>>() {
            public void onFailure(Throwable caught) {
                CoreGUI.getErrorHandler().handleError(MSG.dataSource_resourceGroups_loadFailed(), caught);
                response.setStatus(RPCResponse.STATUS_FAILURE);
                processResponse(request.getRequestId(), response);
            }

            public void onSuccess(PageList<ResourceGroup> result) {
                response.setData(buildRecords(result));
                setPagingInfo(response, result);
                processResponse(request.getRequestId(), response);
            }
        });
    }

    @Override
    protected ResourceGroupCriteria getFetchCriteria(final DSRequest request) {
        ResourceGroupCriteria criteria = new ResourceGroupCriteria();

        criteria.addFilterId(getFilter(request, "id", Integer.class));
        criteria.addFilterName(getFilter(request, NAME.propertyName(), String.class));
        criteria.addFilterGroupCategory(getFilter(request, CATEGORY.propertyName(), GroupCategory.class));
        criteria.addFilterDownMemberCount(getFilter(request, "downMemberCount", Long.class));
        criteria.addFilterExplicitResourceIds(getFilter(request, "explicitResourceId", Integer.class));
        criteria.addFilterGroupDefinitionId(getFilter(request, "groupDefinitionId", Integer.class));
        criteria.setSearchExpression(getFilter(request, "search", String.class));
        criteria.addFilterIds(getArrayFilter(request, FILTER_GROUP_IDS, Integer.class));

        return criteria;
    }

    @Override
    public ResourceGroup copyValues(Record from) {
        Integer idAttrib = from.getAttributeAsInt("id");
        String nameAttrib = from.getAttribute(NAME.propertyName());
        String descriptionAttrib = from.getAttribute(DESCRIPTION.propertyName());
        String typeNameAttrib = from.getAttribute(TYPE.propertyName());
        String pluginNameAttrib = from.getAttribute(PLUGIN.propertyName());
        ResourceType rt = null;

        ResourceGroup rg = new ResourceGroup(nameAttrib);
        rg.setId(idAttrib);
        rg.setDescription(descriptionAttrib);
        if (null != typeNameAttrib) {
            rt = new ResourceType();
            rt.setName(typeNameAttrib);
            rt.setPlugin(pluginNameAttrib);
            rg.setResourceType(rt);
        }

        return rg;
    }

    @Override
    public ListGridRecord copyValues(ResourceGroup from) {
        ListGridRecord record = new ListGridRecord();
        record.setAttribute("group", from);
        record.setAttribute("id", from.getId());
        record.setAttribute(NAME.propertyName(), from.getName());
        record.setAttribute(DESCRIPTION.propertyName(), from.getDescription());
        record.setAttribute(CATEGORY.propertyName(), from.getGroupCategory().name());

        if (from.getResourceType() != null) {
            record.setAttribute("resourceType", from.getResourceType());
            record.setAttribute(TYPE.propertyName(), from.getResourceType().getName());
            record.setAttribute(PLUGIN.propertyName(), from.getResourceType().getPlugin());
        }

        return record;
    }

}