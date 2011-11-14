/*
 * RHQ Management Platform
 * Copyright (C) 2005-2011 Red Hat, Inc.
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
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

import com.arjuna.ats.internal.jdbc.drivers.modifiers.list;

import org.rhq.core.domain.alert.Alert;
import org.rhq.core.domain.criteria.AlertCriteria;
import org.rhq.core.domain.measurement.Availability;
import org.rhq.core.domain.measurement.DataType;
import org.rhq.core.domain.measurement.MeasurementDefinition;
import org.rhq.core.domain.measurement.MeasurementSchedule;
import org.rhq.core.domain.resource.InventoryStatus;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceCategory;
import org.rhq.enterprise.server.alert.AlertManagerLocal;
import org.rhq.enterprise.server.rest.domain.AvailabilityRest;
import org.rhq.enterprise.server.rest.domain.Link;
import org.rhq.enterprise.server.rest.domain.MetricSchedule;
import org.rhq.enterprise.server.rest.domain.ResourceWithChildren;
import org.rhq.enterprise.server.rest.domain.ResourceWithType;
import org.rhq.core.domain.util.PageControl;
import org.rhq.enterprise.server.measurement.AvailabilityManagerLocal;
import org.rhq.enterprise.server.measurement.MeasurementScheduleManagerLocal;
import org.rhq.enterprise.server.resource.ResourceManagerLocal;

/**
 * Class that deals with getting data about resources
 * @author Heiko W. Rupp
 */
@Interceptors(SetCallerInterceptor.class)
@Stateless
public class ResourceHandlerBean extends AbstractRestBean implements ResourceHandlerLocal {

    @EJB
    ResourceManagerLocal resMgr;
    @EJB
    AvailabilityManagerLocal availMgr;
    @EJB
    MeasurementScheduleManagerLocal scheduleManager;
    @EJB
    AlertManagerLocal alertManager;

    @Override
    public Response getResource(int id, @Context Request request, @Context HttpHeaders headers,
                         @Context UriInfo uriInfo) {

        // Create a cache control
        CacheControl cc = new CacheControl();
        cc.setMaxAge(300); // Resources are valid for 5 mins
        cc.setPrivate(false); // Proxies may cache this

        Resource res;
        res = getFromCache(id,Resource.class);
        if (res==null) {
            res = resMgr.getResource(caller, id);
            if (res!=null)
                putToCache(id, Resource.class,res);
            else
                throw new StuffNotFoundException("Resource with id " + id);
        }

        Response.ResponseBuilder builder=null;

        long mtime = res.getMtime();
        EntityTag eTag = new EntityTag(Long.toOctalString(res.hashCode()+ mtime)); // factor in mtime in etag
        builder = request.evaluatePreconditions(new Date(mtime),eTag);

        if (builder!=null) {
            builder.cacheControl(cc);
            return builder.build();
        }


        ResourceWithType rwt = fillRWT(res, uriInfo);

        // What media type does the user request?
        MediaType mediaType = headers.getAcceptableMediaTypes().get(0);

        if (mediaType.equals(MediaType.TEXT_HTML_TYPE)) {
            builder = Response.ok(renderTemplate("resourceWithType", rwt), mediaType);
        }
        else {
            builder = Response.ok(rwt);
        }

        return builder.build();
    }


    @Override
    public Response getPlatforms(@Context Request request, @Context HttpHeaders headers,
                         @Context UriInfo uriInfo) {

        PageControl pc = new PageControl();
        List<Resource> ret = resMgr.findResourcesByCategory(caller, ResourceCategory.PLATFORM, InventoryStatus.COMMITTED, pc) ;
        List<ResourceWithType> rwtList = new ArrayList<ResourceWithType>(ret.size());
        for (Resource r: ret) {
            putToCache(r.getId(),Resource.class,r);
            ResourceWithType rwt = fillRWT(r, uriInfo);
            rwtList.add(rwt);
        }
        // What media type does the user request?
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
    public ResourceWithChildren getHierarchy(int baseResourceId) {
        // TODO optimize to do less recursion
        Resource start;
        start = getFromCache(baseResourceId,Resource.class);
        if (start==null) {
            start = resMgr.getResource(caller,baseResourceId);
            if (start!=null)
                putToCache(start.getId(),Resource.class,start);
        }
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
        ResourceWithChildren rwc = new ResourceWithChildren(""+baseResource.getId(),baseResource.getName());

        PageControl pc = new PageControl();
        List<Resource> ret = resMgr.findResourceByParentAndInventoryStatus(caller,baseResource,InventoryStatus.COMMITTED,pc);
        if (!ret.isEmpty()) {
            List<ResourceWithChildren> resList = new ArrayList<ResourceWithChildren>(ret.size());
            for (Resource res : ret) {
                ResourceWithChildren child = getHierarchy(res);
                resList.add(child);
                putToCache(res.getId(),Resource.class,res);
            }
            if (!resList.isEmpty())
                rwc.setChildren(resList);
        }
        return rwc;
    }

    @Override
    public AvailabilityRest getAvailability(int resourceId) {

        Availability avail = availMgr.getCurrentAvailabilityForResource(caller, resourceId);
        AvailabilityRest availabilityRest = new AvailabilityRest(avail.getAvailabilityType(),avail.getStartTime().getTime(),
                avail.getResource().getId());
        return availabilityRest;
    }

    public Response getSchedules(int resourceId,
                                             String scheduleType,
                                          @Context Request request,
                                          @Context HttpHeaders headers,
                                          @Context UriInfo uriInfo) {

        // allow metric as input
        if (scheduleType.equals("metric"))
            scheduleType=DataType.MEASUREMENT.toString().toLowerCase();

        Resource res = resMgr.getResource(caller, resourceId);

        Set<MeasurementSchedule> schedules = res.getSchedules();
        List<MetricSchedule> ret = new ArrayList<MetricSchedule>(schedules.size());
        for (MeasurementSchedule schedule : schedules) {
            putToCache(schedule.getId(),MeasurementSchedule.class,schedule);
            MeasurementDefinition definition = schedule.getDefinition();

            // user can opt to e.g. only get "measurement" or "trait" metrics

            if ("all".equals(scheduleType) ||
                    scheduleType.toLowerCase().equals(definition.getDataType().toString().toLowerCase()) ) {
                MetricSchedule ms = new MetricSchedule(schedule.getId(), definition.getName(), definition.getDisplayName(),
                        schedule.isEnabled(),schedule.getInterval(), definition.getUnits().toString(),
                        definition.getDataType().toString());
                UriBuilder uriBuilder;
                URI uri;
                if (definition.getDataType()== DataType.MEASUREMENT) {
                    uriBuilder = uriInfo.getBaseUriBuilder();
                    uriBuilder.path("/metric/data/{id}");
                    uri = uriBuilder.build(schedule.getId());
                    Link metricLink = new Link("metric",uri.toString());
                    ms.addLink(metricLink);
                }
                // create link to the resource
                uriBuilder = uriInfo.getBaseUriBuilder();
                uriBuilder.path("resource/" + schedule.getResource().getId());
                uri = uriBuilder.build();
                Link link = new Link("resource",uri.toString());
                ms.addLink(link);

                ret.add(ms);
            }
        }

        // What media type does the user request?
        MediaType mediaType = headers.getAcceptableMediaTypes().get(0);
        Response.ResponseBuilder builder;

        if (mediaType.equals(MediaType.TEXT_HTML_TYPE)) {
            builder = Response.ok(renderTemplate("listMetricSchedule", ret), mediaType);
        }
        else {
            GenericEntity<List<MetricSchedule>> list = new GenericEntity<List<MetricSchedule>>(ret){};
            builder = Response.ok(list,mediaType);
        }

        return builder.build();
    }

    @Override
    public Response getChildren(int id, @Context Request request, @Context HttpHeaders headers,
                         @Context UriInfo uriInfo) {
        PageControl pc = new PageControl();
        Resource parent;
        parent = getFromCache(id,Resource.class);
        if (parent==null) {
            parent = resMgr.getResource(caller,id);
            if (parent==null)
                throw new StuffNotFoundException("Resource with id " + id);
            else
                putToCache(id,Resource.class,parent);
        }
        List<Resource> ret = resMgr.findResourceByParentAndInventoryStatus(caller,parent,InventoryStatus.COMMITTED,pc);
        List<ResourceWithType> rwtList = new ArrayList<ResourceWithType>(ret.size());
        for (Resource r: ret) {
            putToCache(r.getId(),Resource.class,r);
            ResourceWithType rwt = fillRWT(r, uriInfo);
            rwtList.add(rwt);
        }

        // What media type does the user request?
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
    public List<Link> getAlertsForResource(int resourceId) {
        AlertCriteria criteria = new AlertCriteria();
        criteria.addFilterResourceIds(resourceId);
        List<Alert> alerts = alertManager.findAlertsByCriteria(caller,criteria);
        List<Link> links = new ArrayList<Link>(alerts.size());
        for (Alert al: alerts) {
            Link link = new Link();
            link.setRel("alert");
            link.setHref("/alert/" + al.getId());
        }
        return links;
    }
}
