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
import static org.rhq.enterprise.gui.coregui.client.inventory.groups.ResourceGroupDataSourceField.DESCRIPTION;
import static org.rhq.enterprise.gui.coregui.client.inventory.groups.ResourceGroupDataSourceField.NAME;
import static org.rhq.enterprise.gui.coregui.client.inventory.groups.ResourceGroupDataSourceField.PLUGIN;
import static org.rhq.enterprise.gui.coregui.client.inventory.groups.ResourceGroupDataSourceField.TYPE;

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
import org.rhq.core.domain.resource.group.composite.ResourceGroupComposite;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.ImageManager;
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

        return fields;
    }

    public void executeFetch(final DSRequest request, final DSResponse response) {
        ResourceGroupCriteria criteria = getFetchCriteria(request);

        groupService.findResourceGroupCompositesByCriteria(criteria,
            new AsyncCallback<PageList<ResourceGroupComposite>>() {
                public void onFailure(Throwable caught) {
                    CoreGUI.getErrorHandler().handleError(MSG.view_inventory_groups_loadFailed(), caught);
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
        criteria.addFilterDownMemberCount(getFilter(request, "downMemberCount", Long.class));
        criteria.addFilterExplicitResourceIds(getFilter(request, "explicitResourceId", Integer.class));
        criteria.addFilterGroupDefinitionId(getFilter(request, "groupDefinitionId", Integer.class));

        return criteria;
    }

    @Override
    public ResourceGroupComposite copyValues(Record from) {
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
        record.setAttribute(NAME.propertyName(), from.getResourceGroup().getName());
        record.setAttribute(DESCRIPTION.propertyName(), from.getResourceGroup().getDescription());
        record.setAttribute(CATEGORY.propertyName(), from.getResourceGroup().getGroupCategory().name());

        record.setAttribute("explicitUp", String.valueOf(from.getExplicitUp()));
        record.setAttribute("explicitDown", String.valueOf(from.getExplicitDown()));
        record.setAttribute("implicitUp", String.valueOf(from.getImplicitUp()));
        record.setAttribute("implicitDown", String.valueOf(from.getImplicitDown()));

        record.setAttribute("availabilityChildren", getExplicitFormatted(from));
        record.setAttribute("availabilityDescendents", getImplicitFormatted(from));

        if (from.getResourceGroup().getResourceType() != null) {
            record.setAttribute("resourceType", from.getResourceGroup().getResourceType());
            record.setAttribute(TYPE.propertyName(), from.getResourceGroup().getResourceType().getName());
            record.setAttribute(PLUGIN.propertyName(), from.getResourceGroup().getResourceType().getPlugin());
        }

        return record;
    }

    private String getExplicitFormatted(ResourceGroupComposite from) {
        return getAlignedAvailabilityResults(from.getExplicitUp(), from.getExplicitDown());
    }

    private String getImplicitFormatted(ResourceGroupComposite from) {
        return getAlignedAvailabilityResults(from.getImplicitUp(), from.getImplicitDown());
    }

    private String getAlignedAvailabilityResults(long up, long down) {
        StringBuilder results = new StringBuilder();
        results.append("<table width=\"120px\"><tr>");
        if (up == 0 && down == 0) {
            results.append(getColumn(false, "<img src=\""
                + ImageManager.getFullImagePath(ImageManager.getAvailabilityIcon(null)) + "\" /> 0"));
            results.append(getColumn(true));
            results.append(getColumn(false));
        } else {
            if (up > 0) {
                results.append(getColumn(false, " <img src=\""
                    + ImageManager.getFullImagePath(ImageManager.getAvailabilityIcon(Boolean.TRUE)) + "\" />", up));
            }

            if (up > 0 && down > 0) {
                results.append(getColumn(true)); // , " / ")); // use a vertical separator image if we want a separator
            }

            if (down > 0) {
                results.append(getColumn(false, " <img src=\""
                    + ImageManager.getFullImagePath(ImageManager.getAvailabilityIcon(Boolean.FALSE)) + "\" />", down));
            } else {
                results.append(getColumn(false,
                    "&nbsp;&nbsp;<img src=\"/images/blank.png\" width=\"16px\" height=\"16px\" />"));
            }
        }
        results.append("</tr></table>");
        return results.toString();
    }

    private String getColumn(boolean isSpacerColumn, Object... data) {
        StringBuilder results = new StringBuilder();
        if (isSpacerColumn) {
            results.append("<td nowrap=\"nowrap\" style=\"white-space:nowrap;\" width=\"10px\" align=\"left\" >");
        } else {
            results.append("<td nowrap=\"nowrap\" style=\"white-space:nowrap;\" width=\"55px\" align=\"left\" >");
        }
        if (data == null) {
            results.append("&nbsp;");
        } else {
            for (Object datum : data) {
                results.append(datum == null ? "&nbsp;" : datum);
            }
        }
        results.append("</td>");
        return results.toString();
    }
}
