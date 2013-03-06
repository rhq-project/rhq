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
import org.rhq.core.domain.drift.DriftConfigurationDefinition.DriftHandlingMode;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageOrdering;

/**
 * The generic implementation of DriftCriteria makes no assumptions about the actual drift server
 * plugin that will service the relevant requests.  It is a simple impl of the interface and is
 * suitable for use by any component that can not assume a backend implmentation, like a GUI
 * client.  Server side implementations will use this to populate the plugin-specific implementation
 * of the interface.  
 * 
 * @author Jay Shaughnessy
 * @author John Sanda
 */
public class GenericDriftCriteria implements Serializable, DriftCriteria {

    private static final long serialVersionUID = 1L;

    private String filterId;

    private List<DriftCategory> filterCategories = new ArrayList<DriftCategory>();

    private String filterChangeSetId;

    private Integer filterChangeSetStartVersion;

    private Integer filterChangeSetEndVersion;

    private Integer filterDriftDefinitionId;

    private List<DriftHandlingMode> filterDriftHandlingModes = new ArrayList<DriftHandlingMode>();

    private String filterPath;

    private String filterDirectory;

    private List<Integer> filterResourceIds = new ArrayList<Integer>();

    private boolean fetchChangeSet;

    private Long filterStartTime;

    private Long filterEndTime;

    private PageControl pageControl;

    private PageOrdering sortId;

    private PageOrdering sortCtime;

    private boolean strict;

    @Override
    public void addFilterId(String filterId) {
        this.filterId = filterId;
    }

    @Override
    public String getFilterId() {
        return filterId;
    }

    @Override
    public void addFilterCategories(DriftCategory... filterCategories) {
        this.filterCategories = getListIgnoringNulls(filterCategories);
    }

    @Override
    public DriftCategory[] getFilterCategories() {
        return filterCategories.toArray(new DriftCategory[filterCategories.size()]);
    }

    @Override
    public void addFilterChangeSetId(String filterChangeSetId) {
        this.filterChangeSetId = filterChangeSetId;
    }

    @Override
    public String getFilterChangeSetId() {
        return filterChangeSetId;
    }

    @Override
    public void addFilterChangeSetStartVersion(Integer filterChangeSetStartVersion) {
        this.filterChangeSetStartVersion = filterChangeSetStartVersion;
    }

    @Override
    public Integer getFilterChangeSetStartVersion() {
        return filterChangeSetStartVersion;
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
        this.filterDriftHandlingModes = getListIgnoringNulls(filterDriftHandlingModes);
    }

    @Override
    public DriftHandlingMode[] getFilterDriftHandlingModes() {
        return filterDriftHandlingModes.toArray(new DriftHandlingMode[filterDriftHandlingModes.size()]);
    }

    @Override
    public Integer getFilterChangeSetEndVersion() {
        return filterChangeSetEndVersion;
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
        this.filterResourceIds = getListIgnoringNulls(filterResourceIds);
    }

    @Override
    public Integer[] getFilterResourceIds() {
        return filterResourceIds.toArray(new Integer[filterResourceIds.size()]);
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
    public void addSortId(PageOrdering sortId) {
        this.sortId = sortId;
    }

    @Override
    public void addSortCtime(PageOrdering sortCtime) {
        this.sortCtime = sortCtime;
    }

    @Override
    public PageOrdering getSortCtime() {
        return sortCtime;
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
        if (null != sortCtime) {
            result.add("ctime");
        }
        return result;
    }

}
