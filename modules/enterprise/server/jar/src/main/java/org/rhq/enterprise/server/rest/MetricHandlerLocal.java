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

import java.util.Collection;
import java.util.List;

import javax.ejb.Local;
import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.PUT;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import javax.ws.rs.core.UriInfo;

import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiError;
import com.wordnik.swagger.annotations.ApiErrors;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;

import org.rhq.enterprise.server.rest.domain.Baseline;
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
@Local
public interface MetricHandlerLocal {

    static String NO_RESOURCE_FOR_ID = "If no resource with the passed id exists";
    static String NO_SCHEDULE_FOR_ID = "No schedule with the passed id exists";

    @GET
    @Path("data/{scheduleId}")
    @Produces({MediaType.APPLICATION_JSON,MediaType.APPLICATION_XML,MediaType.TEXT_HTML})
    @ApiOperation(value = "Get the bucketized metric values for the schedule ")
    @ApiError(code = 404, reason = NO_SCHEDULE_FOR_ID)
    Response getMetricData(@ApiParam("Schedule Id of the values to query") @PathParam("scheduleId") int scheduleId,
                           @ApiParam(value="Start time since epoch.", defaultValue = "End time - 8h") @QueryParam("startTime")  long startTime,
                           @ApiParam(value="End time since epoch.", defaultValue = "Now") @QueryParam("endTime") long endTime,
                           @ApiParam("Number of buckets - currently fixed at 60") @QueryParam("dataPoints") @DefaultValue("60") int dataPoints,
                                  @QueryParam("hideEmpty") boolean hideEmpty,
                                  @Context Request request,
                                  @Context HttpHeaders headers);

    @GET
    @Path("data")
    @Produces({MediaType.APPLICATION_JSON,MediaType.APPLICATION_XML,MediaType.TEXT_HTML})
    Response getMetricDataMulti(@QueryParam("sid") String scheduleIds,
                                  @QueryParam("startTime")  long startTime,
                                  @QueryParam("endTime") long endTime,
                                  @QueryParam("dataPoints") int dataPoints,
                                  @QueryParam("hideEmpty") boolean hideEmpty,
                                  @Context Request request,
                                  @Context HttpHeaders headers);


    @GET
    @Path("data/resource/{resourceId}")
    @ApiOperation("Retrieve a list of high/low/average/data aggregate for the resource")
    @ApiError(code = 404, reason = NO_RESOURCE_FOR_ID)
    List<MetricAggregate> getAggregatesForResource(
            @ApiParam("Resource to query") @PathParam("resourceId") int resourceId,
            @ApiParam(value = "Start time since epoch.", defaultValue="End time - 8h") @QueryParam("startTime") long startTime,
            @ApiParam(value = "End time since epoch.", defaultValue = "Now") @QueryParam("endTime") long endTime);

    /**
     * Get information about the schedule
     * @param scheduleId id of the schedule
     * @param request Injected request
     * @param headers Injected http headers
     * @param uriInfo Injected Uri
     * @return the schedule
     */
    @GET
    @Path("/schedule/{id}")
    @Produces({MediaType.APPLICATION_JSON,MediaType.APPLICATION_XML,MediaType.TEXT_HTML})
    @ApiOperation("Get the metric schedule for the passed id")
    @ApiError(code = 404, reason = NO_SCHEDULE_FOR_ID)
    Response getSchedule(@ApiParam("Schedule Id") @PathParam("id") int scheduleId,
                         @Context Request request, @Context HttpHeaders headers,
                         @Context UriInfo uriInfo);

    /**
     * Update a schedule. Currently change of collection interval and enabled/disabled state are supported.
     * @param scheduleId Id of the schedule to update
     * @param in Modified schedule object
     * @param headers Injected http headers
     * @return  Result of updating
     */
    @PUT
    @Path("/schedule/{id}")
    @Consumes({MediaType.APPLICATION_JSON,MediaType.APPLICATION_XML})
    @Produces({MediaType.APPLICATION_JSON,MediaType.APPLICATION_XML})
    @ApiOperation(value = "Update the schedule (enabled, interval) ", responseClass = "MetricSchedule")
    @ApiError(code = 404, reason = NO_SCHEDULE_FOR_ID)
    Response updateSchedule(@ApiParam("Id of the schedule to query") @PathParam("id") int scheduleId,
                            @ApiParam(value = "New schedule data", required = true) MetricSchedule in,
                            @Context HttpHeaders headers);

    /**
     * Expose the raw metrics for the given schedule
     * @param scheduleId Schedule id
     * @param startTime Start time, if 0 and duration=, start time = 8h before endTime
     * @param endTime End time. If 0, now is used
     * @param duration Duration in seconds. If duration=0, startTime is used
     * @param request Injected Request headers
     * @param headers Injected HttpHeaders
     * @return an encoded stream of numerical values
     */
    @ApiOperation(value = "Expose the raw metrics of a single schedule. This can only expose raw data, which means the start date may "
        + "not be older than 7 days.")
    @GET
    @Path("data/{scheduleId}/raw")
    @Produces({MediaType.APPLICATION_JSON,MediaType.APPLICATION_XML,"text/csv",MediaType.TEXT_HTML})
    @ApiErrors({
        @ApiError(code = 404, reason = NO_SCHEDULE_FOR_ID)
    })
    StreamingOutput getMetricDataRaw(@ApiParam(required = true) @PathParam("scheduleId") int scheduleId,
                                     @ApiParam(value="Start time since epoch", defaultValue = "Now - 8h") @QueryParam("startTime") long startTime,
                                     @ApiParam(value="End time since epoch", defaultValue = "Now") @QueryParam("endTime") long endTime,
                                     @ApiParam(defaultValue = "8h = 28800000ms", value = "Timespan in ms") @QueryParam("duration") long duration,
                                     @Context Request request,
                                     @Context HttpHeaders headers);

    /**
     * Submit a single (numerical) metric value to the server.
     * @param scheduleId Id of the schedule to submit to
     * @param timestamp Timestamp of the entry
     * @param point Datapoint of class NumericDataPoint
     * @param headers Injected HTTP headers
     * @param uriInfo Injected info about the uri
     * @return
     */
    @PUT
    @Produces({MediaType.APPLICATION_JSON,MediaType.APPLICATION_XML})
    @Consumes({MediaType.APPLICATION_JSON,MediaType.APPLICATION_XML})
    @Path("data/{scheduleId}/raw/{timeStamp}")
    @ApiOperation("Submit a single (numerical) metric to the server")
    @ApiError(code=404, reason = NO_SCHEDULE_FOR_ID)
    Response putMetricValue(@ApiParam("Id of the schedule") @PathParam("scheduleId") int scheduleId,
                            @ApiParam("Timestamp of the metric") @PathParam("timeStamp") long timestamp,
                            @ApiParam(value = "Data point", required = true) NumericDataPoint point,
                            @Context HttpHeaders headers,
                            @Context UriInfo uriInfo);

    /**
     * Submit a series of (numerical) metric values to the server
     * @param points Collection of NumericDataPoint entries
     * @param headers Injected HTTP headers
     * @return response object
     */
    @POST
    @Path("data/raw")
    @Consumes({MediaType.APPLICATION_JSON,MediaType.APPLICATION_XML})
    @ApiOperation("Submit a series of (numerical) metric values to the server")
    Response postMetricValues(Collection<NumericDataPoint> points, @Context HttpHeaders headers);

    @GET
    @Path("data/{scheduleId}/baseline")
    @Produces({MediaType.APPLICATION_JSON,MediaType.APPLICATION_XML})
    @ApiOperation(value = "Get the current baseline for the schedule")
    @ApiError(code = 404, reason = NO_SCHEDULE_FOR_ID)
    Baseline getBaseline(@ApiParam("Id of the schedule") @PathParam("scheduleId") int scheduleId,
                         @Context HttpHeaders headers,
                         @Context UriInfo uriInfo);

    @PUT
    @Path("data/{scheduleId}/baseline")
    @Consumes({MediaType.APPLICATION_JSON,MediaType.APPLICATION_XML})
    @ApiOperation(value = "Set a new baseline for the schedule")
    @ApiError(code = 404, reason = NO_SCHEDULE_FOR_ID)
    void setBaseline(@ApiParam("Id of the schedule") @PathParam("scheduleId") int scheduleId,
                     Baseline baseline,
                     @Context HttpHeaders headers,
                     @Context UriInfo uriInfo);

    @PUT
    @Path("data/{scheduleId}/trait")
    @Consumes({MediaType.APPLICATION_JSON,MediaType.APPLICATION_XML})
    @ApiOperation(value = "Submit a new trait value for the passed schedule id")
    @ApiError(code = 404, reason = NO_SCHEDULE_FOR_ID)
    Response putTraitValue(@ApiParam("Id of the schedule") @PathParam("scheduleId") int scheduleId, StringValue value);

    @GET
    @Path("data/{scheduleId}/trait")
    @Produces({MediaType.APPLICATION_JSON,MediaType.APPLICATION_XML})
    @ApiOperation(value="Get the current value of the trait with the passed schedule id", responseClass = "StringValue")
    @ApiError(code = 404, reason = NO_SCHEDULE_FOR_ID)
    Response getTraitValue(@ApiParam("Id of the schedule") @PathParam("scheduleId") int scheduleId);
}
