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

import org.rhq.core.domain.resource.ResourceCategory;
import org.rhq.core.domain.resource.group.GroupCategory;
import org.rhq.core.domain.resource.group.ResourceGroup;
import org.rhq.core.domain.util.CriteriaUtils;
import org.rhq.core.domain.util.PageOrdering;

/**
 * @author Joseph Marques
 */
@XmlAccessorType(XmlAccessType.FIELD)
@SuppressWarnings("unused")
public class ResourceGroupCriteria extends TaggedCriteria {
    private static final long serialVersionUID = 2L;

    private String filterName;
    private Boolean filterRecursive;
    private Integer filterResourceTypeId; // requires overrides
    private String filterResourceTypeName; // requires overrides
    private Integer filterSubjectId; // requires overrides
    private Integer filterAutoGroupParentResourceId; // requires overrides    
    private String filterPluginName; // requires overrides
    private GroupCategory filterGroupCategory;
    private Long filterDownMemberCount; // required overrides
    private List<Integer> filterExplicitResourceIds; // requires overrides
    private List<Integer> filterImplicitResourceIds; // requires overrides
    private ResourceCategory filterExplicitResourceCategory; // requires overrides    
    private Integer filterExplicitResourceTypeId; // requires overrides    
    private String filterExplicitResourceTypeName; // requires overrides    
    private Integer filterGroupDefinitionId; // requires overrides
    private Boolean filterPrivate; /* if true, show only private groups for the calling user */
    private Boolean filterVisible = true; /* only show visible groups by default */
    private NonBindingOverrideFilter filterBundleTargetableOnly; // requires overrides - finds only those that have bundle config - that is, can be targeted for bundle deployment
    private String filterAcceptableTargetForBundleType; //only show groups to which given bundle type can be deployed to

    private boolean fetchExplicitResources;
    private boolean fetchImplicitResources;
    private boolean fetchOperationHistories;
    private boolean fetchConfigurationUpdates;
    private boolean fetchGroupDefinition;
    private boolean fetchResourceType;
    private boolean fetchRoles;

    private PageOrdering sortName;
    private PageOrdering sortResourceTypeName; // requires overrides
    private PageOrdering sortPluginName; // requires overrides

    public ResourceGroupCriteria() {
        filterOverrides.put("resourceTypeId", "resourceType.id = ?");
        filterOverrides.put("resourceTypeName", "resourceType.name like ?");
        filterOverrides.put("autoGroupParentResourceId", "autoGroupParentResource.id = ?");
        filterOverrides.put("subjectId", "subject.id = ?");
        filterOverrides.put("pluginName", "resourceType.plugin like ?");
        filterOverrides.put("downMemberCount", "" //
            + "id IN ( SELECT implicitGroup.id " //
            + "          FROM Resource res " //
            + "          JOIN res.implicitGroups implicitGroup " //
            + "         WHERE res.currentAvailability.availabilityType = 0 AND res.inventoryStatus = 'COMMITTED' " //
            + "      GROUP BY implicitGroup.id " // 
            + "         HAVING COUNT(res) >= ? )");
        filterOverrides.put("explicitResourceIds", "" //
            + "id IN ( SELECT explicitGroup.id " //
            + "          FROM Resource res " //
            + "          JOIN res.explicitGroups explicitGroup " //
            + "         WHERE res.id IN ( ? ) AND res.inventoryStatus = 'COMMITTED' )");
        filterOverrides.put("implicitResourceIds", "" //
            + "id IN ( SELECT implicitGroup.id " //
            + "          FROM Resource res " //
            + "          JOIN res.implicitGroups implicitGroup " //
            + "         WHERE res.id IN ( ? ) AND res.inventoryStatus = 'COMMITTED' )");
        filterOverrides.put("explicitResourceCategory", "" //
            + "NOT EXISTS " //
            + "(  SELECT res " //
            + "   FROM Resource res " //
            + "   JOIN res.explicitGroups explicitGroup " //
                    + "   WHERE resourcegroup.id = explicitGroup.id AND NOT res.resourceType.category = ? AND res.inventoryStatus = 'COMMITTED' )");
        filterOverrides.put("explicitResourceTypeId", "" //
            + "NOT EXISTS " //
            + "(  SELECT res " //
            + "   FROM Resource res " //
            + "   JOIN res.explicitGroups explicitGroup " //
                    + "   WHERE resourcegroup.id = explicitGroup.id AND NOT res.resourceType.id = ? AND res.inventoryStatus = 'COMMITTED' )");
        filterOverrides.put("explicitResourceTypeName", "" //
            + "NOT EXISTS " //
            + "(  SELECT res " //
            + "   FROM Resource res " //
            + "   JOIN res.explicitGroups explicitGroup " //
                    + "   WHERE resourcegroup.id = explicitGroup.id AND NOT res.resourceType.name = ? AND res.inventoryStatus = 'COMMITTED' )");
        filterOverrides.put("groupDefinitionId", "groupDefinition.id = ?");
        filterOverrides.put("bundleTargetableOnly", "resourceType.bundleConfiguration IS NOT NULL");

        // the double nesting is necessary so that we can capture the 2 conditions we're checking here using
        // a single IN check against the resourceType.id. The expression is concatenated to the table alias
        // during query generation and it might happen that the second part of the OR clause wouldn't correctly
        // match against the right table if it weren't nested.
        filterOverrides.put("acceptableTargetForBundleType", //
            "resourceType.id IN (SELECT rt.id FROM ResourceType rt" + //
            "                    WHERE rt.id IN (SELECT innerRt.id FROM ResourceType innerRt" + //
            "                                    JOIN innerRt.explicitlyTargetingBundleTypes bt" + //
            "                                    WHERE bt.name LIKE ?)" + //
            "                          OR" + //
            "                          rt.id IN (SELECT innerRt.id FROM ResourceType innerRt, BundleType bt" + //
            "                                    WHERE bt.explicitlyTargetedResourceTypes IS EMPTY" + //
            "                                    AND bt.name LIKE ?)" + //
            "                    )");
        sortOverrides.put("resourceTypeName", "resourceType.name");
        sortOverrides.put("pluginName", "resourceType.plugin");
    }

    @Override
    public Class<ResourceGroup> getPersistentClass() {
        return ResourceGroup.class;
    }

    /**
     * Only returns groups with at least this many downed implicit resource members
     */
    public void addFilterDownMemberCount(Long filterDownMemberCount) {
        this.filterDownMemberCount = filterDownMemberCount;
    }

    public void addFilterName(String filterName) {
        this.filterName = filterName;
    }

    public void addFilterRecursive(Boolean filterRecursive) {
        this.filterRecursive = filterRecursive;
    }

    public void addFilterResourceTypeId(Integer filterResourceTypeId) {
        this.filterResourceTypeId = filterResourceTypeId;
    }

    public void addFilterResourceTypeName(String filterResourceTypeName) {
        this.filterResourceTypeName = filterResourceTypeName;
    }

    /**
     * Requires MANAGE_INVENTORY.  Use addFilterPrivate(true) to filter on the caller's private groups.
     * @param filterSubjectId
     */
    public void addFilterSubjectId(Integer filterSubjectId) {
        this.filterSubjectId = filterSubjectId;
    }

    public void addFilterAutoGroupParentResourceId(Integer filterAutoGroupParentResourceId) {
        this.filterAutoGroupParentResourceId = filterAutoGroupParentResourceId;
    }

    public void addFilterPluginName(String filterPluginName) {
        this.filterPluginName = filterPluginName;
    }

    public void addFilterGroupCategory(GroupCategory filterGroupCategory) {
        this.filterGroupCategory = filterGroupCategory;
    }

    public void addFilterExplicitResourceIds(Integer... filterExplicitResourceIds) {
        this.filterExplicitResourceIds = CriteriaUtils.getListIgnoringNulls(filterExplicitResourceIds);
    }

    public void addFilterImplicitResourceIds(Integer... filterImplicitResourceIds) {
        this.filterImplicitResourceIds = CriteriaUtils.getListIgnoringNulls(filterImplicitResourceIds);
    }

    /** A somewhat special case filter that ensures that all explicit group members
     * are of the specified category (e.g. PLATFORM). Useful for filtering Mixed groups.
     * 
     * @param filterExplicitResourceCategory
     */
    public void addFilterExplicitResourceCategory(ResourceCategory filterExplicitResourceCategory) {
        this.filterExplicitResourceCategory = filterExplicitResourceCategory;
    }

    /** A somewhat special case filter that ensures that all explicit group members
     * are of the specified resource type (id). Useful for filtering Mixed groups.
     * 
     * @param filterExplicitResourceTypeId
     */
    public void addFilterExplicitResourceTypeId(Integer filterExplicitResourceTypeId) {
        this.filterExplicitResourceTypeId = filterExplicitResourceTypeId;
    }

    /** A somewhat special case filter that ensures that all explicit group members
     * are of the specified resource type (id). Useful for filtering Mixed groups.
     * 
     * @param filterExplicitResourceTypeName
     */
    public void addFilterExplicitResourceTypeName(String filterExplicitResourceTypeName) {
        this.filterExplicitResourceTypeName = filterExplicitResourceTypeName;
    }

    public void addFilterGroupDefinitionId(Integer filterGroupDefinitionId) {
        this.filterGroupDefinitionId = filterGroupDefinitionId;
    }

    public void addFilterPrivate(Boolean filterPrivate) {
        this.filterPrivate = filterPrivate;
    }

    public boolean isFilterPrivate() {
        return (Boolean.TRUE.equals(this.filterPrivate));
    }

    /**
     * @param filterVisible not null. A single fetch may be for visible or invisible groups, but not both.
     */
    public void addFilterVisible(Boolean filterVisible) {
        if (null == filterVisible) {
            throw new IllegalArgumentException("A single fetch may be for visible or invisible groups, but not both.");
        }
        this.filterVisible = filterVisible;
    }

    public boolean isFilterVisible() {
        return (Boolean.TRUE.equals(this.filterVisible));
    }

    /**
     * If true is passed in, only those groups that can be targeted for bundle deployments will
     * be fetched. By definition, this means no mixed groups are ever fetched and only
     * compatible groups with resource types that support bundle deployments are fetched.
     * Technically, what this means is only those compatible groups whose
     * resource types have non-null bundle configurations are fetched.
     * 
     * @param filterBundleTargetableOnly
     */
    public void addFilterBundleTargetableOnly(boolean filterBundleTargetableOnly) {
        this.filterBundleTargetableOnly = (filterBundleTargetableOnly ? NonBindingOverrideFilter.ON
            : NonBindingOverrideFilter.OFF);
    }

    /**
     * Selects only groups that can be target of deployment of bundles of given bundle type.
     * <p/>
     * Note that due to a limitation in query generation, it is recommended to set the criteria
     * to case sensitive ({@link #setCaseSensitive(boolean)}) when using this filter, otherwise
     * some results might be missed.
     *
     * @param bundleType the bundle type the groups should be deployable to.
     * @since 4.13
     */
    public void addFilterAcceptableTargetForBundleType(String bundleType) {
        this.filterAcceptableTargetForBundleType = bundleType;
    }

    public void fetchExplicitResources(boolean fetchExplicitResources) {
        this.fetchExplicitResources = fetchExplicitResources;
    }

    public void fetchImplicitResources(boolean fetchImplicitResources) {
        this.fetchImplicitResources = fetchImplicitResources;
    }

    public void fetchOperationHistories(boolean fetchOperationHistories) {
        this.fetchOperationHistories = fetchOperationHistories;
    }

    public void fetchConfigurationUpdates(boolean fetchConfigurationUpdates) {
        this.fetchConfigurationUpdates = fetchConfigurationUpdates;
    }

    public void fetchGroupDefinition(boolean fetchGroupDefinition) {
        this.fetchGroupDefinition = fetchGroupDefinition;
    }

    public void fetchResourceType(boolean fetchResourceType) {
        this.fetchResourceType = fetchResourceType;
    }

    /**
     * Requires MANAGE_SECURITY
     * @param fetchRoles
     */
    public void fetchRoles(boolean fetchRoles) {
        this.fetchRoles = fetchRoles;
    }

    public void addSortName(PageOrdering sortName) {
        addSortField("name");
        this.sortName = sortName;
    }

    public void addSortResourceTypeName(PageOrdering sortResourceTypeName) {
        addSortField("resourceTypeName");
        this.sortResourceTypeName = sortResourceTypeName;
    }

    public void addSortPluginName(PageOrdering sortPluginName) {
        addSortField("pluginName");
        this.sortPluginName = sortPluginName;
    }

    /** subclasses should override as necessary */
    @Override
    public boolean isSecurityManagerRequired() {
        return this.fetchRoles;
    }

    @Override
    public boolean isInventoryManagerRequired() {
        // presently only inventory managers can view/manage group definitions or see other user's private groups
        return this.filterGroupDefinitionId != null || this.filterSubjectId != null;
    }

}
