package org.rhq.core.domain.criteria;

import java.io.Serializable;

import org.rhq.core.domain.drift.DriftChangeSetCategory;
import org.rhq.core.domain.util.PageOrdering;

public interface DriftChangeSetCriteria extends Serializable {

    void addFilterId(String filterId);

    String getFilterId();

    void addFilterVersion(String filterVersion);

    String getFilterVersion();

    void addFilterResourceId(Integer filterResourceId);

    Integer getFilterResourceId();

    void addFilterCategory(DriftChangeSetCategory filterCategory);

    DriftChangeSetCategory getFilterCategory();

    void fetchDrifts(boolean fetchDrifts);

    boolean isFetchDrifts();

    void addSortVersion(PageOrdering sortVersion);

    PageOrdering getSortVersion();

}
