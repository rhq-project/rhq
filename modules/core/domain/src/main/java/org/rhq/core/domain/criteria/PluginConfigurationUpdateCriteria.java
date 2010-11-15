/*
 * RHQ Management Platform
 * Copyright (C) 2005-2010 Red Hat, Inc.
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

import org.rhq.core.domain.configuration.PluginConfigurationUpdate;
import org.rhq.core.domain.util.PageOrdering;

@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
@SuppressWarnings("unused")
public class PluginConfigurationUpdateCriteria extends AbstractResourceConfigurationUpdateCriteria {
    private static final long serialVersionUID = 1L;

    public static final String SORT_FIELD_RESOURCE_NAME = "name";
    public static final String SORT_FIELD_RESOURCE_ID = "resourceId";

    private String filterResourceTypeId; // requires overrides
    private String filterResourceTypeName; // requires overrides
    private List<Integer> filterResourceIds; // requires overrides
    private List<Integer> filterResourceGroupIds; // requires overrides

    private boolean fetchResource;
    private boolean fetchGroupConfigurationUpdate;

    private PageOrdering sortName; // requires sort override
    private PageOrdering sortResourceId; // requires sort override

    public PluginConfigurationUpdateCriteria() {

        filterOverrides.put("resourceTypeId", "resource.resourceType.id = ?");
        filterOverrides.put("resourceTypeName", "resource.resourceType.name like ?");
        filterOverrides.put("resourceIds", "resource.id IN ( ? )");
        filterOverrides.put("resourceGroupIds", "resource.id IN " //
            + "( SELECT res.id " //
            + "    FROM ResourceGroup rg " //
            + "    JOIN rg.explicitResources res " //
            + "   WHERE rg.id = ? )");

        sortOverrides.put(SORT_FIELD_RESOURCE_NAME, "resource.name");
        sortOverrides.put(SORT_FIELD_RESOURCE_ID, "resource.id");
    }

    @Override
    public Class<PluginConfigurationUpdate> getPersistentClass() {
        return PluginConfigurationUpdate.class;
    }

    public void addFilterResourceTypeId(String filterResourceTypeId) {
        this.filterResourceTypeId = filterResourceTypeId;
    }

    public void addFilterResourceTypeName(String filterResourceTypeName) {
        this.filterResourceTypeName = filterResourceTypeName;
    }

    public void addFilterResourceIds(Integer... filterResourceIds) {
        this.filterResourceIds = Arrays.asList(filterResourceIds);
    }

    public void addFilterResourceGroupIds(Integer... filterResourceGroupIds) {
        this.filterResourceGroupIds = Arrays.asList(filterResourceGroupIds);
    }

    public void fetchResource(boolean fetchResource) {
        this.fetchResource = fetchResource;
    }

    public void fetchGroupConfigurationUpdate(boolean fetchGroupConfigurationUpdate) {
        this.fetchGroupConfigurationUpdate = fetchGroupConfigurationUpdate;
    }

    public void addSortResourceName(PageOrdering sortName) {
        addSortField(SORT_FIELD_RESOURCE_NAME);
        this.sortName = sortName;
    }

    public void addSortResourceId(PageOrdering sortResourceId) {
        addSortField(SORT_FIELD_RESOURCE_ID);
        this.sortResourceId = sortResourceId;
    }
}
