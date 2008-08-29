package org.rhq.core.domain.cluster.composite;

public class FailoverListDetailsComposite {

    public final int ordinal;
    public final int serverId;
    public final long assignedAgentCount;

    public FailoverListDetailsComposite(int ordinal, int serverId, long assignedAgentCount) {
        this.ordinal = ordinal;
        this.serverId = serverId;
        this.assignedAgentCount = assignedAgentCount;
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder("[ AssignedLoad(");
        result.append(" ordinal=");
        result.append(ordinal);
        result.append(" serverId=");
        result.append(serverId);
        result.append(" load=");
        result.append(assignedAgentCount);
        result.append(" )");

        return result.toString();
    }
}
