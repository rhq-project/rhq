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

import java.util.Arrays;
import java.util.Date;
import java.util.List;

import com.google.gwt.user.client.Timer;
import com.smartgwt.client.widgets.HTMLFlow;

import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.Messages;
import org.rhq.enterprise.gui.coregui.client.components.measurement.AbstractMeasurementRangeEditor;
import org.rhq.enterprise.gui.coregui.client.inventory.common.AbstractD3GraphListView;
import org.rhq.enterprise.gui.coregui.client.util.Log;
import org.rhq.enterprise.gui.coregui.client.util.enhanced.EnhancedVLayout;
import org.rhq.enterprise.gui.coregui.client.util.message.Message;
import org.rhq.enterprise.gui.coregui.client.util.preferences.MeasurementUserPreferences;
import org.rhq.enterprise.gui.coregui.client.util.preferences.UserPreferenceNames;

/**
 * Component to allow selection of Date/Time range for graphs using a button group from bootstrap.
 *
 * @author Mike Thompson
 */
@Deprecated
public class ButtonBarDateTimeRangeEditor extends EnhancedVLayout {

    private MeasurementUserPreferences measurementUserPreferences;
    private AbstractD3GraphListView d3GraphListView;
    private static final Messages MSG = CoreGUI.getMessages();


    public ButtonBarDateTimeRangeEditor(MeasurementUserPreferences measurementUserPrefs,
                                        AbstractD3GraphListView d3GraphListView) {
        this.measurementUserPreferences = measurementUserPrefs;
        this.d3GraphListView = d3GraphListView;

        AbstractMeasurementRangeEditor.MetricRangePreferences prefs = measurementUserPreferences.getMetricRangePreferences();
        Log.debug("ButtonBarDateTimeRangeEditor initialized with start: "+ prefs.begin +" end: "+prefs.end);
        Log.debug("ButtonBarDateTimeRangeEditor initialized with start Date: "+ new Date(prefs.begin) +" end Date: "+new Date(prefs.end));
    }

    /**
     * Setup the page elements especially the div and svg elements that serve as
     * placeholders for the d3 stuff to grab onto and add svg tags to render the chart.
     * Later the drawJsniGraph() is called to actually fill in the div/svg element
     * created here with the actual svg element.
     * NOTE: auxiliary javascript functions in rhq.js
     *
     */
    public void createDateSliderMarker() {
        Log.debug("drawGraph marker for: buttonBarDateTimeRangeEditor" );

        // append the bootstrap buttongroup since smartGWT doesn't have one
        StringBuilder buttonBarDiv = new StringBuilder();
        buttonBarDiv.append("<div id=\"graphDateTimeRangeEditor\">" +
                "<div class=\"accordion\" id=\"graphDateTimeEditorAccordion\" style=\"width:758px;\">" +
                "        <div class=\"accordion-group\">" +
                "            <div id=\"graphDateTimeEditorCollapse\" class=\"accordion-body collapse in\">" +
                "                <div class=\"accordion-inner\">" +
                "                    <span id=\"timeRangeButtons\" class=\"btn-group\" data-toggle=\"buttons-radio\">" +
                "                        <button id=\"radio1h\" type=\"button\" class=\"btn btn-mini\" >1h</button>" +
                "                        <button id=\"radio6h\" type=\"button\" class=\"btn btn-mini\">6h</button>" +
                "                        <button id=\"radio12h\" type=\"button\" class=\"btn btn-mini\">12h</button>" +
                "                        <button id=\"radio1d\" type=\"button\" class=\"btn btn-mini\">1d</button>" +
                "                        <button id=\"radio5d\" type=\"button\" class=\"btn btn-mini\">5d</button>" +
                "                        <button id=\"radio1m\" type=\"button\" class=\"btn btn-mini\">1m</button>" +
                "                        <button id=\"radio3m\" type=\"button\" class=\"btn btn-mini\">3m</button>" +
                "                        <button id=\"radio1y\" type=\"button\" class=\"btn btn-mini\">1y</button>" +
                "                        <button id=\"radioCustom\" type=\"button\" class=\"btn btn-mini\">Custom...</button>" +
                "                    </span>" +
                "                    <span id=\"dateRange\" class=\"\" style=\"margin-left:30px;margin-top:7px;width:270px;\"  /></span>" +
                "                    <button id=\"buttonReset\" type=\"button\" style=\"float:right;margin-right:10px;\" class=\"btn btn-mini\">Reset Manual Zoom</button>" +
                "                </div>" +
                "            </div>" +
                "        </div>" +
                "    </div>");
        HTMLFlow buttonBarDivFlow = new HTMLFlow(buttonBarDiv.toString());
        buttonBarDivFlow.setWidth100();
        buttonBarDivFlow.setHeight(50);
        addMember(buttonBarDivFlow);
        new Timer(){
            @Override
            public void run() {
                attachGraphDateRangeEditorButtonGroupHandlers();
            }
        }.schedule(200);
    }

    public native void attachGraphDateRangeEditorButtonGroupHandlers() /*-{
        console.log("Draw GraphDateTimeRangeEditor");
        var global = this;

        function updateDateDisplay(startDate, endDate ) {
            var formattedDateRange = startDate.format('MM/DD/YYYY h:mm a') + '  -  ' + endDate.format('MM/DD/YYYY h:mm a');
            $wnd.jQuery('#dateRange').text(formattedDateRange);
        }

        function saveDateRange(startDate, endDate){
            var start = startDate | 0, end = endDate | 0;
            global.@org.rhq.enterprise.gui.coregui.client.inventory.common.graph.ButtonBarDateTimeRangeEditor::changeDateRange(DD)(start,end);
            //@todo: fixme
            //global.@org.rhq.enterprise.gui.coregui.client.inventory.common.graph.GraphDateTimeRangeEditor::refreshGraphs()();
        }

        var graphDateContext = new $wnd.GraphDateContext($wnd.moment().startOf('day'), $wnd.moment() );

        $wnd.jQuery("#radio1h").bind('click', function (event) {
            console.log("1h selected");
            graphDateContext.startDate = $wnd.moment().subtract(1, 'hours');
            graphDateContext.endDate = $wnd.moment();
            updateDateDisplay(graphDateContext.startDate, graphDateContext.endDate);
            saveDateRange(graphDateContext.startDate, graphDateContext.endDate);
        });
        $wnd.jQuery("#radio6h").bind('click', function (event) {
            console.log(" 6h selected");
            graphDateContext.startDate = $wnd.moment().subtract(6, 'hours');
            graphDateContext.endDate = $wnd.moment();
            updateDateDisplay(graphDateContext.startDate, graphDateContext.endDate);
            saveDateRange(graphDateContext.startDate, graphDateContext.endDate);
        });
        $wnd.jQuery("#radio12h").bind('click', function (event) {
            console.log("12h selected");
            graphDateContext.startDate = $wnd.moment().subtract(12, 'hours');
            graphDateContext.endDate = $wnd.moment();
            updateDateDisplay(graphDateContext.startDate, graphDateContext.endDate);
            saveDateRange(graphDateContext.startDate, graphDateContext.endDate);
        });
        $wnd.jQuery("#radio1d").bind('click', function (event) {
            console.log("1d selected");
            graphDateContext.startDate = $wnd.moment().subtract(24, 'hours');
            graphDateContext.endDate = $wnd.moment();
            updateDateDisplay(graphDateContext.startDate, graphDateContext.endDate);
            saveDateRange(graphDateContext.startDate, graphDateContext.endDate);
        });
        $wnd.jQuery("#radio5d").bind('click', function (event) {
            console.log("5d selected");
            graphDateContext.startDate = $wnd.moment().subtract(5, 'days');
            graphDateContext.endDate = $wnd.moment();
            updateDateDisplay(graphDateContext.startDate, graphDateContext.endDate);
            saveDateRange(graphDateContext.startDate, graphDateContext.endDate);
        });
        $wnd.jQuery("#radio1m").bind('click', function (event) {
            console.log("1m selected");
            graphDateContext.startDate = $wnd.moment().subtract(1, 'months');
            graphDateContext.endDate = $wnd.moment();
            updateDateDisplay(graphDateContext.startDate, graphDateContext.endDate);
            saveDateRange(graphDateContext.startDate, graphDateContext.endDate);
        });
        $wnd.jQuery("#radio3m").bind('click', function (event) {
            console.log("3m selected");
            graphDateContext.startDate = $wnd.moment().subtract(3, 'months');
            graphDateContext.endDate = $wnd.moment();
            updateDateDisplay(graphDateContext.startDate, graphDateContext.endDate);
            saveDateRange(graphDateContext.startDate, graphDateContext.endDate);
        });
        $wnd.jQuery("#radio1y").bind('click', function (event) {
            console.log("1y selected");
            graphDateContext.startDate = $wnd.moment().subtract(1, 'years');
            graphDateContext.endDate = $wnd.moment();
            updateDateDisplay(graphDateContext.startDate, graphDateContext.endDate);
            saveDateRange(graphDateContext.startDate, graphDateContext.endDate);
        });
        $wnd.jQuery("#radioCustom").bind('click', function (event) {
            console.log("Custom Range selected");
            graphDateContext.startDate = $wnd.moment().subtract(2, 'years');
            graphDateContext.endDate = $wnd.moment();
            updateDateDisplay(graphDateContext.startDate, graphDateContext.endDate);
            saveDateRange(graphDateContext.startDate, graphDateContext.endDate);
        });


        // initially populate with default click
        $wnd.jQuery("#radio12h").click();

    }-*/;

    public native void refreshGraphs() /*-{
        console.log("Calling GraphDateTimeRangeEditor.refreshGraphs");
        this.@org.rhq.enterprise.gui.coregui.client.inventory.common.graph.ButtonBarDateTimeRangeEditor::redrawGraphs()();
    }-*/;



    public void redrawGraphs(){
        d3GraphListView.redrawGraphs();
    }

    @Override
    protected void onDraw() {
        super.onDraw();
        removeMembers(getMembers());
        createDateSliderMarker();
    }

    @Override
    public void parentResized() {
        super.parentResized();
        removeMembers(getMembers());
        createDateSliderMarker();
        //drawJsniChart();
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
    public void changeDateRange(double startTime, double endTime){

        //@todo: uncomment when ready
        final boolean advanced = true;
        AbstractMeasurementRangeEditor.MetricRangePreferences prefs = measurementUserPreferences.getMetricRangePreferences();
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
