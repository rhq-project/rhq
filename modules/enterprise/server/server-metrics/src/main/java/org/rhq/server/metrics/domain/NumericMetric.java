package org.rhq.server.metrics.domain;

/**
 * @author John Sanda
 */
public interface NumericMetric {

    int getScheduleId();

    Double getMin();

    Double getMax();

    Double getAvg();

}
