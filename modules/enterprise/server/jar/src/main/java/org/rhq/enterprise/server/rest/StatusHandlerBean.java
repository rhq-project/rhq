/*
 * RHQ Management Platform
 * Copyright (C) 2005-2011 Red Hat, Inc.
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

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.interceptor.Interceptors;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import org.rhq.core.domain.alert.Alert;
import org.rhq.core.domain.alert.AlertDefinition;
import org.rhq.core.domain.content.composite.PackageListItemComposite;
import org.rhq.core.domain.criteria.AlertCriteria;
import org.rhq.core.domain.criteria.AlertDefinitionCriteria;
import org.rhq.core.domain.criteria.Criteria;
import org.rhq.core.domain.criteria.ResourceCriteria;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceCategory;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.server.alert.AlertDefinitionManagerLocal;
import org.rhq.enterprise.server.alert.AlertManagerLocal;
import org.rhq.enterprise.server.measurement.MeasurementScheduleManagerLocal;
import org.rhq.enterprise.server.resource.ResourceManagerLocal;
import org.rhq.enterprise.server.rest.domain.Status;

/**
 * Return system status
 * @author Heiko W. Rupp
 */
@Stateless
@Interceptors(SetCallerInterceptor.class)
public class StatusHandlerBean extends AbstractRestBean implements StatusHandlerLocal {


    @EJB
    MeasurementScheduleManagerLocal scheduleManager;
    @EJB
    ResourceManagerLocal resourceManager;
    @EJB
    AlertManagerLocal alertManager;
    @EJB
    AlertDefinitionManagerLocal alertDefinitionManager;

    @Override
    public Status getStatus() {

        Status status = new Status();

        ResourceCriteria criteria = new ResourceCriteria();
        criteria.addFilterResourceCategories(ResourceCategory.PLATFORM);
        criteria.setRestriction(Criteria.Restriction.COUNT_ONLY);
        PageList<Resource> resList = resourceManager.findResourcesByCriteria(caller,criteria);
        status.setPlatforms(resList.getTotalSize());
        criteria = new ResourceCriteria();
        criteria.addFilterResourceCategories(ResourceCategory.SERVER);
        criteria.setRestriction(Criteria.Restriction.COUNT_ONLY);
        resList = resourceManager.findResourcesByCriteria(caller,criteria);
        status.setServers(resList.getTotalSize());
        criteria = new ResourceCriteria();
        criteria.addFilterResourceCategories(ResourceCategory.SERVICE);
        criteria.setRestriction(Criteria.Restriction.COUNT_ONLY);
        resList = resourceManager.findResourcesByCriteria(caller,criteria);
        status.setServices(resList.getTotalSize());

        AlertCriteria alertCriteria = new AlertCriteria();
        alertCriteria.setRestriction(Criteria.Restriction.COUNT_ONLY);
        PageList<Alert> alertList = alertManager.findAlertsByCriteria(caller,alertCriteria);
        status.setAlerts(alertList.getTotalSize());

        AlertDefinitionCriteria alertDefinitionCriteria = new AlertDefinitionCriteria();
        alertDefinitionCriteria.setRestriction(Criteria.Restriction.COUNT_ONLY);
        PageList<AlertDefinition> defList = alertDefinitionManager.findAlertDefinitionsByCriteria(caller,alertDefinitionCriteria);
        status.setAlertDefinitions(defList.getTotalSize());

        status.setSchedules(-1); // TODO

        status.setMetricsMin(scheduleManager.getScheduledMeasurementsPerMinute());

        return status;
    }

    @Override
    public String getStatusHtml() {
        Status status = getStatus();
        return renderTemplate("status",status);
    }
}
