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

import java.util.ArrayList;
import java.util.List;
import javax.faces.model.SelectItem;
import org.quartz.SchedulerException;
import org.quartz.SimpleTrigger;
import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.common.composite.IntegerOptionItem;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.gui.util.FacesContextUtility;
import org.rhq.enterprise.gui.common.scheduling.HtmlSimpleTrigger;
import org.rhq.enterprise.gui.operation.definition.group.ResourceGroupOperationDefinitionUtils;
import org.rhq.enterprise.gui.operation.model.OperationParameters;
import org.rhq.enterprise.gui.util.EnterpriseFacesContextUtility;
import org.rhq.enterprise.server.operation.GroupOperationSchedule;
import org.rhq.enterprise.server.operation.OperationManagerLocal;
import org.rhq.enterprise.server.operation.OperationSchedule;
import org.rhq.enterprise.server.scheduler.SchedulerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

public abstract class OperationScheduleDetailsUIBean {
    protected OperationManagerLocal manager;
    private OperationSchedule schedule;
    private OperationParameters parameters;
    private HtmlSimpleTrigger trigger;

    private List<SelectItem> resourceExecutionOptions;
    private String resourceExecutionOption;
    private List<IntegerOptionItem> resourceNameItems;

    public OperationScheduleDetailsUIBean() {
        manager = LookupUtil.getOperationManager();
    }

    private void init() {
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

            this.resourceExecutionOptions = ResourceGroupOperationDefinitionUtils.getResourceExecutionOptions();
            this.resourceExecutionOption = getResourceExecutionOption((GroupOperationSchedule) this.schedule);
            this.resourceNameItems = getResourceNameItems((GroupOperationSchedule) this.schedule);

            String jobName = schedule.getJobName();
            String jobGroup = schedule.getJobGroup();
            SimpleTrigger quartzTrigger = null;

            try {
                quartzTrigger = (SimpleTrigger) scheduler.getTrigger(jobName, jobGroup);
            } catch (SchedulerException se) {
                // capture all known info and throw a RuntimeException
                throw new IllegalStateException(se.getMessage(), se);
            }

            trigger = new HtmlSimpleTrigger(quartzTrigger);
        }
    }

    private List<IntegerOptionItem> getResourceNameItems(GroupOperationSchedule schedule) {
        List<Resource> resourceOrder = schedule.getExecutionOrder();
        if (resourceOrder == null) {
            return new ArrayList<IntegerOptionItem>();
        }

        List<IntegerOptionItem> results = new ArrayList<IntegerOptionItem>();
        for (Resource next : resourceOrder) {
            results.add(new IntegerOptionItem(next.getId(), next.getName()));
        }

        return results;
    }

    private String getResourceExecutionOption(GroupOperationSchedule schedule) {
        List<Resource> order = schedule.getExecutionOrder();

        boolean isOrdered = (order != null) && (order.size() > 0);

        return ResourceGroupOperationDefinitionUtils.getExecutionOption(isOrdered == false);
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

    public HtmlSimpleTrigger getTrigger() {
        init();

        return trigger;
    }

    public List<SelectItem> getResourceExecutionOptions() {
        init();

        return resourceExecutionOptions;
    }

    public String getResourceExecutionOption() {
        init();

        return resourceExecutionOption;
    }

    public List<IntegerOptionItem> getResourceNameItems() {
        init();

        return resourceNameItems;
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

    public void setTrigger(HtmlSimpleTrigger trigger) {
        this.trigger = trigger;
    }

    public void setResourceExecutionOptions(List<SelectItem> resourceExecutionOptions) {
        this.resourceExecutionOptions = resourceExecutionOptions;
    }

    public void setResourceExecutionOption(String resourceExecutionOption) {
        this.resourceExecutionOption = resourceExecutionOption;
    }

    public void setResourceNameItems(List<IntegerOptionItem> resourceNameItems) {
        this.resourceNameItems = resourceNameItems;
    }
}