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
import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebService;
import javax.jws.soap.SOAPBinding;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.criteria.GroupOperationHistoryCriteria;
import org.rhq.core.domain.criteria.OperationDefinitionCriteria;
import org.rhq.core.domain.criteria.ResourceOperationHistoryCriteria;
import org.rhq.core.domain.operation.GroupOperationHistory;
import org.rhq.core.domain.operation.OperationDefinition;
import org.rhq.core.domain.operation.ResourceOperationHistory;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.server.exception.DeleteException;
import org.rhq.enterprise.server.exception.ScheduleException;
import org.rhq.enterprise.server.exception.UnscheduleException;
import org.rhq.enterprise.server.exception.UpdateException;

@SOAPBinding(style = SOAPBinding.Style.DOCUMENT)
@WebService
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
     * @param historyId
     *            the ID of the history item identifying the in-progress operation
     * @param ignoreAgentErrors
     *            if <code>true</code> this will still flag the history items in the database as canceled, even if the
     *            method failed to notify the agent(s) that the operation should be canceled. If <code>false</code>,
     *            this method will not update the history status unless it could successfully tell the agent(s) to
     *            cancel the operation.
     * @throws UpdateException
     */
    @WebMethod
    void cancelOperationHistory( //
        @WebParam(name = "subject") Subject subject, //
        @WebParam(name = "operationHistoryId") int operationHistoryId, //
        @WebParam(name = "ignoreAgentErrors") boolean ignoreAgentErrors) //
        throws UpdateException;

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
     * @param historyId
     *            the ID of the history to be deleted
     * @param purgeInProgress
     *            if <code>true</code>, even if the operation is in progress, the history entity will be deleted. You
     *            normally do not want to purge operation histories until they are completed, so you normally pass in
     *            <code>false</code>, but a user might want to force it to be purged, in which case the UI will want
     *            to pass in <code>true</code>
     * @throws DeleteException
     */
    @WebMethod
    void deleteOperationHistory( //
        @WebParam(name = "subject") Subject subject, //
        @WebParam(name = "operationHistoryId") int operationHistoryId, //
        @WebParam(name = "purgeInProgress") boolean purgeInProgress) //
        throws DeleteException;

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
     * @throws ScheduleException TODO
     */
    @WebMethod
    ResourceOperationSchedule scheduleResourceOperation( //
        @WebParam(name = "subject") Subject subject, //
        @WebParam(name = "resourceid") int resourceId, //
        @WebParam(name = "operationName") String operationName, //
        @WebParam(name = "delay") long delay, //
        @WebParam(name = "repeatInterval") long repeatInterval, //
        @WebParam(name = "repeatCount") int repeatCount, //
        @WebParam(name = "timeout") int timeout, //
        @WebParam(name = "parameters") Configuration parameters, //
        @WebParam(name = "description") String description) //
        throws ScheduleException;

    /**
     * Unschedules the resource operation identified with the given job ID.
     *
     * @param subject
     *            The logged in user's subject.
     * @param jobId
     *            identifies the operation to unschedule
     * @param resourceId
     *            the ID of the resource whose operation is getting unscheduled
     * @throws UnscheduleException TODO
     * @throws Exception
     */
    @WebMethod
    void unscheduleResourceOperation( //
        @WebParam(name = "subject") Subject subject, //
        @WebParam(name = "jobId") String jobId, //
        @WebParam(name = "resourceId") int resourceId) //
        throws UnscheduleException;

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
     * @return
     * @throws ScheduleException
     */
    @WebMethod
    GroupOperationSchedule scheduleGroupOperation(//
        @WebParam(name = "subject") Subject subject,//
        @WebParam(name = "groupId") int groupId,//
        @WebParam(name = "executionOrderResourceIds") int[] executionOrderResourceIds,//
        @WebParam(name = "haltOnFailure") boolean haltOnFailure,//
        @WebParam(name = "operationName") String operationName,//
        @WebParam(name = "parameters") Configuration parameters,//
        @WebParam(name = "delay") long delay,//
        @WebParam(name = "repeatInterval") long repeatInterval,//
        @WebParam(name = "repeatCount") int repeatCount,//
        @WebParam(name = "timeout") int timeout,//
        @WebParam(name = "description") String description)//
        throws ScheduleException;

    /**
     * Unschedules the group operation identified with the given job ID.
     *
     * @param  subject          the user who is asking to unschedule the operation
     * @param  jobId           identifies the operation to unschedule
     * @param  resourceGroupId the ID of the group whose operation is getting unscheduled
     * @throws UnscheduleException TODO
     */
    @WebMethod
    void unscheduleGroupOperation(//
        @WebParam(name = "subject") Subject subject,//
        @WebParam(name = "jobId") String jobId,//
        @WebParam(name = "resourceGroupId") int resourceGroupId)//
        throws UnscheduleException;

    /**
     * Returns the list of scheduled operations for the given resource. This only includes scheduled jobs on the
     * individual resource - it will not include schedules from groups, even if the resource is a member of a group that
     * has scheduled jobs.
     *
     * @param user
     *            The logged in user's subject.
     * @param resourceId
     *
     * @return resource scheduled operations
     *
     * @throws Exception
     */
    @WebMethod
    List<ResourceOperationSchedule> findScheduledResourceOperations( //
        @WebParam(name = "subject") Subject subject, //
        @WebParam(name = "resourceId") int resourceId) //
        throws Exception;

    @WebMethod
    List<GroupOperationSchedule> findScheduledGroupOperations( //
        @WebParam(name = "subject") Subject subject, //
        @WebParam(name = "groupId") int groupId) //
        throws Exception;

    @WebMethod
    List<OperationDefinition> findOperationDefinitionsByCriteria( //
        @WebParam(name = "subject") Subject subject, //
        @WebParam(name = "criteria") OperationDefinitionCriteria criteria);

    @WebMethod
    PageList<ResourceOperationHistory> findResourceOperationHistoriesByCriteria( //
        @WebParam(name = "subject") Subject subject, //
        @WebParam(name = "criteria") ResourceOperationHistoryCriteria criteria);

    @WebMethod
    PageList<GroupOperationHistory> findGroupOperationHistoriesByCriteria( //
        @WebParam(name = "subject") Subject subject, //
        @WebParam(name = "criteria") GroupOperationHistoryCriteria criteria);
}