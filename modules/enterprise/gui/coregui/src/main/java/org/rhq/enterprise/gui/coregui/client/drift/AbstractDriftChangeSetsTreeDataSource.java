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
import org.rhq.core.domain.criteria.GenericDriftChangeSetCriteria;
import org.rhq.core.domain.criteria.GenericDriftCriteria;
import org.rhq.core.domain.drift.Drift;
import org.rhq.core.domain.drift.DriftChangeSet;
import org.rhq.core.domain.drift.DriftConfiguration;
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

        // if parentId is null, the nodes that we need to create are the top-level drift configurations
        // if parentId is a number (that is, a string that doesn't have a "_" separator), it is a drift config node.
        // if parentId is has two numbers separated with a "_", it is a changeset

        if (parentId == null) {
            fetchDriftConfigurations(request, response); // we are at the root of the tree - get the top nodes (the drift configs)
        } else if (parentId.indexOf('_') == -1) {
            // There is no parent - we are at the root of the tree.
            // Get the top nodes (the change sets) but to be as fast as possible don't load their drifts.
            // We will lazily load drifts when the these changeset tree nodes are opened
            GenericDriftChangeSetCriteria criteria = getDriftChangeSetCriteria(request);

            driftService.findDriftChangeSetsByCriteria(criteria,
                new AsyncCallback<PageList<? extends DriftChangeSet>>() {
                    public void onSuccess(PageList<? extends DriftChangeSet> result) {
                        response.setData(buildRecords(result));
                        response.setTotalRows(result.getTotalSize());
                        processResponse(request.getRequestId(), response);
                    }

                    public void onFailure(Throwable caught) {
                        CoreGUI.getErrorHandler().handleError(MSG.view_drift_snapshots_tree_loadFailure(), caught);
                        response.setStatus(DSResponse.STATUS_FAILURE);
                        processResponse(request.getRequestId(), response);
                    }
                });
        } else {
            String changesetId = parentId.substring(parentId.indexOf('_') + 1);
            GenericDriftCriteria criteria = new GenericDriftCriteria();
            criteria.addFilterChangeSetId(changesetId);
            // TODO, this should probably not need to be eager loaded, the drift tree build should not need this 
            criteria.fetchChangeSet(true);

            driftService.findDriftsByCriteria(criteria, new AsyncCallback<PageList<? extends Drift>>() {
                public void onFailure(Throwable caught) {
                    CoreGUI.getErrorHandler().handleError(MSG.view_drift_snapshots_tree_loadFailure(), caught);
                    response.setStatus(DSResponse.STATUS_FAILURE);
                    processResponse(request.getRequestId(), response);
                }

                public void onSuccess(PageList<? extends Drift> result) {
                    response.setData(buildRecords(result));
                    response.setTotalRows(result.getTotalSize());
                    processResponse(request.getRequestId(), response);
                }
            });
        }

        return;
    }

    /**
     * Fetches the top level node data - the drift configuration nodes.
     * @param request
     * @param response
     */
    protected abstract void fetchDriftConfigurations(DSRequest request, DSResponse response);

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
    protected GenericDriftChangeSetCriteria getDriftChangeSetCriteria(final DSRequest request) {
        GenericDriftChangeSetCriteria criteria = new GenericDriftChangeSetCriteria();
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
        if (dataObjects == null) {
            return null;
        }

        final List<ListGridRecord> records = new ArrayList<ListGridRecord>();

        for (Object item : dataObjects) {
            records.add(copyValues(item));
        }

        return records.toArray(new ListGridRecord[records.size()]);
    }

    @Override
    public ListGridRecord copyValues(Object from) {
        TreeNode node;
        if (from instanceof DriftConfiguration) {
            DriftConfiguration driftConfig = (DriftConfiguration) from;
            node = new AbstractDriftChangeSetsTreeView.DriftConfigurationTreeNode(driftConfig);
        } else if (from instanceof DriftChangeSet) {
            DriftChangeSet changeset = (DriftChangeSet) from;
            node = new AbstractDriftChangeSetsTreeView.ChangeSetTreeNode(changeset);
        } else if (from instanceof Drift) {
            Drift drift = (Drift) from;
            node = new AbstractDriftChangeSetsTreeView.DriftTreeNode(drift);
        } else {
            throw new IllegalArgumentException("please report this bug - bad value: " + from);
        }

        return node;
    }
}
