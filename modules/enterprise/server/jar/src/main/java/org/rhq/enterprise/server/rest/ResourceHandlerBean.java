/*
 * RHQ Management Platform
 * Copyright (C) 2005-2012 Red Hat, Inc.
 * All rights reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2, as
 * published by the Free Software Foundation, and/or the GNU Lesser
 * General Public License, version 2.1, also as published by the Free
 * Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License and the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License
 * and the GNU Lesser General Public License along with this program;
 * if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */
package org.rhq.enterprise.server.rest;

import java.net.URI;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.interceptor.Interceptors;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
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
import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiError;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;

import org.jboss.resteasy.annotations.GZIP;
import org.jboss.resteasy.annotations.cache.Cache;
import org.jboss.resteasy.links.AddLinks;
import org.jboss.resteasy.links.LinkResource;

import org.rhq.core.domain.alert.Alert;
import org.rhq.core.domain.criteria.AlertCriteria;
import org.rhq.core.domain.criteria.AvailabilityCriteria;
import org.rhq.core.domain.criteria.ResourceCriteria;
import org.rhq.core.domain.discovery.AvailabilityReport;
import org.rhq.core.domain.measurement.Availability;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.domain.measurement.DataType;
import org.rhq.core.domain.measurement.MeasurementDefinition;
import org.rhq.core.domain.measurement.MeasurementSchedule;
import org.rhq.core.domain.resource.Agent;
import org.rhq.core.domain.resource.InventoryStatus;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceCategory;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.core.domain.util.PageOrdering;
import org.rhq.enterprise.server.RHQConstants;
import org.rhq.enterprise.server.alert.AlertManagerLocal;
import org.rhq.enterprise.server.core.AgentManagerLocal;
import org.rhq.enterprise.server.measurement.AvailabilityManagerLocal;
import org.rhq.enterprise.server.measurement.MeasurementScheduleManagerLocal;
import org.rhq.enterprise.server.resource.ResourceAlreadyExistsException;
import org.rhq.enterprise.server.resource.ResourceTypeManagerLocal;
import org.rhq.enterprise.server.rest.domain.*;

/**
 * Class that deals with getting data about resources
 * @author Heiko W. Rupp
 */
@Produces({MediaType.APPLICATION_JSON,MediaType.APPLICATION_XML,MediaType.TEXT_HTML})
@Path("/resource")
@Api(value="Resource related", description = "This endpoint deals with individual resources, not resource groups")
@Interceptors(SetCallerInterceptor.class)
@Stateless
public class ResourceHandlerBean extends AbstractRestBean {

    private static final String NO_RESOURCE_FOR_ID = "If no resource with the passed id exists";

    @EJB
    AvailabilityManagerLocal availMgr;
    @EJB
    MeasurementScheduleManagerLocal scheduleManager;
    @EJB
    AlertManagerLocal alertManager;
    @EJB
    ResourceTypeManagerLocal resourceTypeManager;
    @EJB
    AgentManagerLocal agentMgr;

    @PersistenceContext(unitName = RHQConstants.PERSISTENCE_UNIT_NAME)
    private EntityManager entityManager;

    @GET
    @Path("/{id:\\d+}")
    @Cache(isPrivate = true,maxAge = 120)
    @ApiOperation(value = "Retrieve a single resource", responseClass = "ResourceWithType")
    @ApiError(code = 404, reason = NO_RESOURCE_FOR_ID)
    public Response getResource(@ApiParam("Id of the resource to retrieve") @PathParam("id") int id,
                @Context Request request, @Context HttpHeaders headers,
                             @Context UriInfo uriInfo) {

        Resource res;
        res = fetchResource(id);

        long mtime = res.getMtime();
        EntityTag eTag = new EntityTag(Long.toOctalString(res.hashCode() + mtime)); // factor in mtime in etag
        Response.ResponseBuilder builder = request.evaluatePreconditions(new Date(mtime), eTag);

        if (builder != null) {
            return builder.build();
        }

        ResourceWithType rwt = fillRWT(res, uriInfo);

        // What media type does the user request?
        MediaType mediaType = headers.getAcceptableMediaTypes().get(0);

        if (mediaType.equals(MediaType.TEXT_HTML_TYPE)) {
            builder = Response.ok(renderTemplate("resourceWithType", rwt), mediaType);
        } else {
            builder = Response.ok(rwt);
        }

        return builder.build();
    }

    @GET @GZIP
    @Path("/")
    @ApiOperation(value = "Search for resources by the given search string, possibly limited by category and paged", responseClass = "ResourceWithType")
    public Response getResourcesByQuery(@ApiParam("String to search in the resource name") @QueryParam("q") String q,
                                        @ApiParam("Limit to category (PLATFORM, SERVER, SERVICE") @QueryParam("category") String category,
                                        @ApiParam("Page size for paging") @QueryParam("ps") @DefaultValue("20") int pageSize,
                                        @ApiParam("Page for paging") @QueryParam("page") Integer page,
                                        @Context HttpHeaders headers,
                                        @Context UriInfo uriInfo) {
        ResourceCriteria criteria = new ResourceCriteria();
        if (q!=null) {
            criteria.addFilterName(q);
        }
        if (category!=null) {
            criteria.addFilterResourceCategories(ResourceCategory.valueOf(category.toUpperCase()));
        }
        if (page!=null) {
            criteria.setPaging(page,pageSize);
            criteria.addSortName(PageOrdering.ASC);
        }
        PageList<Resource> ret = resMgr.findResourcesByCriteria(caller,criteria);

        Response.ResponseBuilder builder = getResponseBuilderForResourceList(headers,uriInfo,ret, page, pageSize);

        return builder.build();
    }

    @GZIP
    @GET
    @Path("/platforms")
    @Cache(isPrivate = true,maxAge = 300)
    @ApiOperation(value = "List all platforms in the system", multiValueResponse = true, responseClass = "ResourceWithType")
    public Response getPlatforms(@Context HttpHeaders headers,
                                 @Context UriInfo uriInfo) {

        PageControl pc = new PageControl();
        PageList<Resource> ret = resMgr.findResourcesByCategory(caller, ResourceCategory.PLATFORM,
            InventoryStatus.COMMITTED, pc);
        Response.ResponseBuilder builder = getResponseBuilderForResourceList(headers, uriInfo, ret, null, 20);

        return builder.build();
    }

    /**
     * Translate the passed list of resources into a response according to the acceptable mime types etc.
     *
     *
     * @param headers HttpHeaders from the request
     * @param uriInfo Uri from the request
     * @param resources List of resources
     * @param page Page of pageSize. If null, paging is ignored
     * @param pageSize numer of elements on a page
     * @return An initialized ResponseBuilder
     */
    private Response.ResponseBuilder getResponseBuilderForResourceList(HttpHeaders headers, UriInfo uriInfo,
                                                                       PageList<Resource> resources, Integer page,
                                                                       int pageSize) {
        List<ResourceWithType> rwtList = new ArrayList<ResourceWithType>(resources.size());
        for (Resource r : resources) {
            putToCache(r.getId(), Resource.class, r);
            ResourceWithType rwt = fillRWT(r, uriInfo);
            rwtList.add(rwt);
        }
        // What media type does the user request?
        MediaType mediaType = headers.getAcceptableMediaTypes().get(0);
        Response.ResponseBuilder builder = Response.ok();
        builder.type(mediaType);
        UriBuilder uriBuilder;
        if (page!=null) {

            // TODO look a the page control and check if there is a next page at all
            if (resources.getTotalSize()> page*pageSize) {
                int nextPage = page+1;
                uriBuilder = uriInfo.getRequestUriBuilder(); // adds ?q, ?ps and ?category if needed
                uriBuilder.replaceQueryParam("page",nextPage);

                builder.header("Link",new Link("next",uriBuilder.build().toString()));
            }

            if (page>1) {
                int prevPage = page -1;
                uriBuilder = uriInfo.getRequestUriBuilder(); // adds ?q, ?ps and ?category if needed
                uriBuilder.replaceQueryParam("page",prevPage);
                builder.header("prev",uriBuilder.build().toString());
            }
        }

        if (mediaType.equals(MediaType.TEXT_HTML_TYPE)) {
            builder.entity(renderTemplate("listResourceWithType", rwtList));

        } else {
            GenericEntity<List<ResourceWithType>> list = new GenericEntity<List<ResourceWithType>>(rwtList) {
            };
            builder.entity(list);
        }
        return builder;
    }

    @GET @GZIP
    @Path("/{id}/hierarchy")
    @Produces({"application/json","application/xml"})
    @ApiOperation(value = "Retrieve the hierarchy of resources starting with the passed one", multiValueResponse = true, responseClass = "ResourceWithType")
    @ApiError(code = 404, reason = NO_RESOURCE_FOR_ID)
    public ResourceWithChildren getHierarchy(@ApiParam("Id of the resource to start with") @PathParam("id")int baseResourceId) {
        // TODO optimize to do less recursion
        Resource start = obtainResource(baseResourceId);
        return getHierarchy(start);
    }

    private ResourceWithChildren getHierarchy(Resource baseResource) {
        ResourceWithChildren rwc = new ResourceWithChildren("" + baseResource.getId(), baseResource.getName());

        PageControl pc = new PageControl();
        List<Resource> ret = resMgr.findResourceByParentAndInventoryStatus(caller, baseResource,
            InventoryStatus.COMMITTED, pc);
        if (!ret.isEmpty()) {
            List<ResourceWithChildren> resList = new ArrayList<ResourceWithChildren>(ret.size());
            for (Resource res : ret) {
                ResourceWithChildren child = getHierarchy(res);
                resList.add(child);
                putToCache(res.getId(), Resource.class, res);
            }
            if (!resList.isEmpty())
                rwc.setChildren(resList);
        }
        return rwc;
    }

    @GET
    @Path("/{id}/availability")
    @ApiError(code = 404, reason = NO_RESOURCE_FOR_ID)
    @ApiOperation(value = "Return the current availability for the passed resource", responseClass = "AvailabilityRest")
    public Response getAvailability(@ApiParam("Id of the resource to query") @PathParam("id") int resourceId, @Context HttpHeaders headers) {

        Availability avail = availMgr.getCurrentAvailabilityForResource(caller, resourceId);
        AvailabilityRest availabilityRest;
        if (avail.getAvailabilityType() != null)
            availabilityRest = new AvailabilityRest(avail.getAvailabilityType(), avail.getStartTime(), avail
                .getResource().getId());
        else
            availabilityRest = new AvailabilityRest(avail.getStartTime(), resourceId);

        MediaType mediaType = headers.getAcceptableMediaTypes().get(0);
        Response.ResponseBuilder builder;

        if (mediaType.equals(MediaType.TEXT_HTML_TYPE)) {
            builder = Response.ok(renderTemplate("availability.ftl",availabilityRest), mediaType);
        } else {
            builder = Response.ok(availabilityRest);
        }
        return builder.build();
    }

    @GZIP
    @GET
    @Path("/{id}/availability/history")
    @ApiError(code = 404, reason = NO_RESOURCE_FOR_ID)
    @ApiOperation(value = "Return the availability history for the passed resource", responseClass = "AvailabilityRest", multiValueResponse = true)
    public Response getAvailabilityHistory(
            @ApiParam("Id of the resource to query") @PathParam("id") int resourceId,
            @ApiParam(value="Start time", defaultValue = "30 days ago") @QueryParam("start") long start,
            @ApiParam(value="End time", defaultValue = "Now") @QueryParam("end") long end,
            @Context HttpHeaders headers) {
        if (end==0)
            end = System.currentTimeMillis();

        if (start==0)
            start = end - (30*86400*1000L); // 30 days

        AvailabilityCriteria criteria = new AvailabilityCriteria();
        criteria.addFilterInterval(start,end);
        criteria.addFilterResourceId(resourceId);
        criteria.addSortStartTime(PageOrdering.DESC);
        List<Availability> points = availMgr.findAvailabilityByCriteria(caller,criteria);
        List<AvailabilityRest> ret = new ArrayList<AvailabilityRest>(points.size());
        for (Availability avail : points) {
            AvailabilityRest availabilityRest;
            if (avail.getAvailabilityType() != null) {
                availabilityRest = new AvailabilityRest(avail.getAvailabilityType(), avail.getStartTime(), avail
                    .getResource().getId());
            }
            else {
                availabilityRest = new AvailabilityRest(avail.getStartTime(), resourceId);
            }
            if (avail.getEndTime()!=null)
                availabilityRest.setUntil(avail.getEndTime());
            ret.add(availabilityRest);
        }
        MediaType mediaType = headers.getAcceptableMediaTypes().get(0);
        Response.ResponseBuilder builder;

        if (mediaType.equals(MediaType.TEXT_HTML_TYPE)) {
            builder = Response.ok(renderTemplate("listAvailability.ftl",ret), mediaType);
        } else {
            GenericEntity<List<AvailabilityRest>> availabilityRest = new GenericEntity<List<AvailabilityRest>>(ret) {};
            builder = Response.ok(availabilityRest);
        }
        return builder.build();

    }


    @PUT
    @Path("/{id}/availability")
    @ApiOperation("Set the current availability of the passed resource")
    @TransactionAttribute(TransactionAttributeType.NEVER)
    public void reportAvailability(@ApiParam("Id of the resource to update") @PathParam("id") int resourceId,
                @ApiParam(value= "New Availability setting", required = true) AvailabilityRest avail) {
        if (avail.getResourceId() != resourceId)
            throw new IllegalArgumentException("Resource Ids do not match");

        Resource resource = obtainResource(resourceId);

        AvailabilityType at;
        at = AvailabilityType.valueOf(avail.getType());

        // According to jshaughn, plaforms must not be set to DISABLED, so catch this case here.
        if (resource.getResourceType().getCategory()==ResourceCategory.PLATFORM && at==AvailabilityType.DISABLED) {
            throw new BadArgumentException("Availability","Platforms must not be set to DISABLED");
        }

        Agent agent = agentMgr.getAgentByResourceId(caller,resourceId);

        AvailabilityReport report = new AvailabilityReport(true, agent.getName());
        Availability availability = new Availability(resource, avail.getSince(), at);
        report.addAvailability(availability);

        availMgr.mergeAvailabilityReport(report);
    }

    @GZIP
    @GET
    @Path("/{id}/schedules")
    @LinkResource(rel="schedules",value = MetricSchedule.class)
    @Cache(isPrivate = true,maxAge = 60)
    @ApiOperation(value ="Get the metric schedules of the passed resource id", multiValueResponse = true, responseClass = "MetricSchedule")
    @ApiError(code = 404, reason = NO_RESOURCE_FOR_ID)
    public Response getSchedules(
            @ApiParam("Id of the resource to obtain the schedules for") @PathParam("id") int resourceId,
            @ApiParam(value = "Limit by type",
                    allowableValues = "<empty>, all, metric, trait, measurement") @QueryParam("type") @DefaultValue(
                    "all") String scheduleType,
            @ApiParam(value = "Limit by enabled schedules") @QueryParam("enabledOnly") @DefaultValue(
                    "true") boolean enabledOnly,
            @ApiParam(value = "Limit by name") @QueryParam("name") String name,
            @Context HttpHeaders headers,
            @Context UriInfo uriInfo) {

        // allow metric as input
        if (scheduleType.equals("metric"))
            scheduleType = DataType.MEASUREMENT.toString().toLowerCase();

        Resource res = resMgr.getResource(caller, resourceId); // Don't fetch(), as this would yield a LazyLoadException

        Set<MeasurementSchedule> schedules = res.getSchedules();
        List<MetricSchedule> ret = new ArrayList<MetricSchedule>(schedules.size());
        for (MeasurementSchedule schedule : schedules) {
            putToCache(schedule.getId(), MeasurementSchedule.class, schedule);
            MeasurementDefinition definition = schedule.getDefinition();

            // user can opt to e.g. only get "measurement" or "trait" metrics

            if ("all".equals(scheduleType)
                || scheduleType.toLowerCase().equals(definition.getDataType().toString().toLowerCase())) {
                if (!enabledOnly || (enabledOnly && schedule.isEnabled())) {
                    if (name == null || (name != null && name.equals(definition.getName()))) {
                        MetricSchedule ms = new MetricSchedule(schedule.getId(), definition.getName(),
                            definition.getDisplayName(), schedule.isEnabled(), schedule.getInterval(), definition
                                .getUnits().toString(), definition.getDataType().toString());
                        UriBuilder uriBuilder;
                        URI uri;
                        if (definition.getDataType() == DataType.MEASUREMENT) {
                            uriBuilder = uriInfo.getBaseUriBuilder();
                            uriBuilder.path("/metric/data/{id}");
                            uri = uriBuilder.build(schedule.getId());
                            Link metricLink = new Link("metric", uri.toString());
                            ms.addLink(metricLink);
                            uriBuilder = uriInfo.getBaseUriBuilder();
                            uriBuilder.path("/metric/data/{id}/raw");
                            uri = uriBuilder.build(schedule.getId());
                            metricLink = new Link("metric-raw", uri.toString());
                            ms.addLink(metricLink);
                        }
                        // create link to the resource
                        uriBuilder = uriInfo.getBaseUriBuilder();
                        uriBuilder.path("resource/" + schedule.getResource().getId());
                        uri = uriBuilder.build();
                        Link link = new Link("resource", uri.toString());
                        ms.addLink(link);

                        ret.add(ms);
                    }
                }
            }
        }

        // What media type does the user request?
        MediaType mediaType = headers.getAcceptableMediaTypes().get(0);
        Response.ResponseBuilder builder;

        if (mediaType.equals(MediaType.TEXT_HTML_TYPE)) {
            builder = Response.ok(renderTemplate("listMetricSchedule", ret), mediaType);
        } else {
            GenericEntity<List<MetricSchedule>> list = new GenericEntity<List<MetricSchedule>>(ret) {
            };
            builder = Response.ok(list, mediaType);
        }

        return builder.build();
    }

    @GZIP
    @GET
    @Path("/{id}/children")
    @LinkResource(rel="children", value = ResourceWithType.class)
    @ApiOperation(value = "Get the direct children of the passed resource")
    @ApiError(code = 404, reason = NO_RESOURCE_FOR_ID)
    public Response getChildren(
            @ApiParam("Id of the resource to get children") @PathParam("id") int id,
            @Context HttpHeaders headers,
            @Context UriInfo uriInfo) {

        PageControl pc = new PageControl();
        Resource parent;
        parent = fetchResource(id);
        List<Resource> ret = resMgr.findResourceByParentAndInventoryStatus(caller, parent, InventoryStatus.COMMITTED,
            pc);
        List<ResourceWithType> rwtList = new ArrayList<ResourceWithType>(ret.size());
        for (Resource r : ret) {
            ResourceWithType rwt = fillRWT(r, uriInfo);
            rwtList.add(rwt);
        }

        // What media type does the user request?
        MediaType mediaType = headers.getAcceptableMediaTypes().get(0);
        Response.ResponseBuilder builder;

        if (mediaType.equals(MediaType.TEXT_HTML_TYPE)) {
            builder = Response.ok(renderTemplate("listResourceWithType", rwtList), mediaType);
        } else {
            GenericEntity<List<ResourceWithType>> list = new GenericEntity<List<ResourceWithType>>(rwtList) {
            };
            builder = Response.ok(list);
        }

        return builder.build();

    }

    private Resource obtainResource(int resourceId) {
        Resource resource = resMgr.getResource(caller,resourceId);
        if (resource == null) {
            resource = resMgr.getResource(caller, resourceId);
            if (resource != null)
                putToCache(resourceId, Resource.class, resource);
        }
        return resource;
    }

    @GZIP
    @AddLinks
    @GET
    @Path(("/{id}/alerts"))
    @ApiError(code = 404, reason = NO_RESOURCE_FOR_ID)
    @ApiOperation("Get a list of links to the alerts for the passed resource")
    public List<Link> getAlertsForResource(@ApiParam("Id of the resource to query") @PathParam("id") int resourceId) {
        AlertCriteria criteria = new AlertCriteria();
        criteria.addFilterResourceIds(resourceId);
        List<Alert> alerts = alertManager.findAlertsByCriteria(caller, criteria);
        List<Link> links = new ArrayList<Link>(alerts.size());
        for (Alert al : alerts) {
            Link link = new Link();
            link.setRel("alert");
            link.setHref("/alert/" + al.getId());
            links.add(link);
        }
        return links;
    }

    @ApiOperation(value = "Create a new platform in the Server. If the platform already exists, this is a no-op." +
            "The platform internally has a special name so that it will not clash with one that was generated" +
            "via a normal RHQ agent. DEPRECATED Use POST /platforms instead")
    @POST
    @Path("platform/{name}")
    public Response createPlatformOLD(
            @ApiParam(value = "Name of the platform") @PathParam("name") String name,
            @ApiParam(value = "Type of the platform", allowableValues = "Linux,Windows,... TODO") StringValue typeValue,
            @Context UriInfo uriInfo) {

        String typeName = typeValue.getValue();

        return createPlatformInternal(name, typeName, uriInfo);

    }

    @POST
    @Path("platforms")
    @ApiOperation(value = "Create a new platform in the Server. If the platform already exists, this is a no-op." +
            "The platform internally has a special name so that it will not clash with one that was generated" +
            "via a normal RHQ agent. Only resourceName and typeName need to be supplied in the passed object")
    public Response createPlatform(
        @ApiParam("The info about the platform. Only type name and resource name need to be supplied") ResourceWithType resource,
        @Context UriInfo uriInfo)
    {

        String typeName = resource.getTypeName();
        String resourceName = resource.getResourceName();

        return createPlatformInternal(resourceName,typeName,uriInfo);
    }


    private Response createPlatformInternal(String name, String typeName, UriInfo uriInfo) {

        ResourceType type = resourceTypeManager.getResourceTypeByNameAndPlugin(typeName,"Platforms");
        if (type==null) {
            throw new StuffNotFoundException("Platform with type [" + typeName + "]");
        }

        String resourceKey = "p:" + name;
        Resource r = resMgr.getResourceByParentAndKey(caller,null,resourceKey,"Platforms",typeName);
        if (r!=null) {
            // platform exists - return it
            ResourceWithType rwt = fillRWT(r,uriInfo);

            UriBuilder uriBuilder = uriInfo.getBaseUriBuilder();
            uriBuilder.path("/resource/{id}");
            URI uri = uriBuilder.build(r.getId());


            Response.ResponseBuilder builder = Response.created(uri);
            builder.entity(rwt);
            return builder.build();

        }

        // Create a dummy agent per platform - otherwise we can't delete the platform later
        Agent agent ;
        agent = new Agent("dummy-agent:name"+name,"-dummy-p:"+name,12345,"http://foo.com/p:name/"+name,"abc-"+name);
        agentMgr.createAgent(agent);

        Resource platform = new Resource(resourceKey,name,type);
        platform.setUuid(UUID.randomUUID().toString());
        platform.setAgent(agent);
        platform.setInventoryStatus(InventoryStatus.COMMITTED);
        platform.setModifiedBy(caller.getName());
        platform.setDescription(type.getDescription() + ". Created via REST-api");
        platform.setItime(System.currentTimeMillis());

        try {
            resMgr.createResource(caller,platform,-1);

            createSchedules(platform);

            ResourceWithType rwt = fillRWT(platform,uriInfo);
            UriBuilder uriBuilder = uriInfo.getBaseUriBuilder();
            uriBuilder.path("/resource/{id}");
            URI uri = uriBuilder.build(platform.getId());

            Response.ResponseBuilder builder = Response.created(uri);
            builder.entity(rwt);
            return builder.build();


        } catch (ResourceAlreadyExistsException e) {
            throw new IllegalArgumentException(e);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    @ApiOperation(value = "Create a resource with a given type below a certain parent. DEPRECATED Use POST / instead")
    @POST
    @Path("{name}")
    public Response createResourceOLD(
            @ApiParam("Name of the new resource") @PathParam("name") String name,
            @ApiParam("Name of the Resource type") StringValue typeValue,
            @ApiParam("Name of the plugin providing the type") @QueryParam("plugin") String plugin,
            @ApiParam("Id of the future parent to attach this to") @QueryParam("parentId") int parentId,
            @Context UriInfo uriInfo) {

        String typeName = typeValue.getValue();

        return createResourceInternal(name, plugin, parentId, typeName, uriInfo);
    }

    @POST
    @Path("/")
    @ApiOperation("Create a new resource as a child of an existing resource¡")
    public Response createResource(
        @ApiParam("THe info about the resource. You need to supply resource name, resource type name, plugin name, id of the parent") ResourceWithType resource,
        @Context UriInfo uriInfo)
    {
        return createResourceInternal(resource.getResourceName(),resource.getPluginName(),resource.getParentId(),resource.getTypeName(),uriInfo);
    }

    private Response createResourceInternal(String name, String plugin, int parentId, String typeName,
                                            UriInfo uriInfo) {
        Resource parent = resMgr.getResourceById(caller,parentId);

        ResourceType resType = resourceTypeManager.getResourceTypeByNameAndPlugin(typeName,plugin);
        if (resType==null)
            throw new StuffNotFoundException("ResourceType with name [" + typeName + "] and plugin [" + plugin + "]");

        String resourceKey = "res:" + name + ":" + parentId;

        Resource r = resMgr.getResourceByParentAndKey(caller,parent,resourceKey,plugin,typeName);
        if (r!=null) {
            // resource exists - return it
            ResourceWithType rwt = fillRWT(r,uriInfo);

            UriBuilder uriBuilder = uriInfo.getBaseUriBuilder();
            uriBuilder.path("/resource/{id}");
            URI uri = uriBuilder.build(r.getId());

            Response.ResponseBuilder builder = Response.created(uri);
            builder.entity(rwt);
            return builder.build();
        }

        Resource res = new Resource(resourceKey,name,resType);
        res.setUuid(UUID.randomUUID().toString());
        res.setAgent(parent.getAgent());
        res.setParentResource(parent);
        res.setInventoryStatus(InventoryStatus.COMMITTED);
        res.setDescription(resType.getDescription() + ". Created via REST-api");

        try {
            resMgr.createResource(caller,res,parent.getId());

            createSchedules(res);

            ResourceWithType rwt = fillRWT(res,uriInfo);

            UriBuilder uriBuilder = uriInfo.getBaseUriBuilder();
            uriBuilder.path("/resource/{id}");
            URI uri = uriBuilder.build(res.getId());

            Response.ResponseBuilder builder = Response.created(uri);
            builder.entity(rwt);
            return builder.build();


        } catch (ResourceAlreadyExistsException e) {
            throw new IllegalArgumentException(e);
        }
    }

    @DELETE
    @Path("/{id}")
    @ApiOperation("Remove a resource from inventory")
    public Response uninventoryOrDeleteResource(
            @PathParam("id") int resourceId
            /*,@DefaultValue("false") @QueryParam("physical") boolean delete*/) {

        resMgr.uninventoryResource(caller,resourceId);

        return Response.status(Response.Status.NO_CONTENT).build();

    }

    private void createSchedules(Resource resource) {
        ResourceType rt = resource.getResourceType();
        Set<MeasurementDefinition> definitions = rt.getMetricDefinitions ();
        for (MeasurementDefinition definition : definitions) {
            MeasurementSchedule schedule = new MeasurementSchedule(definition,resource);
            schedule.setEnabled(definition.isDefaultOn());
            schedule.setInterval(definition.getDefaultInterval());
            entityManager.persist(schedule);
        }
    }

}
