/*
 * RHQ Management Platform
 * Copyright (C) 2005-2013 Red Hat, Inc.
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
 * along with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA
 */

package org.rhq.enterprise.server.rest;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.interceptor.Interceptors;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiError;
import com.wordnik.swagger.annotations.ApiOperation;

import org.jboss.resteasy.annotations.cache.Cache;

import org.rhq.core.domain.resource.ResourceType;
import org.rhq.enterprise.server.resource.ResourceTypeManagerLocal;
import org.rhq.enterprise.server.resource.ResourceTypeNotFoundException;
import org.rhq.enterprise.server.rest.domain.ResourceTypeRest;

/**
 * Deal with resource types
 * @author Heiko W. Rupp
 */
@Produces({MediaType.APPLICATION_JSON,MediaType.APPLICATION_XML})
@Path("/resource/type")
@Api(value="Resource type related", description = "This endpoint deals with resource types")
@Interceptors(SetCallerInterceptor.class)
@Stateless

public class ResourceTypeHandlerBean extends AbstractRestBean {


    @EJB
    ResourceTypeManagerLocal typeManager;


    @Cache(maxAge = 600)
    @GET
    @Path("{id}")
    @ApiOperation(value = "Return information about the resource type with the passed id",responseClass = "ResourceTypeRest")
    @ApiError(code = 404, reason = "There is no type with the passed id")
    public Response getTypeById(
        @PathParam("id") int resourceTypeId,
        @Context HttpHeaders headers,
        @Context UriInfo uriInfo) {


        ResourceType type;
        try {
            type = typeManager.getResourceTypeById(caller,resourceTypeId);
        } catch (ResourceTypeNotFoundException e) {
            throw new StuffNotFoundException("Resource type with id " + resourceTypeId);
        }

        ResourceTypeRest rtr = new ResourceTypeRest();
        rtr.setId(resourceTypeId);
        rtr.setName(type.getName());
        rtr.setPluginName(type.getPlugin());
        rtr.setCreatePolicy(type.getCreateDeletePolicy());
        rtr.setDataType(type.getCreationDataType());

        MediaType mediaType = headers.getAcceptableMediaTypes().get(0);

        Response.ResponseBuilder builder = Response.ok();
        builder.type(mediaType);
        builder.entity(rtr);
        return builder.build();

    }

}
