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

package org.rhq.enterprise.gui.coregui.client.inventory.resource.detail;

import java.util.ArrayList;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.data.DSRequest;
import com.smartgwt.client.data.DSResponse;
import com.smartgwt.client.data.Record;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.grid.ListGridField;
import com.smartgwt.client.widgets.grid.ListGridRecord;

import org.rhq.core.domain.criteria.Criteria;
import org.rhq.core.domain.resource.CreateResourceHistory;
import org.rhq.core.domain.resource.DeleteResourceHistory;
import org.rhq.core.domain.resource.composite.ResourceComposite;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.components.table.TableSection;
import org.rhq.enterprise.gui.coregui.client.components.view.ViewName;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.util.RPCDataSource;

/**
 * @author John Mazzitelli
 */
public class ChildHistoryView extends TableSection<ChildHistoryView.DataSource> {

    public static final ViewName VIEW_ID = new ViewName("ChildHistory", MSG.view_tabs_common_child_history());
    public static final String CHILD_CREATED_ICON = "[skin]/MultiUploadItem/icon_add_files.png";
    public static final String CHILD_DELETED_ICON = "[skin]/MultiUploadItem/icon_remove_files.png";

    private final ResourceComposite resourceComposite;

    public ChildHistoryView(String locatorId, ResourceComposite resourceComposite) {
        super(locatorId, VIEW_ID.getTitle());
        this.resourceComposite = resourceComposite;
        setDataSource(new DataSource());
    }

    @Override
    protected void configureTable() {
        ListGridField idField = new ListGridField(DataSource.Field.ID, MSG.common_title_id());

        ListGridField errorMessageField = new ListGridField(DataSource.Field.ERROR_MESSAGE, MSG.common_title_error());

        ListGridField subjectField = new ListGridField(DataSource.Field.SUBJECT_NAME, MSG.common_title_user());

        ListGridField createdField = new ListGridField(DataSource.Field.CREATED_DATE, MSG.common_title_dateCreated());

        ListGridField modifiedField = new ListGridField(DataSource.Field.LAST_MODIFIED_TIME, MSG
            .common_title_lastUpdated());

        ListGridField statusField = new ListGridField(DataSource.Field.STATUS, MSG.common_title_status());

        ListGridField detailsField = new ListGridField("details", MSG.common_title_details());
    }

    @Override
    public Canvas getDetailsView(int id) {
        // TODO Auto-generated method stub
        return null;
    }

    class DataSource extends RPCDataSource<Object, Criteria> {
        public class Field {
            public static final String ID = "id";
            public static final String ERROR_MESSAGE = "errorMessage";
            public static final String SUBJECT_NAME = "subjectName";
            public static final String CREATED_DATE = "createdDate";
            public static final String LAST_MODIFIED_TIME = "lastModifiedDate";
            public static final String STATUS = "status"; // name of either [Create,Delete]ResourceStatus
            public static final String OBJECT = "object";

            // create history only
            public static final String CREATED_RESOURCE_NAME = "createdResourceName";
            public static final String NEW_RESOURCE_KEY = "newResourceKey";
        }

        /**
         * Given a record, returns either the CreateResourceHistory or the DeleteResourceHistory entity object
         * that the record represents.
         * 
         * @param from a record that represents either a create or delete resource history item
         * @return the create/delete resource history item
         */
        @Override
        public Object copyValues(Record from) {
            return from.getAttributeAsObject(Field.OBJECT);
        }

        /**
         * Given either a CreateResourceHistory or a DeleteResourceHistory entity object,
         * returns its record representation.
         * 
         * @param from a create or delete resource history item
         * @return the record that represents the given history item
         */
        @Override
        public ListGridRecord copyValues(Object from) {
            ListGridRecord record = new ListGridRecord();
            record.setAttribute(Field.OBJECT, from);

            if (from instanceof CreateResourceHistory) {
                CreateResourceHistory history = (CreateResourceHistory) from;
                record.setAttribute(Field.ID, history.getId());
                record.setAttribute(Field.ERROR_MESSAGE, history.getErrorMessage());
                record.setAttribute(Field.SUBJECT_NAME, history.getSubjectName());
                record.setAttribute(Field.CREATED_DATE, history.getCreatedDate());
                record.setAttribute(Field.LAST_MODIFIED_TIME, history.getLastModifiedDate());
                record.setAttribute(Field.STATUS, history.getStatus().name());
                record.setAttribute(Field.CREATED_RESOURCE_NAME, history.getCreatedResourceName());
                record.setAttribute(Field.NEW_RESOURCE_KEY, history.getNewResourceKey());
            } else if (from instanceof DeleteResourceHistory) {
                DeleteResourceHistory history = (DeleteResourceHistory) from;
                record.setAttribute(Field.ID, history.getId());
                record.setAttribute(Field.ERROR_MESSAGE, history.getErrorMessage());
                record.setAttribute(Field.SUBJECT_NAME, history.getSubjectName());
                record.setAttribute(Field.CREATED_DATE, history.getCreatedDate());
                record.setAttribute(Field.LAST_MODIFIED_TIME, history.getLastModifiedDate());
                record.setAttribute(Field.STATUS, history.getStatus().name());
            } else {
                CoreGUI.getErrorHandler().handleError("invalid child history type: " + from.getClass()); // should never occur
            }

            return record;
        }

        @Override
        protected void executeFetch(final DSRequest request, final DSResponse response, Criteria criteria) {
            final Long beginDate = System.currentTimeMillis() - 1000 * 60 * 60 * 24 * 30;
            final Long endDate = System.currentTimeMillis();
            final PageControl pc1 = PageControl.getUnlimitedInstance();
            final PageControl pc2 = PageControl.getUnlimitedInstance();

            GWTServiceLookup.getResourceService().findCreateChildResourceHistory(
                resourceComposite.getResource().getId(), beginDate, endDate, pc1,
                new AsyncCallback<PageList<CreateResourceHistory>>() {
                    @Override
                    public void onSuccess(final PageList<CreateResourceHistory> createList) {
                        GWTServiceLookup.getResourceService().findDeleteChildResourceHistory(
                            resourceComposite.getResource().getId(), beginDate, endDate, pc2,
                            new AsyncCallback<PageList<DeleteResourceHistory>>() {
                                @Override
                                public void onSuccess(final PageList<DeleteResourceHistory> deleteList) {
                                    ArrayList<Object> fullList = new ArrayList<Object>();
                                    fullList.addAll(createList);
                                    fullList.addAll(deleteList);
                                    ListGridRecord[] records = buildRecords(fullList);
                                    response.setData(records);
                                    processResponse(request.getRequestId(), response);
                                }

                                @Override
                                public void onFailure(Throwable caught) {
                                    CoreGUI.getErrorHandler()
                                        .handleError("Failed to load child delete history", caught);
                                }
                            });
                    }

                    @Override
                    public void onFailure(Throwable caught) {
                        CoreGUI.getErrorHandler().handleError("Failed to load child create history", caught);
                    }
                });
        }

        @Override
        protected Criteria getFetchCriteria(DSRequest request) {
            return null; // using special server side API to get the create/delete histories, no criteria involved
        }
    }
}
