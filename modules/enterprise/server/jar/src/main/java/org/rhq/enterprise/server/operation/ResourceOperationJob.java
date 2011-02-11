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

import javax.ejb.EJBException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.operation.OperationHistory;
import org.rhq.core.domain.operation.ResourceOperationHistory;
import org.rhq.core.domain.operation.bean.ResourceOperationSchedule;
import org.rhq.core.domain.resource.InventoryStatus;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.util.exception.ThrowableUtil;
import org.rhq.enterprise.server.agentclient.AgentClient;
import org.rhq.enterprise.server.auth.SubjectManagerLocal;
import org.rhq.enterprise.server.core.AgentManagerLocal;
import org.rhq.enterprise.server.resource.ResourceManagerLocal;
import org.rhq.enterprise.server.resource.ResourceNotFoundException;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * A job that invokes an operation on a single resource.
 *
 * @author John Mazzitelli
 */
public class ResourceOperationJob extends OperationJob {
    public static final String DATAMAP_INT_RESOURCE_ID = "resourceId";

    private static final Log log = LogFactory.getLog(ResourceOperationJob.class);

    /**
     * Prefix for all job names and job groups names of resource operations.
     */
    private static final String RESOURCE_JOB_NAME_PREFIX = "rhq-resource-";

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

            // retrieve the stored schedule using the overlord so it succeeds no matter what
            schedule = operationManager.getResourceOperationSchedule(getOverlord(), jobDetail);

            // Login the schedule's subject so its assigned a session, so our security tests pass.
            // Create a new session even if user is logged in elsewhere, we don't want to attach to that user's session
            schedule.setSubject(getUserWithSession(schedule.getSubject(), false));

            // for the security check, can the user who scheduled the operation in the first 
            // place still have the authority to execute it against the resource in question
            operationManager.getResourceOperationSchedule(schedule.getSubject(), jobDetail);

            ResourceOperationHistory resourceHistory = createOperationHistory(jobDetail.getName(),
                jobDetail.getGroup(), schedule, null, operationManager);

            invokeOperationOnResource(schedule, resourceHistory, operationManager);
        } catch (Exception e) {
            if (e instanceof CancelJobException) {
                // if a cancel job exception was thrown we do not need to do anything else.
                // we can just rethrow the exception.
                throw (CancelJobException) e;
            }

            String error = "Failed to execute scheduled operation [" + schedule + "]";
            log.error(error, e);

            if (isResourceUncommitted(context.getJobDetail())) {
                int resourceId = getResourceId(context.getJobDetail());
                String msg = "The resource with id " + resourceId + " is not committed in inventory. It may have "
                    + "been deleted from inventory. Canceling job.";
                log.warn(msg);
                throw new CancelJobException(msg, e);
            }

            throw new JobExecutionException(error, e, false);
        }
    }

    private Subject getOverlord() {
        SubjectManagerLocal subjectMgr = LookupUtil.getSubjectManager();
        return subjectMgr.getOverlord();
    }

    private int getResourceId(JobDetail jobDetail) {
        JobDataMap jobDataMap = jobDetail.getJobDataMap();
        return jobDataMap.getIntFromString(DATAMAP_INT_RESOURCE_ID);
    }

    private boolean isResourceUncommitted(JobDetail jobDetail) {
        ResourceManagerLocal resourceMgr = LookupUtil.getResourceManager();
        int resourceId = getResourceId(jobDetail);

        try {
            Resource resource = resourceMgr.getResource(getOverlord(), resourceId);
            return isResourceUncommitted(resource);
        } catch (EJBException e) {
            if (e.getCausedByException() instanceof ResourceNotFoundException) {
                return true;
            }
            throw e;
        }
    }

    private boolean isResourceUncommitted(Resource resource) {
        return resource == null || resource.getInventoryStatus() != InventoryStatus.COMMITTED;
    }

    /**
     * Actually invokes the operation by sending the command to the agent. This is package-scoped so the group job can
     * call this for each member resource in the group. If groupHistory is not <code>null</code>, it means this resource
     * job is being executed as part of a group execution.
     *
     * @param  schedule
     * @param  resourceHistory
     * @param  operationManager
     *
     * @return the history item that you can use to track progress
     *
     * @throws Exception
     */
    void invokeOperationOnResource(ResourceOperationSchedule schedule, ResourceOperationHistory resourceHistory,
        OperationManagerLocal operationManager) throws Exception {
        // make sure the session is still valid
        schedule.setSubject(getUserWithSession(schedule.getSubject(), true));

        resourceHistory.setStartedTime();
        resourceHistory = (ResourceOperationHistory) operationManager.updateOperationHistory(
                getUserWithSession(schedule.getSubject(), true), resourceHistory);

        // now tell the agent to invoke it!
        try {
            Resource resource = schedule.getResource();

            if (isResourceUncommitted(resource)) {
                String msg = "The resource with id " + resource.getId() + " is not committed in inventory. It may "
                    + "have been deleted from inventory. Canceling job.";
                log.warn(msg);
                throw new CancelJobException(msg);
            }

            AgentManagerLocal agentManager = LookupUtil.getAgentManager();
            AgentClient agentClient = agentManager.getAgentClient(getOverlord(), resource.getId());

            agentClient.getOperationAgentService().invokeOperation(resourceHistory.getJobId().toString(),
                resource.getId(), schedule.getOperationName(), schedule.getParameters());
        } catch (Exception e) {
            // failed to even send to the agent, immediately mark the job as failed
            resourceHistory.setErrorMessage(ThrowableUtil.getStackAsString(e));
            operationManager.updateOperationHistory(getUserWithSession(schedule.getSubject(), true), resourceHistory);
            operationManager.checkForCompletedGroupOperation(resourceHistory.getId());
            throw e;
        }

        return;
    }
}