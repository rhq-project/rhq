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
package org.rhq.enterprise.gui.coregui.client.content.repository.tree;

import java.util.List;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.data.DSRequest;
import com.smartgwt.client.data.DSResponse;
import com.smartgwt.client.data.DataSourceField;
import com.smartgwt.client.data.Record;
import com.smartgwt.client.data.fields.DataSourceTextField;
import com.smartgwt.client.widgets.grid.ListGridRecord;
import com.smartgwt.client.widgets.tree.TreeNode;

import org.rhq.core.domain.content.Repo;
import org.rhq.core.domain.criteria.RepoCriteria;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.gwt.RepoGWTServiceAsync;
import org.rhq.enterprise.gui.coregui.client.util.RPCDataSource;

/**
 * @author Greg Hinkle
 */
public class ContentRepositoryTreeDataSource extends RPCDataSource<Repo> {

    private RepoGWTServiceAsync repoService = GWTServiceLookup.getRepoService();

    public ContentRepositoryTreeDataSource() {
        super();
        List<DataSourceField> fields = addDataSourceFields();
        addFields(fields);
    }

    @Override
    protected List<DataSourceField> addDataSourceFields() {
        List<DataSourceField> fields = super.addDataSourceFields();

        DataSourceField idDataField = new DataSourceTextField("id", "ID");
        idDataField.setPrimaryKey(true);
        fields.add(idDataField);

        DataSourceTextField nameDataField = new DataSourceTextField("name", "Name");
        nameDataField.setCanEdit(false);
        fields.add(nameDataField);

        DataSourceTextField parentIdField = new DataSourceTextField("parentId", "Parent ID");
        parentIdField.setForeignKey("id");
        fields.add(parentIdField);

        return fields;
    }

    @Override
    protected void executeFetch(final DSRequest request, final DSResponse response) {

        RepoCriteria criteria = new RepoCriteria();

        repoService.findReposByCriteria(criteria, new AsyncCallback<PageList<Repo>>() {
            public void onFailure(Throwable caught) {
                CoreGUI.getErrorHandler().handleError("Failed to load repositories", caught);
            }

            public void onSuccess(PageList<Repo> result) {
                response.setData(buildRecords(result));
                processResponse(request.getRequestId(), response);
            }
        });

    }

    @Override
    public Repo copyValues(Record from) {
        return null; // TODO: Implement this method.
    }

    @Override
    public ListGridRecord copyValues(Repo from) {
        TreeNode record = new TreeNode();

        record.setID(String.valueOf(from.getId()));
        record.setName(from.getName());

        record.setAttribute("id", from.getId());
        record.setAttribute("name", from.getName());
        record.setAttribute("description", from.getDescription());
        record.setAttribute("syncSchedule", from.getSyncSchedule());
        record.setAttribute("syncStatus", from.getSyncStatus());

        record.setIsFolder(false); // don't attempt to load any children

        return record;
    }
}
