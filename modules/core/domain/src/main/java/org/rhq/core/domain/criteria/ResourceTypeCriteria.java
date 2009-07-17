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

import org.rhq.core.domain.resource.CreateDeletePolicy;
import org.rhq.core.domain.resource.ResourceCategory;
import org.rhq.core.domain.resource.ResourceCreationDataType;
import org.rhq.core.domain.resource.ResourceSubCategory;
import org.rhq.core.domain.util.PageOrdering;

/**
 * @author Joseph Marques
 */
public class ResourceTypeCriteria extends Criteria {

    private Integer filterId;
    private String filterName;
    private String filterDescription;
    private ResourceCategory filterCategory;
    private ResourceCreationDataType filterCreationDataType;
    private CreateDeletePolicy filterCreateDeletePolicy;
    private Boolean filterSupportsManualAdd;
    private String filterPluginName; // needs overrides
    private ResourceSubCategory filterSubCategory;

    private boolean fetchChildResourceTypes;
    private boolean fetchParentResourceTypes;
    private boolean fetchPluginConfigurationDefinition;
    private boolean fetchResourceConfigurationDefinition;
    private boolean fetchMetricDefinitions;
    private boolean fetchEventDefinitions;
    private boolean fetchOperationDefinitions;
    private boolean fetchProcessScans;
    private boolean fetchPackageTypes;
    private boolean fetchSubCategories;
    private boolean fetchResources;
    private boolean fetchProductVersions;

    private PageOrdering sortName;
    private PageOrdering sortCategory;
    private PageOrdering sortPluginName; // needs overrides

    public ResourceTypeCriteria() {
        super();

        filterOverrides.put("pluginName", "plugin = ?");

        sortOverrides.put("pluginName", "plugin");
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

    public void addFilterCategory(ResourceCategory filterCategory) {
        this.filterCategory = filterCategory;
    }

    public void addFilterCreationDataType(ResourceCreationDataType filterCreationDataType) {
        this.filterCreationDataType = filterCreationDataType;
    }

    public void addFilterCreateDeletePolicy(CreateDeletePolicy filterCreateDeletePolicy) {
        this.filterCreateDeletePolicy = filterCreateDeletePolicy;
    }

    public void addFilterSupportsManualAdd(Boolean filterSupportsManualAdd) {
        this.filterSupportsManualAdd = filterSupportsManualAdd;
    }

    public void addFilterPluginName(String filterPluginName) {
        this.filterPluginName = filterPluginName;
    }

    public void addFilterSubCategory(ResourceSubCategory filterSubCategory) {
        this.filterSubCategory = filterSubCategory;
    }

    public void fetchChildResourceTypes(boolean fetchChildResourceTypes) {
        this.fetchChildResourceTypes = fetchChildResourceTypes;
    }

    public void fetchParentResourceTypes(boolean fetchParentResourceTypes) {
        this.fetchParentResourceTypes = fetchParentResourceTypes;
    }

    public void fetchPluginConfigurationDefinition(boolean fetchPluginConfigurationDefinition) {
        this.fetchPluginConfigurationDefinition = fetchPluginConfigurationDefinition;
    }

    public void fetchResourceConfigurationDefinition(boolean fetchResourceConfigurationDefinition) {
        this.fetchResourceConfigurationDefinition = fetchResourceConfigurationDefinition;
    }

    public void fetchMetricDefinitions(boolean fetchMetricDefinitions) {
        this.fetchMetricDefinitions = fetchMetricDefinitions;
    }

    public void fetchEventDefinitions(boolean fetchEventDefinitions) {
        this.fetchEventDefinitions = fetchEventDefinitions;
    }

    public void fetchOperationDefinitions(boolean fetchOperationDefinitions) {
        this.fetchOperationDefinitions = fetchOperationDefinitions;
    }

    public void fetchProcessScans(boolean fetchProcessScans) {
        this.fetchProcessScans = fetchProcessScans;
    }

    public void fetchPackageTypes(boolean fetchPackageTypes) {
        this.fetchPackageTypes = fetchPackageTypes;
    }

    public void fetchSubCategories(boolean fetchSubCategories) {
        this.fetchSubCategories = fetchSubCategories;
    }

    public void fetchResources(boolean fetchResources) {
        this.fetchResources = fetchResources;
    }

    public void fetchProductVersions(boolean fetchProductVersions) {
        this.fetchProductVersions = fetchProductVersions;
    }

    public void addSortName(PageOrdering sortName) {
        addSortField("name");
        this.sortName = sortName;
    }

    public void addSortCategory(PageOrdering sortCategory) {
        addSortField("category");
        this.sortCategory = sortCategory;
    }

    public void addSortPluginName(PageOrdering sortPluginName) {
        addSortField("pluginName");
        this.sortPluginName = sortPluginName;
    }

}
