/*
 * RHQ Management Platform
 * Copyright (C) 2011 Red Hat, Inc.
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

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

import org.rhq.core.domain.drift.DriftCategory;
import org.rhq.core.domain.drift.DriftConfigurationDefinition.DriftHandlingMode;
import org.rhq.core.domain.drift.JPADrift;
import org.rhq.core.domain.util.CriteriaUtils;
import org.rhq.core.domain.util.PageOrdering;

/**
 * The JPA Drift Server plugin (the RHQ default) implementation of DriftCriteria.
 * 
 * @author Jay Shaughnessy
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class JPADriftCriteria extends Criteria implements DriftCriteria {
    private static final long serialVersionUID = 1L;

    private List<DriftCategory> filterCategories = new ArrayList<DriftCategory>();
    private Integer filterChangeSetId; // needs override
    private Integer filterChangeSetStartVersion; // needs override
    private Integer filterChangeSetEndVersion; // needs override 
    private Integer filterDriftDefinitionId; // needs override
    private List<DriftHandlingMode> filterDriftHandlingModes = new ArrayList<DriftHandlingMode>(); // needs override    
    private String filterPath;
    private String filterDirectory;
    private List<Integer> filterResourceIds = new ArrayList<Integer>();
    private Long filterStartTime; // requires overrides
    private Long filterEndTime; // requires overrides    

    private boolean fetchChangeSet;

    private PageOrdering sortCtime;

    public JPADriftCriteria() {
        this(null);
    }

    public JPADriftCriteria(DriftCriteria driftCriteria) {
        filterOverrides.put("changeSetId", "changeSet.id = ?");
        filterOverrides.put("changeSetStartVersion", "changeSet.version >= ?");
        filterOverrides.put("changeSetEndVersion", "changeSet.version <= ?");
        filterOverrides.put("categories", "category IN ( ? )");
        filterOverrides.put("driftDefinitionId", "changeSet.driftDefinition.id = ?");
        filterOverrides.put("driftHandlingModes", "changeSet.driftHandlingMode IN ( ? )");
        filterOverrides.put("resourceIds", "changeSet.resource.id IN ( ? )");
        filterOverrides.put("startTime", "ctime >= ?");
        filterOverrides.put("endTime", "ctime <= ?");

        // seed the JPA criteria with anything set in the provided criteria
        if (null != driftCriteria) {
            this.fetchChangeSet(driftCriteria.isFetchChangeSet());

            this.addFilterId(driftCriteria.getFilterId());
            this.addFilterCategories(driftCriteria.getFilterCategories());
            this.addFilterChangeSetId(driftCriteria.getFilterChangeSetId());
            this.addFilterChangeSetStartVersion(driftCriteria.getFilterChangeSetStartVersion());
            this.addFilterChangeSetEndVersion(driftCriteria.getFilterChangeSetEndVersion());
            this.addFilterDriftDefinitionId(driftCriteria.getFilterDriftDefinitionId());
            this.addFilterDriftHandlingModes(driftCriteria.getFilterDriftHandlingModes());
            this.addFilterEndTime(driftCriteria.getFilterEndTime());
            this.addFilterId(driftCriteria.getFilterId());
            this.addFilterPath(driftCriteria.getFilterPath());
            this.addFilterDirectory(driftCriteria.getFilterDirectory());
            this.addFilterResourceIds(driftCriteria.getFilterResourceIds());
            this.addFilterStartTime(driftCriteria.getFilterStartTime());

            this.addSortCtime(driftCriteria.getSortCtime());

            this.setStrict(driftCriteria.isStrict());
        }
    }

    @Override
    public Class<JPADrift> getPersistentClass() {
        return JPADrift.class;
    }

    @Override
    public void addFilterId(String filterId) {
        this.filterId = (null == filterId) ? null : Integer.parseInt(filterId);
    }

    @Override
    public String getFilterId() {
        return (null == filterId) ? null : filterId.toString();
    }

    @Override
    public void addFilterCategories(DriftCategory... filterCategories) {
        this.filterCategories = CriteriaUtils.getListIgnoringNulls(filterCategories);
    }

    @Override
    public DriftCategory[] getFilterCategories() {
        return filterCategories.toArray(new DriftCategory[filterCategories.size()]);
    }

    @Override
    public void addFilterChangeSetId(String filterChangeSetId) {
        if (filterChangeSetId != null) {
            this.filterChangeSetId = Integer.parseInt(filterChangeSetId);
        }
    }

    @Override
    public String getFilterChangeSetId() {
        return filterChangeSetId == null ? null : filterChangeSetId.toString();
    }

    @Override
    public Integer getFilterChangeSetStartVersion() {
        return filterChangeSetStartVersion;
    }

    @Override
    public void addFilterChangeSetStartVersion(Integer filterChangeSetStartVersion) {
        this.filterChangeSetStartVersion = filterChangeSetStartVersion;
    }

    @Override
    public Integer getFilterChangeSetEndVersion() {
        return filterChangeSetEndVersion;
    }

    @Override
    public void addFilterChangeSetEndVersion(Integer filterChangeSetEndVersion) {
        this.filterChangeSetEndVersion = filterChangeSetEndVersion;
    }

    @Override
    public void addFilterDriftDefinitionId(Integer filterDriftDefinitionId) {
        this.filterDriftDefinitionId = filterDriftDefinitionId;
    }

    @Override
    public Integer getFilterDriftDefinitionId() {
        return this.filterDriftDefinitionId;
    }

    @Override
    public void addFilterDriftHandlingModes(DriftHandlingMode... filterDriftHandlingModes) {
        this.filterDriftHandlingModes = CriteriaUtils.getListIgnoringNulls(filterDriftHandlingModes);
    }

    @Override
    public DriftHandlingMode[] getFilterDriftHandlingModes() {
        return this.filterDriftHandlingModes.toArray(new DriftHandlingMode[this.filterDriftHandlingModes.size()]);
    }

    @Override
    public void addFilterPath(String filterPath) {
        this.filterPath = filterPath;
    }

    @Override
    public String getFilterPath() {
        return filterPath;
    }

    @Override
    public void addFilterDirectory(String filterDirectory) {
        this.filterDirectory = filterDirectory;
    }

    @Override
    public String getFilterDirectory() {
        return filterDirectory;
    }

    @Override
    public void addFilterResourceIds(Integer... filterResourceIds) {
        this.filterResourceIds = CriteriaUtils.getListIgnoringNulls(filterResourceIds);
    }

    @Override
    public Integer[] getFilterResourceIds() {
        return filterResourceIds.toArray(new Integer[this.filterResourceIds.size()]);
    }

    @Override
    public void addFilterStartTime(Long filterStartTime) {
        this.filterStartTime = filterStartTime;
    }

    @Override
    public Long getFilterStartTime() {
        return filterStartTime;
    }

    @Override
    public void addFilterEndTime(Long filterEndTime) {
        this.filterEndTime = filterEndTime;
    }

    @Override
    public Long getFilterEndTime() {
        return filterEndTime;
    }

    @Override
    public void fetchChangeSet(boolean fetchChangeSet) {
        this.fetchChangeSet = fetchChangeSet;
    }

    @Override
    public boolean isFetchChangeSet() {
        return fetchChangeSet;
    }

    @Override
    public void addSortCtime(PageOrdering sortCtime) {
        addSortField("ctime");
        this.sortCtime = sortCtime;
    }

    @Override
    public PageOrdering getSortCtime() {
        return sortCtime;
    }

    @Override
    public void addFilterIds(Integer... filterIds) {
        throw new UnsupportedOperationException("The filter is not supported by DriftCriteria.");
    }

    @Override
    public void addSortId(PageOrdering pageOrdering) {
        throw new UnsupportedOperationException("The sort is not supported by DriftCriteria.");
    }

}
