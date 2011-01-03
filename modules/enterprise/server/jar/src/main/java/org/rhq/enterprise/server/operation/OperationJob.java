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

import org.apache.commons.logging.LogFactory;
import org.quartz.Job;
import org.quartz.JobDetail;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.operation.GroupOperationHistory;
import org.rhq.core.domain.operation.OperationDefinition;
import org.rhq.core.domain.operation.ResourceOperationHistory;
import org.rhq.core.domain.operation.ScheduleJobId;
import org.rhq.core.domain.operation.bean.ResourceOperationSchedule;
import org.rhq.enterprise.server.auth.SubjectManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

public abstract class OperationJob implements Job {
    public static final String DATAMAP_STRING_OPERATION_NAME = "operationName";
    public static final String DATAMAP_STRING_OPERATION_DISPLAY_NAME = "operationDisplayName";
    public static final String DATAMAP_INT_PARAMETERS_ID = "parametersId"; // the configuration ID
    public static final String DATAMAP_INT_SUBJECT_ID = "subjectId";
    // id of the associated OperationScheduleEntity - may be null for jobs created prior to upgrading to RHQ 4.0
    public static final String DATAMAP_INT_ENTITY_ID = "entityId";

    /**
     * For security purposes, we need to provide a subject with a valid login session. This creates such a subject by
     * logging in as the given user and returning the subject with a valid session. Call this even if the user already
     * has a session if you think the session may have timed out - this will refresh the session.
     *
     * @param  user     the user whose needs a valid session assigned to it
     * @param  reattach if the user already has a session, reattach to it rather than creating a new session
     *
     * @return the user with a valid session associated with it
     *
     * @throws Exception
     *
     * @see    SubjectManagerLocal#loginUnauthenticated(String, boolean)
     */
    protected Subject getUserWithSession(Subject user, boolean reattach) throws Exception {
        return LookupUtil.getSubjectManager().loginUnauthenticated(user.getName(), reattach);
    }

    protected void updateOperationScheduleEntity(JobDetail jobDetail, Date nextFireTime,
        OperationManagerLocal operationManager) {
        try {
            String jobName = jobDetail.getName();
            String jobGroup = jobDetail.getGroup();
            ScheduleJobId jobId = new ScheduleJobId(jobName, jobGroup);

            if (nextFireTime == null) {
                operationManager.deleteOperationScheduleEntity(jobId);
            } else {
                operationManager.updateOperationScheduleEntity(jobId, nextFireTime.getTime());
            }
        } catch (Exception e) {
            // do not abort the execution of the job, just log an error
            // this schedule entity is just a tracking entity and if it fails to update or delete
            // its not a fatal error.  But this is still bad - it means we'll have a row that either
            // exists but shouldn't or has an old next fire time (and queries relying on it will not
            // produce proper results).  But again, this should not effect the actual operation invocation.
            LogFactory.getLog(OperationJob.class).error("Failed to update schedule entity for job: " + jobDetail, e);
        }
    }

    protected ResourceOperationHistory createOperationHistory(String jobName, String jobGroup,
        ResourceOperationSchedule schedule, GroupOperationHistory groupHistory, OperationManagerLocal operationManager) {
        // we need the operation definition to fill in the history item
        OperationDefinition op;
        op = operationManager.getSupportedResourceOperation(schedule.getSubject(), schedule.getResource().getId(),
            schedule.getOperationName(), false);

        // first we need to create an INPROGRESS history item
        Configuration parameters = schedule.getParameters();
        if (parameters != null) {
            parameters = parameters.deepCopy(false); // we need a copy to avoid constraint violations upon delete
        }

        ResourceOperationHistory history;
        history = new ResourceOperationHistory(jobName, jobGroup, schedule.getSubject().getName(), op, parameters,
            schedule.getResource(), groupHistory);

        // resource-level ops can start immediately, group ops will be started as appropriate by the GroupOperationJob
        if (groupHistory == null) {
            history.setStartedTime();
        }

        // persist the results of the initial create
        history = (ResourceOperationHistory) operationManager.updateOperationHistory(schedule.getSubject(), history);

        return history;
    }
}