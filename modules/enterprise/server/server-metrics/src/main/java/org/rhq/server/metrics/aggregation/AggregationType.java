package org.rhq.server.metrics.aggregation;

import org.joda.time.Days;
import org.joda.time.Duration;
import org.joda.time.Hours;

import org.rhq.server.metrics.domain.IndexBucket;

/**
 * @author John Sanda
 */
public enum AggregationType {

    RAW("raw data", IndexBucket.RAW, Hours.ONE.toStandardDuration()),

    ONE_HOUR("one hour data", IndexBucket.ONE_HOUR, Hours.SIX.toStandardDuration()),

    SIX_HOUR("six hour data", IndexBucket.SIX_HOUR, Days.ONE.toStandardDuration());

    private String type;

    private IndexBucket bucket;

    private Duration timeSliceDuration;

    private AggregationType(String type, IndexBucket bucket, Duration timeSliceDuration) {
        this.type = type;
        this.bucket = bucket;
        this.timeSliceDuration = timeSliceDuration;
    }

    public IndexBucket getBucket() {
        return bucket;
    }

    public Duration getTimeSliceDuration() {
        return timeSliceDuration;
    }

    @Override
    public String toString() {
        return type;
    }
}
