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
package org.rhq.enterprise.gui.coregui.client.inventory.resource;

import org.rhq.core.domain.criteria.ResourceCriteria;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;

import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.data.DSRequest;
import com.smartgwt.client.data.DSResponse;
import com.smartgwt.client.data.DataSource;
import com.smartgwt.client.data.DataSourceField;
import com.smartgwt.client.data.fields.DataSourceImageField;
import com.smartgwt.client.data.fields.DataSourceIntegerField;
import com.smartgwt.client.data.fields.DataSourceTextField;
import com.smartgwt.client.rpc.RPCResponse;
import com.smartgwt.client.types.DSDataFormat;
import com.smartgwt.client.types.DSProtocol;
import com.smartgwt.client.widgets.grid.ListGridRecord;
import com.smartgwt.client.widgets.tree.TreeNode;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

/**
 * @author Greg Hinkle
 */
public class ResourceTreeDatasource extends DataSource {
private boolean initialized = false;


    private String query;

    public ResourceTreeDatasource() {
        setClientOnly(false);
        setDataProtocol(DSProtocol.CLIENTCUSTOM);
        setDataFormat(DSDataFormat.CUSTOM);

        DataSourceField idDataField = new DataSourceTextField("id", "ID");
        idDataField.setPrimaryKey(true);

        DataSourceTextField nameDataField = new DataSourceTextField("name", "Name");
        nameDataField.setCanEdit(false);

        DataSourceTextField descriptionDataField = new DataSourceTextField("description", "Description");
        descriptionDataField.setCanEdit(false);

        DataSourceImageField availabilityDataField = new DataSourceImageField("currentAvailability", "Availability");
        descriptionDataField.setCanEdit(false);

        DataSourceTextField parentIdField = new DataSourceTextField("parentId", "Parent ID");
//        parentIdField.setForeignKey("id");
//        parentIdField.setRootValue("10001");

        DataSourceTextField parentKeyField = new DataSourceTextField("parentKey", "Parent KEY");
        parentKeyField.setForeignKey("id");
        parentKeyField.setRootValue("10001");




//        nameDataField.setType(FieldType.);

        setFields(idDataField, parentIdField, parentKeyField, nameDataField, descriptionDataField, availabilityDataField);
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    @Override
    protected Object transformRequest(DSRequest request) {
        String requestId = request.getRequestId();
        DSResponse response = new DSResponse();
        response.setAttribute("clientContext", request.getAttributeAsObject("clientContext"));
        // Asume success
        response.setStatus(0);
        switch (request.getOperationType()) {
            case ADD:
                //executeAdd(lstRec, true);
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

        String p = request.getCriteria().getAttribute("parentKey") == null ? "10001" : request.getCriteria().getAttribute("parentKey");
        System.out.println(Arrays.toString(request.getCriteria().getAttributes()));
        System.out.println("Loading " + p + "(" + request.getCriteria().getAttribute("parentKey") + ")");

        ResourceCriteria criteria = new ResourceCriteria();
        criteria.addFilterParentResourceId(Integer.parseInt(p));
        criteria.fetchResourceType(true);


        ResourceGWTServiceAsync resourceService = ResourceGWTService.App.getInstance();

        resourceService.findResourcesByCriteria(criteria, new AsyncCallback<PageList<Resource>>() {
            public void onFailure(Throwable caught) {
                Window.alert("Failed to load " + caught.getMessage());
                System.err.println("Failed to fetch resources");
                response.setStatus(RPCResponse.STATUS_FAILURE);
                processResponse(requestId, response);
            }

            public void onSuccess(PageList<Resource> result) {

                ResourceTreeNode[] records = new ResourceTreeNode[result.size()];
                for (int x=0; x<result.size(); x++) {
                    Resource res = result.get(x);
                    ResourceTreeNode record = new ResourceTreeNode(res);
                    records[x] = record;
                }



                response.setData(build(records));
                response.setTotalRows(result.getTotalSize());	// for paging to work we have to specify size of full result set
                processResponse(requestId, response);

            }
        });
    }


    private TreeNode[] build(ResourceTreeNode[] nodes) {
        ArrayList<TreeNode> built = new ArrayList<TreeNode>();
        HashMap<ResourceType, TypeTreeNode> types = new HashMap<ResourceType, TypeTreeNode>();

        for (ResourceTreeNode node : nodes) {

            if (!types.containsKey(node.getResourceType())) {
                TypeTreeNode typeNode = new TypeTreeNode(node.getParentId(),node.getResourceType().getName());
                built.add(typeNode);
                types.put(node.getResourceType(), typeNode);
            }
            built.add(node);
        }


        return built.toArray(new TreeNode[built.size()]);

    }


    private boolean sameTypes(ResourceTreeNode[] nodes) {
        ResourceType first = nodes[0].getResourceType();
        for (ResourceTreeNode node : nodes) {
            if (!first.equals(node)) {
                return false;
            }
        }
        return true;
    }




    public static class TypeTreeNode extends TreeNode {


        private TypeTreeNode(int parentId, String type) {
            setAttribute("id", parentId + type);
            setAttribute("parentId", parentId);
            setAttribute("parentKey", parentId);
            setAttribute("name", type);
        }

    }



    public static class ResourceTreeNode extends TreeNode {

        private Resource resource;

        private ResourceTreeNode(Resource resource) {
            this.resource = resource;

            setAttribute("id",resource.getId());
            setAttribute("parentId", resource.getParentResource() == null ? 0 : resource.getParentResource().getId());
            setAttribute("parentKey", resource.getParentResource() == null ? 0 : (resource.getParentResource().getId() + resource.getResourceType().getName()));
            setAttribute("name",resource.getName());
            setAttribute("description",resource.getDescription());
            setAttribute("currentAvailability",
                    resource.getCurrentAvailability().getAvailabilityType() == AvailabilityType.UP
                    ? "/images/icons/availability_green_16.png"
                    : "/images/icons/availability_red_16.png");

        }

        public int getParentId() {
            return getAttributeAsInt("parentId");
        }


        public Resource getResource() {
            return resource;
        }

        public void setResource(Resource resource) {
            this.resource = resource;
        }

        public ResourceType getResourceType() {
            return resource.getResourceType();
        }


    }
}
