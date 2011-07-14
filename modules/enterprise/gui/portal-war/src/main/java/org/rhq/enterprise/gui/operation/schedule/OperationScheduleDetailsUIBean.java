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
package org.rhq.enterprise.gui.operation.schedule;

import org.quartz.SchedulerException;
import org.quartz.SimpleTrigger;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.operation.bean.OperationSchedule;
import org.rhq.core.gui.util.FacesContextUtility;
import org.rhq.enterprise.gui.common.scheduling.OperationDetailsScheduleComponent;
import org.rhq.enterprise.gui.operation.model.OperationParameters;
import org.rhq.enterprise.gui.util.EnterpriseFacesContextUtility;
import org.rhq.enterprise.server.operation.OperationManagerLocal;
import org.rhq.enterprise.server.scheduler.SchedulerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

public abstract class OperationScheduleDetailsUIBean {
    protected OperationManagerLocal manager;
    protected OperationSchedule schedule;
    private OperationParameters parameters;
    private OperationDetailsScheduleComponent operationDetails;

    public OperationScheduleDetailsUIBean() {
        manager = LookupUtil.getOperationManager();
    }

    protected void init() {
        if (this.schedule == null) {
            Subject subject = EnterpriseFacesContextUtility.getSubject();
            String jobId = FacesContextUtility.getRequiredRequestParameter("jobId");
            SchedulerLocal scheduler = LookupUtil.getSchedulerBean();

            try {
                this.schedule = getOperationSchedule(subject, jobId);
            } catch (Exception e) {
                // capture all known info and throw a RuntimeException
                throw new IllegalStateException(e.getMessage(), e);
            }

            this.parameters = new OperationParameters(this.schedule);

            String jobName = schedule.getJobName();
            String jobGroup = schedule.getJobGroup();
            SimpleTrigger quartzTrigger = null;

            try {
                quartzTrigger = (SimpleTrigger) scheduler.getTrigger(jobName, jobGroup);
            } catch (SchedulerException se) {
                // capture all known info and throw a RuntimeException
                throw new IllegalStateException(se.getMessage(), se);
            }

            operationDetails = new OperationDetailsScheduleComponent(quartzTrigger);
        }
    }

    public abstract OperationSchedule getOperationSchedule(Subject subject, String jobId) throws Exception;

    public OperationSchedule getSchedule() {
        init();

        return this.schedule;
    }

    public OperationParameters getParameters() {
        init();

        return parameters;
    }

    public OperationDetailsScheduleComponent getOperationDetails() {
        init();
        return operationDetails;
    }

    public OperationManagerLocal getManager() {
        return manager;
    }

    public void setManager(OperationManagerLocal manager) {
        this.manager = manager;
    }

    public void setSchedule(OperationSchedule schedule) {
        this.schedule = schedule;
    }

    public void setParameters(OperationParameters parameters) {
        this.parameters = parameters;
    }

    public void setOperationDetails(OperationDetailsScheduleComponent operationDetails) {
        this.operationDetails = operationDetails;
    }
}