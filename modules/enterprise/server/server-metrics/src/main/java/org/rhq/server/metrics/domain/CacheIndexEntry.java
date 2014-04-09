package org.rhq.server.metrics.domain;

import java.util.Collections;
import java.util.Set;

/**
 * Represents a CQL row in the metrics_cache_index table.
 *
 * @author John Sanda
 */
public class CacheIndexEntry {

    private MetricsTable bucket;

    private long day;

    private long insertTimeSlice;

    private int partition;

    private int startScheduleId;

    private long collectionTimeSlice;

    private Set<Integer> scheduleIds = Collections.emptySet();

    public MetricsTable getBucket() {
        return bucket;
    }

    public void setBucket(MetricsTable bucket) {
        this.bucket = bucket;
    }

    /**
     * This is {@link #getCollectionTimeSlice() collectionTimeSlice} rounded down to the start of its 24 hour time slice.
     */
    public long getDay() {
        return day;
    }

    public void setDay(long day) {
        this.day = day;
    }

    /**
     * <p>
     * This is the timestamp of when raw data is inserted, rounded down to the start of the hour. For aggregate metrics
     * it is not used and will be null.
     * </p>
     * <p>
     * This will differ from {@link #getCollectionTimeSlice() collectionTimeSlice} for late data. That is, data which
     * is stored during an hour time slice that is later than the one in which it was collected.
     * </p>
     */
    public long getInsertTimeSlice() {
        return insertTimeSlice;
    }

    public void setInsertTimeSlice(long insertTimeSlice) {
        this.insertTimeSlice = insertTimeSlice;
    }

    /**
     * Support for the partition field is currently not implemented.
     */
    public int getPartition() {
        return partition;
    }

    public void setPartition(int partition) {
        this.partition = partition;
    }

    /**
     * This is used to map to a partition in the metrics cache table. That partition contains data for
     * {@link #getScheduleIds() scheduleIds}.
     */
    public int getStartScheduleId() {
        return startScheduleId;
    }

    public void setStartScheduleId(int startScheduleId) {
        this.startScheduleId = startScheduleId;
    }

    /**
     * For raw data, this is its timestamp rounded down to the start of the hour. For 1 hour data, it is the timestamp
     * rounded down to the start of the 6 hour time slice. And for 6 hour data, it is the timestamp rounded down to the
     * start of the 24 hour time slice.
     */
    public long getCollectionTimeSlice() {
        return collectionTimeSlice;
    }

    public void setCollectionTimeSlice(long collectionTimeSlice) {
        this.collectionTimeSlice = collectionTimeSlice;
    }

    /**
     * The set of schedule ids for which there is data in the cache partition specified by this entry.
     */
    public Set<Integer> getScheduleIds() {
        return scheduleIds;
    }

    public void setScheduleIds(Set<Integer> scheduleIds) {
        this.scheduleIds = scheduleIds;
    }

    @Override
    public String toString() {
        return "CacheIndexEntry[bucket: " + bucket + ", day: " + day + ", insertTimeSlice: " + insertTimeSlice +
            ", partition: " + partition + ", collectionTimeSlice: " + collectionTimeSlice + ", startScheduleId: " +
            startScheduleId + ", insertTimeSlice: " + insertTimeSlice + "]";
    }
}
