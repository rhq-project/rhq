/*
 * RHQ Management Platform
 * Copyright (C) 2005-2011 Red Hat, Inc.
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
package org.rhq.enterprise.server.rest;

import java.net.URI;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.interceptor.Interceptors;
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

import org.rhq.core.domain.measurement.DataType;
import org.rhq.core.domain.measurement.MeasurementDefinition;
import org.rhq.core.domain.measurement.MeasurementSchedule;
import org.rhq.core.domain.measurement.composite.MeasurementDataNumericHighLowComposite;
import org.rhq.enterprise.server.resource.ResourceManagerLocal;
import org.rhq.enterprise.server.rest.domain.Link;
import org.rhq.enterprise.server.rest.domain.MetricAggregate;
import org.rhq.enterprise.server.measurement.MeasurementAggregate;
import org.rhq.enterprise.server.measurement.MeasurementDataManagerLocal;
import org.rhq.enterprise.server.measurement.MeasurementScheduleManagerLocal;
import org.rhq.enterprise.server.rest.domain.MetricSchedule;

/**
 * Deal with metrics
 * @author Heiko W. Rupp
 */
@Interceptors(SetCallerInterceptor.class)
@Stateless
public class MetricHandlerBean  extends AbstractRestBean implements MetricHandlerLocal {

    @EJB
    MeasurementDataManagerLocal dataManager;
    @EJB
    MeasurementScheduleManagerLocal scheduleManager;
    @EJB
    ResourceManagerLocal resMgr;

    private static final long EIGHT_HOURS = 8 * 3600L * 1000L;

    @Override
    public Response getMetricData(int scheduleId, long startTime, long endTime,
                                         int dataPoints,boolean hideEmpty,
                                         @Context Request request,
                                         @Context HttpHeaders headers) {

        if (dataPoints<0)
            throw new IllegalArgumentException("dataPoints must be >0 ");

        if (startTime==0) {
            endTime = System.currentTimeMillis();
            startTime = endTime - EIGHT_HOURS;
        }


        MeasurementSchedule schedule = scheduleManager.getScheduleById(caller,scheduleId);
        if (schedule.getDefinition().getDataType()!= DataType.MEASUREMENT)
            throw new IllegalArgumentException("Schedule [" + scheduleId + "] is not a (numerical) metric");

        MeasurementAggregate aggr = dataManager.getAggregate(caller, scheduleId, startTime, endTime);
        MetricAggregate res = new MetricAggregate(scheduleId, aggr.getMin(),aggr.getAvg(),aggr.getMax());

        int definitionId = schedule.getDefinition().getId();
        List<List<MeasurementDataNumericHighLowComposite>> listList = dataManager.findDataForResource(caller,
                schedule.getResource().getId(), new int[]{definitionId}, startTime, endTime, dataPoints);

        long minTime=Long.MAX_VALUE;
        long maxTime=0;

        if (!listList.isEmpty()) {
            List<MeasurementDataNumericHighLowComposite> list = listList.get(0);
            for (MeasurementDataNumericHighLowComposite c : list) {
                long timestamp = c.getTimestamp();
                if (!Double.isNaN(c.getValue()) || !hideEmpty) {
                    MetricAggregate.DataPoint dp = new MetricAggregate.DataPoint(timestamp,c.getValue(),c.getHighValue(),c.getLowValue());
                    res.addDataPoint(dp);
                }
                if (timestamp <minTime)
                    minTime= timestamp;
                if (timestamp >maxTime)
                    maxTime= timestamp;
            }
            res.setNumDataPoints(list.size());
        }
        res.setMaxTimeStamp(maxTime);
        res.setMinTimeStamp(minTime);

        CacheControl cc = new CacheControl();
        int maxAge = (int) (schedule.getInterval() / 1000L)/2; // millis  ; half of schedule interval
        cc.setMaxAge(maxAge); // these are seconds
        cc.setPrivate(false);
        cc.setNoCache(false);


        Response.ResponseBuilder builder;
        MediaType mediaType = headers.getAcceptableMediaTypes().get(0);
        if (mediaType.equals(MediaType.TEXT_HTML_TYPE)) {
            String htmlString = renderTemplate("metricData", res);
            builder = Response.ok(htmlString,mediaType);
        }
        else
            builder= Response.ok(res);
        builder.cacheControl(cc);

        return builder.build();
    }


    /**
     * Return a metric schedule with the respective status codes for cache validation
     *
     *
     * @param scheduleId ID of the schedule
     * @param request the REST request - injected by the REST framework
     * @param headers the REST request http headers - injected by the REST framework
     * @param uriInfo
     * @return Schedule with respective headers
     */
    public Response getSchedule(int scheduleId, Request request, HttpHeaders headers, UriInfo uriInfo) {

        MeasurementSchedule schedule = scheduleManager.getScheduleById(caller, scheduleId);
        if (schedule==null)
            throw new StuffNotFoundException("Schedule with id " + scheduleId);

        MeasurementDefinition definition = schedule.getDefinition();
        MetricSchedule metricSchedule = new MetricSchedule(schedule.getId(), definition.getName(),
                definition.getDisplayName(),
                schedule.isEnabled(), schedule.getInterval(), definition.getUnits().toString(),
                definition.getDataType().toString());
        if (schedule.getMtime()!=null)
            metricSchedule.setMtime(schedule.getMtime());

        // What media type does the user request?
        MediaType mediaType = headers.getAcceptableMediaTypes().get(0);

        // Check for conditional get
        // Interestingly computing the hashCode of the original schedule is slower, as it also
        // pulls in data from the definition and the resource
        EntityTag eTag = new EntityTag(Integer.toOctalString(metricSchedule.hashCode()));
        Response.ResponseBuilder builder = request.evaluatePreconditions(new Date(metricSchedule.getMtime()),eTag);

        if (builder==null) {
            // preconditions not met, we need to send the resource

            // create link to metrics
            UriBuilder uriBuilder = uriInfo.getBaseUriBuilder();
            uriBuilder.path("metric/data/" + scheduleId);
            URI uri = uriBuilder.build();

            Link link = new Link("metrics",uri.toString());
            metricSchedule.addLink(link);

            // Link for updates
            uriBuilder = uriInfo.getAbsolutePathBuilder();
            uri = uriBuilder.build();
            Link updateLink = new Link("edit",uri.toString());
            metricSchedule.addLink(updateLink);


            if (mediaType.equals(MediaType.TEXT_HTML_TYPE)) {
                builder = Response.ok(renderTemplate("metricSchedule", metricSchedule), mediaType);
            }
            else {
                builder = Response.ok(metricSchedule);
            }
        }

        // Create a cache control
        CacheControl cc = new CacheControl();
        cc.setMaxAge(300); // Schedules are valid for 5 mins
        cc.setPrivate(false); // Proxies may cache this

        builder.cacheControl(cc);
        builder.tag(eTag);

        return builder.build();
    }

    @Override
    public List<MetricAggregate> getAggregatesForResource( int resourceId) {

        List<MeasurementSchedule> schedules = scheduleManager.findSchedulesForResourceAndType(caller,
                resourceId, DataType.MEASUREMENT, null,false);
        List<MetricAggregate> ret = new ArrayList<MetricAggregate>(schedules.size());

        long now = System.currentTimeMillis();
        long then = now - EIGHT_HOURS;

        for (MeasurementSchedule schedule: schedules) {
            MeasurementAggregate aggr = dataManager.getAggregate(caller,schedule.getId(),then,now);
            MetricAggregate res = new MetricAggregate(schedule.getId(), aggr.getMin(),aggr.getAvg(),aggr.getMax());
            ret.add(res);
        }
        return ret;

    }

    @Override
    public MetricSchedule updateSchedule(int scheduleId, MetricSchedule in) {
        if (in==null)
            throw new StuffNotFoundException("Input is null"); // TODO other type of exception
        if (in.getScheduleId()==null)
            throw new StuffNotFoundException("Invalid input data");

        MeasurementSchedule schedule = scheduleManager.getScheduleById(caller, scheduleId);
        if (schedule==null)
            throw new StuffNotFoundException("Schedule with id " + scheduleId);

        schedule.setEnabled(in.getEnabled());
        schedule.setInterval(in.getCollectionInterval());

        scheduleManager.updateSchedule(caller, schedule);

        schedule = scheduleManager.getScheduleById(caller,scheduleId);
        MeasurementDefinition def = schedule.getDefinition();

        MetricSchedule ret = new MetricSchedule(scheduleId,def.getName(),def.getDisplayName(),
                schedule.isEnabled(),schedule.getInterval(),def.getUnits().toString(),def.getDataType().toString());

        return ret;
    }
}
