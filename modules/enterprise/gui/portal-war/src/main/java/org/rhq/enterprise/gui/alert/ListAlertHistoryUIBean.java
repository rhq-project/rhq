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
package org.rhq.enterprise.gui.alert;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.faces.application.FacesMessage;
import javax.faces.model.DataModel;
import javax.faces.model.SelectItem;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.domain.alert.Alert;
import org.rhq.core.domain.alert.AlertCondition;
import org.rhq.core.domain.alert.AlertConditionLog;
import org.rhq.core.domain.alert.AlertPriority;
import org.rhq.core.domain.alert.composite.AlertWithLatestConditionLog;
import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.common.composite.IntegerOptionItem;
import org.rhq.core.domain.measurement.util.MeasurementConverter;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.core.gui.util.FacesContextUtility;
import org.rhq.core.gui.util.StringUtility;
import org.rhq.enterprise.gui.common.converter.SelectItemUtils;
import org.rhq.enterprise.gui.common.framework.PagedDataTableUIBean;
import org.rhq.enterprise.gui.common.paging.PageControlView;
import org.rhq.enterprise.gui.common.paging.PagedListDataModel;
import org.rhq.enterprise.gui.legacy.action.resource.common.monitor.alerts.AlertDefUtil;
import org.rhq.enterprise.gui.util.EnterpriseFacesContextUtility;
import org.rhq.enterprise.server.alert.AlertDefinitionManagerLocal;
import org.rhq.enterprise.server.alert.AlertManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * @author Greg Hinkle
 */
public class ListAlertHistoryUIBean extends PagedDataTableUIBean {
    private static final Log log = LogFactory.getLog(ListAlertHistoryUIBean.class);

    public static final String MANAGED_BEAN_NAME = "ListAlertHistoryUIBean";

    // filter stuff
    private String dateFilter;
    private String dateErrors;
    private String alertDefinitionFilter;
    private String alertPriorityFilter;
    private SelectItem[] alertDefinitionSelectItems;
    private SelectItem[] alertPrioritySelectItems;

    private AlertManagerLocal alertManager = LookupUtil.getAlertManager();
    private AlertDefinitionManagerLocal alertDefinitionManager = LookupUtil.getAlertDefinitionManager();

    public ListAlertHistoryUIBean() {
    }

    public String getDateFilter() {
        if (dateFilter == null) {
            dateFilter = FacesContextUtility.getOptionalRequestParameter("alertHistoryForm:dateFilter");
        }
        return this.dateFilter;
    }

    public void setDateFilter(String dateFilter) {
        this.dateFilter = dateFilter;
        this.dataModel = null;
    }

    public String getDateErrors() {
        return this.dateErrors;
    }

    public void setDateErrors(String dateErrors) {
        this.dateErrors = dateErrors;
        this.dataModel = null;
    }

    /*
     * definition filter stuff
     */
    public String getAlertDefinitionFilter() {
        if (alertDefinitionFilter == null) {
            alertDefinitionFilter = SelectItemUtils.getSelectItemFilter("alertHistoryForm:alertDefinitionFilter");
        }
        return SelectItemUtils.cleanse(alertDefinitionFilter);
    }

    public void setAlertDefinitionFilter(String alertDefinitionFilter) {
        this.alertDefinitionFilter = alertDefinitionFilter;
        this.dataModel = null;
    }

    public SelectItem[] getAlertDefinitionSelectItems() {
        if (alertDefinitionSelectItems == null) {
            List<IntegerOptionItem> optionItems = alertDefinitionManager.getAlertDefinitionOptionItems(getSubject(),
                getResource().getId());
            alertDefinitionSelectItems = SelectItemUtils.convertFromListOptionItem(optionItems, true);
        }

        return alertDefinitionSelectItems;
    }

    public void setAlertDefinitionSelectItems(SelectItem[] alertDefinitionSelectItems) {
        this.alertDefinitionSelectItems = alertDefinitionSelectItems;
        this.dataModel = null;
    }

    /*
     * priority filter stuff
     */
    public String getAlertPriorityFilter() {
        if (alertPriorityFilter == null) {
            alertPriorityFilter = SelectItemUtils.getSelectItemFilter("alertHistoryForm:alertPriorityFilter");
        }
        return SelectItemUtils.cleanse(alertPriorityFilter);
    }

    public void setAlertPriorityFilter(String alertPriorityFilter) {
        this.alertPriorityFilter = alertPriorityFilter;
        this.dataModel = null;
    }

    public SelectItem[] getAlertPrioritySelectItems() {
        if (alertPrioritySelectItems == null) {
            alertPrioritySelectItems = SelectItemUtils.convertFromEnum(AlertPriority.class, true);
        }

        return alertPrioritySelectItems;
    }

    public void setAlertPrioritySelectItems(SelectItem[] alertPrioritySelectItems) {
        this.alertPrioritySelectItems = alertPrioritySelectItems;
        this.dataModel = null;
    }

    public String deleteSelectedAlerts() {
        Subject subject = EnterpriseFacesContextUtility.getSubject();
        Resource resource = EnterpriseFacesContextUtility.getResource();

        String[] selectedAlerts = getSelectedAlerts();
        Integer[] alertDefinitionIds = StringUtility.getIntegerArray(selectedAlerts);

        try {
            alertManager.deleteAlerts(subject, resource.getId(), alertDefinitionIds);
            FacesContextUtility.addMessage(FacesMessage.SEVERITY_INFO, "Deleted " + alertDefinitionIds.length
                + " alerts.");
        } catch (Exception e) {
            FacesContextUtility.addMessage(FacesMessage.SEVERITY_ERROR, "Failed to delete selected alerts.", e);
        }

        this.dataModel = null;

        return "success";
    }

    public String purgeAllAlerts() {
        Subject subject = EnterpriseFacesContextUtility.getSubject();
        Resource resource = EnterpriseFacesContextUtility.getResource();

        try {
            int numDeleted = alertManager.deleteAlerts(subject, resource.getId());
            FacesContextUtility.addMessage(FacesMessage.SEVERITY_INFO, "Deleted " + numDeleted
                + " alerts on this resource");
        } catch (Exception e) {
            FacesContextUtility.addMessage(FacesMessage.SEVERITY_ERROR, "Failed to delete alerts for resource[ "
                + resource.getId() + " ]", e);
            log.error("failed to delete alerts for resource[ " + resource.getId() + " ]", e);
        }
        this.dataModel = null;

        return "success";
    }

    @Override
    public DataModel getDataModel() {
        if (dataModel == null) {
            dataModel = new ListAlertDefinitionsDataModel(PageControlView.AlertHistoryList, MANAGED_BEAN_NAME);
        }

        return dataModel;
    }

    private class ListAlertDefinitionsDataModel extends PagedListDataModel<AlertWithLatestConditionLog> {
        public ListAlertDefinitionsDataModel(PageControlView view, String beanName) {
            super(view, beanName);
        }

        @Override
        public PageList<AlertWithLatestConditionLog> fetchPage(PageControl pc) {
            String dateStr = getDateFilter(); // get the outer class's JSF-managed property
            Date date = null;

            if ((dateStr != null) && !dateStr.equals("")) {
                try {
                    DateFormat df = new SimpleDateFormat("MM/dd/yyyy");
                    date = df.parse(dateStr);
                } catch (ParseException pe) {
                    ListAlertHistoryUIBean.this.setDateErrors("Error: Invalid date filter, format is MM/dd/yyyy");
                    // do nothing else, things will pass through will a null date and function properly
                }
            }

            Integer alertDefinitionId = getAlertDefinitionId();
            AlertPriority alertPriority = getAlertPriority();

            long MILLIS_IN_DAY = 24 * 60 * 60 * 1000;
            Long beginTime = null;
            Long endTime = null;
            if (date != null) {
                beginTime = date.getTime();
                endTime = new Date(beginTime + MILLIS_IN_DAY).getTime();
            }
            PageList<Alert> alerts = alertManager.findAlerts(getResource().getId(), alertDefinitionId, alertPriority,
                beginTime, endTime, pc);

            List<AlertWithLatestConditionLog> results = new ArrayList<AlertWithLatestConditionLog>(alerts.size());

            HttpServletRequest request = FacesContextUtility.getRequest();
            for (Alert alert : alerts) {
                if (alert.getConditionLogs().size() > 1) {
                    results.add(new AlertWithLatestConditionLog(alert, "Multiple Conditions", "--"));
                } else if (alert.getConditionLogs().size() == 1) {
                    AlertConditionLog log = alert.getConditionLogs().iterator().next();
                    AlertCondition condition = log.getCondition();
                    String displayText = AlertDefUtil.formatAlertConditionForDisplay(condition, request);

                    String firedValue = log.getValue();
                    if (condition.getMeasurementDefinition() != null) {
                        firedValue = MeasurementConverter.format(Double.valueOf(log.getValue()), condition
                            .getMeasurementDefinition().getUnits(), true);
                    }

                    results.add(new AlertWithLatestConditionLog(alert, displayText, firedValue));
                } else {
                    results.add(new AlertWithLatestConditionLog(alert, "No Conditions", "--"));
                }
            }

            return new PageList<AlertWithLatestConditionLog>(results, alerts.getTotalSize(), pc);
        }
    }

    private Integer getAlertDefinitionId() {
        String alertDefinitionString = getAlertDefinitionFilter();
        if (alertDefinitionString != null) {
            return Integer.parseInt(alertDefinitionString);
        }
        return null;
    }

    private AlertPriority getAlertPriority() {
        String alertPriorityName = getAlertPriorityFilter();
        if (alertPriorityName != null) {
            return Enum.valueOf(AlertPriority.class, alertPriorityName);
        }
        return null;
    }

    private String[] getSelectedAlerts() {
        return FacesContextUtility.getRequest().getParameterValues("selectedAlerts");
    }

    public String getRecoveryInfo() {
        return "N/A";
    }
}