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

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.interceptor.Interceptors;
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
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiError;
import com.wordnik.swagger.annotations.ApiErrors;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.jboss.resteasy.annotations.GZIP;
import org.jboss.resteasy.annotations.cache.Cache;

import org.rhq.core.domain.criteria.ResourceGroupCriteria;
import org.rhq.core.domain.criteria.ResourceGroupDefinitionCriteria;
import org.rhq.core.domain.measurement.DataType;
import org.rhq.core.domain.measurement.MeasurementDefinition;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.resource.group.GroupDefinition;
import org.rhq.core.domain.resource.group.ResourceGroup;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.server.resource.ResourceManagerLocal;
import org.rhq.enterprise.server.resource.ResourceTypeManagerLocal;
import org.rhq.enterprise.server.resource.ResourceTypeNotFoundException;
import org.rhq.enterprise.server.resource.group.ResourceGroupDeleteException;
import org.rhq.enterprise.server.resource.group.definition.GroupDefinitionManagerLocal;
import org.rhq.enterprise.server.resource.group.definition.exception.GroupDefinitionAlreadyExistsException;
import org.rhq.enterprise.server.resource.group.definition.exception.GroupDefinitionCreateException;
import org.rhq.enterprise.server.resource.group.definition.exception.GroupDefinitionDeleteException;
import org.rhq.enterprise.server.resource.group.definition.exception.GroupDefinitionNotFoundException;
import org.rhq.enterprise.server.rest.domain.GroupDefinitionRest;
import org.rhq.enterprise.server.rest.domain.GroupRest;
import org.rhq.enterprise.server.rest.domain.Link;
import org.rhq.enterprise.server.rest.domain.MetricSchedule;
import org.rhq.enterprise.server.rest.domain.ResourceWithType;
import org.rhq.enterprise.server.util.CriteriaQuery;
import org.rhq.enterprise.server.util.CriteriaQueryExecutor;

/**
 * Deal with group related things.
 * @author Heiko W. Rupp
 */
@Stateless
@Interceptors(SetCallerInterceptor.class)
@Path("/group")
@Api(value="Deal with groups and DynaGroups", description = "Api that deals with resource groups and group definitions")
@Produces({MediaType.APPLICATION_JSON,MediaType.APPLICATION_XML,MediaType.TEXT_HTML, "application/yaml"})
public class GroupHandlerBean extends AbstractRestBean  {

    private final Log log = LogFactory.getLog(GroupHandlerBean.class);

    @EJB
    ResourceManagerLocal resourceManager;

    @EJB
    ResourceTypeManagerLocal resourceTypeManager;

    @EJB
    GroupDefinitionManagerLocal definitionManager;

    @GZIP
    @GET
    @Path("/")
    @ApiOperation(value = "List all groups", multiValueResponse = true)
    public Response getGroups(@ApiParam("String to search in the group name") @QueryParam("q") String q,
                              @Context HttpHeaders headers, @Context UriInfo uriInfo) {

        ResourceGroupCriteria criteria = new ResourceGroupCriteria();
        if (q!=null) {
            criteria.addFilterName(q);
        }

        //Use CriteriaQuery to automatically chunk/page through criteria query results
        CriteriaQueryExecutor<ResourceGroup, ResourceGroupCriteria> queryExecutor = new CriteriaQueryExecutor<ResourceGroup, ResourceGroupCriteria>() {
            @Override
            public PageList<ResourceGroup> execute(ResourceGroupCriteria criteria) {
                return resourceGroupManager.findResourceGroupsByCriteria(caller, criteria);
            }
        };

        CriteriaQuery<ResourceGroup, ResourceGroupCriteria> groups = new CriteriaQuery<ResourceGroup, ResourceGroupCriteria>(
            criteria, queryExecutor);

        List<GroupRest> list = new ArrayList<GroupRest>();
        for (ResourceGroup group : groups) {
            list.add(fillGroup(group, uriInfo));
        }

        MediaType mediaType = headers.getAcceptableMediaTypes().get(0);
        Response.ResponseBuilder builder;

        if (mediaType.equals(MediaType.TEXT_HTML_TYPE)) {
            builder = Response.ok(renderTemplate("listGroup", list), mediaType);
        }
        else {
            GenericEntity<List<GroupRest>> ret = new GenericEntity<List<GroupRest>>(list) {};
            builder = Response.ok(ret);
        }

        return builder.build();

    }

    @Cache(isPrivate = true,maxAge = 60)
    @GET
    @Path("{id}")
    @ApiOperation(value = "Get the group with the passed id")
    public Response getGroup(@ApiParam(value = "Id of the group") @PathParam("id") int id,
                             @Context HttpHeaders headers,
                             @Context UriInfo uriInfo) {

        ResourceGroup group = fetchGroup(id, false);

        GroupRest groupRest = fillGroup(group, uriInfo);

        MediaType mediaType = headers.getAcceptableMediaTypes().get(0);

        Response.ResponseBuilder builder;

        if (mediaType.equals(MediaType.TEXT_HTML_TYPE)) {
            builder = Response.ok(renderTemplate("group", groupRest), mediaType);
        }
        else {
            builder = Response.ok(groupRest,mediaType);
        }

        return builder.build();
    }

    @POST
    @Path("/")
    @ApiOperation(value = "Create a new group")
    public Response createGroup(
            @ApiParam(value = "A GroupRest object containing at least a name for the group") GroupRest group,
            @Context HttpHeaders headers, @Context UriInfo uriInfo) {

        if (group==null)
            throw new BadArgumentException("A group must be provided");
        if (group.getName()==null)
            throw new BadArgumentException("A group name is required");

        ResourceGroup newGroup = new ResourceGroup(group.getName());
        if (group.getResourceTypeId()!=null) {

            ResourceType resourceType = null;
            try {
                resourceType = resourceTypeManager.getResourceTypeById(caller,group.getResourceTypeId());
                newGroup.setResourceType(resourceType);
            } catch (ResourceTypeNotFoundException e) {
                throw new StuffNotFoundException("ResourceType with id " + group.getResourceTypeId());
            }
        }

        MediaType mediaType = headers.getAcceptableMediaTypes().get(0);
        Response.ResponseBuilder builder;
        try {
            newGroup = resourceGroupManager.createResourceGroup(caller, newGroup);
            UriBuilder uriBuilder = uriInfo.getBaseUriBuilder();
            uriBuilder.path("/group/{id}");
            URI uri = uriBuilder.build(newGroup.getId());

            builder=Response.created(uri);
            builder.type(mediaType);
            putToCache(newGroup.getId(),ResourceGroup.class,newGroup);
        } catch (Exception e) {
            builder=Response.status(Response.Status.NOT_ACCEPTABLE);
            builder.type(mediaType);
            builder.entity(e.getCause());
        }
        return builder.build();
    }

    @PUT
    @Path("{id}")
    @ApiOperation(value = "Update the passed group. Currently only name change is supported")
    public Response updateGroup(@ApiParam(value = "Id of the group to update") @PathParam("id") int id,
                                @ApiParam(value = "New version of the group") GroupRest in,
                                @Context HttpHeaders headers,
                                @Context UriInfo uriInfo) {

        ResourceGroup resourceGroup = fetchGroup(id, false);
        resourceGroup.setName(in.getName());
        Response.ResponseBuilder builder;

        try {
            resourceGroup = resourceGroupManager.updateResourceGroup(caller,resourceGroup);
            builder=Response.ok(fillGroup(resourceGroup,uriInfo));
            putToCache(resourceGroup.getId(),ResourceGroup.class,resourceGroup);
        }
        catch (Exception e) {
            builder = Response.status(Response.Status.NOT_ACCEPTABLE); // TODO correct?
        }
        return builder.build();
    }

    @DELETE
    @Path("{id}")
    @ApiOperation(value="Delete the group with the passed id")
    public Response deleteGroup(@ApiParam("Id of the group to delete") @PathParam("id") int id) {

        try {
            resourceGroupManager.deleteResourceGroup(caller,id);
            removeFromCache(id,ResourceGroup.class);
            return Response.ok().build();
        } catch (ResourceGroupDeleteException e) {
            e.printStackTrace();  // TODO: Customise this generated block
            return Response.serverError().build(); // TODO what exactly ?
        }
    }

    @GZIP
    @GET
    @Path("{id}/resources")
    @Cache(isPrivate = true,maxAge = 60)
    @ApiOperation(value="Get the resources of the group", multiValueResponse = true)
    public Response getResources(@ApiParam("Id of the group to retrieve the resources for") @PathParam("id") int id,
                                 @Context HttpHeaders headers,
                                 @Context UriInfo uriInfo) {

        ResourceGroup resourceGroup = fetchGroup(id, false);

        Set<Resource> resources = resourceGroup.getExplicitResources();
        List<ResourceWithType> rwtList = new ArrayList<ResourceWithType>(resources.size());
        for (Resource res: resources) {
            rwtList.add(fillRWT(res,uriInfo));
        }
        MediaType mediaType = headers.getAcceptableMediaTypes().get(0);
        Response.ResponseBuilder builder;

        if (mediaType.equals(MediaType.TEXT_HTML_TYPE)) {
            builder = Response.ok(renderTemplate("listResourceWithType", rwtList), mediaType);
        }
        else {
            GenericEntity<List<ResourceWithType>> list = new GenericEntity<List<ResourceWithType>>(rwtList){};
            builder = Response.ok(list);
        }

        return builder.build();

    }

    @PUT
    @Path("{id}/resource/{resourceId}")
    @ApiOperation(value="Add a resource to an existing group")
    @ApiErrors({
            @ApiError(code = 404,reason = "If there is no resource or group with the passed id "),
            @ApiError(code = 409,reason =" Resource type does not match the group one")
    })
    public Response addResource(@ApiParam("Id of the existing group") @PathParam("id") int id,
                                @ApiParam("Id of the resource to add") @PathParam("resourceId") int resourceId,
                                @Context HttpHeaders headers, @Context UriInfo uriInfo) {

        ResourceGroup resourceGroup = fetchGroup(id, false);
        Resource res = resourceManager.getResource(caller,resourceId);
        if (res==null)
            throw new StuffNotFoundException("Resource with id " + resourceId);

        // A resource type is set for the group, so only allow to add resources with the same type.
        if (resourceGroup.getResourceType()!=null) {
            if (!res.getResourceType().equals(resourceGroup.getResourceType()))
                return Response.status(Response.Status.CONFLICT).build();
        }

        // TODO if comp group and no resourceTypeId set, shall we allow to have it change to a mixed group?
        resourceGroup.addExplicitResource(res);

        return Response.ok().build(); // TODO right code?

    }

    @DELETE
    @Path("{id}/resource/{resourceId}")
    @ApiOperation("Remove the resource with the passed id from the group")
    public Response removeResource(@ApiParam("Id of the existing group") @PathParam("id") int id,
                                   @ApiParam("Id of the resource to remove") @PathParam("resourceId") int resourceId,
                                   @Context HttpHeaders headers, @Context UriInfo uriInfo) {

        ResourceGroup resourceGroup = fetchGroup(id, false);
        Resource res = resourceManager.getResource(caller, resourceId);
        if (res==null)
            throw new StuffNotFoundException("Resource with id " + resourceId);

        resourceGroup.removeExplicitResource(res);

        return Response.ok().build(); // TODO right code?

    }

    @GET
    @GZIP
    @Path("{id}/metricDefinitions")
    @ApiOperation(value = "Get the metric definitions for the compatible group with the passed id")
    public Response getMetricDefinitionsForGroup(@ApiParam(value = "Id of the group") @PathParam("id") int id,
                                                 @Context HttpHeaders headers,
                                                 @Context UriInfo uriInfo) {
        ResourceGroup group = fetchGroup(id, true);

        Set<MeasurementDefinition> definitions = group.getResourceType().getMetricDefinitions();
        List<MetricSchedule> schedules = new ArrayList<MetricSchedule>(definitions.size());
        for (MeasurementDefinition def : definitions) {
            MetricSchedule schedule = new MetricSchedule(def.getId(),def.getName(),def.getDisplayName(),false,def.getDefaultInterval(),
                    def.getUnits().getName(),def.getDataType().toString());
            if (def.getDataType()== DataType.MEASUREMENT) {
                UriBuilder uriBuilder = uriInfo.getBaseUriBuilder();
                uriBuilder.path("/metric/data/group/{groupId}/{definitionId}");
                URI uri = uriBuilder.build(id,def.getId());
                Link link = new Link("metric",uri.toString());
                schedule.addLink(link);
            }

            schedules.add(schedule);
        }

        MediaType mediaType = headers.getAcceptableMediaTypes().get(0);
        Response.ResponseBuilder builder;
        if (mediaType.equals(MediaType.TEXT_HTML_TYPE)) {
            builder = Response.ok(renderTemplate("listMetricDefinitions",schedules));
        }
        else {
            GenericEntity<List<MetricSchedule>> ret = new GenericEntity<List<MetricSchedule>>(schedules) {};
            builder = Response.ok(ret);
        }
        return builder.build();
    }

    @GZIP
    @GET
    @Path("/definitions")
    @ApiOperation(value="List all existing GroupDefinitions",multiValueResponse = true)
    public Response getGroupDefinitions(
            @ApiParam("String to search in the group definition name") @QueryParam("q") String q,
            @Context HttpHeaders headers,
            @Context UriInfo uriInfo) {

        ResourceGroupDefinitionCriteria criteria = new ResourceGroupDefinitionCriteria();
        if (q!=null) {
            criteria.addFilterName(q);
        }

        //Use CriteriaQuery to automatically chunk/page through criteria query results
        CriteriaQueryExecutor<GroupDefinition, ResourceGroupDefinitionCriteria> queryExecutor = new CriteriaQueryExecutor<GroupDefinition, ResourceGroupDefinitionCriteria>() {
            @Override
            public PageList<GroupDefinition> execute(ResourceGroupDefinitionCriteria criteria) {
                return definitionManager.findGroupDefinitionsByCriteria(caller, criteria);
            }
        };

        CriteriaQuery<GroupDefinition, ResourceGroupDefinitionCriteria> gdlist = new CriteriaQuery<GroupDefinition, ResourceGroupDefinitionCriteria>(
            criteria, queryExecutor);

        List<GroupDefinitionRest> list = new ArrayList<GroupDefinitionRest>();
        for (GroupDefinition def: gdlist) {
            GroupDefinitionRest definitionRest = buildGDRestFromDefinition(def);
            createLinksForGDRest(uriInfo,definitionRest);
            list.add(definitionRest);
        }

        MediaType mediaType = headers.getAcceptableMediaTypes().get(0);
        Response.ResponseBuilder builder;

        if (mediaType.equals(MediaType.TEXT_HTML_TYPE)) {
            builder = Response.ok(renderTemplate("listGroupDefinition", list), mediaType);
        }
        else {
            GenericEntity<List<GroupDefinitionRest>> ret = new GenericEntity<List<GroupDefinitionRest>>(list) {
            };
            builder = Response.ok(ret);
        }

        return builder.build();
    }

    @GZIP
    @GET
    @Path("/definition/{id}")
    @Cache(isPrivate = true,maxAge = 60)
    @ApiOperation(value = "Retrieve a single GroupDefinition by id")
    public Response getGroupDefinition(
            @ApiParam("The id of the definition to retrieve") @PathParam("id") int definitionId,
            @Context HttpHeaders headers, @Context UriInfo uriInfo) {

        try {
            GroupDefinition def = definitionManager.getById(definitionId);
            GroupDefinitionRest gdr = buildGDRestFromDefinition(def);

            createLinksForGDRest(uriInfo,gdr);

            MediaType mediaType = headers.getAcceptableMediaTypes().get(0);
            Response.ResponseBuilder builder;
            if (mediaType.equals(MediaType.TEXT_HTML_TYPE)) {
                builder = Response.ok(renderTemplate("groupDefinition", gdr), mediaType);
            }
            else {
                builder= Response.ok(gdr);
            }
            return builder.build();
        } catch (GroupDefinitionNotFoundException e) {
            throw new StuffNotFoundException("Group definition with id " + definitionId);
        }
    }

    private GroupDefinitionRest buildGDRestFromDefinition(GroupDefinition def) {
        GroupDefinitionRest gdr = new GroupDefinitionRest(def.getId(),def.getName(),def.getDescription(), def.getRecalculationInterval());
        gdr.setRecursive(def.isRecursive());

        List<Integer> generatedGroups;
        if (def.getManagedResourceGroups()!=null) {
            generatedGroups = new ArrayList<Integer>(def.getManagedResourceGroups().size());
            for (ResourceGroup group : def.getManagedResourceGroups() ) {
                generatedGroups.add(group.getId());
            }
        } else {
            generatedGroups = Collections.emptyList();
        }
        gdr.setGeneratedGroupIds(generatedGroups);
        gdr.setExpression(def.getExpressionAsList());
        return gdr;
    }

    @DELETE
    @Path("/definition/{id}")
    @ApiOperation("Delete the GroupDefinition with the passed id")
    public Response deleteGroupDefinition(
            @ApiParam("The id of the definition to delete") @PathParam("id") int definitionId,
            @Context HttpHeaders headers, @Context UriInfo uriInfo) {

        try {
            GroupDefinition def = definitionManager.getById(definitionId);
            definitionManager.removeGroupDefinition(caller,definitionId);
            return Response.noContent().build(); // Return 206, as we don't include a body
        } catch (GroupDefinitionNotFoundException e) {
            // Idem potent
            return Response.noContent().build(); // Return 206, as we don't include a body
        } catch (GroupDefinitionDeleteException e) {
            throw new StuffNotFoundException("Group definition with id " + definitionId);
        }
    }

    @POST
    @Path("/definitions")
    @Consumes({MediaType.APPLICATION_JSON,MediaType.APPLICATION_XML})
    @ApiOperation("Create a new GroupDefinition. The name of the group is required in the passed definition.")
    public Response createGroupDefinition(GroupDefinitionRest definition,
                                          @Context HttpHeaders headers,
                                          @Context UriInfo uriInfo) {

        Response.ResponseBuilder builder = null;

        if (definition.getName()==null||definition.getName().isEmpty()) {
            builder = Response.status(Response.Status.NOT_ACCEPTABLE);
            builder.entity("No name for the definition given");
        }
        if (builder!=null)
            return builder.build();


        GroupDefinition gd = new GroupDefinition(definition.getName());
        gd.setDescription(definition.getDescription());
        List<String> expressionList = definition.getExpression();
        StringBuilder sb = new StringBuilder();
        for(String e : expressionList ) {
            sb.append(e);
            sb.append("\n");
        }
        gd.setExpression(sb.toString());
        gd.setRecalculationInterval(definition.getRecalcInterval());
        gd.setRecursive(definition.isRecursive());

        try {
            GroupDefinition res = definitionManager.createGroupDefinition(caller,gd);
            UriBuilder uriBuilder = uriInfo.getBaseUriBuilder();
            uriBuilder.path("/group/definition/{id}");
            URI location = uriBuilder.build(res.getId());

            Link link = new Link("edit",location.toString());
            builder= Response.created(location);
            GroupDefinitionRest gdr = buildGDRestFromDefinition(res);
            createLinksForGDRest(uriInfo,gdr);

            builder.entity(gdr);

        } catch (GroupDefinitionAlreadyExistsException e) {
            builder =Response.status(Response.Status.CONFLICT);
            builder.entity(e.getMessage());
        } catch (GroupDefinitionCreateException e) {
            e.printStackTrace();  // TODO: Customise this generated block
            builder = Response.status(Response.Status.NOT_ACCEPTABLE);
            builder.entity(e.getMessage());
        }
        return builder.build();
    }

    @PUT
    @Path("/definition/{id}")
    @Consumes({MediaType.APPLICATION_JSON,MediaType.APPLICATION_XML})
    @ApiOperation("Update an existing GroupDefinition or recalculate it if the query param 'recalculate' is set to true")
    public Response updateGroupDefinition(@ApiParam("Id fo the definition to update") @PathParam("id") int definitionId,
                                          @ApiParam("If true, trigger a re-calculation") @QueryParam( "recalculate")
                                          @DefaultValue("false") boolean recalculate,
                                          GroupDefinitionRest definition, // TODO mark as optional?
                                          @Context HttpHeaders headers,
                                          @Context UriInfo uriInfo) {

        GroupDefinition gd;
        try {
            gd = definitionManager.getById(definitionId);
        } catch (GroupDefinitionNotFoundException e) {
            throw new StuffNotFoundException("Group Definition with id " + definitionId);
        }

        Response.ResponseBuilder builder = null;

        if (recalculate) {
            try {
                definitionManager.calculateGroupMembership(caller,gd.getId());
                builder = Response.noContent();
            } catch (Exception e) {
                builder = Response.status(Response.Status.NOT_ACCEPTABLE);
                builder.entity(e.getLocalizedMessage());
            }
            return builder.build();
        }

        // Not recalculation, but an update

        if (!definition.getName().isEmpty())
            gd.setName(definition.getName());
        gd.setDescription(definition.getDescription());
        List<String> expressionList = definition.getExpression();
        StringBuilder sb = new StringBuilder();
        for(String e : expressionList ) {
            sb.append(e);
            sb.append("\n");
        }
        gd.setExpression(sb.toString());

        gd.setRecalculationInterval(definition.getRecalcInterval());
        gd.setRecursive(definition.isRecursive());

        try {
            definitionManager.updateGroupDefinition(caller,gd);
        } catch (Exception e) {
            builder = Response.status(Response.Status.NOT_ACCEPTABLE);
            builder.entity(e.getLocalizedMessage());
            return builder.build();
        }


        try {
            // Re-fetch, as groups may have changed
            gd = definitionManager.getById(gd.getId());
            GroupDefinitionRest gdr = buildGDRestFromDefinition(gd);
            createLinksForGDRest(uriInfo, gdr);

            builder = Response.ok(gdr);
        } catch (GroupDefinitionNotFoundException e) {
            throw new StuffNotFoundException("Group Definition with id " + gd.getId());
        }

        return builder.build();
    }

    private void createLinksForGDRest(UriInfo uriInfo, GroupDefinitionRest gdr) {
        UriBuilder uriBuilder = uriInfo.getBaseUriBuilder();
        uriBuilder.path("/group/definition/{id}");
        URI location = uriBuilder.build(gdr.getId());
        Link link = new Link("edit",location.toString());
        gdr.addLink(link);

        uriBuilder = uriInfo.getBaseUriBuilder();
        uriBuilder.path("/group/definition");
        location = uriBuilder.build(new Object[]{});
        link = new Link("create",location.toString());
        gdr.addLink(link);
    }
}
