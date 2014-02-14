/*
 * RHQ Management Platform
 * Copyright (C) 2005-2010 Red Hat, Inc.
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
package org.rhq.coregui.server.gwt;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.criteria.GroupOperationHistoryCriteria;
import org.rhq.core.domain.criteria.ResourceOperationHistoryCriteria;
import org.rhq.core.domain.operation.GroupOperationHistory;
import org.rhq.core.domain.operation.ResourceOperationHistory;
import org.rhq.core.domain.operation.bean.GroupOperationSchedule;
import org.rhq.core.domain.operation.bean.ResourceOperationSchedule;
import org.rhq.core.domain.operation.composite.GroupOperationScheduleComposite;
import org.rhq.core.domain.operation.composite.ResourceOperationLastCompletedComposite;
import org.rhq.core.domain.operation.composite.ResourceOperationScheduleComposite;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.core.domain.util.PageOrdering;
import org.rhq.coregui.client.gwt.OperationGWTService;
import org.rhq.coregui.server.util.SerialUtility;
import org.rhq.enterprise.server.operation.OperationManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * @author Greg Hinkle
 * @author Ian Springer
 */
public class OperationGWTServiceImpl extends AbstractGWTServiceImpl implements OperationGWTService {

    private static final long serialVersionUID = 1L;

    private static Set<String> resourceFieldsSet;

    static {
        // Like Resource services, filter Resource entities in operation histories for speedier transport
        resourceFieldsSet = new HashSet<String>(Arrays.asList(ResourceGWTServiceImpl.importantFields));
        resourceFieldsSet.add("operationHistories");
    }

    private OperationManagerLocal operationManager = LookupUtil.getOperationManager();

    public PageList<ResourceOperationHistory> findResourceOperationHistoriesByCriteria(
        ResourceOperationHistoryCriteria criteria) throws RuntimeException {
        try {
            PageList<ResourceOperationHistory> result = operationManager.findResourceOperationHistoriesByCriteria(
                getSessionSubject(), criteria);
            if (!result.isEmpty()) {
                List<Resource> resources = new ArrayList<Resource>(result.size());
                for (ResourceOperationHistory history : result) {
                    resources.add(history.getResource());
                }
                ObjectFilter.filterFieldsInCollection(resources, resourceFieldsSet);
            }

            return SerialUtility.prepare(result, "OperationService.findResourceOperationHistoriesByCriteria");
        } catch (Throwable t) {
            throw getExceptionToThrowToClient(t);
        }
    }

    public PageList<GroupOperationHistory> findGroupOperationHistoriesByCriteria(GroupOperationHistoryCriteria criteria)
        throws RuntimeException {
        try {
            PageList<GroupOperationHistory> result = operationManager.findGroupOperationHistoriesByCriteria(
                getSessionSubject(), criteria);

            return SerialUtility.prepare(result, "OperationService.findGroupOperationHistoriesByCriteria");
        } catch (Throwable t) {
            throw getExceptionToThrowToClient(t);
        }
    }

    public void deleteOperationHistories(int[] operationHistoryIds, boolean deleteEvenIfInProgress)
        throws RuntimeException {
        try {
            operationManager.deleteOperationHistories(getSessionSubject(), operationHistoryIds, deleteEvenIfInProgress);
        } catch (Throwable t) {
            throw getExceptionToThrowToClient(t);
        }
    }

    public void scheduleResourceOperation(int resourceId, String operationName, Configuration parameters,
        String description, int timeout) throws RuntimeException {
        try {
            ResourceOperationSchedule opSchedule = operationManager.scheduleResourceOperation(getSessionSubject(),
                resourceId, operationName, 0, 0, 0, 0, parameters, description);
        } catch (Throwable t) {
            throw getExceptionToThrowToClient(t);
        }
    }

    public void scheduleResourceOperation(int resourceId, String operationName, Configuration parameters,
        String description, int timeout, String cronString) throws RuntimeException {
        try {
            ResourceOperationSchedule opSchedule = operationManager.scheduleResourceOperationUsingCron(
                getSessionSubject(), resourceId, operationName, cronString, timeout, parameters, description);
        } catch (Throwable t) {
            throw getExceptionToThrowToClient(t);
        }
    }

    public int scheduleResourceOperation(ResourceOperationSchedule resourceOperationSchedule) throws RuntimeException {
        try {
            return operationManager.scheduleResourceOperation(getSessionSubject(), resourceOperationSchedule);
        } catch (Throwable t) {
            throw getExceptionToThrowToClient(t);
        }
    }

    public int scheduleGroupOperation(GroupOperationSchedule groupOperationSchedule) throws RuntimeException {
        try {
            return operationManager.scheduleGroupOperation(getSessionSubject(), groupOperationSchedule);
        } catch (Throwable t) {
            throw getExceptionToThrowToClient(t);
        }
    }

    public ResourceOperationSchedule getResourceOperationSchedule(int scheduleId) throws RuntimeException {
        try {
            ResourceOperationSchedule resourceOperationSchedule = operationManager.getResourceOperationSchedule(
                getSessionSubject(), scheduleId);
            return SerialUtility.prepare(resourceOperationSchedule, "getResourceOperationSchedule");
        } catch (Throwable t) {
            throw getExceptionToThrowToClient(t);
        }
    }

    public GroupOperationSchedule getGroupOperationSchedule(int scheduleId) throws RuntimeException {
        try {
            GroupOperationSchedule groupOperationSchedule = operationManager.getGroupOperationSchedule(
                getSessionSubject(), scheduleId);
            return SerialUtility.prepare(groupOperationSchedule, "getGroupOperationSchedule");
        } catch (Throwable t) {
            throw getExceptionToThrowToClient(t);
        }
    }

    public void unscheduleResourceOperation(ResourceOperationSchedule resourceOperationSchedule)
        throws RuntimeException {
        try {
            operationManager.unscheduleResourceOperation(getSessionSubject(), resourceOperationSchedule.getJobId()
                .toString(), resourceOperationSchedule.getResource().getId());
        } catch (Throwable t) {
            throw getExceptionToThrowToClient(t);
        }
    }

    public void unscheduleGroupOperation(GroupOperationSchedule groupOperationSchedule) throws RuntimeException {
        try {
            operationManager.unscheduleGroupOperation(getSessionSubject(),
                groupOperationSchedule.getJobId().toString(), groupOperationSchedule.getGroup().getId());
        } catch (Throwable t) {
            throw getExceptionToThrowToClient(t);
        }
    }

    /** Find recently completed operations, disambiguate them and return that list.
     *
     */
    public PageList<ResourceOperationLastCompletedComposite> findRecentCompletedOperations(int resourceId,
        PageControl pageControl) throws RuntimeException {
        Integer resourceIdentifier = null;
        if (resourceId > 0) {
            resourceIdentifier = new Integer(resourceId);
        }
        try {
            PageList<ResourceOperationLastCompletedComposite> lastCompletedResourceOps = operationManager
                .findRecentlyCompletedResourceOperations(getSessionSubject(), resourceIdentifier, pageControl);

            return SerialUtility.prepare(lastCompletedResourceOps, "OperationService.findRecentCompletedOperations");
        } catch (Throwable t) {
            throw getExceptionToThrowToClient(t);
        }
    }

    public PageList<ResourceOperationScheduleComposite> findCurrentlyScheduledResourceOperations(int pageSize)
        throws RuntimeException {
        try {
            PageControl pageControl = new PageControl(0, pageSize);
            pageControl.initDefaultOrderingField("ro.nextFireTime", PageOrdering.ASC);
            PageList<ResourceOperationScheduleComposite> scheduledResourceOps = operationManager
                .findCurrentlyScheduledResourceOperations(getSessionSubject(), pageControl);
            return SerialUtility.prepare(scheduledResourceOps,
                "OperationService.findCurrentlyScheduledResourceOperations");
        } catch (Throwable t) {
            throw getExceptionToThrowToClient(t);
        }
    }

    public PageList<GroupOperationScheduleComposite> findCurrentlyScheduledGroupOperations(int pageSize)
        throws RuntimeException {
        try {
            PageControl pageControl = new PageControl(0, pageSize);
            pageControl.initDefaultOrderingField("go.nextFireTime", PageOrdering.ASC);
            PageList<GroupOperationScheduleComposite> scheduledGroupOps = operationManager
                .findCurrentlyScheduledGroupOperations(getSessionSubject(), pageControl);
            return SerialUtility.prepare(scheduledGroupOps, "OperationService.findCurrentlyScheduledGroupOperations");
        } catch (Throwable t) {
            throw getExceptionToThrowToClient(t);
        }
    }

    public List<ResourceOperationSchedule> findScheduledResourceOperations(int resourceId) throws RuntimeException {
        try {
            List<ResourceOperationSchedule> resourceOperationSchedules = operationManager
                .findScheduledResourceOperations(getSessionSubject(), resourceId);

            return SerialUtility.prepare(resourceOperationSchedules, "findScheduledResourceOperations");
        } catch (Throwable t) {
            throw getExceptionToThrowToClient(t);
        }
    }

    public List<GroupOperationSchedule> findScheduledGroupOperations(int groupId) throws RuntimeException {
        try {
            List<GroupOperationSchedule> groupOperationSchedules = operationManager.findScheduledGroupOperations(
                getSessionSubject(), groupId);

            return SerialUtility.prepare(groupOperationSchedules, "findScheduledGroupOperations");
        } catch (Throwable t) {
            throw getExceptionToThrowToClient(t);
        }
    }

    public void cancelOperationHistory(int historyId, boolean ignoreAgentErrors) throws RuntimeException {
        try {
            operationManager.cancelOperationHistory(getSessionSubject(), historyId, ignoreAgentErrors);
        } catch (Throwable t) {
            throw getExceptionToThrowToClient(t);
        }
    }
}
