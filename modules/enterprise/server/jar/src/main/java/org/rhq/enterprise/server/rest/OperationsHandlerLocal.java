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

import javax.ejb.Local;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
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

import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;

import org.jboss.resteasy.annotations.cache.Cache;

import org.rhq.enterprise.server.rest.domain.OperationRest;

/**
 * Service that deals with operations
 * @author Heiko W. Rupp
 */
@Local
@Path("/operation")
@Produces({MediaType.APPLICATION_JSON,MediaType.APPLICATION_XML})
public interface OperationsHandlerLocal {


    @GET
    @Path("definition/{id}")
    @Cache(maxAge = 1200)
    @ApiOperation("Retrieve a single operation definition by its id")
    public Response getOperationDefinition(
            @ApiParam("Id of the definition to retrieve") @PathParam("id") int definitionId,
            @ApiParam("Id of a resource that supports this operation") @QueryParam("resourceId") Integer resourceId,
               @Context UriInfo uriInfo,
               @Context Request request,
               @Context HttpHeaders httpHeaders);

    @GET
    @Path("definitions")
    @Cache(maxAge = 1200)
    @ApiOperation("List all operation definitions for a resource")
    public Response getOperationDefinitions(
            @ApiParam(value = "Id of the resource",required = true) @QueryParam("resourceId") Integer resourceId,
                                            @Context UriInfo uriInfo,
                                            @Context Request request,
                                            @Context HttpHeaders httpHeaders
    );

    @POST
    @Path("definition/{id}")
    @ApiOperation("Create a new (draft) operation from the passed definition id for the passed resource")
    public Response createOperation(
            @ApiParam("Id of the definition") @PathParam("id") int definitionId,
            @ApiParam(value = "Id of the resource", required = true) @QueryParam("resourceId") Integer resourceId,
                                    @Context UriInfo uriInfo);

    @GET
    @Path("{id}")
    @ApiOperation("Return a (draft) operation")
    public Response getOperation(
            @ApiParam("Id of the operation to retrieve") @PathParam("id") int operationId);

    @PUT
    @Path("{id}")
    @Consumes({MediaType.APPLICATION_JSON,MediaType.APPLICATION_XML})
    @ApiOperation("Update a (draft) operation. If the state is set to 'ready', the operation will be scheduled")
    public Response updateOperation(
            @ApiParam("Id of the operation to update") @PathParam("id") int operationId,
            OperationRest operation, @Context UriInfo uriInfo);

    @DELETE
    @Path("{id}")
    @ApiOperation("Delete a (draft) operation")
    public Response cancelOperation(
            @ApiParam("Id of the operation to remove") @PathParam("id") int operationId);

    @GET
    @Path("history/{id}")
    @ApiOperation("Return the outcome of the scheduled operation")
    @Produces({MediaType.APPLICATION_JSON,MediaType.APPLICATION_XML,MediaType.TEXT_HTML})
    public Response outcome(
            @ApiParam("Name of the submitted job.") @PathParam("id") String jobName,
            @Context UriInfo uriInfo,
            @Context Request request,
            @Context HttpHeaders httpHeaders);

    @GET
    @Path("history")
    @ApiOperation("Return the outcome of the scheduled operations")
    @Produces({MediaType.APPLICATION_JSON,MediaType.APPLICATION_XML,MediaType.TEXT_HTML})
    public Response listHistory(
            @ApiParam("Id of a resource to limit to") @QueryParam("resourceId") int resourceId,
            @Context UriInfo uriInfo,
            @Context Request request,
            @Context HttpHeaders httpHeaders);


    @DELETE
    @Path("history/{id}")
    @ApiOperation(value = "Delete the operation history item with the passed id")
    public Response deleteOperationHistoryItem(
            @ApiParam("Name fo the submitted job") @PathParam("id") String jobId);
}
