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
package org.rhq.enterprise.gui.coregui.client.inventory.resource.detail;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.allen_sauer.gwt.log.client.Log;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.data.DSRequest;
import com.smartgwt.client.data.DSResponse;
import com.smartgwt.client.data.DataSource;
import com.smartgwt.client.data.DataSourceField;
import com.smartgwt.client.data.fields.DataSourceTextField;
import com.smartgwt.client.rpc.RPCResponse;
import com.smartgwt.client.types.DSDataFormat;
import com.smartgwt.client.types.DSProtocol;
import com.smartgwt.client.widgets.tree.TreeNode;

import org.rhq.core.domain.criteria.ResourceCriteria;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceSubCategory;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.Messages;
import org.rhq.enterprise.gui.coregui.client.components.tree.EnhancedTreeNode;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.gwt.ResourceGWTServiceAsync;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.type.ResourceTypeRepository;
import org.rhq.enterprise.gui.coregui.client.util.StringUtility;

/**
 * This doesn't extend RPCDataSource because it is tree-oriented and behaves differently than normal list data sources
 * in some places.
 *
 * @author Greg Hinkle
 * @author Ian Springer
 */
public class ResourceTreeDatasource extends DataSource {

    Messages MSG = CoreGUI.getMessages();

    private List<Resource> initialData;
    private List<Resource> lockedData;

    private ResourceGWTServiceAsync resourceService = GWTServiceLookup.getResourceService();

    public ResourceTreeDatasource(List<Resource> initialData, List<Resource> lockedData) {
        this.setClientOnly(false);
        this.setDataProtocol(DSProtocol.CLIENTCUSTOM);
        this.setDataFormat(DSDataFormat.CUSTOM);

        this.initialData = initialData;
        this.lockedData = (null != lockedData) ? lockedData : new ArrayList<Resource>();

        DataSourceField idDataField = new DataSourceTextField("id", MSG.common_title_id());
        idDataField.setPrimaryKey(true);

        DataSourceTextField nameDataField = new DataSourceTextField("name", MSG.common_title_name());
        nameDataField.setCanEdit(false);

        DataSourceTextField descriptionDataField = new DataSourceTextField("description", MSG
            .common_title_description());
        descriptionDataField.setCanEdit(false);

        DataSourceTextField parentIdField = new DataSourceTextField("parentId", MSG.common_title_id_parent());
        parentIdField.setForeignKey("id");

        this.setDropExtraFields(false);

        this.setFields(idDataField, nameDataField, descriptionDataField);
    }

    @Override
    protected Object transformRequest(DSRequest request) {
        String requestId = request.getRequestId();
        DSResponse response = new DSResponse();
        response.setAttribute("clientContext", request.getAttributeAsObject("clientContext"));
        // Assume success
        response.setStatus(0);
        switch (request.getOperationType()) {
        case ADD:
            //executeAdd(request, response);
            break;
        case FETCH:
            executeFetch(requestId, request, response);
            break;
        case REMOVE:
            //executeRemove(lstRec);
            break;
        case UPDATE:
            //executeAdd(lstRec, false);
            break;
        default:
            break;
        }

        return request.getData();
    }

    public void executeFetch(final String requestId, final DSRequest request, final DSResponse response) {
        //final long start = System.currentTimeMillis();

        final String parentResourceId = request.getCriteria().getAttribute("parentId");
        //com.allen_sauer.gwt.log.client.Log.info("All attributes: " + Arrays.toString(request.getCriteria().getAttributes()));

        ResourceCriteria criteria = new ResourceCriteria();

        if (parentResourceId == null) {
            // If this gets called more than once it's a problem. Don't load initial data more than once.
            // Subsequent fetches should be due to parent node tree expansion
            if (null != this.initialData) {
                Log.debug("ResourceTreeDatasource: Loading initial data...");

                processIncomingData(this.initialData, response, requestId);
                response.setStatus(DSResponse.STATUS_SUCCESS);
                this.initialData = null;
            } else {
                processResponse(requestId, response);
                response.setStatus(DSResponse.STATUS_FAILURE);
            }

        } else {
            Log.debug("ResourceTreeDatasource: Loading Resource [" + parentResourceId + "]...");

            criteria.addFilterParentResourceId(Integer.parseInt(parentResourceId));

            resourceService.findResourcesByCriteria(criteria, new AsyncCallback<PageList<Resource>>() {
                public void onFailure(Throwable caught) {
                    CoreGUI.getErrorHandler().handleError(MSG.view_tree_common_loadFailed_children(), caught);
                    response.setStatus(RPCResponse.STATUS_FAILURE);
                    processResponse(requestId, response);
                }

                public void onSuccess(PageList<Resource> result) {
                    processIncomingData(result, response, requestId);
                }
            });
        }
    }

    private void processIncomingData(List<Resource> result, final DSResponse response, final String requestId) {

        ResourceTypeRepository.Cache.getInstance().loadResourceTypes(
            result,
            EnumSet.of(ResourceTypeRepository.MetadataType.operations, ResourceTypeRepository.MetadataType.children,
                ResourceTypeRepository.MetadataType.subCategory),
            new ResourceTypeRepository.ResourceTypeLoadedCallback() {
                public void onResourceTypeLoaded(List<Resource> result) {
                    TreeNode[] treeNodes = buildNodes(result, lockedData);
                    response.setData(treeNodes);
                    processResponse(requestId, response);
                    response.setStatus(DSResponse.STATUS_SUCCESS);
                }
            });
    }

    /**
     * Construct a set of TreeNodes from a list of resources
     *
     * @param resources
     * @return
     */
    public static TreeNode[] buildNodes(List<Resource> resources, List<Resource> lockedData) {
        List<ResourceTreeNode> resourceNodes = new ArrayList<ResourceTreeNode>(resources.size());
        for (Resource resource : resources) {
            ResourceTreeNode node = new ResourceTreeNode(resource, lockedData.contains(resource));
            resourceNodes.add(node);
        }

        List<TreeNode> result = introduceTypeAndCategoryNodes(resourceNodes);

        return result.toArray(new TreeNode[result.size()]);
    }

    /**  
     * @param resourceNodes ordered such that referenced parent nodes have lower indexes than the referencing child.
     * @return a new List, properly ordered and including AG and Subcategory nodes.
     */
    private static List<TreeNode> introduceTypeAndCategoryNodes(final List<ResourceTreeNode> resourceNodes) {
        // The resulting list of nodes, including AG and SC nodes. The list is ordered to ensure all
        // referenced parent nodes have lower indexes than the referencing child.
        List<TreeNode> allNodes = new ArrayList<TreeNode>(resourceNodes.size());

        // Keep track of the node IDs added so far to ensure we don't add the same node more than once. Note
        // that the list of resourceNodes passed in may have duplicates as the caller may not be able to
        // ensure a clean set.
        Set<String> allNodeIds = new HashSet<String>(resourceNodes.size() * 2);

        for (ResourceTreeNode resourceNode : resourceNodes) {
            if (allNodeIds.contains(resourceNode.getID())) {
                Log.debug("Duplicate ResourceTreeNode - Skipping: " + resourceNode);
                continue;
            }

            Resource resource = resourceNode.getResource();

            if (resourceNode.isParentSubCategory()) {

                // If the parent node is a subcategory node, make sure the subcategory node is in the
                // tree prior to the resource node.  Note that it could itself be a tree of subcategories.
                addSubCategoryNodes(allNodes, allNodeIds, resource);

            } else if (resourceNode.isParentAutoGroup()) {

                // If the parent node is an autogroup node, make sure the autogroup node is in the
                // tree prior to the resource node.

                if (!allNodeIds.contains(resourceNode.getParentID())) {
                    AutoGroupTreeNode autogroupNode = new AutoGroupTreeNode(resource);

                    if (autogroupNode.isParentSubcategory()) {

                        // If the parent node of the autogroup node is a subcategory node, make sure the subcategory
                        // node is in the tree prior to the autogroup node.  Note that it could itself be a
                        // tree of subcategories.   
                        addSubCategoryNodes(allNodes, allNodeIds, resource);

                    }
                    allNodeIds.add(resourceNode.getParentID());
                    allNodes.add(autogroupNode);
                }
            }

            allNodeIds.add(resourceNode.getID());
            allNodes.add(resourceNode);
        }

        return allNodes;
    }

    // convenience routine to avoid code duplication
    private static void addSubCategoryNodes(List<TreeNode> allNodes, Set<String> allNodeIds, Resource resource) {
        Resource parentResource = resource.getParentResource();
        ResourceType type = resource.getResourceType();
        ResourceSubCategory subCategory = type.getSubCategory();
        String subCategoryNodeId = null;
        int insertAt = allNodes.size();

        do {
            subCategoryNodeId = SubCategoryTreeNode.idOf(subCategory, parentResource);

            if (!allNodeIds.contains(subCategoryNodeId)) {
                SubCategoryTreeNode subCategoryNode = new SubCategoryTreeNode(subCategory, parentResource);
                allNodeIds.add(subCategoryNodeId);
                allNodes.add(insertAt, subCategoryNode);
            }
        } while ((subCategory = subCategory.getParentSubCategory()) != null);
    }

    public static class ResourceTreeNode extends EnhancedTreeNode {
        private Resource resource;
        private boolean isLocked;
        private boolean parentAutoGroup = false;
        private boolean parentSubCategory = false;

        /**
         * The parentID will be set to the parent resource at construction.  It can be changed
         * later (prior to tree linkage) if the resource node should logically be set to an
         * autogroup or subcategory parent.  
         * @param resource
         * @param isLocked
         */
        private ResourceTreeNode(Resource resource, boolean isLocked) {
            this.resource = resource;
            this.isLocked = isLocked;

            String id = idOf(resource);
            setID(id);

            // a resource node can have any of three different parent node types; resource, subcategory or autogroup.
            // this can be determined at construction time so set it properly now and assume the proper node
            // structure will be in place later.
            Resource parentResource = resource.getParentResource();
            String parentId = null;

            if (null != parentResource) {
                // non-singletons will always be autogrouped
                if (!resource.getResourceType().isSingleton()) {
                    parentId = AutoGroupTreeNode.idOf(resource);
                    this.parentAutoGroup = true;

                } else {
                    ResourceSubCategory subCategory = resource.getResourceType().getSubCategory();
                    if (null != subCategory) {
                        parentId = SubCategoryTreeNode.idOf(subCategory, parentResource);
                        this.parentSubCategory = true;
                    } else
                        parentId = ResourceTreeNode.idOf(parentResource);
                }
            }
            this.setParentID(parentId);

            String name = resource.getName();
            setName(name);

            setAttribute(Attributes.DESCRIPTION, resource.getDescription());

            Set<ResourceType> childTypes = resource.getResourceType().getChildResourceTypes();
            setIsFolder((childTypes != null && !childTypes.isEmpty()));
        }

        public Resource getResource() {
            return this.resource;
        }

        public boolean isLocked() {
            return isLocked;
        }

        public boolean isParentAutoGroup() {
            return parentAutoGroup;
        }

        public boolean isParentSubCategory() {
            return parentSubCategory;
        }

        public static String idOf(Resource resource) {
            return idOf(resource.getId());
        }

        public static String idOf(int resourceId) {
            return String.valueOf(resourceId);
        }
    }

    /**
     * The folder node for a Resource subCategory.
     */
    public static class SubCategoryTreeNode extends EnhancedTreeNode {

        public SubCategoryTreeNode(ResourceSubCategory category, Resource parentResource) {
            String id = idOf(category, parentResource);
            setID(id);

            ResourceSubCategory parentCategory = category.getParentSubCategory();
            String parentId = (parentCategory != null) ? SubCategoryTreeNode.idOf(parentCategory, parentResource)
                : ResourceTreeNode.idOf(parentResource);
            setParentID(parentId);

            // Note, subCategory names are typically already plural, so there's no need to pluralize them.
            String name = category.getDisplayName();
            setName(name);

            setAttribute(Attributes.DESCRIPTION, category.getDescription());
        }

        public static String idOf(ResourceSubCategory category, Resource parentResource) {
            return "subcat_" + category.getId() + "_" + parentResource.getId();
        }
    }

    /**
     * The folder node for a Resource autogroup.
     */
    public static class AutoGroupTreeNode extends EnhancedTreeNode {

        private Resource parentResource;
        private ResourceType resourceType;
        private boolean parentSubcategory = false;

        /**
         * @param resource requires resourceType field be set.  requires parentResource field be set (null for no parent)
         */
        private AutoGroupTreeNode(Resource resource) {
            this.parentResource = resource.getParentResource();
            this.resourceType = resource.getResourceType();

            String id = idOf(resource);
            setID(id);

            // parent node is either a subCategory node or a resource node
            String parentId;
            ResourceSubCategory subcategory = this.resourceType.getSubCategory();
            if (subcategory != null) {
                parentId = SubCategoryTreeNode.idOf(subcategory, this.parentResource);
                this.parentSubcategory = true;
            } else {
                parentId = ResourceTreeNode.idOf(this.parentResource);
            }
            setParentID(parentId);

            String name = StringUtility.pluralize(this.resourceType.getName());
            setName(name);

            setAttribute(Attributes.DESCRIPTION, this.resourceType.getDescription());
        }

        public Resource getParentResource() {
            return parentResource;
        }

        public ResourceType getResourceType() {
            return resourceType;
        }

        /**
         * Generates a backing group name based on the resource type name and parent resource name.  It may not be unique
         * so should not be used to query for the group (use rtId and parentResId). The name may be displayed to the
         * user.
         * 
         * @return The name of the backing group.
         */
        public String getBackingGroupName() {
            return this.getParentResource().getName() + " ( " + this.getResourceType().getName() + " )";
        }

        public boolean isParentSubcategory() {
            return parentSubcategory;
        }

        /**
         * Given a Resource, generate a unique ID for the AGNode. 
         * 
         * @param resource requires resourceType field be set.  requires parentResource field be set (null for no parent) 
         * @return The name string or null if the parentResource is null.
         */
        public static String idOf(Resource resource) {
            Resource parentResource = resource.getParentResource();
            return idOf(parentResource, resource.getResourceType());
        }

        /**
         * Given an autogroup's parent Resource and member ResourceType, generate a unique ID for an autogroup TreeNode.
         * 
         * @param parentResource requires resourceType field be set.  requires parentResource field be set (null for no parent)
         * @param resourceType the member ResourceType
         *
         * @return The name string or null if the parentResource is null
         */
        public static String idOf(Resource parentResource, ResourceType resourceType) {
            return (parentResource != null) ? "autogroup_" + resourceType.getId() + "_" + parentResource.getId() : null;
        }
    }

}
