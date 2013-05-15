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
package org.rhq.enterprise.gui.coregui.client.inventory.common.charttype;

import com.google.gwt.user.client.Timer;
import com.smartgwt.client.widgets.HTMLFlow;

import org.rhq.enterprise.gui.coregui.client.util.Log;
import org.rhq.enterprise.gui.coregui.client.util.enhanced.EnhancedVLayout;
import org.rhq.enterprise.gui.coregui.client.util.preferences.MeasurementUserPreferences;

/**
 * Component to allow selection of Date/Time range for graphs.
 *
 * @author Mike Thompson
 */
public class GraphDateTimeRangeEditor extends EnhancedVLayout {

    private JQueryDateTimeRangeEditor graphDateTimeRangeEditor;
    private MeasurementUserPreferences measurementUserPreferences;


    public GraphDateTimeRangeEditor(MeasurementUserPreferences measurementUserPrefs) {
        this.measurementUserPreferences = measurementUserPrefs;
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
                "<div class=\"accordion\" id=\"graphDateTimeEditorAccordion\" style=\"width:875px;>\n" +
                "        <div class=\"accordion-group\">\n" +
                "            \n" +
                "            <div id=\"graphDateTimeEditorCollapse\" class=\"accordion-body collapse in\">\n" +
                "                <div class=\"accordion-inner\">\n" +
                "                    <span class=\"btn-group\" data-toggle=\"buttons-radio\">\n" +
                "                        <button id=\"radioMin\" type=\"button\" class=\"btn \" >Min</button>\n" +
                "                        <button id=\"radioHour\" type=\"button\" class=\"btn \">Hour</button>\n" +
                "                        <button id=\"radioWeek\" type=\"button\" class=\"btn \">Week</button>\n" +
                "                        <button id=\"radioMonth\" type=\"button\" class=\"btn \">Month</button>\n" +
                "                        <button id=\"radioYear\" type=\"button\" class=\"btn \">Year</button>\n" +
                "                    </span>\n" +
                "                   <input id=\"dateRange\" style=\"margin-left:30px;width:280px;\" type=\"text\"/>\n" +
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
        console.warn("Registering Handlers");

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
            console.warn("minute selected");
            graphDateContext.startDate = $wnd.moment().startOf('hour');
            updateDateDisplay(graphDateContext);
        });
        $wnd.jQuery("#radioHour").bind('click', function (event) {
            console.log("Hour selected");
            graphDateContext.startDate = $wnd.moment().startOf('hour');
            updateDateDisplay(graphDateContext.startDate, graphDateContext.endDate);
        });
        $wnd.jQuery("#radioWeek").bind('click', function (event) {
            console.log("Week selected");
            graphDateContext.startDate = $wnd.moment().startOf('week');
            updateDateDisplay(graphDateContext.startDate, graphDateContext.endDate);
        });
        $wnd.jQuery("#radioMonth").bind('click', function (event) {
            console.log("month selected");
            graphDateContext.startDate = $wnd.moment().startOf('month');
            updateDateDisplay(graphDateContext.startDate, graphDateContext.endDate);
        });
        $wnd.jQuery("#radioYear").bind('click', function (event) {
            console.log("year selected");
            graphDateContext.startDate = $wnd.moment().startOf('year');
            updateDateDisplay(graphDateContext.startDate, graphDateContext.endDate);
        });

        // initially populate
        updateDateDisplay(graphDateContext.startDate, graphDateContext.endDate);
        $wnd.jQuery("#radioHour").click();

    }-*/;

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

//    public void setStartTime(Long startTime) {
//        this.startTime = startTime;
//    }

    public Long getEndTime() {
        return measurementUserPreferences.getMetricRangePreferences().end;
    }

//    public void setEndTime(Long endTime) {
//        this.endTime = endTime;
//    }
}
