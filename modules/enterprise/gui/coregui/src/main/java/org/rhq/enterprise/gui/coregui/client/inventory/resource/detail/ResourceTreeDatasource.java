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
            Resource res = resources.get(x);
            ResourceTreeNode record = new ResourceTreeNode(res);
            records[x] = record;
        }

        return introduceTypeAndCategoryNodes(records);
    }

    private static TreeNode[] introduceTypeAndCategoryNodes(ResourceTreeNode[] nodes) {
        List<TreeNode> updatedNodes = new ArrayList<TreeNode>();
        Map<Integer, CategoryTreeNode> categories = new HashMap<Integer, CategoryTreeNode>();
        Map<ResourceType, TypeTreeNode> types = new HashMap<ResourceType, TypeTreeNode>();

        for (ResourceTreeNode node : nodes) {
            updatedNodes.add(node);

            ResourceType type = node.getResourceType();
            if (type.getCategory() != ResourceCategory.PLATFORM) {
                if (!types.containsKey(type)) {

                    String parentResourceId = String.valueOf(node.getResource().getParentResource().getId());

                    CategoryTreeNode categoryNode = null;
                    if (type.getSubCategory() != null) {
                        ResourceSubCategory category = type.getSubCategory();
                        if (category.getName() != null) {
                            categoryNode = categories.get(category.getId());
                            if (categoryNode == null) {
                                // TODO (ips): Handle connecting child subcat nodes to their parent subcats.
                                /*ResourceSubCategory parentCategory = category.getParentSubCategory();
                                while (parentCategory != null) {
                                    Resource parentType = parentCategory.findParentResourceType();
                                    if (parentCategory.findTaggedResourceTypes().isEmpty()) {
                                        CategoryTreeNode parentCategoryNode =
                                                new CategoryTreeNode(parentResourceId, parentCategory);
                                    }
                                }*/
                                categoryNode = new CategoryTreeNode(parentResourceId, category);
                                categories.put(category.getId(), categoryNode);
                                updatedNodes.add(categoryNode);
                            }
                        }
                    }

                    String parentId = (categoryNode != null) ? categoryNode.getID() : parentResourceId;
                    TypeTreeNode typeNode = new TypeTreeNode(parentId, parentResourceId, type);
                    updatedNodes.add(typeNode);
                    types.put(type, typeNode);
                }
            }
        }

        return updatedNodes.toArray(new TreeNode[updatedNodes.size()]);
    }

    private static boolean sameTypes(ResourceTreeNode[] nodes) {
        ResourceType first = nodes[0].getResourceType();
        for (ResourceTreeNode node : nodes) {
            if (!first.equals(node)) {
                return false;
            }
        }
        return true;
    }

    public static class CategoryTreeNode extends EnhancedTreeNode {
        public CategoryTreeNode(String parentResourceId, ResourceSubCategory category) {
            String id = parentResourceId + "__" + fixId(category.getName());
            setID(id);
            setAttribute("id", id);

            setParentID(parentResourceId);
            setAttribute("parentId", parentResourceId);

            String name = pluralize(category.getDisplayName());
            setName(name);
            setAttribute("name", name);
        }
    }

    public static class TypeTreeNode extends EnhancedTreeNode {
        private TypeTreeNode(String parentId, String parentResourceId, ResourceType type) {
            String id = parentResourceId + "_" + type.getId();
            setID(id);
            setAttribute("id", id);

            if (parentId == null) {
                try {
                    throw new IllegalStateException("**************** WARNING: parent ID is null for type " + type);
                } catch (IllegalStateException e) {
                    e.printStackTrace();
                }
            }
            setParentID(parentId);
            setAttribute("parentId", parentId);

            //            setAttribute("parentKey", parentId);

            String name = pluralize(type.getName());
            setName(name);
            setAttribute("name", name);
        }

        @Override
        public void setParentID(String parentID) {
            if (parentID == null) {
                try {
                    throw new IllegalStateException("**************** WARNING: setting parent ID to null for type " + getName());
                } catch (IllegalStateException e) {
                    e.printStackTrace();
                }
            }
            super.setParentID(parentID);
        }

        @Override
        public void setAttribute(String property, String value) {
            if (property.equals("parentId") && value == null) {
                try {
                    throw new IllegalStateException("**************** WARNING: setting parent ID to null for type " + getName());
                } catch (IllegalStateException e) {
                    e.printStackTrace();
                }
            }
            super.setAttribute(property, value);
        }
    }

    public static class ResourceTreeNode extends EnhancedTreeNode {
        private Resource resource;

        private ResourceTreeNode(Resource resource) {
            this.resource = resource;

            String id = String.valueOf(resource.getId());
            setID(id);
            setAttribute("id", id);

            String parentId = (resource.getParentResource() != null) ?
                    (resource.getParentResource().getId() + "_" + resource.getResourceType().getId()) : null;
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

        public ResourceType getResourceType() {
            return resource.getResourceType();
        }
    }

    private static String fixId(String id) {
        return id.replace(' ', '_');
    }

    private static String pluralize(String s) {
        // TODO: Make this smarter.
        return s + "s";
    }
}
