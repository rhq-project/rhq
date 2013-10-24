package org.rhq.server.metrics;

import org.rhq.server.metrics.domain.MetricsTable;

/**
 * @author John Sanda
 */
public class MetricsUtil {

    public static final int METRICS_INDEX_ROW_SIZE = 5000;

    public static String indexPartitionKey(MetricsTable table, int scheduleId) {
        int offset = (scheduleId / METRICS_INDEX_ROW_SIZE) * METRICS_INDEX_ROW_SIZE;
        return table + ":" + Integer.toString(offset);
    }

}
