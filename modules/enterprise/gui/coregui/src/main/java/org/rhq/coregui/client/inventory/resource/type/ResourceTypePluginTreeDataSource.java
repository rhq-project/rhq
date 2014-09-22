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
 * along with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA
 */

package org.rhq.coregui.client.inventory.resource.type;

import static java.lang.Boolean.FALSE;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Set;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.data.DSRequest;
import com.smartgwt.client.data.DSResponse;
import com.smartgwt.client.data.DataSource;
import com.smartgwt.client.data.fields.DataSourceTextField;
import com.smartgwt.client.types.DSDataFormat;
import com.smartgwt.client.types.DSProtocol;
import com.smartgwt.client.widgets.tree.TreeNode;

import org.rhq.core.domain.criteria.ResourceTypeCriteria;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.coregui.client.CoreGUI;
import org.rhq.coregui.client.IconEnum;
import org.rhq.coregui.client.ImageManager;
import org.rhq.coregui.client.Messages;
import org.rhq.coregui.client.gwt.GWTServiceLookup;
import org.rhq.coregui.client.gwt.ResourceTypeGWTServiceAsync;

/**
 * @author Greg Hinkle
 */
public class ResourceTypePluginTreeDataSource extends DataSource {
    private static final String ID = "id";
    private static final String PARENT_ID = "parentId";
    private static final String ITEM_ID = "itemId";
    private static final String NAME = "name";
    private static final String PLUGIN = "plugin";
    private static final String CATEGORY = "category";

    private final Messages MSG = CoreGUI.getMessages();
    private final ResourceTypeGWTServiceAsync resourceTypeService = GWTServiceLookup.getResourceTypeGWTService();
    private final boolean showIgnoredResourceTypes;

    public ResourceTypePluginTreeDataSource(boolean showIgnoredResourceTypes) {
        this.showIgnoredResourceTypes = showIgnoredResourceTypes;

        setClientOnly(false);
        setDataProtocol(DSProtocol.CLIENTCUSTOM);
        setDataFormat(DSDataFormat.CUSTOM);

        DataSourceTextField idField = new DataSourceTextField(ID, MSG.common_title_id());
        idField.setPrimaryKey(true);
        DataSourceTextField parentIdField = new DataSourceTextField(PARENT_ID, MSG.view_type_parentId());
        parentIdField.setForeignKey(ID);
        DataSourceTextField itemIdField = new DataSourceTextField(ITEM_ID);
        DataSourceTextField resourceNameField = new DataSourceTextField(NAME, MSG.common_title_name());
        DataSourceTextField resourceKeyField = new DataSourceTextField(PLUGIN, MSG.common_title_plugin());
        DataSourceTextField resourceTypeField = new DataSourceTextField(CATEGORY, MSG.common_title_category());
        setFields(idField, parentIdField, itemIdField, resourceNameField, resourceKeyField, resourceTypeField);
    }

    @Override
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

    private void executeFetch(final DSRequest request, final DSResponse response) {
        String parentIdString = request.getCriteria().getAttributeAsString(PARENT_ID);
        if (parentIdString != null) {
            processResponse(request.getRequestId(), response);
        } else {
            ResourceTypeCriteria criteria = new ResourceTypeCriteria();
            criteria.addFilterIgnored((showIgnoredResourceTypes ? null : FALSE));
            criteria.fetchParentResourceTypes(true);
            PageControl pc = PageControl.getUnlimitedInstance();
            pc.addDefaultOrderingField("name");
            criteria.setPageControl(pc);

            resourceTypeService.findResourceTypesByCriteria(criteria, new AsyncCallback<PageList<ResourceType>>() {
                @Override
                public void onFailure(Throwable caught) {
                    CoreGUI.getErrorHandler().handleError(MSG.view_type_typeTreeLoadFailure(), caught);
                }

                @Override
                public void onSuccess(PageList<ResourceType> result) {
                    TreeNodesBuilder treeNodesBuilder = new TreeNodesBuilder(result);
                    response.setData(treeNodesBuilder.buildNodes());
                    processResponse(request.getRequestId(), response);
                }
            });
        }
    }

    private static class TreeNodesBuilder {
        private final PageList<ResourceType> resourceTypes;
        HashMap<String, ArrayList<ResourceType>> rootTypes;
        HashMap<ResourceType, ArrayList<ResourceType>> typeChildren;
        int id;
        ArrayList<TreeNode> nodes;

        private TreeNodesBuilder(PageList<ResourceType> resourceTypes) {
            this.resourceTypes = resourceTypes;
        }

        TreeNode[] buildNodes() {
            rootTypes = new LinkedHashMap<String, ArrayList<ResourceType>>();
            typeChildren = new LinkedHashMap<ResourceType, ArrayList<ResourceType>>();
            id = 0;

            nodes = new ArrayList<TreeNode>(resourceTypes.size() + 1 /* at least this size*/);
            // Add a dummy node so that if the user selects a plugin or resource type, he has
            // the ability to undo the selection. This data source is used with a IPickTreeItem
            // and that widget does not allow you to select the initial value once the user
            // selects a value. See https://bugzilla.redhat.com/show_bug.cgi?id=749801.
            nodes.add(new TreeNode(""));

            for (ResourceType type : resourceTypes) {
                String plugin = type.getPlugin();
                Set<ResourceType> parentTypes = type.getParentResourceTypes();
                if (parentTypes == null || parentTypes.isEmpty() || parentTypes.size() > 1) {
                    ArrayList<ResourceType> pluginRoots = rootTypes.get(plugin);
                    if (pluginRoots == null) {
                        pluginRoots = new ArrayList<ResourceType>();
                        rootTypes.put(plugin, pluginRoots);
                    }
                    pluginRoots.add(type);
                } else {
                    ResourceType parentType = parentTypes.iterator().next();
                    ArrayList<ResourceType> siblingTypes = typeChildren.get(parentType);
                    if (siblingTypes == null) {
                        siblingTypes = new ArrayList<ResourceType>();
                        typeChildren.put(parentType, siblingTypes);
                    }
                    siblingTypes.add(type);
                }
            }

            for (String pluginName : rootTypes.keySet()) {
                PluginTreeNode pluginNode = new PluginTreeNode(null, String.valueOf(id++), pluginName);
                nodes.add(pluginNode);

                for (ResourceType rootType : rootTypes.get(pluginName)) {
                    ResourceTypeTreeNode typeNode = new ResourceTypeTreeNode(pluginNode.id, String.valueOf(id++),
                        rootType);
                    nodes.add(typeNode);
                    addChildrenRecursively(typeNode);
                }
            }

            return nodes.toArray(new TreeNode[nodes.size()]);
        }

        void addChildrenRecursively(ResourceTypeTreeNode parentNode) {
            ResourceType parentType = parentNode.resourceType;
            ArrayList<ResourceType> siblings = typeChildren.get(parentType);
            if (siblings == null) {
                return;
            }
            for (ResourceType type : siblings) {
                ResourceTypeTreeNode typeNode = new ResourceTypeTreeNode(parentNode.id, String.valueOf(id++), type);
                nodes.add(typeNode);
                addChildrenRecursively(typeNode);
            }
        }
    }

    private static class PluginTreeNode extends TreeNode {
        static String pluginStr = CoreGUI.getMessages().common_title_plugin();

        final String parentId;
        final String id;

        PluginTreeNode(String parentId, String id, String pluginName) {
            this.parentId = parentId;
            this.id = id;

            setID(id);
            setParentID(parentId);

            setAttribute(ID, id);
            setAttribute(PARENT_ID, parentId);
            setAttribute(ITEM_ID, pluginName);
            setAttribute(NAME, pluginName + " " + pluginStr);

            setIcon(IconEnum.PLUGIN.getIcon16x16Path());
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            ResourceTypeTreeNode that = (ResourceTypeTreeNode) o;
            return id.equals(that.id);

        }

        @Override
        public int hashCode() {
            return id.hashCode();
        }
    }

    private static class ResourceTypeTreeNode extends TreeNode {
        final String parentId;
        final String id;
        final ResourceType resourceType;

        private ResourceTypeTreeNode(String parentId, String id, ResourceType resourceType) {
            this.parentId = parentId;
            this.id = id;
            this.resourceType = resourceType;

            setID(id);
            setParentID(parentId);

            setAttribute(ID, id);
            setAttribute(PARENT_ID, parentId);
            setAttribute(ITEM_ID, resourceType.getId());
            setAttribute(NAME, resourceType.getName());
            setAttribute(PLUGIN, resourceType.getPlugin());
            setAttribute("category", resourceType.getCategory().getDisplayName());

            setIcon(ImageManager.getResourceIcon(resourceType.getCategory()));
        }

        public ResourceType getResourceType() {
            return resourceType;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            ResourceTypeTreeNode that = (ResourceTypeTreeNode) o;
            return id.equals(that.id);

        }

        @Override
        public int hashCode() {
            return id.hashCode();
        }
    }
}
