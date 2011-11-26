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

import java.util.List;

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

import org.jboss.resteasy.annotations.cache.Cache;

import org.rhq.enterprise.server.rest.domain.OperationDefinitionRest;
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
    public Response getOperationDefinition(@PathParam("id") int definitionId,
                                           @QueryParam("resourceId") Integer resourceId,
                                           @Context UriInfo uriInfo,
                                           @Context Request request,
                                           @Context HttpHeaders httpHeaders);

    @GET
    @Path("definitions")
    @Cache(maxAge = 1200)
    public Response getOperationDefinitions(@QueryParam("resourceId") Integer resourceId,
                                            @Context UriInfo uriInfo,
                                            @Context Request request
    );

    @POST
    @Path("definition/{id}")
    public Response createOperation(@PathParam("id") int definitionId,
                                    @QueryParam("resourceId") Integer resourceId,
                                    @Context UriInfo uriInfo);

    @GET
    @Path("{id}")
    public Response getOperation(@PathParam("id") int operationId);

    @PUT
    @Path("{id}")
    @Consumes({MediaType.APPLICATION_JSON,MediaType.APPLICATION_XML})
    public Response updateOperation(@PathParam("id") int operationId, OperationRest operation, @Context UriInfo uriInfo);

    @DELETE
    @Path("{id}")
    public Response cancelOperation(@PathParam("id") int operationId);

    @GET
    @Path("history/{id}")
    public Response outcome(@PathParam("id") String jobName, @Context UriInfo uriInfo);
}
