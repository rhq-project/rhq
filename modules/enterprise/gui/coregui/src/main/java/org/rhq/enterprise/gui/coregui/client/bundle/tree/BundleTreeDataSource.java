/*
 * RHQ Management Platform
 * Copyright (C) 2005-2010 Red Hat, Inc.
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
package org.rhq.enterprise.gui.coregui.client.bundle.tree;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.data.DSRequest;
import com.smartgwt.client.data.DSResponse;
import com.smartgwt.client.data.DataSourceField;
import com.smartgwt.client.data.Record;
import com.smartgwt.client.data.fields.DataSourceTextField;
import com.smartgwt.client.widgets.grid.ListGridRecord;
import com.smartgwt.client.widgets.tree.TreeNode;

import org.rhq.core.domain.bundle.Bundle;
import org.rhq.core.domain.bundle.BundleDeployment;
import org.rhq.core.domain.bundle.BundleDestination;
import org.rhq.core.domain.bundle.BundleVersion;
import org.rhq.core.domain.criteria.BundleCriteria;
import org.rhq.core.domain.criteria.BundleDeploymentCriteria;
import org.rhq.core.domain.criteria.BundleDestinationCriteria;
import org.rhq.core.domain.criteria.BundleVersionCriteria;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.gwt.BundleGWTServiceAsync;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.util.RPCDataSource;

/**
 * @author Greg Hinkle
 */
@SuppressWarnings("unchecked")
public class BundleTreeDataSource extends RPCDataSource {

    private BundleGWTServiceAsync bundleService = GWTServiceLookup.getBundleService();
    private final boolean canManageBundles;

    public BundleTreeDataSource(boolean canManageBundles) {
        super();
        this.canManageBundles = canManageBundles;
        List<DataSourceField> fields = addDataSourceFields();
        addFields(fields);
    }

    @Override
    protected List<DataSourceField> addDataSourceFields() {
        List<DataSourceField> fields = super.addDataSourceFields();

        DataSourceField idDataField = new DataSourceTextField("id", MSG.common_title_id());
        idDataField.setPrimaryKey(true);
        fields.add(idDataField);

        DataSourceTextField nameDataField = new DataSourceTextField("name", MSG.common_title_name());
        nameDataField.setCanEdit(false);
        fields.add(nameDataField);

        DataSourceTextField parentIdField = new DataSourceTextField("parentId", MSG.common_title_id_parent());
        parentIdField.setForeignKey("id");
        fields.add(parentIdField);

        return fields;
    }

    @Override
    protected void executeFetch(final DSRequest request, final DSResponse response) {

        String p = request.getCriteria().getAttribute("parentId");

        if (p == null) {

            // there is no parent - we are at the root of the tree
            // get the top nodes (the bundles)
            BundleCriteria criteria = new BundleCriteria();
            criteria.fetchDestinations(true);
            criteria.setPageControl(getPageControl(request));

            bundleService.findBundlesByCriteria(criteria, new AsyncCallback<PageList<Bundle>>() {
                public void onFailure(Throwable caught) {
                    CoreGUI.getErrorHandler().handleError(MSG.view_bundle_tree_loadFailure(), caught);
                    response.setStatus(DSResponse.STATUS_FAILURE);
                    processResponse(request.getRequestId(), response);
                }

                public void onSuccess(PageList<Bundle> result) {
                    response.setData(buildRecords(result));
                    response.setTotalRows(result.getTotalSize());
                    processResponse(request.getRequestId(), response);
                }
            });

        } else {

            // we are at an inner node, being asked to get the children of it
            if (p.endsWith("_versions")) {
                int bundleId = Integer.parseInt(p.substring(0, p.indexOf("_")));
                BundleVersionCriteria criteria = new BundleVersionCriteria();
                criteria.addFilterBundleId(bundleId);
                bundleService.findBundleVersionsByCriteria(criteria, new AsyncCallback<PageList<BundleVersion>>() {
                    public void onFailure(Throwable caught) {
                        CoreGUI.getErrorHandler().handleError(MSG.view_bundle_tree_loadFailure(), caught);
                        response.setStatus(DSResponse.STATUS_FAILURE);
                        processResponse(request.getRequestId(), response);
                    }

                    public void onSuccess(PageList<BundleVersion> result) {
                        response.setData(buildRecords(result));
                        response.setTotalRows(result.getTotalSize());
                        processResponse(request.getRequestId(), response);
                    }
                });

            } else if (p.endsWith("_deployments")) {
                int bundleId = Integer.parseInt(p.substring(0, p.indexOf("_")));
                BundleDeploymentCriteria criteria = new BundleDeploymentCriteria();
                criteria.fetchBundleVersion(true);
                criteria.addFilterBundleId(bundleId);
                bundleService.findBundleDeploymentsByCriteria(criteria,
                    new AsyncCallback<PageList<BundleDeployment>>() {
                        public void onFailure(Throwable caught) {
                            CoreGUI.getErrorHandler().handleError(MSG.view_bundle_tree_loadFailure(), caught);
                        }

                        public void onSuccess(PageList<BundleDeployment> result) {
                            response.setData(buildRecords(result));
                            processResponse(request.getRequestId(), response);
                        }
                    });

            } else if (p.endsWith("_destinations")) {
                final int bundleId = Integer.parseInt(p.substring(0, p.indexOf("_")));
                BundleDestinationCriteria criteria = new BundleDestinationCriteria();
                criteria.addFilterBundleId(bundleId);
                criteria.fetchDeployments(true);
                bundleService.findBundleDestinationsByCriteria(criteria,
                    new AsyncCallback<PageList<BundleDestination>>() {
                        public void onFailure(Throwable caught) {
                            CoreGUI.getErrorHandler().handleError(MSG.view_bundle_tree_loadFailure(), caught);
                        }

                        public void onSuccess(PageList<BundleDestination> result) {
                            response.setData(buildRecordsForKnownBundle(result, bundleId));
                            processResponse(request.getRequestId(), response);
                        }
                    });
            }
        }
    }

    @Override
    public Object copyValues(Record from) {
        return null; // don't need this method.
    }

    @Override
    public ListGridRecord[] buildRecords(Collection dataObjects) {
        return buildRecordsForKnownBundle(dataObjects, null);
    }

    public ListGridRecord[] buildRecordsForKnownBundle(Collection dataObjects, Integer bundleId) {
        if (dataObjects == null) {
            return null;
        }

        final List<ListGridRecord> records = new ArrayList<ListGridRecord>();

        for (Object item : dataObjects) {

            // the resultant item is a direct node to build
            records.add(copyValues(item));

            // now build the children of the node
            if (item instanceof Bundle) {
                Bundle bundle = (Bundle) item;

                // each bundle has two direct children - the versions and destinations folders
                TreeNode versionNode = new TreeNode(MSG.view_bundle_versions());
                versionNode.setID(bundle.getId() + "_versions");
                versionNode.setParentID(String.valueOf(bundle.getId()));
                versionNode.setName(MSG.view_bundle_versions());
                versionNode.setAttribute("name", MSG.view_bundle_versions());
                records.add(versionNode);

                TreeNode deploymentsNode = new TreeNode(MSG.view_bundle_destinations());
                deploymentsNode.setID(bundle.getId() + "_destinations");
                deploymentsNode.setParentID(String.valueOf(bundle.getId()));
                deploymentsNode.setName(MSG.view_bundle_destinations());
                records.add(deploymentsNode);
            } else if (item instanceof BundleDestination) {
                if (canManageBundles) {
                    BundleDestination dest = (BundleDestination) item;

                    // each destination has 0, 1 or more deployments
                    if (dest.getDeployments() != null) {
                        for (BundleDeployment deploy : dest.getDeployments()) {
                            records.add(copyValuesForKnownBundle(deploy, bundleId));
                        }
                    }
                }
            }
        }
        return records.toArray(new ListGridRecord[records.size()]);
    }

    @Override
    public ListGridRecord copyValues(Object from) {
        return copyValuesForKnownBundle(from, null);
    }

    public ListGridRecord copyValuesForKnownBundle(Object from, Integer bundleId) {
        String parentID;
        TreeNode node = new TreeNode();
        if (from instanceof Bundle) {
            Bundle bundle = (Bundle) from;
            node.setIsFolder(true);
            node.setIcon("subsystems/bundle/Bundle_16.png");
            node.setID(String.valueOf(bundle.getId()));
            node.setName(bundle.getName());

        } else if (from instanceof BundleVersion) {
            BundleVersion version = (BundleVersion) from;
            node.setIsFolder(false);
            node.setIcon("subsystems/bundle/BundleVersion_16.png");
            parentID = version.getBundle().getId() + "_versions";
            node.setParentID(parentID);
            node.setID(parentID + '_' + version.getId());
            node.setName(version.getVersion());

        } else if (from instanceof BundleDeployment) {
            BundleDeployment deployment = (BundleDeployment) from;
            node.setIsFolder(false);
            node.setIcon("subsystems/bundle/BundleDeployment_16.png");
            parentID = bundleId + "_destinations_" + deployment.getDestination().getId();
            node.setParentID(parentID);
            node.setID(bundleId + "_deployments_" + deployment.getId());
            if (deployment.isLive()) {
                node.setName("<span style=\"color: green; font-weight: bold\">(live)</span> " + deployment.getName());
            } else {
                node.setName(deployment.getName());
            }

        } else if (from instanceof BundleDestination) {
            BundleDestination destination = (BundleDestination) from;
            node.setIsFolder(true);
            node.setIcon("subsystems/bundle/BundleDestination_16.png");
            parentID = destination.getBundle().getId() + "_destinations";
            node.setParentID(parentID);
            node.setID(parentID + '_' + destination.getId());
            node.setName(destination.getName());
        }

        return node;
    }
}
