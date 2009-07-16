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

import org.rhq.core.domain.resource.InventoryStatus;
import org.rhq.core.domain.resource.ResourceCategory;
import org.rhq.core.domain.util.PageOrdering;

/**
 * @author Joseph Marques
 */
public class ResourceCriteria extends Criteria {

    private Integer filterId;
    private String filterName;
    private String filterResourceKey;
    private InventoryStatus filterInventoryStatus;
    private String filterVersion;
    private String filterDescription;
    private String filterResourceTypeName; // needs overrides
    private ResourceCategory filterResourceCategory; // needs overrides
    private String filterPluginName; // needs overrides
    private String filterParentResourceName; // needs overrides
    private String filterAgentName; // needs overrides

    private boolean fetchResourceType;
    private boolean fetchChildResources;
    private boolean fetchParentResource;
    private boolean fetchResourceConfiguration;
    private boolean fetchPluginConfiguration;
    private boolean fetchAgent;
    private boolean fetchAlertDefinitions;
    private boolean fetchResourceConfigurationUpdates;
    private boolean fetchPluginConfigurationUpdates;
    private boolean fetchImplicitGroups;
    private boolean fetchExplicitGroups;
    private boolean fetchContentServiceRequests;
    private boolean fetchCreateChildResourceRequests;
    private boolean fetchDeleteResourceRequests;
    private boolean fetchOperationHistories;
    private boolean fetchInstalledPackages;
    private boolean fetchInstalledPackageHistory;
    private boolean fetchResourceChannels;
    private boolean fetchSchedules;
    private boolean fetchCurrentAvailability;
    private boolean fetchResourceErrors;
    private boolean fetchEventSources;
    private boolean fetchProductVersion;

    private PageOrdering sortName;
    private PageOrdering sortInventoryStatus;
    private PageOrdering sortVersion;
    private PageOrdering sortResourceTypeName; // needs overrides
    private PageOrdering sortResourceCategory; // needs overrides
    private PageOrdering sortPluginName; // needs overrides
    private PageOrdering sortParentResourceName; // needs overrides
    private PageOrdering sortAgentName; // needs overrides
    private PageOrdering sortCurrentAvailability; // needs overrides

    public ResourceCriteria() {
        super();

        filterOverrides.put("resourceTypeName", "resourceType.name like ?");
        filterOverrides.put("resourceCategory", "resourceType.category like ?");
        filterOverrides.put("plugin", "resourceType.plugin = ?");
        filterOverrides.put("parentResourceName", "parentResource.name like ?");
        filterOverrides.put("agentName", "agent.name like ?");

        sortOverrides.put("resourceTypeName", "resourceType.name");
        sortOverrides.put("resourceCategory", "resourceType.category");
        sortOverrides.put("pluginName", "resourceType.plugin");
        sortOverrides.put("parentResourceName", "parentResource.name");
        sortOverrides.put("agentName", "agent.name");
        sortOverrides.put("currentAvailability", "currentAvailability.availabilityType");
    }

    public void addFilterId(Integer filterId) {
        this.filterId = filterId;
    }

    public void addFilterName(String filterName) {
        this.filterName = filterName;
    }

    public void addFilterResourceKey(String filterResourceKey) {
        this.filterResourceKey = filterResourceKey;
    }

    public void addFilterInventoryStatus(InventoryStatus filterInventoryStatus) {
        this.filterInventoryStatus = filterInventoryStatus;
    }

    public void addFilterVersion(String filterVersion) {
        this.filterVersion = filterVersion;
    }

    public void addFilterDescription(String filterDescription) {
        this.filterDescription = filterDescription;
    }

    public void addFilterResourceTypeName(String filterResourceTypeName) {
        this.filterResourceTypeName = filterResourceTypeName;
    }

    public void addFilterResourceCategory(ResourceCategory filterResourceCategory) {
        this.filterResourceCategory = filterResourceCategory;
    }

    public void addFilterPluginName(String filterPluginName) {
        this.filterPluginName = filterPluginName;
    }

    public void addFilterParentResourceName(String filterParentResourceName) {
        this.filterParentResourceName = filterParentResourceName;
    }

    public void addFilterAgentName(String filterAgentName) {
        this.filterAgentName = filterAgentName;
    }

    public void fetchResourceType(boolean fetchResourceType) {
        this.fetchResourceType = fetchResourceType;
    }

    public void fetchChildResources(boolean fetchChildResources) {
        this.fetchChildResources = fetchChildResources;
    }

    public void fetchParentResource(boolean fetchParentResource) {
        this.fetchParentResource = fetchParentResource;
    }

    public void fetchResourceConfiguration(boolean fetchResourceConfiguration) {
        this.fetchResourceConfiguration = fetchResourceConfiguration;
    }

    public void fetchPluginConfiguration(boolean fetchPluginConfiguration) {
        this.fetchPluginConfiguration = fetchPluginConfiguration;
    }

    public void fetchAgent(boolean fetchAgent) {
        this.fetchAgent = fetchAgent;
    }

    public void fetchAlertDefinitions(boolean fetchAlertDefinitions) {
        this.fetchAlertDefinitions = fetchAlertDefinitions;
    }

    public void fetchResourceConfigurationUpdates(boolean fetchResourceConfigurationUpdates) {
        this.fetchResourceConfigurationUpdates = fetchResourceConfigurationUpdates;
    }

    public void fetchPluginConfigurationUpdates(boolean fetchPluginConfigurationUpdates) {
        this.fetchPluginConfigurationUpdates = fetchPluginConfigurationUpdates;
    }

    public void fetchImplicitGroups(boolean fetchImplicitGroups) {
        this.fetchImplicitGroups = fetchImplicitGroups;
    }

    public void fetchExplicitGroups(boolean fetchExplicitGroups) {
        this.fetchExplicitGroups = fetchExplicitGroups;
    }

    public void fetchContentServiceRequests(boolean fetchContentServiceRequests) {
        this.fetchContentServiceRequests = fetchContentServiceRequests;
    }

    public void fetchCreateChildResourceRequests(boolean fetchCreateChildResourceRequests) {
        this.fetchCreateChildResourceRequests = fetchCreateChildResourceRequests;
    }

    public void fetchDeleteResourceRequests(boolean fetchDeleteResourceRequests) {
        this.fetchDeleteResourceRequests = fetchDeleteResourceRequests;
    }

    public void fetchOperationHistories(boolean fetchOperationHistories) {
        this.fetchOperationHistories = fetchOperationHistories;
    }

    public void fetchInstalledPackages(boolean fetchInstalledPackages) {
        this.fetchInstalledPackages = fetchInstalledPackages;
    }

    public void fetchInstalledPackageHistory(boolean fetchInstalledPackageHistory) {
        this.fetchInstalledPackageHistory = fetchInstalledPackageHistory;
    }

    public void fetchResourceChannels(boolean fetchResourceChannels) {
        this.fetchResourceChannels = fetchResourceChannels;
    }

    public void fetchSchedules(boolean fetchSchedules) {
        this.fetchSchedules = fetchSchedules;
    }

    public void fetchCurrentAvailability(boolean fetchCurrentAvailability) {
        this.fetchCurrentAvailability = fetchCurrentAvailability;
    }

    public void fetchResourceErrors(boolean fetchResourceErrors) {
        this.fetchResourceErrors = fetchResourceErrors;
    }

    public void fetchEventSources(boolean fetchEventSources) {
        this.fetchEventSources = fetchEventSources;
    }

    public void fetchProductVersion(boolean fetchProductVersion) {
        this.fetchProductVersion = fetchProductVersion;
    }

    public void addSortName(PageOrdering sortName) {
        addSortField("name");
        this.sortName = sortName;
    }

    public void addSortInventoryStatus(PageOrdering sortInventoryStatus) {
        addSortField("inventoryStatus");
        this.sortInventoryStatus = sortInventoryStatus;
    }

    public void addSortVersion(PageOrdering sortVersion) {
        addSortField("version");
        this.sortVersion = sortVersion;
    }

    public void addSortResourceTypeName(PageOrdering sortResourceTypeName) {
        addSortField("resourceTypeName");
        this.sortResourceTypeName = sortResourceTypeName;
    }

    public void addSortResourceCategory(PageOrdering sortResourceCategory) {
        addSortField("resourceCategory");
        this.sortResourceCategory = sortResourceCategory;
    }

    public void addSortPluginName(PageOrdering sortPluginName) {
        addSortField("pluginName");
        this.sortPluginName = sortPluginName;
    }

    public void addSortParentResourceName(PageOrdering sortParentResourceName) {
        addSortField("parentResourceName");
        this.sortParentResourceName = sortParentResourceName;
    }

    public void addSortAgentName(PageOrdering sortAgentName) {
        addSortField("agentName");
        this.sortAgentName = sortAgentName;
    }

    public void addSortCurrentAvailability(PageOrdering sortCurrentAvailability) {
        addSortField("currentAvailability");
        this.sortCurrentAvailability = sortCurrentAvailability;
    }

}
