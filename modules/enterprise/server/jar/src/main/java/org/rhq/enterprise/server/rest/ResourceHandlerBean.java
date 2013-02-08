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

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.interceptor.Interceptors;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

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
@Interceptors(SetCallerInterceptor.class)
@Stateless
public class ResourceHandlerBean extends AbstractRestBean implements ResourceHandlerLocal {

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

    @Override
    public Response getResource(int id, Request request, HttpHeaders headers, UriInfo uriInfo) {

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

    @Override
    public Response getResourcesByQuery(String q, Request request, HttpHeaders headers,
                             UriInfo uriInfo) {
        ResourceCriteria criteria = new ResourceCriteria();
        criteria.addFilterName(q);
        List<Resource> ret = resMgr.findResourcesByCriteria(caller,criteria);

        Response.ResponseBuilder builder = getResponseBuilderForResourceList(headers,uriInfo,ret);

        return builder.build();
    }

    @Override
    public Response getPlatforms(Request request, HttpHeaders headers, UriInfo uriInfo) {

        PageControl pc = new PageControl();
        List<Resource> ret = resMgr.findResourcesByCategory(caller, ResourceCategory.PLATFORM,
            InventoryStatus.COMMITTED, pc);
        Response.ResponseBuilder builder = getResponseBuilderForResourceList(headers, uriInfo, ret);

        return builder.build();
    }

    /**
     * Translate the passed list of resources into a response according to the acceptable mime types etc.
     * @param headers HttpHeaders from the request
     * @param uriInfo Uri from the request
     * @param resources List of resources
     * @return An initialized ResponseBuilder
     */
    private Response.ResponseBuilder getResponseBuilderForResourceList(HttpHeaders headers, UriInfo uriInfo,
                                                                       List<Resource> resources) {
        List<ResourceWithType> rwtList = new ArrayList<ResourceWithType>(resources.size());
        for (Resource r : resources) {
            putToCache(r.getId(), Resource.class, r);
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
        return builder;
    }

    @Override
    public ResourceWithChildren getHierarchy(int baseResourceId) {
        // TODO optimize to do less recursion
        Resource start = obtainResource(baseResourceId);
        ResourceWithChildren rwc = getHierarchy(start);
        /*new ResourceWithChildren(""+start.getId(),start.getName());

        PageControl pc = new PageControl();
        List<Resource> ret = resMgr.findResourceByParentAndInventoryStatus(caller,start,InventoryStatus.COMMITTED,pc);
        if (!ret.isEmpty()) {
            List<ResourceWithChildren> resList = new ArrayList<ResourceWithChildren>(ret.size());
            for (Resource res : ret) {
                ResourceWithChildren child = getHierarchy(res.getId());
                resList.add(child);
            }
            rwc.setChildren(resList);
        }*/
        return rwc;
    }

    ResourceWithChildren getHierarchy(Resource baseResource) {
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

    @Override
    public Response getAvailability(int resourceId, HttpHeaders headers) {

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

    @Override
    public Response getAvailabilityHistory(int resourceId, long start, long end, HttpHeaders headers) {
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


    @Override
    public void reportAvailability(int resourceId, AvailabilityRest avail) {
        if (avail.getResourceId() != resourceId)
            throw new IllegalArgumentException("Resource Ids do not match");

        Resource resource = obtainResource(resourceId);

        AvailabilityType at;
        at = AvailabilityType.valueOf(avail.getType());

        AvailabilityReport report = new AvailabilityReport(true, resource.getAgent().getName());
        Availability availability = new Availability(resource, avail.getSince(), at);
        report.addAvailability(availability);

        availMgr.mergeAvailabilityReport(report);
    }

    public Response getSchedules(int resourceId, String scheduleType, boolean enabledOnly, String name,
        Request request, HttpHeaders headers, UriInfo uriInfo) {

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

    @Override
    public Response getChildren(int id, Request request, HttpHeaders headers, UriInfo uriInfo) {
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
        Resource resource = getFromCache(resourceId, Resource.class);
        if (resource == null) {
            resource = resMgr.getResource(caller, resourceId);
            if (resource != null)
                putToCache(resourceId, Resource.class, resource);
        }
        return resource;
    }

    @Override
    public List<Link> getAlertsForResource(int resourceId) {
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

    @Override
    public Response createPlatform(String name, StringValue typeValue, UriInfo uriInfo) {
        String typeName = typeValue.getValue();

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


            javax.ws.rs.core.Response.ResponseBuilder builder = Response.ok(rwt);
            builder.location(uri);
            return builder.build();

        }

        // Create a dummy agent per platform - otherwise we can't delete the platform later
        Agent agent ;
        agent = new Agent("dummy-agent:name"+name,"-dummy-p:"+name,12345,"http://foo.com/p:name/"+name,"abc-"+name);
        agentMgr.createAgent(agent);



        Resource platform = new Resource(resourceKey,name,type);
        platform.setUuid(resourceKey);
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

            javax.ws.rs.core.Response.ResponseBuilder builder = Response.created(uri);
            builder.entity(rwt);
            return builder.build();


        } catch (ResourceAlreadyExistsException e) {
            throw new IllegalArgumentException(e);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

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

    @Override
    public Response createResource(String name, StringValue typeValue,
                                   String plugin, int parentId, UriInfo uriInfo) {

        Resource parent = resMgr.getResourceById(caller,parentId);
        if (parent==null)
            throw new StuffNotFoundException("Parent with id [" + parentId + "]");

        String typeName = typeValue.getValue();
        ResourceType resType = resourceTypeManager.getResourceTypeByNameAndPlugin(typeName,plugin);
        if (resType==null)
            throw new StuffNotFoundException("ResourceType with name [" + typeName + "] and plugin [" + plugin + "]");

        String resourceKey = "res:" + name + ":" + parentId;


        Resource r = resMgr.getResourceByParentAndKey(caller,null,resourceKey,plugin,typeName);
        if (r!=null) {
            // platform exists - return it
            ResourceWithType rwt = fillRWT(r,uriInfo);

            UriBuilder uriBuilder = uriInfo.getBaseUriBuilder();
            uriBuilder.path("/resource/{id}");
            URI uri = uriBuilder.build(r.getId());


            javax.ws.rs.core.Response.ResponseBuilder builder = Response.ok(rwt);
            builder.location(uri);
            return builder.build();

        }


        Resource res = new Resource(resourceKey,name,resType);
        res.setUuid(resourceKey);
        res.setAgent(parent.getAgent());
        res.setParentResource(parent);
        res.setInventoryStatus(InventoryStatus.COMMITTED);
        res.setDescription(resType.getDescription() + ". Created via REST-api");

        try {
            resMgr.createResource(caller,res,parent.getId());

            createSchedules(res);

            ResourceWithType rwt = fillRWT(res,uriInfo);

            javax.ws.rs.core.Response.ResponseBuilder builder = Response.ok(rwt);
            return builder.build();


        } catch (ResourceAlreadyExistsException e) {
            throw new IllegalArgumentException(e);
        }


    }

}
