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

import java.util.List;

import com.google.gwt.user.client.Timer;
import com.smartgwt.client.widgets.HTMLFlow;

import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.Messages;
import org.rhq.enterprise.gui.coregui.client.components.measurement.AbstractMeasurementRangeEditor;
import org.rhq.enterprise.gui.coregui.client.inventory.common.AbstractD3GraphListView;
import org.rhq.enterprise.gui.coregui.client.inventory.common.graph.graphtype.DateSliderGraphType;
import org.rhq.enterprise.gui.coregui.client.util.Log;
import org.rhq.enterprise.gui.coregui.client.util.enhanced.EnhancedVLayout;
import org.rhq.enterprise.gui.coregui.client.util.preferences.MeasurementUserPreferences;

/**
 * Component to allow selection of Date/Time range for graphs using a button group from bootstrap.
 *
 * @author Mike Thompson
 */
public class GraphDateTimeRangeEditor extends EnhancedVLayout {

    private MeasurementUserPreferences measurementUserPreferences;
    private AbstractD3GraphListView d3GraphListView;
    private static final Messages MSG = CoreGUI.getMessages();
    //private DateSliderGraphType dateSliderGraphType;
    private Long startDateTime;
    private Long endDateTime;


    public GraphDateTimeRangeEditor(MeasurementUserPreferences measurementUserPrefs,
                                    AbstractD3GraphListView d3GraphListView) {
        this.measurementUserPreferences = measurementUserPrefs;
        this.d3GraphListView = d3GraphListView;


        AbstractMeasurementRangeEditor.MetricRangePreferences prefs = measurementUserPreferences.getMetricRangePreferences();

        // initialize with saved begin/end times
        List<Long> beginEndTimes = prefs.getBeginEndTimes();
        startDateTime = beginEndTimes.get(0);
        endDateTime = beginEndTimes.get(1);
     //   this.dateSliderGraphType = new DateSliderGraphType(beginEndTimes.get(0), beginEndTimes.get(1));
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
        Log.debug("drawGraph marker in AvailabilityD3Graph for: graphDateTimeRangeEditor" );

        // append the bootstrap buttongroup since smartGWT doesn't have one
        StringBuilder divAndSvgDefs = new StringBuilder();
        divAndSvgDefs.append("<div id=\"graphDateTimeRangeEditor\">" +
                "<div class=\"accordion\" id=\"graphDateTimeEditorAccordion\" style=\"width:758px;\">" +
                "        <div class=\"accordion-group\">" +
                "            <div id=\"graphDateTimeEditorCollapse\" class=\"accordion-body collapse in\">" +
                "                <div class=\"accordion-inner\">" +
                "<div>"+
                "                    <span id=\"timeRangeButtons\" class=\"btn-group\" data-toggle=\"buttons-radio\">" +
                "                        <button id=\"radio1h\" type=\"button\" class=\"btn btn-mini\" >1h</button>" +
                "                        <button id=\"radio6h\" type=\"button\" class=\"btn btn-mini\">6h</button>" +
                "                        <button id=\"radio12h\" type=\"button\" class=\"btn btn-mini\">12h</button>" +
                "                        <button id=\"radio1d\" type=\"button\" class=\"btn btn-mini\">1d</button>" +
                "                        <button id=\"radio5d\" type=\"button\" class=\"btn btn-mini\">5d</button>" +
                "                        <button id=\"radio1m\" type=\"button\" class=\"btn btn-mini\">1m</button>" +
                "                        <button id=\"radio3m\" type=\"button\" class=\"btn btn-mini\">3m</button>" +
                "                        <button id=\"radio1y\" type=\"button\" class=\"btn btn-mini\">1y</button>" +
                "                    </span>" +
                "                    <input id=\"dateRange\" style=\"margin-left:30px;margin-top:5px;width:280px;\" type=\"text\" readonly=\"readonly\" />" +
                "</div>"+
                "                    <button id=\"buttonCustom\" type=\"button\" class=\"btn btn-mini btn-link\">Custom...</button>" +
                "                </div>" +
                "            </div>" +
                "        </div>" +
                "    </div>");
        HTMLFlow graph = new HTMLFlow(divAndSvgDefs.toString());
        graph.setWidth100();
        graph.setHeight(65);
        addMember(graph);
        new Timer(){
            @Override
            public void run() {
                changeDateRange(getStartTime(), getEndTime());
                //drawJsniChart(dateSliderGraphType.getStartTime(), dateSliderGraphType.getEndTime());
                attachGraphDateRangeEditorButtonGroupHandlers();
            }
        }.schedule(400);
    }

    public native void attachGraphDateRangeEditorButtonGroupHandlers() /*-{
        console.log("Draw GraphDateTimeRangeEditor");
        var global = this;

        function updateDateDisplay(startDate, endDate ) {
            var formattedDateRange = startDate.format('MM/DD/YYYY h:mm a') + ' - ' + endDate.format('MM/DD/YYYY h:mm a');
            $wnd.jQuery('#dateRange').val(formattedDateRange);
        }

        function saveDateRange(startDate, endDate){
            var start = startDate | 0, end = endDate | 0;
            global.@org.rhq.enterprise.gui.coregui.client.inventory.common.graph.GraphDateTimeRangeEditor::changeDateRange(DD)(start,end);
            //@todo: fixme
            //global.@org.rhq.enterprise.gui.coregui.client.inventory.common.graph.GraphDateTimeRangeEditor::refreshGraphs()();
        }

        var graphDateContext = new $wnd.GraphDateContext($wnd.moment().startOf('day'), $wnd.moment() );

        $wnd.jQuery("#radioMin").bind('click', function (event) {
            console.log("Minute selected");
            graphDateContext.startDate = $wnd.moment().startOf('hour');
            graphDateContext.endDate = $wnd.moment();
            updateDateDisplay(graphDateContext.startDate, graphDateContext.endDate);
            saveDateRange(graphDateContext.startDate, graphDateContext.endDate);
            //this.@org.rhq.enterprise.gui.coregui.client.inventory.common.graph.GraphDateTimeRangeEditor::drawJsniDateSlider(Ljava/lang/Long;Ljava/lang/Long;)(graphDateContext.startDate, graphDateContext.endDate);
        });
        $wnd.jQuery("#radioHour").bind('click', function (event) {
            console.log("Hour selected");
            graphDateContext.startDate = $wnd.moment().startOf('day');
            graphDateContext.endDate = $wnd.moment();
            updateDateDisplay(graphDateContext.startDate, graphDateContext.endDate);
            saveDateRange(graphDateContext.startDate, graphDateContext.endDate);
            //this.@org.rhq.enterprise.gui.coregui.client.inventory.common.graph.GraphDateTimeRangeEditor::drawJsniDateSlider(Ljava/lang/Long;Ljava/lang/Long;)(graphDateContext.startDate, graphDateContext.endDate);
        });
        $wnd.jQuery("#radioDay").bind('click', function (event) {
            console.log("Day selected");
            graphDateContext.startDate = $wnd.moment().startOf('week');
            graphDateContext.endDate = $wnd.moment();
            updateDateDisplay(graphDateContext.startDate, graphDateContext.endDate);
            saveDateRange(graphDateContext.startDate, graphDateContext.endDate);
            //this.@org.rhq.enterprise.gui.coregui.client.inventory.common.graph.GraphDateTimeRangeEditor::drawJsniDateSlider(Ljava/lang/Long;Ljava/lang/Long;)(graphDateContext.startDate, graphDateContext.endDate);
        });
        $wnd.jQuery("#radioMonth").bind('click', function (event) {
            console.log("month selected");
            graphDateContext.startDate = $wnd.moment().startOf('month');
            graphDateContext.endDate = $wnd.moment();
            updateDateDisplay(graphDateContext.startDate, graphDateContext.endDate);
            saveDateRange(graphDateContext.startDate, graphDateContext.endDate);
            //this.@org.rhq.enterprise.gui.coregui.client.inventory.common.graph.GraphDateTimeRangeEditor::drawJsniDateSlider(Ljava/lang/Long;Ljava/lang/Long;)(graphDateContext.startDate, graphDateContext.endDate);
        });
        $wnd.jQuery("#radioYear").bind('click', function (event) {
            console.log("year selected");
            graphDateContext.startDate = $wnd.moment().startOf('year');
            graphDateContext.endDate = $wnd.moment();
            updateDateDisplay(graphDateContext.startDate, graphDateContext.endDate);
            saveDateRange(graphDateContext.startDate, graphDateContext.endDate);
            //this.@org.rhq.enterprise.gui.coregui.client.inventory.common.graph.GraphDateTimeRangeEditor::drawJsniDateSlider(Ljava/lang/Long;Ljava/lang/Long;)(graphDateContext.startDate, graphDateContext.endDate);
        });
        $wnd.jQuery("#expandCollapseButton").bind('click', function (event) {
            console.log("expand/collapse selected");
            //$wnd.jQuery("#timeRange").toggle();
            var buttonBarVisibility = $wnd.jQuery("#timeRangeButtons").attr("visibility");
            if(buttonBarVisibility === 'none'){
                $wnd.jQuery("#timeRangeButtons").attr("visibility","visible");
            }else {
                $wnd.jQuery("#timeRangeButtons").attr("visibility","none");
            }

        });

        // initially populate
        updateDateDisplay(graphDateContext.startDate, graphDateContext.endDate);
        //$wnd.jQuery("#radioHour").click();

    }-*/;

    public native void refreshGraphs() /*-{
        console.log("Calling GraphDateTimeRangeEditor.refreshGraphs");
        this.@org.rhq.enterprise.gui.coregui.client.inventory.common.graph.GraphDateTimeRangeEditor::redrawGraphs()();
    }-*/;


//    public void drawJsniDateSlider(){
//        dateSliderGraphType.drawJsniChart(dateSliderGraphType.getStartTime(), dateSliderGraphType.getEndTime());
//    }
//
//    public void drawJsniDateSlider(Long startTime, Long endTime){
//        dateSliderGraphType.drawJsniChart(startTime, endTime);
//    }

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
        //createDateSliderMarker();
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

        startDateTime = (long)startTime;
        endDateTime = (long)endTime;

        //@todo: uncomment when ready
//        final boolean advanced = true;
//        AbstractMeasurementRangeEditor.MetricRangePreferences prefs = measurementUserPreferences.getMetricRangePreferences();
//        prefs.explicitBeginEnd = advanced;
//        prefs.begin = (long) startTime;
//        prefs.end = (long) endTime;
//        if (null != prefs.begin && null != prefs.end && prefs.begin > prefs.end) {
//            CoreGUI.getMessageCenter().notify(new Message(MSG.view_measureTable_startBeforeEnd()));
//        } else {
//            measurementUserPreferences.setMetricRangePreferences(prefs);
//        }

    }



}
