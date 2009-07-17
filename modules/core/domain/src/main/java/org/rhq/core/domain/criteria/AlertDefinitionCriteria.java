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
public class AlertDefinitionCriteria extends Criteria {

    private Integer filterId;
    private String filterName;
    private String filterDescription;
    private AlertPriority filterPriority;
    private String filterAlertTemplateParentId; // requires overrides
    private String filterAlertTemplateResourceTypeId; // requires overrides
    private String filterAlertTemplateResourceTypeName; // requires overrides
    private List<Integer> filterResourceIds; // requires overrides
    private Boolean filterEnabled;
    private String filterOperationName; // requires overrides

    private boolean fetchAlertDefinition;
    private boolean fetchConditionLogs;
    private boolean fetchNotificationLogs;

    private PageOrdering sortName;
    private PageOrdering sortPriority;

    public AlertDefinitionCriteria() {
        super();

        filterOverrides.put("alertTemplateParentId", "parentId = ?");
        filterOverrides.put("alertTemplateResourceTypeId", "alertDefinition.resourceType.id = ?");
        filterOverrides.put("alertTemplateResourceTypeName", "alertDefinition.resourceType.name like ?");
        filterOverrides.put("resourceIds", "alertDefinition.resource.id IN ( ? )");
        filterOverrides.put("operationName", "operationDefinition.name like ?");
    }

    public void addFilterId(Integer filterId) {
        this.filterId = filterId;
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

    public void addFilterAlertTemplateParentId(String filterAlertTemplateParentId) {
        this.filterAlertTemplateParentId = filterAlertTemplateParentId;
    }

    public void addFilterAlertTemplateResourceTypeId(String filterAlertTemplateResourceTypeId) {
        this.filterAlertTemplateResourceTypeId = filterAlertTemplateResourceTypeId;
    }

    public void addFilterAlertTemplateResourceTypeName(String filterAlertTemplateResourceTypeName) {
        this.filterAlertTemplateResourceTypeName = filterAlertTemplateResourceTypeName;
    }

    public void addFilterResourceIds(List<Integer> filterResourceIds) {
        this.filterResourceIds = filterResourceIds;
    }

    public void addFilterEnabled(Boolean filterEnabled) {
        this.filterEnabled = filterEnabled;
    }

    public void addFilterOperationName(String filterOperationName) {
        this.filterOperationName = filterOperationName;
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

    public void addSortPriority(PageOrdering sortPriority) {
        addSortField("priority");
        this.sortPriority = sortPriority;
    }
}
