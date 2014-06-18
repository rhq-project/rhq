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
 * <strong>Important Note when calling as a non-inventory manager</strong>.  When searching for group level alert definitions you must
 * filter with {@link #addFilterResourceGroupIds(Integer...) even if further filtering with
 * {@link #addFilterId(Integer)} or {@link #addFilterIds(Integer...)}.   Otherwise proper authorization
 * can not be performed and no results will be returned.
 *
 * @author Joseph Marques
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
@SuppressWarnings("unused")
public class AlertDefinitionCriteria extends Criteria {
    private static final long serialVersionUID = 2L;

    public static final String SORT_FIELD_RESOURCE_ID = "resourceId";
    public static final String SORT_FIELD_RESOURCE_NAME = "resourceName";

    private String filterName;
    private String filterDescription;
    private AlertPriority filterPriority;
    private Integer filterAlertId; // requires overrides
    private NonBindingOverrideFilter filterAlertTemplateOnly; // requires overrides - finds only alert templates
    private Integer filterAlertTemplateParentId; // requires overrides
    private Integer filterAlertTemplateResourceTypeId; // requires overrides
    private String filterAlertTemplateResourceTypeName; // requires overrides
    private List<Integer> filterResourceIds; // requires overrides
    private List<Integer> filterResourceGroupIds; // requires overrides
    private Integer filterGroupAlertDefinitionId; // requires overrides
    private Integer filterGroupAlertDefinitionGroupId; // requires overrides
    private Boolean filterReadOnly;
    private Boolean filterEnabled;
    private Boolean filterDeleted = false; // by default don't return deleted definitions
    private NonBindingOverrideFilter filterResourceOnly; // requires overrides - finds only those associated with a resource
    private List<String> filterNotificationSenderNames;

    private boolean fetchAlerts;
    private boolean fetchGroupAlertDefinition;
    private boolean fetchConditions;
    private boolean fetchAlertNotifications;
    private boolean fetchResource;
    private boolean fetchResourceType;

    private PageOrdering sortName;
    private PageOrdering sortPriority;
    private PageOrdering sortResourceId; // requires sort override
    private PageOrdering sortResourceName; // requires sort override

    public AlertDefinitionCriteria() {
        filterOverrides.put("alertId", "" + "id IN ( SELECT alert.alertDefinition.id " //
            + "          FROM Alert alert " //
            + "         WHERE alert.id = ? )");
        filterOverrides.put("alertTemplateOnly", "resourceType IS NOT NULL");
        filterOverrides.put("alertTemplateParentId", "parentId = ?");
        filterOverrides.put("alertTemplateResourceTypeId", "resourceType.id = ?");
        filterOverrides.put("alertTemplateResourceTypeName", "resourceType.name like ?");
        filterOverrides.put("resourceIds", "resource.id IN ( ? )");
        filterOverrides.put("resourceGroupIds", "group.id IN ( ? )");
        filterOverrides.put("groupAlertDefinitionId", "groupAlertDefinition.id = ?");
        filterOverrides.put("groupAlertDefinitionGroupId", "groupAlertDefinition.group.id = ?");
        filterOverrides.put("resourceOnly", "resource IS NOT NULL");
        filterOverrides.put("notificationSenderNames", "id IN ("
            + "SELECT notif.alertDefinition.id FROM AlertNotification notif " + "WHERE notif.senderName IN ( ? ))");

        sortOverrides.put(SORT_FIELD_RESOURCE_ID, "resource.id");
        sortOverrides.put(SORT_FIELD_RESOURCE_NAME, "resource.name");

        fetchGroupAlertDefinition = true; // fetch group alert def by default
    }

    @Override
    public Class<AlertDefinition> getPersistentClass() {
        return AlertDefinition.class;
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

    public void addFilterAlertId(Integer filterAlertId) {
        this.filterAlertId = filterAlertId;
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

    /**
     * This must be supplied when searching for group-level definitions, even if further filtered by a specific ID.
     *
     * @param filterResourceGroupIds
     */
    public void addFilterResourceGroupIds(Integer... filterResourceGroupIds) {
        this.filterResourceGroupIds = Arrays.asList(filterResourceGroupIds);
    }

    public void addFilterEnabled(Boolean filterEnabled) {
        this.filterEnabled = filterEnabled;
    }

    public void addFilterDeleted(Boolean filterDeleted) {
        this.filterDeleted = filterDeleted;
    }

    public void addFilterResourceOnly(boolean filterResourceOnly) {
        this.filterResourceOnly = (filterResourceOnly ? NonBindingOverrideFilter.ON : NonBindingOverrideFilter.OFF);
    }

    public void addFilterAlertTemplateOnly(boolean filterAlertTemplateOnly) {
        this.filterAlertTemplateOnly = (filterAlertTemplateOnly ? NonBindingOverrideFilter.ON
            : NonBindingOverrideFilter.OFF);
    }

    public void addFilterNotificationNames(String... notificationNames) {
        fetchAlertNotifications(true);
        this.filterNotificationSenderNames = Arrays.asList(notificationNames);
    }

    public void addFilterGroupAlertDefinitionId(Integer groupAlertDefinitionId) {
        this.filterGroupAlertDefinitionId = groupAlertDefinitionId;
    }

    public void addFilterGroupAlertDefinitionGroupId(Integer groupAlertDefinitionGroupId) {
        this.filterGroupAlertDefinitionGroupId = groupAlertDefinitionGroupId;
    }

    public void addFilterReadOnly(Boolean readOnly) {
        this.filterReadOnly = readOnly;
    }

    public void fetchAlerts(boolean fetchAlerts) {
        this.fetchAlerts = fetchAlerts;
    }

    public void fetchGroupAlertDefinition(boolean fetchGroupAlertDefinition) {
        this.fetchGroupAlertDefinition = fetchGroupAlertDefinition;
    }

    public void fetchConditions(boolean fetchConditions) {
        this.fetchConditions = fetchConditions;
    }

    public void fetchAlertNotifications(boolean fetchAlertNotifications) {
        this.fetchAlertNotifications = fetchAlertNotifications;
    }

    public void fetchResource(boolean fetchResource) {
        this.fetchResource = fetchResource;
    }

    public void fetchResourceType(boolean fetchResourceType) {
        this.fetchResourceType = fetchResourceType;
    }

    public void addSortName(PageOrdering sortName) {
        addSortField("name");
        this.sortName = sortName;
    }

    public void addSortPriority(PageOrdering sortPriority) {
        addSortField("priority");
        this.sortPriority = sortPriority;
    }

    public boolean isTemplateCriteria() {
        return null != filterAlertTemplateOnly //
            || null != filterAlertTemplateParentId //
            || null != filterAlertTemplateResourceTypeId //
            || null != filterAlertTemplateResourceTypeName;

    }

    public boolean isGroupCriteria() {
        return null != filterResourceGroupIds;
    }

    public void addSortResourceId(PageOrdering sortResourceId) {
        addSortField(SORT_FIELD_RESOURCE_ID);
        this.sortResourceId = sortResourceId;
    }

    public void addSortResourceName(PageOrdering sortResourceName) {
        addSortField(SORT_FIELD_RESOURCE_NAME);
        this.sortResourceName = sortResourceName;
    }

}
