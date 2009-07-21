package org.rhq.core.domain.criteria;

import java.util.List;

public class GroupOperationHistoryCriteria extends OperationHistoryCriteria {
    private List<Integer> filterResourceGroupIds; // requires override

    public GroupOperationHistoryCriteria() {
        super();

        filterOverrides.put("resourceGroupIds", "group.id IN ( ? )");
    }

    public void addFilterResourceGroupIds(List<Integer> filterResourceGroupIds) {
        this.filterResourceGroupIds = filterResourceGroupIds;
    }
}
