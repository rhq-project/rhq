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

import org.rhq.enterprise.server.rest.domain.MetricAggregate;
import org.rhq.enterprise.server.rest.domain.MetricSchedule;
import org.rhq.enterprise.server.rest.domain.NumericDataPoint;

/**
 * Deal with metrics
 * @author Heiko W. Rupp
 */
@Produces({"application/json","application/xml", "text/html"})
@Path("/metric")
@Local
public interface MetricHandlerLocal {


    @GET
    @Path("data/{scheduleId}")
    @Produces({MediaType.APPLICATION_JSON,MediaType.APPLICATION_XML,MediaType.TEXT_HTML})
    Response getMetricData(@PathParam("scheduleId") int scheduleId,
                                  @QueryParam("startTime")  long startTime,
                                  @QueryParam("endTime") long endTime,
                                  @QueryParam("dataPoints") @DefaultValue("60") int dataPoints,
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
    List<MetricAggregate> getAggregatesForResource(@PathParam("resourceId") int resourceId);

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
    Response getSchedule(@PathParam("id") int scheduleId, @Context Request request, @Context HttpHeaders headers,
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
    Response updateSchedule(@PathParam("id") int scheduleId,  MetricSchedule in,@Context HttpHeaders headers);

    /**
     * Expose the raw metrics for the given schedule
     * @param scheduleId Schedule id
     * @param startTime Start time, if 0 and duration=, start time = 8h before endTime
     * @param endTime End time. If 0, now is used
     * @param duration Duration in seconds. If duration=0, startTime is used
     * @param request Injected Request headers
     * @param headers Injected HttpHeaders
     * @return a JSON encoded stream of numerical values
     */
    @GET
    @Path("data/{scheduleId}/raw")
    @Produces({MediaType.APPLICATION_JSON,MediaType.APPLICATION_XML})
    StreamingOutput getMetricDataRaw(@PathParam("scheduleId") int scheduleId,
                                     @QueryParam("startTime") long startTime,
                                     @QueryParam("endTime") long endTime,
                                     @QueryParam("duration") long duration,
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
    Response putMetricValue(@PathParam("scheduleId") int scheduleId,
                            @PathParam("timeStamp") long timestamp, NumericDataPoint point,
                            @Context HttpHeaders headers,
                            @Context UriInfo uriInfo);

    /**
     * Submit a series of (numerical) metric values to the server
     * @param points Collection of NumericDataPoint entries
     * @param headers Injected HTTP headers
     * @return
     */
    @POST
    @Path("data/raw")
    @Consumes({MediaType.APPLICATION_JSON,MediaType.APPLICATION_XML})
    Response postMetricValues(Collection<NumericDataPoint> points, @Context HttpHeaders headers);
}
