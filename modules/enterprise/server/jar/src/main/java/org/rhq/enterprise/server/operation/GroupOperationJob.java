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

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.operation.GroupOperationHistory;
import org.rhq.core.domain.operation.OperationDefinition;
import org.rhq.core.domain.operation.OperationHistory;
import org.rhq.core.domain.operation.OperationRequestStatus;
import org.rhq.core.domain.operation.ResourceOperationHistory;
import org.rhq.core.domain.operation.bean.GroupOperationSchedule;
import org.rhq.core.domain.operation.bean.ResourceOperationSchedule;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.group.ResourceGroup;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.util.exception.ThrowableUtil;
import org.rhq.enterprise.server.auth.SubjectManagerLocal;
import org.rhq.enterprise.server.resource.ResourceManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * A job that invokes an operation on all resources that are members of a group.
 *
 * @author John Mazzitelli
 */
public class GroupOperationJob extends OperationJob {
    private static final Log log = LogFactory.getLog(GroupOperationJob.class);

    public static final String DATAMAP_INT_GROUP_ID = "groupId";
    public static final String DATAMAP_INT_ARRAY_EXECUTION_ORDER = "executionOrder"; // comma-separated list of IDs
    public static final String DATAMAP_BOOL_HALT_ON_FAILURE = "haltOnFailure";
    public static final int BREAK_VALUE = 1000 * 60 * 60 * 24;

    class ResourceOperationDetailsComposite {
        Resource resource;
        ResourceOperationHistory history;
        ResourceOperationSchedule schedule;

        public ResourceOperationDetailsComposite(Resource resource, ResourceOperationHistory history,
            ResourceOperationSchedule schedule) {
            this.resource = resource;
            this.history = history;
            this.schedule = schedule;
        }
    }

    /**
     * Prefix for all job names and job groups names of group operations.
     */
    private static final String GROUP_JOB_NAME_PREFIX = "rhq-group-";

    public static String createUniqueJobName(ResourceGroup group, String operationName) {
        return GROUP_JOB_NAME_PREFIX + group.getId() + "-" + operationName.hashCode() + "-"
            + System.currentTimeMillis();
    }

    public static String createJobGroupName(ResourceGroup group) {
        return GROUP_JOB_NAME_PREFIX + group.getId();
    }

    public void execute(JobExecutionContext context) throws JobExecutionException {
        GroupOperationSchedule schedule = null;
        GroupOperationHistory groupHistory;
        Subject user = null;

        try {
            JobDetail jobDetail = context.getJobDetail();
            OperationManagerLocal operationManager = LookupUtil.getOperationManager();

            updateOperationScheduleEntity(jobDetail, context.getNextFireTime(), operationManager);

            // we only got here because the user was allowed to execute / schedule the operation in the first place,
            // thus it's safe to pass in the overlord here
            schedule = operationManager.getGroupOperationSchedule(LookupUtil.getSubjectManager().getOverlord(),
                jobDetail);
            if (schedule == null) {
                throw new CancelJobException("Resource Schedule no longer exists, canceling job");
            }

            // create a new session even if user is logged in elsewhere, we don't want to attach to that user's session
            user = getUserWithSession(schedule.getSubject(), false);
            ResourceGroup group = schedule.getGroup();

            // we need the operation definition to fill in the history item
            OperationDefinition op;
            op = operationManager.getSupportedGroupOperation(user, group.getId(), schedule.getOperationName(), false);

            // first we need to create an INPROGRESS *group* history item
            Configuration parameters = schedule.getParameters();
            if (parameters != null) {
                parameters = parameters.deepCopy(false); // we need a copy to avoid constraint violations upon delete
            }

            groupHistory = new GroupOperationHistory(jobDetail.getName(), jobDetail.getGroup(), user.getName(), op,
                parameters, group);

            groupHistory = (GroupOperationHistory) operationManager.updateOperationHistory(user, groupHistory);

            // get the resources to operate on, ordered or not
            List<Resource> resourcesToOperateOn;
            if (schedule.getExecutionOrder() != null) {
                resourcesToOperateOn = schedule.getExecutionOrder();
            } else {
                ResourceManagerLocal resourceManager = LookupUtil.getResourceManager();
                PageControl pageControl = PageControl.getUnlimitedInstance();
                resourcesToOperateOn = resourceManager.findExplicitResourcesByResourceGroup(user, group, pageControl);
            }

            // now create detail composites from the resource list
            List<ResourceOperationDetailsComposite> resourceComposites = new ArrayList<ResourceOperationDetailsComposite>();
            getUserWithSession(user, true); // refresh our session to reset the timeout clock
            for (Resource nextResourceToOperateOn : resourcesToOperateOn) {
                // create the non-quartz schedule entity for the given job execution context data
                ResourceOperationSchedule resourceSchedule = createScheduleForResource(schedule, jobDetail.getGroup(),
                    user, nextResourceToOperateOn);

                // create the resource-level history entity for the newly created non-quartz schedule entity
                // this method also does the persisting
                ResourceOperationHistory resourceHistory = createOperationHistory(resourceSchedule.getJobName(),
                    resourceSchedule.getJobGroup(), resourceSchedule, groupHistory, operationManager);

                // add all three elements to the composite, which will be iterated over below for the bulk of the work
                resourceComposites.add(new ResourceOperationDetailsComposite(nextResourceToOperateOn, resourceHistory,
                    resourceSchedule));
            }

            // now tell the agents to invoke the operation for all resources
            if (schedule.getExecutionOrder() != null) {
                boolean hadFailure = false;

                // synchronously execute, waiting for each operation to finish before going to the next
                for (ResourceOperationDetailsComposite composite : resourceComposites) {
                    try {
                        if (hadFailure) {
                            // there was a failure during execution of this group operation;
                            // thus, mark all remaining operation histories as cancelled
                            composite.history.setErrorMessage("This has been cancelled due to halt-on-error "
                                + "being set on the parent group operation schedule. "
                                + "A previous resource operation that executed prior "
                                + "to this resource operation failed, thus causing "
                                + "this resource operation to be cancelled.");
                            composite.history.setStatus(OperationRequestStatus.CANCELED);
                            composite.history = (ResourceOperationHistory) operationManager.updateOperationHistory(
                                getUserWithSession(user, true), composite.history);
                            continue;
                        }

                        invokeOperationOnResource(composite, operationManager);

                        int resourceHistoryId = composite.history.getId();
                        OperationHistory updatedOperationHistory = null;
                        long sleep = 1000L; // quick sleep for fast ops, then slow down
                        long maxSleep = 5000L;
                        do {
                            Thread.sleep(sleep);
                            sleep = (sleep == maxSleep) ? sleep : sleep + 1000L;

                            // it's unlikely but possible that a client program could actually query for, process, and
                            // delete the history before this code gets a chance to run.  If the record is gone just
                            // assume things were handled externally.
                            try {
                                updatedOperationHistory = operationManager.getOperationHistoryByHistoryId(
                                    getUserWithSession(user, true), resourceHistoryId);
                            } catch (IllegalArgumentException e) {
                                if (log.isDebugEnabled()) {
                                    log.debug("Failed to find operation history", e);
                                }
                                break;
                            }

                            // if the duration was ridiculously long, let's break out of here. this will rarely
                            // be triggered because our operation manager will timeout long running operations for us
                            // (based on the operation's define timeout).  But, me being paranoid, I want to be able
                            // to break this infinite loop if for some reason the operation manager isn't doing its job.
                            // if the operation took longer than 24 hours, this breaks the loop.

                            if (updatedOperationHistory.getDuration() > (GroupOperationJob.BREAK_VALUE)) {
                                break;
                            }
                        } while (updatedOperationHistory.getStatus() == OperationRequestStatus.INPROGRESS);

                        // halt the rest if we got a failure and were told not to go on
                        if (null != updatedOperationHistory
                            && (updatedOperationHistory.getStatus() != OperationRequestStatus.SUCCESS)
                            && schedule.isHaltOnFailure()) {
                            hadFailure = true;
                        }
                    } catch (Exception e) {
                        // failed to even send to the agent, immediately mark the job as failed
                        groupHistory.setErrorMessage(ThrowableUtil.getStackAsString(e));
                        groupHistory = (GroupOperationHistory) operationManager.updateOperationHistory(
                            getUserWithSession(user, true), groupHistory);

                        if (schedule.isHaltOnFailure()) {
                            hadFailure = true;
                        }
                    }
                }
            } else {
                // send the invocation requests without waiting for each to return
                for (ResourceOperationDetailsComposite composite : resourceComposites) {
                    try {
                        invokeOperationOnResource(composite, operationManager);
                    } catch (Exception e) {
                        if (e instanceof CancelJobException) {
                            throw e;
                        }

                        // failed to even send to the agent, immediately mark the job as failed
                        groupHistory.setErrorMessage(ThrowableUtil.getStackAsString(e));
                        groupHistory = (GroupOperationHistory) operationManager.updateOperationHistory(
                            getUserWithSession(user, true), groupHistory);

                        // Note: in actuality - I don't think users have a way in the user interface to turn on halt-on-failure for parallel execution.
                        // So this isHaltOnFailure will probably always be false. But in case we want to support this, leave this here.
                        // What will happen is we will stop sending requests to the agents to invoke more resource operations. Any previous
                        // resource operations invoked, however, will still be running and allowed to finish on their respective agents.
                        if (schedule.isHaltOnFailure()) {
                            throw e;
                        }
                    }
                }
            }
        } catch (Exception e) {
            if (e instanceof CancelJobException) {
                throw (CancelJobException) e;
            }

            String error = "Failed to execute scheduled operation [" + schedule + "]";
            LogFactory.getLog(GroupOperationJob.class).error(error, e);
            throw new JobExecutionException(error, e, false);
        } finally {
            // clean up our temporary session by logging out of it
            try {
                if (user != null) {
                    SubjectManagerLocal subjectMgr = LookupUtil.getSubjectManager();
                    subjectMgr.logout(user);
                }
            } catch (Exception e) {
                log.debug("Failed to log out of temporary group operation session - will be cleaned up during session purge later: "
                    + ThrowableUtil.getAllMessages(e));
            }
        }
    }

    private ResourceOperationSchedule createScheduleForResource(GroupOperationSchedule schedule, String jobGroup,
        Subject user, Resource resource) throws Exception {
        ResourceOperationSchedule resourceSchedule;

        // We need to provide a unique job name for the group member.
        // The job name will be unique but it will have a job group name of the group job.
        // NOTE! This job-name/job-group combination will NOT have a Quartz scheduled job in the Quartz tables.
        // This is an invocation that JON spawns when Quartz triggers the group job.  Quartz does not trigger
        // these group member resource jobs.
        String resourceJobName = ResourceOperationJob.createUniqueJobName(resource, schedule.getOperationName());

        resourceSchedule = new ResourceOperationSchedule();
        resourceSchedule.setJobName(resourceJobName);
        resourceSchedule.setJobGroup(jobGroup);
        resourceSchedule.setDescription(schedule.getDescription());
        resourceSchedule.setOperationName(schedule.getOperationName());
        resourceSchedule.setParameters(schedule.getParameters());
        resourceSchedule.setSubject(user);
        resourceSchedule.setResource(resource);

        return resourceSchedule;
    }

    private void invokeOperationOnResource(ResourceOperationDetailsComposite composite,
        OperationManagerLocal operationManager) throws Exception {
        new ResourceOperationJob().invokeOperationOnResource(composite.schedule, composite.history, operationManager);
    }
}