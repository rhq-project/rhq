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

import org.rhq.core.domain.util.PageOrdering;

/**
 * @author Joseph Marques
 */
public class OperationDefinitionCriteria extends Criteria {
    private Integer filterId;
    private String filterName;
    private String filterDisplayName;
    private String filterDescription;
    private Integer filterTimeout;
    private Integer filterResourceTypeId; // requires overrides
    private Integer filterResourceTypeName; // requires overrides
    private Integer filterPluginName; // requires overrides
    private List<Integer> filterResourceIds; // requires overrides
    private List<Integer> filterResourceGroupIds; // requires overrides

    private boolean fetchParametersConfigurationDefinition;
    private boolean fetchResultsConfigurationDefinition;

    private PageOrdering sortName;

    public OperationDefinitionCriteria() {
        super();

        filterOverrides.put("resourceTypeId", "resourceType.id = ?");
        filterOverrides.put("resourceTypeName", "resourceType.name like ?");
        filterOverrides.put("pluginName", "resourceType.plugin like ?");
        filterOverrides.put("resourceIds", "resourceType.id IN " //
            + " ( SELECT type.id " //
            + "     FROM Resource res " //
            + "     JOIN res.resourceType type " //
            + "    WHERE res.id = ? ) ");
        filterOverrides.put("resourceGroupIds", "resourceType.id IN " //
            + " ( SELECT type.id " //
            + "     FROM ResourceGroup group " //
            + "     JOIN group.resourceType type " //
            + "    WHERE group.id = ? ) ");
    }

    public void addFilterId(Integer filterId) {
        this.filterId = filterId;
    }

    public void addFilterName(String filterName) {
        this.filterName = filterName;
    }

    public void addFilterDisplayName(String filterDisplayName) {
        this.filterDisplayName = filterDisplayName;
    }

    public void addFilterDescription(String filterDescription) {
        this.filterDescription = filterDescription;
    }

    public void addFilterTimeout(Integer filterTimeout) {
        this.filterTimeout = filterTimeout;
    }

    public void addFilterResourceTypeId(Integer filterResourceTypeId) {
        this.filterResourceTypeId = filterResourceTypeId;
    }

    public void addFilterResourceTypeName(Integer filterResourceTypeName) {
        this.filterResourceTypeName = filterResourceTypeName;
    }

    public void addFilterPluginName(Integer filterPluginName) {
        this.filterPluginName = filterPluginName;
    }

    public void addFilterResourceIds(List<Integer> filterResourceIds) {
        this.filterResourceIds = filterResourceIds;
    }

    public void addFilterResourceGroupIds(List<Integer> filterResourceGroupIds) {
        this.filterResourceGroupIds = filterResourceGroupIds;
    }

    public void fetchParametersConfigurationDefinition(boolean fetchParametersConfigurationDefinition) {
        this.fetchParametersConfigurationDefinition = fetchParametersConfigurationDefinition;
    }

    public void fetchResultsConfigurationDefinition(boolean fetchResultsConfigurationDefinition) {
        this.fetchResultsConfigurationDefinition = fetchResultsConfigurationDefinition;
    }

    public void addSortName(PageOrdering sortName) {
        addSortField("name");
        this.sortName = sortName;
    }
}
