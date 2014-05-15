/*
 * RHQ Management Platform
 * Copyright (C) 2005-2011 Red Hat, Inc.
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
import java.util.HashSet;
import java.util.Set;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

import org.rhq.core.domain.resource.CreateDeletePolicy;
import org.rhq.core.domain.resource.ResourceCategory;
import org.rhq.core.domain.resource.ResourceCreationDataType;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.util.PageOrdering;

/**
 * @author Joseph Marques
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
@SuppressWarnings("unused")
public class ResourceTypeCriteria extends Criteria {
    private static final long serialVersionUID = 2L;

    private Integer filterParentId; // required overrides

    private String filterName;
    private String filterDescription;
    private ResourceCategory filterCategory;
    private ResourceCreationDataType filterCreationDataType;
    private CreateDeletePolicy filterCreateDeletePolicy;
    private Boolean filterSupportsManualAdd;
    private String filterPluginName; // needs overrides
    private Integer filterBundleTypeId; // needs overrides
    private Boolean filterDeleted = false; // by default, we don't want to fetch resource types that are marked deleted
    private Boolean filterIgnored = false; // by default, we don't want to fetch types the user requested to be ignored
    private Set<ResourceCategory> filterCategories; // needs overrides
    private Boolean filterParentResourceTypesEmpty; // needs overrides

    private boolean fetchChildResourceTypes;
    private boolean fetchParentResourceTypes;
    private boolean fetchPluginConfigurationDefinition;
    private boolean fetchResourceConfigurationDefinition;
    private boolean fetchMetricDefinitions;
    private boolean fetchEventDefinitions;
    private boolean fetchOperationDefinitions;
    private boolean fetchProcessScans;
    private boolean fetchPackageTypes;
    private boolean fetchProductVersions;
    private boolean fetchBundleType;
    private boolean fetchResources;
    private boolean fetchDriftDefinitionTemplates;
    private boolean fetchBundleConfiguration;

    private PageOrdering sortName;
    private PageOrdering sortCategory;
    private PageOrdering sortPluginName; // needs overrides

    public ResourceTypeCriteria() {
        filterOverrides.put("parentId", "" //
            + "id IN ( SELECT innerRt.id " //
            + "          FROM ResourceType innerRt " //
            + "          JOIN innerRt.parentResourceTypes innerParentRt " //
            + "         WHERE innerParentRt.id IN ( ? ) )");
        filterOverrides.put("pluginName", "plugin like ?");
        filterOverrides.put("bundleTypeId", "bundleType.id = ?");
        filterOverrides.put("categories", "category in ( ? )");
        filterOverrides.put("parentResourceTypesEmpty", "" //
            + "id IN ( SELECT innerRt.id FROM ResourceType innerRt " //
            + "        LEFT JOIN innerRt.parentResourceTypes innerParentRt" //
            + "        WHERE ( ? = true  AND innerRt.parentResourceTypes IS EMPTY ) " //
            + "           OR ( ? = false AND innerRt.parentResourceTypes IS NOT EMPTY ) )");

        sortOverrides.put("pluginName", "plugin");
    }

    @Override
    public Class<ResourceType> getPersistentClass() {
        return ResourceType.class;
    }

    public void addFilterParentId(Integer filterParentId) {
        this.filterParentId = filterParentId;
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

    public void addFilterBundleTypeId(Integer filterBundleTypeId) {
        this.filterBundleTypeId = filterBundleTypeId;
    }

    public void addFilterDeleted(boolean deleted) {
        this.filterDeleted = deleted;
    }

    public void addFilterDeleted(Boolean deleted) {
        this.filterDeleted = deleted;
    }

    public void addFilterIgnored(boolean ignored) {
        this.filterIgnored = ignored;
    }

    public void addFilterIgnored(Boolean ignored) {
        this.filterIgnored = ignored;
    }

    public void addFilterCategories(ResourceCategory... filterCategories) {
        this.filterCategories = new HashSet<ResourceCategory>(Arrays.asList(filterCategories));
    }

    /**
     * If set to true this is equivalent to asking only for top level resource types. In other words,
     * those types whose parent will be the platform.
     *
     * @param filterParentResourceTypesEmpty
     */
    public void addFilterParentResourceTypesEmpty(boolean filterParentResourceTypesEmpty) {
        this.filterParentResourceTypesEmpty = filterParentResourceTypesEmpty;
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

    public void fetchProductVersions(boolean fetchProductVersions) {
        this.fetchProductVersions = fetchProductVersions;
    }

    public void fetchBundleType(boolean fetchBundleType) {
        this.fetchBundleType = fetchBundleType;
    }

    public void fetchResources(boolean fetchResources) {
        this.fetchResources = fetchResources;
    }

    public void fetchDriftDefinitionTemplates(boolean fetchDriftDefinitionTemplates) {
        this.fetchDriftDefinitionTemplates = fetchDriftDefinitionTemplates;
    }

    public void fetchBundleConfiguration(boolean fetchBundleConfiguration) {
        this.fetchBundleConfiguration = fetchBundleConfiguration;
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

    /**
     * Deprecated due to a simpler but more powerful subcategory design.
     * Please see https://bugzilla.redhat.com/show_bug.cgi?id=1069545
     */
    @Deprecated
    public void fetchSubCategories(boolean notused) {
    }

    /**
     * Deprecated due to a simpler but more powerful subcategory design.
     * Please see https://bugzilla.redhat.com/show_bug.cgi?id=1069545
     */
    @Deprecated
    public void fetchSubCategory(boolean notused) {
    }
}
