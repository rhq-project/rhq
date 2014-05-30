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

import org.apache.commons.logging.LogFactory;
import org.quartz.Job;
import org.quartz.JobDetail;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.criteria.ResourceOperationHistoryCriteria;
import org.rhq.core.domain.operation.GroupOperationHistory;
import org.rhq.core.domain.operation.JobId;
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
     * logging in as the given user and returning the subject with a valid session. A new session is created
     * each time this is called unless <code>reattach</code> is <code>true</code>, in which case if a session
     * already exists, it will be refreshed - only if a session doesn't exist will a new one be created.
     *
     * @param  user the user whose needs a valid session assigned to it - the session ID will be assigned to this object
     * @param  reattach if the user already has a session, reattach to it rather than creating a new session
     * @return the user with a valid session associated with it
     *
     * @throws Exception
     * @see    SubjectManagerLocal#loginUnauthenticated(String)
     */
    protected Subject getUserWithSession(Subject user, boolean reattach) throws Exception {
        SubjectManagerLocal subjectManager = LookupUtil.getSubjectManager();
        Subject subject = null;

        if (reattach && user.getSessionId() != null) {
            try {
                subject = subjectManager.getSubjectBySessionId(user.getSessionId());
            } catch (Exception e) {
                // session either doesn't exist or has timed out - fall thru to create a new session
            }
        }

        if (subject == null) {
            subject = subjectManager.loginUnauthenticated(user.getName());
            user.setSessionId(subject.getSessionId()); // we update the passed in object so the caller can use it as well

        }

        return subject;
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

        // persist the results of the initial create
        ResourceOperationHistory persisted;
        persisted = (ResourceOperationHistory) operationManager.updateOperationHistory(schedule.getSubject(), history);
        history.setId(persisted.getId()); // we need this - this enables the server to successfully update the group history later

        return persisted;
    }

    /**
     * @return This returns all optional data and should be suitable for modification and subsequent update.
     */
    protected ResourceOperationHistory findOperationHistory(String name, String group,
        OperationManagerLocal operationManager, ResourceOperationSchedule schedule) {

        JobId jobId = new JobId(name, group);

        ResourceOperationHistoryCriteria criteria = new ResourceOperationHistoryCriteria();
        criteria.addFilterJobId(jobId);
        criteria.fetchOperationDefinition(true);
        criteria.fetchParameters(true);
        criteria.fetchResults(true);

        ResourceOperationHistory history;
        List<ResourceOperationHistory> list = operationManager.findResourceOperationHistoriesByCriteria(
            schedule.getSubject(), criteria);
        if (list == null || list.isEmpty()) {
            return null;
        }

        history = list.get(0);
        return history;
    }
}