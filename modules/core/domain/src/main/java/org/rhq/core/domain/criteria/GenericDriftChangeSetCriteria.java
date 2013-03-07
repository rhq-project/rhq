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

import static org.rhq.core.domain.util.CriteriaUtils.getListIgnoringNulls;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.rhq.core.domain.drift.DriftCategory;
import org.rhq.core.domain.drift.DriftChangeSetCategory;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageOrdering;

/**
 * The generic implementation of DriftChangeSetCriteria makes no assumptions about the actual drift server
 * plugin that will service the relevant requests.  It is a simple impl of the interface and is
 * suitable for use by any component that can not assume a backend implmentation, like a GUI
 * client.  Server side implementations will use this to populate the plugin-specific implementation
 * of the interface.  
 * 
 * @author Jay Shaughnessy
 * @author John Sanda
 */
public class GenericDriftChangeSetCriteria implements Serializable, DriftChangeSetCriteria {

    private static final long serialVersionUID = 1L;

    private String filterId;

    private String filterVersion;

    private String filterStartVersion;

    private String filterEndVersion;

    private Long filterCreatedAfter;

    private Long filterCreatedBefore;

    private Integer filterResourceId;

    private Integer filterDriftDefId;

    private DriftChangeSetCategory filterCategory;

    private List<DriftCategory> filterDriftCategories;

    private String filterDriftDirectory;

    private String filterDriftPath;

    private boolean fetchDrifts;

    private PageOrdering sortId;

    private PageOrdering sortVersion;

    private PageControl pageControl;

    private boolean strict;

    @Override
    public void addFilterVersion(String filterVersion) {
        this.filterVersion = filterVersion;
    }

    @Override
    public String getFilterVersion() {
        return filterVersion;
    }

    @Override
    public void addFilterStartVersion(String filterStartVersion) {
        this.filterStartVersion = filterStartVersion;
    }

    @Override
    public String getFilterStartVersion() {
        return filterStartVersion;
    }

    @Override
    public void addFilterEndVersion(String filterEndVersion) {
        this.filterEndVersion = filterEndVersion;
    }

    @Override
    public String getFilterEndVersion() {
        return filterEndVersion;
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
        this.filterDriftDefId = filterDriftDefId;
    }

    @Override
    public Integer getFilterDriftDefinitionId() {
        return filterDriftDefId;
    }

    @Override
    public void addFilterCategory(DriftChangeSetCategory filterCategory) {
        this.filterCategory = filterCategory;
    }

    @Override
    public DriftChangeSetCategory getFilterCategory() {
        return filterCategory;
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
    public void addFilterDriftPath(String filterDriftPath) {
        this.filterDriftPath = filterDriftPath;
    }

    @Override
    public String getFilterDriftPath() {
        return this.filterDriftPath;
    }

    @Override
    public void addFilterDriftDirectory(String filterDriftDirectory) {
        // this requires strict equality
        if (null != filterDriftDirectory) {
            setStrict(true);
        }
        this.filterDriftDirectory = filterDriftDirectory;
    }

    @Override
    public String getFilterDriftDirectory() {
        return this.filterDriftDirectory;
    }

    @Override
    public void fetchDrifts(boolean fetchDrifts) {
        this.fetchDrifts = fetchDrifts;
    }

    @Override
    public boolean isFetchDrifts() {
        return fetchDrifts;
    }

    @Override
    public void addSortId(PageOrdering sortId) {
        this.sortId = sortId;
    }

    @Override
    public void addSortVersion(PageOrdering sortVersion) {
        this.sortVersion = sortVersion;
    }

    @Override
    public PageOrdering getSortVersion() {
        return sortVersion;
    }

    @Override
    public PageControl getPageControlOverrides() {
        return pageControl;
    }

    @Override
    public void setPageControl(PageControl pageControl) {
        this.pageControl = pageControl;
    }

    @Override
    public void setStrict(boolean strict) {
        this.strict = strict;
    }

    @Override
    public boolean isStrict() {
        return this.strict;
    }

    @Override
    public void setPaging(int pageNumber, int pageSize) {
        pageControl.setPageNumber(pageNumber);
        pageControl.setPageSize(pageSize);
    }

    @Override
    public List<String> getOrderingFieldNames() {
        List<String> result = new ArrayList<String>(2);
        if (null != sortId) {
            result.add("id");
        }
        if (null != sortVersion) {
            result.add("version");
        }
        return result;
    }

    @Override
    public void addFilterId(String filterId) {
        this.filterId = filterId;
    }

    @Override
    public String getFilterId() {
        return filterId;
    }

}
