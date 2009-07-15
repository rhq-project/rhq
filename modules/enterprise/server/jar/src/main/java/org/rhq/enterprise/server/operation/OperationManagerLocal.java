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

import javax.ejb.Local;

import org.jetbrains.annotations.Nullable;
import org.quartz.JobDetail;
import org.quartz.SchedulerException;
import org.quartz.Trigger;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.common.composite.IntegerOptionItem;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.operation.GroupOperationHistory;
import org.rhq.core.domain.operation.OperationDefinition;
import org.rhq.core.domain.operation.OperationHistory;
import org.rhq.core.domain.operation.ResourceOperationHistory;
import org.rhq.core.domain.operation.ScheduleJobId;
import org.rhq.core.domain.operation.composite.GroupOperationLastCompletedComposite;
import org.rhq.core.domain.operation.composite.GroupOperationScheduleComposite;
import org.rhq.core.domain.operation.composite.ResourceOperationLastCompletedComposite;
import org.rhq.core.domain.operation.composite.ResourceOperationScheduleComposite;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.server.exception.DeleteException;
import org.rhq.enterprise.server.exception.FetchException;
import org.rhq.enterprise.server.exception.ScheduleException;
import org.rhq.enterprise.server.exception.UnscheduleException;
import org.rhq.enterprise.server.exception.UpdateException;

@Local
public interface OperationManagerLocal {
    public List<IntegerOptionItem> getResourceNameOptionItems(int groupId);

    /**
     * Schedules an operation for execution on the given resource.
     *
     * @param  subject        the user who is asking to schedule the job
     * @param  resourceId    the resource that is the target of the operation
     * @param  operationName the actual operation to invoke
     * @param  parameters    optional parameters to pass into the operation
     * @param  trigger       the schedule of when the job runs (the names and group names in this trigger are ignored
     *                       and reset by this method)
     * @param  description   user-entered description of the job to be scheduled
     *
     * @return the information on the new schedule
     *
     * @throws SchedulerException if failed to schedule the operation
     */
    ResourceOperationSchedule scheduleResourceOperation(Subject subject, int resourceId, String operationName,
        Configuration parameters, Trigger trigger, String description) throws SchedulerException;

    /**
     * Schedules an operation for execution on members of the given group.
     *
     * @param  subject                    the user who is asking to schedule the job
     * @param  groupId                   the group whose member resources are the target of the operation
     * @param  executionOrderResourceIds optional order of exection - if not<code>null</code>, these are group members
     *                                   resource IDs in the order in which the operations are invoked
     * @param  haltOnFailure             if <code>true</code>, the group operation will halt whenever one individual
     *                                   resource fails to execute. When executing in order, this means once a failure
     *                                   occurs, the resources next in line to execute will abort and not attempt to
     *                                   execute. If not executing in any particular order, you are not guaranteed which
     *                                   will stop and which will continue since all are executed as fast as possible,
     *                                   but the group operation will attempt to stop as best it can.
     * @param  operationName             the actual operation to invoke
     * @param  parameters                optional parameters to pass into the operation
     * @param  trigger                   the schedule of when the job runs (the names and group names in this trigger
     *                                   are ignored and reset by this method)
     * @param  description               user-entered description of the job to be scheduled
     *
     * @return the information on the new schedule
     *
     * @throws SchedulerException if failed to schedule the operation
     */
    GroupOperationSchedule scheduleGroupOperation(Subject subject, int groupId, int[] executionOrderResourceIds,
        boolean haltOnFailure, String operationName, Configuration parameters, Trigger trigger, String description)
        throws SchedulerException;

    /**
     * This will delete an operation schedule entity identified with the given job ID. Note that this does <b>not</b>
     * actually unschedule the job - it merely removed our schedule tracking entity. This schedule tracking entity is
     * really just used to provide some additional querying capabilities since it allows us to link a resource or group
     * ID with a Quartz job directly within SQL.
     *
     * <p>This method is really just for the {@link OperationJob} objects to clean out the schedule tracking entity when
     * the schedule has run its course and will no longer be triggered. The UI or other pieces of client code will
     * usually not have to ever use this method; if you are thinking about having a need to call this method, please
     * know what you are doing.</p>
     *
     * @param jobId
     */
    void deleteOperationScheduleEntity(ScheduleJobId jobId);

    /**
     * This allows you to update an operation schedule entity with a new next-fire-time.
     *
     * <p>This method is really just for the {@link OperationJob} objects to update the schedule tracking entity. The UI
     * or other pieces of client code will usually not have to ever use this method; if you are thinking about having a
     * need to call this method, please know what you are doing.</p>
     *
     * @param jobId
     * @param nextFireTime the next time this job is due to be triggered
     */
    void updateOperationScheduleEntity(ScheduleJobId jobId, long nextFireTime);

    /**
     * Returns the list of schedules for the group itself. The returned schedules will only be for operations scheduled
     * on the group; it will not include individually scheduled resource operations, even if the resource is a member of
     * the group.
     *
     * @param  subject
     * @param  groupId
     *
     * @return group scheduled operations
     *
     * @throws SchedulerException
     */
    List<GroupOperationSchedule> findScheduledGroupOperations(Subject subject, int groupId) throws SchedulerException;

    /**
     * Given a resource job's details, this returns the schedule for that resource job.
     *
     * @param subject 
     * @param jobDetail
     *
     * @return the object that encapsulates the resource schedule
     */
    ResourceOperationSchedule getResourceOperationSchedule(Subject subject, JobDetail jobDetail);

    /**
     * Given a resource job's id, this returns the schedule for that resource job.
     *
     * @param subject 
     * @param jobId
     *
     * @return the object that encapsulates the resource schedule
     *
     * @throws SchedulerException if failed to find the schedule
     */
    ResourceOperationSchedule getResourceOperationSchedule(Subject subject, String jobId) throws SchedulerException;

    /**
     * Given a group job's details, this returns the schedule for that group job.
     *
     * @param subject 
     * @param jobDetail
     *
     * @return the object that encapsulates the group schedule
     */
    GroupOperationSchedule getGroupOperationSchedule(Subject subject, JobDetail jobDetail);

    /**
     * Given a group job's id, this returns the schedule for that group job.
     *
     * @param subject 
     * @param jobId
     *
     * @return the object that encapsulates the group schedule
     *
     * @throws SchedulerException if failed to find the schedule
     */
    GroupOperationSchedule getGroupOperationSchedule(Subject subject, String jobId) throws SchedulerException;

    /**
     * Get the paged resource operation histories for a given group history.
     *
     * @param  subject    the user that wants to see the history
     * @param  historyId ID of the history to retrieve
     * @param  pc        the page control used for sorting and paging of results
     *
     * @return the requested page of sorted resource operation history results for the given group history
     */
    public PageList<ResourceOperationHistory> findResourceOperationHistoriesByGroupHistoryId(Subject subject,
        int historyId, PageControl pc);

    /**
     * Returns the list of completed operation histories for the group resource. This will return all items that are no
     * longer INPROGRESS that were invoked on this group. This only returns the group history item - not the
     * individual resource operation histories for the group member invocation results. See
     * {@link #findCompletedResourceOperationHistories(Subject, int, Long, Long, PageControl)} for that.
     *
     * @param  subject
     * @param  groupId
     * @param  pc
     *
     * @return all group histories
     */
    PageList<GroupOperationHistory> findCompletedGroupOperationHistories(Subject subject, int groupId, PageControl pc);

    /**
     * Returns the list of pending operation histories for the group resource. This will return all items that are still
     * INPROGRESS that were invoked on this group. This only returns the group history item - not the individual
     * resource operation histories for the group member invocation results. See
     * {@link #findPendingResourceOperationHistories(Subject, int, PageControl)} for that.
     *
     * @param  subject
     * @param  groupId
     * @param  pc
     *
     * @return all group histories
     */
    PageList<GroupOperationHistory> findPendingGroupOperationHistories(Subject subject, int groupId, PageControl pc);

    /**
     * This is called by the jobs so they can update the history. The job will pass in an updated history object, this
     * method just updates the database with the new data.
     *
     * <p>Every call to this method will trigger at least one check against the AlertConditionCacheManager. The first is
     * against the passed history element itself, which captures the creation events (history element will be in the
     * INPROGRESS state), as well as updated to ResourceOperationHistory elements (completed, failed, etc).</p>
     *
     * <p>Furthermore, since ResourceOperationHistory elements are reused for execution of group operations, this method
     * also checks to see if it is part of a larger group operation. If it is, and if it is the last member of that
     * group to finish, the group status will be updated. This will trigger a second check against the
     * AlertConditionCacheManager using that corresponding group history element.</p>
     *
     * @param  subject  the user that the job is executing under
     * @param  history the history with the data to be updated
     *
     * @return the updated history
     */
    OperationHistory updateOperationHistory(Subject subject, OperationHistory history);

    /**
     * This is, for all intents and purposes, and internal method.  It should be called just after updating any
     * OperationHistory element.  To date, this includes two places:
     * 
     * a) post-processing hook to updateOperationHistory(Subject whoami, OperationHistory history)
     * b) embedded inside the logic for checkForTimedOutOperations(Subject)
     * 
     * This method will perform the following logic:
     * 
     * 1) checks whether the entity corresponding to the passed id is part of a larger GroupOperationHistory
     * 2) if #1 is true, it will further check whether that entity was the last element from the group to complete
     * 3) if #2 is true, it will perform the necessary logic to moving the associated GroupOperationHistory entity
     *    to the appropriate termination state
     * 
     * @param historyId the integer id of the OperationHistory entity that needs to be checked
     */
    void checkForCompletedGroupOperation(int historyId);

    /**
     * Returns the definitions of all the operations supported by the given resource type.
     *
     * @param  subject
     * @param  resourceTypeId
     * @param  eagerLoaded if true the parametersConfigurationDefinition, resultsConfigurationDefinition, and
     *         resourceType fields are eagerly loaded, otherwise they are left as null references
     *
     * @return the operation definitions for the resource type
     */
    List<OperationDefinition> findSupportedResourceTypeOperations(Subject subject, int resourceTypeId,
        boolean eagerLoaded);

    /**
     * Returns the definitions of all the operations supported by the given group.
     *
     * @param  subject
     * @param  compatibleGroupId
     * @param  eagerLoaded if true the parametersConfigurationDefinition, resultsConfigurationDefinition, and
     *         resourceType fields are eagerly loaded, otherwise they are left as null references
     *
     * @return the operation definitions for the group
     */
    List<OperationDefinition> findSupportedGroupOperations(Subject subject, int compatibleGroupId, boolean eagerLoaded);

    /**
     * Returns the definition of the named operation supported by the given resource. If the operation is not valid for
     * the resource, an exception is thrown.
     *
     * @param  subject
     * @param  resourceId
     * @param  operationName
     * @param  eagerLoaded if true the parametersConfigurationDefinition, resultsConfigurationDefinition, and
     *         resourceType fields are eagerly loaded, otherwise they are left as null references
     *
     * @return the named operation definition for the resource
     */
    OperationDefinition getSupportedResourceOperation(Subject subject, int resourceId, String operationName,
        boolean eagerLoaded);

    /**
     * Returns the definition of the named operation supported by the given group. If the operation is not valid for the
     * group, an exception is thrown.
     *
     * @param  subject
     * @param  compatibleGroupId
     * @param  operationName
     * @param  eagerLoaded if true the parametersConfigurationDefinition, resultsConfigurationDefinition, and
     *         resourceType fields are eagerly loaded, otherwise they are left as null references
     *
     * @return the named operation definition for the group
     */
    OperationDefinition getSupportedGroupOperation(Subject subject, int compatibleGroupId, String operationName,
        boolean eagerLoaded);

    /**
     * Determines if the given resource has at least one operation.
     *
     * @param  subject
     * @param  resourceId
     *
     * @return <code>true</code> if the resource has at least one operation
     */
    boolean isResourceOperationSupported(Subject subject, int resourceId);

    /**
     * Determines if the given group has at least one operation.
     *
     * @param  subject
     * @param  resourceGroupId
     *
     * @return <code>true</code> if the group has at least one operation
     */
    boolean isGroupOperationSupported(Subject subject, int resourceGroupId);

    /**
     * Will check to see if any in progress operation jobs are taking too long to finish and if so marks their histories
     * as failed. This method will be perodically called by the JON Server.
     *
     * <p>Calls to this method could trigger a number of checks against the AlertConditionCacheManager. The first set of
     * checks would be against any and all resource operations that have timed out. Then, for any of these timeouts,
     * since ResourceOperationHistory elements are reused for execution of group operations, this method also checks to
     * see if it is part of a larger group operation. If it is, and if it is the last member of that group to finish,
     * the group status will be updated. This will trigger another check against the AlertConditionCacheManager using
     * that corresponding group history element.</p>
     *
     * @param whoami only the overlord may execute this system operation
     */
    void checkForTimedOutOperations(Subject whoami);

    /**
     * Get the mostly recently run operation for the {@link Resource} with the given id, or <code>null</code> if the
     * resource has had no operations performed against it yet (or if all previously performed operations have been
     * deleted from the history). Returns the result of the operation as it is known on the server-side in the database.
     *
     * @param  subject     the user who wants to see the information
     * @param  resourceId a {@link Resource} id
     *
     * @return the most recent operation performed against the {@link Resource} with the given id, or <code>null</code>
     *         if the resource has had no operations performed against it yet.
     */
    @Nullable
    ResourceOperationHistory getLatestCompletedResourceOperation(Subject subject, int resourceId);

    /**
     * Get the oldest operation still in progress for the {@link Resource} with the given id, or <code>null</code> if
     * the resource has no operations being performed against it. Returns the INPROCESS element with empty results.
     *
     * @param  subject     the user who wants to see the information
     * @param  resourceId a {@link Resource} id
     *
     * @return the oldest operation still in progress for the {@link Resource} with the given id, or <code>null</code>
     *         if the resource has no operations being performed against it.
     */
    @Nullable
    ResourceOperationHistory getOldestInProgressResourceOperation(Subject subject, int resourceId);

    /**
     * Get the OperationDefinition object that corresponds to this operationId
     *
     * @param  whoami      the user who wants to see the information
     * @param  operationId
     *
     * @return the OperationDefinition object that corresponds to this operationId
     */
    OperationDefinition getOperationDefinition(Subject whoami, int operationId);

    /**
     * @param  resourceTypeId
     * @param  operationName
     * @param  eagerLoaded if true the parametersConfigurationDefinition, resultsConfigurationDefinition, and
     *         resourceType fields are eagerly loaded, otherwise they are left as null references
     *
     * @return the operation definition
     *
     * @throws OperationDefinitionNotFoundException
     */
    OperationDefinition getOperationDefinitionByResourceTypeAndName(int resourceTypeId, String operationName,
        boolean eagerLoaded) throws OperationDefinitionNotFoundException;

    /**
     * Gets a list of all recently completed resource operations. This is used to support the dashboard operations
     * portlet. <b>WARNING:</b> Do not send in a page control that asks for an unlimited page list - depending on the
     * number of operations in the system's history, that kind of request could blow up.
     *
     * <p>Only resource operations are returned (group operations are ignored by this method). However, if a group
     * operation was executed, all of the resource executions that occurred as part of it will be returned (that is, the
     * resources in that group will have its resource operations returned.</p>
     *
     * @param  subject    the user asking for the data; the returned list is limited to what this user can see
     * @param  resourceId if non-null, will only result recent completed operations for this resource
     * @param  pc         limits the number of composite objects returned
     *
     * @return the list of recently completed resource operations
     */
    PageList<ResourceOperationLastCompletedComposite> findRecentlyCompletedResourceOperations(Subject subject,
        Integer resourceId, PageControl pc);

    /**
     * Gets a list of all recently completed group operations. This is used to support the dashboard operations portlet.
     * <b>WARNING:</b> Do not send in a page control that asks for an unlimited page list - depending on the number of
     * operations in the system's history, that kind of request could blow up.
     *
     * @param  subject the user asking for the data; the returned list is limited to what this user can see
     * @param  pc      limits the number of composite objects returned
     *
     * @return the list of recently completed group operations
     */
    PageList<GroupOperationLastCompletedComposite> findRecentlyCompletedGroupOperations(Subject subject, PageControl pc);

    /**
     * Gets a list of all currently scheduled resource operations (that is, scheduled but not yet invoked and/or
     * completed).
     *
     * @param  subject the user asking for the data; the returned list is limited to what this user can see
     * @param  pc      limits the number of composite objects returned
     *
     * @return the list of scheduled resource operations
     */
    PageList<ResourceOperationScheduleComposite> findCurrentlyScheduledResourceOperations(Subject subject,
        PageControl pc);

    /**
     * Gets a list of all currently scheduled group operations (that is, scheduled but not yet invoked and/or
     * completed).
     *
     * @param  subject the user asking for the data; the returned list is limited to what this user can see
     * @param  pc      limits the number of composite objects returned
     *
     * @return the list of scheduled group operations
     */
    PageList<GroupOperationScheduleComposite> findCurrentlyScheduledGroupOperations(Subject subject, PageControl pc);

    // !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
    //
    // The following are shared with the Remote Interface
    //
    // !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!

    /**
     * Cancels a currently in-progress operation. Doing this will attempt to stop the invocation if it is currently
     * running on the agent.
     *
     * <p>Note that this method will handle canceling a resource or group history - depending on what the given <code>
     * historyId</code> refers to. If it refers to a group history, it will cancel all the resource invocations for that
     * group invocation.</p>
     *
     * <p>If the cancel request succeeds, the history element will be checked against the
     * AlertConditionCacheManager.</p>
     *
     * @param subject            the user that wants to cancel the operation
     * @param historyId         the ID of the history item identifying the in-progress operation
     * @param ignoreAgentErrors if <code>true</code> this will still flag the history items in the database as canceled,
     *                          even if the method failed to notify the agent(s) that the operation should be canceled.
     *                          If <code>false</code>, this method will not update the history status unless it could
     *                          successfully tell the agent(s) to cancel the operation.
     * @throws UpdateException
     */
    void cancelOperationHistory(Subject subject, int historyId, boolean ignoreAgentErrors) throws UpdateException;

    /**
     * Purges the history from the database. Doing this loses all audit trails of the invoked operation. This can handle
     * deleting a group or resource history.
     *
     * <p>Note that this method will handle deleting a resource or group history - depending on what the given <code>
     * historyId</code> refers to.</p>
     *
     * @param subject          the user that wants to delete the history
     * @param historyId       the ID of the history to be deleted
     * @param purgeInProgress if <code>true</code>, even if the operation is in progress, the history entity will be
     *                        deleted. You normally do not want to purge operation histories until they are completed,
     *                        so you normally pass in <code>false</code>, but a user might want to force it to be
     *                        purged, in which case the UI will want to pass in <code>true</code>
     * @throws DeleteException
     */
    void deleteOperationHistory(Subject subject, int historyId, boolean purgeInProgress) throws DeleteException;

    /**
     * Returns the list of completed operation histories for the given resource. This will return all items that are no
     * longer INPROGRESS that were invoked as part of a group operation to which this resource belongs or on the
     * resource directly.
     *
     * @param  subject
     * @param  resourceId
     * @param  beginDate filter used to show only results occurring after this epoch millis parameter, nullable
     * @param  endDate   filter used to show only results occurring before this epoch millis parameter, nullable
     * @param  pc
     * @return all operation histories for the given resource
     */
    PageList<ResourceOperationHistory> findCompletedResourceOperationHistories(Subject subject, int resourceId,
        Long startDate, Long endDate, PageControl pc);

    /**
     * #see {@link OperationManagerRemote#findOperationHistories
     */
    PageList<ResourceOperationHistory> findOperationHistories(Subject subject, ResourceOperationHistory criteria,
        PageControl pc) throws FetchException;

    /**
     * Returns the list of pending operation histories for the given resource. This will return all items that are still
     * INPROGRESS that were invoked as part of a group operation to which this resource belongs or on the resource
     * directly.
     *
     * @param  subject
     * @param  resourceId
     * @param  pc
     *
     * @return all operation histories for the given resource
     */
    PageList<ResourceOperationHistory> findPendingResourceOperationHistories(Subject subject, int resourceId,
        PageControl pc);

    /**
     * #see {@link OperationManagerRemote#getOperationHistoryByHistoryId
     */
    OperationHistory getOperationHistoryByHistoryId(Subject subject, int historyId) throws FetchException;

    /**
     * #see {@link OperationManagerRemote#getOperationHistoryByJobId
     */
    OperationHistory getOperationHistoryByJobId(Subject subject, String historyJobId) throws FetchException;

    /**
     * Returns the list of scheduled operations for the given resource. This only includes scheduled jobs on the
     * individual resource - it will not include schedules from groups, even if the resource is a member of a group that
     * has scheduled jobs.
     *
     * @param  subject
     * @param  resourceId
     *
     * @return resource scheduled operations
     *
     * @throws SchedulerException
     */
    List<ResourceOperationSchedule> findScheduledResourceOperations(Subject subject, int resourceId)
        throws SchedulerException;

    /**
     * Returns the definitions of all the operations supported by the given resource.
     *
     * @param  subject
     * @param  resourceId
     * @param  eagerLoaded if true the parametersConfigurationDefinition, resultsConfigurationDefinition, and
     *         resourceType fields are eagerly loaded, otherwise they are left as null references
     *
     * @return the operation definitions for the resource
     */
    List<OperationDefinition> findSupportedResourceOperations(Subject subject, int resourceId, boolean eagerLoaded);

    /**
     * #see {@link OperationManagerRemote#scheduleGroupOperation
     */
    GroupOperationSchedule scheduleGroupOperation(Subject subject, int groupId, int[] executionOrderResourceIds,
        boolean haltOnFailure, String operationName, Configuration parameters, long delay, long repeatInterval,
        int repeatCount, int timeout, String description) throws ScheduleException;

    /**
     * Schedules an operation for execution on the given resource.
     *
     * @param  subject           The logged in user's subject.
     * @param  resourceId     the resource that is the target of the operation
     * @param  operationName  the actual operation to invoke
     * @param  delay          the number of milliseconds to delay this operation, 0 for immediate start.
     * @param  repeatInterval the number of milliseconds after completion to repeat this operation. 0 for no repeat.
     * @param  repeatCount    the number of times to repeat this operation. -1 infinite, 0 for no repeat. 
     * @param  timeout        the number of seconds before this operation will fail due to timeout. 0 for no timeout.
     * @param  parameters     the names parameters for the operation. 
     * @param  description    user-entered description of the job to be scheduled
     *
     * @return the information on the new schedule
     * @throws ScheduleException TODO
     */
    ResourceOperationSchedule scheduleResourceOperation(Subject subject, int resourceId, String operationName,
        long delay, long repeatInterval, int repeatCount, int timeout, Configuration parameters, String description)
        throws ScheduleException;

    /**
     * Unschedules the resource operation identified with the given job ID.
     *
     * @param  subject     the user who is asking to unschedule the operation
     * @param  jobId      identifies the operation to unschedule
     * @param  resourceId the ID of the resource whose operation is getting unscheduled
     * @throws UnscheduleException TODO
     */
    void unscheduleResourceOperation(Subject subject, String jobId, int resourceId) throws UnscheduleException;

    /**
     * #see {@link OperationManagerRemote#unscheduleGroupOperation(Subject, String, PageControl)
     */
    void unscheduleGroupOperation(Subject subject, String jobId, int resourceGroupId) throws UnscheduleException;

}