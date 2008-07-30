package org.rhq.core.domain.cluster;

public enum PartitionEventType {

    AGENT_REGISTRATION, //
    AGENT_JOIN, //
    AGENT_LOAD_CHANGE, //
    AGENT_LEAVE, //

    SERVER_JOIN, //
    SERVER_LEAVE, //
    SERVER_CORE_COUNT_CHANGE, //

    AFFINITY_GROUP_CHANGE, //

    USER_INITIATED_PARTITION, //
    SYSTEM_INITIATED_PARTITION, //

    MAINTENANCE_MODE_SOFT, //
    MAINTENANCE_MODE_HARD, //

}
