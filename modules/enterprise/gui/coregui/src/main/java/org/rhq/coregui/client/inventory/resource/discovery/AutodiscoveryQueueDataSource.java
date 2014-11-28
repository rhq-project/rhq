/*
 * RHQ Management Platform
 * Copyright (C) 2005-2011 Red Hat, Inc.
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
package org.rhq.coregui.client.inventory.resource.discovery;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.data.DSRequest;
import com.smartgwt.client.data.DSResponse;
import com.smartgwt.client.data.DataSource;
import com.smartgwt.client.data.fields.DataSourceTextField;
import com.smartgwt.client.types.DSDataFormat;
import com.smartgwt.client.types.DSProtocol;
import com.smartgwt.client.widgets.tree.TreeGrid;
import com.smartgwt.client.widgets.tree.TreeNode;

import org.rhq.core.domain.authz.Permission;
import org.rhq.core.domain.criteria.ResourceCriteria;
import org.rhq.core.domain.resource.InventoryStatus;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceCategory;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.core.domain.util.PageOrdering;
import org.rhq.core.domain.util.ResourceTypeUtility;
import org.rhq.coregui.client.CoreGUI;
import org.rhq.coregui.client.Messages;
import org.rhq.coregui.client.PermissionsLoadedListener;
import org.rhq.coregui.client.PermissionsLoader;
import org.rhq.coregui.client.gwt.GWTServiceLookup;
import org.rhq.coregui.client.gwt.ResourceGWTServiceAsync;
import org.rhq.coregui.client.util.Log;

/**
 * @author Greg Hinkle
 */
public class AutodiscoveryQueueDataSource extends DataSource {

    private static Messages MSG = CoreGUI.getMessages();

    public static final String NEW = MSG.common_button_new();
    public static final String IGNORED = MSG.view_autoDiscoveryQ_ignored();
    public static final String NEW_AND_IGNORED = MSG.view_autoDiscoveryQ_newAndIgnored();

    private static final String NO_MANAGE_INVENTORY_PERMS_EMPTY_MESSAGE = MSG.view_autoDiscoveryQ_noperm();
    private static final String EMPTY_MESSAGE = MSG.common_msg_noItemsToShow();

    private static final Permission MANAGE_INVENTORY = Permission.MANAGE_INVENTORY;

    private int unlimited = -1;
    private int maximumPlatformsToDisplay = -1;

    // Specify 60s timeout to compensate for slow loading of this view due to lack of paging of results.
    // TODO (ips, 08/31/11): Remove this once paging has been implemented.
    private ResourceGWTServiceAsync resourceService = GWTServiceLookup.getResourceService(60 * 1000);

    private PermissionsLoader permissionsLoader = new PermissionsLoader();
    private TreeGrid dataContainerReference = null;

    private List<AsyncCallback> failedFetchListeners = new ArrayList<AsyncCallback>();

    public AutodiscoveryQueueDataSource() {
        setClientOnly(false);
        setDataProtocol(DSProtocol.CLIENTCUSTOM);
        setDataFormat(DSDataFormat.CUSTOM);

        DataSourceTextField idField = new DataSourceTextField("id", MSG.common_title_id());
        idField.setPrimaryKey(true);

        DataSourceTextField parentIdField = new DataSourceTextField("parentId",
            MSG.view_autoDiscoveryQ_field_parentId());
        parentIdField.setForeignKey("id");

        DataSourceTextField resourceNameField = new DataSourceTextField("name", MSG.common_title_resource_name());

        DataSourceTextField resourceKeyField = new DataSourceTextField("resourceKey", MSG.common_title_resource_key());

        DataSourceTextField resourceTypeField = new DataSourceTextField("typeName", MSG.common_title_resource_type());

        DataSourceTextField descriptionField = new DataSourceTextField("description", MSG.common_title_description());

        DataSourceTextField timestampField = new DataSourceTextField("ctime",
            MSG.view_autoDiscoveryQ_field_discoveryTime());

        DataSourceTextField statusField = new DataSourceTextField("statusLabel",
            MSG.view_autoDiscoveryQ_field_inventoryStatus());

        setFields(idField, parentIdField, resourceNameField, resourceKeyField, resourceTypeField, descriptionField,
            statusField, timestampField);
    }

    public AutodiscoveryQueueDataSource(TreeGrid treeGrid) {
        this();
        this.dataContainerReference = treeGrid;
    }

    protected Object transformRequest(DSRequest request) {
        DSResponse response = new DSResponse();
        response.setAttribute("clientContext", request.getAttributeAsObject("clientContext"));
        // assume success
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
        final String platformId = request.getCriteria().getAttribute("parentId");
        final ArrayList<InventoryStatus> statuses = new ArrayList<InventoryStatus>();

        String statusesString = request.getCriteria().getAttributeAsString("status");
        if (statusesString != null) {
            if (NEW.equals(statusesString)) {
                statuses.add(InventoryStatus.NEW);
            } else if (IGNORED.equals(statusesString)) {
                statuses.add(InventoryStatus.IGNORED);
            } else {
                statuses.add(InventoryStatus.NEW);
                statuses.add(InventoryStatus.IGNORED);
            }
        } else {
            statuses.add(InventoryStatus.NEW);
        }

        //determine if has manage inventory perms, if so then chain and proceed with getting Q resources
        permissionsLoader.loadExplicitGlobalPermissions(new PermissionsLoadedListener() {
            @Override
            public void onPermissionsLoaded(Set<Permission> permissions) {
                if (permissions != null) {
                    if (!permissions.contains(MANAGE_INVENTORY)) {
                        Log.debug("(User does not have required managed inventory permissions. " + EMPTY_MESSAGE);
                        response.setTotalRows(0);
                        if (dataContainerReference != null) {
                            Log.trace("Setting better empty container message."
                                + NO_MANAGE_INVENTORY_PERMS_EMPTY_MESSAGE);
                            dataContainerReference.setEmptyMessage(NO_MANAGE_INVENTORY_PERMS_EMPTY_MESSAGE);
                        }
                        processResponse(request.getRequestId(), response);
                        return;
                    }

                    if (dataContainerReference != null) {
                        dataContainerReference.setEmptyMessage(EMPTY_MESSAGE);
                    }

                    if (null == platformId) {
                        // query for platforms
                        final PageControl pc = getPageControl(request);
                        resourceService.getQueuedPlatforms(statuses, pc, new AsyncCallback<PageList<Resource>>() {
                            public void onFailure(Throwable caught) {
                                CoreGUI.getErrorHandler().handleError(MSG.view_autoDiscoveryQ_loadFailure(), caught);
                                for (AsyncCallback failedFetchListener : failedFetchListeners) {
                                    failedFetchListener.onFailure(caught);
                                }
                            }

                            public void onSuccess(PageList<Resource> result) {
                                response.setData(buildPlatformNodes(result));
                                processResponse(request.getRequestId(), response);
                            }
                        });
                    } else {
                        final int platformResourceId = Integer.valueOf(platformId);
                        final Resource parentResourceStub = new Resource(platformResourceId);

                        ResourceCriteria fetchCriteria = new ResourceCriteria();
                        fetchCriteria.addFilterParentResourceId(platformResourceId);
                        fetchCriteria.addFilterResourceCategories(ResourceCategory.SERVER);
                        fetchCriteria.addFilterInventoryStatuses(statuses);
                        fetchCriteria.clearPaging();
                        fetchCriteria.addSortName(PageOrdering.ASC);
                        resourceService.findResourcesByCriteria(fetchCriteria, new AsyncCallback<PageList<Resource>>() {
                            public void onFailure(Throwable caught) {
                                CoreGUI.getErrorHandler().handleError(MSG.view_autoDiscoveryQ_loadFailure(), caught);
                                for (AsyncCallback failedFetchListener : failedFetchListeners) {
                                    failedFetchListener.onFailure(caught);
                                }
                            }

                            public void onSuccess(PageList<Resource> result) {
                                response.setData(buildServerNodes(parentResourceStub, result));
                                processResponse(request.getRequestId(), response);
                            }
                        });
                    }
                }
            }
        });
    }

    /**
     * @param callback The onFailure() method will be invoked if the DS fails a fetch operation.
     */
    public void addFailedFetchListener(AsyncCallback callback) {
        failedFetchListeners.add(callback);
    }

    private TreeNode[] buildPlatformNodes(PageList<Resource> platforms) {

        ArrayList<ResourceTreeNode> nodes = new ArrayList<ResourceTreeNode>();
        for (Resource platform : platforms) {
            nodes.add(new ResourceTreeNode(platform));
        }
        TreeNode[] treeNodes = nodes.toArray(new TreeNode[nodes.size()]);
        return treeNodes;
    }

    private TreeNode[] buildServerNodes(Resource parentResource, PageList<Resource> servers) {

        ArrayList<ResourceTreeNode> nodes = new ArrayList<ResourceTreeNode>();
        for (Resource server : servers) {
            server.setParentResource(parentResource); // set the parent so the tree relationship gets set
            nodes.add(new ResourceTreeNode(server));
        }
        TreeNode[] treeNodes = nodes.toArray(new TreeNode[nodes.size()]);
        return treeNodes;
    }

    /**
     * Returns a pre-populated PageControl based on the provided DSRequest. This will set sort fields,
     * pagination, but *not* filter fields.
     *
     * @param request the request to turn into a page control
     * @return the page control for passing to criteria and other queries
     */
    protected PageControl getPageControl(DSRequest request) {
        // Initialize paging.
        PageControl pageControl;
        if (getMaximumPlatformsToDisplay() > -1) {
            pageControl = new PageControl(0, getMaximumPlatformsToDisplay());
        } else {
            pageControl = new PageControl(0, unlimited);
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

    public static class ResourceTreeNode extends TreeNode {

        private Resource resource;

        private ResourceTreeNode(Resource resource) {
            this.resource = resource;

            String id = String.valueOf(resource.getId());
            String parentId = resource.getParentResource() == null ? null : String.valueOf((resource
                .getParentResource().getId()));

            setID(id);
            setParentID(parentId);
            setIsFolder(null == parentId);

            setAttribute("id", id);
            setAttribute("parentId", parentId);
            setAttribute("name", resource.getName());
            setAttribute("typeName", ResourceTypeUtility.displayName(resource.getResourceType()));
            setAttribute("resourceKey", resource.getResourceKey());
            setAttribute("description", resource.getDescription());
            setAttribute("ctime", new Date(resource.getCtime()));
            setAttribute("status", resource.getInventoryStatus().name());
            switch (resource.getInventoryStatus()) {
            case NEW:
                setAttribute("statusLabel", MSG.common_button_new());
                break;
            case COMMITTED:
                setAttribute("statusLabel", MSG.view_autoDiscoveryQ_committed());
                break;
            case IGNORED:
                setAttribute("statusLabel", MSG.view_autoDiscoveryQ_ignored());
                break;
            case UNINVENTORIED:
                setAttribute("statusLabel", MSG.view_autoDiscoveryQ_uninventoried());
                break;
            }
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

    public int getMaximumPlatformsToDisplay() {
        return maximumPlatformsToDisplay;
    }

    public void setMaximumPlatformsToDisplay(int maximumPlatformsToDisplay) {
        this.maximumPlatformsToDisplay = maximumPlatformsToDisplay;
    }

}
