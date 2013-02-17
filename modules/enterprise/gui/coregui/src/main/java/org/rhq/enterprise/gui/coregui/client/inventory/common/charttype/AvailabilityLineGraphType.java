/*
 * RHQ Management Platform
 * Copyright (C) 2005-2012 Red Hat, Inc.
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

import java.util.Date;
import java.util.List;

import org.rhq.core.domain.measurement.Availability;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.domain.measurement.MeasurementUnits;
import org.rhq.core.domain.measurement.composite.MeasurementDataNumericHighLowComposite;
import org.rhq.core.domain.measurement.composite.MeasurementNumericValueAndUnits;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.gui.coregui.client.util.Log;
import org.rhq.enterprise.gui.coregui.client.util.MeasurementConverterClient;

/**
 * Contains the javascript chart definition for an implementation of
 * the d3 availability chart. This implementation is just a line that
 * changes color based on availability type: up=green, down=red, unknown=grey.
 *
 * @author Mike Thompson
 */
public class AvailabilityLineGraphType {

    private List<MeasurementDataNumericHighLowComposite> metricData;
    private PageList<Availability> availabilityList;
    private Integer entityId;

    /**
     * General constructor for stacked bar graph when you have all the data needed to
     * produce the graph. (This is true for all cases but the dashboard portlet).
     */
    public AvailabilityLineGraphType(Integer entityId){
        this.entityId = entityId;
    }

    public void setAvailabilityList(PageList<Availability> availabilityList) {
        this.availabilityList = availabilityList;
    }

    public void setMetricData(List<MeasurementDataNumericHighLowComposite> metricData) {
        this.metricData = metricData;
    }

    public String getAvailabilityJson() {
        StringBuilder sb = new StringBuilder("[");
        //Log.debug(" avail records loaded: " + availabilityList.size());
        if (null != metricData) {

            for (MeasurementDataNumericHighLowComposite measurement : metricData) {
                sb.append("{ \"x\":" + measurement.getTimestamp() + ",");

                if (null != availabilityList) {
                    // loop through the avail down intervals
                    for (Availability availability : availabilityList) {

                        // we know we are in an interval
                        // @todo: when resource is down measurement is null NPE
                        if (measurement.getTimestamp() >= availability.getStartTime()
                                && measurement.getTimestamp() <= availability.getEndTime()) {
                            if (availability.getAvailabilityType().equals(AvailabilityType.DOWN)
                                    || availability.getAvailabilityType().equals(AvailabilityType.DISABLED)) {
                                sb.append(" \"availStart\":" + availability.getStartTime() + ", ");
                                sb.append(" \"availEnd\":" + availability.getEndTime() + ", ");
                                long availDuration = availability.getEndTime() - availability.getStartTime();
                                String availDurationString = MeasurementConverterClient.format((double) availDuration,
                                        MeasurementUnits.MILLISECONDS, true);
                                sb.append(" \"availDuration\": \"" + availDurationString + "\", ");
                            } else if (availability.getAvailabilityType().equals(AvailabilityType.UNKNOWN)) {
                                sb.append(" \"unknownStart\":" + availability.getStartTime() + ", ");
                                sb.append(" \"unknownEnd\":" + availability.getEndTime() + ", ");
                                long availDuration = availability.getEndTime() - availability.getStartTime();
                                String availDurationString = MeasurementConverterClient.format((double) availDuration,
                                        MeasurementUnits.MILLISECONDS, true);
                                sb.append(" \"unknownDuration\": \"" + availDurationString + "\", ");

                            }
                            break;
                        }
                    }
                }


                if (isAvailabilityDownOrDisabledForBar(measurement.getTimestamp())) {
                    sb.append(" \"down\":true ");
                } else if (isAvailabilityUnknownForBar(measurement.getTimestamp())) {
                    sb.append(" \"unknown\":true ");
                } else {
                    if (Double.isNaN(measurement.getValue())) {
                        if (!isAvailabilityDownOrDisabledForBar(measurement.getTimestamp())) {
                            // NaN measure no measurement was collected
                            sb.append(" \"up\":true },");
                        } else {
                            sb.append(" },");
                        }
                    }
                }
                if (!sb.toString().endsWith("},")) {
                    sb.append(" },");
                }
            }
            sb.setLength(sb.length() - 1); // delete the last ','
        }
        sb.append("]");
        Log.debug(sb.toString());
//        if (null != availabilityList) {
//            Log.debug(" avail records loaded: " + availabilityList.size());
//            //@todo: test for start/end timestamps
//            for (Availability availability : availabilityList) {
//                sb.append("{ ");
//                sb.append(" \"availability\":" + "\"" + availability.getAvailabilityType() + "\",");
//                sb.append(" \"startTime\":" + availability.getStartTime() + ", ");
//                sb.append(" \"endTime\":" + availability.getEndTime() + ", ");
//                long availDuration = availability.getEndTime() - availability.getStartTime();
//                String availDurationString = MeasurementConverterClient.format((double) availDuration,
//                    MeasurementUnits.MILLISECONDS, true);
//                sb.append(" \"duration\": \"" + availDurationString + "\"},");
//
//            }
//            sb.setLength(sb.length() - 1); // delete the last ','
//            sb.append("]");
//        }

        return sb.toString();
    }

    private boolean isAvailabilityDownOrDisabledForBar(long timestamp) {
        Date timestampDate = new Date(timestamp);
        if (null != availabilityList) {
            for (Availability availability : availabilityList) {
                boolean downOrDisabled = (availability.getAvailabilityType().equals(AvailabilityType.DOWN) || availability
                        .getAvailabilityType().equals(AvailabilityType.DISABLED));
                if (downOrDisabled
                        && (timestampDate.after(new Date(availability.getStartTime())) && timestampDate.before(new Date(
                        availability.getEndTime())))) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean isAvailabilityUnknownForBar(long timestamp) {
        Date timestampDate = new Date(timestamp);
        if (null != availabilityList) {
            for (Availability availability : availabilityList) {
                if (availability.getAvailabilityType().equals(AvailabilityType.UNKNOWN)
                        && (timestampDate.after(new Date(availability.getStartTime())) && timestampDate.before(new Date(
                        availability.getEndTime())))) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * The magic JSNI to draw the charts with d3.
     */
    public native void drawJsniChart() /*-{
        console.log("Draw Availability Line chart");
        var global = this,
                chartId = global.@org.rhq.enterprise.gui.coregui.client.inventory.common.charttype.AvailabilityLineGraphType::getChartId()(),
                chartHandle = "#availChart-" + chartId,
                chartSelection = chartHandle + " svg",
                json = $wnd.jQuery.parseJSON(global.@org.rhq.enterprise.gui.coregui.client.inventory.common.charttype.AvailabilityLineGraphType::getAvailabilityJson()());
        console.log("Availability chart id: " + chartSelection);
        console.log(" *** JSON: " + json);

        function draw(data) {
            "use strict";

            var margin = {top: 5, right: 5, bottom: 5, left: 40},
                    width = 750 - margin.left - margin.right,
                    height = 20 - margin.top - margin.bottom,
                    interpolation = "basis",

                    timeScale = $wnd.d3.time.scale()
                            .range([0, width])
                            .domain($wnd.d3.extent(data, function (d) {
                                return d.x;
                            })),

            yScale = $wnd.d3.scale.linear()
                    .clamp(true)
                    .rangeRound([height, 0])
                    .domain([0,1]),

            xAxis = $wnd.d3.svg.axis()
                    .scale(timeScale)
                    .ticks(12)
                    .tickSubdivide(5)
                    .tickSize(4, 4, 0)
                    .orient("top"),


            availLine = $wnd.d3.svg.line()
                            .interpolate(interpolation)
                            .x(function (d) {
                                return timeScale(d.x);
                            })
                            .y(function (d) {
                                return yScale(0);
                            }),
            svg = $wnd.d3.select(chartSelection).append("g")
                    .attr("width", width + margin.left + margin.right)
                    .attr("height", height + margin.top + margin.bottom)
                    .attr("transform", "translate(" + margin.left + "," + margin.top + ")");

            svg.append("g")
                    .attr("class", "x axis")
                    .attr("transform", "translate(0," + height + ")")
                    .call(xAxis);

            console.log("finished axes");

            svg.append("path")
                    .datum(data)
                    .attr("class", "availLine")
                    .attr("fill", "none")
                    .attr("stroke", function(d) {
                        if(d.down){
                                return "red";
                        }else if(d.unknown){
                            return "gray";
                        }else {
                            return "green";
                        }

                    })
                    .attr("stroke-width", "2")
                    .attr("d", availLine);

            console.log("finished paths");
        }
        draw(json);

    }-*/;

    public String getChartId() {
        return String.valueOf(entityId);
    }

}
