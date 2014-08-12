/*
 * RHQ Management Platform
 * Copyright (C) 2005-2014 Red Hat, Inc.
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
package org.rhq.coregui.client.bundle.tree;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.google.gwt.i18n.client.NumberFormat;
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
import org.rhq.core.domain.bundle.BundleGroup;
import org.rhq.core.domain.bundle.BundleVersion;
import org.rhq.core.domain.criteria.BundleCriteria;
import org.rhq.core.domain.criteria.BundleDeploymentCriteria;
import org.rhq.core.domain.criteria.BundleDestinationCriteria;
import org.rhq.core.domain.criteria.BundleGroupCriteria;
import org.rhq.core.domain.criteria.BundleVersionCriteria;
import org.rhq.core.domain.criteria.Criteria;
import org.rhq.core.domain.util.PageList;
import org.rhq.coregui.client.CoreGUI;
import org.rhq.coregui.client.ImageManager;
import org.rhq.coregui.client.gwt.BundleGWTServiceAsync;
import org.rhq.coregui.client.gwt.GWTServiceLookup;
import org.rhq.coregui.client.util.RPCDataSource;
import org.rhq.coregui.client.util.StringUtility;

/**
 * @author Greg Hinkle
 * @author John Mazzitelli
 */
public class BundleTreeDataSource extends RPCDataSource<Object, Criteria> {

    // this is the field that will contain data which can be used to sort the tree nodes
    public static final String FIELD_SORT_VALUE = "sortValue";

    private final BundleGWTServiceAsync bundleService = GWTServiceLookup.getBundleService();

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

        DataSourceTextField sortValueDataField = new DataSourceTextField(FIELD_SORT_VALUE);
        sortValueDataField.setCanView(false);
        fields.add(sortValueDataField);

        DataSourceTextField parentIdField = new DataSourceTextField("parentId", MSG.common_title_id_parent());
        parentIdField.setForeignKey("id");
        fields.add(parentIdField);

        return fields;
    }

    @Override
    protected void executeFetch(final DSRequest request, final DSResponse response, final Criteria unused) {

        final String parentId = request.getCriteria().getAttribute("parentId");

        if (parentId == null) {

            // there is no parent - we are at the root of the tree
            // get the bundles and build the bundle groups (including unassigned) from that
            BundleCriteria criteria = new BundleCriteria();
            criteria.fetchBundleGroups(true);

            bundleService.findBundlesByCriteria(criteria, new AsyncCallback<PageList<Bundle>>() {
                public void onFailure(Throwable caught) {
                    CoreGUI.getErrorHandler().handleError(MSG.view_bundle_tree_loadFailure(), caught);
                    response.setStatus(DSResponse.STATUS_FAILURE);
                    processResponse(request.getRequestId(), response);
                }

                public void onSuccess(final PageList<Bundle> result) {
                    BundleGroup unassignedBundleGroup = new BundleGroup();
                    unassignedBundleGroup.setId(0); // ID=0 is an indicator we use to denote the unassigned group
                    unassignedBundleGroup.setName(MSG.view_bundle_tree_unassigned_name());
                    unassignedBundleGroup.setDescription(MSG.view_bundle_tree_unassigned_desc());

                    // Because findBundleGroupsByCriteria would not have given us all unassigned bundles, we used
                    // findBundlesByCriteria. But we need to organize our tree structure with groups at the top, so
                    // we need to put our results aggregated in groups.
                    HashMap<Integer, BundleGroup> visibleBundleGroups = new HashMap<Integer, BundleGroup>();
                    for (Bundle bundle : result) {
                        Set<BundleGroup> bundleBundleGroups = bundle.getBundleGroups();
                        if (bundleBundleGroups == null || bundleBundleGroups.isEmpty()) {
                            unassignedBundleGroup.addBundle(bundle);
                        } else {
                            for (BundleGroup bundleBundleGroup : bundleBundleGroups) {
                                BundleGroup theGroup = visibleBundleGroups.get(bundleBundleGroup.getId());
                                if (theGroup == null) {
                                    visibleBundleGroups.put(bundleBundleGroup.getId(), bundleBundleGroup);
                                    theGroup = bundleBundleGroup;
                                }
                                theGroup.addBundle(bundle);
                            }
                        }
                    }

                    final ArrayList<BundleGroup> allVisibleBundleGroups = new ArrayList<BundleGroup>(visibleBundleGroups.values());
                    if (!unassignedBundleGroup.getBundles().isEmpty()) {
                        allVisibleBundleGroups.add(unassignedBundleGroup);
                    }

                    BundleGroupCriteria bundleGroupCriteria = new BundleGroupCriteria();
                    bundleGroupCriteria.addFilterIds(visibleBundleGroups.keySet().toArray(new Integer[0]));
                    bundleService.findBundleGroupsByCriteria(bundleGroupCriteria, new AsyncCallback<PageList<BundleGroup>>() {
                        public void onFailure(Throwable caught) {
                            // just log a message, but keep going, this just means we can't show lock icons where applicable
                            CoreGUI.getErrorHandler().handleError(MSG.view_bundle_tree_loadFailure(), caught);
                        }

                        public void onSuccess(PageList<BundleGroup> result) {
                            // if any of the bundle group tree nodes represent bundle groups the user isn't allowed
                            // to see, then mark the node with a locked icon
                            HashSet<Integer> permittedBundleGroups = new HashSet<Integer>();
                            for (BundleGroup bg : result) {
                                permittedBundleGroups.add(bg.getId());
                            }
                            ListGridRecord[] dataRecords = buildRecords(allVisibleBundleGroups);
                            for (ListGridRecord dataRecord : dataRecords) {
                                // we only want to examine bundle group records - and they are the only ones
                                // with ID attributes that are a simple number without "_" character.
                                // Ignore the Unassigned Bundle Group (whose id = "0") - never show a lock for that.
                                TreeNode dataRecordNode = (TreeNode) dataRecord;
                                String idString = dataRecordNode.getAttribute("id");
                                if (!idString.contains("_") && !idString.equals("0")
                                    && !permittedBundleGroups.contains(Integer.valueOf(idString))) {
                                    dataRecordNode.setIcon(ImageManager.getLockedIcon());
                                    dataRecordNode.setEnabled(false);
                                }
                            }
                            response.setData(dataRecords);
                            response.setTotalRows(allVisibleBundleGroups.size());
                            processResponse(request.getRequestId(), response);
                        }
                    });
                }
            });

        } else {
            String[] splitParentId = parentId.split("_", 3); // <bundleGroupId>_<bundleId>_<and the rest>
            final Integer bundleGroupId = Integer.parseInt(splitParentId[0]);
            Integer tmp;
            try {
                tmp = Integer.parseInt(splitParentId[1]);
            } catch (NumberFormatException e) {
                tmp = null;
            }
            final Integer bundleId = tmp;

            // we are at an inner node, being asked to get the children of it
            if (parentId.endsWith("_versions")) {
                BundleVersionCriteria criteria = new BundleVersionCriteria();
                criteria.addFilterBundleId(bundleId);
                bundleService.findBundleVersionsByCriteria(criteria, new AsyncCallback<PageList<BundleVersion>>() {
                    public void onFailure(Throwable caught) {
                        CoreGUI.getErrorHandler().handleError(MSG.view_bundle_tree_loadFailure(), caught);
                        response.setStatus(DSResponse.STATUS_FAILURE);
                        processResponse(request.getRequestId(), response);
                    }

                    public void onSuccess(PageList<BundleVersion> result) {
                        response.setData(buildRecordsForKnownBundle(result, bundleGroupId, bundleId));
                        response.setTotalRows(result.getTotalSize());
                        processResponse(request.getRequestId(), response);
                    }
                });

            } else if (parentId.endsWith("_deployments")) {
                BundleDeploymentCriteria criteria = new BundleDeploymentCriteria();
                criteria.fetchBundleVersion(true);
                criteria.addFilterBundleId(bundleId);
                bundleService.findBundleDeploymentsByCriteria(criteria,
                        new AsyncCallback<PageList<BundleDeployment>>() {
                            public void onFailure(Throwable caught) {
                                CoreGUI.getErrorHandler().handleError(MSG.view_bundle_tree_loadFailure(), caught);
                            }

                            public void onSuccess(PageList<BundleDeployment> result) {
                                response.setData(buildRecordsForKnownBundle(result, bundleGroupId, bundleId));
                                processResponse(request.getRequestId(), response);
                            }
                        });

            } else if (parentId.endsWith("_destinations")) {
                BundleDestinationCriteria criteria = new BundleDestinationCriteria();
                criteria.addFilterBundleId(bundleId);
                criteria.fetchDeployments(true);
                bundleService.findBundleDestinationsByCriteria(criteria,
                    new AsyncCallback<PageList<BundleDestination>>() {
                        public void onFailure(Throwable caught) {
                            CoreGUI.getErrorHandler().handleError(MSG.view_bundle_tree_loadFailure(), caught);
                        }

                        public void onSuccess(PageList<BundleDestination> result) {
                            response.setData(buildRecordsForKnownBundle(result, bundleGroupId, bundleId));
                            processResponse(request.getRequestId(), response);
                        }
                    });
            } else {
                // we are at a child node under a bundle node - its an individual destination or deployment node
                BundleCriteria criteria = new BundleCriteria();
                criteria.addFilterId(bundleId);
                criteria.fetchDestinations(true);

                bundleService.findBundlesByCriteria(criteria, new AsyncCallback<PageList<Bundle>>() {
                    public void onFailure(Throwable caught) {
                        CoreGUI.getErrorHandler().handleError(MSG.view_bundle_tree_loadFailure(), caught);
                        response.setStatus(DSResponse.STATUS_FAILURE);
                        processResponse(request.getRequestId(), response);
                    }

                    public void onSuccess(PageList<Bundle> result) {
                        final ListGridRecord[] listGridRecordsData = buildRecordsForKnownBundle(result, bundleGroupId, bundleId);
                        response.setData(listGridRecordsData);
                        processResponse(request.getRequestId(), response);
                    }
                });
            }
        }
    }

    @Override
    protected Criteria getFetchCriteria(DSRequest request) {
        // our executeFetch will determine on its own what criteria to use based on what is to be fetched
        // thus we don't return anything here and let the executeFetch do everything
        return null;
    }

    @Override
    public Object copyValues(Record from) {
        return null; // don't need this method.
    }

    @Override
    public ListGridRecord[] buildRecords(Collection dataObjects) {
        return buildRecordsForKnownBundle(dataObjects, null, null);
    }

    private ListGridRecord[] buildRecordsForKnownBundle(Collection dataObjects, Integer bundleGroupId, Integer bundleId) {
        if (dataObjects == null) {
            return null;
        }

        final List<ListGridRecord> records = new ArrayList<ListGridRecord>();

        for (Object item : dataObjects) {

            // the resultant item is a direct node to build
            records.add(copyValuesForKnownBundle(item, bundleGroupId, bundleId));

            // now build the children of the node
            if (item instanceof BundleGroup) {
                BundleGroup bundleGroup = (BundleGroup) item;
                Set<Bundle> bundles = bundleGroup.getBundles();
                if (bundles != null) {
                    for (Bundle bundle : bundles) {
                        records.add(copyValuesForKnownBundle(bundle, bundleGroup.getId(), bundle.getId()));
                        // while we are here, automatically add the two direct children - the versions and destinations folders
                        TreeNode versionNode = new TreeNode(MSG.view_bundle_versions());
                        versionNode.setID(bundleGroup.getId() + "_" + bundle.getId() + "_versions");
                        versionNode.setParentID(bundleGroup.getId() + "_" + bundle.getId());
                        versionNode.setName(MSG.view_bundle_versions());
                        records.add(versionNode);

                        TreeNode deploymentsNode = new TreeNode(MSG.view_bundle_destinations());
                        deploymentsNode.setID(bundleGroup.getId() + "_" + bundle.getId() + "_destinations");
                        deploymentsNode.setParentID(bundleGroup.getId() + "_" + bundle.getId());
                        deploymentsNode.setName(MSG.view_bundle_destinations());
                        records.add(deploymentsNode);
                    }
                }

            } else if (item instanceof Bundle) {
                Bundle bundle = (Bundle) item;

                // each bundle has two direct children - the versions and destinations folders
                TreeNode versionNode = new TreeNode(MSG.view_bundle_versions());
                versionNode.setID(bundleGroupId.toString() + "_" + bundle.getId() + "_versions");
                versionNode.setParentID(bundleGroupId.toString() + "_" + bundle.getId());
                versionNode.setName(MSG.view_bundle_versions());
                records.add(versionNode);

                TreeNode deploymentsNode = new TreeNode(MSG.view_bundle_destinations());
                deploymentsNode.setID(bundleGroupId.toString() + "_" + bundle.getId() + "_destinations");
                deploymentsNode.setParentID(bundleGroupId.toString() + "_" + bundle.getId());
                deploymentsNode.setName(MSG.view_bundle_destinations());
                records.add(deploymentsNode);

            } else if (item instanceof BundleDestination) {
                BundleDestination dest = (BundleDestination) item;

                // each destination has 0, 1 or more deployments
                if (dest.getDeployments() != null) {
                    for (BundleDeployment deploy : dest.getDeployments()) {
                        records.add(copyValuesForKnownBundle(deploy, bundleGroupId, bundleId));
                    }
                }
            }
        }
        return records.toArray(new ListGridRecord[records.size()]);
    }

    @Override
    public ListGridRecord copyValues(Object from) {
        return copyValuesForKnownBundle(from, null, null);
    }

    private ListGridRecord copyValuesForKnownBundle(Object from, Integer bundleGroupId, Integer bundleId) {
        String parentID;
        TreeNode node = new TreeNode();
        String sortValue = "";

        if (from instanceof BundleGroup) {
            BundleGroup bundleGroup = (BundleGroup) from;
            node.setIsFolder(true);
            node.setIcon("subsystems/bundle/BundleGroup_16.png");
            node.setID(String.valueOf(bundleGroup.getId()));
            node.setName(StringUtility.escapeHtml(bundleGroup.getName()));

            if (bundleGroup.getId() == 0) {
                node.setEnabled(false);
                sortValue = "\uFFFDZZZZ"; // always show this as the last folder node in the tree
            } else{
                sortValue = bundleGroup.getName();
            }
        } else if (from instanceof Bundle) {
            Bundle bundle = (Bundle) from;
            node.setIsFolder(true);
            node.setIcon("subsystems/bundle/Bundle_16.png");
            node.setID(String.valueOf(bundleGroupId) + "_" + bundle.getId());
            node.setParentID(String.valueOf(bundleGroupId));
            node.setName(StringUtility.escapeHtml(bundle.getName()));
            sortValue = bundle.getName();
        } else if (from instanceof BundleVersion) {
            BundleVersion version = (BundleVersion) from;
            node.setIsFolder(false);
            node.setIcon("subsystems/bundle/BundleVersion_16.png");
            parentID = bundleGroupId.toString() + "_" + version.getBundle().getId() + "_versions";
            node.setParentID(parentID);
            node.setID(parentID + '_' + version.getId());
            node.setName(version.getVersion());
            sortValue = NumberFormat.getFormat("000000").format(version.getVersionOrder());

        } else if (from instanceof BundleDeployment) {
            BundleDeployment deployment = (BundleDeployment) from;
            node.setIsFolder(false);
            node.setIcon("subsystems/bundle/BundleDeployment_16.png");
            parentID = bundleGroupId.toString() + "_" + bundleId + "_destinations_" + deployment.getDestination().getId();
            node.setParentID(parentID);
            node.setID(bundleGroupId.toString() + "_" + bundleId + "_deployments_" + deployment.getId());
            String name = StringUtility.escapeHtml(deployment.getName());
            if (deployment.isLive()) {
                node.setName("<span style=\"color: green; font-weight: bold\">(live)</span> " + name);
            } else {
                node.setName(name);
            }
            sortValue = deployment.getName();

        } else if (from instanceof BundleDestination) {
            BundleDestination destination = (BundleDestination) from;
            node.setIsFolder(true);
            node.setIcon("subsystems/bundle/BundleDestination_16.png");
            parentID = bundleGroupId.toString() + "_" + destination.getBundle().getId() + "_destinations";
            node.setParentID(parentID);
            node.setID(parentID + '_' + destination.getId());
            node.setName(StringUtility.escapeHtml(destination.getName()));
            sortValue = destination.getName();
        }

        node.setAttribute(FIELD_SORT_VALUE, sortValue.toLowerCase());

        return node;
    }
}
