package org.rhq.enterprise.server.rest;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.interceptor.Interceptors;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.domain.criteria.ResourceGroupCriteria;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.resource.group.GroupDefinition;
import org.rhq.core.domain.resource.group.ResourceGroup;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.server.resource.ResourceManagerLocal;
import org.rhq.enterprise.server.resource.ResourceTypeManagerLocal;
import org.rhq.enterprise.server.resource.ResourceTypeNotFoundException;
import org.rhq.enterprise.server.resource.group.ResourceGroupDeleteException;
import org.rhq.enterprise.server.resource.group.ResourceGroupManagerLocal;
import org.rhq.enterprise.server.resource.group.definition.GroupDefinitionManagerLocal;
import org.rhq.enterprise.server.resource.group.definition.exception.GroupDefinitionAlreadyExistsException;
import org.rhq.enterprise.server.resource.group.definition.exception.GroupDefinitionCreateException;
import org.rhq.enterprise.server.resource.group.definition.exception.GroupDefinitionDeleteException;
import org.rhq.enterprise.server.resource.group.definition.exception.GroupDefinitionNotFoundException;
import org.rhq.enterprise.server.rest.domain.GroupDefinitionRest;
import org.rhq.enterprise.server.rest.domain.GroupRest;
import org.rhq.enterprise.server.rest.domain.Link;
import org.rhq.enterprise.server.rest.domain.ResourceWithType;

/**
 * Deal with group related things.
 * @author Heiko W. Rupp
 */
@Stateless
@Interceptors(SetCallerInterceptor.class)
public class GroupHandlerBean extends AbstractRestBean implements GroupHandlerLocal {

    private final Log log = LogFactory.getLog(GroupHandlerBean.class);

    @EJB
    ResourceGroupManagerLocal resourceGroupManager;

    @EJB
    ResourceManagerLocal resourceManager;

    @EJB
    ResourceTypeManagerLocal resourceTypeManager;

    @EJB
    GroupDefinitionManagerLocal definitionManager;

    public Response getGroups(@Context Request request, @Context HttpHeaders headers, @Context UriInfo uriInfo) {

        ResourceGroupCriteria criteria = new ResourceGroupCriteria();
        List<ResourceGroup> groups = resourceGroupManager.findResourceGroupsByCriteria(caller,criteria);

        List<GroupRest> list = new ArrayList<GroupRest>(groups.size());
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

    @Override
    public Response getGroup(int id, @Context Request request, @Context HttpHeaders headers,
                             @Context UriInfo uriInfo) {

        ResourceGroup group = fetchGroup(id);

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

    @Override
    public Response createGroup(GroupRest group, @Context Request request, @Context HttpHeaders headers, @Context UriInfo uriInfo) {

        ResourceGroup newGroup = new ResourceGroup(group.getName());
        if (group.getResourceTypeId()!=null) {

            ResourceType resourceType = null;
            try {
                resourceType = resourceTypeManager.getResourceTypeById(caller,group.getResourceTypeId());
                newGroup.setResourceType(resourceType);
            } catch (ResourceTypeNotFoundException e) {
                e.printStackTrace();  // TODO: Customise this generated block
                throw new StuffNotFoundException("ResourceType with id " + group.getResourceTypeId());
            }
        }

        Response.ResponseBuilder builder;
        try {
            newGroup = resourceGroupManager.createResourceGroup(caller,newGroup);
            UriBuilder uriBuilder = uriInfo.getBaseUriBuilder();
            uriBuilder.path("/group/{id}");
            URI uri = uriBuilder.build(newGroup.getId());

            builder=Response.created(uri);
            putToCache(newGroup.getId(),ResourceGroup.class,newGroup);
        } catch (Exception e) {
            e.printStackTrace();  // TODO: Customise this generated block
            builder=Response.status(Response.Status.NOT_ACCEPTABLE);
        }
        return builder.build();
    }

    @Override
    public Response updateGroup(int id, GroupRest in, @Context Request request,
                                @Context HttpHeaders headers,
                                @Context UriInfo uriInfo) {

        ResourceGroup resourceGroup = fetchGroup(id);
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

    @Override
    public Response deleteGroup(int id, @Context Request request, @Context HttpHeaders headers,
                                @Context UriInfo uriInfo) {

        try {
            resourceGroupManager.deleteResourceGroup(caller,id);
            removeFromCache(id,ResourceGroup.class);
            return Response.ok().build();
        } catch (ResourceGroupDeleteException e) {
            e.printStackTrace();  // TODO: Customise this generated block
            return Response.serverError().build(); // TODO what exactly ?
        }
    }

    @Override
    public Response getResources(int id, @Context Request request, @Context HttpHeaders headers,
                                 @Context UriInfo uriInfo) {

        ResourceGroup resourceGroup = fetchGroup(id);

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

    @Override
    public Response addResource(int id, int resourceId,
                                @Context Request request, @Context HttpHeaders headers, @Context UriInfo uriInfo) {

        ResourceGroup resourceGroup = fetchGroup(id);
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

    @Override
    public Response removeResource(int id, int resourceId,
                                   @Context Request request, @Context HttpHeaders headers, @Context UriInfo uriInfo) {

        ResourceGroup resourceGroup = fetchGroup(id);
        Resource res = resourceManager.getResource(caller,resourceId);
        if (res==null)
            throw new StuffNotFoundException("Resource with id " + resourceId);

        resourceGroup.removeExplicitResource(res);

        return Response.ok().build(); // TODO right code?

    }

    /**
     * Fetch the group with the passed id
     * @param groupId id of the resource group
     * @return the group object if found
     * @throws StuffNotFoundException if the group is not found (or not accessible by the caller)
     */
    private ResourceGroup fetchGroup(int groupId) {
        ResourceGroup resourceGroup;
        resourceGroup = getFromCache(groupId,ResourceGroup.class);
        if (resourceGroup==null) {
            resourceGroup = resourceGroupManager.getResourceGroup(caller, groupId);
            if (resourceGroup==null)
                throw new StuffNotFoundException("Group with id " + groupId);
            putToCache(groupId,ResourceGroup.class,resourceGroup);
        }
        return resourceGroup;
    }



    private GroupRest fillGroup(ResourceGroup group, UriInfo uriInfo) {

        GroupRest gr = new GroupRest(group.getName());
        gr.setId(group.getId());
        gr.setCategory(group.getGroupCategory());
        gr.setRecursive(group.isRecursive());
        UriBuilder uriBuilder = uriInfo.getBaseUriBuilder();
        uriBuilder.path("/group/{id}");
        URI uri = uriBuilder.build(group.getId());

        Link link = new Link("edit",uri.toASCIIString());
        gr.getLinks().add(link);

        return gr;
    }

    @Override
    public Response getDefinitions(@Context Request request, @Context HttpHeaders headers, @Context UriInfo uriInfo) {

        PageList<GroupDefinition> gdlist =  definitionManager.getGroupDefinitions(caller,new PageControl());
        List<GroupDefinitionRest> list = new ArrayList<GroupDefinitionRest>(gdlist.getTotalSize());
        for (GroupDefinition def: gdlist) {
            GroupDefinitionRest definitionRest = new GroupDefinitionRest(def.getId(),def.getName(),def.getDescription(),
                    def.getRecalculationInterval());
            definitionRest.setExpression(def.getExpressionAsList());

            List<Integer> generatedGroups = new ArrayList<Integer>(def.getManagedResourceGroups().size());
            for (ResourceGroup group : def.getManagedResourceGroups() ) {
                generatedGroups.add(group.getId());
            }
            definitionRest.setGeneratedGroupIds(generatedGroups);
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

    @Override
    public Response getDefinition(int definitionId, @Context Request request,
                                  @Context HttpHeaders headers, @Context UriInfo uriInfo) {

        try {
            GroupDefinition def = definitionManager.getById(definitionId);
            GroupDefinitionRest gdr = new GroupDefinitionRest(def.getId(),def.getName(),def.getDescription(), def.getRecalculationInterval());
            gdr.setRecursive(def.isRecursive());
            List<Integer> generatedGroups = new ArrayList<Integer>(def.getManagedResourceGroups().size());
            for (ResourceGroup group : def.getManagedResourceGroups() ) {
                generatedGroups.add(group.getId());
            }
            gdr.setGeneratedGroupIds(generatedGroups);
            gdr.setExpression(def.getExpressionAsList());

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

    @Override
    public Response deleteDefinition(int definitionId, @Context Request request,
                                     @Context HttpHeaders headers, @Context UriInfo uriInfo) {

        try {
            GroupDefinition def = definitionManager.getById(definitionId);
            definitionManager.removeGroupDefinition(caller,definitionId);
            return Response.ok().build();
        } catch (GroupDefinitionNotFoundException e) {
            // Idem potent
            return Response.ok().build();
        } catch (GroupDefinitionDeleteException e) {
            throw new StuffNotFoundException("Group definition with id " + definitionId);
        }
    }

    @Override
    public Response createDefinition(GroupDefinitionRest definition,
                                     @Context Request request, @Context HttpHeaders headers,
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
            builder= Response.created(location);

        } catch (GroupDefinitionAlreadyExistsException e) {
            builder =Response.status(Response.Status.CONFLICT);
        } catch (GroupDefinitionCreateException e) {
            e.printStackTrace();  // TODO: Customise this generated block
            builder = Response.status(Response.Status.INTERNAL_SERVER_ERROR);
        }
        return builder.build();
    }

    @Override
    public Response updateDefinition(int definitionId,
                                     boolean recalculate, GroupDefinitionRest definition,
                                     @Context Request request, @Context HttpHeaders headers, @Context UriInfo uriInfo) {

        GroupDefinition gd;
        try {
            gd = definitionManager.getById(definitionId);
        } catch (GroupDefinitionNotFoundException e) {
            throw new StuffNotFoundException("Group Definition with id " + definitionId);
        }

        Response.ResponseBuilder builder = null;

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
            e.printStackTrace();  // TODO: Customise this generated block
            builder = Response.status(Response.Status.NOT_ACCEPTABLE);
            builder.entity(e.getLocalizedMessage());
            return builder.build();
        }

        String msg=null;
        if (recalculate) {
            try {
                definitionManager.calculateGroupMembership(caller,gd.getId());
            } catch (Exception e) {
                msg = e.getLocalizedMessage();
//                e.printStackTrace();  // TODO: Customise this generated block
            }
        }
        builder = Response.ok(gd);
        if (msg!=null) {
            builder.entity(msg);
        }

        return builder.build();
    }
}
