package org.rhq.core.domain.criteria;

import java.io.Serializable;

import org.rhq.core.domain.drift.DriftChangeSetCategory;
import org.rhq.core.domain.util.PageOrdering;

public interface DriftChangeSetCriteria extends Serializable {

    void addFilterId(String filterId);

    String getFilterId();

    void addFilterVersion(String filterVersion);

    String getFilterVersion();

    void addFilterStartVersion(String filterStartVersion);

    String getFilterStartVersion();

    void addFilterEndVersion(String filterEndVersion);

    String getFilterEndVersion();

    void addFilterCreatedAfter(Long filterCreatedAfter);

    Long getFilterCreatedAfter();

    void addFilterCreatedBefore(Long filterCreatedBefore);

    Long getFilterCreatedBefore();

    void addFilterResourceId(Integer filterResourceId);

    Integer getFilterResourceId();

    void addFilterCategory(DriftChangeSetCategory filterCategory);

    DriftChangeSetCategory getFilterCategory();

    void fetchDrifts(boolean fetchDrifts);

    boolean isFetchDrifts();

    void addSortVersion(PageOrdering sortVersion);

    PageOrdering getSortVersion();

}
