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
public class ChannelCriteria extends Criteria {
    private String filterId;
    private String filterName;
    private String filterDescription;
    private List<Integer> filterResourceIds; // needs overrides

    private boolean fetchResourceChannels;
    private boolean fetchContentSources;
    private boolean fetchPackageVersions;

    private PageOrdering sortName;

    public ChannelCriteria() {
        super();

        filterOverrides.put("resourceIds", "resourceChannels.resource.id IN ( ? )");
    }

    public void addFilterId(String filterId) {
        this.filterId = filterId;
    }

    public void addFilterName(String filterName) {
        this.filterName = filterName;
    }

    public void addFilterDescription(String filterDescription) {
        this.filterDescription = filterDescription;
    }

    public void addFilterResourceIds(List<Integer> filterResourceIds) {
        this.filterResourceIds = filterResourceIds;
    }

    public void fetchResourceChannels(boolean fetchResourceChannels) {
        this.fetchResourceChannels = fetchResourceChannels;
    }

    public void fetchContentSources(boolean fetchContentSources) {
        this.fetchContentSources = fetchContentSources;
    }

    public void fetchPackageVersions(boolean fetchPackageVersions) {
        this.fetchPackageVersions = fetchPackageVersions;
    }

    public void addSortName(PageOrdering sortName) {
        addSortField("name");
        this.sortName = sortName;
    }
}
