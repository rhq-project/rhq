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

package org.rhq.enterprise.gui.coregui.client.inventory.resource.detail.monitoring.table;

import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.types.VerticalAlignment;
import com.smartgwt.client.widgets.form.DynamicForm;
import com.smartgwt.client.widgets.form.fields.CanvasItem;
import com.smartgwt.client.widgets.form.fields.FormItem;
import com.smartgwt.client.widgets.form.fields.StaticTextItem;

import org.rhq.core.domain.measurement.MeasurementUnits;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.composite.ResourceAvailabilitySummary;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.components.table.TimestampCellFormatter;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.inventory.common.graph.graphtype.AvailabilitySummaryPieGraphType;
import org.rhq.enterprise.gui.coregui.client.util.Log;
import org.rhq.enterprise.gui.coregui.client.util.MeasurementConverterClient;
import org.rhq.enterprise.gui.coregui.client.util.enhanced.EnhancedVLayout;

/**
 * This shows the availability history for a resource.
 *
 * @author Jay Shaughnessy
 * @author John Mazzitelli
 * @author Mike Thompson
 */
public class ResourceMetricAvailabilityView extends EnhancedVLayout {

    private Resource resource;
    private StaticTextItem currentField;
    private StaticTextItem availTimeField;
    private StaticTextItem downTimeField;
    private StaticTextItem disabledTimeField;
    private StaticTextItem failureCountField;
    private StaticTextItem disabledCountField;
    private StaticTextItem mtbfField;
    private StaticTextItem mttrField;
    private StaticTextItem unknownField;
    private StaticTextItem currentTimeField;

    private AvailabilitySummaryPieGraphType availabilitySummaryPieGraph;

    public ResourceMetricAvailabilityView(Resource resource) {
        super();

        this.resource = resource;
        availabilitySummaryPieGraph = new AvailabilitySummaryPieGraphType();

        setWidth100();
        setHeight(165);
    }

    @Override
    protected void onInit() {
        super.onInit();
        addMember(createSummaryForm());
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
        availTimeField = new StaticTextItem("availTime", MSG.view_resource_monitor_availability_uptime());
        availTimeField.setWrapTitle(false);
        prepareTooltip(availTimeField, MSG.view_resource_monitor_availability_uptime_tooltip());

        // row 3
        downTimeField = new StaticTextItem("downTime", MSG.view_resource_monitor_availability_downtime());
        downTimeField.setWrapTitle(false);
        prepareTooltip(downTimeField, MSG.view_resource_monitor_availability_downtime_tooltip());

        // row 4
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

        CanvasItem availPieChartItem = new CanvasItem();
        //@todo: i18n
        availPieChartItem.setTitle("Availability");
        availPieChartItem.setCanvas(availabilitySummaryPieGraph.createGraphMarker());
        availPieChartItem.setRowSpan(3);
        availPieChartItem.setVAlign(VerticalAlignment.TOP);
        availPieChartItem.setTitleVAlign(VerticalAlignment.TOP);
        availPieChartItem.setHeight(60);
        availPieChartItem.setWidth(60);

        form.setItems(currentField, availPieChartItem, availTimeField,  downTimeField,
            disabledTimeField, failureCountField, disabledCountField, mtbfField, mttrField, unknownField,
            currentTimeField);

        reloadSummaryData();

        return form;
    }

    private void reloadSummaryData() {
        GWTServiceLookup.getResourceService().getResourceAvailabilitySummary(resource.getId(),
            new AsyncCallback<ResourceAvailabilitySummary>() {

                @Override
                public void onSuccess(ResourceAvailabilitySummary result) {
                    Log.debug("reloadSummaryData");

                    //@todo: i18n
                    availabilitySummaryPieGraph.setAvailabilityData(
                            "Up", result.getUpPercentage(),
                            "Down", result.getDownPercentage(),
                            "Disabled" ,result.getDisabledPercentage()
                    );
                    new Timer(){

                        @Override
                        public void run() {
                            Log.debug("Run Avail Graph");
                            availabilitySummaryPieGraph.drawJsniChart();
                        }
                    }.schedule(150);

                    currentField.setValue(MSG.view_resource_monitor_availability_currentStatus_value(result
                        .getCurrent().getName(), TimestampCellFormatter.format(result.getLastChange().getTime())));
                    availTimeField.setValue(MeasurementConverterClient.format((double) result.getUpTime(),
                        MeasurementUnits.MILLISECONDS, true));
                    downTimeField.setValue(MeasurementConverterClient.format((double) result.getDownTime(),
                        MeasurementUnits.MILLISECONDS, true));
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

}
