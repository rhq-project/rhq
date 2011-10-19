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

import static org.rhq.core.domain.util.CriteriaUtils.getListIgnoringNulls;

import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

import org.rhq.core.domain.drift.DriftCategory;
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
    private Integer filterDriftDefinitionId; // needs override
    private Integer filterVersion;
    private Integer filterStartVersion;
    private Integer filterEndVersion;
    private Long filterCreatedAfter;
    private Long filterCreatedBefore;
    private DriftChangeSetCategory filterCategory;
    private List<DriftCategory> filterDriftCategories; // needs override      
    private String filterDriftDirectory; // needs override
    private String filterDriftPath; // needs override    
    private Boolean fetchDrifts = false;
    private Boolean fetchDriftDefinition = false;
    private Boolean fetchInitialDriftSet = false;

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
        filterOverrides.put("driftDefinitionId", "driftDefinition.id = ?");
        filterOverrides.put("driftCategories", "" //
            + "id IN ( SELECT innerChangeSet.id " //
            + "          FROM JPADriftChangeSet innerChangeSet " //
            + "          JOIN innerChangeSet.drifts drift " //
            + "         WHERE drift.category IN ( ? ) )");
        filterOverrides.put("driftDirectory", "" //
            + "id IN ( SELECT innerChangeSet.id " //
            + "          FROM JPADriftChangeSet innerChangeSet " //
            + "          JOIN innerChangeSet.drifts drift " //
            + "         WHERE drift.directory = ? )"); // note, this uses = on purpose, it is always strict equality 
        filterOverrides.put("driftPath", "" //
            + "id IN ( SELECT innerChangeSet.id " //
            + "          FROM JPADriftChangeSet innerChangeSet " //
            + "          JOIN innerChangeSet.drifts drift " //
            + "         WHERE drift.path like ? )"); // note, this uses 'like' on purpose, it is always substring

        if (null != changeSetCriteria) {
            this.addFilterCategory(changeSetCriteria.getFilterCategory());
            this.addFilterCreatedAfter(changeSetCriteria.getFilterCreatedAfter());
            this.addFilterCreatedBefore(changeSetCriteria.getFilterCreatedBefore());
            this.addFilterDriftDefinitionId(changeSetCriteria.getFilterDriftDefinitionId());
            this.addFilterEndVersion(changeSetCriteria.getFilterEndVersion());
            this.addFilterId(changeSetCriteria.getFilterId());
            this.addFilterResourceId(changeSetCriteria.getFilterResourceId());
            this.addFilterStartVersion(changeSetCriteria.getFilterStartVersion());
            this.addFilterVersion(changeSetCriteria.getFilterVersion());
            this.addFilterDriftCategories(changeSetCriteria.getFilterDriftCategories());
            this.addFilterDriftDirectory(changeSetCriteria.getFilterDriftDirectory());
            this.addFilterDriftPath(changeSetCriteria.getFilterDriftPath());

            this.addSortVersion(changeSetCriteria.getSortVersion());

            this.fetchDrifts(changeSetCriteria.isFetchDrifts());

            this.setStrict(changeSetCriteria.isStrict());
        }
    }

    @Override
    public Class<JPADriftChangeSet> getPersistentClass() {
        return JPADriftChangeSet.class;
    }

    @Override
    public void addFilterId(String filterId) {
        if (filterId != null) {
            this.filterId = Integer.parseInt(filterId);
        }
    }

    @Override
    public String getFilterId() {
        return filterId == null ? null : filterId.toString();
    }

    public void addFilterVersion(Integer filterVersion) {
        this.filterVersion = filterVersion;
    }

    @Override
    public void addFilterVersion(String filterVersion) {
        if (filterVersion != null) {
            this.filterVersion = Integer.parseInt(filterVersion);
        }
    }

    @Override
    public String getFilterVersion() {
        return filterVersion == null ? null : filterVersion.toString();
    }

    public void addFilterStartVersion(Integer filterStartVersion) {
        this.filterStartVersion = filterStartVersion;
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

    public void addFilterEndVersion(Integer filterEndVersion) {
        this.filterEndVersion = filterEndVersion;
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

    @Override
    public void addFilterResourceId(Integer filterResourceId) {
        this.filterResourceId = filterResourceId;
    }

    @Override
    public Integer getFilterResourceId() {
        return filterResourceId;
    }

    @Override
    public void addFilterDriftDefinitionId(Integer filterDriftDefId) {
        this.filterDriftDefinitionId = filterDriftDefId;
    }

    @Override
    public Integer getFilterDriftDefinitionId() {
        return filterDriftDefinitionId;
    }

    @Override
    public void addFilterCategory(DriftChangeSetCategory filterCategory) {
        this.filterCategory = filterCategory;
    }

    @Override
    public DriftChangeSetCategory getFilterCategory() {
        return filterCategory;
    }

    public void addFilterDriftCategories(List<DriftCategory> filterDriftCategories) {
        this.filterDriftCategories = filterDriftCategories;
    }

    @Override
    public void addFilterDriftCategories(DriftCategory... filterDriftCategories) {
        this.filterDriftCategories = getListIgnoringNulls(filterDriftCategories);
    }

    @Override
    public List<DriftCategory> getFilterDriftCategories() {
        return filterDriftCategories;
    }

    @Override
    public void addFilterDriftDirectory(String filterDriftDirectory) {
        this.filterDriftDirectory = filterDriftDirectory;
    }

    @Override
    public String getFilterDriftDirectory() {
        return this.filterDriftDirectory;
    }

    @Override
    public void addFilterDriftPath(String filterDriftPath) {
        this.filterDriftPath = filterDriftPath;
    }

    @Override
    public String getFilterDriftPath() {
        return this.filterDriftPath;
    }

    @Override
    public void fetchDrifts(boolean fetchDrifts) {
        this.fetchDrifts = fetchDrifts;
    }

    @Override
    public boolean isFetchDrifts() {
        return fetchDrifts;
    }

    public void fetchDriftDefinition(boolean fetchDriftDefinition) {
        this.fetchDriftDefinition = fetchDriftDefinition;
    }

    public boolean isFetchInitialDriftSet() {
        return fetchInitialDriftSet;
    }

    public void fetchInitialDriftSet(boolean fetchInitialDriftSet) {
        this.fetchInitialDriftSet = fetchInitialDriftSet;
    }

    @Override
    public void addSortVersion(PageOrdering sortVersion) {
        addSortField("version");
        this.sortVersion = sortVersion;
    }

    @Override
    public PageOrdering getSortVersion() {
        return sortVersion;
    }
}
