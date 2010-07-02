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

import java.util.Arrays;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

import org.rhq.core.domain.alert.AlertDefinition;
import org.rhq.core.domain.alert.AlertPriority;
import org.rhq.core.domain.util.PageOrdering;

/**
 * @author Joseph Marques
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
@SuppressWarnings("unused")
public class AlertDefinitionCriteria extends Criteria {
    private static final long serialVersionUID = 1L;

    private Integer filterId;
    private String filterName;
    private String filterDescription;
    private AlertPriority filterPriority;
    private Integer filterAlertTemplateParentId; // requires overrides
    private Integer filterAlertTemplateResourceTypeId; // requires overrides
    private String filterAlertTemplateResourceTypeName; // requires overrides
    private List<Integer> filterResourceIds; // requires overrides
    private List<Integer> filterResourceGroupIds; // requires overrides
    private Boolean filterEnabled;
    private String filterOperationName; // requires overrides
    private Boolean filterDeleted;

    private boolean fetchAlerts;
    private boolean fetchConditions;
    private boolean fetchAlertNotifications;

    private PageOrdering sortName;
    private PageOrdering sortPriority;

    public AlertDefinitionCriteria() {
        filterOverrides.put("alertTemplateParentId", "parentId = ?");
        filterOverrides.put("alertTemplateResourceTypeId", "resourceType.id = ?");
        filterOverrides.put("alertTemplateResourceTypeName", "resourceType.name like ?");
        filterOverrides.put("resourceIds", "resource.id IN ( ? )");
        filterOverrides.put("resourceGroupIds", "resourceGroup.id IN ( ? )");
        filterOverrides.put("operationName", "operationDefinition.name like ?");
    }

    @Override
    public Class<?> getPersistentClass() {
        return AlertDefinition.class;
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

    public void addFilterAlertTemplateParentId(Integer filterAlertTemplateParentId) {
        this.filterAlertTemplateParentId = filterAlertTemplateParentId;
    }

    public void addFilterAlertTemplateResourceTypeId(Integer filterAlertTemplateResourceTypeId) {
        this.filterAlertTemplateResourceTypeId = filterAlertTemplateResourceTypeId;
    }

    public void addFilterAlertTemplateResourceTypeName(String filterAlertTemplateResourceTypeName) {
        this.filterAlertTemplateResourceTypeName = filterAlertTemplateResourceTypeName;
    }

    public void addFilterResourceIds(Integer... filterResourceIds) {
        this.filterResourceIds = Arrays.asList(filterResourceIds);
    }

    public void addFilterResourceGroupIds(Integer... filterResourceGroupIds) {
        this.filterResourceGroupIds = Arrays.asList(filterResourceGroupIds);
    }

    public void addFilterEnabled(Boolean filterEnabled) {
        this.filterEnabled = filterEnabled;
    }

    public void addFilterOperationName(String filterOperationName) {
        this.filterOperationName = filterOperationName;
    }

    public void addFilterDeleted(Boolean filterDeleted) {
        this.filterDeleted = filterDeleted;
    }

    public void fetchAlerts(boolean fetchAlerts) {
        this.fetchAlerts = fetchAlerts;
    }

    public void fetchConditions(boolean fetchConditions) {
        this.fetchConditions = fetchConditions;
    }

    public void fetchAlertNotifications(boolean fetchAlertNotifications) {
        this.fetchAlertNotifications = fetchAlertNotifications;
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
