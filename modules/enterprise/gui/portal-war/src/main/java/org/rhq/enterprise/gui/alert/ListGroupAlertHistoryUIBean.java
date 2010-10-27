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
import org.rhq.core.domain.common.EntityContext;
import org.rhq.core.domain.common.composite.IntegerOptionItem;
import org.rhq.core.domain.criteria.AlertCriteria;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.group.ResourceGroup;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.core.gui.util.FacesContextUtility;
import org.rhq.core.gui.util.StringUtility;
import org.rhq.core.server.MeasurementConverter;
import org.rhq.enterprise.gui.common.converter.SelectItemUtils;
import org.rhq.enterprise.gui.common.framework.PagedDataTableUIBean;
import org.rhq.enterprise.gui.common.paging.PageControlView;
import org.rhq.enterprise.gui.common.paging.PagedListDataModel;
import org.rhq.enterprise.gui.legacy.action.resource.common.monitor.alerts.AlertDefUtil;
import org.rhq.enterprise.gui.util.EnterpriseFacesContextUtility;
import org.rhq.enterprise.server.alert.AlertDefinitionManagerLocal;
import org.rhq.enterprise.server.alert.AlertManagerLocal;
import org.rhq.enterprise.server.resource.ResourceManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * @author Joseph Marques
 */
public class ListGroupAlertHistoryUIBean extends PagedDataTableUIBean {
    private static final Log log = LogFactory.getLog(ListGroupAlertHistoryUIBean.class);

    public static final String MANAGED_BEAN_NAME = "ListGroupAlertHistoryUIBean";

    // filter stuff
    private String dateFilter;
    private String dateErrors;
    private String alertDefinitionFilter;
    private String alertPriorityFilter;
    private SelectItem[] alertDefinitionSelectItems;
    private SelectItem[] alertPrioritySelectItems;

    private AlertManagerLocal alertManager = LookupUtil.getAlertManager();
    private ResourceManagerLocal resourceManager = LookupUtil.getResourceManager();

    private AlertDefinitionManagerLocal alertDefinitionManager = LookupUtil.getAlertDefinitionManager();

    public ListGroupAlertHistoryUIBean() {
    }

    public String getDateFilter() {
        if (dateFilter == null) {
            dateFilter = FacesContextUtility.getOptionalRequestParameter("alertHistoryForm:dateFilter");
        }
        return this.dateFilter;
    }

    public void setDateFilter(String dateFilter) {
        this.dateFilter = dateFilter;
    }

    public String getDateErrors() {
        return this.dateErrors;
    }

    public void setDateErrors(String dateErrors) {
        this.dateErrors = dateErrors;
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
    }

    public SelectItem[] getAlertDefinitionSelectItems() {
        if (alertDefinitionSelectItems == null) {
            List<IntegerOptionItem> optionItems = alertDefinitionManager.findAlertDefinitionOptionItemsForGroup(
                getSubject(), getResourceGroup().getId());
            alertDefinitionSelectItems = SelectItemUtils.convertFromListOptionItem(optionItems, true);
        }

        return alertDefinitionSelectItems;
    }

    public void setAlertDefinitionSelectItems(SelectItem[] alertDefinitionSelectItems) {
        this.alertDefinitionSelectItems = alertDefinitionSelectItems;
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
    }

    public SelectItem[] getAlertPrioritySelectItems() {
        if (alertPrioritySelectItems == null) {
            alertPrioritySelectItems = SelectItemUtils.convertFromEnum(AlertPriority.class, true);
        }

        return alertPrioritySelectItems;
    }

    public void setAlertPrioritySelectItems(SelectItem[] alertPrioritySelectItems) {
        this.alertPrioritySelectItems = alertPrioritySelectItems;
    }

    public String deleteSelectedAlerts() {
        Subject subject = EnterpriseFacesContextUtility.getSubject();

        String[] selectedAlerts = getSelectedAlerts();
        int[] alertIds = StringUtility.getIntArray(selectedAlerts);

        try {
            alertManager.deleteAlerts(subject, alertIds);
            FacesContextUtility.addMessage(FacesMessage.SEVERITY_INFO, "Deleted " + alertIds.length + " alerts.");
        } catch (Exception e) {
            FacesContextUtility.addMessage(FacesMessage.SEVERITY_ERROR, "Failed to delete selected alerts.", e);
        }

        return "success";
    }

    public String purgeAllAlerts() {
        Subject subject = EnterpriseFacesContextUtility.getSubject();
        ResourceGroup resourceGroup = EnterpriseFacesContextUtility.getResourceGroup();

        try {
            int numDeleted = alertManager.deleteAlertsByContext(subject, EntityContext.forGroup(resourceGroup.getId()));
            FacesContextUtility.addMessage(FacesMessage.SEVERITY_INFO, "Deleted " + numDeleted
                + " alerts on this group");
        } catch (Exception e) {
            FacesContextUtility.addMessage(FacesMessage.SEVERITY_ERROR, "Failed to delete alerts for group[ "
                + resourceGroup.getId() + " ]", e);
            log.error("failed to delete alerts for group[ " + resourceGroup.getId() + " ]", e);
        }

        return "success";
    }

    @Override
    public DataModel getDataModel() {
        if (dataModel == null) {
            dataModel = new ListGroupAlertDefinitionsDataModel(PageControlView.GroupAlertHistoryList, MANAGED_BEAN_NAME);
        }

        return dataModel;
    }

    private class ListGroupAlertDefinitionsDataModel extends PagedListDataModel<AlertWithLatestConditionLog> {
        public ListGroupAlertDefinitionsDataModel(PageControlView view, String beanName) {
            super(view, beanName);
        }

        @Override
        public PageList<AlertWithLatestConditionLog> fetchPage(PageControl pc) {
            String dateStr = getDateFilter(); // get the outer class's JSF-managed property
            Date date = null;

            ListGroupAlertHistoryUIBean.this.setDateErrors(null);
            if ((dateStr != null) && !dateStr.equals("")) {
                try {
                    DateFormat df = new SimpleDateFormat("MM/dd/yy");
                    date = df.parse(dateStr);
                } catch (ParseException pe) {
                    ListGroupAlertHistoryUIBean.this.setDateErrors("Error: Invalid date filter, format is MM/dd/yy");
                    // do nothing else, things will pass through will a null date and function properly
                }
            }

            Integer alertDefinitionId = getAlertDefinitionId();
            AlertPriority alertPriority = getAlertPriority();
            Integer resourceGroupId = getResourceGroup().getId();

            long MILLIS_IN_DAY = 24 * 60 * 60 * 1000;
            Long beginTime = null;
            Long endTime = null;
            if (date != null) {
                beginTime = date.getTime();
                endTime = new Date(beginTime + MILLIS_IN_DAY).getTime();
            }

            AlertCriteria searchCriteria = new AlertCriteria();
            if (alertDefinitionId != null) {
                searchCriteria.addFilterGroupAlertDefinitionIds(alertDefinitionId);
            }

            // show alerts for any resource in the group, not just those attached to the group alert definitions
            List<Integer> resourceIds = resourceManager.findImplicitResourceIdsByResourceGroup(resourceGroupId);
            searchCriteria.addFilterResourceIds(resourceIds.toArray(new Integer[resourceIds.size()]));
            searchCriteria.addFilterPriorities(alertPriority);
            searchCriteria.addFilterStartTime(beginTime);
            searchCriteria.addFilterEndTime(endTime);
            searchCriteria.setPageControl(pc);
            searchCriteria.fetchAlertDefinition(true);
            // this is done by default at the object layer
            // searchCriteria.fetchConditionLogs(true); 

            PageList<Alert> alerts = alertManager.findAlertsByCriteria(getSubject(), searchCriteria);

            List<AlertWithLatestConditionLog> results = new ArrayList<AlertWithLatestConditionLog>(alerts.size());

            HttpServletRequest request = FacesContextUtility.getRequest();
            for (Alert alert : alerts) {
                Resource res = alert.getAlertDefinition().getResource();
                String recoveryInfo = AlertDefUtil.getAlertRecoveryInfo(alert, res.getId());

                if (alert.getConditionLogs().size() > 1) {
                    results.add(new AlertWithLatestConditionLog(alert, "Multiple Conditions", "--", recoveryInfo));
                } else if (alert.getConditionLogs().size() == 1) {
                    AlertConditionLog log = alert.getConditionLogs().iterator().next();
                    AlertCondition condition = log.getCondition();
                    String displayText = AlertDefUtil.formatAlertConditionForDisplay(condition, request);

                    String firedValue = log.getValue();
                    if (condition.getMeasurementDefinition() != null) {
                        firedValue = MeasurementConverter.format(Double.valueOf(log.getValue()), condition
                            .getMeasurementDefinition().getUnits(), true);
                    }

                    results.add(new AlertWithLatestConditionLog(alert, displayText, firedValue, recoveryInfo));
                } else {
                    results.add(new AlertWithLatestConditionLog(alert, "No Conditions", "--", recoveryInfo));
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
}