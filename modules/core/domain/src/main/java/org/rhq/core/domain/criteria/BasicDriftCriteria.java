package org.rhq.core.domain.criteria;

import java.util.ArrayList;
import java.util.List;

import org.rhq.core.domain.drift.DriftCategory;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageOrdering;

import static org.rhq.core.domain.util.CriteriaUtils.getListIgnoringNulls;

public class BasicDriftCriteria implements DriftCriteria {

    private static final long serialVersionUID = 1L;

    private String filterId;

    private List<DriftCategory> filterCategories = new ArrayList<DriftCategory>();

    private String filterChangeSetId;

    private String filterPath;

    private List<Integer> filterResourceIds = new ArrayList<Integer>();

    private boolean fetchChangeSet;

    private Long filterStartTime;

    private Long filterEndTime;

    private PageControl pageControl;

    private PageOrdering sortCtime;

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
    public List<DriftCategory> getFilterCategories() {
        return filterCategories;
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
    public void addFilterPath(String filterPath) {
        this.filterPath = filterPath;
    }

    @Override
    public String getFilterPath() {
        return filterPath;
    }

    @Override
    public void addFilterResourceIds(Integer... filterResourceIds) {
        this.filterResourceIds = getListIgnoringNulls(filterResourceIds);
    }

    @Override
    public List<Integer> getFilterResourceIds() {
        return filterResourceIds;
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
}
