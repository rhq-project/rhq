/*
 * RHQ Management Platform
 * Copyright (C) 2005-2009 Red Hat, Inc.
 * All rights reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */

package org.rhq.core.domain.criteria;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

import org.rhq.core.domain.drift.DriftChangeSetCategory;
import org.rhq.core.domain.drift.JPADriftChangeSet;
import org.rhq.core.domain.util.PageOrdering;

/**
 * The JPA Drift Server plugin (the RHQ default) implementation of DriftChangeSetCriteria.
 * 
 * @author Jay Shaughnessy
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
@SuppressWarnings("unused")
public class JPADriftChangeSetCriteria extends Criteria implements DriftChangeSetCriteria {
    private static final long serialVersionUID = 1L;

    private Integer filterId;
    private Integer filterInitial; // needs override
    private Integer filterResourceId; // needs override
    private Integer filterDriftConfigurationId;
    private Integer filterVersion;
    private Integer filterStartVersion;
    private Integer filterEndVersion;
    private Long filterCreatedAfter;
    private Long filterCreatedBefore;
    private DriftChangeSetCategory filterCategory;
    private boolean fetchDrifts;

    private PageOrdering sortVersion;

    public JPADriftChangeSetCriteria() {
        this(null);
    }

    public JPADriftChangeSetCriteria(DriftChangeSetCriteria changeSetCriteria) {
        filterOverrides.put("initial", "version = 0");
        filterOverrides.put("resourceId", "resource.id = ?");
        filterOverrides.put("startVersion", "version >= ?");
        filterOverrides.put("endVersion", "version <= ?");
        filterOverrides.put("createdAfter", "ctime >= ?");
        filterOverrides.put("createdBefore", "ctime <= ?");

        if (null != changeSetCriteria) {
            this.addFilterId(changeSetCriteria.getFilterId());
            this.addFilterResourceId(changeSetCriteria.getFilterResourceId());
            this.addFilterCategory(changeSetCriteria.getFilterCategory());
            this.addFilterCreatedAfter(changeSetCriteria.getFilterCreatedAfter());
            this.addFilterCreatedBefore(changeSetCriteria.getFilterCreatedBefore());
            this.addFilterEndVersion(changeSetCriteria.getFilterEndVersion());
            this.addFilterStartVersion(changeSetCriteria.getFilterStartVersion());
            this.addFilterVersion(changeSetCriteria.getFilterVersion());

            this.addSortVersion(changeSetCriteria.getSortVersion());

            this.fetchDrifts(changeSetCriteria.isFetchDrifts());
        }
    }

    @Override
    public Class<JPADriftChangeSet> getPersistentClass() {
        return JPADriftChangeSet.class;
    }

    public void addFilterId(String filterId) {
        if (filterId != null) {
            this.filterId = Integer.parseInt(filterId);
        }
    }

    @Override
    public String getFilterId() {
        return filterId == null ? null : filterId.toString();
    }

    public void addFilterVersion(String filterVersion) {
        if (filterVersion != null) {
            this.filterVersion = Integer.parseInt(filterVersion);
        }
    }

    @Override
    public String getFilterVersion() {
        return filterVersion == null ? null : filterVersion.toString();
    }

    @Override
    public void addFilterStartVersion(String filterStartVersion) {
        if (filterStartVersion != null) {
            this.filterStartVersion = Integer.parseInt(filterStartVersion);
        }
    }

    @Override
    public String getFilterStartVersion() {
        return filterStartVersion == null ? null : filterStartVersion.toString();
    }

    @Override
    public void addFilterEndVersion(String filterEndVersion) {
        if (filterEndVersion != null) {
            this.filterEndVersion = Integer.parseInt(filterEndVersion);
        }
    }

    @Override
    public String getFilterEndVersion() {
        return filterEndVersion == null ? null : filterEndVersion.toString();
    }

    @Override
    public void addFilterCreatedAfter(Long filterCreatedAfter) {
        this.filterCreatedAfter = filterCreatedAfter;
    }

    @Override
    public Long getFilterCreatedAfter() {
        return filterCreatedAfter;
    }

    @Override
    public void addFilterCreatedBefore(Long filterCreatedBefore) {
        this.filterCreatedBefore = filterCreatedBefore;
    }

    @Override
    public Long getFilterCreatedBefore() {
        return filterCreatedBefore;
    }

    public void addFilterResourceId(Integer filterResourceId) {
        this.filterResourceId = filterResourceId;
    }

    @Override
    public Integer getFilterResourceId() {
        return filterResourceId;
    }

    @Override
    public void addFilterDriftConfigurationId(Integer filterDriftConfigId) {
        this.filterDriftConfigurationId = filterDriftConfigId;
    }

    @Override
    public Integer getFilterDriftConfigurationId() {
        return filterDriftConfigurationId;
    }

    public void addFilterCategory(DriftChangeSetCategory filterCategory) {
        this.filterCategory = filterCategory;
    }

    @Override
    public DriftChangeSetCategory getFilterCategory() {
        return filterCategory;
    }

    public void fetchDrifts(boolean fetchDrifts) {
        this.fetchDrifts = fetchDrifts;
    }

    @Override
    public boolean isFetchDrifts() {
        return fetchDrifts;
    }

    public void addSortVersion(PageOrdering sortVersion) {
        addSortField("version");
        this.sortVersion = sortVersion;
    }

    @Override
    public PageOrdering getSortVersion() {
        return sortVersion;
    }
}
