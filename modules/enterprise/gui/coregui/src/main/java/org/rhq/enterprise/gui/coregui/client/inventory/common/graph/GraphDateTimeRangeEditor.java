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

/**
 * Component to allow selection of Date/Time range for graphs using a button group from bootstrap.
 *
 * @author Mike Thompson
 */
public class GraphDateTimeRangeEditor extends EnhancedVLayout {

    private JQueryDateTimeRangeEditor graphDateTimeRangeEditor;
    private MeasurementUserPreferences measurementUserPreferences;
    private AbstractD3GraphListView d3GraphListView;
    private static final Messages MSG = CoreGUI.getMessages();


    public GraphDateTimeRangeEditor(MeasurementUserPreferences measurementUserPrefs,AbstractD3GraphListView d3GraphListView) {
        this.measurementUserPreferences = measurementUserPrefs;
        this.d3GraphListView = d3GraphListView;
        this.graphDateTimeRangeEditor = new JQueryDateTimeRangeEditor();
    }

    /**
     * Setup the page elements especially the div and svg elements that serve as
     * placeholders for the d3 stuff to grab onto and add svg tags to render the chart.
     * Later the drawJsniGraph() is called to actually fill in the div/svg element
     * created here with the actual svg element.
     * NOTE: auxiliary javascript functions in rhq.js
     *
     */
    public void createGraphMarker() {
        Log.debug("drawGraph marker in AvailabilityD3Graph for: graphDateTimeRangeEditor" );

        // append the bootstrap buttongroup since smartGWT doesn't have one
        StringBuilder divAndSvgDefs = new StringBuilder();
        divAndSvgDefs.append("<div id=\"graphDateTimeRangeEditor\">" +
                "<div class=\"accordion\" id=\"graphDateTimeEditorAccordion\" style=\"width:895px;\">\n" +
                "        <div class=\"accordion-group\">\n" +
                "            \n" +
                "            <div id=\"graphDateTimeEditorCollapse\" class=\"accordion-body collapse in\">\n" +
                "                <div class=\"accordion-inner\">\n" +
                "                    <span id=\"timeRange\" class=\"btn-group\" data-toggle=\"buttons-radio\">\n" +
                "                        <button id=\"radioMin\" type=\"button\" class=\"btn btn-mini\" >"+MSG.chart_slider_button_bar_minute()+"</button>\n" +
                "                        <button id=\"radioHour\" type=\"button\" class=\"btn btn-mini\">"+MSG.chart_slider_button_bar_hour()+"</button>\n" +
                "                        <button id=\"radioDay\" type=\"button\" class=\"btn btn-mini\">"+MSG.chart_slider_button_bar_day()+"</button>\n" +
                "                        <button id=\"radioMonth\" type=\"button\" class=\"btn btn-mini\">"+MSG.chart_slider_button_bar_month()+"</button>\n" +
                "                        <button id=\"radioYear\" type=\"button\" class=\"btn btn-mini\">"+MSG.chart_slider_button_bar_year()+"</button>\n" +
                "                    </span>\n" +
                "                    <input id=\"dateRange\" style=\"margin-left:30px;margin-top:5px;width:280px;\" type=\"text\" readonly=\"readonly\" />\n" +
                "                    <button id=\"expandCollapseButton\" style=\"margin-left:202px;\" type=\"button\" class=\"btn btn-mini\">+/-</button>\n" +
                "                </div>\n" +
                "            </div>\n" +
                "        </div>\n" +
                "    </div>\n"+
                "<svg xmlns=\"http://www.w3.org/2000/svg\" version=\"1.1\" style=\"height:65px;\">");
        divAndSvgDefs.append("</svg></div>");
        HTMLFlow graph = new HTMLFlow(divAndSvgDefs.toString());
        graph.setWidth100();
        graph.setHeight(65);
        addMember(graph);
        new Timer(){
            @Override
            public void run() {
                attachGraphDateRangeEditorButtonGroupHandlers();
            }
        }.schedule(400);
    }

    public native void attachGraphDateRangeEditorButtonGroupHandlers() /*-{
        console.log("Draw GraphDateTimeRangeEditor");
        var global = this;

        function updateDateDisplay(startDate, endDate ) {
            var formattedDateRange;

            if (isSameDay(startDate, endDate) && isSameMonth(startDate, endDate) && isSameYear(startDate, endDate)) {
                // no need to display date only time
                formattedDateRange = startDate.format('ddd') + ': ' + startDate.format('h:mm a') + ' - ' + endDate.format('h:mm a');
            } else {
                formattedDateRange = startDate.format('MM/DD/YYYY h:mm a') + ' - ' + endDate.format('MM/DD/YYYY h:mm a');
            }
            $wnd.jQuery('#dateRange').val(formattedDateRange);
        }

        function saveDateRange(startDate, endDate){
            var start = startDate | 0, end = endDate | 0;
            global.@org.rhq.enterprise.gui.coregui.client.inventory.common.graph.GraphDateTimeRangeEditor::changeDateRange(DD)(start,end);
            //@todo: fixme
            global.@org.rhq.enterprise.gui.coregui.client.inventory.common.graph.GraphDateTimeRangeEditor::refreshGraphs()();
        }

        function isSameDay(startDate, endDate) {
            return (startDate.date() === endDate.date());
        }
        function isSameMonth(startDate, endDate) {
            return (startDate.month() === endDate.month());
        }
        function isSameYear(startDate, endDate) {
            return (startDate.year() === endDate.year());
        }

        var graphDateContext = new $wnd.GraphDateContext($wnd.moment().startOf('day'), $wnd.moment() );

        $wnd.jQuery("#radioMin").bind('click', function (event) {
            console.log("Minute selected");
            graphDateContext.startDate = $wnd.moment().startOf('hour');
            graphDateContext.endDate = $wnd.moment();
            updateDateDisplay(graphDateContext.startDate, graphDateContext.endDate);
            saveDateRange(graphDateContext.startDate, graphDateContext.endDate);
        });
        $wnd.jQuery("#radioHour").bind('click', function (event) {
            console.log("Hour selected");
            graphDateContext.startDate = $wnd.moment().startOf('day');
            graphDateContext.endDate = $wnd.moment();
            updateDateDisplay(graphDateContext.startDate, graphDateContext.endDate);
            saveDateRange(graphDateContext.startDate, graphDateContext.endDate);
        });
        $wnd.jQuery("#radioDay").bind('click', function (event) {
            console.log("Day selected");
            graphDateContext.startDate = $wnd.moment().startOf('week');
            graphDateContext.endDate = $wnd.moment();
            updateDateDisplay(graphDateContext.startDate, graphDateContext.endDate);
            saveDateRange(graphDateContext.startDate, graphDateContext.endDate);
        });
        $wnd.jQuery("#radioMonth").bind('click', function (event) {
            console.log("month selected");
            graphDateContext.startDate = $wnd.moment().startOf('month');
            graphDateContext.endDate = $wnd.moment();
            updateDateDisplay(graphDateContext.startDate, graphDateContext.endDate);
            saveDateRange(graphDateContext.startDate, graphDateContext.endDate);
        });
        $wnd.jQuery("#radioYear").bind('click', function (event) {
            console.log("year selected");
            graphDateContext.startDate = $wnd.moment().startOf('year');
            graphDateContext.endDate = $wnd.moment();
            updateDateDisplay(graphDateContext.startDate, graphDateContext.endDate);
            saveDateRange(graphDateContext.startDate, graphDateContext.endDate);
        });
        $wnd.jQuery("#expandCollapseButton").bind('click', function (event) {
            console.log("expand/collapse selected");
            $wnd.jQuery("#timeRange").toggle();

        });

        // initially populate
        updateDateDisplay(graphDateContext.startDate, graphDateContext.endDate);
        //$wnd.jQuery("#radioHour").click();

    }-*/;

    public native void refreshGraphs() /*-{
        console.log("Calling GraphDateTimeRangeEditor.refreshGraphs");
        this.@org.rhq.enterprise.gui.coregui.client.inventory.common.graph.GraphDateTimeRangeEditor::redrawGraphs()();
    }-*/;


    public void redrawGraphs(){
        d3GraphListView.redrawGraphs();
    }

    @Override
    protected void onDraw() {
        super.onDraw();
        removeMembers(getMembers());
        createGraphMarker();
    }

    @Override
    public void parentResized() {
        super.parentResized();
        removeMembers(getMembers());
        createGraphMarker();
        drawJsniChart();
    }


    /**
     * Delegate the call to rendering the JSNI chart.
     * This way the chart type can be swapped out at any time.
     */
    public void drawJsniChart() {
        graphDateTimeRangeEditor.drawJsniChart();
    }


    public Long getStartTime() {
        return measurementUserPreferences.getMetricRangePreferences().begin;
    }

    public void changeDateRange(double startTime, double endTime){

        final boolean advanced = true;
        AbstractMeasurementRangeEditor.MetricRangePreferences prefs = measurementUserPreferences.getMetricRangePreferences();
        prefs.explicitBeginEnd = advanced;
        prefs.begin = new Long(String.valueOf(startTime));
        prefs.end = new Long(String.valueOf(endTime));
        if (null != prefs.begin && null != prefs.end && prefs.begin > prefs.end) {
            CoreGUI.getMessageCenter().notify(new Message(MSG.view_measureTable_startBeforeEnd()));
        } else {
            measurementUserPreferences.setMetricRangePreferences(prefs);
        }

    }

    public Long getEndTime() {
        return measurementUserPreferences.getMetricRangePreferences().end;
    }

}
