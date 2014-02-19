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
package org.rhq.enterprise.server.operation;

import java.util.List;

import javax.ejb.Remote;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.criteria.GroupOperationHistoryCriteria;
import org.rhq.core.domain.criteria.OperationDefinitionCriteria;
import org.rhq.core.domain.criteria.ResourceOperationHistoryCriteria;
import org.rhq.core.domain.operation.GroupOperationHistory;
import org.rhq.core.domain.operation.OperationDefinition;
import org.rhq.core.domain.operation.ResourceOperationHistory;
import org.rhq.core.domain.operation.bean.GroupOperationSchedule;
import org.rhq.core.domain.operation.bean.ResourceOperationSchedule;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.server.common.ApplicationException;
import org.rhq.enterprise.server.exception.ScheduleException;
import org.rhq.enterprise.server.exception.UnscheduleException;

/**
 * Public Operation Manager remote.
 */
@Remote
public interface OperationManagerRemote {

    /**
     * Cancels a currently in-progress operation. Doing this will attempt to stop the invocation if it is currently
     * running on the agent.
     *
     * <p>
     * Note that this method will handle canceling a resource or group history - depending on what the given <code>
     * historyId</code>
     * refers to. If it refers to a group history, it will cancel all the resource invocations for that group
     * invocation.
     * </p>
     *
     * <p>
     * If the cancel request succeeds, the history element will be checked against the AlertConditionCacheManager.
     * </p>
     *
     * @param subject
     *            The logged in user's subject.
     * @param operationHistoryId
     *            the ID of the history item identifying the in-progress operation
     * @param ignoreAgentErrors
     *            if <code>true</code> this will still flag the history items in the database as canceled, even if the
     *            method failed to notify the agent(s) that the operation should be canceled. If <code>false</code>,
     *            this method will not update the history status unless it could successfully tell the agent(s) to
     *            cancel the operation.
     */
    void cancelOperationHistory(Subject subject, int operationHistoryId, boolean ignoreAgentErrors);

    /**
     * Purges the history from the database. Doing this loses all audit trails of the invoked operation. This can handle
     * deleting a group or resource history.
     *
     * <p>
     * Note that this method will handle deleting a resource or group history - depending on what the given <code>
     * historyId</code>
     * refers to.
     * </p>
     *
     * @param subject
     *            The logged in user's subject.
     * @param operationHistoryId
     *            the ID of the history to be deleted
     * @param purgeInProgress
     *            if <code>true</code>, even if the operation is in progress, the history entity will be deleted. You
     *            normally do not want to purge operation histories until they are completed, so you normally pass in
     *            <code>false</code>, but a user might want to force it to be purged, in which case the UI will want
     *            to pass in <code>true</code>
     */
    void deleteOperationHistory(Subject subject, int operationHistoryId, boolean purgeInProgress);

    /**
     * Same as {@link #deleteOperationHistory(Subject, int, boolean)} but applied to all supplied historyIds.
     * Supports partial success.
     *
     * @param subject
     * @param historyIds
     * @param deleteEvenIfInProgress
     * @Throws {@link ApplicationException} Thrown if any history records fail to delete. Message indicates
     * partial success and gives detail on each failure.
     */
    void deleteOperationHistories(Subject subject, int[] historyIds, boolean deleteEvenIfInProgress)
        throws ApplicationException;

    /**
     * Schedules an operation for execution on the given resource.
     *
     * @param subject
     *            The logged in user's subject.
     * @param resourceId
     *            the resource that is the target of the operation
     * @param operationName
     *            the actual operation to invoke
     * @param delay
     *            the number of milliseconds to delay this operation, 0 for immediate start.
     * @param repeatInterval
     *            the number of milliseconds after completion to repeat this operation. 0 for no repeat.
     * @param repeatCount
     *            the number of times to repeat this operation. -1 infinite, 0 for no repeat.
     * @param timeout
     *            the number of seconds before this operation will fail due to timeout. 0 for no timeout.
     * @param parameters
     *            the names parameters for the operation.
     * @param description
     *            user-entered description of the job to be scheduled
     *
     * @return the information on the new schedule
     * @throws ScheduleException if failed to schedule the operation
     */
    ResourceOperationSchedule scheduleResourceOperation(Subject subject, int resourceId, String operationName,
        long delay, long repeatInterval, int repeatCount, int timeout, Configuration parameters, String description)
        throws ScheduleException;

    /**
     * Schedules a Resource operation for execution using the cron expression.
     *
     * @param  subject        the user who is asking to schedule the job
     * @param  resourceId     the resource that is the target of the operation
     * @param  operationName  the actual operation to invoke
     * @param  cronExpression the cron expression specifying the repetition.
     *                        For example:
     * <pre>
     *   0 0 12 * * ?              Fire at 12pm (noon) every day
     *   0 15 10 ? * *             Fire at 10:15am every day
     *   0 15 10 * * ?             Fire at 10:15am every day
     *   0 15 10 * * ? *           Fire at 10:15am every day
     *   0 15 10 * * ? 2005        Fire at 10:15am every day during the year 2005
     *   0 * 14 * * ?              every minute starting at 2pm and ending at 2:59pm, every day
     *   0 0/5 14 * * ?            every 5 minutes starting at 2pm and ending at 2:55pm, ev. d.
     *   0 0-5 14 * * ?            Fire every minute starting at 2pm and ending at 2:05pm, every day
     *   0 10,44 14 ? 3 WED        Fire at 2:10pm and at 2:44pm every Wednesday in the month of March.
     *   0 15 10 ? * MON-FRI       Fire at 10:15am every Monday, Tuesday, Wednesday, Thursday and Friday
     *   0 15 10 15 * ?            Fire at 10:15am on the 15th day of every month
     *   0 15 10 L * ?             Fire at 10:15am on the last day of every month
     *   0 15 10 ? * 6L            Fire at 10:15am on the last Friday of every month
     *   0 15 10 ? * 6L            Fire at 10:15am on the last Friday of every month
     *   0 15 10 ? * 6L 2002-2005  at 10:15am on every last Friday of every month during the years
     *   0 15 10 ? * 6#3           Fire at 10:15am on the third Friday of every month
     *   0 11 11 11 11 ?           Fire every November 11th at 11:11am.
     * </pre>
     * @param  timeout        the number of seconds before this operation will fail due to timeout. 0 for no timeout.
     * @param  parameters     the names parameters for the operation.
     * @param  description    user-entered description of the job to be scheduled
     *
     * @return the information on the new schedule
     *
     * @throws ScheduleException if failed to schedule the operation
     */
    ResourceOperationSchedule scheduleResourceOperationUsingCron(Subject subject, int resourceId, String operationName,
        String cronExpression, int timeout, Configuration parameters, String description) throws ScheduleException;

    /**
     * Unschedule the resource operation identified with the given job ID.
     *
     * @param subject
     *            The logged in user's subject.
     * @param jobId
     *            identifies the operation to unschedule
     * @param resourceId
     *            the ID of the resource whose operation is getting unscheduled
     * @throws UnscheduleException TODO
     * @throws UnscheduleException
     */
    void unscheduleResourceOperation(Subject subject, String jobId, int resourceId) throws UnscheduleException;

    /**
     * @param subject
     * @param groupId
     * @param executionOrderResourceIds
     * @param haltOnFailure
     * @param operationName
     * @param parameters
     * @param delay
     * @param repeatInterval
     * @param repeatCount
     * @param timeout
     * @param description
     * @return The group operation schedule
     * @throws ScheduleException
     */
    GroupOperationSchedule scheduleGroupOperation(Subject subject, int groupId, int[] executionOrderResourceIds,
        boolean haltOnFailure, String operationName, Configuration parameters, long delay, long repeatInterval,
        int repeatCount, int timeout, String description) throws ScheduleException;

    /**
     * Schedules an operation for execution on members of the given group using the cron expression.
     *
     * @param  subject                   the user who is asking to schedule the job
     * @param  groupId                   the compatible group whose member resources are the target of the operation
     * @param  executionOrderResourceIds optional order of execution - if not<code>null</code>, these are group members
     *                                   resource IDs in the order in which the operations are invoked
     * @param  haltOnFailure             if <code>true</code>, the group operation will halt whenever one individual
     *                                   resource fails to execute. When executing in order, this means once a failure
     *                                   occurs, the resources next in line to execute will abort and not attempt to
     *                                   execute. If not executing in any particular order, you are not guaranteed which
     *                                   will stop and which will continue since all are executed as fast as possible,
     *                                   but the group operation will attempt to stop as best it can.
     * @param  operationName             the actual operation to invoke
     * @param  parameters                optional parameters to pass into the operation
     * @param  cronExpression the cron expression specifying the repetition.
     *                        For example:
     * <pre>
     *   0 0 12 * * ?              Fire at 12pm (noon) every day
     *   0 15 10 ? * *             Fire at 10:15am every day
     *   0 15 10 * * ?             Fire at 10:15am every day
     *   0 15 10 * * ? *           Fire at 10:15am every day
     *   0 15 10 * * ? 2005        Fire at 10:15am every day during the year 2005
     *   0 * 14 * * ?              every minute starting at 2pm and ending at 2:59pm, every day
     *   0 0/5 14 * * ?            every 5 minutes starting at 2pm and ending at 2:55pm, ev. d.
     *   0 0-5 14 * * ?            Fire every minute starting at 2pm and ending at 2:05pm, every day
     *   0 10,44 14 ? 3 WED        Fire at 2:10pm and at 2:44pm every Wednesday in the month of March.
     *   0 15 10 ? * MON-FRI       Fire at 10:15am every Monday, Tuesday, Wednesday, Thursday and Friday
     *   0 15 10 15 * ?            Fire at 10:15am on the 15th day of every month
     *   0 15 10 L * ?             Fire at 10:15am on the last day of every month
     *   0 15 10 ? * 6L            Fire at 10:15am on the last Friday of every month
     *   0 15 10 ? * 6L            Fire at 10:15am on the last Friday of every month
     *   0 15 10 ? * 6L 2002-2005  at 10:15am on every last Friday of every month during the years
     *   0 15 10 ? * 6#3           Fire at 10:15am on the third Friday of every month
     *   0 11 11 11 11 ?           Fire every November 11th at 11:11am.
     * </pre>
     * @param  timeout          the number of seconds before this operation will fail due to timeout. 0 for no timeout.
     *                          are ignored and reset by this method)
     * @param  description      user-entered description of the job to be scheduled
     *
     * @return the information  on the new schedule
     *
     * @throws ScheduleException if failed to schedule the operation
     */
    GroupOperationSchedule scheduleGroupOperationUsingCron(Subject subject, int groupId,
        int[] executionOrderResourceIds, boolean haltOnFailure, String operationName, Configuration parameters,
        String cronExpression, int timeout, String description) throws ScheduleException;

    /**
     * Unschedule the group operation identified with the given job ID.
     *
     * @param  subject          the user who is asking to unschedule the operation
     * @param  jobId           identifies the operation to unschedule
     * @param  resourceGroupId the ID of the group whose operation is getting unscheduled
     * @throws UnscheduleException TODO
     */
    void unscheduleGroupOperation(Subject subject, String jobId, int resourceGroupId) throws UnscheduleException;

    /**
     * Returns the list of scheduled operations for the given resource. This only includes scheduled jobs on the
     * individual resource - it will not include schedules from groups, even if the resource is a member of a group that
     * has scheduled jobs.
     *
     * @param subject
     *            The logged in user's subject.
     * @param resourceId
     *
     * @return resource scheduled operations
     * @throws Exception TODO
     * @throws Exception
     */
    List<ResourceOperationSchedule> findScheduledResourceOperations(Subject subject, int resourceId) throws Exception;

    /**
     * Returns the list of scheduled operations for the given resource group. This only includes scheduled jobs on the
     * individual resource group.
     *
     * @param subject
     *            The logged in user's subject.
     * @param groupId
     *
     * @return resource scheduled operations
     * @throws Exception TODO
     * @throws Exception
     */
    List<GroupOperationSchedule> findScheduledGroupOperations(Subject subject, int groupId) throws Exception;

    /**
     * TODO: major release: this should return PageList as all our criteria finder do
     *
     * @param subject The logged in user's subject.
     * @param criteria The criteria object for the finding.
     * @return instance of PageList<OperationDefinition> (can be safely casted)
     */
    List<OperationDefinition> findOperationDefinitionsByCriteria(Subject subject, OperationDefinitionCriteria criteria);

    /**
     * @param subject
     * @param criteria
     * @return not null
     */
    PageList<ResourceOperationHistory> findResourceOperationHistoriesByCriteria(Subject subject,
        ResourceOperationHistoryCriteria criteria);

    /**
     * @param subject
     * @param criteria
     * @return not null
     */
    PageList<GroupOperationHistory> findGroupOperationHistoriesByCriteria(Subject subject,
        GroupOperationHistoryCriteria criteria);
}