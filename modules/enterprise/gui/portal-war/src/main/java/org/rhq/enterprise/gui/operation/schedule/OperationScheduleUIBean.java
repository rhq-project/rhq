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
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.faces.application.FacesMessage;
import javax.faces.model.DataModel;

import org.quartz.SimpleTrigger;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.operation.OperationDefinition;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.core.gui.util.FacesContextUtility;
import org.rhq.core.gui.util.StringUtility;
import org.rhq.core.util.exception.ThrowableUtil;
import org.rhq.enterprise.gui.common.framework.PagedDataTableUIBean;
import org.rhq.enterprise.gui.common.paging.PageControlView;
import org.rhq.enterprise.gui.common.paging.PagedListDataModel;
import org.rhq.enterprise.gui.common.scheduling.OperationDetailsScheduleComponent;
import org.rhq.enterprise.gui.operation.definition.OperationDefinitionParametersUIBean;
import org.rhq.enterprise.gui.operation.definition.OperationDefinitionUIBean;
import org.rhq.enterprise.gui.operation.definition.group.ResourceGroupOperationDefinitionUIBean;
import org.rhq.enterprise.gui.operation.definition.resource.ResourceOperationDefinitionUIBean;
import org.rhq.enterprise.gui.operation.schedule.group.ResourceGroupOperationScheduleUIBean;
import org.rhq.enterprise.gui.operation.schedule.resource.ResourceOperationScheduleUIBean;
import org.rhq.enterprise.gui.util.EnterpriseFacesContextUtility;
import org.rhq.enterprise.server.operation.OperationManagerLocal;
import org.rhq.enterprise.server.operation.OperationSchedule;
import org.rhq.enterprise.server.operation.ResourceOperationSchedule;
import org.rhq.enterprise.server.util.LookupUtil;

public abstract class OperationScheduleUIBean extends PagedDataTableUIBean {
    private OperationSchedule selectedOperationSchedule;
    protected OperationManagerLocal manager;
    private OperationDetailsScheduleComponent operationDetails;

    public OperationScheduleUIBean() {
        manager = LookupUtil.getOperationManager();
    }

    public OperationSchedule getSelectedOperationSchedule() {
        return selectedOperationSchedule;
    }

    public void setSelectedOperationSchedule(OperationSchedule selectedOperationSchedule) {
        this.selectedOperationSchedule = selectedOperationSchedule;
    }

    public String selectScheduleToView() {
        ResourceOperationSchedule operationSchedule = (ResourceOperationSchedule) FacesContextUtility.getRequest()
            .getAttribute("item");
        setSelectedOperationSchedule(operationSchedule);
        return "success";
    }

    public OperationDetailsScheduleComponent getOperationDetails() {
        if (this.operationDetails == null)
            this.operationDetails = new OperationDetailsScheduleComponent();
        return this.operationDetails;
    }

    public void setOperationDetails(OperationDetailsScheduleComponent operationDetails) {
        this.operationDetails = operationDetails;
    }

    public String schedule() {
        try {
            Subject subject = EnterpriseFacesContextUtility.getSubject();

            SimpleTrigger simpleTrigger = getTrigger();

            long startTime = simpleTrigger.getStartTime().getTime();
            long now = System.currentTimeMillis();
            if (now - startTime > 60000) {
                /*
                 * allow a resource to be scheduled up to 60 seconds in the past, in case the clock
                 * ticks forward while the user is still filling out the operation schedule form
                 */
                throw new IllegalArgumentException("Can not schedule operations in the past");
            }

            OperationDefinitionParametersUIBean operationParametersUIBean = FacesContextUtility
                .getBean(OperationDefinitionParametersUIBean.class);
            Configuration configuration = operationParametersUIBean.getConfiguration();

            OperationDefinitionUIBean operationDefUIBean;
            if (this instanceof ResourceOperationScheduleUIBean) {
                operationDefUIBean = FacesContextUtility.getBean(ResourceOperationDefinitionUIBean.class);
            } else if (this instanceof ResourceGroupOperationScheduleUIBean) {
                operationDefUIBean = FacesContextUtility.getBean(ResourceGroupOperationDefinitionUIBean.class);
            } else {
                throw new IllegalStateException("Unsupported class - this is a bug, please report it: "
                    + this.getClass().toString());
            }

            // if the user selected a timeout, add it to our configuration
            String timeout = operationDefUIBean.getTimeout();
            if (!timeout.trim().equals("")) {
                Integer.parseInt(timeout); // see if it fails, to return back to the UI

                if (configuration == null) {
                    configuration = new Configuration();
                }

                configuration.put(new PropertySimple(OperationDefinition.TIMEOUT_PARAM_NAME, timeout));
            }

            String operationName = operationDefUIBean.getName();
            String description = operationDefUIBean.getDescription();

            scheduleOperation(subject, operationName, configuration, simpleTrigger, description);
        } catch (Exception e) {
            FacesContextUtility.addMessage(FacesMessage.SEVERITY_ERROR,
                "There was an error scheduling your operation: " + e.getMessage());
            return "success";
        }

        if (getOperationDetails().getDeferred()) {
            // a deferred trigger is a new schedule
            return "viewOperationSchedules";
        } else {
            try {
                // sleep just enough to let "fast" operations complete before being redirected
                Thread.sleep(1500);
            } catch (InterruptedException ie) {
                // let this thread be interrupted without user warning
            }

            // otherwise, it is immediately executed
            return "viewOperationHistory";
        }
    }

    public String unschedule() {
        Subject subject = EnterpriseFacesContextUtility.getSubject();
        String[] selectedItems = FacesContextUtility.getRequest().getParameterValues("selectedItems");

        List<String> success = new ArrayList<String>();
        Map<String, String> failure = new HashMap<String, String>();

        for (String doomedJobId : selectedItems) {
            try {
                unscheduleOperation(subject, doomedJobId);

                success.add(doomedJobId);
            } catch (Exception e) {
                failure.put(doomedJobId, ThrowableUtil.getAllMessages(e, true));
            }
        }

        if (success.size() > 0) {
            // one success message for all successful deletions
            FacesContextUtility.addMessage(FacesMessage.SEVERITY_INFO, "Removed operation schedules: "
                + StringUtility.getListAsDelimitedString(success));
        }

        for (Map.Entry<String, String> error : failure.entrySet()) {
            // one message per failed deletion (hopefully rare)
            FacesContextUtility.addMessage(FacesMessage.SEVERITY_ERROR, "Failed to remove operation schedules: "
                + error.getKey() + ". Cause: " + error.getValue());
        }

        return "success";
    }

    public abstract void scheduleOperation(Subject subject, String operationName, Configuration parameters,
        SimpleTrigger simpleTrigger, String description) throws Exception;

    public abstract void unscheduleOperation(Subject subject, String doomedJobId) throws Exception;

    @Override
    public DataModel getDataModel() {
        if (dataModel == null) {
            dataModel = new ListOperationScheduleDataModel(PageControlView.NONE, getManagedBeanName());
        }

        return dataModel;
    }

    private class ListOperationScheduleDataModel extends PagedListDataModel<OperationSchedule> {
        public ListOperationScheduleDataModel(PageControlView view, String beanName) {
            super(view, beanName);
        }

        @Override
        public PageList<OperationSchedule> fetchPage(PageControl pc) {
            List<? extends OperationSchedule> results = getOperationScheduleList();

            PageList<OperationSchedule> pagedResults = new PageList<OperationSchedule>(results, results.size(),
                PageControl.getUnlimitedInstance());
            return pagedResults;
        }
    }

    public abstract String getManagedBeanName();

    public abstract List<? extends OperationSchedule> getOperationScheduleList();

    public SimpleTrigger getTrigger() {
        OperationDetailsScheduleComponent component = this.getOperationDetails();
        SimpleTrigger trigger = new SimpleTrigger();
        //Validate if a start date is specified
        Date now = Calendar.getInstance().getTime();
        if ((component.getStart().equals("immediate"))) {
            trigger.setStartTime(now);
        } else {
            component.setDeferred(true);
            if (component.getStartDate() == null) {
                throw new IllegalArgumentException("Please select a start date");
            }
            if (now.after(component.getStartDate())) {
                throw new IllegalArgumentException("Scheduling cannot occur in the past");
                //                FacesMessages.instance().addToControl("start", "Scheduling cannot occur in the past");
            } else {
                trigger.setStartTime(component.getStartDate());
                if (component.getRecur().equals("never")) {
                } else {
                    //set the repetition interval of the trigger
                    trigger.setRepeatCount(SimpleTrigger.REPEAT_INDEFINITELY);
                    component.setRepeat(true);
                    long repeatInterval = component.getUnit().getMillis() * component.getRepeatInterval();
                    trigger.setRepeatInterval(repeatInterval);
                    if (component.getEnd().equals("endDate")) {
                        if (component.getEndDate() == null) {
                            throw new IllegalArgumentException("Please select an end date");
                        }
                        component.setTerminate(true);
                        if (component.getEndDate().before(component.getStartDate())) {
                            throw new IllegalArgumentException("Scheduling cannot occur before the start date");
                        } else {
                            //set the end date of the trigger
                            trigger.setEndTime(component.getEndDate());
                        }
                    }
                }
            }
        }
        return trigger;
    }
}