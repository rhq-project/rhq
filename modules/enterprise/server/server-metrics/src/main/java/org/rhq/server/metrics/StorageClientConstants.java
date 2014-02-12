package org.rhq.server.metrics;

/**
 * @author John Sanda
 */
public class StorageClientConstants {

    public static final String REQUEST_LIMIT_MIN = "rhq.storage.request.limit.min";

    public static final String REQUEST_LIMIT = "rhq.storage.request.limit";

    public static final String REQUEST_TIMEOUT_DELTA = "rhq.storage.request.limit.timeout-delta";

    public static final String REQUEST_TIMEOUT_DAMPENING = "rhq.storage.request.timeout-dampening";

    public static final String REQUEST_TOPOLOGY_CHANGE_DELTA = "rhq.storage.request.limit.topology-delta";

    public static final String LOAD_BALANCING = "rhq.storage.client.load-balancing";

    public static final String DATA_CENTER = "rhq.storage.dc";

    private StorageClientConstants() {
    }

}
