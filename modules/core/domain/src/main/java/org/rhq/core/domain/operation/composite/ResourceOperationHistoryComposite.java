package org.rhq.core.domain.operation.composite;

import org.rhq.core.domain.operation.OperationHistory;

public class ResourceOperationHistoryComposite {

    private final OperationHistory history;
    private final int parentResourceId;
    private final String parentResourceName;

    public ResourceOperationHistoryComposite(OperationHistory history, int parentResourceId, String parentResourceName) {
        super();
        this.history = history;
        this.parentResourceId = parentResourceId;
        this.parentResourceName = parentResourceName;
    }

    public OperationHistory getHistory() {
        return history;
    }

    public int getParentResourceId() {
        return parentResourceId;
    }

    public String getParentResourceName() {
        return parentResourceName;
    }

}
