package org.rhq.core.domain.criteria;

import org.rhq.core.domain.drift.DriftChangeSetCategory;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageOrdering;

public class BasicDriftChangeSetCriteria implements DriftChangeSetCriteria {

    private static final long serialVersionUID = 1L;

    private String filterId;

    private String filterVersion;

    private String filterStartVersion;

    private String filterEndVersion;

    private Long filterCreatedAfter;

    private Long filterCreatedBefore;

    private Integer filterResourceId;

    private DriftChangeSetCategory filterCategory;

    private boolean fetchDrifts;

    private PageOrdering sortVersion;

    private PageControl pageControl;

    @Override
    public void addFilterId(String filterId) {
        this.filterId = filterId;
    }

    @Override
    public String getFilterId() {
        return filterId;
    }

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
    public void addFilterCategory(DriftChangeSetCategory filterCategory) {
        this.filterCategory = filterCategory;
    }

    @Override
    public DriftChangeSetCategory getFilterCategory() {
        return filterCategory;
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
}
