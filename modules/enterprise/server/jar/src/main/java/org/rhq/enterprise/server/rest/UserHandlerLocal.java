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

import javax.ejb.Local;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;

import org.jboss.resteasy.annotations.GZIP;
import org.jboss.resteasy.annotations.cache.Cache;


/**
 * Bean that deals with user specific stuff
 * @author Heiko W. Rupp
 */
@Produces({"application/json","application/xml","text/plain","text/html"})
@Path("/user")
@Local
@Api(value="Api that deals with user related stuff")
public interface UserHandlerLocal {

    @GZIP
    @GET
    @Path("favorites/resource")
    @ApiOperation(value = "Return a list of favorite resources of the caller", multiValueResponse = true, responseClass = "ResourceWithType")
    Response getFavorites(@Context UriInfo uriInfo,@Context HttpHeaders headers);

    @PUT
    @Path("favorites/resource/{id}")
    @ApiOperation(value = "Add a resource as favorite for the caller")
    public void addFavoriteResource(
            @ApiParam(name = "id", value = "Id of the resource")
            @PathParam("id") int id);

    @DELETE
    @Path("favorites/resource/{id}")
    @ApiOperation(value="Remove a resource from favorites")
    public void removeResourceFromFavorites(
            @ApiParam(name="id", value = "Id of the resource")
            @PathParam("id") int id);


    @GET
    @Cache(maxAge = 600)
    @Path("{id}")
    @ApiOperation(value = "Get info about a user", responseClass = "UserRest")
    public Response getUserDetails(@ApiParam(value="Login of the user") @PathParam("id")String loginName,
                                   @Context Request request,@Context HttpHeaders headers);
}
