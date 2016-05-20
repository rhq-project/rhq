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

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.interceptor.Interceptors;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

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
import org.rhq.core.domain.util.PageOrdering;
import org.rhq.enterprise.server.resource.ResourceManagerLocal;
import org.rhq.enterprise.server.resource.ResourceTypeManagerLocal;
import org.rhq.enterprise.server.resource.ResourceTypeNotFoundException;
import org.rhq.enterprise.server.resource.group.ResourceGroupAlreadyExistsException;
import org.rhq.enterprise.server.resource.group.ResourceGroupDeleteException;
import org.rhq.enterprise.server.resource.group.ResourceGroupNotFoundException;
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

import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiError;
import com.wordnik.swagger.annotations.ApiErrors;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;

/**
 * Deal with group related things.
 * @author Heiko W. Rupp
 */
@Stateless
@Interceptors(SetCallerInterceptor.class)
@Path("/group")
@Api(value="Deal with groups and DynaGroups", description = "Api that deals with resource groups and group definitions")
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
    @ApiOperation(value = "List all groups", multiValueResponse = true, responseClass = "GroupRest")
    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public Response getGroups(@ApiParam("String to search in the group name") @QueryParam("q") String q,
                              @ApiParam("Page size for paging") @QueryParam("ps") @DefaultValue("20") int pageSize,
                              @ApiParam("Page number for paging, 0-based") @QueryParam("page") Integer page,
                              @Context HttpHeaders headers, @Context UriInfo uriInfo) {

        ResourceGroupCriteria criteria = new ResourceGroupCriteria();
        criteria.fetchGroupDefinition(true);
        criteria.addSortId(PageOrdering.ASC);

        if (q!=null) {
            criteria.addFilterName(q);
        }
        if (page!=null) {
            criteria.setPaging(page,pageSize);
        }

        PageList<ResourceGroup> groups = resourceGroupManager.findResourceGroupsByCriteria(caller, criteria);

        List<GroupRest> list = new ArrayList<GroupRest>();
        for (ResourceGroup group : groups) {
            list.add(fillGroup(group, uriInfo));
        }

        MediaType mediaType = headers.getAcceptableMediaTypes().get(0);
        Response.ResponseBuilder builder = Response.ok();
        builder.type(mediaType);

        if (mediaType.equals(MediaType.TEXT_HTML_TYPE)) {
            builder.entity(renderTemplate("listGroup", list));
        }
        else if (mediaType.equals(wrappedCollectionJsonType)) {
            wrapForPaging(builder,uriInfo,groups,list);
        }
        else {
            GenericEntity<List<GroupRest>> ret = new GenericEntity<List<GroupRest>>(list) {};
            builder.entity(ret);
            createPagingHeader(builder,uriInfo,groups);
        }


        return builder.build();

    }

    @Cache(isPrivate = true,maxAge = 60)
    @GET
    @Path("{id}")
    @ApiOperation(value = "Get the group with the passed id")
    @ApiError(code = 404, reason = "Group with passed id not found")
    public Response getGroup(@ApiParam(value = "Id of the group") @PathParam("id") int id,
                             @Context HttpHeaders headers,
                             @Context UriInfo uriInfo) {

        ResourceGroup group = fetchGroup(id, false);

        GroupRest groupRest = fillGroup(group, uriInfo);

        MediaType mediaType = headers.getAcceptableMediaTypes().get(0);

        Response.ResponseBuilder builder = Response.ok();
        builder.type(mediaType);

        if (mediaType.equals(MediaType.TEXT_HTML_TYPE)) {
            builder.entity(renderTemplate("group", groupRest));
        }
        else {
            builder.entity(groupRest);
        }

        return builder.build();
    }

    @POST
    @Path("/")
    @ApiOperation(value = "Create a new group")
    @ApiErrors({
        @ApiError(code = 404, reason = "Resource type for provided type id does not exist"),
        @ApiError(code = 406, reason = "No group provided"),
        @ApiError(code = 406, reason = "Provided group has no name")
    })
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
        UriBuilder uriBuilder = uriInfo.getBaseUriBuilder();
        uriBuilder.path("/group/{id}");

        try {
            newGroup = resourceGroupManager.createResourceGroup(caller, newGroup);
            URI uri = uriBuilder.build(newGroup.getId());

            builder=Response.created(uri);
        } catch (ResourceGroupAlreadyExistsException e) {

            ResourceGroupCriteria criteria = new ResourceGroupCriteria();
            criteria.setStrict(true);
            criteria.addFilterName(newGroup.getName());
            // TODO also case sensitive?
            List<ResourceGroup> groups = resourceGroupManager.findResourceGroupsByCriteria(caller,criteria);
            newGroup = groups.get(0);

            URI uri = uriBuilder.build(newGroup.getId());

            builder=Response.ok(uri);
        } catch (Exception e) {
            builder=Response.status(Response.Status.NOT_ACCEPTABLE);
            builder.type(mediaType);
            builder.entity(e.getCause());
        }

        builder.type(mediaType);
        builder.entity(fillGroup(newGroup,uriInfo));
        putToCache(newGroup.getId(),ResourceGroup.class,newGroup);

        return builder.build();
    }

    @PUT
    @Path("{id}")
    @ApiOperation(value = "Update the passed group. Currently only name change is supported")
    @ApiErrors({
        @ApiError(code = 404, reason = "Group with the passed id does not exist"),
        @ApiError(code = 406, reason = "Updating the name failed")
    })
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
            builder = Response.status(Response.Status.NOT_ACCEPTABLE);
        }

        MediaType mediaType = headers.getAcceptableMediaTypes().get(0);
        builder.type(mediaType);

        return builder.build();
    }

    @DELETE
    @Path("{id}")
    @ApiOperation(value="Delete the group with the passed id", notes = "This operation is by default idempotent, returning 204." +
        "If you want to check if the group existed at all, you need to pass the 'validate' query parameter.")
    @ApiErrors({
        @ApiError(code = 204, reason = "Group was deleted or did not exist with validation not set"),
        @ApiError(code = 404, reason = "Group did not exist and validate was set")
    })
    public Response deleteGroup(@ApiParam("Id of the group to delete") @PathParam("id") int id,
                                @ApiParam("Validate if the group exists") @QueryParam("validate") @DefaultValue("false") boolean validate) {

        Response.ResponseBuilder builder;
        try {
            resourceGroupManager.deleteResourceGroup(caller,id);
            removeFromCache(id,ResourceGroup.class);
            builder = Response.noContent();
        } catch (ResourceGroupNotFoundException e) {
            if (validate) {
                builder = Response.status(Response.Status.NOT_FOUND);
            } else {
                builder = Response.noContent();
            }
        } catch (ResourceGroupDeleteException e) {
            builder = Response.serverError();
            builder.entity(e.getMessage());
        }

        return builder.build();
    }

    @GZIP
    @GET
    @Path("{id}/resources")
    @Cache(isPrivate = true,maxAge = 60)
    @ApiOperation(value="Get the resources of the group", multiValueResponse = true, responseClass = "ResourceWithType")
    @ApiError(code = 404, reason = "Group with passed id does not exist")
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
    @ApiOperation(value="Add a resource to an existing group", notes = "If you have created the group as " +
        "a compatible group and a resource type was provided on creation, only resources with this type" +
        "may be added.")
    @ApiErrors({
            @ApiError(code = 404,reason = "If there is no resource or group with the passed id "),
            @ApiError(code = 409,reason =" Resource type does not match the group one")
    })
    public Response addResource(@ApiParam("Id of the existing group") @PathParam("id") int id,
                                @ApiParam("Id of the resource to add") @PathParam("resourceId") int resourceId,
                                @Context HttpHeaders headers, @Context UriInfo uriInfo) {

        MediaType mediaType = headers.getAcceptableMediaTypes().get(0);

        ResourceGroup resourceGroup = fetchGroup(id, false);
        Resource res = resourceManager.getResource(caller,resourceId);
        if (res==null)
            throw new StuffNotFoundException("Resource with id " + resourceId);

        // A resource type is set for the group, so only allow to add resources with the same type.
        if (resourceGroup.getResourceType()!=null) {
            if (!res.getResourceType().equals(resourceGroup.getResourceType())) {
                Response.ResponseBuilder status = Response.status(Response.Status.CONFLICT);
                status.type(mediaType);
                return status.build();
            }
        }

        // TODO if comp group and no resourceTypeId set, shall we allow to have it change to a mixed group?
        resourceGroupManager.addResourcesToGroup(caller,id,new int[]{resourceId});

        resourceGroup = fetchGroup(id, false);
        GroupRest gr = fillGroup(resourceGroup,uriInfo);

        Response.ResponseBuilder builder = Response.ok();
        builder.entity(gr);
        builder.type(mediaType);
        return builder.build();

    }

    @DELETE
    @Path("{id}/resource/{resourceId}")
    @ApiOperation(value = "Remove the resource with the passed id from the group", notes = "This operation is by default idempotent, returning 204" +
        "even if the resource was not member of the group." +
                        "If you want to check if the resource existed at all, you need to pass the 'validate' query parameter.")
    @ApiErrors({
        @ApiError(code = 404, reason = "Group with the passed id does not exist"),
        @ApiError(code = 404, reason = "Resource with the passed id does not exist"),
        @ApiError(code = 204, reason = "Resource was removed from the group or was no member and validation was not set"),
        @ApiError(code = 404, reason = "Resource was no member of the group and validate was set")
    })
    public Response removeResource(@ApiParam("Id of the existing group") @PathParam("id") int id,
                                   @ApiParam("Id of the resource to remove") @PathParam("resourceId") int resourceId,
                                   @ApiParam("Validate if the resource exists in the group") @QueryParam(
                                       "validate") @DefaultValue("false") boolean validate) {

        ResourceGroup resourceGroup = fetchGroup(id, false);
        Resource res = resourceManager.getResource(caller, resourceId);
        if (res==null)
            throw new StuffNotFoundException("Resource with id " + resourceId);

        boolean removed = resourceGroup.removeExplicitResource(res);
        if (!removed && validate) {
            throw new StuffNotFoundException("Resource " + resourceId + " in group " + id);
        }

        return Response.noContent().build();
    }

    @GET
    @GZIP
    @Path("{id}/metricDefinitions")
    @ApiOperation(value = "Get the metric definitions for the compatible group with the passed id")
    @ApiError(code = 404, reason = "Group with the passed id does not exist")
    public Response getMetricDefinitionsForGroup(@ApiParam(value = "Id of the group") @PathParam("id") int id,
                                                 @Context HttpHeaders headers,
                                                 @Context UriInfo uriInfo) {
        ResourceGroup group = fetchGroup(id, true);

        Set<MeasurementDefinition> definitions = group.getResourceType().getMetricDefinitions();
        List<MetricSchedule> schedules = new ArrayList<MetricSchedule>(definitions.size());
        for (MeasurementDefinition def : definitions) {
            MetricSchedule schedule = new MetricSchedule(def.getId(),def.getName(),def.getDisplayName(),false,def.getDefaultInterval(),
                    def.getUnits().getName(),def.getDataType().toString());
            schedule.setDefinitionId(def.getId());
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
    @ApiOperation(value="List all existing GroupDefinitions",multiValueResponse = true, responseClass = "GroupDefinitionRest")
    public Response getGroupDefinitions(
            @ApiParam("String to search in the group definition name") @QueryParam("q") String q,
            @Context HttpHeaders headers,
            @Context UriInfo uriInfo) {

        ResourceGroupDefinitionCriteria criteria = new ResourceGroupDefinitionCriteria();
        if (q!=null) {
            criteria.addFilterName(q);
        }

        PageList<GroupDefinition> gdlist = definitionManager.findGroupDefinitionsByCriteria(caller, criteria);

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
    @ApiOperation(value = "Retrieve a single GroupDefinition by id", responseClass = "GroupDefinitionRest")
    @ApiError(code = 404, reason = "Group definition with the passed id does not exist.")
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
    @ApiOperation(value = "Delete the GroupDefinition with the passed id", notes = "This operation is by default idempotent, returning 204." +
                        "If you want to check if the definition existed at all, you need to pass the 'validate' query parameter.")
    @ApiErrors({
        @ApiError(code = 204, reason = "Definition was deleted or did not exist with validation not set"),
        @ApiError(code = 404, reason = "Definition did not exist and validate was set")
    })
    public Response deleteGroupDefinition(
        @ApiParam("The id of the definition to delete") @PathParam("id") int definitionId,
        @ApiParam("Validate if the definition exists") @QueryParam("validate") @DefaultValue("false") boolean validate,
        @Context HttpHeaders headers) {

        Response.ResponseBuilder builder;
        try {
            GroupDefinition def = definitionManager.getById(definitionId);
            definitionManager.removeGroupDefinition(caller,definitionId);
            builder = Response.noContent();
        } catch (GroupDefinitionNotFoundException e) {
            if (validate) {
                builder = Response.status(Response.Status.NOT_FOUND);
                builder.entity("Definition with id " + definitionId);
            }
            else {
                builder = Response.noContent();
            }
        } catch (GroupDefinitionDeleteException e) {
            builder = Response.status(Response.Status.INTERNAL_SERVER_ERROR);
            builder.entity(e.getMessage());
        }
        MediaType type = headers.getAcceptableMediaTypes().get(0);
        builder.type(type);

        return builder.build();
    }

    @POST
    @Path("/definitions")
    @Consumes({MediaType.APPLICATION_JSON,MediaType.APPLICATION_XML})
    @ApiOperation(
        value = "Create a new GroupDefinition.", notes = "The name of the group is required in the passed " +
        "definition, as well as a non-empty expression. A recalcInterval of 0 means to never recalculate.")
    @ApiErrors({
        @ApiError(code = 406, reason = "Passed group definition has no name"),
        @ApiError(code = 406, reason = "Passed expression was empty"),
        @ApiError(code = 406, reason = "Recalculation interval is < 0 "),
        @ApiError(code = 409, reason = "There already exists a definition by this name"),
        @ApiError(code = 406, reason = "Group creation failed")
    })
    public Response createGroupDefinition(GroupDefinitionRest definition,
                                          @Context HttpHeaders headers,
                                          @Context UriInfo uriInfo) {

        Response.ResponseBuilder builder = null;

        if (definition.getName()==null||definition.getName().trim().isEmpty()) {
            builder = Response.status(Response.Status.NOT_ACCEPTABLE);
            builder.entity("No name for the definition given");
        }
        if (builder!=null)
            return builder.build();


        GroupDefinition gd = new GroupDefinition(definition.getName());
        gd.setDescription(definition.getDescription());
        addGroupDefinitionExpression(definition, gd);
        if (definition.getRecalcInterval() < 0 ) {
            throw new BadArgumentException("Recalculation interval must be >= 0");
        }
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

    private void addGroupDefinitionExpression(GroupDefinitionRest definition, GroupDefinition gd) {
        boolean isEmpty = false;
        List<String> expressionList = definition.getExpression();
        if (expressionList.isEmpty()) {
            isEmpty=true;
        }
        StringBuilder sb = new StringBuilder();
        int countEmpty = 0;
        for(String e : expressionList ) {
            if (e==null) {
                countEmpty++;
                continue;
            }
            sb.append(e);
            if (e.trim().isEmpty()) {
                countEmpty++;
            }
            sb.append("\n");
        }
        if (countEmpty == expressionList.size()) {
            isEmpty = true;
        }
        if (isEmpty) {
            throw new BadArgumentException("The expression must not be empty");
        }
        gd.setExpression(sb.toString());
    }

    @PUT
    @Path("/definition/{id}")
    @Consumes({MediaType.APPLICATION_JSON,MediaType.APPLICATION_XML})
    @ApiOperation(value = "Update or recalculate an existing GroupDefinition",
        notes = "If the query param 'recalculate' is set to true, the group with the passed id is recalculated. " +
            "Otherwise the existing group will be updated with the passed definition. The expression in the " +
            "definition must be empty. If the name is emtpy, the old name is kept. A recalcInterval" +
            "of 0 means no recalculation.")
    @ApiErrors({
        @ApiError(code = 404, reason = "Group with the passed id does not exist"),
        @ApiError(code = 406, reason = "Passed expression was empty"),
        @ApiError(code = 406, reason = "Recalculation interval is < 0 "),
        @ApiError(code = 406, reason = "Group membership calculation failed")
    })
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

        if (!definition.getName().isEmpty()) {
            gd.setName(definition.getName());
        }
        gd.setDescription(definition.getDescription());
        addGroupDefinitionExpression(definition,gd);
        if (definition.getRecalcInterval() < 0 ) {
            throw new BadArgumentException("Recalculation interval must be >= 0");
        }
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
            builder = Response.status(Response.Status.INTERNAL_SERVER_ERROR);
            builder.entity("Group Definition with id " + gd.getId());
        }

        MediaType mediaType = headers.getAcceptableMediaTypes().get(0);
        builder.type(mediaType);
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
