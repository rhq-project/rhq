/*
 * RHQ Management Platform
 * Copyright (C) 2005-2008 Red Hat, Inc.
 * All rights reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2, as
 * published by the Free Software Foundation, and/or the GNU Lesser
 * General Public License, version 2.1, also as published by the Free
 * Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License and the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License
 * and the GNU Lesser General Public License along with this program;
 * if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */

package org.rhq.coregui.client.dashboard.portlets.recent.imported;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.data.DSRequest;
import com.smartgwt.client.data.DSResponse;
import com.smartgwt.client.data.DataSource;
import com.smartgwt.client.data.fields.DataSourceTextField;
import com.smartgwt.client.types.DSDataFormat;
import com.smartgwt.client.types.DSProtocol;
import com.smartgwt.client.widgets.tree.TreeNode;

import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.resource.composite.RecentlyAddedResourceComposite;
import org.rhq.coregui.client.CoreGUI;
import org.rhq.coregui.client.ImageManager;
import org.rhq.coregui.client.Messages;
import org.rhq.coregui.client.UserSessionManager;
import org.rhq.coregui.client.dashboard.Portlet;
import org.rhq.coregui.client.gwt.GWTServiceLookup;
import org.rhq.coregui.client.util.Log;
import org.rhq.coregui.client.util.MeasurementUtility;

public class RecentlyAddedResourceDS extends DataSource {
    private static final Messages MSG = CoreGUI.getMessages();
    private Portlet portlet;
    private int maximumRecentlyAddedToDisplay;
    private int maximumRecentlyAddedWithinHours;
    private long oldestDate = -1;

    public RecentlyAddedResourceDS(Portlet recentlyAddedPortlet) {
        this.portlet = recentlyAddedPortlet;
        setClientOnly(false);
        setDataProtocol(DSProtocol.CLIENTCUSTOM);
        setDataFormat(DSDataFormat.CUSTOM);

        DataSourceTextField idField = new DataSourceTextField("id", MSG.common_title_id());
        idField.setPrimaryKey(true);

        DataSourceTextField parentIdField = new DataSourceTextField("parentId", MSG
            .dataSource_measurementOob_field_parentName()
            + " " + MSG.dataSource_users_field_id());
        parentIdField.setForeignKey("id");

        DataSourceTextField resourceNameField = new DataSourceTextField("name", MSG.common_title_resource_name());
        resourceNameField.setPrimaryKey(true);

        DataSourceTextField timestampField = new DataSourceTextField("timestamp", MSG.common_title_timestamp());

        setFields(idField, parentIdField, resourceNameField, timestampField);
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

    public void executeFetch(final DSRequest request, final DSResponse response) {

        long ctime = -1;
        int maxItems = -1;
        //retrieve current portlet display settings
        if ((this.portlet != null) && (this.portlet instanceof RecentlyAddedResourcesPortlet)) {
            RecentlyAddedResourcesPortlet recentAdditionsPortlet = (RecentlyAddedResourcesPortlet) this.portlet;
            if (recentAdditionsPortlet != null) {
                if (getMaximumRecentlyAddedToDisplay() > 0) {
                    maxItems = getMaximumRecentlyAddedToDisplay();
                }

                //define the time window
                if (getMaximumRecentlyAddedWithinHours() > 0) {
                    ctime = System.currentTimeMillis()
                        - (getMaximumRecentlyAddedWithinHours() * MeasurementUtility.HOURS);
                    setOldestDate(ctime);
                }

            }
        }

        // TODO: spinder: revisit this later. ResourceCriteria mechanism does not work. Not sure if it's better?
        //        ResourceCriteria c = new ResourceCriteria();
        //
        //        String p = request.getCriteria().getAttribute("parentId");
        //
        //        if (p == null) {
        //            c.addFilterResourceCategory(ResourceCategory.PLATFORM);
        //            c.fetchChildResources(true);
        //        } else {
        //            c.addFilterParentResourceId(Integer.parseInt(p));
        //        }

        // TODO GH: Enhance resourceCriteria query to support itime based filtering for
        // "Recently imported" resources

        //if logged in then proceed making server side calls
        if (UserSessionManager.isLoggedIn()) {
            GWTServiceLookup.getResourceService().findRecentlyAddedResources(ctime, maxItems,
                new AsyncCallback<List<RecentlyAddedResourceComposite>>() {
                    public void onFailure(Throwable throwable) {
                        CoreGUI.getErrorHandler().handleError(MSG.view_portlet_recentlyAdded_error1(), throwable);
                    }

                    public void onSuccess(List<RecentlyAddedResourceComposite> recentlyAddedList) {
                        List<RecentlyAddedResourceComposite> list = new ArrayList<RecentlyAddedResourceComposite>();

                        for (RecentlyAddedResourceComposite recentlyAdded : recentlyAddedList) {
                            list.add(recentlyAdded);
                            list.addAll(recentlyAdded.getChildren());
                        }

                        response.setData(buildNodes(list));
                        response.setTotalRows(list.size());
                        processResponse(request.getRequestId(), response);
                    }
                });
        } else {//
            Log.debug("user is not logged in. Not fetching recently added resource now.");
            //answer the datasource
            response.setTotalRows(0);
            processResponse(request.getRequestId(), response);
        }
    }

    private TreeNode[] buildNodes(List<RecentlyAddedResourceComposite> list) {
        TreeNode[] treeNodes = new TreeNode[list.size()];
        for (int i = 0; i < list.size(); ++i) {
            treeNodes[i] = new RecentlyAddedTreeNode(list.get(i));
        }
        return treeNodes;
    }

    public static class RecentlyAddedTreeNode extends TreeNode {
        private RecentlyAddedResourceComposite recentlyAdded;

        private RecentlyAddedTreeNode(RecentlyAddedResourceComposite c) {
            recentlyAdded = c;
            Date dateAdded = new Date(recentlyAdded.getCtime());

            String id = String.valueOf(recentlyAdded.getId());
            String parentId = recentlyAdded.getParentId() == 0 ? null : String.valueOf((recentlyAdded.getParentId()));

            setID(id);
            setParentID(parentId);

            setAttribute("id", id);
            setAttribute("parentId", parentId);
            setAttribute("name", recentlyAdded.getName());
            setAttribute("timestamp", dateAdded);
            setIsFolder(recentlyAdded.getParentId() == 0);
        }
    }

    public static class ResourceTreeNode extends TreeNode {

        private Resource resource;

        private ResourceTreeNode(Resource resource) {
            this.resource = resource;

            String id = String.valueOf(resource.getId());
            String parentId = resource.getParentResource() == null ? null : String.valueOf((resource
                .getParentResource().getId()));

            setID(id);
            setParentID(parentId);

            setAttribute("id", id);
            setAttribute("parentId", parentId);
            setAttribute("name", resource.getName());
            setAttribute("timestamp", "");//String.valueOf(resource.getItime())); // Seems to be null
            setAttribute("currentAvailability", ImageManager.getAvailabilityIconFromAvailType(resource
                .getCurrentAvailability().getAvailabilityType()));
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

        public String getParentId() {
            return getAttribute("parentId");
        }
    }

    public int getMaximumRecentlyAddedToDisplay() {
        return maximumRecentlyAddedToDisplay;
    }

    public void setMaximumRecentlyAddedToDisplay(int maximumRecentlyAddedToDisplay) {
        this.maximumRecentlyAddedToDisplay = maximumRecentlyAddedToDisplay;
    }

    public int getMaximumRecentlyAddedWithinHours() {
        return maximumRecentlyAddedWithinHours;
    }

    public void setMaximumRecentlyAddedWithinHours(int maximumRecentlyAddedWithinHours) {
        this.maximumRecentlyAddedWithinHours = maximumRecentlyAddedWithinHours;
    }

    public long getOldestDate() {
        return oldestDate;
    }

    public void setOldestDate(long oldestDate) {
        this.oldestDate = oldestDate;
    }

}
