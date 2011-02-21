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
package org.rhq.enterprise.gui.coregui.server.gwt;

import java.util.List;

import org.quartz.CronTrigger;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.criteria.GroupOperationHistoryCriteria;
import org.rhq.core.domain.criteria.ResourceOperationHistoryCriteria;
import org.rhq.core.domain.operation.GroupOperationHistory;
import org.rhq.core.domain.operation.ResourceOperationHistory;
import org.rhq.core.domain.operation.bean.GroupOperationSchedule;
import org.rhq.core.domain.operation.bean.ResourceOperationSchedule;
import org.rhq.core.domain.operation.composite.ResourceOperationLastCompletedComposite;
import org.rhq.core.domain.operation.composite.ResourceOperationScheduleComposite;
import org.rhq.core.domain.resource.composite.DisambiguationReport;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.core.util.IntExtractor;
import org.rhq.core.util.exception.ThrowableUtil;
import org.rhq.enterprise.gui.coregui.client.gwt.OperationGWTService;
import org.rhq.enterprise.gui.coregui.server.util.SerialUtility;
import org.rhq.enterprise.server.operation.OperationManagerLocal;
import org.rhq.enterprise.server.resource.ResourceManagerLocal;
import org.rhq.enterprise.server.resource.disambiguation.DefaultDisambiguationUpdateStrategies;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * @author Greg Hinkle
 * @author Ian Springer
 */
public class OperationGWTServiceImpl extends AbstractGWTServiceImpl implements OperationGWTService {

    private OperationManagerLocal operationManager = LookupUtil.getOperationManager();
    private ResourceManagerLocal resourceManager = LookupUtil.getResourceManager();

    public PageList<DisambiguationReport<ResourceOperationHistory>> findResourceOperationHistoriesByCriteria(
        ResourceOperationHistoryCriteria criteria) throws RuntimeException {
        try {
            PageList<ResourceOperationHistory> resourceOperationHistories = SerialUtility.prepare(operationManager
                .findResourceOperationHistoriesByCriteria(getSessionSubject(), criteria),
                "OperationService.findResourceOperationHistoriesByCriteria");

            List<DisambiguationReport<ResourceOperationHistory>> disambiguatedLastCompletedResourceOps = resourceManager
                .disambiguate(resourceOperationHistories, RESOURCE_OPERATION_HISTORY_RESOURCE_ID_EXTRACTOR,
                    DefaultDisambiguationUpdateStrategies.getDefault());

            return new PageList<DisambiguationReport<ResourceOperationHistory>>(disambiguatedLastCompletedResourceOps,
                resourceOperationHistories.getTotalSize(), resourceOperationHistories.getPageControl());
        } catch (Throwable t) {
            throw new RuntimeException(ThrowableUtil.getAllMessages(t));
        }
    }

    public PageList<GroupOperationHistory> findGroupOperationHistoriesByCriteria(GroupOperationHistoryCriteria criteria)
        throws RuntimeException {
        try {
            return SerialUtility.prepare(operationManager.findGroupOperationHistoriesByCriteria(getSessionSubject(),
                criteria), "OperationService.findGroupOperationHistoriesByCriteria");
        } catch (Throwable t) {
            throw new RuntimeException(ThrowableUtil.getAllMessages(t));
        }
    }

    public List<DisambiguationReport<GroupOperationHistory>> findGroupOperationHistoriesByCriteriaDisambiguated(
        GroupOperationHistoryCriteria criteria) throws RuntimeException {
        try {
            try {
                PageList<GroupOperationHistory> lastCompletedGroupOps = operationManager
                    .findGroupOperationHistoriesByCriteria(getSessionSubject(), criteria);

                //translate the returned groupOperationHistories to disambiguated links
                List<DisambiguationReport<GroupOperationHistory>> disambiguatedLastCompletedGroupOps = resourceManager
                    .disambiguate(lastCompletedGroupOps, GROUP_OPERATION_HISTORY_RESOURCE_ID_EXTRACTOR,
                        DefaultDisambiguationUpdateStrategies.getDefault());

                return SerialUtility.prepare(disambiguatedLastCompletedGroupOps,
                    "OperationService.findGroupOperationHistoriesByCriteriaDisambiguated");
            } catch (Throwable t) {
                throw new RuntimeException(ThrowableUtil.getAllMessages(t));
            }
        } catch (Throwable t) {
            throw new RuntimeException(ThrowableUtil.getAllMessages(t));
        }
    }

    public void deleteOperationHistory(int operationHistoryId, boolean deleteEvenIfInProgress) {
        try {
            operationManager.deleteOperationHistory(getSessionSubject(), operationHistoryId, deleteEvenIfInProgress);
        } catch (RuntimeException e) {
            throw new RuntimeException(ThrowableUtil.getAllMessages(e));
        }
    }

    public void invokeResourceOperation(int resourceId, String operationName, Configuration parameters,
        String description, int timeout) throws RuntimeException {
        try {
            ResourceOperationSchedule opSchedule = operationManager.scheduleResourceOperation(getSessionSubject(),
                resourceId, operationName, 0, 0, 0, 0, parameters, description);
        } catch (Throwable t) {
            throw new RuntimeException(ThrowableUtil.getAllMessages(t));
        }
    }

    public void scheduleResourceOperation(int resourceId, String operationName, Configuration parameters,
        String description, int timeout, String cronString) throws RuntimeException {
        try {
            CronTrigger cronTrigger = new CronTrigger("resource " + resourceId + "_" + operationName, "group",
                cronString);
            ResourceOperationSchedule opSchedule = operationManager.scheduleResourceOperation(getSessionSubject(),
                resourceId, operationName, parameters, cronTrigger, description);
        } catch (Throwable t) {
            throw new RuntimeException(ThrowableUtil.getAllMessages(t));
        }
    }

    public int scheduleResourceOperation(ResourceOperationSchedule resourceOperationSchedule) throws RuntimeException {
        try {
            return operationManager.scheduleResourceOperation(getSessionSubject(), resourceOperationSchedule);
        } catch (RuntimeException e) {
            e.printStackTrace();
            throw new RuntimeException(ThrowableUtil.getAllMessages(e));
        }
    }

    public int scheduleGroupOperation(GroupOperationSchedule groupOperationSchedule) throws RuntimeException {
        try {
            return operationManager.scheduleGroupOperation(getSessionSubject(), groupOperationSchedule);
        } catch (RuntimeException e) {
            e.printStackTrace();
            throw new RuntimeException(ThrowableUtil.getAllMessages(e));
        }
    }

    public ResourceOperationSchedule getResourceOperationSchedule(int scheduleId) {
        ResourceOperationSchedule resourceOperationSchedule = operationManager.getResourceOperationSchedule(
            getSessionSubject(), scheduleId);
        return SerialUtility.prepare(resourceOperationSchedule, "getResourceOperationSchedule");
    }

    public GroupOperationSchedule getGroupOperationSchedule(int scheduleId) {
        GroupOperationSchedule groupOperationSchedule = operationManager.getGroupOperationSchedule(getSessionSubject(),
            scheduleId);
        return SerialUtility.prepare(groupOperationSchedule, "getGroupOperationSchedule");
    }

    public void unscheduleResourceOperation(ResourceOperationSchedule resourceOperationSchedule)
        throws RuntimeException {
        try {
            operationManager.unscheduleResourceOperation(getSessionSubject(), resourceOperationSchedule.getJobId()
                .toString(), resourceOperationSchedule.getResource().getId());
        } catch (RuntimeException e) {
            e.printStackTrace();
            throw new RuntimeException(ThrowableUtil.getAllMessages(e));
        }
    }

    public void unscheduleGroupOperation(GroupOperationSchedule groupOperationSchedule) throws RuntimeException {
        try {
            operationManager.unscheduleGroupOperation(getSessionSubject(),
                groupOperationSchedule.getJobId().toString(), groupOperationSchedule.getGroup().getId());
        } catch (RuntimeException e) {
            e.printStackTrace();
            throw new RuntimeException(ThrowableUtil.getAllMessages(e));
        }
    }

    /** Find recently completed operations, disambiguate them and return that list.
     * 
     */
    public List<DisambiguationReport<ResourceOperationLastCompletedComposite>> findRecentCompletedOperations(
        int resourceId, PageControl pageControl) throws RuntimeException {
        Integer resourceIdentifier = null;
        if (resourceId > 0) {
            resourceIdentifier = new Integer(resourceId);
        }
        try {
            PageList<ResourceOperationLastCompletedComposite> lastCompletedResourceOps = operationManager
                .findRecentlyCompletedResourceOperations(getSessionSubject(), resourceIdentifier, pageControl);

            //translate the returned problem resources to disambiguated links
            List<DisambiguationReport<ResourceOperationLastCompletedComposite>> disambiguatedLastCompletedResourceOps = resourceManager
                .disambiguate(lastCompletedResourceOps, RESOURCE_OPERATION_RESOURCE_ID_EXTRACTOR,
                    DefaultDisambiguationUpdateStrategies.getDefault());

            return disambiguatedLastCompletedResourceOps;
        } catch (Throwable t) {
            throw new RuntimeException(ThrowableUtil.getAllMessages(t));
        }
    }

    /** Find scheduled operations, disambiguate them and return that list.
     * 
     */
    public List<DisambiguationReport<ResourceOperationScheduleComposite>> findScheduledOperations(int pageSize)
        throws RuntimeException {
        try {
            PageControl pageControl = new PageControl(0, pageSize);
            PageList<ResourceOperationScheduleComposite> scheduledResourceOps = operationManager
                .findCurrentlyScheduledResourceOperations(getSessionSubject(), pageControl);

            //translate the returned problem resources to disambiguated links
            List<DisambiguationReport<ResourceOperationScheduleComposite>> disambiguatedNextScheduledResourceOps = resourceManager
                .disambiguate(scheduledResourceOps, RESOURCE_OPERATION_SCHEDULE_RESOURCE_ID_EXTRACTOR,
                    DefaultDisambiguationUpdateStrategies.getDefault());

            return disambiguatedNextScheduledResourceOps;
        } catch (Throwable t) {
            throw new RuntimeException(ThrowableUtil.getAllMessages(t));
        }
    }

    public List<ResourceOperationSchedule> findScheduledResourceOperations(int resourceId) throws RuntimeException {
        try {
            List<ResourceOperationSchedule> resourceOperationSchedules = operationManager
                .findScheduledResourceOperations(getSessionSubject(), resourceId);
            return SerialUtility.prepare(resourceOperationSchedules, "findScheduledResourceOperations");
        } catch (Throwable t) {
            throw new RuntimeException(ThrowableUtil.getAllMessages(t));
        }
    }

    public List<GroupOperationSchedule> findScheduledGroupOperations(int groupId) throws RuntimeException {
        try {
            List<GroupOperationSchedule> groupOperationSchedules = operationManager.findScheduledGroupOperations(
                getSessionSubject(), groupId);
            return SerialUtility.prepare(groupOperationSchedules, "findScheduledGroupOperations");
        } catch (Throwable t) {
            throw new RuntimeException(ThrowableUtil.getAllMessages(t));
        }
    }

    private static final IntExtractor<GroupOperationHistory> GROUP_OPERATION_HISTORY_RESOURCE_ID_EXTRACTOR = new IntExtractor<GroupOperationHistory>() {
        public int extract(GroupOperationHistory groupOperationHistory) {
            return groupOperationHistory.getGroup().getId();
        }
    };

    private static final IntExtractor<ResourceOperationHistory> RESOURCE_OPERATION_HISTORY_RESOURCE_ID_EXTRACTOR = new IntExtractor<ResourceOperationHistory>() {
        public int extract(ResourceOperationHistory resourceOperationHistory) {
            return resourceOperationHistory.getResource().getId();
        }
    };

    private static final IntExtractor<ResourceOperationLastCompletedComposite> RESOURCE_OPERATION_RESOURCE_ID_EXTRACTOR = new IntExtractor<ResourceOperationLastCompletedComposite>() {
        public int extract(ResourceOperationLastCompletedComposite resourceOperationLastCompletedComposite) {
            return resourceOperationLastCompletedComposite.getResourceId();
        }
    };

    private static final IntExtractor<ResourceOperationScheduleComposite> RESOURCE_OPERATION_SCHEDULE_RESOURCE_ID_EXTRACTOR = new IntExtractor<ResourceOperationScheduleComposite>() {
        public int extract(ResourceOperationScheduleComposite resourceOperationScheduleComposite) {
            return resourceOperationScheduleComposite.getResourceId();
        }
    };

}
