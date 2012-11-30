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
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiError;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;

import org.jboss.resteasy.annotations.GZIP;
import org.jboss.resteasy.annotations.cache.Cache;
import org.jboss.resteasy.links.AddLinks;
import org.jboss.resteasy.links.LinkResource;
import org.jboss.resteasy.links.LinkResources;

import org.rhq.enterprise.server.rest.domain.*;

/**
 * Interface class that describes the REST interface
 * @author Heiko W. Rupp
 */
@Produces({MediaType.APPLICATION_JSON,MediaType.APPLICATION_XML,MediaType.TEXT_HTML})
@Path("/resource")
@Local
@Api(value="Resource related", description = "This endpoint deals with individual resources, not resource groups")
public interface ResourceHandlerLocal {

    static String NO_RESOURCE_FOR_ID = "If no resource with the passed id exists";

    @AddLinks
    @LinkResources({
            @LinkResource(rel="children", value = ResourceWithType.class)
    })
    @GET
    @Path("/{id}")
    @Cache(isPrivate = true,maxAge = 120)
    @ApiOperation(value = "Retrieve a single resource", responseClass = "ResourceWithType")
    @ApiError(code = 404, reason = NO_RESOURCE_FOR_ID)
    Response getResource(
            @ApiParam("Id of the resource to retrieve") @PathParam("id") int id,
            @Context Request request, @Context HttpHeaders headers,
                         @Context UriInfo uriInfo);

    @GET
    @Path("/")
    @ApiOperation(value = "Search for resources by the given search string", responseClass = "ResourceWithType")
    Response getResourcesByQuery(@ApiParam("String to search in the resource name") @QueryParam("q")String q,
                                 @Context Request request, @Context HttpHeaders headers,
                             @Context UriInfo uriInfo);

    @GZIP
    @GET
    @Path("/platforms")
    @Cache(isPrivate = true,maxAge = 300)
    @ApiOperation(value = "List all platforms in the system", multiValueResponse = true, responseClass = "ResourceWithType")
    Response getPlatforms(@Context Request request, @Context HttpHeaders headers,
                         @Context UriInfo uriInfo);


    @GET
    @Path("/{id}/hierarchy")
    @Produces({"application/json","application/xml"})
    @ApiOperation(value = "Retrieve the hierarchy of resources starting with the passed one", multiValueResponse = true, responseClass = "ResourceWithType")
    @ApiError(code = 404, reason = NO_RESOURCE_FOR_ID)
    ResourceWithChildren getHierarchy(
            @ApiParam("Id of the resource to start with") @PathParam("id")int baseResourceId);

    @GET
    @Path("/{id}/availability")
    @ApiError(code = 404, reason = NO_RESOURCE_FOR_ID)
    @ApiOperation(value = "Return the current availability for the passed resource", responseClass = "AvailabilityRest")
    Response getAvailability(
            @ApiParam("Id of the resource to query") @PathParam("id") int resourceId, @Context HttpHeaders headers);

    @GZIP
    @GET
    @Path("/{id}/availability/history")
    @ApiError(code = 404, reason = NO_RESOURCE_FOR_ID)
    @ApiOperation(value = "Return the availability history for the passed resource", responseClass = "AvailabilityRest", multiValueResponse = true)
    Response getAvailabilityHistory(
            @ApiParam("Id of the resource to query") @PathParam("id") int resourceId,
            @ApiParam(value="Start time", defaultValue = "30 days ago") @QueryParam("start") long start,
            @ApiParam(value="End time", defaultValue = "Now") @QueryParam("end") long end,

             @Context HttpHeaders headers);

    @PUT
    @Path("/{id}/availability")
    @ApiOperation("Set the current availability of the passed resource")
    public void reportAvailability(
            @ApiParam("Id of the resource to update") @PathParam("id") int resourceId,
            @ApiParam(value= "New Availability setting", required = true) AvailabilityRest avail);

    @GZIP
    @GET
    @Path("/{id}/schedules")
    @LinkResource(rel="schedules",value = MetricSchedule.class)
    @Cache(isPrivate = true,maxAge = 60)
    @ApiOperation(value ="Get the metric schedules of the passed resource id", multiValueResponse = true, responseClass = "MetricSchedule")
    @ApiError(code = 404, reason = NO_RESOURCE_FOR_ID)
    Response getSchedules(@ApiParam("Id of the resource to obtain the schedules for") @PathParam("id") int resourceId,
          @ApiParam(value = "Limit by type", allowableValues = "<empty>, all, metric, trait, measurement" )@QueryParam("type") @DefaultValue("all") String scheduleType,
          @ApiParam(value = "Limit by enabled schedules") @QueryParam("enabledOnly") @DefaultValue("true") boolean enabledOnly,
          @ApiParam(value = "Limit by name") @QueryParam("name") String name,
          @Context Request request,
          @Context HttpHeaders headers,
          @Context UriInfo uriInfo);


    @GZIP
    @GET
    @Path("/{id}/children")
    @LinkResource(rel="children", value = ResourceWithType.class)
    @ApiOperation(value = "Get the direct children of the passed resource")
    @ApiError(code = 404, reason = NO_RESOURCE_FOR_ID)
    Response getChildren(
            @ApiParam("Id of the resource to get children") @PathParam("id") int id,
            @Context Request request, @Context HttpHeaders headers,
                         @Context UriInfo uriInfo);


    @GZIP
    @AddLinks
    @GET
    @Path(("/{id}/alerts"))
    @ApiError(code = 404, reason = NO_RESOURCE_FOR_ID)
    @ApiOperation("Get a list of links to the alerts for the passed resource")
    List<Link> getAlertsForResource(
            @ApiParam("Id of the resource to query") @PathParam("id") int resourceId); // TODO paging + status



    @ApiOperation(value = "Creata a new platform in the Server. If the platform already exists, this is a no-op." +
            "The platform internally has a special name so that it will not clash with one that was generated" +
            "via a normal RHQ agent")
    @POST
    @Path("platform/{name}")
    public Response createPlatform(
            @ApiParam(value = "Name of the platform") @PathParam("name") String name,
            @ApiParam(value = "Type of the platform", allowableValues = "Linux,Windows,... TODO") StringValue type,
            @Context UriInfo uriInfo);

    @ApiOperation(value = "Create a resource with a given type below a certain parent")
    @POST
    @Path("{name}")
    public Response createResource(
            @ApiParam("Name of the new resource") @PathParam("name") String name,
            @ApiParam("Name of the Resource tpye") StringValue type,
            @ApiParam("Name of the plugin providing the type") @QueryParam("plugin") String plugin,
            @ApiParam("Id of the future parent to attach this to") @QueryParam("parentId") int parentId,
            @Context UriInfo uriInfo);


}
