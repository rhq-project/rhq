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

import java.util.Date;
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
import org.rhq.core.domain.operation.bean.GroupOperationSchedule;
import org.rhq.core.domain.operation.bean.ResourceOperationSchedule;
import org.rhq.core.domain.operation.composite.GroupOperationLastCompletedComposite;
import org.rhq.core.domain.operation.composite.GroupOperationScheduleComposite;
import org.rhq.core.domain.operation.composite.ResourceOperationLastCompletedComposite;
import org.rhq.core.domain.operation.composite.ResourceOperationScheduleComposite;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.server.exception.ScheduleException;

@Local
public interface OperationManagerLocal extends OperationManagerRemote {

    /**
     * TODO
     *
     * @param groupId
     *
     * @return
     */
    public List<IntegerOptionItem> getResourceNameOptionItems(int groupId);

    /**
     * Schedules a Resource operation for execution.
     *
     * @param subject the user who is asking to schedule the job
     * @param schedule the information describing the operation to be scheduled along with the schedule to be used
     *
     * @return the id of the {@link org.rhq.core.domain.operation.ResourceOperationScheduleEntity} created to track the
     *         scheduled operation
     */
    int scheduleResourceOperation(Subject subject, ResourceOperationSchedule schedule);

    /**
     * Schedules a Resource group operation for execution.
     *
     * @param subject the user who is asking to schedule the job
     * @param schedule the information describing the operation to be scheduled along with the schedule to be used
     *
     * @return the id of the {@link org.rhq.core.domain.operation.GroupOperationScheduleEntity} created to track the
     *         scheduled operation
     */
    int scheduleGroupOperation(Subject subject, GroupOperationSchedule schedule) throws ScheduleException;

    /**
     * Schedules an operation for execution on the given resource.
     *
     * @param  subject       the user who is asking to schedule the job
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
     * @param  subject                   the user who is asking to schedule the job
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
     * TODO
     *
     * @param whoami
     * @param scheduleId
     * @return
     */
    ResourceOperationSchedule getResourceOperationSchedule(Subject whoami, int scheduleId);

    /**
     * TODO
     *
     * @param whoami
     * @param scheduleId
     * @return
     */
    GroupOperationSchedule getGroupOperationSchedule(Subject whoami, int scheduleId);

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
     * Given a Resource operation job's details, this returns a schedule POJO corresponding to that job.
     *
     * @param subject
     * @param jobDetail
     *
     * @return the object that encapsulates the resource schedule, or null if the specified job is no longer scheduled
     */
    @Nullable
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
     * Given a group operation job's details, this returns a schedule POJO corresponding to that job.
     *
     * @param subject
     * @param jobDetail
     *
     * @return the object that encapsulates the group schedule, , or null if the specified job is no longer scheduled
     */
    @Nullable
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
     * @param  subject   the user that wants to see the history
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
     * @param  history the history with the data to be updated.  The history record will be created if id is set to 0.
     * Otherwise the record must already exist and will be updated.
     * @return the updated or newly created history
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
     * @param  subject    the user who wants to see the information
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
     * @param  subject    the user who wants to see the information
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
     * @param  subject     the user who wants to see the information
     * @param  operationId
     *
     * @return the OperationDefinition object that corresponds to this operationId
     */
    OperationDefinition getOperationDefinition(Subject subject, int operationId);

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

    /**
     * Returns the list of completed operation histories for the given resource. This will return all items that are no
     * longer INPROGRESS that were invoked as part of a group operation to which this resource belongs or on the
     * resource directly.
     *
     * @param  subject
     * @param  resourceId
     * @param  startDate filter used to show only results occurring after this epoch millis parameter, nullable
     * @param  endDate   filter used to show only results occurring before this epoch millis parameter, nullable
     * @param  pc
     * @return all operation histories for the given resource
     */
    PageList<ResourceOperationHistory> findCompletedResourceOperationHistories(Subject subject, int resourceId,
        Long startDate, Long endDate, PageControl pc);

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
     * TODO
     */
    OperationHistory getOperationHistoryByHistoryId(Subject subject, int historyId);

    /**
     * TODO
     */
    OperationHistory getOperationHistoryByJobId(Subject subject, String historyJobId);

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
     * Purge all operation history created before the specified time.  Note that all ResourceOperationHIstory for
     * an eligible GroupOperationHistory will be removed regardless of whether the resource-level history was
     * created before the specified time.
     * <p/>
     * In-Progress operation history is still purged, on the assumption that it is obsolete and possibly in a bad
     * state.
     *
     * @param purgeBeforeTime
     * @return the number of purged history records. GroupOperationHistory counts as 1 regardless of the number of
     * associated ResourceOperationHistory records.
     */
    int purgeOperationHistory(Date purgeBeforeTime);

    /**
     * This does the work of {@link #purgeOperationHistory(Date)}, targeting group or resource history, and
     * limits transaction size by applying a limit to the number of history records purged.  The records purged
     * are not done in any order, any history meeting the <b>purgeBeforeTime</b> constraint is eligible.
     *
     * @param purgeBeforeTime
     * @param isGroupPurge
     * @param limit
     * @return
     */
    int purgeOperationHistoryInNewTransaction(Date purgeBeforeTime, boolean isGroupPurge, int limit);
}