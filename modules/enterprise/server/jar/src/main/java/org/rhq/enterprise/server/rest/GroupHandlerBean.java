package org.rhq.enterprise.server.rest;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.interceptor.Interceptors;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
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
import org.rhq.core.domain.resource.group.ResourceGroup;
import org.rhq.enterprise.server.resource.ResourceManagerLocal;
import org.rhq.enterprise.server.resource.group.ResourceGroupDeleteException;
import org.rhq.enterprise.server.resource.group.ResourceGroupManagerLocal;
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
    @GET
    @Path("{id}")
    public Response getGroup(@PathParam("id") int id, @Context Request request, @Context HttpHeaders headers,
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
    @POST
    @Path("/")
    public Response createGroup(ResourceGroup group, @Context Request request, @Context HttpHeaders headers, @Context UriInfo uriInfo) {

        Response.ResponseBuilder builder;
        try {
            ResourceGroup newGroup = resourceGroupManager.createResourceGroup(caller,group);
            UriBuilder uriBuilder = uriInfo.getBaseUriBuilder();
            uriBuilder.path("/group/{id}");
            URI uri = uriBuilder.build(newGroup.getId());

            builder=Response.created(uri);
        } catch (Exception e) {
            e.printStackTrace();  // TODO: Customise this generated block
            builder=Response.serverError();
        }
        return builder.build();
    }

    @Override
    @PUT
    @Path("{id}")
    public Response updateGroup(@PathParam("id") int id, GroupRest in, @Context Request request,
                                @Context HttpHeaders headers,
                                @Context UriInfo uriInfo) {

        ResourceGroup resourceGroup = fetchGroup(id);
        resourceGroup.setName(in.getName());
        Response.ResponseBuilder builder;

        try {
            resourceGroup = resourceGroupManager.updateResourceGroup(caller,resourceGroup);
            builder=Response.ok(fillGroup(resourceGroup,uriInfo));
        }
        catch (Exception e) {
            builder = Response.status(Response.Status.NOT_ACCEPTABLE); // TODO correct?
        }
        return builder.build();
    }

    @Override
    @DELETE
    @Path("{id}")
    public Response deleteGroup(@PathParam("id") int id, @Context Request request, @Context HttpHeaders headers,
                                @Context UriInfo uriInfo) {

        try {
            resourceGroupManager.deleteResourceGroup(caller,id);
            return Response.ok().build();
        } catch (ResourceGroupDeleteException e) {
            e.printStackTrace();  // TODO: Customise this generated block
            return Response.serverError().build(); // TODO what exactly ?
        }
    }

    @Override
    @GET
    @Path("{id}/resources")  // TODO introduce paging through the list
    public Response getResources(@PathParam("id") int id, @Context Request request, @Context HttpHeaders headers,
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
    @PUT
    @Path("{id}/resource/{resourceId}")
    public Response addResource(@PathParam("id") int id, @PathParam("resourceId") int resourceId,
                                @Context Request request, @Context HttpHeaders headers, @Context UriInfo uriInfo) {

        ResourceGroup resourceGroup = fetchGroup(id);
        Resource res = resourceManager.getResource(caller,resourceId);
        if (res==null)
            throw new StuffNotFoundException("Resource with id " + resourceId);

        // TODO do we want/ need to check that the group category stays the same?

        resourceGroup.addExplicitResource(res);

        return Response.ok().build(); // TODO right code?

    }

    @Override
    @DELETE
    @Path("{id}/resource/{resourceId}")
    public Response removeResource(@PathParam("id") int id, @PathParam("resourceId") int resourceId,
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
        ResourceGroup resourceGroup = resourceGroupManager.getResourceGroup(caller, groupId);
        if (resourceGroup==null)
            throw new StuffNotFoundException("Group with id " + groupId);
        return resourceGroup;
    }



    private GroupRest fillGroup(ResourceGroup group, UriInfo uriInfo) {

        GroupRest gr = new GroupRest(group.getName());
        gr.setId(group.getId());
        gr.setCategory(group.getGroupCategory());
        UriBuilder uriBuilder = uriInfo.getBaseUriBuilder();
        uriBuilder.path("/group/{id}");
        URI uri = uriBuilder.build(group.getId());

        Link link = new Link("edit",uri.toASCIIString());
        gr.getLinks().add(link);

        return gr;
    }
}
