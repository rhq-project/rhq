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
package org.rhq.coregui.client.bundle.group;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.data.DSRequest;
import com.smartgwt.client.data.DSResponse;
import com.smartgwt.client.data.DataSourceField;
import com.smartgwt.client.data.Record;
import com.smartgwt.client.data.fields.DataSourceIntegerField;
import com.smartgwt.client.data.fields.DataSourceTextField;
import com.smartgwt.client.widgets.grid.ListGridRecord;

import org.rhq.core.domain.bundle.Bundle;
import org.rhq.core.domain.bundle.BundleGroup;
import org.rhq.core.domain.criteria.BundleGroupCriteria;
import org.rhq.core.domain.util.PageList;
import org.rhq.coregui.client.CoreGUI;
import org.rhq.coregui.client.LinkManager;
import org.rhq.coregui.client.admin.users.UsersDataSource.Field;
import org.rhq.coregui.client.bundle.list.BundlesDataSource;
import org.rhq.coregui.client.gwt.GWTServiceLookup;
import org.rhq.coregui.client.util.RPCDataSource;

/**
 * @author Jay Shaughnessy
 */
public class BundleGroupsDataSource extends RPCDataSource<BundleGroup, BundleGroupCriteria> {

    public static final String FIELD_ID = "id";
    public static final String FIELD_NAME = "name";
    public static final String FIELD_NAMELINK = "nameLink";
    public static final String FIELD_DESCRIPTION = "description";
    public static final String FIELD_BUNDLES = "bundles";

    private static BundleGroupsDataSource INSTANCE;

    public static BundleGroupsDataSource getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new BundleGroupsDataSource();
        }
        return INSTANCE;
    }

    public BundleGroupsDataSource() {
        super();
        List<DataSourceField> fields = addDataSourceFields();
        addFields(fields);
    }

    @Override
    protected List<DataSourceField> addDataSourceFields() {
        List<DataSourceField> fields = super.addDataSourceFields();

        DataSourceIntegerField idField = new DataSourceIntegerField(FIELD_ID, MSG.common_title_id());
        idField.setPrimaryKey(true);
        fields.add(idField);

        DataSourceTextField nameField = new DataSourceTextField(FIELD_NAME, MSG.common_title_name());
        fields.add(nameField);

        DataSourceTextField descriptionField = new DataSourceTextField(FIELD_DESCRIPTION,
            MSG.common_title_description());
        fields.add(descriptionField);

        return fields;
    }

    @Override
    protected void executeFetch(final DSRequest request, final DSResponse response, final BundleGroupCriteria criteria) {
        GWTServiceLookup.getBundleService().findBundleGroupsByCriteria(criteria,
            new AsyncCallback<PageList<BundleGroup>>() {
            public void onFailure(Throwable caught) {
                CoreGUI.getErrorHandler().handleError(MSG.dataSource_bundle_loadFailed(), caught);
                response.setStatus(DSResponse.STATUS_FAILURE);
                processResponse(request.getRequestId(), response);
            }

            public void onSuccess(PageList<BundleGroup> result) {
                response.setData(buildRecords(result));
                setPagingInfo(response, result);
                processResponse(request.getRequestId(), response);
            }
        });
    }

    @Override
    protected BundleGroupCriteria getFetchCriteria(final DSRequest request) {
        BundleGroupCriteria criteria = new BundleGroupCriteria();

        // may support tags in future, but not in rev1
        //criteria.addFilterTagNamespace(getFilter(request, "tagNamespace", String.class));
        //criteria.addFilterTagSemantic(getFilter(request, "tagSemantic", String.class));
        //criteria.addFilterTagName(getFilter(request, "tagName", String.class));
        //criteria.addFilterTagSemantic(getFilter(request, "tagSemantic", String.class));

        criteria.addFilterId(getFilter(request, FIELD_ID, Integer.class));
        criteria.addFilterName(getFilter(request, "search", String.class));
        criteria.addFilterBundleIds(getArrayFilter(request, "bundleIds", Integer.class));
        criteria.fetchBundles(true);

        return criteria;
    }

    @Override
    protected void executeAdd(final Record recordToAdd, final DSRequest request, final DSResponse response) {
        final BundleGroup newBundleGroup = copyValues(recordToAdd);

        GWTServiceLookup.getBundleService().createBundleGroup(newBundleGroup, new AsyncCallback<BundleGroup>() {
            public void onFailure(Throwable caught) {
                // TODO: Throw more specific SLSB exceptions so we can set the right validation errors.
                String message = caught.getMessage();
                if (message != null && message.contains("javax.persistence.EntityExistsException")) {
                    Map<String, String> errorMessages = new HashMap<String, String>();
                    errorMessages.put(Field.NAME, MSG.view_bundle_fail_existingName(newBundleGroup.getName()));
                    sendValidationErrorResponse(request, response, errorMessages);
                } else {
                    throw new RuntimeException(caught);
                }
            }

            public void onSuccess(final BundleGroup createdBundleGroup) {
                Record createdBundleGroupRecord = copyValues(createdBundleGroup, false);
                sendSuccessResponse(request, response, createdBundleGroupRecord);
            }
        });
    }

    @Override
    protected void executeUpdate(final Record editedBundleGroupRecord, Record oldBundleGroupRecord,
        final DSRequest request, final DSResponse response) {
        final BundleGroup editedBundleGroup = copyValues(editedBundleGroupRecord);

        GWTServiceLookup.getBundleService().updateBundleGroup(editedBundleGroup, new AsyncCallback<BundleGroup>() {
            public void onFailure(Throwable caught) {
                String message = "Failed to update bundle group [" + editedBundleGroup.getName() + "].";
                sendFailureResponse(request, response, message, caught);
            }

            public void onSuccess(final BundleGroup updatedBundleGroup) {
                sendSuccessResponse(request, response, editedBundleGroupRecord);
            }
        });
    }

    @Override
    public BundleGroup copyValues(Record from) {
        BundleGroup to = new BundleGroup();

        to.setId(from.getAttributeAsInt(FIELD_ID));
        to.setName(from.getAttributeAsString(FIELD_NAME));
        to.setDescription(from.getAttributeAsString(FIELD_DESCRIPTION));

        Record[] bundleRecords = from.getAttributeAsRecordArray(FIELD_BUNDLES);
        Set<Bundle> bundles = new BundlesDataSource().buildDataObjects(bundleRecords);
        to.setBundles(bundles);

        return to;
    }

    @Override
    public ListGridRecord copyValues(BundleGroup from) {
        return copyValues(from, true);
    }

    @Override
    public ListGridRecord copyValues(BundleGroup from, boolean cascade) {
        ListGridRecord record = new ListGridRecord();

        record.setAttribute(FIELD_ID, from.getId());
        record.setAttribute(FIELD_NAME, from.getName());
        record.setAttribute(FIELD_NAMELINK, LinkManager.getBundleGroupLink(from.getId()));
        record.setAttribute(FIELD_DESCRIPTION, (from.getDescription() == null) ? "" : from.getDescription());

        if (cascade) {
            Set<Bundle> bundles = from.getBundles();
            ListGridRecord[] bundleRecords = new BundlesDataSource().buildRecords(bundles, false);
            record.setAttribute(FIELD_BUNDLES, bundleRecords);

        }

        return record;
    }
}
