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

        String p = request.getCriteria().getAttribute("parentId");
        //        System.out.println("All attributes: " + Arrays.toString(request.getCriteria().getAttributes()));

        ResourceCriteria criteria = new ResourceCriteria();

        if (p == null) {
            System.out.println("DataSourceTree: Loading initial data");

            //            criteria.addFilterId(rootId);

            processIncomingData(initialData, response, requestId);
            response.setStatus(DSResponse.STATUS_SUCCESS);
            return;

        } else {
            System.out.println("DataSourceTree: Loading " + p);

            criteria.addFilterParentResourceId(Integer.parseInt(p));
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
            EnumSet.of(ResourceTypeRepository.MetadataType.operations, ResourceTypeRepository.MetadataType.children,
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
        ResourceTreeNode[] records = new ResourceTreeNode[resources.size()];
        for (int x = 0; x < resources.size(); x++) {
            Resource resource = resources.get(x);
            ResourceTreeNode record = new ResourceTreeNode(resource);
            records[x] = record;
        }

        return introduceTypeAndCategoryNodes(records);
    }

    private static TreeNode[] introduceTypeAndCategoryNodes(ResourceTreeNode[] nodes) {
        List<TreeNode> updatedNodes = new ArrayList<TreeNode>();
        // Maps category node IDs to the corresponding category nodes.
        Map<String, CategoryTreeNode> categories = new HashMap<String, CategoryTreeNode>();
        // Maps Resource types to the corresponding type nodes.
        Map<ResourceType, TypeTreeNode> types = new HashMap<ResourceType, TypeTreeNode>();

        for (ResourceTreeNode node : nodes) {
            updatedNodes.add(node);

            Resource resource = node.getResource();
            ResourceType type = resource.getResourceType();
            if (type.getCategory() != ResourceCategory.PLATFORM) {
                if (!types.containsKey(type)) {
                    Resource parentResource = resource.getParentResource();
                    ResourceSubCategory category = type.getSubCategory();
                    if (category != null) {
                        do {
                            String categoryNodeId = CategoryTreeNode.idOf(category, parentResource);
                            CategoryTreeNode categoryNode = categories.get(categoryNodeId);
                            if (categoryNode == null) {
                                categoryNode = new CategoryTreeNode(category, parentResource);
                                categories.put(categoryNode.getID(), categoryNode);
                                updatedNodes.add(categoryNode);
                            }
                        } while ((category = category.getParentSubCategory()) != null);                                                
                    }

                    TypeTreeNode typeNode = new TypeTreeNode(resource);
                    updatedNodes.add(typeNode);
                    types.put(type, typeNode);
                }
            }
        }

        return updatedNodes.toArray(new TreeNode[updatedNodes.size()]);
    }

    public static class CategoryTreeNode extends EnhancedTreeNode {
        public CategoryTreeNode(ResourceSubCategory category, Resource parentResource) {
            String id = idOf(category, parentResource);
            setID(id);
            setAttribute("id", id);

            ResourceSubCategory parentCategory = category.getParentSubCategory();
            String parentId = (parentCategory != null) ?
                CategoryTreeNode.idOf(parentCategory, parentResource) :
                ResourceTreeNode.idOf(parentResource);
            setParentID(parentId);
            setAttribute("parentId", parentId);

            // Note, subcategory names are typically already plural, so there's no need to pluralize them.
            String name = category.getDisplayName();
            setName(name);
            setAttribute("name", name);
        }

        public static String idOf(ResourceSubCategory category, Resource parentResource) {
            return "subcat" + category.getId() + "_" + parentResource.getId();
        }
    }

    /**
     * The Resource type folder node for an autogroup.
     */
    public static class TypeTreeNode extends EnhancedTreeNode {
        private TypeTreeNode(Resource resource) {
            String id = idOf(resource);
            setID(id);
            setAttribute("id", id);

            String parentId = parentIdOf(resource);
            setParentID(parentId);
            setAttribute("parentId", parentId);

            //            setAttribute("parentKey", parentId);

            ResourceType type = resource.getResourceType();
            String name = pluralize(type.getName());
            setName(name);
            setAttribute("name", name);
        }

        public static String idOf(Resource resource) {
            Resource parentResource = resource.getParentResource();
            return (parentResource != null) ? "type" + resource.getResourceType().getId() + "_"
                + parentResource.getId() : null;
        }

        public static String parentIdOf(Resource resource) {
            ResourceType type = resource.getResourceType();
            ResourceSubCategory parentCategory = type.getSubCategory();
            String parentId = (parentCategory != null) ?
                CategoryTreeNode.idOf(parentCategory, resource.getParentResource()) :
                ResourceTreeNode.idOf(resource.getParentResource());
            return parentId;
        }        
    }

    public static class ResourceTreeNode extends EnhancedTreeNode {
        private Resource resource;

        private ResourceTreeNode(Resource resource) {
            this.resource = resource;

            String id = idOf(resource);
            setID(id);
            setAttribute("id", id);

            Resource parentResource = resource.getParentResource();
            String parentId;
            if (parentResource != null) {
                parentId = resource.getResourceType().isSingleton() ?
                    TypeTreeNode.parentIdOf(resource) :
                    TypeTreeNode.idOf(resource);
            }
            else {
                parentId = null;
            }
            setParentID(parentId);
            setAttribute("parentId", parentId);

            //            System.out.println(id + " / " + parentId);
            //            setAttribute("parentKey", resource.getParentResource() == null ? 0 : (resource.getParentResource().getId() + resource.getResourceType().getName()));

            setName(resource.getName());
            setAttribute("name", resource.getName());
            setAttribute("description", resource.getDescription());
            ResourceAvailability currentAvail = resource.getCurrentAvailability();
            setAttribute(
                "currentAvailability",
                (null != currentAvail && currentAvail.getAvailabilityType() == AvailabilityType.UP) ? "/images/icons/availability_green_16.png"
                    : "/images/icons/availability_red_16.png");

            setIsFolder((resource.getResourceType().getChildResourceTypes() != null && !resource.getResourceType()
                .getChildResourceTypes().isEmpty()));
        }

        public Resource getResource() {
            return resource;
        }

        public static String idOf(Resource resource) {
            return idOf(resource.getId());
        }

        public static String idOf(int resourceId) {
            return "resource" + resourceId;
        }
    }

    private static String pluralize(String singularNoun) {
        // TODO: Make this smarter.
        String pluralNoun;
        if (singularNoun.endsWith("y") && !singularNoun.endsWith("ay") && !singularNoun.endsWith("ey") &&
            !singularNoun.endsWith("oy")) {
            pluralNoun = singularNoun.substring(0, singularNoun.length() - 1) + "ies";
        } else {
            pluralNoun = singularNoun + "s";
        }
        return pluralNoun;
    }
}
