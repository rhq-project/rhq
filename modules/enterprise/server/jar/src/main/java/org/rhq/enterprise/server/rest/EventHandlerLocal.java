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

import java.util.List;

import javax.ejb.Local;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;

import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;

import org.jboss.resteasy.annotations.GZIP;

import org.rhq.enterprise.server.rest.domain.EventRest;
import org.rhq.enterprise.server.rest.domain.EventSourceRest;

/**
 * Class that deals with events
 * @author Heiko W. Rupp
 */
@Local
@Path("/event")
@Api("Api that deals with Events (e.g snmp traps, logfile lines)")
@Produces({MediaType.APPLICATION_JSON,MediaType.APPLICATION_XML,MediaType.TEXT_HTML})
public interface EventHandlerLocal {



    @GET
    @Path("/{id}/sources")
    @ApiOperation(value = "List the defined event sources for the resource", responseClass = "EventSourceRest", multiValueResponse = true)
    Response listEventSourcesForResource(
            @ApiParam("id of the resource") @PathParam("id") int resourceId,
                                         @Context Request request,
                                         @Context HttpHeaders headers);



    @POST
    @Path("/{id}/sources")
    @ApiOperation("Add a new event source for a resource. This can e.g. be a different logfile. " +
            "The source.name must match an existing definition fo this resource. " +
            "If an event source for the definition name and resource with the same location already exists, no new source is created. " +
            "NOTE: An Event source added this way will not sow up in the connection properties.")
    EventSourceRest addEventSource(
            @ApiParam("id of the resource") @PathParam("id") int resourceId,
            EventSourceRest source);

    @DELETE
    @Path("/source/{id}")
    @ApiOperation(value = "Delete the event source with the passed id")
    Response deleteEventSource(
            @ApiParam("Id of the source to delete") @PathParam("id") int sourceId);

    @GET
    @Path("/source/{id}")
    @ApiOperation(value = "Retrieve the event source with the passed id", responseClass = "EventSourceRest")
    EventSourceRest getEventSource(
            @ApiParam("Id of the source to retrieve") @PathParam("id") int sourceId);

    @POST
    @Path("/source/{id}/events")
    @ApiOperation("Submit multiple events for one given event source; the event source in the passed Events is ignored.")
    Response addEventsToSource(
            @ApiParam("Id of the source to add data to")  @PathParam("id") int sourceId, List<EventRest> event);

    @GET @GZIP
    @Path("/source/{id}/events")
    @ApiOperation(value = "List the events for the event source with the passed id. If no time range is given, the last 200 entries will be displayed",
            responseClass = "EventRest", multiValueResponse = true)
    Response getEventsForSource(@PathParam("id") int sourceId,
                                @QueryParam("startTime") long startTime,
                                @QueryParam("endTime") long endTime,
                                @ApiParam(value="Select the severity to display. Default is to show all",
                                allowableValues = "DEBUG, INFO, WARN, ERROR, FATAL") @QueryParam("severity") String severity,
                                @Context Request request,
                                @Context HttpHeaders headers);

    @GET @GZIP
    @Path("/{id}/events")
    @ApiOperation(value="List the events for the resource with the passed id. If no time range is given, the last 200 entries will be displayed",
            responseClass = "EventRest", multiValueResponse = true)
    Response getEventsForResource(@PathParam("id") int resourceId,
                                  @QueryParam("startTime") long startTime,
                                  @QueryParam("endTime") long endTime,
                                  @QueryParam("severity") String severity,
                                  @Context Request request,
                                  @Context HttpHeaders headers);


}
