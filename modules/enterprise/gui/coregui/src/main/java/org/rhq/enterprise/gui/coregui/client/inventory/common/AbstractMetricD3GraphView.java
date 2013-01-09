/*
 * RHQ Management Platform
 * Copyright (C) 2005-2010 Red Hat, Inc.
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
package org.rhq.enterprise.gui.coregui.client.inventory.common;

import java.util.Date;
import java.util.List;

import com.google.gwt.user.client.Timer;
import com.smartgwt.client.widgets.HTMLFlow;
import com.smartgwt.client.widgets.Img;
import com.smartgwt.client.widgets.events.ClickEvent;
import com.smartgwt.client.widgets.events.ClickHandler;
import com.smartgwt.client.widgets.layout.HLayout;

import org.rhq.core.domain.measurement.Availability;
import org.rhq.core.domain.measurement.MeasurementDefinition;
import org.rhq.core.domain.measurement.MeasurementUnits;
import org.rhq.core.domain.measurement.composite.MeasurementDataNumericHighLowComposite;
import org.rhq.core.domain.measurement.composite.MeasurementNumericValueAndUnits;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.gui.coregui.client.IconEnum;
import org.rhq.enterprise.gui.coregui.client.JsonMetricProducer;
import org.rhq.enterprise.gui.coregui.client.util.Log;
import org.rhq.enterprise.gui.coregui.client.util.MeasurementConverterClient;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableHLayout;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableImg;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableVLayout;

/**
 * Provide common functionality for drawing JSNI graphs and manipulating metric
 * data so that it can be graphed.
 *
 * @author Greg Hinkle
 * @author Jay Shaughnessy
 * @author Mike Thompson
 */
public abstract class AbstractMetricD3GraphView extends LocatableVLayout implements JsonMetricProducer{

    protected HTMLFlow resourceTitle;

    private int entityId;
    private String entityName;
    private int definitionId;

    private MeasurementUnits adjustedMeasurementUnits;
    private MeasurementDefinition definition;
    private List<MeasurementDataNumericHighLowComposite> data;
    private PageList<Availability> availabilityDownList;

    private final String chartTitleMinLabel = MSG.chart_title_min_label();
    private final String chartTitleAvgLabel = MSG.chart_title_avg_label();
    private final String chartTitlePeakLabel = MSG.chart_title_peak_label();

    //private String chartHeight;

    public AbstractMetricD3GraphView(String locatorId) {
        super(locatorId);
    }


    public AbstractMetricD3GraphView(String locatorId, int entityId, String entityName, MeasurementDefinition def,
                                     List<MeasurementDataNumericHighLowComposite> data ) {
        this(locatorId);

        this.entityName = entityName;
        setEntityId(entityId);
        setDefinitionId(def.getId());
        this.definition = def;
        this.data = data;
        setHeight100();
        setWidth100();
    }

    protected abstract void renderGraph();

    protected HTMLFlow getEntityTitle(){
        return resourceTitle;
    }

    public int getEntityId() {
        return this.entityId;
    }


    public void setEntityId(int entityId) {
        this.entityId = entityId;
        this.definition = null;
    }
    public int getDefinitionId() {
        return definitionId;
    }

    public void setDefinitionId(int definitionId) {
        this.definitionId = definitionId;
        this.definition = null;
    }


    public MeasurementDefinition getDefinition() {
        return definition;
    }

    public void setDefinition(MeasurementDefinition definition) {
        this.definition = definition;
    }

    public String getChartId(){
        return entityId + "-" + definition.getId();
    }
    public List<MeasurementDataNumericHighLowComposite> getData() {
        return data;
    }

    public void setData(List<MeasurementDataNumericHighLowComposite> data) {
        this.data = data;
    }

    public PageList<Availability> getAvailabilityDownList()
    {
        return availabilityDownList;
    }

    public void setAvailabilityDownList(PageList<Availability> availabilityDownList)
    {
        this.availabilityDownList = availabilityDownList;
    }

    @Override
    protected void onDraw() {
        super.onDraw();
        removeMembers(getMembers());
        renderGraph();
    }

    @Override
    public void parentResized() {
        super.parentResized();
        removeMembers(getMembers());
        renderGraph();
    }

    /**
     * Setup the page elements especially the div and svg elements that serve as
     * placeholders for the d3 stuff to grab onto and add svg tags to render the chart.
     * Later the drawJsniGraph() is called to actually fill in the div/svg element
     * created here with the actual svg element.
     *
     */
    protected void drawGraph() {
        Log.debug("drawGraph marker in AbstractMetricD3GraphView for: "+ definition + ","+definitionId);

        HLayout titleHLayout = new LocatableHLayout(extendLocatorId("HTitle"));

        if (definition != null) {
            titleHLayout.setAutoHeight();
            titleHLayout.setWidth100();

            HTMLFlow entityTitle = getEntityTitle();
            if (null != entityTitle) {
                entityTitle.setWidth("*");
                titleHLayout.addMember(entityTitle);
            }

            if (supportsLiveGraphViewDialog()) {
                Img liveGraph = createLiveGraphImage();
                titleHLayout.addMember(liveGraph);
            }

            addMember(titleHLayout);

            StringBuilder divAndSvgDefs = new StringBuilder();
            divAndSvgDefs.append("<div id=\"rChart-" + getChartId() + "\" ><svg xmlns=\"http://www.w3.org/2000/svg\" version=\"1.1\" style=\"height:320px;\">");
            divAndSvgDefs.append(getSvgDefs());
            divAndSvgDefs.append("</svg></div>");
            HTMLFlow graph = new HTMLFlow(divAndSvgDefs.toString());
            graph.setWidth100();
            graph.setHeight100();
            addMember(graph);

            new Timer() {
                @Override
                public void run() {
                    //@todo: this is a hack around timing issue of jsni not seeing the DOM
                    drawJsniChart();
                }
            }.schedule(100);
        }
    }

    /**
     * Svg definitions for patterns and gradients to use on SVG shapes.
     * @return xml String
     */
    private static String getSvgDefs(){
      return " <defs>" +
              "               <linearGradient id=\"headerGrad\" x1=\"0%\" y1=\"0%\" x2=\"0%\" y2=\"100%\">" +
              "                   <stop offset=\"0%\" style=\"stop-color:#707883;stop-opacity:1\"/>" +
              "                   <stop offset=\"100%\" style=\"stop-color:#425b64;stop-opacity:1\"/>" +
              "               </linearGradient>" +
              "               <linearGradient id=\"leaderBarGrad\" x1=\"0%\" y1=\"0%\" x2=\"0%\" y2=\"100%\">" +
              "                   <stop offset=\"0%\" style=\"stop-color:#d3d3d6;stop-opacity:1\"/>" +
              "                   <stop offset=\"100%\" style=\"stop-color:#d3d3d6;stop-opacity:1\"/>" +
              "               </linearGradient>" +
              "               <linearGradient id=\"heavyLeaderBarGrad\" x1=\"0%\" y1=\"0%\" x2=\"0%\" y2=\"100%\">" +
              "                   <stop offset=\"0%\" style=\"stop-color:#a7a7ac;stop-opacity:1\"/>" +
              "                   <stop offset=\"100%\" style=\"stop-color:#a7a7ac;stop-opacity:1\"/>" +
              "               </linearGradient>" +
              "               <linearGradient id=\"topBarGrad\" x1=\"0%\" y1=\"0%\" x2=\"0%\" y2=\"100%\">" +
              "                   <stop offset=\"0%\" style=\"stop-color:#067aba;stop-opacity:1\"/>" +
              "                   <stop offset=\"100%\" style=\"stop-color:#1278a8;stop-opacity:1\"/>" +
              "               </linearGradient>" +
              "               <linearGradient id=\"bottomBarGrad\" x1=\"0%\" y1=\"0%\" x2=\"0%\" y2=\"100%\">" +
              "                   <stop offset=\"0%\" style=\"stop-color:#6bc0e6;stop-opacity:1\"/>" +
              "                   <stop offset=\"100%\" style=\"stop-color:#388bb0;stop-opacity:1\"/>" +
              "               </linearGradient>" +
              "               <pattern id=\"grayStripes\" patternUnits=\"userSpaceOnUse\" x=\"0\" y=\"0\"" +
              "                        width=\"6\" height=\"3\">" +
              "                   <path d=\"M 0 0 6 0\" style=\"stroke:#dfdfdf; fill:none;\"/>" +
              "               </pattern>" +
              "               <pattern id=\"redStripes\" patternUnits=\"userSpaceOnUse\" x=\"0\" y=\"0\"" +
              "                        width=\"6\" height=\"3\">" +
              "                   <path d=\"M 0 0 6 0\" style=\"stroke:#ff8a9a; fill:none;\"/>" +
              "               </pattern>" +
              "</defs>";
    }

    public abstract  void drawJsniChart();

    private Img createLiveGraphImage() {
        Img liveGraph = new LocatableImg(extendLocatorId("Live"), IconEnum.RECENT_MEASUREMENTS.getIcon16x16Path(), 16, 16);
        liveGraph.setTooltip(MSG.view_resource_monitor_graph_live_tooltip());

        liveGraph.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent clickEvent) {
                displayLiveGraphViewDialog();
            }
        });
        return liveGraph;
    }

//    public void setChartHeight(String height) {
//        this.chartHeight = height;
//    }

    protected boolean supportsLiveGraphViewDialog() {
        return false;
    }

    protected void displayLiveGraphViewDialog() {
        return;
    }

    @Override
    public void destroy() {
        super.destroy();
    }

    @Override
    public void hide() {
        super.hide();
    }

    public String getYAxisTitle(){
        if(null != definition.getDisplayName() && definition.getDisplayName().length() > 55){
            return entityName+" - " +definition.getDisplayName().substring(0,55)+"..."  ;
        }else {
            return entityName+" - " +definition.getDisplayName()  ;
        }
    }

    /**
     * Returns the y-axis units normalized to highest scale (Bytes -> Gb).
     * NOTE: this requires a dependency such that getJsonMetrics is called
     * before this method as the adjustedMeasurementUnits are calculated in that method.
     * @return yAxisUnits -- normalized to highest UOM.
     */
    public String getYAxisUnits(){
        if(adjustedMeasurementUnits == null){
           Log.error("AbstractMetricD3GraphView.adjustedMeasurementUnits is populated by getJsonMetrics. Make sure it is called first.");
        }
       return adjustedMeasurementUnits.toString();
    }

    public String getXAxisTitle(){
       return MSG.view_charts_time_axis_label();
    }

    @Override
    public String getJsonMetrics(){
        StringBuilder sb = new StringBuilder("[");
        boolean gotAdjustedMeasurementUnits = false;
        //Log.debug(" avail records loaded: "+getAvailabilityDownList().size());
        for (MeasurementDataNumericHighLowComposite measurement : data) {
            sb.append("{ x:"+measurement.getTimestamp()+",");
            if(isTimestampDownOrDisabled(measurement.getTimestamp())){
                sb.append(" down:true, ");
            }
            if(!Double.isNaN(measurement.getValue())){

                MeasurementNumericValueAndUnits newHigh = normalizeUnitsAndValues(measurement.getHighValue(), definition.getUnits());
                MeasurementNumericValueAndUnits newLow = normalizeUnitsAndValues(measurement.getLowValue(), definition.getUnits());
                MeasurementNumericValueAndUnits newValue = normalizeUnitsAndValues(measurement.getValue(), definition.getUnits());
                if(!gotAdjustedMeasurementUnits){
                    adjustedMeasurementUnits = newValue.getUnits();
                   gotAdjustedMeasurementUnits = true;
                }
                sb.append(" high:"+newHigh.getValue()+",");
                sb.append(" low:"+newLow.getValue()+",");
                sb.append(" y:"+newValue.getValue()+"},");
            }else {
                // NaN measure no measurement was collected
                sb.append(" nodata:true },");
            }
        }
        sb.setLength(sb.length()-1); // delete the last ','
        sb.append("]");
        //Log.debug("Json data has "+data.size()+" entries.");
        //Log.debug(sb.toString());
        return sb.toString();
    }


    private boolean isTimestampDownOrDisabled(long timestamp){
        Date timestampDate = new Date(timestamp);
        if(null == availabilityDownList){
            //@todo: take this out this is just to testing purposes
            Log.debug("AvailabilityList is null");
        }
        if(null != availabilityDownList){
            for (Availability availability : availabilityDownList)
            {
                if(timestampDate.after(new Date(availability.getStartTime()))
                        && timestampDate.before(new Date(availability.getEndTime()))){
                    return true;
                }
            }
        }
        return false;
    }


    private  MeasurementNumericValueAndUnits normalizeUnitsAndValues(double value, MeasurementUnits measurementUnits){
        MeasurementNumericValueAndUnits newValue = MeasurementConverterClient.fit(value, measurementUnits);
        MeasurementNumericValueAndUnits returnValue = null;

        // adjust for percentage numbers
        if(measurementUnits.equals(MeasurementUnits.PERCENTAGE)) {
            returnValue = new MeasurementNumericValueAndUnits(newValue.getValue() * 100,newValue.getUnits());
        }  else {
            returnValue = new MeasurementNumericValueAndUnits(newValue.getValue() ,newValue.getUnits());
        }

        return returnValue;
    }

    public String getChartTitleMinLabel()
    {
        return chartTitleMinLabel;
    }

    public String getChartTitleAvgLabel()
    {
        return chartTitleAvgLabel;
    }

    public String getChartTitlePeakLabel()
    {
        return chartTitlePeakLabel;
    }

    /**
     * If there is more than 2 days time window then return true so we can show day of week
     * in axis labels. Function to switch the timescale to whichever is more appropriate hours
     * or hours with days of week.
     * @return true if difference between startTime and endTime is >= x days
     */
   public boolean shouldDisplayDayOfWeekInXAxisLabel(){
       Long startTime = data.get(0).getTimestamp();
       Long endTime = data.get(data.size() -1).getTimestamp();
       long timeThreshold = 24 * 60 * 60 * 1000; // 1 days
       return  startTime + timeThreshold < endTime;
   }
}
