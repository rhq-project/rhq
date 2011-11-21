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
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
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
import org.jboss.resteasy.links.AddLinks;
import org.jboss.resteasy.links.LinkResource;
import org.jboss.resteasy.links.LinkResources;

import org.rhq.enterprise.server.rest.domain.AvailabilityRest;
import org.rhq.enterprise.server.rest.domain.Link;
import org.rhq.enterprise.server.rest.domain.MetricSchedule;
import org.rhq.enterprise.server.rest.domain.ResourceWithChildren;
import org.rhq.enterprise.server.rest.domain.ResourceWithType;

/**
 * Interface class that describes the REST interface
 * @author Heiko W. Rupp
 */
@Produces({MediaType.APPLICATION_JSON,MediaType.APPLICATION_XML,MediaType.TEXT_HTML})
@Path("/resource")
@Local
public interface ResourceHandlerLocal {

    @AddLinks
    @LinkResources({
            @LinkResource(rel="children", value = ResourceWithType.class)
    })
    @GET
    @Path("/{id}")
    @Cache(isPrivate = true,maxAge = 120)
    Response getResource(@PathParam("id") int id, @Context Request request, @Context HttpHeaders headers,
                         @Context UriInfo uriInfo);

    @GET
    @Path("/platforms")
            @Cache(isPrivate = true,maxAge = 300)
    Response getPlatforms(@Context Request request, @Context HttpHeaders headers,
                         @Context UriInfo uriInfo);


    @GET
    @Path("/{id}/hierarchy")
    @Produces({"application/json","application/xml"})
    ResourceWithChildren getHierarchy(@PathParam("id")int baseResourceId);

    @LinkResource(rel = "availability", value = AvailabilityRest.class)
    @GET
    @Path("/{id}/availability")
    AvailabilityRest getAvailability(@PathParam("id") int resourceId);

    @GET
    @Path("/{id}/schedules")
    @LinkResource(rel="schedules",value = MetricSchedule.class)
    @Cache(isPrivate = true,maxAge = 60)
    Response getSchedules(@PathParam("id") int resourceId,
                                      @QueryParam("type") @DefaultValue("all") String scheduleType,
                                      @Context Request request,
                                      @Context HttpHeaders headers,
                                      @Context UriInfo uriInfo);


    @GET
    @Path("/{id}/children")
    @LinkResource(rel="children", value = ResourceWithType.class)
    Response getChildren(@PathParam("id") int id, @Context Request request, @Context HttpHeaders headers,
                         @Context UriInfo uriInfo);


    @AddLinks
    @GET
    @Path(("/{id}/alerts"))
    List<Link> getAlertsForResource(@PathParam("id") int resourceId); // TODO paging + status

}
