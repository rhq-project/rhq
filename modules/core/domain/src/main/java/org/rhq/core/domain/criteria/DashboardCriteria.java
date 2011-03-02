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

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;

import org.rhq.core.domain.dashboard.Dashboard;
import org.rhq.core.domain.dashboard.DashboardCategory;
import org.rhq.core.domain.util.PageOrdering;

/**
 * The criteria for fetching {@link Dashboard}s.
 *
 * @author Jay Shaughnessy
 */
@XmlAccessorType(XmlAccessType.FIELD)
@SuppressWarnings("unused")
public class DashboardCriteria extends Criteria {
    private static final long serialVersionUID = 1L;

    private Integer filterId;
    private DashboardCategory filterCategory = DashboardCategory.INVENTORY;
    private Integer filterGroupId; // needs overrides
    private String filterName;
    private Integer filterOwnerId; // needs overrides
    private Integer filterResourceId; // needs overrides
    private Boolean filterShared;

    private boolean fetchConfiguration;
    private boolean fetchOwner;

    private PageOrdering sortName;

    /**
     * Note: Default Criteria Settings:<br/>
     * ownerId  = sessionSubject (i.e. the caller)<br/>
     * category = INVENTORY<br/>
     */
    public DashboardCriteria() {
        filterOverrides.put("groupId", "group.id = ?");
        filterOverrides.put("ownerId", "owner.id = ?");
        filterOverrides.put("resourceId", "resource.id = ?");
    }

    @Override
    public Class<Dashboard> getPersistentClass() {
        return Dashboard.class;
    }

    public void addFilterId(Integer filterId) {
        this.filterId = filterId;
    }

    /**
     * If not set explicitly this defaults to {@link DashboardCategory.INVENTORY}.
     * @param category
     */
    public void addFilterCategory(DashboardCategory category) {
        this.filterCategory = category;
    }

    public DashboardCategory getFilterCategory() {
        return this.filterCategory;
    }

    public void addFilterGroupId(Integer filterGroupId) {
        this.filterGroupId = filterGroupId;
    }

    public void addFilterName(String filterName) {
        this.filterName = filterName;
    }

    /**
     * Requires MANAGE_INVENTORY to set.  When not set defaults to dashboards owned by the current user.
     * @param filterOwnerId set to 0 for dashboards owned by anyone
     */
    public void addFilterOwnerId(Integer filterOwnerId) {
        this.filterOwnerId = filterOwnerId;
    }

    public Integer getFilterOwnerId() {
        return filterOwnerId;
    }

    public void addFilterResourceId(Integer filterResourceId) {
        this.filterResourceId = filterResourceId;
    }

    public void addFilterShared(Boolean filterShared) {
        this.filterShared = filterShared;
    }

    public void addSortName(PageOrdering sortName) {
        addSortField("name");
        this.sortName = sortName;
    }

    @Override
    public boolean isInventoryManagerRequired() {
        // presently only inventory managers can view/manage other people's dashboards
        return this.filterOwnerId != null;
    }

}
