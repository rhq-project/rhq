/*
 * RHQ Management Platform
 * Copyright (C) 2005-2008 Red Hat, Inc.
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
package org.rhq.enterprise.gui.operation.schedule.resource;

import java.util.List;
import org.quartz.SchedulerException;
import org.quartz.SimpleTrigger;
import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.resource.Resource;
import org.rhq.enterprise.gui.operation.schedule.OperationScheduleUIBean;
import org.rhq.enterprise.gui.util.EnterpriseFacesContextUtility;
import org.rhq.enterprise.server.operation.ResourceOperationSchedule;

public class ResourceOperationScheduleUIBean extends OperationScheduleUIBean {
    private Resource resource;

    public ResourceOperationScheduleUIBean() {
    }

    @Override
    public String getManagedBeanName() {
        return "ResourceOperationScheduleUIBean";
    }

    @Override
    public List<ResourceOperationSchedule> getOperationScheduleList() {
        Subject subject = EnterpriseFacesContextUtility.getSubject();
        Resource requestResource = EnterpriseFacesContextUtility.getResourceIfExists();

        if (requestResource == null) {
            requestResource = resource; // request not associated with a resource - use the resource we used before
        } else {
            resource = requestResource; // request switched the resource this UI bean is using
        }

        List<ResourceOperationSchedule> results = null;
        try {
            results = manager.getScheduledResourceOperations(subject, requestResource.getId());
        } catch (SchedulerException se) {
            // throw up all known information to the caller for now
            throw new IllegalStateException(se.getMessage(), se);
        }

        return results;
    }

    @Override
    public void unscheduleOperation(Subject subject, String doomedJobId) throws Exception {
        if (resource == null) {
            resource = EnterpriseFacesContextUtility.getResource();

            if (resource == null) {
                throw new IllegalStateException("Could not find resource from which to delete operation schedules");
            }
        }

        manager.unscheduleResourceOperation(subject, doomedJobId, resource.getId());
    }

    @Override
    public void scheduleOperation(Subject subject, String operationName, Configuration parameters,
        SimpleTrigger simpleTrigger, String description) throws Exception {
        if (resource == null) {
            resource = EnterpriseFacesContextUtility.getResource();

            if (resource == null) {
                throw new IllegalStateException("Could not find resource against which to schedule operations");
            }
        }

        manager.scheduleResourceOperation(subject, resource.getId(), operationName, parameters, simpleTrigger,
            description);
    }
}