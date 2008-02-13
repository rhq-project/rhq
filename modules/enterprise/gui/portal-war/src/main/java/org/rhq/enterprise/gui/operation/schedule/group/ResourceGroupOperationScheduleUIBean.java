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
package org.rhq.enterprise.gui.operation.schedule.group;

import java.util.List;
import org.quartz.JobDetail;
import org.quartz.SchedulerException;
import org.quartz.SimpleTrigger;
import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.common.composite.IntegerOptionItem;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.group.ResourceGroup;
import org.rhq.core.gui.util.FacesContextUtility;
import org.rhq.enterprise.gui.operation.definition.group.ResourceGroupOperationDefinitionUIBean;
import org.rhq.enterprise.gui.operation.schedule.OperationScheduleUIBean;
import org.rhq.enterprise.gui.util.EnterpriseFacesContextUtility;
import org.rhq.enterprise.server.operation.GroupOperationSchedule;
import org.rhq.enterprise.server.scheduler.SchedulerLocal;
import org.rhq.enterprise.server.util.LookupUtil;
import org.rhq.enterprise.server.util.QuartzUtil;

public class ResourceGroupOperationScheduleUIBean extends OperationScheduleUIBean {
    private ResourceGroup group;

    public ResourceGroupOperationScheduleUIBean() {
    }

    @Override
    public String getManagedBeanName() {
        return "ResourceGroupOperationScheduleUIBean";
    }

    @Override
    public List<GroupOperationSchedule> getOperationScheduleList() {
        Subject subject = EnterpriseFacesContextUtility.getSubject();
        ResourceGroup requestGroup = EnterpriseFacesContextUtility.getResourceGroup();

        if (requestGroup == null) {
            requestGroup = group; // request not associated with a resource - use the resource we used before
        } else {
            group = requestGroup; // request switched the resource this UI bean is using
        }

        List<GroupOperationSchedule> results = null;
        try {
            results = manager.getScheduledGroupOperations(subject, requestGroup.getId());
        } catch (SchedulerException se) {
            // throw up all known information to the caller for now
            throw new IllegalStateException(se.getMessage(), se);
        }

        return results;
    }

    @Override
    public void unscheduleOperation(Subject subject, String doomedJobId) throws Exception {
        if (group == null) {
            group = EnterpriseFacesContextUtility.getResourceGroup();

            if (group == null) {
                throw new IllegalStateException(
                    "Could not find resource group from which to delete operation schedules");
            }
        }

        manager.unscheduleGroupOperation(subject, doomedJobId, group.getId());
    }

    public String executeNow() throws Exception {
        Subject subject = EnterpriseFacesContextUtility.getSubject();
        String[] selectedItems = FacesContextUtility.getRequest().getParameterValues("selectedItems");
        SchedulerLocal scheduler = LookupUtil.getSchedulerBean();

        for (String jobIdString : selectedItems) {
            GroupOperationSchedule groupSchedule;
            try {
                groupSchedule = manager.getGroupOperationSchedule(subject, jobIdString);
            } catch (SchedulerException se) {
                throw new IllegalStateException(se.getMessage(), se);
            }

            List<Resource> resources = groupSchedule.getExecutionOrder();
            int[] resourceIds;
            if (resources == null) {
                resourceIds = new int[0];
            } else {
                resourceIds = new int[resources.size()];

                int i = 0;
                for (Resource next : resources) {
                    resourceIds[i++] = next.getId();
                }
            }

            JobDetail jobDetail = scheduler.getJobDetail(groupSchedule.getJobName(), groupSchedule.getJobGroup());
            scheduleOperation(subject, groupSchedule.getOperationName(), resourceIds, groupSchedule.isHaltOnFailure(),
                groupSchedule.getParameters(), (SimpleTrigger) QuartzUtil.getFireOnceImmediateTrigger(jobDetail),
                groupSchedule.getDescription());
        }

        return "viewOperationHistory";
    }

    @Override
    public void scheduleOperation(Subject subject, String operationName, Configuration parameters,
        SimpleTrigger simpleTrigger, String description) throws Exception {
        ResourceGroupOperationDefinitionUIBean bean = FacesContextUtility
            .getBean(ResourceGroupOperationDefinitionUIBean.class);

        scheduleOperation(subject, operationName, getResourceIds(), bean.isHaltOnFailure(), parameters, simpleTrigger,
            description);
    }

    private void scheduleOperation(Subject subject, String operationName, int[] passedResourceIds,
        boolean haltOnFailure, Configuration parameters, SimpleTrigger simpleTrigger, String description)
        throws Exception {
        if (group == null) {
            group = EnterpriseFacesContextUtility.getResourceGroup();

            if (group == null) {
                throw new IllegalStateException("Could not find resource group against which to schedule operations");
            }
        }

        manager.scheduleGroupOperation(subject, group.getId(), passedResourceIds, haltOnFailure, operationName,
            parameters, simpleTrigger, description);
    }

    public int[] getResourceIds() {
        ResourceGroupOperationDefinitionUIBean operationDefinitionUIBean = FacesContextUtility
            .getBean(ResourceGroupOperationDefinitionUIBean.class);
        if (operationDefinitionUIBean.isConcurrent()) {
            return null;
        }

        List<IntegerOptionItem> orderedNameItems = operationDefinitionUIBean.getResourceNameItems();

        int i = 0;
        int[] resourceIds = new int[orderedNameItems.size()];
        for (IntegerOptionItem nameItem : orderedNameItems) {
            resourceIds[i++] = nameItem.getId();
        }

        return resourceIds;
    }
}