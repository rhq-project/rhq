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
package org.rhq.enterprise.server.rest;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.URI;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.interceptor.Interceptors;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.sql.DataSource;
import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiError;
import com.wordnik.swagger.annotations.ApiErrors;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;

import org.jboss.resteasy.annotations.GZIP;

import org.rhq.core.domain.common.EntityContext;
import org.rhq.core.domain.measurement.DataType;
import org.rhq.core.domain.measurement.MeasurementBaseline;
import org.rhq.core.domain.measurement.MeasurementDataNumeric;
import org.rhq.core.domain.measurement.MeasurementDataPK;
import org.rhq.core.domain.measurement.MeasurementDataTrait;
import org.rhq.core.domain.measurement.MeasurementDefinition;
import org.rhq.core.domain.measurement.MeasurementSchedule;
import org.rhq.core.domain.measurement.composite.MeasurementDataNumericHighLowComposite;
import org.rhq.core.domain.resource.group.ResourceGroup;
import org.rhq.core.util.jdbc.JDBCUtil;
import org.rhq.enterprise.server.RHQConstants;
import org.rhq.enterprise.server.measurement.MeasurementAggregate;
import org.rhq.enterprise.server.measurement.MeasurementDataManagerLocal;
import org.rhq.enterprise.server.measurement.MeasurementDefinitionManagerLocal;
import org.rhq.enterprise.server.measurement.MeasurementScheduleManagerLocal;
import org.rhq.enterprise.server.measurement.util.MeasurementDataManagerUtility;
import org.rhq.enterprise.server.resource.ResourceManagerLocal;
import org.rhq.enterprise.server.resource.group.ResourceGroupManagerLocal;
import org.rhq.enterprise.server.rest.domain.Baseline;
import org.rhq.enterprise.server.rest.domain.Link;
import org.rhq.enterprise.server.rest.domain.MetricAggregate;
import org.rhq.enterprise.server.rest.domain.MetricSchedule;
import org.rhq.enterprise.server.rest.domain.NumericDataPoint;
import org.rhq.enterprise.server.rest.domain.StringValue;

/**
 * Deal with metrics
 * @author Heiko W. Rupp
 */
@Api(value = "Deal with metrics",
        description = "This part of the API deals with exporting metrics")
@Produces({"application/json","application/xml", "text/html"})
@Path("/metric")
@Interceptors(SetCallerInterceptor.class)
@Stateless
@javax.annotation.Resource(name = "RHQ_DS", mappedName = RHQConstants.DATASOURCE_JNDI_NAME)
public class MetricHandlerBean  extends AbstractRestBean  {

    static final String NO_RESOURCE_FOR_ID = "If no resource with the passed id exists";
    static final String NO_SCHEDULE_FOR_ID = "No schedule with the passed id exists";

    @EJB
    MeasurementDataManagerLocal dataManager;
    @EJB
    MeasurementScheduleManagerLocal scheduleManager;
    @EJB
    MeasurementDefinitionManagerLocal definitionManager;
    @EJB
    ResourceManagerLocal resMgr;
    @EJB
    ResourceGroupManagerLocal groupMgr;
    @PersistenceContext(unitName = RHQConstants.PERSISTENCE_UNIT_NAME)
    EntityManager em;

    @javax.annotation.Resource(name = "RHQ_DS")
    private DataSource rhqDs;


    private static final long EIGHT_HOURS = 8 * 3600L * 1000L;

    @GZIP
    @GET
    @Path("data/{scheduleId}")
    @Produces({MediaType.APPLICATION_JSON,MediaType.APPLICATION_XML,MediaType.TEXT_HTML})
    @ApiOperation(value = "Get the bucketized metric values for the schedule ")
    @ApiError(code = 404, reason = NO_SCHEDULE_FOR_ID)
    public Response getMetricData(
            @ApiParam("Schedule Id of the values to query") @PathParam("scheduleId") int scheduleId,
            @ApiParam(value = "Start time since epoch.", defaultValue = "End time - 8h") @QueryParam(
                    "startTime") long startTime,
            @ApiParam(value = "End time since epoch.", defaultValue = "Now") @QueryParam("endTime") long endTime,
            @ApiParam("Number of buckets - currently fixed at 60") @QueryParam("dataPoints") @DefaultValue(
                    "60") int dataPoints,
            @ApiParam(value = "Hide rows that are NaN only", defaultValue = "false") @QueryParam(
                    "hideEmpty") boolean hideEmpty,
            @Context HttpHeaders headers) {

        if (dataPoints<=0)
            throw new IllegalArgumentException("dataPoints must be >0 ");

        if (startTime==0) {
            endTime = System.currentTimeMillis();
            startTime = endTime - EIGHT_HOURS;
        }

        MediaType mediaType = headers.getAcceptableMediaTypes().get(0);
        boolean isHtml = mediaType.equals(MediaType.TEXT_HTML_TYPE);

        MeasurementSchedule schedule = obtainSchedule(scheduleId, false, DataType.MEASUREMENT);

        MeasurementAggregate aggr = dataManager.getAggregate(caller, scheduleId, startTime, endTime);
        MetricAggregate res = new MetricAggregate(scheduleId, aggr.getMin(),aggr.getAvg(),aggr.getMax());

        int definitionId = schedule.getDefinition().getId();
        List<List<MeasurementDataNumericHighLowComposite>> listList = dataManager.findDataForResource(caller,
                schedule.getResource().getId(), new int[]{definitionId}, startTime, endTime, dataPoints);

        if (!listList.isEmpty()) {
            List<MeasurementDataNumericHighLowComposite> list = listList.get(0);
            fill(res, list,scheduleId,hideEmpty, isHtml);
        }

        CacheControl cc = new CacheControl();
        int maxAge = (int) (schedule.getInterval() / 1000L)/2; // millis  ; half of schedule interval
        cc.setMaxAge(maxAge); // these are seconds
        cc.setPrivate(false);
        cc.setNoCache(false);

        Response.ResponseBuilder builder;
        if (isHtml) {
            String htmlString = renderTemplate("metricData", res);
            builder = Response.ok(htmlString,mediaType);
        }
        else
            builder= Response.ok(res,mediaType);
        builder.cacheControl(cc);

        return builder.build();
    }
    @GZIP
    @GET
    @Path("data/group/{groupId}/{definitionId}")
    @ApiOperation(value = "Get the bucketized metric values for the metric definition of the group ")
    @Produces({MediaType.APPLICATION_JSON,MediaType.APPLICATION_XML,MediaType.TEXT_HTML})
    public Response getMetricDataForGroupAndDefinition(
            @ApiParam("Id of the group to query") @PathParam("groupId") int groupId,
            @ApiParam("Id of the metric definition to retrieve") @PathParam("definitionId") int definitionId,
            @ApiParam(value = "Start time since epoch.", defaultValue = "End time - 8h") @QueryParam(
                    "startTime") long startTime,
            @ApiParam(value = "End time since epoch.", defaultValue = "Now") @QueryParam("endTime") long endTime,
            @ApiParam("Number of buckets - currently fixed at 60") @QueryParam("dataPoints") @DefaultValue(
                    "60") int dataPoints,
            @ApiParam(value = "Hide rows that are NaN only", defaultValue = "false") @QueryParam(
                    "hideEmpty") boolean hideEmpty,
            @Context HttpHeaders headers) {

        if (startTime==0) {
            endTime = System.currentTimeMillis();
            startTime = endTime - EIGHT_HOURS;
        }

        if (dataPoints<1)
            throw new IllegalArgumentException("datapoints must be >=0");

        MediaType mediaType = headers.getAcceptableMediaTypes().get(0);
        boolean isHtml = mediaType.equals(MediaType.TEXT_HTML_TYPE);

        fetchGroup(groupId,true); // Make sure the group exists and is compatible
        MeasurementDefinition definition = definitionManager.getMeasurementDefinition(caller,definitionId);
        if (definition==null) {
            throw new StuffNotFoundException("There is no definition with id " + definitionId);
        }

        MeasurementAggregate aggr = dataManager.getAggregate(caller, groupId, definitionId, startTime, endTime);
        MetricAggregate res = new MetricAggregate(definitionId, aggr.getMin(),aggr.getAvg(),aggr.getMax());
        res.setGroup(true);

        List<List<MeasurementDataNumericHighLowComposite>> listList = dataManager.findDataForCompatibleGroup(caller,
                groupId,definitionId,startTime,endTime,dataPoints);

        if (listList.isEmpty()) {
            throw new StuffNotFoundException("Data for group with id " + groupId + " and definition " + definitionId);
        }
        List<MeasurementDataNumericHighLowComposite> list = listList.get(0);
        if (!listList.isEmpty()) {
            fill(res, list,definitionId,hideEmpty,isHtml);
        }

        CacheControl cc = new CacheControl();
        int maxAge = (int) (definition.getDefaultInterval() / 1000L)/2; // millis  ; half of schedule interval
        cc.setMaxAge(maxAge); // these are seconds
        cc.setPrivate(false);
        cc.setNoCache(false);

        Response.ResponseBuilder builder;
        if (isHtml) {
            String htmlString = renderTemplate("metricData", res);
            builder = Response.ok(htmlString,mediaType);
        }
        else
            builder= Response.ok(res,mediaType);
        builder.cacheControl(cc);

        return builder.build();
    }

    /**
     * Get the schedule for the passed schedule id
     *
     * @param scheduleId id to look up
     * @param force
     * @param type
     * @return schedule
     * @throws StuffNotFoundException if there is no schedule with the passed id
     */
    private MeasurementSchedule obtainSchedule(int scheduleId, boolean force, DataType type) {
        MeasurementSchedule schedule=null;
        if(!force)
            schedule = getFromCache(scheduleId,MeasurementSchedule.class);
        if (schedule==null) {
            schedule = scheduleManager.getScheduleById(caller,scheduleId);
            if (schedule==null) {
                throw new StuffNotFoundException("Schedule with id " + scheduleId);
            }
            else
                putToCache(scheduleId,MeasurementSchedule.class,schedule);
        }

        if (schedule.getDefinition().getDataType()!= type)
            throw new IllegalArgumentException("Schedule [" + scheduleId + "] is not a ("+ type + ") metric");
        return schedule;
    }
    private MetricAggregate fill(MetricAggregate res, List<MeasurementDataNumericHighLowComposite> list, int scheduleId,
                                 boolean hideEmpty, boolean isHtmlOutput) {
        long minTime=Long.MAX_VALUE;
        long maxTime=0;
        res.setScheduleId(scheduleId);

        for (MeasurementDataNumericHighLowComposite c : list) {
            long timestamp = c.getTimestamp();
            if (Double.isNaN(c.getValue()) && hideEmpty)
                continue;

            MetricAggregate.DataPoint dp;
            if (isHtmlOutput) {
                dp = new MetricAggregate.DataPoint(timestamp);

                Double v = c.getLowValue();
                v= nullifyIfNaN(v);
                dp.setLow(v);
                v = c.getHighValue();
                v =nullifyIfNaN(v);
                dp.setHigh(v);
                v = c.getValue();
                v= nullifyIfNaN(v);
                dp.setValue(v);

            } else {
                dp = new MetricAggregate.DataPoint(timestamp,c.getValue(),c.getHighValue(),c.getLowValue());
            }
            res.addDataPoint(dp);

            if (timestamp <minTime)
                minTime= timestamp;
            if (timestamp >maxTime)
                maxTime= timestamp;
        }
        res.setNumDataPoints(list.size());
        res.setMaxTimeStamp(maxTime);
        res.setMinTimeStamp(minTime);

        return res;
    }

    private Double nullifyIfNaN(Double v) {
        if (Double.isNaN(v))
            v =null;
        return v;
    }

    @GZIP
    @GET
    @Path("data")
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_HTML})
    public Response getMetricDataMulti(@QueryParam("sid") String schedules, @QueryParam("startTime") long startTime,
                                       @QueryParam("endTime") long endTime, @QueryParam("dataPoints") int dataPoints,
                                       @QueryParam("hideEmpty") boolean hideEmpty,
                                       @Context HttpHeaders headers) {

        MediaType mediaType = headers.getAcceptableMediaTypes().get(0);

        long now = System.currentTimeMillis();
        if (endTime==0)
            endTime = now;

        if (startTime==0) {
            endTime = System.currentTimeMillis();
            startTime = endTime - EIGHT_HOURS;
        }

        String[] tmp = schedules.split(",");
        Integer[] scheduleIds = new Integer[tmp.length];
        try {
            for (int i = 0; i < tmp.length ; i++)
                scheduleIds[i] = Integer.parseInt(tmp[i]);
        }
        catch (NumberFormatException nfe) {
            throw new IllegalArgumentException("Bad input: " + nfe.getMessage());
        }

        List<MetricAggregate> resList = new ArrayList<MetricAggregate>(scheduleIds.length);
        for (Integer scheduleId : scheduleIds) {
            MeasurementSchedule sched = scheduleManager.getScheduleById(caller,scheduleId);
            if (sched==null)
                throw new StuffNotFoundException("Schedule with id " + scheduleId);
            int definitionId = sched.getDefinition().getId();
            List<List<MeasurementDataNumericHighLowComposite>> listList =
                dataManager.findDataForContext(caller, EntityContext.forResource(sched.getResource().getId()),definitionId,startTime,endTime,dataPoints);
            if (!listList.isEmpty()) {
                List<MeasurementDataNumericHighLowComposite> list = listList.get(0);
                MetricAggregate res = new MetricAggregate();
                boolean isHtml = mediaType.equals(MediaType.TEXT_HTML_TYPE);
                fill(res, list,scheduleId,hideEmpty, isHtml);
                resList.add(res);
            }
            else
                throw new StuffNotFoundException("Metrics for schedule " + scheduleId);
        }

        GenericEntity<List<MetricAggregate>> metAgg = new GenericEntity<List<MetricAggregate>>(resList) {};

        return Response.ok(metAgg,mediaType).build();

    }

    /**
     * Return a metric schedule with the respective status codes for cache validation
     *
     *
     * @param scheduleId ID of the schedule
     * @param request the REST request - injected by the REST framework
     * @param headers the REST request http headers - injected by the REST framework
     * @param uriInfo info about the called uri to build links
     * @return Schedule with respective headers
     */
    @GET
    @Path("/schedule/{id}")
    @Produces({MediaType.APPLICATION_JSON,MediaType.APPLICATION_XML,MediaType.TEXT_HTML})
    @ApiOperation("Get the metric schedule for the passed id")
    @ApiError(code = 404, reason = NO_SCHEDULE_FOR_ID)
    public Response getSchedule(@ApiParam("Schedule Id") @PathParam("id") int scheduleId,
                             @Context Request request, @Context HttpHeaders headers,
                             @Context UriInfo uriInfo) {

        MediaType mediaType = headers.getAcceptableMediaTypes().get(0);

        MeasurementSchedule schedule;
        Response.ResponseBuilder builder;

        // Create a cache control
        CacheControl cc = new CacheControl();
        cc.setMaxAge(300); // Schedules are valid for 5 mins
        cc.setPrivate(false); // Proxies may cache this

        schedule = getFromCache(scheduleId, MeasurementSchedule.class);
        if (schedule!=null) {
                // If it is on cache, quickly return if match
            long tim = schedule.getMtime() != null ? schedule.getMtime() : 0;
            EntityTag eTag = new EntityTag(Long.toOctalString(schedule.hashCode()+tim)); // factor in mtime in etag
            builder = request.evaluatePreconditions(new Date(tim),eTag);

            if (builder!=null) {
                builder.cacheControl(cc);
                return builder.build();
            }
        }

        if (schedule==null) {
            schedule = scheduleManager.getScheduleById(caller, scheduleId);
            if (schedule==null)
                throw new StuffNotFoundException("Schedule with id " + scheduleId);
            else
                putToCache(scheduleId, MeasurementSchedule.class, schedule);
        }

        MeasurementDefinition definition = schedule.getDefinition();
        MetricSchedule metricSchedule = new MetricSchedule(schedule.getId(), definition.getName(),
                definition.getDisplayName(),
                schedule.isEnabled(), schedule.getInterval(), definition.getUnits().toString(),
                definition.getDataType().toString());
        if (schedule.getMtime()!=null)
            metricSchedule.setMtime(schedule.getMtime());


        // Check for conditional get again
        // Interestingly computing the hashCode of the original schedule is slower, as it also
        // pulls in data from the definition and the resource
        long tim = schedule.getMtime() != null ? schedule.getMtime() : 0;
        EntityTag eTag = new EntityTag(Long.toOctalString(schedule.hashCode()+tim));
        builder = request.evaluatePreconditions(new Date(tim),eTag); // factor in mtime in etag


        if (builder==null) {
            // preconditions not met, we need to send the resource

            UriBuilder uriBuilder;
            URI uri;
            Link link;
            if (definition.getDataType()==DataType.MEASUREMENT) {
                // create link to metrics
                uriBuilder = uriInfo.getBaseUriBuilder();
                uriBuilder.path("metric/data/" + scheduleId);
                uri = uriBuilder.build();
                link = new Link("metric",uri.toString());
                metricSchedule.addLink(link);
            }

            // create link to the resource
            uriBuilder = uriInfo.getBaseUriBuilder();
            uriBuilder.path("resource/" + schedule.getResource().getId());
            uri = uriBuilder.build();
            link = new Link("resource",uri.toString());
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
                builder = Response.ok(metricSchedule,mediaType);
            }
        }


        builder.cacheControl(cc);
        builder.tag(eTag);

        return builder.build();
    }

    @GZIP
    @GET
    @Path("data/resource/{resourceId}")
    @ApiOperation("Retrieve a list of high/low/average/data aggregate for the resource")
    @ApiError(code = 404, reason = NO_RESOURCE_FOR_ID)
    public List<MetricAggregate> getAggregatesForResource(
            @ApiParam("Id of the resource to query") @PathParam("resourceId") int resourceId,
            @ApiParam(value = "Start time since epoch.", defaultValue="End time - 8h") @QueryParam("startTime") long startTime,
            @ApiParam(value = "End time since epoch.", defaultValue = "Now") @QueryParam("endTime") long endTime) {

        long now = System.currentTimeMillis();
        if (endTime==0)
            endTime = now;
        if (startTime==0) {
            startTime = endTime - EIGHT_HOURS;
        }


        List<MeasurementSchedule> schedules = scheduleManager.findSchedulesForResourceAndType(caller,
                resourceId, DataType.MEASUREMENT, null,false);
        for (MeasurementSchedule sched: schedules) {
            putToCache(sched.getId(),MeasurementSchedule.class,sched);
        }
        List<MetricAggregate> ret = new ArrayList<MetricAggregate>(schedules.size());

        for (MeasurementSchedule schedule: schedules) {
            MeasurementAggregate aggr = dataManager.getAggregate(caller,schedule.getId(),startTime,endTime);
            MetricAggregate res = new MetricAggregate(schedule.getId(), aggr.getMin(),aggr.getAvg(),aggr.getMax());
            ret.add(res);
        }
        return ret;

    }

    @GZIP
    @GET
    @Path("data/group/{groupId}")
    @ApiOperation("Retrieve a list of high/low/average/data aggregate for the group")
    public List<MetricAggregate> getAggregatesForGroup(
            @ApiParam("Id of the group to query") @PathParam("groupId") int groupId,
            @ApiParam(value = "Start time since epoch.", defaultValue="End time - 8h") @QueryParam("startTime") long startTime,
            @ApiParam(value = "End time since epoch.", defaultValue = "Now") @QueryParam("endTime") long endTime) {
        long now = System.currentTimeMillis();
        if (endTime==0)
            endTime = now;
        if (startTime==0) {
            startTime = endTime - EIGHT_HOURS;
        }
        ResourceGroup group = fetchGroup(groupId, true);

        Set<MeasurementDefinition> definitions = group.getResourceType().getMetricDefinitions();

        List<MetricAggregate> ret = new ArrayList<MetricAggregate>(definitions.size());
        for (MeasurementDefinition def : definitions) {
            if (def.getDataType()==DataType.MEASUREMENT) {
                MeasurementAggregate aggregate = dataManager.getAggregate(caller, groupId, def.getId(), startTime, endTime);
                MetricAggregate res = new MetricAggregate(def.getId(), aggregate.getMin(),aggregate.getAvg(),aggregate.getMax());
                res.setGroup(true);
                ret.add(res);
            }
        }
        return ret;
    }

    @PUT
    @Path("/schedule/{id}")
    @Consumes({MediaType.APPLICATION_JSON,MediaType.APPLICATION_XML})
    @Produces({MediaType.APPLICATION_JSON,MediaType.APPLICATION_XML})
    @ApiOperation(value = "Update the schedule (enabled, interval) ", responseClass = "MetricSchedule")
    @ApiError(code = 404, reason = NO_SCHEDULE_FOR_ID)
    public Response updateSchedule(@ApiParam("Id of the schedule to query") @PathParam("id") int scheduleId,
                                @ApiParam(value = "New schedule data", required = true) MetricSchedule in,
                                @Context HttpHeaders headers) {
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
        putToCache(scheduleId, MeasurementSchedule.class, schedule);
        MeasurementDefinition def = schedule.getDefinition();

        MetricSchedule ret = new MetricSchedule(scheduleId,def.getName(),def.getDisplayName(),
                schedule.isEnabled(),schedule.getInterval(),def.getUnits().toString(),def.getDataType().toString());

        return Response.ok(ret,headers.getAcceptableMediaTypes().get(0)).build();
    }

    @GZIP
    @ApiOperation(value = "Expose the raw metrics of a single schedule. This can only expose raw data, which means the start date may "
        + "not be older than 7 days.")
    @GET
    @Path("data/{scheduleId}/raw")
    @Produces({MediaType.APPLICATION_JSON,MediaType.APPLICATION_XML,"text/csv",MediaType.TEXT_HTML})
    @ApiErrors({
        @ApiError(code = 404, reason = NO_SCHEDULE_FOR_ID)
    })
    public StreamingOutput getMetricDataRaw(@ApiParam(required = true) @PathParam("scheduleId") int scheduleId,
                                            @ApiParam(value = "Start time since epoch",
                                                    defaultValue = "Now - 8h") @QueryParam("startTime") long startTime,
                                            @ApiParam(value = "End time since epoch", defaultValue = "Now") @QueryParam(
                                                    "endTime") long endTime,
                                            @ApiParam(defaultValue = "8h = 28800000ms",
                                                    value = "Timespan in ms") @QueryParam("duration") long duration,
                                            @Context HttpHeaders headers) {

        MediaType mediaType = headers.getAcceptableMediaTypes().get(0);

        long now = System.currentTimeMillis();
        if (endTime==0)
            endTime = now;
        if (startTime==0)
            startTime = endTime - EIGHT_HOURS;
        if (duration>0) // overrides start time
            startTime = endTime - duration*1000L; // duration is in seconds

        if (startTime < now -7L*86400*1000)
            throw new IllegalArgumentException("Start time is older than 7 days");

        // Check if the schedule exists
        obtainSchedule(scheduleId, false, DataType.MEASUREMENT);

        RawNumericStreamingOutput so = new RawNumericStreamingOutput();
        so.scheduleId = scheduleId;
        so.startTime = startTime;
        so.endTime = endTime;
        so.mediaType = mediaType;
        so.now = now-1; // pass this so that the for the 7days case is still handled from raw tables.

        return so;
    }

    @PUT
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    @Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    @ApiOperation("Submit a single (numerical) metric to the server")
    @ApiError(code=404, reason = NO_SCHEDULE_FOR_ID)
    @Path("data/{scheduleId}/raw/{timeStamp}")
    public Response putMetricValue(@ApiParam("Id of the schedule") @PathParam("scheduleId") int scheduleId,
                                @ApiParam("Timestamp of the metric") @PathParam("timeStamp") long timestamp,
                                @ApiParam(value = "Data point", required = true) NumericDataPoint point,
                                @Context HttpHeaders headers,
                                @Context UriInfo uriInfo) {

        MediaType mediaType = headers.getAcceptableMediaTypes().get(0);
        MeasurementSchedule schedule = obtainSchedule(scheduleId, false, DataType.MEASUREMENT);

        Set<MeasurementDataNumeric> data = new HashSet<MeasurementDataNumeric>(1);
        data.add(new MeasurementDataNumeric(point.getTimeStamp(),scheduleId,point.getValue()));

        dataManager.addNumericData(data);

        UriBuilder uriBuilder = uriInfo.getBaseUriBuilder();
        uriBuilder.path("/metric/data/{scheduleId}/raw");
        uriBuilder.queryParam("startTime",timestamp);
        uriBuilder.queryParam("endTime",timestamp);
        URI uri = uriBuilder.build(scheduleId);

        return Response.created(uri).type(mediaType).build();
    }

    @PUT
    @Path("data/{scheduleId}/trait")
    @Consumes({MediaType.APPLICATION_JSON,MediaType.APPLICATION_XML})
    @ApiOperation(value = "Submit a new trait value for the passed schedule id")
    @ApiError(code = 404, reason = NO_SCHEDULE_FOR_ID)
    public Response putTraitValue(@ApiParam("Id of the schedule") @PathParam("scheduleId") int scheduleId, StringValue value) {
        MeasurementSchedule schedule = obtainSchedule(scheduleId, false, DataType.TRAIT);

        Set<MeasurementDataTrait> traits = new HashSet<MeasurementDataTrait>(1);
        MeasurementDataPK pk = new MeasurementDataPK(System.currentTimeMillis(),scheduleId);
        traits.add(new MeasurementDataTrait(pk,value.getValue()));

        dataManager.addTraitData(traits);

        return Response.ok().build();
    }

    @GET
    @Path("data/{scheduleId}/trait")
    @Produces({MediaType.APPLICATION_JSON,MediaType.APPLICATION_XML})
    @ApiOperation(value="Get the current value of the trait with the passed schedule id", responseClass = "StringValue")
    @ApiError(code = 404, reason = NO_SCHEDULE_FOR_ID)
    public Response getTraitValue(@ApiParam("Id of the schedule") @PathParam("scheduleId") int scheduleId) {

        MeasurementSchedule schedule = obtainSchedule(scheduleId, false, DataType.TRAIT);
        List<MeasurementDataTrait> traits = dataManager.findTraits(caller,schedule.getResource().getId(),schedule.getDefinition().getId());
        Response.ResponseBuilder builder;
        if (traits!=null && traits.size()>0) {
            builder = Response.ok();
            StringValue value = new StringValue(traits.get(0).getValue());
            builder.entity(value);

        } else {
            builder = Response.status(Response.Status.NOT_FOUND);
        }

        return builder.build();
    }


    @POST
    @Path("data/raw")
    @Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    @ApiOperation(value="Submit a series of (numerical) metric values to the server",responseClass = "No response")
    public Response postMetricValues(Collection<NumericDataPoint> points, @Context HttpHeaders headers) {

        MediaType mediaType = headers.getAcceptableMediaTypes().get(0);
        Set<MeasurementDataNumeric> data = new HashSet<MeasurementDataNumeric>(points.size());
        for (NumericDataPoint point : points) {
            data.add(new MeasurementDataNumeric(point.getTimeStamp(), point.getScheduleId(),point.getValue()));
        }

        dataManager.addNumericData(data);

        return Response.noContent().type(mediaType).build();

    }

    @GET
    @Path("data/{scheduleId}/baseline")
    @Produces({MediaType.APPLICATION_JSON,MediaType.APPLICATION_XML})
    @ApiOperation(value = "Get the current baseline for the schedule")
    @ApiError(code = 404, reason = NO_SCHEDULE_FOR_ID)
    public Baseline getBaseline(@ApiParam("Id of the schedule") @PathParam("scheduleId") int scheduleId,
                                @Context HttpHeaders headers,
                                @Context UriInfo uriInfo) {
        MeasurementSchedule schedule = obtainSchedule(scheduleId, true, DataType.MEASUREMENT);
        MeasurementBaseline mBase = schedule.getBaseline();

        Baseline b;
        if (mBase==null)
            throw new StuffNotFoundException("Baseline for schedule [" + scheduleId +"]");
        else
            b = new Baseline(mBase.getMin(),mBase.getMax(),mBase.getMean(),mBase.getComputeTime().getTime());
        return b;

    }

    @PUT
    @Path("data/{scheduleId}/baseline")
    @Consumes({MediaType.APPLICATION_JSON,MediaType.APPLICATION_XML})
    @ApiOperation(value = "Set a new baseline for the schedule")
    @ApiErrors({
        @ApiError(code = 404, reason = NO_SCHEDULE_FOR_ID),
        @ApiError(code = 406 ,reason = "Baseline data is incorrect")
    })
    public Response setBaseline(@ApiParam("Id of the schedule")  @PathParam("scheduleId") int scheduleId,
                                Baseline baseline, @Context HttpHeaders headers, @Context UriInfo uriInfo) {
        MeasurementSchedule schedule = obtainSchedule(scheduleId, false, DataType.MEASUREMENT);

        // little bit of sanity checking
        if (baseline.getMin()>baseline.getMean() || baseline.getMean()>baseline.getMax() || baseline.getMin()>baseline.getMax()) {
            Response.ResponseBuilder builder = Response.status(Response.Status.NOT_ACCEPTABLE);
            builder.entity("Baseline not correct. it should be min<=mean<=max");
            return builder.build();
        }

        MeasurementBaseline mBase = schedule.getBaseline();
        if (mBase == null) {
            mBase = new MeasurementBaseline();
            mBase.setSchedule(schedule);
            schedule.setBaseline(mBase);
            em.persist(mBase);
        }
        mBase.setMax(baseline.getMax());
        mBase.setMin(baseline.getMin());
        mBase.setMean(baseline.getMean());
        mBase.setUserEntered(true);

        scheduleManager.updateSchedule(caller,schedule);

        return Response.created(uriInfo.getRequestUriBuilder().build()).build();

    }

    /**
     * Write the numeric data points to the output stream in JSON encoding
     * without creating tons of objects in the middle to have them marshalled
     * by JAX-RS
     */
    private class RawNumericStreamingOutput implements StreamingOutput {

        int scheduleId;
        long startTime;
        long endTime;
        long now;
        MediaType mediaType;

        @Override
        public void write(OutputStream outputStream) throws IOException, WebApplicationException {

            String[] tables = MeasurementDataManagerUtility.getTables(startTime,endTime,now);
            StringBuilder sb = new StringBuilder();
            for (int i = 0 ; i < tables.length ; i ++) {
                sb.append("SELECT time_stamp,value FROM ");
                sb.append(tables[i]);
                sb.append(" WHERE schedule_id = ? AND time_stamp BETWEEN ? AND ?");
                if (i < tables.length-1)
                    sb.append(" UNION ALL ");
            }

            sb.append(" ORDER BY time_stamp ASC");



            Connection connection = null;
            PreparedStatement ps = null;
            ResultSet rs = null;
            try {
                connection = rhqDs.getConnection();
                ps = connection.prepareStatement( sb.toString() );
                for (int i = 0; i < tables.length ; i++) {
                    ps.setInt(i * 3 + 1, scheduleId);
                    ps.setLong(i*3+2,startTime);
                    ps.setLong(i*3+3,endTime);
                }
                rs = ps.executeQuery();

                PrintWriter pw = new PrintWriter(outputStream);

                if (mediaType.equals(MediaType.APPLICATION_JSON_TYPE)) {
                    boolean needsComma = false;
                    pw.println("[");
                    while (rs.next()) {
                        if (needsComma) {
                            pw.print(",\n");
                        }
                        needsComma = true;
                        pw.print("{");
                        pw.print("\"scheduleId\":");
                        pw.print(scheduleId);
                        pw.print(", ");
                        pw.print("\"timeStamp\":");
                        pw.print(rs.getLong(1));
                        pw.print(", ");
                        pw.print("\"value\":");
                        pw.print(rs.getDouble(2));
                        pw.print("}");
                    }
                    pw.println("]");
                }
                else if (mediaType.equals(MediaType.APPLICATION_XML_TYPE)) {
                    pw.println("<collection>");
                    while(rs.next()) {
                        pw.print("  <numericDataPoint scheduleId=\"");
                        pw.print(scheduleId);
                        pw.print("\" timeStamp=\"");
                        pw.print(rs.getLong(1));
                        pw.print("\" value=\"");
                        pw.print(rs.getDouble(2));
                        pw.println("\"/>");
                    }
                    pw.println("</collection>");
                }
                else if (mediaType.toString().equals("text/csv")) {
                    pw.println("#schedule,timestamp,value");
                    while (rs.next()) {
                        pw.print(scheduleId);
                        pw.print(',');
                        pw.print(rs.getLong(1));
                        pw.print(',');
                        pw.println(rs.getDouble(2));
                    }
                }
                else if (mediaType.equals(MediaType.TEXT_HTML_TYPE)) {
                    pw.println("<table>");
                    pw.print("<tr><th>time</th><th>value</th></tr>\n");
                    while (rs.next()) {
                        pw.print("  <tr>");
                        pw.print("<td>");
                        pw.print(new Date(rs.getLong(1)));
                        pw.print("</td><td>");
                        pw.print(rs.getDouble(2));
                        pw.print("</td>");
                        pw.println("</tr>");
                    }
                    pw.println("</table>");

                }
                pw.flush();
                pw.close();
            } catch (SQLException e) {
                log.error(e);
            } finally {
                JDBCUtil.safeClose(connection, ps, rs);
            }
        }
    }
}
