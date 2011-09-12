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

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.interceptor.Interceptors;

import org.jboss.resteasy.spi.Link;

import org.rhq.core.domain.alert.Alert;
import org.rhq.core.domain.criteria.AlertCriteria;
import org.rhq.core.domain.measurement.Availability;
import org.rhq.core.domain.measurement.MeasurementDefinition;
import org.rhq.core.domain.measurement.MeasurementSchedule;
import org.rhq.core.domain.resource.InventoryStatus;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceCategory;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.enterprise.server.alert.AlertManagerLocal;
import org.rhq.enterprise.server.plugin.pc.content.RepoSource;
import org.rhq.enterprise.server.rest.domain.AvailabilityRest;
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
    public ResourceWithType getResource(int id) {

        Resource res = resMgr.getResource(caller, id);

        ResourceWithType rwt = fillRWT(res);

        return rwt;
    }

    @Override
    public String getResourceHtml(int id) {
        ResourceWithType rwt = getResource(id);
        return renderTemplate("resourceWithType.ftl", rwt);
    }

    @Override
    public List<ResourceWithType> getPlatforms() {

        PageControl pc = new PageControl();
        List<Resource> ret = resMgr.findResourcesByCategory(caller, ResourceCategory.PLATFORM, InventoryStatus.COMMITTED, pc) ;
        List<ResourceWithType> rwtList = new ArrayList<ResourceWithType>(ret.size());
        for (Resource r: ret) {
            ResourceWithType rwt = fillRWT(r);
            rwtList.add(rwt);
        }
        return rwtList;
    }

    @Override
    public String getPlatformsHtml() {
        List<ResourceWithType> list = getPlatforms();
        return renderTemplate("listResourceWithType", list);
    }

    @Override
    public ResourceWithChildren getHierarchy(int baseResourceId) {
        // TODO optimize to do less recursion
        Resource start = resMgr.getResource(caller,baseResourceId);
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

    private ResourceWithType fillRWT(Resource res) {
        ResourceType resourceType = res.getResourceType();
        ResourceWithType rwt = new ResourceWithType(res.getName(),res.getId());
        rwt.setTypeName(resourceType.getName());
        rwt.setTypeId(resourceType.getId());
        rwt.setPluginName(resourceType.getPlugin());
        Resource parent = res.getParentResource();
        if (parent!=null) {
            rwt.setParentId(parent.getId());
        }
        return rwt;
    }

    public List<MetricSchedule> getSchedules(int resourceId) {

        Resource res = resMgr.getResource(caller, resourceId);

        Set<MeasurementSchedule> schedules = res.getSchedules();
        List<MetricSchedule> ret = new ArrayList<MetricSchedule>(schedules.size());
        for (MeasurementSchedule schedule : schedules) {
            MeasurementDefinition definition = schedule.getDefinition();
            MetricSchedule ms = new MetricSchedule(schedule.getId(), definition.getName(), definition.getDisplayName(),
                    schedule.isEnabled(),schedule.getInterval(), definition.getUnits().toString(),
                    definition.getDataType().toString());
            ret.add(ms);
        }

        return ret;
    }

    public String getSchedulesHtml(int resourceId) {
        List<MetricSchedule> list = getSchedules(resourceId);
        return renderTemplate("listMetricSchedule", list);
    }

    @Override
    public List<ResourceWithType> getChildren(int id) {
        PageControl pc = new PageControl();
        Resource parent = resMgr.getResource(caller,id);
        List<Resource> ret = resMgr.findResourceByParentAndInventoryStatus(caller,parent,InventoryStatus.COMMITTED,pc);
        List<ResourceWithType> rwtList = new ArrayList<ResourceWithType>(ret.size());
        for (Resource r: ret) {
            ResourceWithType rwt = fillRWT(r);
            rwtList.add(rwt);
        }

        return rwtList;
    }

    public String getChildrenHtml(int id) {
        List<ResourceWithType> list = getChildren(id);
        return renderTemplate("listResourceWithType", list);
    }

    @Override
    public List<Link> getAlertsForResource(int resourceId) {
        AlertCriteria criteria = new AlertCriteria();
        criteria.addFilterResourceIds(resourceId);
        List<Alert> alerts = alertManager.findAlertsByCriteria(caller,criteria);
        List<Link> links = new ArrayList<Link>(alerts.size());
        for (Alert al: alerts) {
            Link link = new Link();
            link.setRelationship("alert");
            link.setHref("/alert/" + al.getId());
        }
        return links;
    }
}
