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
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.operation.GroupOperationHistory;
import org.rhq.core.domain.operation.OperationDefinition;
import org.rhq.core.domain.operation.ResourceOperationHistory;
import org.rhq.core.domain.resource.Resource;
import org.rhq.enterprise.server.agentclient.AgentClient;
import org.rhq.enterprise.server.core.AgentManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * A job that invokes an operation on a single resource.
 *
 * @author John Mazzitelli
 */
public class ResourceOperationJob extends OperationJob {
    public static final String DATAMAP_INT_RESOURCE_ID = "resourceId";

    /**
     * Prefix for all job names and job groups names of resource operations.
     */
    private static final String RESOURCE_JOB_NAME_PREFIX = "jon-resource-";

    public static String createUniqueJobName(Resource resource, String operationName) {
        return RESOURCE_JOB_NAME_PREFIX + resource.getId() + "-" + operationName.hashCode() + "-"
            + System.currentTimeMillis();
    }

    public static String createJobGroupName(Resource resource) {
        return RESOURCE_JOB_NAME_PREFIX + resource.getId();
    }

    /**
     * This is invoked every time the operation needs to be invoked. Therefore, for each call to this method, a new
     * history item will be created.
     *
     * @see org.quartz.Job#execute(org.quartz.JobExecutionContext)
     */
    public void execute(JobExecutionContext context) throws JobExecutionException {
        ResourceOperationSchedule schedule = null;

        try {
            JobDetail jobDetail = context.getJobDetail();
            OperationManagerLocal operationManager = LookupUtil.getOperationManager();

            updateOperationScheduleEntity(jobDetail, context.getNextFireTime(), operationManager);

            // we only got here because the user was allowed to execute / schedule the
            // operation in the first place, thus it's safe to pass in the overlord here
            schedule = operationManager.getResourceOperationSchedule(LookupUtil.getSubjectManager().getOverlord(),
                jobDetail);

            // Login the schedule's subject so its assigned a session, so our security tests pass.
            // Create a new session even if user is logged in elsewhere, we don't want to attach to that user's session
            schedule.setSubject(getUserWithSession(schedule.getSubject(), false));

            schedule = operationManager.getResourceOperationSchedule(schedule.getSubject(), jobDetail);

            invokeOperationOnResource(jobDetail.getName(), jobDetail.getGroup(), schedule, null, operationManager);
        } catch (Exception e) {
            String error = "Failed to execute scheduled operation [" + schedule + "]";
            LogFactory.getLog(ResourceOperationJob.class).error(error, e);
            throw new JobExecutionException(error, e, false);
        }
    }

    /**
     * Actually invokes the operation by sending the command to the agent. This is package-scoped so the group job can
     * call this for each member resource in the group. If groupHistory is not <code>null</code>, it means this resource
     * job is being executed as part of a group execution.
     *
     * @param  jobName
     * @param  jobGroup
     * @param  schedule
     * @param  groupHistory
     * @param  operationManager
     *
     * @return the history item that you can use to track progress
     *
     * @throws Exception
     */
    ResourceOperationHistory invokeOperationOnResource(String jobName, String jobGroup,
        ResourceOperationSchedule schedule, GroupOperationHistory groupHistory, OperationManagerLocal operationManager)
        throws Exception {
        // make sure the session is still valid
        schedule.setSubject(getUserWithSession(schedule.getSubject(), true));

        // we need the operation definition to fill in the history item
        OperationDefinition op;
        op = operationManager.getSupportedResourceOperation(schedule.getSubject(), schedule.getResource().getId(),
            schedule.getOperationName());

        // first we need to create an INPROGRESS history item
        Configuration parameters = schedule.getParameters();
        if (parameters != null) {
            parameters = parameters.deepCopy(false); // we need a copy to avoid constraint violations upon delete
        }

        ResourceOperationHistory history;
        history = new ResourceOperationHistory(jobName, jobGroup, schedule.getSubject().getName(), op, parameters,
            schedule.getResource(), groupHistory);

        history = (ResourceOperationHistory) operationManager.updateOperationHistory(schedule.getSubject(), history);

        // now tell the agent to invoke it!
        try {
            Resource resource = schedule.getResource();
            AgentManagerLocal agentManager = LookupUtil.getAgentManager();
            AgentClient agentClient = agentManager.getAgentClient(resource.getId());

            agentClient.getOperationAgentService().invokeOperation(history.getJobId().toString(), resource.getId(),
                schedule.getOperationName(), schedule.getParameters());
        } catch (Exception e) {
            // failed to even send to the agent, immediately mark the job as failed
            history.setErrorMessageFromThrowable(e);
            operationManager.updateOperationHistory(getUserWithSession(schedule.getSubject(), true), history);
            throw e;
        }

        return history;
    }
}