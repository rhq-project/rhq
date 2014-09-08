package org.rhq.server.metrics.domain;

import com.google.common.base.Objects;

import org.joda.time.DateTime;

/**
 * @author John Sanda
 */
public class IndexEntry {

    private MetricsTable bucket;

    private int partition;

    private long timestamp;

    private int scheduleId;

    public IndexEntry() {
    }

    public IndexEntry(MetricsTable bucket, int partition, long timestamp, int scheduleId) {
        this.bucket = bucket;
        this.partition = partition;
        this.timestamp = timestamp;
        this.scheduleId = scheduleId;
    }

    public IndexEntry(MetricsTable bucket, int partition, DateTime time, int scheduleId) {
        this(bucket, partition, time.getMillis(), scheduleId);
    }

    public MetricsTable getBucket() {
        return bucket;
    }

    public void setBucket(MetricsTable bucket) {
        this.bucket = bucket;
    }

    public int getPartition() {
        return partition;
    }

    public void setPartition(int partition) {
        this.partition = partition;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public int getScheduleId() {
        return scheduleId;
    }

    public void setScheduleId(int scheduleId) {
        this.scheduleId = scheduleId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        IndexEntry that = (IndexEntry) o;

        if (partition != that.partition) return false;
        if (scheduleId != that.scheduleId) return false;
        if (timestamp != that.timestamp) return false;
        if (bucket != that.bucket) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = bucket.hashCode();
        result = 31 * result + partition;
        result = 31 * result + (int) (timestamp ^ (timestamp >>> 32));
        result = 31 * result + scheduleId;
        return result;
    }

    @Override
    public String toString() {
        return Objects.toStringHelper(IndexEntry.class)
            .add("bucket", bucket)
            .add("partition", partition)
            .add("time", timestamp)
            .add("scheduleId", scheduleId)
            .toString();
    }
}
