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

package org.rhq.enterprise.gui.coregui.client.dashboard.portlets;

import com.smartgwt.client.data.Criteria;
import com.smartgwt.client.data.DSRequest;
import com.smartgwt.client.data.DSResponse;
import com.smartgwt.client.data.DataSource;
import com.smartgwt.client.data.DataSourceField;
import com.smartgwt.client.types.DSDataFormat;
import com.smartgwt.client.types.FieldType;
import com.smartgwt.client.widgets.grid.ListGridRecord;
import com.smartgwt.client.widgets.tree.TreeNode;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.gui.coregui.client.util.RPCDataSource;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class RecentlyAddedResourceDS extends DataSource {

    boolean fetched;

    public RecentlyAddedResourceDS() {
        setClientOnly(false);
        setDataFormat(DSDataFormat.CUSTOM);
        DataSourceField resourceNameField = new DataSourceField("resourceName", FieldType.TEXT, "Resource Name");
        resourceNameField.setPrimaryKey(true);

        DataSourceField timestampField = new DataSourceField("timestamp", FieldType.TEXT, "Date/Time");

        setFields(resourceNameField, timestampField);
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

    public void executeFetch(String requestId, DSRequest request, DSResponse response) {
        if (fetched) {
            return;
        }
        PageList<RecentlyAddedResource> list = new PageList<RecentlyAddedResource>();
        RecentlyAddedResource platform1 = new RecentlyAddedResource("Platform 1", "today");
        RecentlyAddedResource server1 = new RecentlyAddedResource("Server 1", "today", platform1);
        RecentlyAddedResource service1 = new RecentlyAddedResource("Service 1", "today", server1);

        RecentlyAddedResource platform2 = new RecentlyAddedResource("Platform 2", "today");
        RecentlyAddedResource server2 = new RecentlyAddedResource("Server 2", "today", platform2);
        RecentlyAddedResource service2 = new RecentlyAddedResource("Service 2", "today", server2);

        list.add(platform1);
        list.add(server1);
        list.add(service1);
        list.add(platform2);
        list.add(server2);
        list.add(service2);

        response.setData(buildNodes(list));
        fetched = true;

        processResponse(requestId, response);
    }

    private TreeNode[] buildNodes(List<RecentlyAddedResource> list) {
        TreeNode[] treeNodes = new TreeNode[list.size()];
        for (int i = 0; i < list.size(); ++i) {
            treeNodes[i] = new RecentlyAddedTreeNode(list.get(i));
        }
        return treeNodes;
    }

    static class RecentlyAddedTreeNode extends TreeNode {
        private RecentlyAddedResource recentlyAdded;

        public RecentlyAddedTreeNode(RecentlyAddedResource recentlyAdded) {
            this.recentlyAdded = recentlyAdded;
            setID(recentlyAdded.getResourceName());
            if (recentlyAdded.getParent() != null) {
                setParentID(recentlyAdded.getParent().getResourceName());    
            }

            setAttribute("resourceName", recentlyAdded.getResourceName());
            setAttribute("timestamp", recentlyAdded.getTimestamp());
            setIsFolder(recentlyAdded.getResourceName().startsWith("Platform"));
        }
    }
}
