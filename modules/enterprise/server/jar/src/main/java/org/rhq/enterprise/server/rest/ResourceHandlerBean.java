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
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.measurement.Availability;
import org.rhq.core.domain.measurement.MeasurementDefinition;
import org.rhq.core.domain.measurement.MeasurementSchedule;
import org.rhq.core.domain.resource.InventoryStatus;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceCategory;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.rest.MetricSchedule;
import org.rhq.core.domain.rest.ResourceWithType;
import org.rhq.core.domain.util.PageControl;
import org.rhq.enterprise.server.measurement.AvailabilityManagerLocal;
import org.rhq.enterprise.server.measurement.MeasurementScheduleManagerLocal;
import org.rhq.enterprise.server.resource.ResourceManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * Class that deals with getting data about resources
 * @author Heiko W. Rupp
 */
@Stateless
public class ResourceHandlerBean implements ResourceHandlerLocal {

    @EJB
    ResourceManagerLocal resMgr;
    @EJB
    AvailabilityManagerLocal availMgr;
    @EJB
    MeasurementScheduleManagerLocal scheduleManager;

    @Override
    public ResourceWithType getResource(int id) {


        Subject subject = LookupUtil.getSubjectManager().getOverlord(); // TODO

        Resource res = resMgr.getResource(subject, id);

        ResourceWithType rwt = fillRWT(res);

        return rwt;
    }


    @Override
    public List<ResourceWithType> getPlatforms() {

        Subject subject = LookupUtil.getSubjectManager().getOverlord(); // TODO


        PageControl pc = new PageControl();
        List<Resource> ret = resMgr.findResourcesByCategory(subject, ResourceCategory.PLATFORM, InventoryStatus.COMMITTED, pc) ;
        List<ResourceWithType> rwtList = new ArrayList<ResourceWithType>(ret.size());
        for (Resource r: ret) {
            ResourceWithType rwt = fillRWT(r);
            rwtList.add(rwt);
        }
        return rwtList;
    }

    @Override
    public List<ResourceWithType> getServersForPlatform(int id) {

        Subject subject = LookupUtil.getSubjectManager().getOverlord(); // TODO

        PageControl pc = new PageControl();
        Resource parent = resMgr.getResource(subject,id);
        List<Resource> ret = resMgr.findResourceByParentAndInventoryStatus(subject,parent,InventoryStatus.COMMITTED,pc);
        List<ResourceWithType> rwtList = new ArrayList<ResourceWithType>(ret.size());
        for (Resource r: ret) {
            ResourceWithType rwt = fillRWT(r);
            rwtList.add(rwt);
        }

        return rwtList;
    }

    @Override
    public Availability getAvailability(int resourceId) {

        Subject subject = LookupUtil.getSubjectManager().getOverlord(); // TODO

        Availability avail = availMgr.getCurrentAvailabilityForResource(subject, resourceId);
        return avail;
    }

    private ResourceWithType fillRWT(Resource res) {
        ResourceType resourceType = res.getResourceType();
        ResourceWithType rwt = new ResourceWithType(res.getName(),res.getId(), resourceType.getName(),
                resourceType.getId(), resourceType.getPlugin());
        Resource parent = res.getParentResource();
        if (parent!=null) {
            rwt.setParentId(parent.getId());
            rwt.setParentName(parent.getName());
        }
        return rwt;
    }

    public List<MetricSchedule> getSchedules(int resourceId) {

        Subject subject = LookupUtil.getSubjectManager().getOverlord(); // TODO
        Resource res = resMgr.getResource(subject,resourceId);

        ResourceType rt = res.getResourceType();

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

    public MetricSchedule getSchedule(int scheduleId) {

        Subject subject = LookupUtil.getSubjectManager().getOverlord(); // TODO

        MeasurementSchedule schedule = scheduleManager.getScheduleById(subject,scheduleId);
        MeasurementDefinition definition = schedule.getDefinition();
        MetricSchedule ms = new MetricSchedule(schedule.getId(), definition.getName(), definition.getDisplayName(),
                schedule.isEnabled(),schedule.getInterval(), definition.getUnits().toString(),
                definition.getDataType().toString());

        return ms;
    }
}
