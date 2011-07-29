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
package org.rhq.enterprise.gui.coregui.client.drift;

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

import org.rhq.core.domain.criteria.BaseCriteria;
import org.rhq.core.domain.criteria.BasicDriftChangeSetCriteria;
import org.rhq.core.domain.criteria.DriftChangeSetCriteria;
import org.rhq.core.domain.criteria.DriftCriteria;
import org.rhq.core.domain.criteria.DriftJPACriteria;
import org.rhq.core.domain.drift.Drift;
import org.rhq.core.domain.drift.DriftChangeSet;
import org.rhq.core.domain.util.PageList;
import org.rhq.core.domain.util.PageOrdering;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.gwt.DriftGWTServiceAsync;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.util.RPCDataSource;

/**
 * @author John Mazzitelli
 */
@SuppressWarnings("unchecked")
public abstract class AbstractDriftChangeSetsTreeDataSource extends RPCDataSource<Object, BaseCriteria> {

    public static final String ATTR_PARENT_ID = "parentId";
    public static final String ATTR_ID = "id";
    public static final String ATTR_NAME = "name";

    private DriftGWTServiceAsync driftService = GWTServiceLookup.getDriftService();
    private final boolean canManageDrift;

    public AbstractDriftChangeSetsTreeDataSource(boolean canManageDrift) {
        super();
        this.canManageDrift = canManageDrift;
        List<DataSourceField> fields = addDataSourceFields();
        addFields(fields);
    }

    @Override
    protected List<DataSourceField> addDataSourceFields() {
        List<DataSourceField> fields = super.addDataSourceFields();

        DataSourceField idDataField = new DataSourceTextField(ATTR_ID, MSG.common_title_id());
        idDataField.setPrimaryKey(true);
        fields.add(idDataField);

        DataSourceTextField nameDataField = new DataSourceTextField(ATTR_NAME, MSG.common_title_name());
        nameDataField.setCanEdit(false);
        fields.add(nameDataField);

        DataSourceTextField parentIdField = new DataSourceTextField(ATTR_PARENT_ID, MSG.common_title_id_parent());
        parentIdField.setForeignKey(ATTR_ID);
        fields.add(parentIdField);

        return fields;
    }

    @Override
    protected void executeFetch(final DSRequest request, final DSResponse response, final BaseCriteria unused) {

        String parentId = request.getCriteria().getAttribute(ATTR_PARENT_ID);

        if (parentId == null) {

            // There is no parent - we are at the root of the tree.
            // Get the top nodes (the change sets) but to be as fast as possible don't load their drifts.
            // We will lazily load drifts when the these changeset tree nodes are opened
            DriftChangeSetCriteria criteria = getDriftChangeSetCriteria(request);

            driftService.findDriftChangeSetsByCriteria(criteria, new AsyncCallback<PageList<DriftChangeSet>>() {
                public void onSuccess(PageList<DriftChangeSet> result) {
                    response.setData(buildRecords(result));
                    response.setTotalRows(result.getTotalSize());
                    processResponse(request.getRequestId(), response);
                }

                public void onFailure(Throwable caught) {
                    CoreGUI.getErrorHandler().handleError(MSG.view_drift_changeset_tree_loadFailure(), caught);
                    response.setStatus(DSResponse.STATUS_FAILURE);
                    processResponse(request.getRequestId(), response);
                }
            });
        } else {
            String changesetId = parentId;
            DriftCriteria criteria = new DriftJPACriteria();
            criteria.addFilterChangeSetId(changesetId);

            driftService.findDriftsByCriteria(criteria, new AsyncCallback<PageList<Drift>>() {
                public void onFailure(Throwable caught) {
                    CoreGUI.getErrorHandler().handleError(MSG.view_drift_changeset_tree_loadFailure(), caught);
                    response.setStatus(DSResponse.STATUS_FAILURE);
                    processResponse(request.getRequestId(), response);
                }

                public void onSuccess(PageList<Drift> result) {
                    response.setData(buildRecords(result));
                    response.setTotalRows(result.getTotalSize());
                    processResponse(request.getRequestId(), response);
                }
            });

            /*
             * I am leaving this code commented for future reference. Because today the drit change set tree only
             * shows simple changeset->drift tree, we don't need this. But if we need to have
             * multiple types of child nodes, we'll need something like below to query
             * for the different child node data. For example, we'd need this if we have a tree like:
             * ChangeSet
             *         |____Resources
             *         |            |____ My resource 1
             *         |            |____ ...
             *         |_____Drifts
             *                    |___ Drift #1
             *                    |___ Drift #2
             *                    |___ ...
             * Today we only have Drifts child nodes, so we don't need to have that intermediate
             * "Drifts" node. If we later want to introduce different node types (like Resources)
             * we need additional code like below.
             *

            // we are at an inner node, being asked to get the children of it
            if (p.endsWith("_drifts")) {
                // ...load drift items like above in the real code...
            } else if (p.endsWith("_resources")) {
                // ...load resource items and put those nodes in the tree...
            } else {
                // This is an unknown type of node, so just log an error.
                // Note that if, in the future, we have other types of nodes (like maybe resource nodes for group change sets?)
                // we'll add more if-else statements above to process those different nodes.
                CoreGUI.getErrorHandler().handleError(MSG.view_drift_changeset_tree_loadFailure());
            }

             *
             */
        }

        return;
    }

    /**
     * Returns a criteria that will be used to obtain the root nodes for the tree - that is,
     * the top level change set entries.
     * 
     * Subclasses can override this method, or modify the returned criteria to further
     * filter the change sets that are to be returned.
     * 
     * @param request the request being made of the tree
     * 
     * @return the criteria to use when querying for change setss 
     */
    protected DriftChangeSetCriteria getDriftChangeSetCriteria(final DSRequest request) {
        BasicDriftChangeSetCriteria criteria = new BasicDriftChangeSetCriteria();
        criteria.addSortVersion(PageOrdering.DESC);
        criteria.setPageControl(getPageControl(request));
        return criteria;
    }

    @Override
    protected BaseCriteria getFetchCriteria(DSRequest request) {
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
        return buildRecordsForKnownChangeSets(dataObjects, null);
    }

    public ListGridRecord[] buildRecordsForKnownChangeSets(Collection dataObjects, Integer changeSetId) {
        if (dataObjects == null) {
            return null;
        }

        final List<ListGridRecord> records = new ArrayList<ListGridRecord>();

        for (Object item : dataObjects) {

            // the resultant item is a direct node to build
            records.add(copyValues(item));

            // now build the children of the node
            /*
             * We do not have the need for building any intermeidate nodes today.
             * There is nothing to do, but if in the future we have differnet node types,
             * we'll probably want intermediate nodes that we create here. See BundleTreeDataSource
             * for an example of where this is already done. Commenting this out just as an
             * example of how this can be done.
             
            if (item instanceof DriftChangeSet) {
                DriftChangeSet changeset = (DriftChangeSet) item;

                // each bundle has two direct children - the versions and destinations folders
                TreeNode versionNode = new TreeNode(MSG.view_drift());
                versionNode.setID(changeset.getId() + "_drifts");
                versionNode.setParentID(changeset.getId());
                versionNode.setName(MSG.view_drift());
                versionNode.setAttribute("name", MSG.view_drift());
                records.add(versionNode);
            } else if (item instanceof Drift) {
                if (canManageDrift) {
                    records.add(copyValuesForKnownDriftChangeSet(driftItem, changeSetId));
                }
            }

             *
             */
        }

        return records.toArray(new ListGridRecord[records.size()]);
    }

    @Override
    public ListGridRecord copyValues(Object from) {
        return copyValuesForKnownDriftChangeSet(from, null);
    }

    public ListGridRecord copyValuesForKnownDriftChangeSet(Object from, Integer changeSetId) {
        TreeNode node;
        if (from instanceof DriftChangeSet) {
            DriftChangeSet changeset = (DriftChangeSet) from;
            node = new AbstractDriftChangeSetsTreeView.ChangeSetTreeNode(changeset);
        } else if (from instanceof Drift) {
            Drift drift = (Drift) from;
            node = new AbstractDriftChangeSetsTreeView.DriftTreeNode(drift);
        } else {
            throw new IllegalArgumentException("please report this bug - bad value: " + from);
        }
        // if, in the future, we add more node types, we'll add more else-if statements here

        return node;
    }
}
