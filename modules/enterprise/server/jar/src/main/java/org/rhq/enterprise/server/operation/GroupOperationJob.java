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
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.group.ResourceGroup;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.server.resource.ResourceManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * A job that invokes an operation on all resources that are members of a group.
 *
 * @author John Mazzitelli
 */
public class GroupOperationJob extends OperationJob {
    public static final String DATAMAP_INT_GROUP_ID = "groupId";
    public static final String DATAMAP_INT_ARRAY_EXECUTION_ORDER = "executionOrder"; // comma-separated list of IDs
    public static final String DATAMAP_BOOL_HALT_ON_FAILURE = "haltOnFailure";

    /**
     * Prefix for all job names and job groups names of group operations.
     */
    private static final String GROUP_JOB_NAME_PREFIX = "jon-group-";

    public static String createUniqueJobName(ResourceGroup group, String operationName) {
        return GROUP_JOB_NAME_PREFIX + group.getId() + "-" + operationName.hashCode() + "-"
            + System.currentTimeMillis();
    }

    public static String createJobGroupName(ResourceGroup group) {
        return GROUP_JOB_NAME_PREFIX + group.getId();
    }

    public void execute(JobExecutionContext context) throws JobExecutionException {
        GroupOperationSchedule schedule = null;
        GroupOperationHistory groupHistory = null;

        try {
            JobDetail jobDetail = context.getJobDetail();
            OperationManagerLocal operationManager = LookupUtil.getOperationManager();

            updateOperationScheduleEntity(jobDetail, context.getNextFireTime(), operationManager);

            // we only got here because the user was allowed to execute / schedule the operation in the first place,
            // thus it's safe to pass in the overlord here
            schedule = operationManager.getGroupOperationSchedule(LookupUtil.getSubjectManager().getOverlord(),
                jobDetail);

            // create a new session even if user is logged in elsewhere, we don't want to attach to that user's session
            Subject user = getUserWithSession(schedule.getSubject(), false);
            ResourceGroup group = schedule.getGroup();

            // we need the operation definition to fill in the history item
            OperationDefinition op;
            op = operationManager.getSupportedGroupOperation(user, group.getId(), schedule.getOperationName());

            // first we need to create an INPROGRESS *group* history item
            Configuration parameters = schedule.getParameters();
            if (parameters != null) {
                parameters = parameters.deepCopy(false); // we need a copy to avoid constraint violations upon delete
            }

            groupHistory = new GroupOperationHistory(jobDetail.getName(), jobDetail.getGroup(), user.getName(), op,
                parameters, group);

            groupHistory = (GroupOperationHistory) operationManager.updateOperationHistory(user, groupHistory);

            // now tell the agents to invoke the operation for all resources
            if (schedule.getExecutionOrder() != null) {
                // synchronously execute, waiting for each operation to finish before going to the next
                for (Resource resource : schedule.getExecutionOrder()) {
                    try {
                        OperationHistory resourceHistory = invokeOperationOnResource(schedule, jobDetail.getGroup(),
                            operationManager, getUserWithSession(user, true), resource, groupHistory);

                        while (resourceHistory.getStatus() == OperationRequestStatus.INPROGRESS) {
                            // if the duration was ridiculously long, let's break out of here. this will rarely
                            // be triggered because our operation manager will timeout long running operations for us
                            // (based on the operation's define timeout).  But, me being paranoid, I want to be able
                            // to break this infinite loop if for some reason the operation manager isn't doing its job.
                            // if the operation took longer than 24 hours, this breaks the loop.
                            if (resourceHistory.getDuration() > (1000 * 60 * 60 * 24)) {
                                break;
                            }

                            Thread.sleep(5000);
                            resourceHistory = operationManager.getOperationHistoryByHistoryId(getUserWithSession(user,
                                true), resourceHistory.getId());
                        }

                        // halt the rest if we got a failure and were told not to go on
                        if ((resourceHistory.getStatus() != OperationRequestStatus.SUCCESS)
                            && schedule.isHaltOnFailure()) {
                            break;
                        }
                    } catch (Exception e) {
                        // failed to even send to the agent, immediately mark the job as failed
                        groupHistory.setErrorMessageFromThrowable(e);
                        operationManager.updateOperationHistory(getUserWithSession(user, true), groupHistory);

                        if (schedule.isHaltOnFailure()) {
                            throw e;
                        }
                    }
                }
            } else {
                // send the invocation requests as fast as possible in no particular order
                ResourceManagerLocal resourceManager = LookupUtil.getResourceManager();
                PageControl pageControl = PageControl.getUnlimitedInstance();
                PageList<Resource> resources = resourceManager.getExplicitResourcesByResourceGroup(user, group,
                    pageControl);
                for (Resource resource : resources) {
                    try {
                        invokeOperationOnResource(schedule, jobDetail.getGroup(), operationManager, user, resource,
                            groupHistory);
                    } catch (Exception e) {
                        // failed to even send to the agent, immediately mark the job as failed
                        groupHistory.setErrorMessageFromThrowable(e);
                        operationManager.updateOperationHistory(getUserWithSession(user, true), groupHistory);

                        if (schedule.isHaltOnFailure()) {
                            throw e;
                        }
                    }
                }
            }
        } catch (Exception e) {
            String error = "Failed to execute scheduled operation [" + schedule + "]";
            LogFactory.getLog(GroupOperationJob.class).error(error, e);
            throw new JobExecutionException(error, e, false);
        }
    }

    private ResourceOperationHistory invokeOperationOnResource(GroupOperationSchedule schedule, String jobGroup,
        OperationManagerLocal operationManager, Subject user, Resource resource, GroupOperationHistory groupHistory)
        throws Exception {
        ResourceOperationSchedule resourceSchedule;
        ResourceOperationHistory resourceHistory;

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

        resourceHistory = new ResourceOperationJob().invokeOperationOnResource(resourceJobName, jobGroup,
            resourceSchedule, groupHistory, operationManager);

        return resourceHistory;
    }
}