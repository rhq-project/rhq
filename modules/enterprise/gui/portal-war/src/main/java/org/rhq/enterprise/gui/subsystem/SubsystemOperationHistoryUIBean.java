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

import javax.faces.application.FacesMessage;
import javax.faces.model.DataModel;
import javax.faces.model.SelectItem;

import org.rhq.core.domain.operation.OperationRequestStatus;
import org.rhq.core.domain.operation.composite.ResourceOperationHistoryComposite;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.core.gui.util.FacesContextUtility;
import org.rhq.enterprise.gui.common.converter.SelectItemUtils;
import org.rhq.enterprise.gui.common.framework.PagedDataTableUIBean;
import org.rhq.enterprise.gui.common.paging.PageControlView;
import org.rhq.enterprise.gui.common.paging.PagedListDataModel;
import org.rhq.enterprise.gui.util.EnterpriseFacesContextUtility;
import org.rhq.enterprise.server.subsystem.OperationHistorySubsystemManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * @author Joseph Marques
 */
public class SubsystemOperationHistoryUIBean extends PagedDataTableUIBean {
    public static final String MANAGED_BEAN_NAME = "SubsystemOperationHistoryUIBean";
    private static final String FORM_PREFIX = "operationHistorySubsystemForm:";
    private final String CALENDAR_SUFFIX = "InputDate";

    private OperationHistorySubsystemManagerLocal manager = LookupUtil.getOperationHistorySubsystemManager();

    private static String datePattern;
    private String resourceFilter;
    private String parentFilter;
    private Date dateSubmittedFilter;
    private Date dateCompletedFilter;
    private String statusFilter;
    private SelectItem[] statusFilterItems;

    public SubsystemOperationHistoryUIBean() {
        datePattern = EnterpriseFacesContextUtility.getWebUser().getWebPreferences().getDateTimeDisplayPreferences()
            .getDateTimeFormatTrigger();
        statusFilterItems = SelectItemUtils.convertFromEnum(OperationRequestStatus.class, true);
        statusFilter = (String) statusFilterItems[0].getValue();
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

    public Date getDateSubmittedFilter() {
        return dateSubmittedFilter;
    }

    public void setDateSubmittedFilter(Date dateSubmittedFilter) {
        this.dateSubmittedFilter = dateSubmittedFilter;
    }

    public Date getDateCompletedFilter() {
        return dateCompletedFilter;
    }

    public void setDateCompletedFilter(Date dateCompletedFilter) {
        this.dateCompletedFilter = dateCompletedFilter;
    }

    public String getStatusFilter() {
        return statusFilter;
    }

    public void setStatusFilter(String statusFilter) {
        this.statusFilter = statusFilter;
    }

    public SelectItem[] getStatusFilterItems() {
        return statusFilterItems;
    }

    public void setStatusFilterItems(SelectItem[] statusFilterItems) {
        this.statusFilterItems = statusFilterItems;
    }

    @Override
    public DataModel getDataModel() {
        if (dataModel == null) {
            dataModel = new ResultsDataModel(PageControlView.SubsystemResourceOperationHistory, MANAGED_BEAN_NAME);
        }

        return dataModel;
    }

    private class ResultsDataModel extends PagedListDataModel<ResourceOperationHistoryComposite> {
        public ResultsDataModel(PageControlView view, String beanName) {
            super(view, beanName);
        }

        @Override
        public PageList<ResourceOperationHistoryComposite> fetchPage(PageControl pc) {
            getDataFromRequest();

            String resourceFilter = getResourceFilter();
            String parentFilter = getParentFilter();
            Long startMillis = getDateSubmittedFilter() == null ? null : getDateSubmittedFilter().getTime();
            Long endMillis = getDateCompletedFilter() == null ? null : getDateCompletedFilter().getTime();
            String cleansedStatus = SelectItemUtils.cleanse(getStatusFilter());
            OperationRequestStatus status = cleansedStatus == null ? null : OperationRequestStatus
                .valueOf(cleansedStatus);

            PageList<ResourceOperationHistoryComposite> result;
            result = manager.getResourceOperationHistories(getSubject(), resourceFilter, parentFilter, startMillis,
                endMillis, status, pc);
            return result;
        }

        private void getDataFromRequest() {
            SubsystemOperationHistoryUIBean outer = SubsystemOperationHistoryUIBean.this;
            outer.resourceFilter = FacesContextUtility.getOptionalRequestParameter(FORM_PREFIX + "resourceFilter");
            outer.parentFilter = FacesContextUtility.getOptionalRequestParameter(FORM_PREFIX + "parentFilter");
            outer.dateSubmittedFilter = getDate(FacesContextUtility.getOptionalRequestParameter(FORM_PREFIX
                + "dateSubmittedFilter" + CALENDAR_SUFFIX));
            outer.dateCompletedFilter = getDate(FacesContextUtility.getOptionalRequestParameter(FORM_PREFIX
                + "dateCompletedFilter" + CALENDAR_SUFFIX));
            outer.statusFilter = FacesContextUtility.getOptionalRequestParameter(FORM_PREFIX + "statusFilter");
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