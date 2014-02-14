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
package org.rhq.coregui.client.gwt;

import java.util.List;

import com.google.gwt.user.client.rpc.RemoteService;

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
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;

/**
 * @author Greg Hinkle
 */
public interface OperationGWTService extends RemoteService {

    PageList<ResourceOperationHistory> findResourceOperationHistoriesByCriteria(
        ResourceOperationHistoryCriteria criteria) throws RuntimeException;

    PageList<GroupOperationHistory> findGroupOperationHistoriesByCriteria(GroupOperationHistoryCriteria criteria)
        throws RuntimeException;

    void deleteOperationHistories(int[] operationHistoryIds, boolean deleteEvenIfInProgress) throws RuntimeException;

    PageList<ResourceOperationLastCompletedComposite> findRecentCompletedOperations(int resourceId,
        PageControl pageControl) throws RuntimeException;

    /**
     * Find currently scheduled resource operations
     * @param pageSize page size
     *
     * @return list of ResourceOperationScheduleComposites
     */
    public PageList<ResourceOperationScheduleComposite> findCurrentlyScheduledResourceOperations(int pageSize)
        throws RuntimeException;

    /**
     * Find currently scheduled group operations
     *
     * @param pageSize page size
     * @return list of GroupOperationScheduleComposite
     */
    public PageList<GroupOperationScheduleComposite> findCurrentlyScheduledGroupOperations(int pageSize)
        throws RuntimeException;


    void scheduleResourceOperation(int resourceId, String operationName, Configuration parameters, String description,
        int timeout) throws RuntimeException;

    int scheduleResourceOperation(ResourceOperationSchedule resourceOperationSchedule) throws RuntimeException;

    int scheduleGroupOperation(GroupOperationSchedule groupOperationSchedule) throws RuntimeException;

    ResourceOperationSchedule getResourceOperationSchedule(int scheduleId) throws RuntimeException;

    GroupOperationSchedule getGroupOperationSchedule(int scheduleId) throws RuntimeException;

    void unscheduleResourceOperation(ResourceOperationSchedule resourceOperationSchedule) throws RuntimeException;

    void unscheduleGroupOperation(GroupOperationSchedule groupOperationSchedule) throws RuntimeException;

    void scheduleResourceOperation(int resourceId, String operationName, Configuration parameters, String description,
        int timeout, String cronString) throws RuntimeException;

    List<ResourceOperationSchedule> findScheduledResourceOperations(int resourceId) throws RuntimeException;

    List<GroupOperationSchedule> findScheduledGroupOperations(int groupId) throws RuntimeException;

    /**
     * Cancels a currently in-progress operation. Doing this will attempt to stop the invocation if it is currently
     * running on the agent.
     *
     * <p>Note that this method will handle canceling a resource or group history - depending on what the given
     * <code>historyId</code> refers to. If it refers to a group history, it will cancel all the resource
     * invocations for that group invocation.</p>
     *
     * @param historyId         the ID of the group or resource history item identifying the in-progress operation
     * @param ignoreAgentErrors if <code>true</code> this will still flag the history items in the database as canceled,
     *                          even if the method failed to notify the agent(s) that the operation should be canceled.
     *                          If <code>false</code>, this method will not update the history status unless it could
     *                          successfully tell the agent(s) to cancel the operation.
     */
    void cancelOperationHistory(int historyId, boolean ignoreAgentErrors) throws RuntimeException;
}
