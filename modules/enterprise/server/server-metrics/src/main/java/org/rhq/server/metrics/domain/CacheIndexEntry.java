package org.rhq.server.metrics.domain;

import java.util.Collections;
import java.util.Set;

/**
 * @author John Sanda
 */
public class CacheIndexEntry {

    private MetricsTable bucket;

    private long insertTimeSlice;

    private int partition;

    private int startScheduleId;

    private long collectionTimeSlice;

    private Set<Integer> scheduleIds = Collections.EMPTY_SET;

    public MetricsTable getBucket() {
        return bucket;
    }

    public void setBucket(MetricsTable bucket) {
        this.bucket = bucket;
    }

    public long getInsertTimeSlice() {
        return insertTimeSlice;
    }

    public void setInsertTimeSlice(long insertTimeSlice) {
        this.insertTimeSlice = insertTimeSlice;
    }

    public int getPartition() {
        return partition;
    }

    public void setPartition(int partition) {
        this.partition = partition;
    }

    public int getStartScheduleId() {
        return startScheduleId;
    }

    public void setStartScheduleId(int startScheduleId) {
        this.startScheduleId = startScheduleId;
    }

    public long getCollectionTimeSlice() {
        return collectionTimeSlice;
    }

    public void setCollectionTimeSlice(long collectionTimeSlice) {
        this.collectionTimeSlice = collectionTimeSlice;
    }

    public Set<Integer> getScheduleIds() {
        return scheduleIds;
    }

    public void setScheduleIds(Set<Integer> scheduleIds) {
        this.scheduleIds = scheduleIds;
    }

}
