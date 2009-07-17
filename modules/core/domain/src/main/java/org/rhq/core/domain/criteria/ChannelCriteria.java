package org.rhq.core.domain.criteria;

import java.util.List;

import org.rhq.core.domain.util.PageOrdering;

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
