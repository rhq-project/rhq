package org.rhq.core.domain.criteria;

import java.util.List;

import org.rhq.core.domain.drift.DriftCategory;
import org.rhq.core.domain.util.PageOrdering;

public interface DriftCriteria {

    void addFilterId(String filterId);

    String getFilterId();

    void addFilterCategories(DriftCategory... filterCategories);

    List<DriftCategory> getFilterCategories();

    void addFilterChangeSetId(String filterChangeSetId);

    String getFilterChangeSetId();

    void addFilterPath(String filterPath);

    String getFilterPath();

    void addFilterResourceIds(Integer... filterResourceIds);

    List<Integer> getFilterResourceIds();

    void addFilterStartTime(Long filterStartTime);

    Long getFilterStartTime();

    void addFilterEndTime(Long filterEndTime);

    Long getFilterEndTime();

    void fetchChangeSet(boolean fetchChangeSet);

    boolean isFetchChangeSet();

    void addSortCtime(PageOrdering sortCtime);

    PageOrdering getSortCtime();



}
