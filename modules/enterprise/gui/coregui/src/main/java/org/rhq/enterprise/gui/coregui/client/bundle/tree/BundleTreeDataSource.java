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

    public BundleTreeDataSource() {
        super();
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
        // load the bundles

        if (p == null) {

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
                int bundleId = Integer.parseInt(p.substring(0, p.indexOf("_")));
                BundleDestinationCriteria criteria = new BundleDestinationCriteria();
                criteria.addFilterBundleId(bundleId);
                bundleService.findBundleDestinationsByCriteria(criteria,
                    new AsyncCallback<PageList<BundleDestination>>() {
                        public void onFailure(Throwable caught) {
                            CoreGUI.getErrorHandler().handleError(MSG.view_bundle_tree_loadFailure(), caught);
                        }

                        public void onSuccess(PageList<BundleDestination> result) {
                            response.setData(buildRecords(result));
                            processResponse(request.getRequestId(), response);
                        }
                    });
            }
        }
    }

    @Override
    public Object copyValues(Record from) {
        return null; // TODO: Implement this method.
    }

    @Override
    public ListGridRecord[] buildRecords(Collection list) {
        if (list == null) {
            return null;
        }

        List<ListGridRecord> records = new ArrayList<ListGridRecord>();

        for (Object item : list) {
            records.add(copyValues(item));
            if (item instanceof Bundle) {
                Bundle bundle = (Bundle) item;

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
            }
        }
        return records.toArray(new ListGridRecord[records.size()]);
    }

    @Override
    public ListGridRecord copyValues(Object from) {
        TreeNode node = new TreeNode();
        if (from instanceof Bundle) {
            Bundle bundle = (Bundle) from;
            node.setID(String.valueOf(bundle.getId()));
            node.setName(bundle.getName());
            node.setIcon("subsystems/bundle/Bundle_16.png");

        } else if (from instanceof BundleVersion) {
            BundleVersion version = (BundleVersion) from;
            node.setName(version.getVersion());
            node.setID(version.getBundle().getId() + "_versions_" + version.getId());
            node.setParentID(version.getBundle().getId() + "_versions");
            node.setIsFolder(false);
            node.setIcon("subsystems/bundle/BundleVersion_16.png");

        } else if (from instanceof BundleDeployment) {
            BundleDeployment deployment = (BundleDeployment) from;
            node.setName(deployment.getName() + " (" + deployment.getBundleVersion().getVersion() + ")");
            node.setID(deployment.getBundleVersion().getBundle().getId() + "_deployments_" + deployment.getId());
            node.setParentID(deployment.getBundleVersion().getBundle().getId() + "_deployments");
            node.setIsFolder(false);
            node.setIcon("subsystems/bundle/BundleDeployment_16.png");
        } else if (from instanceof BundleDestination) {
            BundleDestination destination = (BundleDestination) from;
            node.setName(destination.getName());
            node.setID(destination.getBundle().getId() + "_destinations_" + destination.getId());
            node.setParentID(destination.getBundle().getId() + "_destinations");
            node.setIsFolder(false);
            node.setIcon("subsystems/bundle/BundleDestination_16.png");
        }
        return node;
    }
}
