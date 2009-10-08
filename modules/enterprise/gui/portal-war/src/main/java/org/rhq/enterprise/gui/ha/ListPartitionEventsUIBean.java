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
package org.rhq.enterprise.gui.ha;

import javax.faces.application.FacesMessage;
import javax.faces.model.DataModel;
import javax.faces.model.SelectItem;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.cloud.PartitionEvent;
import org.rhq.core.domain.cloud.PartitionEventType;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.core.gui.util.FacesContextUtility;
import org.rhq.core.gui.util.StringUtility;
import org.rhq.enterprise.gui.common.converter.SelectItemUtils;
import org.rhq.enterprise.gui.common.framework.PagedDataTableUIBean;
import org.rhq.enterprise.gui.common.paging.PageControlView;
import org.rhq.enterprise.gui.common.paging.PagedListDataModel;
import org.rhq.enterprise.gui.util.EnterpriseFacesContextUtility;
import org.rhq.enterprise.server.cloud.PartitionEventManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * @author Joseph Marques
 * @author Jason Dobies
 */
public class ListPartitionEventsUIBean extends PagedDataTableUIBean {

    private static final Log log = LogFactory.getLog(ListPartitionEventsUIBean.class);

    public static final String MANAGED_BEAN_NAME = "ListPartitionEventsUIBean";

    private PartitionEventManagerLocal partitionEventManager = LookupUtil.getPartitionEventManager();

    // begin filter stuff
    private String typeFilter;
    private String detailsFilter;
    private String executionStatusFilter;
    private SelectItem[] typeSelectItems;
    private SelectItem[] executionStatusSelectItems;

    public String getTypeFilter() {
        if (typeFilter == null) {
            typeFilter = SelectItemUtils.getSelectItemFilter("partitionEventsForm:typeFilter");
        }
        return SelectItemUtils.cleanse(typeFilter);
    }

    public void setTypeFilter(String typeFilter) {
        this.typeFilter = typeFilter;
    }

    public PartitionEventType getPartitionEventType() {
        String typeName = getTypeFilter();
        if (typeName != null) {
            return Enum.valueOf(PartitionEventType.class, typeName);
        }
        return null;
    }

    public String getDetailsFilter() {
        return detailsFilter;
    }

    public void setDetailsFilter(String detailsFilter) {
        this.detailsFilter = detailsFilter;
    }

    public String getExecutionStatusFilter() {
        if (executionStatusFilter == null) {
            executionStatusFilter = SelectItemUtils.getSelectItemFilter("partitionEventsForm:executionStatusFilter");
        }
        return SelectItemUtils.cleanse(executionStatusFilter);
    }

    public void setExecutionStatusFilter(String executionStatusFilter) {
        this.executionStatusFilter = executionStatusFilter;
    }

    public PartitionEvent.ExecutionStatus getExecutionStatus() {
        String executionStatusName = getExecutionStatusFilter();
        if (executionStatusName != null) {
            return Enum.valueOf(PartitionEvent.ExecutionStatus.class, executionStatusName);
        }
        return null;
    }

    public SelectItem[] getTypeSelectItems() {
        if (typeSelectItems == null) {
            typeSelectItems = SelectItemUtils.convertFromEnum(PartitionEventType.class, true);
        }

        return typeSelectItems;
    }

    public SelectItem[] getExecutionStatusSelectItems() {
        if (executionStatusSelectItems == null) {
            executionStatusSelectItems = SelectItemUtils.convertFromEnum(PartitionEvent.ExecutionStatus.class, true);
        }

        return executionStatusSelectItems;
    }

    // end filter stuff

    @Override
    public DataModel getDataModel() {
        if (dataModel == null) {
            dataModel = new ListPartitionEventsDataModel(PageControlView.ListPartitionEventsView, MANAGED_BEAN_NAME);
        }

        return dataModel;
    }

    public String removeSelectedEvents() {
        Subject subject = EnterpriseFacesContextUtility.getSubject();

        String[] selectedEvents = getSelectedAlerts();
        Integer[] eventIds = StringUtility.getIntegerArray(selectedEvents);

        try {
            partitionEventManager.deletePartitionEvents(subject, eventIds);
            FacesContextUtility.addMessage(FacesMessage.SEVERITY_INFO, "Deleted " + eventIds.length + " events.");
        } catch (Exception e) {
            FacesContextUtility.addMessage(FacesMessage.SEVERITY_ERROR, "Failed to delete selected events.", e);
        }

        return "success";
    }

    public String purgeAllEvents() {
        Subject subject = EnterpriseFacesContextUtility.getSubject();

        try {
            int numDeleted = partitionEventManager.purgeAllEvents(subject);
            FacesContextUtility.addMessage(FacesMessage.SEVERITY_INFO, "Deleted " + numDeleted + " events");
        } catch (Exception e) {
            FacesContextUtility.addMessage(FacesMessage.SEVERITY_ERROR, "Failed to purge events", e);
            log.error("Failed to purge events", e);
        }

        return "success";
    }

    public String repartition() {
        Subject subject = EnterpriseFacesContextUtility.getSubject();

        try {
            partitionEventManager.cloudPartitionEventRequest(subject, PartitionEventType.ADMIN_INITIATED_PARTITION, "");
        } catch (Exception e) {
            FacesContextUtility
                .addMessage(FacesMessage.SEVERITY_ERROR, "Forced repartition / redistribution failed", e);
            log.error("Forced repartition / redistribution failed", e);
        }

        return "success";
    }

    private String[] getSelectedAlerts() {
        return FacesContextUtility.getRequest().getParameterValues("selectedEvents");
    }

    private class ListPartitionEventsDataModel extends PagedListDataModel<PartitionEvent> {

        private ListPartitionEventsDataModel(PageControlView view, String beanName) {
            super(view, beanName);
        }

        @Override
        public PageList<PartitionEvent> fetchPage(PageControl pc) {
            Subject subject = EnterpriseFacesContextUtility.getSubject();

            if (getDetailsFilter() == null) {
                setDetailsFilter(FacesContextUtility.getOptionalRequestParameter("partitionEventsForm:detailsFilter"));
            }

            String details = ListPartitionEventsUIBean.this.getDetailsFilter();
            PartitionEventType type = ListPartitionEventsUIBean.this.getPartitionEventType();
            PartitionEvent.ExecutionStatus status = ListPartitionEventsUIBean.this.getExecutionStatus();

            PageList<PartitionEvent> list = partitionEventManager
                .getPartitionEvents(subject, type, status, details, pc);

            return list;
        }
    }
}
