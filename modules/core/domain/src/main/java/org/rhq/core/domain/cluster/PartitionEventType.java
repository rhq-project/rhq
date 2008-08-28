package org.rhq.core.domain.cluster;

public enum PartitionEventType {

    AGENT_REGISTRATION(false), //
    AGENT_JOIN(false), //
    AGENT_LOAD_CHANGE(false), //
    AGENT_LEAVE(false), //

    SERVER_JOIN(true), //
    SERVER_LEAVE(true), //
    SERVER_CORE_COUNT_CHANGE(true), //

    AFFINITY_GROUP_CHANGE(true), //

    USER_INITIATED_PARTITION(true), //
    SYSTEM_INITIATED_PARTITION(true), //

    MAINTENANCE_MODE_AGENT(false), //
    MAINTENANCE_MODE_SERVER(true); //

    private final boolean cloudPartitionEvent;

    PartitionEventType(boolean cloudPartitionEvent) {
        this.cloudPartitionEvent = cloudPartitionEvent;
    }

    /** 
     * @return true if this event type forces a partition of all agents. false if this is a single agent event.
     */
    public boolean isCloudPartitionEvent() {
        return cloudPartitionEvent;
    }

}
