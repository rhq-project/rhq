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
import com.smartgwt.client.widgets.form.DynamicForm;
import com.smartgwt.client.widgets.form.fields.FormItem;
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
import org.rhq.enterprise.gui.coregui.client.util.selenium.EnhancedVLayout;

/**
 * This shows the availability history for a resource.
 *
 * @author Jay Shaughnessy
 * @author John Mazzitelli
 */
public class ResourceAvailabilityView extends EnhancedVLayout {

    private ResourceComposite resourceComposite;
    private StaticTextItem currentField;
    private StaticTextItem availField;
    private StaticTextItem availTimeField;
    private StaticTextItem downField;
    private StaticTextItem downTimeField;
    private StaticTextItem disabledField;
    private StaticTextItem disabledTimeField;
    private StaticTextItem failureCountField;
    private StaticTextItem disabledCountField;
    private StaticTextItem mtbfField;
    private StaticTextItem mttrField;
    private StaticTextItem unknownField;
    private StaticTextItem currentTimeField;

    public ResourceAvailabilityView(ResourceComposite resourceComposite) {
        super();

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

    private DynamicForm createSummaryForm() {
        DynamicForm form = new DynamicForm();
        form.setWidth100();
        form.setAutoHeight();
        form.setMargin(10);
        form.setNumCols(4);

        // row 1
        currentField = new StaticTextItem("current", MSG.view_resource_monitor_availability_currentStatus());
        currentField.setWrapTitle(false);
        currentField.setColSpan(4);

        // row 2
        availField = new StaticTextItem("avail", MSG.view_resource_monitor_availability_availability());
        availField.setWrapTitle(false);
        prepareTooltip(availField, MSG.view_resource_monitor_availability_availability_tooltip());

        availTimeField = new StaticTextItem("availTime", MSG.view_resource_monitor_availability_uptime());
        availTimeField.setWrapTitle(false);
        prepareTooltip(availTimeField, MSG.view_resource_monitor_availability_uptime_tooltip());

        // row 3
        downField = new StaticTextItem("down", MSG.view_resource_monitor_availability_down());
        downField.setWrapTitle(false);
        prepareTooltip(downField, MSG.view_resource_monitor_availability_down_tooltip());

        downTimeField = new StaticTextItem("downTime", MSG.view_resource_monitor_availability_downtime());
        downTimeField.setWrapTitle(false);
        prepareTooltip(downTimeField, MSG.view_resource_monitor_availability_downtime_tooltip());

        // row 4
        disabledField = new StaticTextItem("disabled", MSG.view_resource_monitor_availability_disabled());
        disabledField.setWrapTitle(false);
        prepareTooltip(disabledField, MSG.view_resource_monitor_availability_disabled_tooltip());

        disabledTimeField = new StaticTextItem("disabledTime", MSG.view_resource_monitor_availability_disabledTime());
        disabledTimeField.setWrapTitle(false);
        prepareTooltip(disabledTimeField, MSG.view_resource_monitor_availability_disabledTime_tooltip());

        // row 5
        failureCountField = new StaticTextItem("failureCount", MSG.view_resource_monitor_availability_numFailures());
        failureCountField.setWrapTitle(false);
        prepareTooltip(failureCountField, MSG.view_resource_monitor_availability_numFailures_tooltip());

        disabledCountField = new StaticTextItem("disabledCount", MSG.view_resource_monitor_availability_numDisabled());
        disabledCountField.setWrapTitle(false);
        prepareTooltip(disabledCountField, MSG.view_resource_monitor_availability_numDisabled_tooltip());

        // row 6
        mtbfField = new StaticTextItem("mtbf", MSG.view_resource_monitor_availability_mtbf());
        mtbfField.setWrapTitle(false);
        prepareTooltip(mtbfField, MSG.view_resource_monitor_availability_mtbf_tooltip());

        mttrField = new StaticTextItem("mttr", MSG.view_resource_monitor_availability_mttr());
        mttrField.setWrapTitle(false);
        prepareTooltip(mttrField, MSG.view_resource_monitor_availability_mttr_tooltip());

        // row 7
        unknownField = new StaticTextItem("unknown");
        unknownField.setWrapTitle(false);
        unknownField.setColSpan(4);
        unknownField.setShowTitle(false);

        // row 8
        currentTimeField = new StaticTextItem("currentTime");
        currentTimeField.setWrapTitle(false);
        currentTimeField.setColSpan(4);
        currentTimeField.setShowTitle(false);

        form.setItems(currentField, availField, availTimeField, downField, downTimeField, disabledField,
            disabledTimeField, failureCountField, disabledCountField, mtbfField, mttrField, unknownField,
            currentTimeField);

        reloadSummaryData();

        return form;
    }

    private void reloadSummaryData() {
        GWTServiceLookup.getResourceService().getResourceAvailabilitySummary(resourceComposite.getResource().getId(),
            new AsyncCallback<ResourceAvailabilitySummary>() {

                @Override
                public void onSuccess(ResourceAvailabilitySummary result) {

                    currentField.setValue(MSG.view_resource_monitor_availability_currentStatus_value(result
                        .getCurrent().getName(), TimestampCellFormatter.format(result.getLastChange().getTime())));
                    availField.setValue(MeasurementConverterClient.format(result.getUpPercentage(),
                        MeasurementUnits.PERCENTAGE, true));
                    availTimeField.setValue(MeasurementConverterClient.format((double) result.getUpTime(),
                        MeasurementUnits.MILLISECONDS, true));
                    downField.setValue(MeasurementConverterClient.format(result.getDownPercentage(),
                        MeasurementUnits.PERCENTAGE, true));
                    downTimeField.setValue(MeasurementConverterClient.format((double) result.getDownTime(),
                        MeasurementUnits.MILLISECONDS, true));
                    disabledField.setValue(MeasurementConverterClient.format(result.getDisabledPercentage(),
                        MeasurementUnits.PERCENTAGE, true));
                    disabledTimeField.setValue(MeasurementConverterClient.format((double) result.getDisabledTime(),
                        MeasurementUnits.MILLISECONDS, true));
                    failureCountField.setValue(result.getFailures());
                    disabledCountField.setValue(result.getDisabled());
                    mtbfField.setValue(MeasurementConverterClient.format((double) result.getMTBF(),
                        MeasurementUnits.MILLISECONDS, true));
                    mttrField.setValue(MeasurementConverterClient.format((double) result.getMTTR(),
                        MeasurementUnits.MILLISECONDS, true));

                    if (result.getUnknownTime() > 0L) {
                        unknownField.setValue(MSG.view_resource_monitor_availability_unknown(MeasurementConverterClient
                            .format((double) result.getUnknownTime(), MeasurementUnits.MILLISECONDS, true)));
                    } else {
                        unknownField.setValue("");
                    }

                    currentTimeField.setValue(MSG.view_resource_monitor_availability_currentAsOf(TimestampCellFormatter
                        .format(result.getCurrentTime())));
                }

                @Override
                public void onFailure(Throwable caught) {
                    currentField.setValue(MSG.common_label_error());
                    CoreGUI.getErrorHandler()
                        .handleError(MSG.view_resource_monitor_availability_summaryError(), caught);
                }
            });
    }

    private void prepareTooltip(FormItem item, String tooltip) {
        item.setHoverWidth(400);
        item.setPrompt(tooltip);
    }

    private Table<ListView.DS> createListView() {
        ListView listView = new ListView(resourceComposite.getResource().getId());
        return listView;
    }

    private class ListView extends Table<ListView.DS> {

        private DS dataSource;
        private int resourceId;

        public ListView(int resourceId) {
            super(null, new SortSpecifier[] { new SortSpecifier("startTime", SortDirection.DESCENDING) });

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
        public void refresh() {
            super.refresh();
            reloadSummaryData();
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

        private class DS extends RPCDataSource<Availability, AvailabilityCriteria> {

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
                durationField.setAlign(Alignment.RIGHT);
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
