package org.rhq.core.domain.criteria;

import java.util.List;

public class ResourceOperationHistoryCriteria extends OperationHistoryCriteria {
    private List<Integer> filterResourceIds; // requires override

    public ResourceOperationHistoryCriteria() {
        super();

        filterOverrides.put("resourceIds", "resource.id IN ( ? )");
    }

    public void addFilterResourceIds(List<Integer> filterResourceIds) {
        this.filterResourceIds = filterResourceIds;
    }
}
