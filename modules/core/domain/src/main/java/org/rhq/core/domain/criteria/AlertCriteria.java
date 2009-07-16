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

import org.rhq.core.domain.alert.AlertPriority;
import org.rhq.core.domain.util.PageOrdering;

/**
 * @author Joseph Marques
 */
public class AlertCriteria extends Criteria {

    private Integer filterId;
    private String filterTriggeredOperation;
    private Long filterStartTime;
    private Long filterEndTime;
    private String filterName;
    private String filterDescription;
    private AlertPriority filterPriority;
    private String filterResourceTypeName;
    private List<Integer> filterResourceIds;

    private boolean fetchAlertDefinition;
    private boolean fetchConditionLogs;
    private boolean fetchNotificationLogs;

    private PageOrdering sortName;
    private PageOrdering sortCtime;
    private PageOrdering sortPriority;

    public AlertCriteria() {
        super();

        filterOverrides.put("startTime", "ctime >= ?");
        filterOverrides.put("endTime", "ctime <= ?");
        filterOverrides.put("name", "alertDefinition.name like ?");
        filterOverrides.put("description", "alertDefinition.description like ?");
        filterOverrides.put("priority", "alertDefinition.priority = ?");
        filterOverrides.put("resourceTypeName", "alertDefinition.resourceType.name like ?");
        filterOverrides.put("resourceIds", "alertDefinition.resource.id IN ( ? )");

        sortOverrides.put("name", "alertDefinition.name");
        sortOverrides.put("priority", "alertDefinition.priority");
    }

    public Integer getFilterId() {
        return filterId;
    }

    public void setFilterId(Integer filterId) {
        this.filterId = filterId;
    }

    public String getFilterTriggeredOperation() {
        return filterTriggeredOperation;
    }

    public void setFilterTriggeredOperation(String filterTriggeredOperation) {
        this.filterTriggeredOperation = filterTriggeredOperation;
    }

    public Long getFilterStartTime() {
        return filterStartTime;
    }

    public void setFilterStartTime(Long filterStartTime) {
        this.filterStartTime = filterStartTime;
    }

    public Long getFilterEndTime() {
        return filterEndTime;
    }

    public void setFilterEndTime(Long filterEndTime) {
        this.filterEndTime = filterEndTime;
    }

    public String getFilterName() {
        return filterName;
    }

    public void setFilterName(String filterName) {
        this.filterName = filterName;
    }

    public String getFilterDescription() {
        return filterDescription;
    }

    public void setFilterDescription(String filterDescription) {
        this.filterDescription = filterDescription;
    }

    public AlertPriority getFilterPriority() {
        return filterPriority;
    }

    public void setFilterPriority(AlertPriority filterPriority) {
        this.filterPriority = filterPriority;
    }

    public String getFilterResourceTypeName() {
        return filterResourceTypeName;
    }

    public void setFilterResourceTypeName(String filterResourceTypeName) {
        this.filterResourceTypeName = filterResourceTypeName;
    }

    public List<Integer> getFilterResourceIds() {
        return filterResourceIds;
    }

    public void setFilterResourceIds(List<Integer> filterResourceIds) {
        this.filterResourceIds = filterResourceIds;
    }

    public boolean isFetchAlertDefinition() {
        return fetchAlertDefinition;
    }

    public void setFetchAlertDefinition(boolean fetchAlertDefinition) {
        this.fetchAlertDefinition = fetchAlertDefinition;
    }

    public boolean isFetchConditionLogs() {
        return fetchConditionLogs;
    }

    public void setFetchConditionLogs(boolean fetchConditionLogs) {
        this.fetchConditionLogs = fetchConditionLogs;
    }

    public boolean isFetchNotificationLogs() {
        return fetchNotificationLogs;
    }

    public void setFetchNotificationLogs(boolean fetchNotificationLogs) {
        this.fetchNotificationLogs = fetchNotificationLogs;
    }

    public PageOrdering getSortName() {
        return sortName;
    }

    public void setSortName(PageOrdering sortName) {
        addSortField("name");
        this.sortName = sortName;
    }

    public PageOrdering getSortCtime() {
        return sortCtime;
    }

    public void setSortCtime(PageOrdering sortCtime) {
        addSortField("ctime");
        this.sortCtime = sortCtime;
    }

    public PageOrdering getSortPriority() {
        return sortPriority;
    }

    public void setSortPriority(PageOrdering sortPriority) {
        addSortField("priority");
        this.sortPriority = sortPriority;
    }
}
