/*
 * RHQ Management Platform
 * Copyright (C) 2005-2008 Red Hat, Inc.
 * All rights reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2, as
 * published by the Free Software Foundation, and/or the GNU Lesser
 * General Public License, version 2.1, also as published by the Free
 * Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License and the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License
 * and the GNU Lesser General Public License along with this program;
 * if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */
package org.rhq.core.domain.criteria;

import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;

import org.rhq.core.domain.operation.JobId;
import org.rhq.core.domain.operation.OperationHistory;
import org.rhq.core.domain.operation.OperationRequestStatus;
import org.rhq.core.domain.util.CriteriaUtils;
import org.rhq.core.domain.util.PageOrdering;

/**
 * @author Joseph Marques
 */
@XmlAccessorType(XmlAccessType.FIELD)
@SuppressWarnings("unused")
public abstract class OperationHistoryCriteria extends Criteria {
    private static final long serialVersionUID = 1L;

    private Integer filterId;
    private String filterJobName;
    private String filterJobGroup;
    private OperationRequestStatus filterStatus;
    private List<OperationRequestStatus> filterStatuses; // requires overrides
    private String filterErrorMessage;
    private Integer filterOperationDefinitionId; // requires override
    private String filterOperationName; // requires override
    private Long filterStartTime; // requires override
    private Long filterEndTime; // requires override

    private boolean fetchOperationDefinition;
    private boolean fetchParameters;

    private PageOrdering sortStatus;
    private PageOrdering sortStartTime; // requires override
    private PageOrdering sortEndTime; // requires override
    private PageOrdering sortOperationName; // requires override

    public OperationHistoryCriteria() {
        filterOverrides.put("operationDefinitionId", "operationDefinition.id = ?");
        filterOverrides.put("operationName", "operationDefinition.name like ?");
        filterOverrides.put("startTime", "startedTime >= ?");
        filterOverrides.put("endTime", "modifiedTime <= ?");

        filterOverrides.put("resourceIds", "id IN " //
            + " ( SELECT roh.id " //
            + "     FROM ResourceOperationHistory roh " //
            + "    WHERE roh.resource.id IN ( ? ) ) ");

        filterOverrides.put("statuses", "status IN ( ? )");

        sortOverrides.put("startTime", "startedTime");
        sortOverrides.put("endTime", "modifiedTime");
        sortOverrides.put("operationName", "operationDefinition.name");
    }

    @Override
    public Class<? extends OperationHistory> getPersistentClass() {
        return OperationHistory.class;
    }

    public void addFilterId(Integer filterId) {
        this.filterId = filterId;
    }

    public void addFilterJobId(JobId filterJobId) {
        this.filterJobName = filterJobId.getJobName();
        this.filterJobGroup = filterJobId.getJobGroup();
    }

    public void addFilterStatus(OperationRequestStatus filterStatus) {
        this.filterStatus = filterStatus;
    }

    public void addFilterStatuses(OperationRequestStatus... operationStatus) {
        this.filterStatuses = CriteriaUtils.getListIgnoringNulls(operationStatus);
    }

    public void addFilterErrorMessage(String filterErrorMessage) {
        this.filterErrorMessage = filterErrorMessage;
    }

    public void addFilterOperationDefinitionId(Integer filterOperationDefinitionId) {
        this.filterOperationDefinitionId = filterOperationDefinitionId;
    }

    public void addFilterOperationName(String filterOperationName) {
        this.filterOperationName = filterOperationName;
    }

    public void addFilterStartTime(Long filterStartTime) {
        this.filterStartTime = filterStartTime;
    }

    public void addFilterEndTime(Long filterEndTime) {
        this.filterEndTime = filterEndTime;
    }

    public void fetchOperationDefinition(boolean fetchOperationDefinition) {
        this.fetchOperationDefinition = fetchOperationDefinition;
    }

    public void fetchParameters(boolean fetchParameters) {
        this.fetchParameters = fetchParameters;
    }

    public void addSortStatus(PageOrdering sortStatus) {
        addSortField("sort");
        this.sortStatus = sortStatus;
    }

    public void addSortStartTime(PageOrdering sortStartTime) {
        addSortField("startTime");
        this.sortStartTime = sortStartTime;
    }

    public void addSortEndTime(PageOrdering sortEndTime) {
        addSortField("endTime");
        this.sortEndTime = sortEndTime;
    }

    public void addSortOperationName(PageOrdering sortOperationName) {
        addSortField("operationName");
        this.sortOperationName = sortOperationName;
    }
}
