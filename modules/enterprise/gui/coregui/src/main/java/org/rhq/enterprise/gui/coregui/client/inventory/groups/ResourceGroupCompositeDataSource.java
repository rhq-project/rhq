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
package org.rhq.enterprise.gui.coregui.client.inventory.groups;

import static org.rhq.enterprise.gui.coregui.client.inventory.groups.ResourceGroupDataSourceField.CATEGORY;
import static org.rhq.enterprise.gui.coregui.client.inventory.groups.ResourceGroupDataSourceField.NAME;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.data.DSRequest;
import com.smartgwt.client.data.DSResponse;
import com.smartgwt.client.data.fields.DataSourceTextField;
import com.smartgwt.client.rpc.RPCResponse;
import com.smartgwt.client.widgets.grid.ListGridRecord;

import org.rhq.core.domain.criteria.ResourceGroupCriteria;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.resource.group.GroupCategory;
import org.rhq.core.domain.resource.group.ResourceGroup;
import org.rhq.core.domain.resource.group.composite.ResourceGroupComposite;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.gwt.ResourceGroupGWTServiceAsync;
import org.rhq.enterprise.gui.coregui.client.util.RPCDataSource;

/**
 * @author Joseph Marques
 */
public class ResourceGroupCompositeDataSource extends RPCDataSource<ResourceGroupComposite> {

    ResourceGroupGWTServiceAsync groupService = GWTServiceLookup.getResourceGroupService();

    private static ResourceGroupCompositeDataSource INSTANCE;

    public static ResourceGroupCompositeDataSource getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new ResourceGroupCompositeDataSource();
        }
        return INSTANCE;
    }

    public ResourceGroupCompositeDataSource() {
        super();

        DataSourceTextField nameDataField = new DataSourceTextField("name", "Name", 200);
        nameDataField.setCanEdit(false);

        DataSourceTextField descriptionDataField = new DataSourceTextField("description", "Description");
        descriptionDataField.setCanEdit(false);

        DataSourceTextField typeNameDataField = new DataSourceTextField("typeName", "Type");
        DataSourceTextField pluginNameDataField = new DataSourceTextField("pluginName", "Plugin");
        DataSourceTextField categoryDataField = new DataSourceTextField("category", "Category");

        setFields(nameDataField, descriptionDataField, typeNameDataField, pluginNameDataField, categoryDataField);
    }

    public void executeFetch(final DSRequest request, final DSResponse response) {
        ResourceGroupCriteria criteria = getFetchCriteria(request);

        groupService.findResourceGroupCompositesByCriteria(criteria,
            new AsyncCallback<PageList<ResourceGroupComposite>>() {
                public void onFailure(Throwable caught) {
                    CoreGUI.getErrorHandler().handleError("Failed to fetch group composite data", caught);
                    response.setStatus(RPCResponse.STATUS_FAILURE);
                    processResponse(request.getRequestId(), response);
                }

                public void onSuccess(PageList<ResourceGroupComposite> result) {
                    response.setData(buildRecords(result));
                    response.setTotalRows(result.getTotalSize()); // for paging to work we have to specify size of full result set
                    processResponse(request.getRequestId(), response);
                }
            });
    }

    protected ResourceGroupCriteria getFetchCriteria(final DSRequest request) {
        ResourceGroupCriteria criteria = new ResourceGroupCriteria();
        criteria.setPageControl(getPageControl(request));

        criteria.addFilterName(getFilter(request, NAME.propertyName(), String.class));
        criteria.addFilterGroupCategory(getFilter(request, CATEGORY.propertyName(), GroupCategory.class));
        criteria.addFilterDownMemberCount(getFilter(request, "downMemberCount", Integer.class));
        criteria.addFilterExplicitResourceIds(getFilter(request, "explicitResourceId", Integer.class));
        criteria.addFilterGroupDefinitionId(getFilter(request, "groupDefinitionId", Integer.class));

        return criteria;
    }

    @Override
    public ResourceGroupComposite copyValues(ListGridRecord from) {
        Integer idAttrib = from.getAttributeAsInt("id");
        String nameAttrib = from.getAttribute("name");
        String descriptionAttrib = from.getAttribute("description");
        String typeNameAttrib = from.getAttribute("typeName");
        String pluginNameAttrib = from.getAttribute("pluginName");
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

        Long explicitUp = Long.valueOf(from.getAttribute("explicitUp"));
        Long explicitDown = Long.valueOf(from.getAttribute("explicitDown"));
        Long implicitUp = Long.valueOf(from.getAttribute("implicitUp"));
        Long implicitDown = Long.valueOf(from.getAttribute("implicitDown"));

        ResourceGroupComposite composite = new ResourceGroupComposite(explicitUp, explicitDown, implicitUp,
            implicitDown, rg);

        return composite;
    }

    @Override
    public ListGridRecord copyValues(ResourceGroupComposite from) {
        ListGridRecord record = new ListGridRecord();
        record.setAttribute("group", from);
        record.setAttribute("id", from.getResourceGroup().getId());
        record.setAttribute("name", from.getResourceGroup().getName());
        record.setAttribute("description", from.getResourceGroup().getDescription());
        record.setAttribute("category", from.getResourceGroup().getGroupCategory().toString());

        record.setAttribute("explicitUp", String.valueOf(from.getExplicitUp()));
        record.setAttribute("explicitDown", String.valueOf(from.getExplicitDown()));
        record.setAttribute("implicitUp", String.valueOf(from.getImplicitUp()));
        record.setAttribute("implicitDown", String.valueOf(from.getImplicitDown()));

        record.setAttribute("availabilityChildren", from.getExplicitFormatted());
        record.setAttribute("availabilityDescendents", from.getImplicitFormatted());

        if (from.getResourceGroup().getResourceType() != null) {
            record.setAttribute("resourceType", from.getResourceGroup().getResourceType());
            record.setAttribute("typeName", from.getResourceGroup().getResourceType().getName());
            record.setAttribute("pluginName", from.getResourceGroup().getResourceType().getPlugin());
        }

        return record;
    }
}
