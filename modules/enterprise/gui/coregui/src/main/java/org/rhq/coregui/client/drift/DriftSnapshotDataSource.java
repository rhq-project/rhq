/*
 * RHQ Management Platform
 * Copyright (C) 2005-2014 Red Hat, Inc.
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

package org.rhq.coregui.client.drift;

import java.util.ArrayList;
import java.util.Collection;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.data.DSRequest;
import com.smartgwt.client.data.DSResponse;
import com.smartgwt.client.data.Record;
import com.smartgwt.client.rpc.RPCResponse;
import com.smartgwt.client.widgets.grid.ListGridField;
import com.smartgwt.client.widgets.grid.ListGridRecord;

import org.rhq.core.domain.criteria.DriftDefinitionTemplateCriteria;
import org.rhq.core.domain.criteria.GenericDriftChangeSetCriteria;
import org.rhq.core.domain.drift.DriftDefinitionTemplate;
import org.rhq.core.domain.drift.DriftSnapshot;
import org.rhq.core.domain.drift.DriftSnapshot.DriftSnapshotDirectory;
import org.rhq.core.domain.drift.DriftSnapshotRequest;
import org.rhq.core.domain.util.PageList;
import org.rhq.coregui.client.CoreGUI;
import org.rhq.coregui.client.gwt.DriftGWTServiceAsync;
import org.rhq.coregui.client.gwt.GWTServiceLookup;
import org.rhq.coregui.client.util.RPCDataSource;

/**
 * @author jshaughnessy
 *
 */
public class DriftSnapshotDataSource extends RPCDataSource<DriftSnapshotDirectory, GenericDriftChangeSetCriteria> {

    public static final String ATTR_FILES = "files";
    public static final String ATTR_ADDED = "added";
    public static final String ATTR_CHANGED = "changed";
    public static final String ATTR_REMOVED = "removed";
    public static final String ATTR_DEF_ID = "defId";
    public static final String ATTR_DIR_PATH = "dirPath";

    private Integer templateId;
    private Integer driftDefId;
    private Integer version;

    private String templateChangeSetId;

    public DriftSnapshotDataSource(int templateId) {
        this(templateId, null, null);
    }

    public DriftSnapshotDataSource(int driftDefId, int version) {
        this(null, driftDefId, version);
    }

    private DriftSnapshotDataSource(Integer templateId, Integer driftDefId, Integer version) {
        this.templateId = templateId;
        this.driftDefId = driftDefId;
        this.version = version;

        this.setCacheAllData(true);
    }

    protected int getDriftDefId() {
        return driftDefId;
    }

    protected int getVersion() {
        return version;
    }

    @Override
    public DriftSnapshotDirectory copyValues(Record from) {
        return null;
    }

    @Override
    public ListGridRecord copyValues(DriftSnapshotDirectory from) {
        ListGridRecord record = new ListGridRecord();

        String dirPath = from.getDirectoryPath();
        record.setAttribute(ATTR_DIR_PATH, (null == dirPath || "".equals(dirPath.trim())) ? "./" : dirPath);
        record.setAttribute(ATTR_FILES, from.getFiles());
        record.setAttribute(ATTR_ADDED, from.getAdded());
        record.setAttribute(ATTR_CHANGED, from.getChanged());
        record.setAttribute(ATTR_REMOVED, from.getRemoved());

        return record;
    }

    @Override
    protected void executeFetch(final DSRequest request, final DSResponse response,
        GenericDriftChangeSetCriteria criteria) {

        if (null == this.templateId) {
            DriftSnapshotRequest snapshotRequest = new DriftSnapshotRequest(driftDefId, version, null, null, true,
                false);
            executeGetSnapshot(request, response, snapshotRequest);

        } else {
            if (null == this.templateChangeSetId) {

                DriftDefinitionTemplateCriteria templateCriteria = new DriftDefinitionTemplateCriteria();
                templateCriteria.addFilterId(this.templateId);

                GWTServiceLookup.getDriftService().findDriftDefinitionTemplatesByCriteria(templateCriteria,
                    new AsyncCallback<PageList<DriftDefinitionTemplate>>() {

                        public void onSuccess(final PageList<DriftDefinitionTemplate> result) {
                            templateChangeSetId = String.valueOf(result.get(0).getChangeSetId());
                            DriftSnapshotRequest snapshotRequest = new DriftSnapshotRequest(templateChangeSetId, null,
                                true, false);
                            executeGetSnapshot(request, response, snapshotRequest);
                        }

                        public void onFailure(Throwable caught) {
                            CoreGUI.getErrorHandler().handleError("Failed to load definition.", caught);
                        }
                    });
            } else {
                DriftSnapshotRequest snapshotRequest = new DriftSnapshotRequest(templateChangeSetId, null, true, false);
                executeGetSnapshot(request, response, snapshotRequest);
            }
        }
    }

    private void executeGetSnapshot(final DSRequest request, final DSResponse response,
        DriftSnapshotRequest snapshotRequest) {

        DriftGWTServiceAsync driftService = GWTServiceLookup.getDriftService();

        driftService.getSnapshot(snapshotRequest, new AsyncCallback<DriftSnapshot>() {
            public void onFailure(Throwable caught) {
                CoreGUI.getErrorHandler().handleError(MSG.view_drift_failure_load(), caught);
                response.setStatus(RPCResponse.STATUS_FAILURE);
                processResponse(request.getRequestId(), response);
            }

            public void onSuccess(DriftSnapshot result) {
                Collection<DriftSnapshotDirectory> dirs = result.getDriftDirectories();
                ListGridRecord[] records = buildRecords(dirs);
                for (Record record : records) {
                    record.setAttribute(ATTR_DEF_ID, result.getRequest().getDriftDefinitionId());
                }
                response.setData(records);
                response.setTotalRows(dirs.size());
                processResponse(request.getRequestId(), response);
            }
        });
    }

    @Override
    protected GenericDriftChangeSetCriteria getFetchCriteria(DSRequest request) {
        return null;
    }

    public ArrayList<ListGridField> getListGridFields() {
        ArrayList<ListGridField> fields = new ArrayList<ListGridField>(7);

        ListGridField dirPathField = new ListGridField(ATTR_DIR_PATH, "Directory");
        fields.add(dirPathField);

        ListGridField filesField = new ListGridField(ATTR_FILES, "Files");
        fields.add(filesField);

        ListGridField addedField = new ListGridField(ATTR_ADDED, "Added");
        fields.add(addedField);

        ListGridField changedField = new ListGridField(ATTR_CHANGED, "Changed");
        fields.add(changedField);

        ListGridField removedField = new ListGridField(ATTR_REMOVED, "Removed");
        fields.add(removedField);

        dirPathField.setWidth("*");
        filesField.setWidth("10%");
        addedField.setWidth("10%");
        changedField.setWidth("10%");
        removedField.setWidth("10%");

        return fields;
    }

}
