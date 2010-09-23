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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.data.DSRequest;
import com.smartgwt.client.data.DSResponse;
import com.smartgwt.client.data.DataSource;
import com.smartgwt.client.data.DataSourceField;
import com.smartgwt.client.data.fields.DataSourceImageField;
import com.smartgwt.client.data.fields.DataSourceTextField;
import com.smartgwt.client.rpc.RPCResponse;
import com.smartgwt.client.types.DSDataFormat;
import com.smartgwt.client.types.DSProtocol;
import com.smartgwt.client.widgets.tree.TreeNode;

import org.rhq.core.domain.criteria.ResourceCriteria;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.domain.measurement.ResourceAvailability;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceCategory;
import org.rhq.core.domain.resource.ResourceSubCategory;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
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
    private List<Resource> initialData;

    private ResourceGWTServiceAsync resourceService = GWTServiceLookup.getResourceService();

    public ResourceTreeDatasource(List<Resource> initialData) {
        setClientOnly(false);
        setDataProtocol(DSProtocol.CLIENTCUSTOM);
        setDataFormat(DSDataFormat.CUSTOM);

        this.initialData = initialData;

        DataSourceField idDataField = new DataSourceTextField("id", "ID");
        idDataField.setPrimaryKey(true);

        DataSourceTextField nameDataField = new DataSourceTextField("name", "Name");
        nameDataField.setCanEdit(false);

        DataSourceTextField descriptionDataField = new DataSourceTextField("description", "Description");
        descriptionDataField.setCanEdit(false);

        DataSourceImageField availabilityDataField = new DataSourceImageField("currentAvailability", "Availability");
        descriptionDataField.setCanEdit(false);

        DataSourceTextField parentIdField = new DataSourceTextField("parentId", "Parent ID");
        parentIdField.setForeignKey("id");
        //        parentIdField.setRootValue(rootId);

        //        DataSourceTextField parentKeyField = new DataSourceTextField("parentKey", "Parent KEY");
        //        parentKeyField.setForeignKey("id");
        //        parentKeyField.setRootValue(rootId);

        //        nameDataField.setType(FieldType.);

        setDropExtraFields(false);

        setFields(idDataField, nameDataField, descriptionDataField, availabilityDataField);
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
        final long start = System.currentTimeMillis();

        String parentResourceId = request.getCriteria().getAttribute("parentId");
        //        System.out.println("All attributes: " + Arrays.toString(request.getCriteria().getAttributes()));

        ResourceCriteria criteria = new ResourceCriteria();

        if (parentResourceId == null) {
            System.out.println("ResourceTreeDatasource: Loading initial data...");

            //            criteria.addFilterId(rootId);

            processIncomingData(initialData, response, requestId);
            response.setStatus(DSResponse.STATUS_SUCCESS);
            return;

        } else {
            System.out.println("ResourceTreeDatasource: Loading Resource [" + parentResourceId + "]...");

            criteria.addFilterParentResourceId(Integer.parseInt(parentResourceId));
        }

        // The server is already eager fetch resource type
        // * criteria.fetchResourceType(true);

        resourceService.findResourcesByCriteria(criteria, new AsyncCallback<PageList<Resource>>() {
            public void onFailure(Throwable caught) {
                CoreGUI.getErrorHandler().handleError("Failed to load resource data for tree", caught);
                response.setStatus(RPCResponse.STATUS_FAILURE);
                processResponse(requestId, response);
            }

            public void onSuccess(PageList<Resource> result) {
                processIncomingData(result, response, requestId);
            }
        });
    }

    private void processIncomingData(List<Resource> result, final DSResponse response, final String requestId) {

        ResourceTypeRepository.Cache.getInstance().loadResourceTypes(
            result,
            EnumSet.of(
                ResourceTypeRepository.MetadataType.operations,
                ResourceTypeRepository.MetadataType.children,
                ResourceTypeRepository.MetadataType.subCategory),
            new ResourceTypeRepository.ResourceTypeLoadedCallback() {
                public void onResourceTypeLoaded(List<Resource> result) {
                    TreeNode[] treeNodes = buildNodes(result);
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
    public static TreeNode[] buildNodes(List<Resource> resources) {
        ResourceTreeNode[] nodes = new ResourceTreeNode[resources.size()];
        for (int i = 0; i < resources.size(); i++) {
            Resource resource = resources.get(i);
            ResourceTreeNode node = new ResourceTreeNode(resource);
            nodes[i] = node;
        }

        return introduceTypeAndCategoryNodes(nodes);
    }

    private static TreeNode[] introduceTypeAndCategoryNodes(ResourceTreeNode[] resourceNodes) {
        List<TreeNode> updatedNodes = new ArrayList<TreeNode>();
        // Maps category node IDs to the corresponding category nodes.
        Map<String, SubCategoryTreeNode> subcategoryNodes = new HashMap<String, SubCategoryTreeNode>();
        // Maps type node IDs to the corresponding type nodes.
        Map<String, AutoGroupTreeNode> autogroupNodes = new HashMap<String, AutoGroupTreeNode>();

        for (ResourceTreeNode resourceNode : resourceNodes) {
            updatedNodes.add(resourceNode);

            Resource resource = resourceNode.getResource();
            ResourceType type = resource.getResourceType();
            if (type.getCategory() != ResourceCategory.PLATFORM) {
                String autogroupNodeId = AutoGroupTreeNode.idOf(resource);
                if (!autogroupNodes.containsKey(autogroupNodeId)) {
                    Resource parentResource = resource.getParentResource();
                    ResourceSubCategory subcategory = type.getSubCategory();
                    if (subcategory != null) {
                        System.out.println("Processing " + subcategory + "...");
                        do {
                            String subcategoryNodeId = SubCategoryTreeNode.idOf(subcategory, parentResource);
                            if (!subcategoryNodes.containsKey(subcategoryNodeId)) {
                                SubCategoryTreeNode subcategoryNode = new SubCategoryTreeNode(subcategory, parentResource);
                                subcategoryNodes.put(subcategoryNode.getID(), subcategoryNode);
                                System.out.println("Adding " + subcategoryNode + " to tree...");
                                updatedNodes.add(subcategoryNode);
                            }
                        } while ((subcategory = subcategory.getParentSubCategory()) != null);
                    }

                    if (!type.isSingleton()) {
                        AutoGroupTreeNode autogroupNode = new AutoGroupTreeNode(resource);
                        autogroupNodes.put(autogroupNodeId, autogroupNode);
                        System.out.println("Adding " + autogroupNode + " to tree...");
                        updatedNodes.add(autogroupNode);
                    }
                }
            }
        }

        return updatedNodes.toArray(new TreeNode[updatedNodes.size()]);
    }

    /**
     * The folder node for a Resource subcategory.
     */
    public static class SubCategoryTreeNode extends EnhancedTreeNode {
        public SubCategoryTreeNode(ResourceSubCategory category, Resource parentResource) {
            String id = idOf(category, parentResource);
            setID(id);
            setAttribute(Attributes.ID, id);

            ResourceSubCategory parentCategory = category.getParentSubCategory();
            String parentId = (parentCategory != null) ?
                SubCategoryTreeNode.idOf(parentCategory, parentResource) :
                ResourceTreeNode.idOf(parentResource);
            setParentID(parentId);
            setAttribute(Attributes.PARENT_ID, parentId);

            // Note, subcategory names are typically already plural, so there's no need to pluralize them.
            String name = category.getDisplayName();
            setName(name);
            setAttribute(Attributes.NAME, name);

            setAttribute(Attributes.DESCRIPTION, category.getDescription());
        }

        public static String idOf(ResourceSubCategory category, Resource parentResource) {
            return "subcat" + category.getId() + "_" + parentResource.getId();
        }
    }

    /**
     * The folder node for a Resource autogroup.
     */
    public static class AutoGroupTreeNode extends EnhancedTreeNode {
        private AutoGroupTreeNode(Resource resource) {
            String id = idOf(resource);
            setID(id);
            setAttribute(Attributes.ID, id);

            String parentId = parentIdOf(resource);
            setParentID(parentId);
            setAttribute(Attributes.PARENT_ID, parentId);

            //            setAttribute("parentKey", parentId);

            ResourceType type = resource.getResourceType();
            String name = StringUtility.pluralize(type.getName());
            setName(name);
            setAttribute(Attributes.NAME, name);

            setAttribute(Attributes.DESCRIPTION, type.getDescription());
        }

        public static String idOf(Resource resource) {
            Resource parentResource = resource.getParentResource();
            return (parentResource != null) ?
                "autogroup" + resource.getResourceType().getId() + "_" + parentResource.getId() :
                null;
        }

        public static String parentIdOf(Resource resource) {
            ResourceType type = resource.getResourceType();
            ResourceSubCategory parentCategory = type.getSubCategory();
            return (parentCategory != null) ?
                SubCategoryTreeNode.idOf(parentCategory, resource.getParentResource()) :
                ResourceTreeNode.idOf(resource.getParentResource());
        }        
    }

    public static class ResourceTreeNode extends EnhancedTreeNode {
        private Resource resource;

        private ResourceTreeNode(Resource resource) {
            this.resource = resource;

            String id = idOf(resource);
            setID(id);
            setAttribute(Attributes.ID, id);

            Resource parentResource = resource.getParentResource();
            String parentId;
            if (parentResource != null) {
                parentId = resource.getResourceType().isSingleton() ?
                    AutoGroupTreeNode.parentIdOf(resource) :
                    AutoGroupTreeNode.idOf(resource);
            }
            else {
                parentId = null;
            }
            setParentID(parentId);
            setAttribute(Attributes.PARENT_ID, parentId);

            //            System.out.println(id + " / " + parentId);
            //            setAttribute("parentKey", resource.getParentResource() == null ? 0 : (resource.getParentResource().getId() + resource.getResourceType().getName()));

            String name = resource.getName();
            setName(name);
            setAttribute(Attributes.NAME, name);

            setAttribute(Attributes.DESCRIPTION, resource.getDescription());

            ResourceAvailability currentAvail = resource.getCurrentAvailability();
            setAttribute(
                "currentAvailability",
                (null != currentAvail && currentAvail.getAvailabilityType() == AvailabilityType.UP) ? "/images/icons/availability_green_16.png"
                    : "/images/icons/availability_red_16.png");

            Set<ResourceType> childTypes = resource.getResourceType().getChildResourceTypes();
            setIsFolder((childTypes != null && !childTypes.isEmpty()));
        }

        public Resource getResource() {
            return this.resource;
        }

        public static String idOf(Resource resource) {
            return idOf(resource.getId());
        }

        public static String idOf(int resourceId) {
            return String.valueOf(resourceId);
        }
    }
}
