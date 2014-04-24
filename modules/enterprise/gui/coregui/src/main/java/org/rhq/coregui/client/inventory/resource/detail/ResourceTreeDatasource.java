/*
 * RHQ Management Platform
 * Copyright (C) 2005-2012 Red Hat, Inc.
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
package org.rhq.coregui.client.inventory.resource.detail;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.data.DSRequest;
import com.smartgwt.client.data.DSResponse;
import com.smartgwt.client.data.DataSource;
import com.smartgwt.client.data.DataSourceField;
import com.smartgwt.client.data.fields.DataSourceTextField;
import com.smartgwt.client.rpc.RPCResponse;
import com.smartgwt.client.types.DSDataFormat;
import com.smartgwt.client.types.DSProtocol;
import com.smartgwt.client.widgets.Label;
import com.smartgwt.client.widgets.tree.Tree;
import com.smartgwt.client.widgets.tree.TreeGrid;
import com.smartgwt.client.widgets.tree.TreeNode;

import org.rhq.core.domain.criteria.ResourceCriteria;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.util.PageOrdering;
import org.rhq.coregui.client.CoreGUI;
import org.rhq.coregui.client.Messages;
import org.rhq.coregui.client.ViewChangedException;
import org.rhq.coregui.client.components.tree.EnhancedTreeNode;
import org.rhq.coregui.client.gwt.GWTServiceLookup;
import org.rhq.coregui.client.gwt.ResourceGWTServiceAsync;
import org.rhq.coregui.client.inventory.resource.type.ResourceTypeRepository;
import org.rhq.coregui.client.util.Log;
import org.rhq.coregui.client.util.StringUtility;

/**
 * This doesn't extend RPCDataSource because it is tree-oriented and behaves differently than normal list data sources
 * in some places.
 *
 * @author Greg Hinkle
 * @author Ian Springer
 */
public class ResourceTreeDatasource extends DataSource {

    private static final Messages MSG = CoreGUI.getMessages();

    private List<Resource> initialData;
    private List<Resource> lockedData;
    // the encompassing grid. It's unfortunate to have the DS know about the encompassing TreeGrid
    // but we have a situation in which a new AG node needs to be able to access its parent TreeNode by ID.
    private TreeGrid treeGrid;
    private Label loadingLabel;

    private ResourceGWTServiceAsync resourceService = GWTServiceLookup.getResourceService();

    public ResourceTreeDatasource(List<Resource> initialData, List<Resource> lockedData, TreeGrid treeGrid,
        Label loadingLabel) {
        this.setClientOnly(false);
        this.setDataProtocol(DSProtocol.CLIENTCUSTOM);
        this.setDataFormat(DSDataFormat.CUSTOM);

        this.initialData = initialData;
        this.lockedData = (null != lockedData) ? lockedData : new ArrayList<Resource>();
        this.treeGrid = treeGrid;
        this.loadingLabel = loadingLabel;

        DataSourceField idDataField = new DataSourceTextField("id", MSG.common_title_id());
        idDataField.setPrimaryKey(true);

        DataSourceTextField nameDataField = new DataSourceTextField("name", MSG.common_title_name());
        nameDataField.setCanEdit(false);

        DataSourceTextField descriptionDataField = new DataSourceTextField("description",
            MSG.common_title_description());
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

        loadingLabel.show();

        final String parentResourceId = request.getCriteria().getAttribute("parentId");
        //com.allen_sauer.gwt.log.client.Log.info("All attributes: " + Arrays.toString(request.getCriteria().getAttributes()));

        if (parentResourceId == null) {
            // If this gets called more than once it's a problem. Don't load initial data more than once.
            // Subsequent fetches should be due to parent node tree expansion
            if (null != this.initialData) {
                Log.debug("ResourceTreeDatasource: Loading initial data...");
                List<Resource> temp = this.initialData;
                this.initialData = null;

                processIncomingData(temp, response, requestId);
            } else {
                response.setStatus(DSResponse.STATUS_FAILURE);
                processResponse(requestId, response);
            }

            loadingLabel.hide();

        } else {
            Log.debug("ResourceTreeDatasource: Loading Resource [" + parentResourceId + "]...");

            // This fetch limits the number of resources that can be returned to protect against fetching a massive
            // number of children for a parent. Doing so may cause an unacceptably slow tree rendering, too much vertical
            // scroll, or perhaps even hang the gui if it consumed too many resources.  To see all children the
            // user will need to visit the Inventory->Children view for the resource.
            ResourceCriteria criteria = new ResourceCriteria();
            criteria.addFilterParentResourceId(Integer.parseInt(parentResourceId));
            // we must sort the results to ensure that if cropped we at least show the same results each time
            criteria.addSortName(PageOrdering.ASC);

            resourceService.findResourcesByCriteriaBounded(criteria, -1, -1, new AsyncCallback<List<Resource>>() {
                public void onFailure(Throwable caught) {
                    CoreGUI.getErrorHandler().handleError(MSG.view_tree_common_loadFailed_children(), caught);
                    response.setStatus(RPCResponse.STATUS_FAILURE);
                    processResponse(requestId, response);

                    loadingLabel.hide();
                }

                public void onSuccess(List<Resource> result) {
                    processIncomingData(result, response, requestId);
                }
            });
        }
    }

    private void processIncomingData(List<Resource> result, final DSResponse response, final String requestId) {

        ResourceTypeRepository.Cache.getInstance().loadResourceTypes(
            result,
            EnumSet.of(ResourceTypeRepository.MetadataType.operations, ResourceTypeRepository.MetadataType.children),
            new ResourceTypeRepository.ResourceTypeLoadedCallback() {

                public void onResourceTypeLoaded(List<Resource> result) {
                    TreeNode[] treeNodes = buildNodes(result, lockedData, treeGrid);
                    response.setData(treeNodes);
                    processResponse(requestId, response);

                    loadingLabel.hide();
                }
            });
    }

    /**
     * Construct a set of TreeNodes from a list of resources
     *
     * @param resources
     * @return
     */
    public static TreeNode[] buildNodes(List<Resource> resources, List<Resource> lockedData, TreeGrid treeGrid) {
        if (treeGrid == null || treeGrid.getTree() == null) {
            throw new ViewChangedException(ResourceTopView.VIEW_ID.getName() + "/*");
        }

        List<ResourceTreeNode> resourceNodes = new ArrayList<ResourceTreeNode>(resources.size());
        for (Resource resource : resources) {
            ResourceTreeNode node = new ResourceTreeNode(resource, lockedData.contains(resource));
            resourceNodes.add(node);
        }

        List<TreeNode> result = introduceTypeAndCategoryNodes(resourceNodes, treeGrid);

        return result.toArray(new TreeNode[result.size()]);
    }

    /**
     * @param resourceNodes ordered such that referenced parent nodes have lower indexes than the referencing child.
     * @return a new List, properly ordered and including AG and Subcategory nodes.
     */
    private static List<TreeNode> introduceTypeAndCategoryNodes(final List<ResourceTreeNode> resourceNodes,
        TreeGrid treeGrid) {
        // The resulting list of nodes, including AG and SC nodes. The list is ordered to ensure all
        // referenced parent nodes have lower indexes than the referencing child.
        List<TreeNode> allNodes = new ArrayList<TreeNode>(resourceNodes.size());

        // Keep track of the node IDs added so far to ensure we don't add the same node more than once. Note
        // that the list of resourceNodes passed in may have duplicates as the caller may not be able to
        // ensure a clean set.
        Set<String> allNodeIds = new HashSet<String>(resourceNodes.size() * 2);

        Map<String, Map<String, AutoGroupTreeNode>> parentNodeIdToAutoGroupsByName = new HashMap<String, Map<String, AutoGroupTreeNode>>();
        Set<AutoGroupTreeNode> ambiguouslyNamedAutoGroupNodes = new HashSet<AutoGroupTreeNode>();

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

                // First we need to ensure we have a properly populated parentResource (id and name, minimally),
                // get this from the parent ResourceTreeNode as resource.parentResource may not be set with
                // anything more than the id.
                Resource parentResource = resource.getParentResource();
                String parentResourceNodeId = ResourceTreeNode.idOf(parentResource);
                Tree tree = treeGrid.getTree();
                TreeNode parentResourceNode = tree.findById(parentResourceNodeId);
                if (null != parentResourceNode) {
                    parentResource = ((ResourceTreeNode) parentResourceNode).getResource();
                    resource.setParentResource(parentResource);
                }
                if (null == parentResource.getName()) {
                    Log.error("AutoGroup node creation using invalid parent resource: " + parentResource);
                }

                String autoGroupNodeID = resourceNode.getParentID();
                if (!allNodeIds.contains(autoGroupNodeID)) {
                    AutoGroupTreeNode autogroupNode = new AutoGroupTreeNode(resource);
                    String parentID = autogroupNode.getParentID();
                    Map<String, AutoGroupTreeNode> autoGroupNodesByName = parentNodeIdToAutoGroupsByName.get(parentID);
                    if (autoGroupNodesByName == null) {
                        autoGroupNodesByName = new HashMap<String, AutoGroupTreeNode>();
                        parentNodeIdToAutoGroupsByName.put(parentID, autoGroupNodesByName);
                    } else {
                        AutoGroupTreeNode ambiguouslyNamedAutogroupNode = autoGroupNodesByName.get(autogroupNode
                            .getName());
                        if (ambiguouslyNamedAutogroupNode != null) {
                            ambiguouslyNamedAutoGroupNodes.add(ambiguouslyNamedAutogroupNode);
                            ambiguouslyNamedAutoGroupNodes.add(autogroupNode);
                        }
                    }
                    autoGroupNodesByName.put(autogroupNode.getName(), autogroupNode);

                    if (autogroupNode.isParentSubcategory()) {
                        // If the parent node of the autogroup node is a subcategory node, make sure the subcategory
                        // node is in the tree prior to the autogroup node.  Note that it could itself be a
                        // tree of subcategories.
                        addSubCategoryNodes(allNodes, allNodeIds, resource);
                    }
                    allNodeIds.add(autoGroupNodeID);
                    allNodes.add(autogroupNode);
                }
            }

            allNodeIds.add(resourceNode.getID());
            allNodes.add(resourceNode);
        }

        for (AutoGroupTreeNode autogroupNode : ambiguouslyNamedAutoGroupNodes) {
            autogroupNode.disambiguateName();
        }

        return allNodes;
    }

    // convenience routine to avoid code duplication
    private static void addSubCategoryNodes(List<TreeNode> allNodes, Set<String> allNodeIds, Resource resource) {
        Resource parentResource = resource.getParentResource();
        ResourceType type = resource.getResourceType();
        String subCategory = type.getSubCategory();
        String subCategoryNodeId = null;

        String[] subCategories = subCategory.split("|");
        for (String currentSubCategory : subCategories) {
            subCategoryNodeId = SubCategoryTreeNode.idOf(currentSubCategory, parentResource);

            if (!allNodeIds.contains(subCategoryNodeId)) {
                SubCategoryTreeNode subCategoryNode = new SubCategoryTreeNode(subCategory, parentResource);
                allNodeIds.add(subCategoryNodeId);
                allNodes.add(subCategoryNode);
            }
        }
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
         *
         * @param resource The resource must have, minimally, id, name, description set. And, if parent is not null,
         * parentResource.id must be set as well. Also, resourceType.childresourceTypes.
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
                    String subCategory = resource.getResourceType().getSubCategory();
                    if (null != subCategory) {
                        parentId = SubCategoryTreeNode.idOf(subCategory, parentResource);
                        this.parentSubCategory = true;
                    } else
                        parentId = ResourceTreeNode.idOf(parentResource);
                }
            }
            this.setParentID(parentId);

            // name and description are user-editable, so escape HTML to prevent XSS attacks
            String name = resource.getName();
            String escapedName = StringUtility.escapeHtml(name);
            setName(escapedName);

            String description = resource.getDescription();
            String escapedDescription = StringUtility.escapeHtml(description);
            setAttribute(Attributes.DESCRIPTION, escapedDescription);

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

        public SubCategoryTreeNode(String category, Resource parentResource) {
            String id = idOf(category, parentResource);
            setID(id);

            setParentID(ResourceTreeNode.idOf(parentResource));

            // Note, subCategory names are typically already plural, so there's no need to pluralize them.
            setName(category);

            setAttribute(Attributes.DESCRIPTION, category);
        }

        public static String idOf(String category, Resource parentResource) {
            return "subcat_" + category + "_" + parentResource.getId();
        }
    }

    /**
     * The folder node for a Resource autogroup.
     */
    public static class AutoGroupTreeNode extends EnhancedTreeNode {

        private Resource parentResource;
        private ResourceType resourceType;
        private boolean parentSubcategory = false;
        private Integer resourceGroupId; // set after the node is visited, otherwise null

        /**
         * @param resource resource.id must be set. resource.parentResource.id, .name must be set.
         * resource.resourceType.id, .name, .description, .subCategory  must be set.
         */
        private AutoGroupTreeNode(Resource resource) {
            this.parentResource = resource.getParentResource();
            this.resourceType = resource.getResourceType();

            String id = idOf(resource);
            setID(id);

            // parent node is either a subCategory node or a resource node
            String parentId;
            String subcategory = this.resourceType.getSubCategory();
            if (subcategory != null) {
                //TODO: BZ 1069545 fix this
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

        public Integer getResourceGroupId() {
            return resourceGroupId;
        }

        public void setResourceGroupId(Integer resourceGroupId) {
            this.resourceGroupId = resourceGroupId;
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

        public void disambiguateName() {
            String typeName = StringUtility.pluralize(this.resourceType.getName());
            String name = typeName + " (" + this.resourceType.getPlugin() + " "
                + MSG.common_title_plugin().toLowerCase() + ")";
            setName(name);
        }
    }

}
