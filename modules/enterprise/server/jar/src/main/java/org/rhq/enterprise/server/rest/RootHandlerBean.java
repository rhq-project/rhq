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

import java.util.ArrayList;
import java.util.List;

import javax.ejb.Stateless;
import javax.interceptor.Interceptors;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;

import org.rhq.enterprise.server.rest.domain.Link;

/**
 * Handle the /rest/ context root
 *
 * @author Heiko W. Rupp
 */
@Api("Handle the root context to have an anchor for discoverability")
@Path("/")
@Interceptors(SetCallerInterceptor.class)
@Stateless
public class RootHandlerBean extends AbstractRestBean  {

    private String[] roots = { // rel, target
            "platforms","resource/platforms",
            "groups","group",
            "dynaGroups","group/definitions",
            "alerts","alert",
            "status","status",
            "favoriteResources","user/favorites/resource",
            "operationHistory","operation/history",
            "reports","reports",
            "plugins","plugins",
            "self",""
    };

    @GET
    @Path("index")
    @ApiOperation("Return links from the root /index of the REST-resource tree")
    public Response index(@Context Request request, @Context HttpHeaders headers, @Context UriInfo uriInfo) {
        return handleIndex(headers,uriInfo);
    }

    @GET
    @Path("/")
    @ApiOperation("Return links from the root / of the REST-resource tree")
    public Response index2(@Context Request request, @Context HttpHeaders headers, @Context UriInfo uriInfo) {
        return handleIndex(headers,uriInfo);
    }

    private Response handleIndex(HttpHeaders headers, UriInfo uriInfo) {

        MediaType mediaType = headers.getAcceptableMediaTypes().get(0);
        Response.ResponseBuilder builder;
        List<Link> links = new ArrayList<Link>(roots.length/2);

        for (int i = 0; i < roots.length ; i+=2) {
            String rel = roots[i];
            String target = roots[i+1];
            // TODO use the uribuilder for the next?
            if (mediaType.equals(MediaType.APPLICATION_JSON_TYPE)) {
                target += ".json";
            } else if (mediaType.equals(MediaType.TEXT_HTML_TYPE)) {
                target += ".html";
            } else if (mediaType.equals(MediaType.APPLICATION_XML_TYPE)) {
                target += ".xml";
            } else {
                log.error("Unknown media type " + mediaType);
                throw new WebApplicationException(Response.Status.NOT_ACCEPTABLE);
            }
            Link link = new Link(rel,target);
            links.add(link);
        }

        if (mediaType.equals(MediaType.TEXT_HTML_TYPE)) {
            builder = Response.ok(renderTemplate("index",links));
        } else {
            GenericEntity<List<Link>> list = new GenericEntity<List<Link>>(links) {
            };
            builder = Response.ok(list);
        }
        CacheControl cacheControl = new CacheControl();
        cacheControl.setMaxAge(86400); // TODO 1 day or longer? What unit is this anyway?
        builder.cacheControl(cacheControl);
        return builder.build();
    }
}
