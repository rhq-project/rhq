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
 * along with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA
 */

package org.rhq.coregui.client.inventory.resource.detail;

import static org.rhq.coregui.client.components.form.DateFilterItem.adjustTimeToEndOfDay;
import static org.rhq.coregui.client.components.form.DateFilterItem.adjustTimeToStartOfDay;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.datepicker.client.CalendarUtil;
import com.smartgwt.client.data.DSRequest;
import com.smartgwt.client.data.DSResponse;
import com.smartgwt.client.data.Record;
import com.smartgwt.client.types.ListGridFieldType;
import com.smartgwt.client.types.SortDirection;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.Window;
import com.smartgwt.client.widgets.events.CloseClickEvent;
import com.smartgwt.client.widgets.events.CloseClickHandler;
import com.smartgwt.client.widgets.events.DoubleClickEvent;
import com.smartgwt.client.widgets.events.DoubleClickHandler;
import com.smartgwt.client.widgets.grid.CellFormatter;
import com.smartgwt.client.widgets.grid.HoverCustomizer;
import com.smartgwt.client.widgets.grid.ListGrid;
import com.smartgwt.client.widgets.grid.ListGridField;
import com.smartgwt.client.widgets.grid.ListGridRecord;

import org.rhq.core.domain.criteria.Criteria;
import org.rhq.core.domain.resource.CreateResourceHistory;
import org.rhq.core.domain.resource.CreateResourceStatus;
import org.rhq.core.domain.resource.DeleteResourceHistory;
import org.rhq.core.domain.resource.DeleteResourceStatus;
import org.rhq.core.domain.resource.composite.ResourceComposite;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.coregui.client.CoreGUI;
import org.rhq.coregui.client.ImageManager;
import org.rhq.coregui.client.components.form.DateFilterItem;
import org.rhq.coregui.client.components.table.Table;
import org.rhq.coregui.client.components.table.TimestampCellFormatter;
import org.rhq.coregui.client.components.view.ViewName;
import org.rhq.coregui.client.gwt.GWTServiceLookup;
import org.rhq.coregui.client.util.RPCDataSource;
import org.rhq.coregui.client.util.async.Command;
import org.rhq.coregui.client.util.async.CountDownLatch;

/**
 * @author John Mazzitelli
 */
public class ChildHistoryView extends Table<ChildHistoryView.DataSource> {

    public static final ViewName VIEW_ID = new ViewName("ChildHistory", MSG.view_tabs_common_child_history());
    public static final String CHILD_CREATED_ICON = ImageManager.getChildCreateIcon();
    public static final String CHILD_DELETED_ICON = ImageManager.getChildDeleteIcon();

    private final ResourceComposite resourceComposite;
    private DateFilterItem startDateFilter;
    private DateFilterItem endDateFilter;

    public ChildHistoryView(ResourceComposite resourceComposite) {
        super(VIEW_ID.getTitle());
        this.resourceComposite = resourceComposite;
        setDataSource(new DataSource());
    }

    @Override
    protected void configureTableFilters() {
        endDateFilter = new DateFilterItem(DateFilterItem.END_DATE_FILTER, MSG.filter_to_date());
        Date date = new Date();
        endDateFilter.setValue(date);
        startDateFilter = new DateFilterItem(DateFilterItem.START_DATE_FILTER, MSG.filter_from_date());
        CalendarUtil.addMonthsToDate(date, -1);
        startDateFilter.setValue(date);
        if (isShowFilterForm()) {
            setFilterFormItems(startDateFilter, endDateFilter);
        }
    }

    @Override
    protected void configureTable() {
        ListGridField idField = new ListGridField(DataSource.Field.ID, MSG.common_title_id());
        idField.setWidth(75);

        ListGridField typeField = new ListGridField(DataSource.Field.TYPE, MSG.common_title_type());
        typeField.setWidth(75);
        typeField.setType(ListGridFieldType.ICON);
        HashMap<String, String> typeIcons = new HashMap<String, String>(2);
        typeIcons.put(DataSource.TYPE_CREATE, CHILD_CREATED_ICON);
        typeIcons.put(DataSource.TYPE_DELETE, CHILD_DELETED_ICON);
        typeField.setValueIcons(typeIcons);
        typeField.setShowHover(true);
        typeField.setHoverCustomizer(new HoverCustomizer() {
            public String hoverHTML(Object value, ListGridRecord record, int rowNum, int colNum) {
                String type = record.getAttribute(DataSource.Field.TYPE);
                if (DataSource.TYPE_CREATE.equals(type)) {
                    return MSG.view_resource_inventory_childhistory_createdChild();
                } else if (DataSource.TYPE_DELETE.equals(type)) {
                    return MSG.view_resource_inventory_childhistory_deletedChild();
                } else {
                    return "?"; // should never happen
                }
            }
        });

        ListGridField createdField = new ListGridField(DataSource.Field.CREATED_DATE, MSG.common_title_dateCreated());
        TimestampCellFormatter.prepareDateField(createdField);

        ListGridField modifiedField = new ListGridField(DataSource.Field.LAST_MODIFIED_TIME,
            MSG.common_title_lastUpdated());
        TimestampCellFormatter.prepareDateField(modifiedField);

        ListGridField subjectField = new ListGridField(DataSource.Field.SUBJECT_NAME, MSG.common_title_user());

        ListGridField statusField = new ListGridField(DataSource.Field.STATUS, MSG.common_title_status());
        statusField.setCellFormatter(new CellFormatter() {
            public String format(Object value, ListGridRecord record, int rowNum, int colNum) {
                String type = record.getAttribute(DataSource.Field.TYPE);
                if (DataSource.TYPE_CREATE.equals(type)) {
                    switch (CreateResourceStatus.valueOf(value.toString())) {
                    case SUCCESS:
                        return MSG.common_status_success();
                    case FAILURE:
                        return MSG.common_status_failed();
                    case IN_PROGRESS:
                        return MSG.common_status_inprogress();
                    case INVALID_ARTIFACT:
                        return MSG.view_resource_inventory_childhistory_status_invalidArtifact();
                    case INVALID_CONFIGURATION:
                        return MSG.view_resource_inventory_childhistory_status_invalidConfig();
                    case TIMED_OUT:
                        return MSG.widget_resourceFactoryWizard_timeoutFailure();
                    }
                } else if (DataSource.TYPE_DELETE.equals(type)) {
                    switch (DeleteResourceStatus.valueOf(value.toString())) {
                    case SUCCESS:
                        return MSG.common_status_success();
                    case FAILURE:
                        return MSG.common_status_failed();
                    case IN_PROGRESS:
                        return MSG.common_status_inprogress();
                    case TIMED_OUT:
                        return MSG.common_status_timedOut();
                    }
                }
                return "?"; // should never happen
            }
        });

        setListGridFields(idField, typeField, createdField, modifiedField, subjectField, statusField);

        // Add a double click handler to show the details for the selected row.
        // Notice this goes against our normal UI design. I would normally have used TableSection
        // and have the details canvas created via the getDetails API.
        // This would make our details view bookmarkable. However, the old design of the create/delete
        // history makes determining what entity to use via an ID is not possible (the create/delete
        // history is split into two tables). So, I'll just pop up the audit details in a dialog window.
        setListGridDoubleClickHandler(new DoubleClickHandler() {
            public void onDoubleClick(DoubleClickEvent event) {
                ListGrid listGrid = (ListGrid) event.getSource();
                ListGridRecord[] selectedRows = listGrid.getSelectedRecords();
                if (selectedRows != null && selectedRows.length == 1) {
                    String typeString = selectedRows[0].getAttribute(DataSource.Field.TYPE);
                    ChildHistoryDetails detailsView = null;
                    if (DataSource.TYPE_CREATE.equals(typeString)) {
                        CreateResourceHistory history = (CreateResourceHistory) selectedRows[0]
                            .getAttributeAsObject(DataSource.Field.OBJECT);
                        detailsView = new ChildHistoryDetails(history);
                    } else if (DataSource.TYPE_DELETE.equals(typeString)) {
                        DeleteResourceHistory history = (DeleteResourceHistory) selectedRows[0]
                            .getAttributeAsObject(DataSource.Field.OBJECT);
                        detailsView = new ChildHistoryDetails(history);
                    }
                    new DetailsWindow(detailsView).show();
                }
            }
        });

        ListGrid listGrid = getListGrid();
        listGrid.setSortField(DataSource.Field.CREATED_DATE);
        listGrid.setSortDirection(SortDirection.DESCENDING);
    }

    class DataSource extends RPCDataSource<Object, Criteria> {
        public static final String TYPE_CREATE = "create";
        public static final String TYPE_DELETE = "delete";

        public class Field {
            public static final String ID = "id";
            public static final String CREATED_DATE = "createdDate";
            public static final String LAST_MODIFIED_TIME = "lastModifiedDate";
            public static final String SUBJECT_NAME = "subjectName";
            public static final String STATUS = "status"; // name of either [Create,Delete]ResourceStatus
            public static final String TYPE = "historyType"; // will be either TYPE_CREATE or TYPE_DELETE
            public static final String OBJECT = "object";
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
                record.setAttribute(Field.TYPE, TYPE_CREATE);
                record.setAttribute(Field.ID, history.getId());
                record.setAttribute(Field.SUBJECT_NAME, history.getSubjectName());
                record.setAttribute(Field.CREATED_DATE, history.getCreatedDate());
                record.setAttribute(Field.LAST_MODIFIED_TIME, history.getLastModifiedDate());
                record.setAttribute(Field.STATUS, history.getStatus().name());
            } else if (from instanceof DeleteResourceHistory) {
                DeleteResourceHistory history = (DeleteResourceHistory) from;
                record.setAttribute(Field.TYPE, TYPE_DELETE);
                record.setAttribute(Field.ID, history.getId());
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
            Date startDate, endDate;
            try {
                startDate = startDateFilter.getValueAsDate();
                endDate = endDateFilter.getValueAsDate();
            } catch (Exception e) {
                // Value parsed is not a date
                processResults(request, response, Collections.emptyList());
                return;
            }

            if (startDate == null || endDate == null) {
                processResults(request, response, Collections.emptyList());
                return;
            }

            long start = adjustTimeToStartOfDay(startDate).getTime();
            long end = adjustTimeToEndOfDay(endDate).getTime();

            final List<Object> fullList = new ArrayList<Object>();

            final CountDownLatch latch = CountDownLatch.create(2, new Command() {
                @Override
                public void execute() {
                    processResults(request, response, fullList);
                }
            });

            GWTServiceLookup.getResourceService().findCreateChildResourceHistory(
                resourceComposite.getResource().getId(), start, end, PageControl.getUnlimitedInstance(),
                new AsyncCallback<PageList<CreateResourceHistory>>() {
                    @Override
                    public void onSuccess(final PageList<CreateResourceHistory> createList) {
                        fullList.addAll(createList);
                        latch.countDown();
                    }

                    @Override
                    public void onFailure(Throwable caught) {
                        CoreGUI.getErrorHandler().handleError("Failed to load child create history", caught);
                        latch.countDown();
                    }
                });

            GWTServiceLookup.getResourceService().findDeleteChildResourceHistory(
                resourceComposite.getResource().getId(), start, end, PageControl.getUnlimitedInstance(),
                new AsyncCallback<PageList<DeleteResourceHistory>>() {
                    @Override
                    public void onSuccess(final PageList<DeleteResourceHistory> deleteList) {
                        fullList.addAll(deleteList);
                        latch.countDown();
                    }

                    @Override
                    public void onFailure(Throwable caught) {
                        CoreGUI.getErrorHandler().handleError("Failed to load child delete history", caught);
                        latch.countDown();
                    }
                });
        }

        private void processResults(DSRequest request, DSResponse response, List<Object> fullList) {
            ListGridRecord[] records = buildRecords(fullList);
            response.setData(records);
            processResponse(request.getRequestId(), response);
        }

        @Override
        protected Criteria getFetchCriteria(DSRequest request) {
            return null; // using special server side API to get the create/delete histories, no criteria involved
        }
    }

    class DetailsWindow extends Window {
        public DetailsWindow(Canvas canvas) {
            super();
            setTitle(MSG.common_title_details());
            setShowMinimizeButton(false);
            setShowMaximizeButton(true);
            setIsModal(true);
            setShowModalMask(true);
            setWidth(600);
            setHeight(400);
            setAutoCenter(true);
            setShowResizer(true);
            setCanDragResize(true);
            centerInPage();
            addCloseClickHandler(new CloseClickHandler() {
                public void onCloseClick(CloseClickEvent event) {
                    markForDestroy();
                }
            });
            if (canvas != null) {
                addItem(canvas);
            }
        }
    }

}
