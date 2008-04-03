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
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.domain.alert.Alert;
import org.rhq.core.domain.alert.AlertCondition;
import org.rhq.core.domain.alert.AlertConditionLog;
import org.rhq.core.domain.alert.composite.AlertWithLatestConditionLog;
import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.measurement.util.MeasurementConverter;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.core.gui.util.FacesContextUtility;
import org.rhq.core.gui.util.StringUtility;
import org.rhq.enterprise.gui.common.framework.PagedDataTableUIBean;
import org.rhq.enterprise.gui.common.paging.PageControlView;
import org.rhq.enterprise.gui.common.paging.PagedListDataModel;
import org.rhq.enterprise.gui.legacy.action.resource.common.monitor.alerts.AlertDefUtil;
import org.rhq.enterprise.gui.util.EnterpriseFacesContextUtility;
import org.rhq.enterprise.server.alert.AlertManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * @author Greg Hinkle
 */
public class ListAlertHistoryUIBean extends PagedDataTableUIBean {
    private static final Log log = LogFactory.getLog(ListAlertHistoryUIBean.class);

    public static final String MANAGED_BEAN_NAME = "ListAlertHistoryUIBean";

    private Resource resource;
    private String dateFilter;
    private String dateErrors;
    private AlertManagerLocal alertManager = LookupUtil.getAlertManager();

    public ListAlertHistoryUIBean() {
    }

    public void initDateFilter() {
        // As a workaround for JSF calling getRowData() prior to calling setChildTypeFilter(), bypass JSF and set the
        // field ourselves.
        if (getDateFilter() == null) {
            setDateFilter(FacesContextUtility.getOptionalRequestParameter("alertHistoryForm:dateFilter"));
        }
    }

    public String getDateFilter() {
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
            Resource requestResource = EnterpriseFacesContextUtility.getResourceIfExists();
            AlertManagerLocal manager = LookupUtil.getAlertManager();

            if (requestResource == null) {
                requestResource = resource; // request not associated with a resource - use the resource we used before
            } else {
                resource = requestResource; // request switched the resource this UI bean is using
            }

            initDateFilter();
            String dateStr = ListAlertHistoryUIBean.this.getDateFilter(); // get the outer class's JSF-managed property
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

            PageList<Alert> alerts = manager.findAlerts(requestResource.getId(), date, pc);

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
                        double measurementValue = Double.valueOf(log.getValue());
                        firedValue = MeasurementConverter.fit(measurementValue,
                            condition.getMeasurementDefinition().getUnits()).toString();
                    }

                    results.add(new AlertWithLatestConditionLog(alert, displayText, firedValue));
                } else {
                    results.add(new AlertWithLatestConditionLog(alert, "No Conditions", "--"));
                }
            }

            return new PageList<AlertWithLatestConditionLog>(results, alerts.getTotalSize(), pageControl);
        }
    }

    private String[] getSelectedAlerts() {
        return FacesContextUtility.getRequest().getParameterValues("selectedAlerts");
    }
}