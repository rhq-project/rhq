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

    public void addFilterId(Integer filterId) {
        this.filterId = filterId;
    }

    public void addFilterTriggeredOperation(String filterTriggeredOperation) {
        this.filterTriggeredOperation = filterTriggeredOperation;
    }

    public void addFilterStartTime(Long filterStartTime) {
        this.filterStartTime = filterStartTime;
    }

    public void addFilterEndTime(Long filterEndTime) {
        this.filterEndTime = filterEndTime;
    }

    public void addFilterName(String filterName) {
        this.filterName = filterName;
    }

    public void addFilterDescription(String filterDescription) {
        this.filterDescription = filterDescription;
    }

    public void addFilterPriority(AlertPriority filterPriority) {
        this.filterPriority = filterPriority;
    }

    public void addFilterResourceTypeName(String filterResourceTypeName) {
        this.filterResourceTypeName = filterResourceTypeName;
    }

    public void addFilterResourceIds(List<Integer> filterResourceIds) {
        this.filterResourceIds = filterResourceIds;
    }

    public void fetchAlertDefinition(boolean fetchAlertDefinition) {
        this.fetchAlertDefinition = fetchAlertDefinition;
    }

    public void fetchConditionLogs(boolean fetchConditionLogs) {
        this.fetchConditionLogs = fetchConditionLogs;
    }

    public void fetchNotificationLogs(boolean fetchNotificationLogs) {
        this.fetchNotificationLogs = fetchNotificationLogs;
    }

    public void addSortName(PageOrdering sortName) {
        addSortField("name");
        this.sortName = sortName;
    }

    public void addSortCtime(PageOrdering sortCtime) {
        addSortField("ctime");
        this.sortCtime = sortCtime;
    }

    public void addSortPriority(PageOrdering sortPriority) {
        addSortField("priority");
        this.sortPriority = sortPriority;
    }
}
