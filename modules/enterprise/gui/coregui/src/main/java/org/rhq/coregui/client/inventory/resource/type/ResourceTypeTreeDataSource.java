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
package org.rhq.coregui.client.inventory.resource.type;

import java.util.HashSet;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.data.DSRequest;
import com.smartgwt.client.data.DSResponse;
import com.smartgwt.client.data.DataSource;
import com.smartgwt.client.data.fields.DataSourceTextField;
import com.smartgwt.client.types.DSDataFormat;
import com.smartgwt.client.types.DSProtocol;
import com.smartgwt.client.widgets.tree.TreeNode;

import org.rhq.core.domain.criteria.ResourceTypeCriteria;
import org.rhq.core.domain.resource.ResourceCategory;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.core.domain.util.PageOrdering;
import org.rhq.core.domain.util.ResourceTypeUtility;
import org.rhq.coregui.client.CoreGUI;
import org.rhq.coregui.client.Messages;
import org.rhq.coregui.client.gwt.GWTServiceLookup;
import org.rhq.coregui.client.gwt.ResourceTypeGWTServiceAsync;

/**
 * @author Greg Hinkle
 */
public class ResourceTypeTreeDataSource extends DataSource {
    private Messages MSG = CoreGUI.getMessages();

    private ResourceTypeGWTServiceAsync resourceTypeService = GWTServiceLookup.getResourceTypeGWTService();

    private final boolean showIgnoredResourceTypes;

    public ResourceTypeTreeDataSource(boolean showIgnoredResourceTypes) {

        this.showIgnoredResourceTypes = showIgnoredResourceTypes;

        setClientOnly(false);
        setDataProtocol(DSProtocol.CLIENTCUSTOM);
        setDataFormat(DSDataFormat.CUSTOM);

        DataSourceTextField idField = new DataSourceTextField("id", MSG.common_title_id());
        idField.setPrimaryKey(true);

        DataSourceTextField parentIdField = new DataSourceTextField("parentId", MSG.view_type_parentId());
        parentIdField.setForeignKey("id");

        DataSourceTextField resourceNameField = new DataSourceTextField("name", MSG.common_title_name());

        DataSourceTextField resourceKeyField = new DataSourceTextField("plugin", MSG.common_title_plugin());

        DataSourceTextField resourceTypeField = new DataSourceTextField("category", MSG.common_title_category());

        setFields(idField, parentIdField, resourceNameField, resourceKeyField, resourceTypeField);
    }

    protected Object transformRequest(DSRequest request) {
        DSResponse response = new DSResponse();
        response.setAttribute("clientContext", request.getAttributeAsObject("clientContext"));
        // Asume success
        response.setStatus(0);
        switch (request.getOperationType()) {
        case FETCH:
            executeFetch(request, response);
            break;
        default:
            break;
        }

        return request.getData();
    }

    protected void executeFetch(final DSRequest request, final DSResponse response) {

        String parentIdString = request.getCriteria().getAttributeAsString("parentId");
        if (parentIdString != null) {
            processResponse(request.getRequestId(), response);
        } else {

            ResourceTypeCriteria criteria = new ResourceTypeCriteria();
            criteria.addFilterIgnored((showIgnoredResourceTypes ? (Boolean) null : Boolean.FALSE));
            criteria.fetchParentResourceTypes(true);
            criteria.setPageControl(PageControl.getUnlimitedInstance());

            resourceTypeService.findResourceTypesByCriteria(criteria, new AsyncCallback<PageList<ResourceType>>() {
                public void onFailure(Throwable caught) {
                    CoreGUI.getErrorHandler().handleError(MSG.view_type_typeTreeLoadFailure(), caught);
                }

                public void onSuccess(PageList<ResourceType> result) {
                    response.setData(buildNodes(result));
                    processResponse(request.getRequestId(), response);
                }
            });
        }
    }

    public static TreeNode[] buildNodes(PageList<ResourceType> result) {

        HashSet<TreeNode> nodes = new HashSet<TreeNode>();

        HashSet<ResourceType> platforms = new HashSet<ResourceType>();
        HashSet<ResourceType> noParentNonPlatformNodes = new HashSet<ResourceType>();

        for (ResourceType type : result) {

            if (type.getCategory() != ResourceCategory.PLATFORM
                && (type.getParentResourceTypes() == null || type.getParentResourceTypes().isEmpty())) {
                noParentNonPlatformNodes.add(type);

            } else if (type.getParentResourceTypes() == null || type.getParentResourceTypes().isEmpty()) {
                platforms.add(type);
                nodes.add(new ResourceTypeTreeNode(type, type.getPlugin()));
            } else {
                for (ResourceType parent : type.getParentResourceTypes()) {
                    nodes.add(new ResourceTypeTreeNode(type, String.valueOf(parent.getId())));
                }
            }
        }

        for (ResourceType platform : platforms) {
            for (ResourceType platformChild : noParentNonPlatformNodes) {
                nodes.add(new ResourceTypeTreeNode(platformChild, String.valueOf(platform.getId())));
            }
        }

        TreeNode[] treeNodes = nodes.toArray(new TreeNode[nodes.size()]);
        return treeNodes;
    }

    /**
     * Returns a prepopulated PageControl based on the provided DSRequest. This will set sort fields,
     * pagination, but *not* filter fields.
     *
     * @param request the request to turn into a page control
     * @return the page control for passing to criteria and other queries
     */
    protected PageControl getPageControl(DSRequest request) {
        // Initialize paging.
        PageControl pageControl;
        if (request.getStartRow() == null || request.getEndRow() == null) {
            pageControl = new PageControl();
        } else {
            pageControl = PageControl.getExplicitPageControl(request.getStartRow(), request.getEndRow()
                - request.getStartRow());
        }

        // Initialize sorting.
        String sortBy = request.getAttribute("sortBy");
        if (sortBy != null) {
            String[] sorts = sortBy.split(",");
            for (String sort : sorts) {
                PageOrdering ordering = (sort.startsWith("-")) ? PageOrdering.DESC : PageOrdering.ASC;
                String columnName = (ordering == PageOrdering.DESC) ? sort.substring(1) : sort;
                pageControl.addDefaultOrderingField(columnName, ordering);
            }
        }

        return pageControl;
    }

    public static class PluginTreeNode extends TreeNode {

        String id;

        PluginTreeNode(String pluginName) {

            setID(pluginName);
            this.id = pluginName;
            setParentID(null);

            setAttribute("name", pluginName);
            //            setAttribute("plugin",pluginName);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (o == null || getClass() != o.getClass())
                return false;

            PluginTreeNode that = (PluginTreeNode) o;

            if (!id.equals(that.id))
                return false;

            return true;
        }

        @Override
        public int hashCode() {
            return id.hashCode();
        }
    }

    public static class ResourceTypeTreeNode extends TreeNode {

        private ResourceType resourceType;
        String id;
        String parentId;

        private ResourceTypeTreeNode(ResourceType resourceType, String parentId) {
            this.resourceType = resourceType;

            String id = String.valueOf(resourceType.getId());

            setID(id);
            this.id = id;
            setParentID(parentId);
            this.parentId = parentId;

            setAttribute("id", id);
            setAttribute("parentId", parentId);
            setAttribute("name", ResourceTypeUtility.displayName(resourceType));
            setAttribute("plugin", resourceType.getPlugin());
            setAttribute("category", resourceType.getCategory().getDisplayName());
            setIsFolder(true);
        }

        public ResourceType getResourceType() {
            return resourceType;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (o == null || getClass() != o.getClass())
                return false;

            ResourceTypeTreeNode that = (ResourceTypeTreeNode) o;

            if (!id.equals(that.id))
                return false;
            if (parentId != null ? !parentId.equals(that.parentId) : that.parentId != null)
                return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result = id.hashCode();
            result = 31 * result + (parentId != null ? parentId.hashCode() : 0);
            return result;
        }
    }
}
