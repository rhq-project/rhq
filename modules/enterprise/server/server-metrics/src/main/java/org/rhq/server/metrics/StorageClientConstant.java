package org.rhq.server.metrics;

/**
 * @author John Sanda
 */
public enum StorageClientConstant {

    REQUEST_LIMIT_MIN("rhq.storage.request.limit.min"),

    REQUEST_LIMIT("rhq.storage.request.limit"),

    REQUEST_TIMEOUT_DELTA("rhq.storage.request.limit.timeout-delta"),

    REQUEST_TIMEOUT_DAMPENING("rhq.storage.request.timeout-dampening"),

    REQUEST_TOPOLOGY_CHANGE_DELTA("rhq.storage.request.limit.topology-delta");

    private String property;

    private StorageClientConstant(String property) {
        this.property = property;
    }

    public String property() {
        return property;
    }

}
