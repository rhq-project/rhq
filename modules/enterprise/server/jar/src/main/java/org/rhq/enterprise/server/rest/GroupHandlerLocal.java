package org.rhq.enterprise.server.rest;

import javax.ejb.Local;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.jboss.resteasy.annotations.cache.Cache;

import org.rhq.enterprise.server.rest.domain.GroupRest;

/**
 * Handler for Group related things
 * @author Heiko W. Rupp
 */
@Local
@Path("/group/")
@Produces({MediaType.APPLICATION_JSON,MediaType.APPLICATION_XML,MediaType.TEXT_HTML, "application/yaml"})
public interface GroupHandlerLocal {

    @GET
    @Path("/")
    public Response getGroups(@Context Request request, @Context HttpHeaders headers,
                             @Context UriInfo uriInfo);

    @Cache(isPrivate = true,maxAge = 60)
    @GET
    @Path("{id}")
    public Response getGroup(@PathParam("id") int id,@Context Request request, @Context HttpHeaders headers,
                             @Context UriInfo uriInfo);


    @POST
    @Path("/")
    public Response createGroup(GroupRest group, @Context Request request, @Context HttpHeaders headers,
                             @Context UriInfo uriInfo);

    @PUT
    @Path("{id}")
    public Response updateGroup(@PathParam("id") int id, GroupRest in, @Context Request request,
                                @Context HttpHeaders headers,
                                @Context UriInfo uriInfo);

    @DELETE
    @Path("{id}")
    public Response deleteGroup(@PathParam("id") int id,@Context Request request, @Context HttpHeaders headers,
                             @Context UriInfo uriInfo);


    @GET
    @Path("{id}/resources")
    @Cache(isPrivate = true,maxAge = 60)
    public Response getResources(@PathParam("id") int id, @Context Request request, @Context HttpHeaders headers,
                                 @Context UriInfo uriInfo);


    @PUT
    @Path("{id}/resource/{resourceId}")
    public Response addResource(@PathParam("id") int id, @PathParam("resourceId") int resourceId,
                                @Context Request request, @Context HttpHeaders headers,
                             @Context UriInfo uriInfo);

    @DELETE
    @Path("{id}/resource/{resourceId}")
    public Response removeResource(@PathParam("id") int id, @PathParam("resourceId") int resourceId,
                                   @Context Request request, @Context HttpHeaders headers,
                             @Context UriInfo uriInfo);
}
