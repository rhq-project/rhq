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

import java.util.ArrayList;
import java.util.List;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.interceptor.Interceptors;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiError;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;

import org.jboss.resteasy.annotations.GZIP;
import org.jboss.resteasy.annotations.cache.Cache;

import org.rhq.core.domain.criteria.ResourceTypeCriteria;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.server.resource.ResourceTypeManagerLocal;
import org.rhq.enterprise.server.resource.ResourceTypeNotFoundException;
import org.rhq.enterprise.server.rest.domain.ResourceTypeRest;
import org.rhq.enterprise.server.rest.domain.ResourceWithType;

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

        ResourceTypeRest rtr = resourceTypeToResourceTypeRest(type);

        MediaType mediaType = headers.getAcceptableMediaTypes().get(0);

        Response.ResponseBuilder builder = Response.ok();
        builder.type(mediaType);
        builder.entity(rtr);
        return builder.build();

    }


    @GET @GZIP
    @Path("/")
    @ApiOperation(value = "Search for resource types", responseClass = "ResourceTypeRest", multiValueResponse = true)
    public Response getTypes(@ApiParam("Limit results to param in the resource type name") @QueryParam("q") String name,
                             @ApiParam("Limit results to the plugin with the passed name") @QueryParam("plugin") String pluginName,
                             @ApiParam("Page size for paging") @QueryParam("ps") @DefaultValue("20") int pageSize,
                             @ApiParam("Page for paging, 0-based") @QueryParam("page") Integer page,
                             @Context UriInfo uriInfo,
        @Context HttpHeaders headers) {

        ResourceTypeCriteria criteria = new ResourceTypeCriteria();
        if (name!=null) {
            criteria.addFilterName(name);
        }

        if (pluginName!=null) {
            criteria.addFilterPluginName(pluginName);
        }

        if (page != null) {
            criteria.setPaging(page, pageSize);
        }

        PageList<ResourceType> pageList = typeManager.findResourceTypesByCriteria(caller,criteria);
        List<ResourceTypeRest> rtrList = new ArrayList<ResourceTypeRest>(pageList.size());
        for (ResourceType type : pageList) {
            ResourceTypeRest rtr = resourceTypeToResourceTypeRest(type);
            rtrList.add(rtr);
        }

        MediaType mediaType = headers.getAcceptableMediaTypes().get(0);
        Response.ResponseBuilder builder = Response.ok();
        builder.type(mediaType);

        if (mediaType.equals(wrappedCollectionJsonType)) {
            wrapForPaging(builder, uriInfo, pageList, rtrList);
        } else {
            GenericEntity<List<ResourceTypeRest>> list = new GenericEntity<List<ResourceTypeRest>>(rtrList) {
        };
            builder.entity(list);
            createPagingHeader(builder,uriInfo,pageList);
        }

        return builder.build();

    }

    private ResourceTypeRest resourceTypeToResourceTypeRest(ResourceType type) {
        ResourceTypeRest rtr = new ResourceTypeRest();
        rtr.setId(type.getId());
        rtr.setName(type.getName());
        rtr.setPluginName(type.getPlugin());
        rtr.setCreatePolicy(type.getCreateDeletePolicy());
        rtr.setDataType(type.getCreationDataType());
        return rtr;
    }


}
