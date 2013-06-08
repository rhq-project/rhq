/*
 * RHQ Management Platform
 * Copyright (C) 2005-2013 Red Hat, Inc.
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
package org.rhq.enterprise.gui.coregui.client.inventory.common.graph;

import java.util.Date;

import com.smartgwt.client.types.SelectionType;
import com.smartgwt.client.widgets.IButton;
import com.smartgwt.client.widgets.Label;
import com.smartgwt.client.widgets.events.ClickEvent;
import com.smartgwt.client.widgets.events.ClickHandler;
import com.smartgwt.client.widgets.toolbar.ToolStrip;

import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.Messages;
import org.rhq.enterprise.gui.coregui.client.components.measurement.AbstractMeasurementRangeEditor;
import org.rhq.enterprise.gui.coregui.client.inventory.common.AbstractD3GraphListView;
import org.rhq.enterprise.gui.coregui.client.util.Log;
import org.rhq.enterprise.gui.coregui.client.util.enhanced.EnhancedVLayout;
import org.rhq.enterprise.gui.coregui.client.util.message.Message;
import org.rhq.enterprise.gui.coregui.client.util.preferences.MeasurementUserPreferences;

/**
 * Component to allow selection of Date/Time range for graphs using a button group from bootstrap.
 *
 * @author Mike Thompson
 */
public class ButtonBarDateTimeRangeEditor2 extends EnhancedVLayout {

    static final String TIMERANGE = "timerange";
    static final int BUTTON_WIDTH = 28;

    private MeasurementUserPreferences measurementUserPreferences;
    private AbstractD3GraphListView d3GraphListView;
    private static final Messages MSG = CoreGUI.getMessages();
    private String dateTimeSelection;
    private Label dateRangeLabel;

    public ButtonBarDateTimeRangeEditor2(MeasurementUserPreferences measurementUserPrefs,
        AbstractD3GraphListView d3GraphListView) {
        this.measurementUserPreferences = measurementUserPrefs;
        this.d3GraphListView = d3GraphListView;

        AbstractMeasurementRangeEditor.MetricRangePreferences prefs = measurementUserPreferences
            .getMetricRangePreferences();
        Log.debug("ButtonBarDateTimeRangeEditor initialized with start: " + prefs.begin + " end: " + prefs.end);
        Log.debug("ButtonBarDateTimeRangeEditor initialized with start Date: " + new Date(prefs.begin) + " end Date: "
            + new Date(prefs.end));
        createButtons();
    }

    public void createButtons() {

        ToolStrip toolStrip = new ToolStrip();
        toolStrip.setWidth100();
        toolStrip.setHeight(24);

        toolStrip.addSpacer(10);

        IButton oneHourButton = new IButton("1h");
        oneHourButton.setWidth(BUTTON_WIDTH);
        oneHourButton.setActionType(SelectionType.RADIO);
        oneHourButton.setRadioGroup(TIMERANGE);
        oneHourButton.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent clickEvent) {
                Log.debug("Pressed 1h button");
                dateTimeSelection = "1h";
            }
        });
        toolStrip.addMember(oneHourButton);

        IButton sixHourButton = new IButton("6h");
        sixHourButton.setWidth(BUTTON_WIDTH);
        sixHourButton.setActionType(SelectionType.RADIO);
        sixHourButton.setRadioGroup(TIMERANGE);
        sixHourButton.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent clickEvent) {
                Log.debug("Pressed 6h button");
                dateTimeSelection = "6h";
            }
        });
        toolStrip.addMember(sixHourButton);

        IButton twelveHourButton = new IButton("12h");
        twelveHourButton.setWidth(BUTTON_WIDTH);
        twelveHourButton.setActionType(SelectionType.RADIO);
        twelveHourButton.setRadioGroup(TIMERANGE);
        twelveHourButton.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent clickEvent) {
                Log.debug("Pressed 12h button");
                dateTimeSelection = "12h";
            }
        });
        toolStrip.addMember(twelveHourButton);

        IButton oneDayButton = new IButton("1d");
        oneDayButton.setWidth(BUTTON_WIDTH);
        oneDayButton.setActionType(SelectionType.RADIO);
        oneDayButton.setRadioGroup(TIMERANGE);
        oneDayButton.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent clickEvent) {
                Log.debug("Pressed 1d button");
                dateTimeSelection = "1d";
            }
        });
        toolStrip.addMember(oneDayButton);

        IButton fiveDayButton = new IButton("5d");
        fiveDayButton.setWidth(BUTTON_WIDTH);
        fiveDayButton.setActionType(SelectionType.RADIO);
        fiveDayButton.setRadioGroup(TIMERANGE);
        fiveDayButton.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent clickEvent) {
                Log.debug("Pressed 5d button");
                dateTimeSelection = "5d";
            }
        });
        toolStrip.addMember(fiveDayButton);

        IButton oneMonthButton = new IButton("1m");
        oneMonthButton.setWidth(BUTTON_WIDTH);
        oneMonthButton.setActionType(SelectionType.RADIO);
        oneMonthButton.setRadioGroup(TIMERANGE);
        oneMonthButton.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent clickEvent) {
                Log.debug("Pressed 1m button");
                dateTimeSelection = "1m";
            }
        });
        toolStrip.addMember(oneMonthButton);

        IButton threeMonthButton = new IButton("3m");
        threeMonthButton.setWidth(BUTTON_WIDTH);
        threeMonthButton.disable();
        threeMonthButton.setActionType(SelectionType.RADIO);
        threeMonthButton.setRadioGroup(TIMERANGE);
        toolStrip.addMember(threeMonthButton);

        IButton oneYearButton = new IButton("1y");
        oneYearButton.setWidth(BUTTON_WIDTH);
        oneYearButton.disable();
        oneYearButton.setActionType(SelectionType.RADIO);
        oneYearButton.setRadioGroup(TIMERANGE);
        toolStrip.addMember(oneYearButton);

        IButton customButton = new IButton("Custom...");
        customButton.setWidth(60);
        customButton.disable();
        customButton.setActionType(SelectionType.RADIO);
        customButton.setRadioGroup(TIMERANGE);
        oneMonthButton.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent clickEvent) {
                Log.debug("Pressed custom button");
                dateTimeSelection = "custom";
            }
        });
        toolStrip.addMember(customButton);

        toolStrip.addSpacer(20);

        dateRangeLabel = new Label();
        dateRangeLabel.setContents("Range: ...");
        dateRangeLabel.setWidth(150);
        toolStrip.addMember(dateRangeLabel);

        toolStrip.addSpacer(20);

        IButton resetZoomButton = new IButton("Reset Manual Zoom");
        resetZoomButton.setWidth(120);
        toolStrip.addMember(resetZoomButton);

        //@todo: static button factory

        addMember(toolStrip);
    }

    /**
     * Setup the page elements especially the div and svg elements that serve as
     * placeholders for the d3 stuff to grab onto and add svg tags to render the chart.
     * Later the drawJsniGraph() is called to actually fill in the div/svg element
     * created here with the actual svg element.
     * NOTE: auxiliary javascript functions in rhq.js
     *
     */
    //    public void createDateSliderMarker() {
    //        Log.debug("drawGraph marker for: buttonBarDateTimeRangeEditor" );
    //
    //        // append the bootstrap buttongroup since smartGWT doesn't have one
    //        StringBuilder buttonBarDiv = new StringBuilder();
    //        buttonBarDiv.append("<div id=\"graphDateTimeRangeEditor\">" +
    //                "<div class=\"accordion\" id=\"graphDateTimeEditorAccordion\" style=\"width:758px;\">" +
    //                "        <div class=\"accordion-group\">" +
    //                "            <div id=\"graphDateTimeEditorCollapse\" class=\"accordion-body collapse in\">" +
    //                "                <div class=\"accordion-inner\">" +
    //                "                    <span id=\"timeRangeButtons\" class=\"btn-group\" data-toggle=\"buttons-radio\">" +
    //                "                        <button id=\"radio1h\" type=\"button\" class=\"btn btn-mini\" >1h</button>" +
    //                "                        <button id=\"radio6h\" type=\"button\" class=\"btn btn-mini\">6h</button>" +
    //                "                        <button id=\"radio12h\" type=\"button\" class=\"btn btn-mini\">12h</button>" +
    //                "                        <button id=\"radio1d\" type=\"button\" class=\"btn btn-mini\">1d</button>" +
    //                "                        <button id=\"radio5d\" type=\"button\" class=\"btn btn-mini\">5d</button>" +
    //                "                        <button id=\"radio1m\" type=\"button\" class=\"btn btn-mini\">1m</button>" +
    //                "                        <button id=\"radio3m\" type=\"button\" class=\"btn btn-mini\">3m</button>" +
    //                "                        <button id=\"radio1y\" type=\"button\" class=\"btn btn-mini\">1y</button>" +
    //                "                        <button id=\"radioCustom\" type=\"button\" class=\"btn btn-mini\">Custom...</button>" +
    //                "                    </span>" +
    //                "                    <span id=\"dateRange\" class=\"\" style=\"margin-left:30px;margin-top:7px;width:270px;\"  /></span>" +
    //                "                    <button id=\"buttonReset\" type=\"button\" style=\"float:right;margin-right:10px;\" class=\"btn btn-mini\">Reset Manual Zoom</button>" +
    //                "                </div>" +
    //                "            </div>" +
    //                "        </div>" +
    //                "    </div>");
    //        HTMLFlow buttonBarDivFlow = new HTMLFlow(buttonBarDiv.toString());
    //        buttonBarDivFlow.setWidth100();
    //        buttonBarDivFlow.setHeight(50);
    //        addMember(buttonBarDivFlow);
    //        new Timer(){
    //            @Override
    //            public void run() {
    //                attachGraphDateRangeEditorButtonGroupHandlers();
    //            }
    //        }.schedule(200);
    //    }

    //    public native void attachGraphDateRangeEditorButtonGroupHandlers() /*-{
    //        console.log("Draw GraphDateTimeRangeEditor");
    //        var global = this;
    //
    //        function updateDateDisplay(startDate, endDate ) {
    //            var formattedDateRange = startDate.format('MM/DD/YYYY h:mm a') + '  -  ' + endDate.format('MM/DD/YYYY h:mm a');
    //            $wnd.jQuery('#dateRange').text(formattedDateRange);
    //        }
    //
    //        function saveDateRange(startDate, endDate){
    //            var start = startDate | 0, end = endDate | 0;
    //            global.@org.rhq.enterprise.gui.coregui.client.inventory.common.graph.ButtonBarDateTimeRangeEditor2::changeDateRange(DD)(start,end);
    //            //@todo: fixme
    //            //global.@org.rhq.enterprise.gui.coregui.client.inventory.common.graph.GraphDateTimeRangeEditor::refreshGraphs()();
    //        }
    //
    //        var graphDateContext = new $wnd.GraphDateContext($wnd.moment().startOf('day'), $wnd.moment() );
    //
    //        $wnd.jQuery("#radio1h").bind('click', function (event) {
    //            console.log("1h selected");
    //            graphDateContext.startDate = $wnd.moment().subtract(1, 'hours');
    //            graphDateContext.endDate = $wnd.moment();
    //            updateDateDisplay(graphDateContext.startDate, graphDateContext.endDate);
    //            saveDateRange(graphDateContext.startDate, graphDateContext.endDate);
    //        });
    //        $wnd.jQuery("#radio6h").bind('click', function (event) {
    //            console.log(" 6h selected");
    //            graphDateContext.startDate = $wnd.moment().subtract(6, 'hours');
    //            graphDateContext.endDate = $wnd.moment();
    //            updateDateDisplay(graphDateContext.startDate, graphDateContext.endDate);
    //            saveDateRange(graphDateContext.startDate, graphDateContext.endDate);
    //        });
    //        $wnd.jQuery("#radio12h").bind('click', function (event) {
    //            console.log("12h selected");
    //            graphDateContext.startDate = $wnd.moment().subtract(12, 'hours');
    //            graphDateContext.endDate = $wnd.moment();
    //            updateDateDisplay(graphDateContext.startDate, graphDateContext.endDate);
    //            saveDateRange(graphDateContext.startDate, graphDateContext.endDate);
    //        });
    //        $wnd.jQuery("#radio1d").bind('click', function (event) {
    //            console.log("1d selected");
    //            graphDateContext.startDate = $wnd.moment().subtract(24, 'hours');
    //            graphDateContext.endDate = $wnd.moment();
    //            updateDateDisplay(graphDateContext.startDate, graphDateContext.endDate);
    //            saveDateRange(graphDateContext.startDate, graphDateContext.endDate);
    //        });
    //        $wnd.jQuery("#radio5d").bind('click', function (event) {
    //            console.log("5d selected");
    //            graphDateContext.startDate = $wnd.moment().subtract(5, 'days');
    //            graphDateContext.endDate = $wnd.moment();
    //            updateDateDisplay(graphDateContext.startDate, graphDateContext.endDate);
    //            saveDateRange(graphDateContext.startDate, graphDateContext.endDate);
    //        });
    //        $wnd.jQuery("#radio1m").bind('click', function (event) {
    //            console.log("1m selected");
    //            graphDateContext.startDate = $wnd.moment().subtract(1, 'months');
    //            graphDateContext.endDate = $wnd.moment();
    //            updateDateDisplay(graphDateContext.startDate, graphDateContext.endDate);
    //            saveDateRange(graphDateContext.startDate, graphDateContext.endDate);
    //        });
    //        $wnd.jQuery("#radio3m").bind('click', function (event) {
    //            console.log("3m selected");
    //            graphDateContext.startDate = $wnd.moment().subtract(3, 'months');
    //            graphDateContext.endDate = $wnd.moment();
    //            updateDateDisplay(graphDateContext.startDate, graphDateContext.endDate);
    //            saveDateRange(graphDateContext.startDate, graphDateContext.endDate);
    //        });
    //        $wnd.jQuery("#radio1y").bind('click', function (event) {
    //            console.log("1y selected");
    //            graphDateContext.startDate = $wnd.moment().subtract(1, 'years');
    //            graphDateContext.endDate = $wnd.moment();
    //            updateDateDisplay(graphDateContext.startDate, graphDateContext.endDate);
    //            saveDateRange(graphDateContext.startDate, graphDateContext.endDate);
    //        });
    //        $wnd.jQuery("#radioCustom").bind('click', function (event) {
    //            console.log("Custom Range selected");
    //            graphDateContext.startDate = $wnd.moment().subtract(2, 'years');
    //            graphDateContext.endDate = $wnd.moment();
    //            updateDateDisplay(graphDateContext.startDate, graphDateContext.endDate);
    //            saveDateRange(graphDateContext.startDate, graphDateContext.endDate);
    //        });
    //
    //
    //        // initially populate with default click
    //        $wnd.jQuery("#radio12h").click();
    //
    //    }-*/;
    //
    //    public native void refreshGraphs() /*-{
    //        console.log("Calling GraphDateTimeRangeEditor.refreshGraphs");
    //        this.@org.rhq.enterprise.gui.coregui.client.inventory.common.graph.ButtonBarDateTimeRangeEditor2::redrawGraphs()();
    //    }-*/;
    //

    public void redrawGraphs() {
        d3GraphListView.redrawGraphs();
    }

    @Override
    protected void onDraw() {
        super.onDraw();
        removeMembers(getMembers());
        createButtons();
    }

    @Override
    public void parentResized() {
        super.parentResized();
        removeMembers(getMembers());
        createButtons();
        //drawJsniChart();
    }

    public String getDateTimeSelection() {
        return dateTimeSelection;
    }

    public Label getDateRangeLabel() {
        return dateRangeLabel;
    }

    public Long getStartTime() {
        return measurementUserPreferences.getMetricRangePreferences().begin;
    }

    public Long getEndTime() {
        return measurementUserPreferences.getMetricRangePreferences().end;
    }

    /**
     * Whenever we make a change to the date range save it here so it gets propogated to
     * the correct places.
     * @param startTime double because JSNI doesnt support long
     * @param endTime  double because JSNI doesnt support long
     */
    public void changeDateRange(double startTime, double endTime) {

        //@todo: uncomment when ready
        final boolean advanced = true;
        AbstractMeasurementRangeEditor.MetricRangePreferences prefs = measurementUserPreferences
            .getMetricRangePreferences();
        prefs.explicitBeginEnd = advanced;
        prefs.begin = (long) startTime;
        prefs.end = (long) endTime;
        ///prefs.setPreference(PREF_METRIC_RANGE, Arrays.asList(prefs.begin, prefs.end));
        if (null != prefs.begin && null != prefs.end && prefs.begin > prefs.end) {
            CoreGUI.getMessageCenter().notify(new Message(MSG.view_measureTable_startBeforeEnd()));
        } else {
            measurementUserPreferences.setMetricRangePreferences(prefs);
        }

    }

}
