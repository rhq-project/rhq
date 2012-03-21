/*
 * RHQ Management Platform
 * Copyright 2012, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
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

package org.rhq.enterprise.gui.coregui.client.inventory.resource.detail.monitoring.avail;

import java.util.ArrayList;
import java.util.Date;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.data.DSRequest;
import com.smartgwt.client.data.DSResponse;
import com.smartgwt.client.data.Record;
import com.smartgwt.client.data.SortSpecifier;
import com.smartgwt.client.rpc.RPCResponse;
import com.smartgwt.client.types.Alignment;
import com.smartgwt.client.types.ListGridFieldType;
import com.smartgwt.client.types.SortDirection;
import com.smartgwt.client.widgets.form.fields.StaticTextItem;
import com.smartgwt.client.widgets.grid.ListGridField;
import com.smartgwt.client.widgets.grid.ListGridRecord;
import com.smartgwt.client.widgets.layout.Layout;

import org.rhq.core.domain.criteria.AvailabilityCriteria;
import org.rhq.core.domain.measurement.Availability;
import org.rhq.core.domain.measurement.MeasurementUnits;
import org.rhq.core.domain.resource.composite.ResourceAvailabilitySummary;
import org.rhq.core.domain.resource.composite.ResourceComposite;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.core.domain.util.PageOrdering;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.ImageManager;
import org.rhq.enterprise.gui.coregui.client.components.table.Table;
import org.rhq.enterprise.gui.coregui.client.components.table.TimestampCellFormatter;
import org.rhq.enterprise.gui.coregui.client.gwt.AvailabilityGWTServiceAsync;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.util.MeasurementConverterClient;
import org.rhq.enterprise.gui.coregui.client.util.RPCDataSource;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableDynamicForm;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableVLayout;

/**
 * @author Jay Shaughnessy
 */
public class ResourceAvailabilityView extends LocatableVLayout {

    private ResourceComposite resourceComposite;

    public ResourceAvailabilityView(String locatorId, ResourceComposite resourceComposite) {
        super(locatorId);

        this.resourceComposite = resourceComposite;

        setWidth100();
        setHeight100();
    }

    @Override
    protected void onInit() {
        super.onInit();

        addMember(createSummaryForm());
        addMember(createListView());
    }

    private LocatableDynamicForm createSummaryForm() {
        LocatableDynamicForm form = new LocatableDynamicForm(extendLocatorId("Summary"));
        form.setWidth100();
        form.setAutoHeight();
        form.setMargin(10);
        form.setNumCols(4);

        final StaticTextItem current = new StaticTextItem("current",
            MSG.view_resource_monitor_availability_currentStatus());
        current.setWrapTitle(false);
        current.setColSpan(4);

        final StaticTextItem avail = new StaticTextItem("avail", MSG.view_resource_monitor_availability_availability());
        avail.setWrapTitle(false);
        final StaticTextItem availTime = new StaticTextItem("availTime",
            MSG.view_resource_monitor_availability_uptime());
        availTime.setWrapTitle(false);

        final StaticTextItem down = new StaticTextItem("down", MSG.view_resource_monitor_availability_down());
        down.setWrapTitle(false);
        final StaticTextItem downTime = new StaticTextItem("downTime",
            MSG.view_resource_monitor_availability_downtime());
        downTime.setWrapTitle(false);

        final StaticTextItem disabled = new StaticTextItem("disabled",
            MSG.view_resource_monitor_availability_disabled());
        disabled.setWrapTitle(false);
        final StaticTextItem disabledTime = new StaticTextItem("disabledTime",
            MSG.view_resource_monitor_availability_disabledTime());
        disabledTime.setWrapTitle(false);

        final StaticTextItem failureCount = new StaticTextItem("failureCount",
            MSG.view_resource_monitor_availability_numFailures());
        failureCount.setWrapTitle(false);
        final StaticTextItem disabledCount = new StaticTextItem("disabledCount",
            MSG.view_resource_monitor_availability_numDisabled());
        disabledCount.setWrapTitle(false);

        final StaticTextItem mtbf = new StaticTextItem("mtbf", MSG.view_resource_monitor_availability_mtbf());
        mtbf.setWrapTitle(false);
        mtbf.setPrompt(MSG.view_resource_monitor_availability_mtbfTooltip());
        final StaticTextItem mttr = new StaticTextItem("mttr", MSG.view_resource_monitor_availability_mttr());
        mttr.setWrapTitle(false);
        mttr.setPrompt(MSG.view_resource_monitor_availability_mttrTooltip());

        final StaticTextItem unknown = new StaticTextItem("unknown");
        unknown.setWrapTitle(false);
        unknown.setColSpan(4);
        unknown.setShowTitle(false);

        final StaticTextItem currentTime = new StaticTextItem("currentTime");
        currentTime.setWrapTitle(false);
        currentTime.setColSpan(4);
        currentTime.setShowTitle(false);

        form.setItems(current, avail, availTime, down, downTime, disabled, disabledTime, failureCount, disabledCount,
            mtbf, mttr, unknown, currentTime);

        GWTServiceLookup.getResourceService().getResourceAvailabilitySummary(resourceComposite.getResource().getId(),
            new AsyncCallback<ResourceAvailabilitySummary>() {

                @Override
                public void onSuccess(ResourceAvailabilitySummary result) {

                    current.setValue(MSG.view_resource_monitor_availability_currentStatus_value(result.getCurrent()
                        .getName(), TimestampCellFormatter.format(result.getLastChange().getTime())));
                    avail.setValue(MeasurementConverterClient.format(result.getUpPercentage(),
                        MeasurementUnits.PERCENTAGE, true));
                    availTime.setValue(MeasurementConverterClient.format((double) result.getUpTime(),
                        MeasurementUnits.MILLISECONDS, true));
                    down.setValue(MeasurementConverterClient.format(result.getDownPercentage(),
                        MeasurementUnits.PERCENTAGE, true));
                    downTime.setValue(MeasurementConverterClient.format((double) result.getDownTime(),
                        MeasurementUnits.MILLISECONDS, true));
                    disabled.setValue(MeasurementConverterClient.format(result.getDisabledPercentage(),
                        MeasurementUnits.PERCENTAGE, true));
                    disabledTime.setValue(MeasurementConverterClient.format((double) result.getDisabledTime(),
                        MeasurementUnits.MILLISECONDS, true));
                    failureCount.setValue(result.getFailures());
                    disabledCount.setValue(result.getDisabled());
                    mtbf.setValue(MeasurementConverterClient.format((double) result.getMTBF(),
                        MeasurementUnits.MILLISECONDS, true));
                    mttr.setValue(MeasurementConverterClient.format((double) result.getMTTR(),
                        MeasurementUnits.MILLISECONDS, true));

                    // TODO: do we want to show the time spent in UNKNOWN state? right now, this field is shown empty for the whitespace
                    //unknown.setValue("This resource was in an unknown state for 1000 hours");
                    currentTime.setValue(MSG.view_resource_monitor_availability_currentAsOf(TimestampCellFormatter
                        .format(result.getCurrentTime())));
                }

                @Override
                public void onFailure(Throwable caught) {
                    current.setValue(MSG.common_label_error());
                    CoreGUI.getErrorHandler()
                        .handleError(MSG.view_resource_monitor_availability_summaryError(), caught);
                }
            });

        return form;
    }

    private Table<ListView.DS> createListView() {
        ListView listView = new ListView(extendLocatorId("AvailList"), resourceComposite.getResource().getId());
        return listView;
    }

    private static class ListView extends Table<ListView.DS> {

        private DS dataSource;
        private int resourceId;

        public ListView(String locatorId, int resourceId) {
            super(locatorId, null, new SortSpecifier[] { new SortSpecifier("startTime", SortDirection.DESCENDING) });

            this.resourceId = resourceId;

            setDataSource(getDataSource());
        }

        @Override
        public DS getDataSource() {
            if (null == this.dataSource) {
                this.dataSource = new DS(resourceId);
            }
            return this.dataSource;
        }

        @Override
        protected void configureTableContents(Layout contents) {
            super.configureTableContents(contents);
            setAutoHeight();
        }

        @Override
        protected void configureTable() {
            ArrayList<ListGridField> dataSourceFields = getDataSource().getListGridFields();
            getListGrid().setFields(dataSourceFields.toArray(new ListGridField[dataSourceFields.size()]));

            super.configureTable();
        }

        private static class DS extends RPCDataSource<Availability, AvailabilityCriteria> {

            public static final String ATTR_ID = "id";
            public static final String ATTR_AVAILABILITY = "availabilityType";
            public static final String ATTR_START_TIME = "startTime";
            public static final String ATTR_END_TIME = "endTime";

            public static final String ATTR_DURATION = "duration";

            private AvailabilityGWTServiceAsync availService = GWTServiceLookup.getAvailabilityService();
            private int resourceId;

            public DS(int resourceId) {
                super();
                this.resourceId = resourceId;
                addDataSourceFields();
            }

            /**
             * The view that contains the list grid which will display this datasource's data will call this
             * method to get the field information which is used to control the display of the data.
             *
             * @return list grid fields used to display the datasource data
             */
            public ArrayList<ListGridField> getListGridFields() {
                ArrayList<ListGridField> fields = new ArrayList<ListGridField>(6);

                ListGridField startTimeField = new ListGridField(ATTR_START_TIME, MSG.common_title_start());
                startTimeField.setCellFormatter(new TimestampCellFormatter());
                startTimeField.setShowHover(true);
                startTimeField.setHoverCustomizer(TimestampCellFormatter.getHoverCustomizer(ATTR_START_TIME));
                startTimeField.setCanSortClientOnly(true);
                fields.add(startTimeField);

                ListGridField endTimeField = new ListGridField(ATTR_END_TIME, MSG.common_title_end());
                endTimeField.setCellFormatter(new TimestampCellFormatter());
                endTimeField.setShowHover(true);
                endTimeField.setHoverCustomizer(TimestampCellFormatter.getHoverCustomizer(ATTR_END_TIME));
                endTimeField.setCanSortClientOnly(true);
                fields.add(endTimeField);

                ListGridField durationField = new ListGridField(ATTR_DURATION, MSG.common_title_duration());
                fields.add(durationField);

                ListGridField availabilityField = new ListGridField(ATTR_AVAILABILITY, MSG.common_title_availability());
                availabilityField.setType(ListGridFieldType.IMAGE);
                availabilityField.setAlign(Alignment.CENTER);
                fields.add(availabilityField);

                return fields;
            }

            @Override
            protected AvailabilityCriteria getFetchCriteria(DSRequest request) {
                AvailabilityCriteria c = new AvailabilityCriteria();
                c.addFilterResourceId(resourceId);
                c.addFilterInitialAvailability(false);

                // This code is unlikely to be necessary as the encompassing view should be using an initial
                // sort specifier. But just in case, make sure we set the initial sort.  Note that we have to
                // manipulate the PageControl directly as per the restrictions on getFetchCriteria() (see jdoc).
                PageControl pageControl = getPageControl(request);
                if (pageControl.getOrderingFields().isEmpty()) {
                    pageControl.initDefaultOrderingField("startTime", PageOrdering.DESC);
                }

                return c;
            }

            @Override
            protected void executeFetch(final DSRequest request, final DSResponse response,
                AvailabilityCriteria criteria) {

                this.availService.findAvailabilityByCriteria(criteria, new AsyncCallback<PageList<Availability>>() {
                    public void onFailure(Throwable caught) {
                        // TODO fix message
                        CoreGUI.getErrorHandler().handleError(MSG.common_label_error(), caught);
                        response.setStatus(RPCResponse.STATUS_FAILURE);
                        processResponse(request.getRequestId(), response);
                    }

                    public void onSuccess(final PageList<Availability> result) {
                        response.setData(buildRecords(result));
                        response.setTotalRows(result.size());
                        processResponse(request.getRequestId(), response);
                    }
                });
            }

            @Override
            public Availability copyValues(Record from) {
                return null;
            }

            @Override
            public ListGridRecord copyValues(Availability from) {
                ListGridRecord record = new ListGridRecord();

                record.setAttribute(ATTR_ID, from.getId());
                record.setAttribute(ATTR_AVAILABILITY,
                    ImageManager.getAvailabilityIconFromAvailType(from.getAvailabilityType()));
                record.setAttribute(ATTR_START_TIME, new Date(from.getStartTime()));
                if (null != from.getEndTime()) {
                    record.setAttribute(ATTR_END_TIME, new Date(from.getEndTime()));
                    long duration = from.getEndTime() - from.getStartTime();
                    record.setAttribute(ATTR_DURATION,
                        MeasurementConverterClient.format((double) duration, MeasurementUnits.MILLISECONDS, true));

                } else {
                    record.setAttribute(ATTR_END_TIME, MSG.common_label_none2());
                    long duration = System.currentTimeMillis() - from.getStartTime();
                    record.setAttribute(ATTR_DURATION,
                        MeasurementConverterClient.format((double) duration, MeasurementUnits.MILLISECONDS, true));

                }

                return record;
            }
        }
    }

}
