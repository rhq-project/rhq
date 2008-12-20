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
package org.rhq.enterprise.gui.subsystem;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Set;

import javax.faces.application.FacesMessage;
import javax.faces.model.DataModel;
import javax.faces.model.SelectItem;
import javax.servlet.http.HttpServletRequest;

import org.rhq.core.domain.alert.AlertCondition;
import org.rhq.core.domain.alert.AlertConditionCategory;
import org.rhq.core.domain.alert.AlertConditionLog;
import org.rhq.core.domain.alert.composite.AlertHistoryComposite;
import org.rhq.core.domain.measurement.util.MeasurementConverter;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.core.gui.util.FacesContextUtility;
import org.rhq.enterprise.gui.common.converter.SelectItemUtils;
import org.rhq.enterprise.gui.common.framework.PagedDataTableUIBean;
import org.rhq.enterprise.gui.common.paging.PageControlView;
import org.rhq.enterprise.gui.common.paging.PagedListDataModel;
import org.rhq.enterprise.gui.legacy.WebUserPreferences;
import org.rhq.enterprise.gui.legacy.action.resource.common.monitor.alerts.AlertDefUtil;
import org.rhq.enterprise.gui.util.EnterpriseFacesContextUtility;
import org.rhq.enterprise.server.subsystem.AlertHistorySubsystemManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * @author Joseph Marques
 */
public class SubsystemAlertHistoryUIBean extends PagedDataTableUIBean {
    public static final String MANAGED_BEAN_NAME = "SubsystemAlertHistoryUIBean";
    private static final String FORM_PREFIX = "alertHistorySubsystemForm:";
    private final String CALENDAR_SUFFIX = "InputDate";

    private AlertHistorySubsystemManagerLocal manager = LookupUtil.getAlertHistorySubsystemManager();

    private static String datePattern;
    private String resourceFilter;
    private String parentFilter;
    private Date dateBeginFilter;
    private Date dateEndFilter;
    private String categoryFilter;
    private SelectItem[] categoryFilterItems;

    public SubsystemAlertHistoryUIBean() {
        datePattern = new WebUserPreferences(EnterpriseFacesContextUtility.getSubject())
            .getDateTimeDisplayPreferences().getDateTimeFormatTrigger();
        categoryFilterItems = SelectItemUtils.convertFromEnum(AlertConditionCategory.class, true);
        categoryFilter = (String) categoryFilterItems[0].getValue();
    }

    public String getDatePattern() {
        return datePattern;
    }

    public String getResourceFilter() {
        return resourceFilter;
    }

    public void setResourceFilter(String resourceFilter) {
        this.resourceFilter = resourceFilter;
    }

    public String getParentFilter() {
        return parentFilter;
    }

    public void setParentFilter(String parentFilter) {
        this.parentFilter = parentFilter;
    }

    public Date getDateBeginFilter() {
        return dateBeginFilter;
    }

    public void setDateBeginFilter(Date dateSubmittedFilter) {
        this.dateBeginFilter = dateSubmittedFilter;
    }

    public Date getDateEndFilter() {
        return dateEndFilter;
    }

    public void setDateEndFilter(Date dateCompletedFilter) {
        this.dateEndFilter = dateCompletedFilter;
    }

    public String getCategoryFilter() {
        return categoryFilter;
    }

    public void setCategoryFilter(String statusFilter) {
        this.categoryFilter = statusFilter;
    }

    public SelectItem[] getCategoryFilterItems() {
        return categoryFilterItems;
    }

    public void setCategoryFilterItems(SelectItem[] statusFilterItems) {
        this.categoryFilterItems = statusFilterItems;
    }

    @Override
    public DataModel getDataModel() {
        if (dataModel == null) {
            dataModel = new ResultsDataModel(PageControlView.SubsystemAlertHistory, MANAGED_BEAN_NAME);
        }

        return dataModel;
    }

    private class ResultsDataModel extends PagedListDataModel<AlertHistoryComposite> {
        public ResultsDataModel(PageControlView view, String beanName) {
            super(view, beanName);
        }

        @Override
        public PageList<AlertHistoryComposite> fetchPage(PageControl pc) {
            getDataFromRequest();

            String resourceFilter = getResourceFilter();
            String parentFilter = getParentFilter();
            Long startMillis = getDateBeginFilter() == null ? null : getDateBeginFilter().getTime();
            Long endMillis = getDateEndFilter() == null ? null : getDateEndFilter().getTime();
            String cleansedStatus = SelectItemUtils.cleanse(getCategoryFilter());
            AlertConditionCategory category = cleansedStatus == null ? null : AlertConditionCategory
                .valueOf(cleansedStatus);

            PageList<AlertHistoryComposite> result;
            result = manager.getAlertHistories(getSubject(), resourceFilter, parentFilter, startMillis, endMillis,
                category, pc);

            // format UI-layer display column attribute values
            HttpServletRequest request = FacesContextUtility.getRequest();
            for (AlertHistoryComposite history : result) {
                Set<AlertConditionLog> acls = history.getAlert().getConditionLogs();
                if (acls.size() > 1) {
                    history.setConditionText("Multiple Conditions");
                    history.setConditionValue("--");
                } else if (acls.size() == 1) {
                    AlertConditionLog log = acls.iterator().next();
                    AlertCondition condition = log.getCondition();
                    String displayText = AlertDefUtil.formatAlertConditionForDisplay(condition, request);

                    String firedValue = log.getValue();
                    if (condition.getMeasurementDefinition() != null) {
                        firedValue = MeasurementConverter.format(Double.valueOf(log.getValue()), condition
                            .getMeasurementDefinition().getUnits(), true);
                    }

                    history.setConditionText(displayText);
                    history.setConditionValue(firedValue);
                } else {
                    history.setConditionText("No Conditions");
                    history.setConditionValue("--");
                }
            }
            return result;
        }

        private void getDataFromRequest() {
            SubsystemAlertHistoryUIBean outer = SubsystemAlertHistoryUIBean.this;
            outer.resourceFilter = FacesContextUtility.getOptionalRequestParameter(FORM_PREFIX + "resourceFilter");
            outer.parentFilter = FacesContextUtility.getOptionalRequestParameter(FORM_PREFIX + "parentFilter");
            outer.dateBeginFilter = getDate(FacesContextUtility.getOptionalRequestParameter(FORM_PREFIX
                + "dateBeginFilter" + CALENDAR_SUFFIX));
            outer.dateEndFilter = getDate(FacesContextUtility.getOptionalRequestParameter(FORM_PREFIX + "dateEndFilter"
                + CALENDAR_SUFFIX));
            outer.categoryFilter = FacesContextUtility.getOptionalRequestParameter(FORM_PREFIX + "categoryFilter");
        }

        private Date getDate(String dateAsString) {
            if (dateAsString == null || dateAsString.trim().equals("")) {
                return null;
            }
            try {
                String datePattern = getDatePattern();
                return new SimpleDateFormat(datePattern).parse(dateAsString);
            } catch (ParseException pe) {
                FacesContextUtility.addMessage(FacesMessage.SEVERITY_ERROR, "Could not parse '" + dateAsString
                    + "' using the following format '" + datePattern + "'");
            }
            return null;
        }
    }
}