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

import static org.rhq.coregui.client.inventory.groups.ResourceGroupDataSourceField.AVAIL_CHILDREN;
import static org.rhq.coregui.client.inventory.groups.ResourceGroupDataSourceField.AVAIL_DESCENDANTS;
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
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.resource.group.GroupCategory;
import org.rhq.core.domain.resource.group.ResourceGroup;
import org.rhq.core.domain.resource.group.composite.ResourceGroupComposite;
import org.rhq.core.domain.util.PageList;
import org.rhq.core.domain.util.ResourceTypeUtility;
import org.rhq.coregui.client.CoreGUI;
import org.rhq.coregui.client.ImageManager;
import org.rhq.coregui.client.Messages;
import org.rhq.coregui.client.gwt.GWTServiceLookup;
import org.rhq.coregui.client.gwt.ResourceGroupGWTServiceAsync;
import org.rhq.coregui.client.util.RPCDataSource;
import org.rhq.coregui.client.util.message.Message;

/**
 * @author Joseph Marques
 */
public class ResourceGroupCompositeDataSource extends RPCDataSource<ResourceGroupComposite, ResourceGroupCriteria> {

    private static final Messages MSG = CoreGUI.getMessages();

    public static final String FILTER_GROUP_IDS = "resourceGroupIds";

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

        DataSourceTextField descriptionDataField = new DataSourceTextField(DESCRIPTION.propertyName(),
            DESCRIPTION.title());
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

    @Override
    public void executeFetch(final DSRequest request, final DSResponse response, final ResourceGroupCriteria criteria) {
        groupService.findResourceGroupCompositesByCriteria(criteria,
            new AsyncCallback<PageList<ResourceGroupComposite>>() {
                public void onFailure(Throwable caught) {
                    if (caught.getMessage().contains("SearchExpressionException")) {
                        Message message = new Message(MSG.view_searchBar_suggest_noSuggest(), Message.Severity.Error);
                        CoreGUI.getMessageCenter().notify(message);
                    } else {
                        CoreGUI.getErrorHandler().handleError(MSG.view_inventory_groups_loadFailed(), caught);
                    }
                    response.setStatus(RPCResponse.STATUS_FAILURE);
                    processResponse(request.getRequestId(), response);
                }

                private PageList<ResourceGroupComposite> applyAvailabilitySearchFilter(
                    PageList<ResourceGroupComposite> result) {

                    if (!isAvailabilitySearch(criteria)) {
                        return result;
                    }
                    PageList<ResourceGroupComposite> pageList = new PageList<ResourceGroupComposite>(result
                        .getPageControl());

                    for (ResourceGroupComposite rgc : result) {
                        if (rgc.getExplicitCount() > 0) {
                            pageList.add(rgc);
                        }
                    }

                    return pageList;
                }

                public void onSuccess(PageList<ResourceGroupComposite> result) {
                    PageList<ResourceGroupComposite> filteredResult = applyAvailabilitySearchFilter(result);
                    response.setData(buildRecords(filteredResult));
                    setPagingInfo(response, result);
                    processResponse(request.getRequestId(), response);
                }
            });
    }

    private boolean isAvailabilitySearch(ResourceGroupCriteria criteria) {
        return criteria.getSearchExpression() != null && criteria.getSearchExpression().startsWith("availability");
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
    public ResourceGroupComposite copyValues(Record from) {
        Integer idAttrib = from.getAttributeAsInt("id");
        String nameAttrib = from.getAttribute(NAME.propertyName());
        String descriptionAttrib = from.getAttribute(DESCRIPTION.propertyName());
        String typeNameAttrib = from.getAttribute(TYPE.propertyName());

        ResourceGroup rg = new ResourceGroup(nameAttrib);
        rg.setId(idAttrib);
        rg.setDescription(descriptionAttrib);
        if (typeNameAttrib != null) {
            ResourceType rt = new ResourceType();
            rt.setName(typeNameAttrib);
            String pluginNameAttrib = from.getAttribute(PLUGIN.propertyName());
            rt.setPlugin(pluginNameAttrib);
            rg.setResourceType(rt);
        }

        Long explicitCount = Long.valueOf(from.getAttribute("explicitCount"));
        Long explicitDown = Long.valueOf(from.getAttribute("explicitDown"));
        Long explicitUnknown = Long.valueOf(from.getAttribute("explicitUnknown"));
        Long explicitDisabled = Long.valueOf(from.getAttribute("explicitDisabled"));
        Long implicitCount = Long.valueOf(from.getAttribute("implicitCount"));
        Long implicitDown = Long.valueOf(from.getAttribute("implicitDown"));
        Long implicitUnknown = Long.valueOf(from.getAttribute("implicitUnknown"));
        Long implicitDisabled = Long.valueOf(from.getAttribute("implicitDisabled"));

        ResourceGroupComposite composite = new ResourceGroupComposite(explicitCount, explicitDown, explicitUnknown,
            explicitDisabled, implicitCount, implicitDown, implicitUnknown, implicitDisabled, rg);

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

        record.setAttribute("explicitCount", String.valueOf(from.getExplicitCount()));
        record.setAttribute("explicitDown", String.valueOf(from.getExplicitDown()));
        record.setAttribute("explicitDisabled", String.valueOf(from.getExplicitDisabled()));
        record.setAttribute("implicitCount", String.valueOf(from.getImplicitCount()));
        record.setAttribute("implicitDown", String.valueOf(from.getImplicitDown()));
        record.setAttribute("implicitDisabled", String.valueOf(from.getImplicitDisabled()));

        record.setAttribute(AVAIL_CHILDREN.propertyName(), getExplicitFormatted(from));
        record.setAttribute(AVAIL_DESCENDANTS.propertyName(), getImplicitFormatted(from));

        if (from.getResourceGroup().getResourceType() != null) {
            record.setAttribute("resourceType", from.getResourceGroup().getResourceType());
            record.setAttribute(TYPE.propertyName(),
                ResourceTypeUtility.displayName(from.getResourceGroup().getResourceType()));
            record.setAttribute(PLUGIN.propertyName(), from.getResourceGroup().getResourceType().getPlugin());
        }

        return record;
    }

    private String getExplicitFormatted(ResourceGroupComposite from) {
        return getAlignedAvailabilityResults(from.getExplicitCount(), from.getExplicitUp(), from.getExplicitDown(),
            from.getExplicitUnknown(), from.getExplicitDisabled());
    }

    private String getImplicitFormatted(ResourceGroupComposite from) {
        return getAlignedAvailabilityResults(from.getImplicitCount(), from.getImplicitUp(), from.getImplicitDown(),
            from.getImplicitUnknown(), from.getImplicitDisabled());
    }

    private String getAlignedAvailabilityResults(long total, long up, long down, long unknown, long disabled) {
        StringBuilder results = new StringBuilder();

        results.append("<table><tr>");
        if (0 == total) {
            results.append(getColumn(false,
                "<img height=\"12\" width=\"12\" src=\"" + ImageManager.getFullImagePath(ImageManager.getAvailabilityIcon(null)) + "\" /> 0"));
            results.append(getColumn(true));
            results.append(getColumn(false));

        } else {
            if (up > 0) {
                String imagePath = ImageManager.getFullImagePath(ImageManager
                    .getAvailabilityIconFromAvailType(AvailabilityType.UP));
                results.append(getColumn(false, " <img height=\"12\" width=\"12\" src=\"" + imagePath + "\" />", up));
            } else {
                results.append(getColumn(false,
                    "&nbsp;<img src=\""+ImageManager.getBlankIcon()+"\" width=\"12px\" height=\"12px\" />"));
            }

            if (down > 0) {
                String imagePath = ImageManager.getFullImagePath(ImageManager
                    .getAvailabilityIconFromAvailType(AvailabilityType.DOWN));
                results.append(getColumn(false, " <img height=\"12\" width=\"12\" src=\"" + imagePath + "\" />", down));
            } else {
                results.append(getColumn(false,
                    "&nbsp;<img src=\""+ImageManager.getBlankIcon()+"\" width=\"12px\" height=\"12px\" />"));
            }

            if (disabled > 0) {
                String imagePath = ImageManager.getFullImagePath(ImageManager
                    .getAvailabilityIconFromAvailType(AvailabilityType.DISABLED));
                results.append(getColumn(false, " <img height=\"12\" width=\"12\" src=\"" + imagePath + "\" />",
                    disabled));
            } else {
                results.append(getColumn(false,
                    "&nbsp;<img src=\""+ImageManager.getBlankIcon()+"\" width=\"12px\" height=\"12px\" />"));
            }

            if (unknown > 0) {
                String imagePath = ImageManager.getFullImagePath(ImageManager
                    .getAvailabilityIconFromAvailType(AvailabilityType.UNKNOWN));
                results.append(getColumn(false, " <img height=\"12\" width=\"12\" src=\"" + imagePath + "\" />",
                    unknown));
            } else {
                results
                    .append(getColumn(false, "&nbsp;<img src=\""+ImageManager.getBlankIcon()+"\" width=\"1px\" height=\"1px\" />"));
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
            results.append("<td nowrap=\"nowrap\" style=\"white-space:nowrap;\" width=\"45px\" align=\"left\" >");
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
