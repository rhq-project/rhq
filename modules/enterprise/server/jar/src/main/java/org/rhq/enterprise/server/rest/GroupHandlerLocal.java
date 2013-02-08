package org.rhq.enterprise.server.rest;

import javax.ejb.Local;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
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
import com.wordnik.swagger.annotations.ApiErrors;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;

import org.jboss.resteasy.annotations.GZIP;
import org.jboss.resteasy.annotations.cache.Cache;

import org.rhq.enterprise.server.rest.domain.GroupDefinitionRest;
import org.rhq.enterprise.server.rest.domain.GroupRest;

/**
 * Handler for Group related things
 * @author Heiko W. Rupp
 */
@Local
@Path("/group")
@Api(value="Deal with groups and DynaGroups", description = "Api that deals with resource groups and group definitions")
@Produces({MediaType.APPLICATION_JSON,MediaType.APPLICATION_XML,MediaType.TEXT_HTML, "application/yaml"})
public interface GroupHandlerLocal {

    @GZIP
    @GET
    @Path("/")
    @ApiOperation(value = "List all groups", multiValueResponse = true)
    public Response getGroups(@ApiParam("String to search in the group name") @QueryParam("q") String q,
            @Context Request request, @Context HttpHeaders headers,
                             @Context UriInfo uriInfo);

    @Cache(isPrivate = true,maxAge = 60)
    @GET
    @Path("{id}")
    @ApiOperation(value = "Get the group with the passed id")
    public Response getGroup(
            @ApiParam(value = "Id of the group") @PathParam("id") int id,
            @Context Request request, @Context HttpHeaders headers,
                             @Context UriInfo uriInfo);


    @GET
    @GZIP
    @Path("{id}/metricDefinitions")
    @ApiOperation(value = "Get the metric definitions for the compatible group with the passed id")
    public Response getMetricDefinitionsForGroup(@ApiParam(value = "Id of the group") @PathParam("id") int id,
                                 @Context Request request, @Context HttpHeaders headers,
                                 @Context UriInfo uriInfo);

    @POST
    @Path("/")
    @ApiOperation(value = "Create a new group")
    public Response createGroup(
            @ApiParam(value = "A GroupRest object containing at least a name for the group") GroupRest group,
            @Context Request request, @Context HttpHeaders headers,
                             @Context UriInfo uriInfo);

    @PUT
    @Path("{id}")
    @ApiOperation(value = "Update the passed group")
    public Response updateGroup(
            @ApiParam(value = "Id of the group to update") @PathParam("id") int id,
            @ApiParam(value="New version of the group") GroupRest in,
            @Context Request request,
                                @Context HttpHeaders headers,
                                @Context UriInfo uriInfo);

    @DELETE
    @Path("{id}")
    @ApiOperation(value="Delete the group with the passed id")
    public Response deleteGroup(
            @ApiParam("Id of the group to delete") @PathParam("id") int id,
            @Context Request request, @Context HttpHeaders headers,
                             @Context UriInfo uriInfo);


    @GZIP
    @GET
    @Path("{id}/resources")
    @Cache(isPrivate = true,maxAge = 60)
    @ApiOperation(value="Get the resources of the group", multiValueResponse = true)
    public Response getResources(
            @ApiParam("Id of the group to retrieve the resources for") @PathParam("id") int id,
            @Context Request request, @Context HttpHeaders headers,
                                 @Context UriInfo uriInfo);


    @PUT
    @Path("{id}/resource/{resourceId}")
    @ApiOperation(value="Add a resource to an existing group")
    @ApiErrors({
            @ApiError(code = 404,reason = "If there is no resource or group with the passed id "),
            @ApiError(code = 409,reason =" Resource type does not match the group one")
    })
    public Response addResource(
            @ApiParam("Id of the existing group") @PathParam("id") int id,
            @ApiParam("Id of the resource to add") @PathParam("resourceId") int resourceId,
                                @Context Request request, @Context HttpHeaders headers,
                             @Context UriInfo uriInfo);

    @DELETE
    @Path("{id}/resource/{resourceId}")
    @ApiOperation("Remove the resource with the passed id from the group")
    public Response removeResource(
            @ApiParam("Id of the existing group") @PathParam("id") int id,
            @ApiParam("Id of the resource to remove") @PathParam("resourceId") int resourceId,
                             @Context Request request, @Context HttpHeaders headers,
                             @Context UriInfo uriInfo);

    @GZIP
    @GET
    @Path("/definitions")
    @ApiOperation(value="List all existing GroupDefinitions",multiValueResponse = true)
    public Response getGroupDefinitions(@ApiParam("String to search in the group definition name") @QueryParam("q") String q,
                                        @Context Request request, @Context HttpHeaders headers,
                                        @Context UriInfo uriInfo);

    @GZIP
    @GET
    @Path("/definition/{id}")
    @Cache(isPrivate = true,maxAge = 60)
    @ApiOperation(value = "Retrieve a single GroupDefinition by id")
    public Response getGroupDefinition(
            @ApiParam("The id of the definition to retrieve") @PathParam("id") int definitionId,
            @Context Request request, @Context HttpHeaders headers,
            @Context UriInfo uriInfo);

    @DELETE
    @Path("/definition/{id}")
    @ApiOperation("Delete the GroupDefinition with the passed id")
    public Response deleteGroupDefinition(
            @ApiParam("The id of the definition to delete") @PathParam("id") int definitionId,
            @Context Request request, @Context HttpHeaders headers,
            @Context UriInfo uriInfo);

    @POST
    @Path("/definitions")
    @Consumes({MediaType.APPLICATION_JSON,MediaType.APPLICATION_XML})
    @ApiOperation("Create a new GroupDefinition. The name of the group is required in the passed definition.")
    public Response createGroupDefinition(
            GroupDefinitionRest definition,
            @Context Request request, @Context HttpHeaders headers,
            @Context UriInfo uriInfo);

    @PUT
    @Path("/definition/{id}")
    @Consumes({MediaType.APPLICATION_JSON,MediaType.APPLICATION_XML})
    @ApiOperation("Update an existing GroupDefinition")
    public Response updateGroupDefinition(
            @ApiParam("Id fo the definition to update") @PathParam("id") int definitionId,
            @ApiParam("If true, trigger a re-calculation") @QueryParam("recalculate") @DefaultValue(
                    "false") boolean recalculate,
            GroupDefinitionRest definition,
            @Context Request request, @Context HttpHeaders headers,
            @Context UriInfo uriInfo);

}
