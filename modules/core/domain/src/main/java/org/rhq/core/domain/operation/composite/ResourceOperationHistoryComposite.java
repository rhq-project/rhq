package org.rhq.core.domain.operation.composite;

import org.rhq.core.domain.operation.ResourceOperationHistory;

public class ResourceOperationHistoryComposite {

    private final ResourceOperationHistory history;
    private final Integer parentResourceId; // use object wrapper, because parent can be null
    private final String parentResourceName;

    public ResourceOperationHistoryComposite(ResourceOperationHistory history, Integer parentResourceId,
        String parentResourceName) {
        super();
        this.history = history;
        this.parentResourceId = parentResourceId;
        this.parentResourceName = parentResourceName;
    }

    public ResourceOperationHistory getHistory() {
        return history;
    }

    public int getParentResourceId() {
        return parentResourceId;
    }

    public String getParentResourceName() {
        return parentResourceName;
    }

}
