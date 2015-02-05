package org.rhq.cassandra.schema.migration;

import com.datastax.driver.core.querybuilder.Batch;

/**
 * @author John Sanda
 */
public class FailedBatch {

    private Integer scheduleId;

    private Batch batch;

    public FailedBatch(Integer scheduleId, Batch batch) {
        this.scheduleId = scheduleId;
        this.batch = batch;
    }

    public Integer getScheduleId() {
        return scheduleId;
    }

    public Batch getBatch() {
        return batch;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        FailedBatch that = (FailedBatch) o;

        if (!batch.equals(that.batch)) return false;
        if (!scheduleId.equals(that.scheduleId)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = scheduleId.hashCode();
        result = 31 * result + batch.hashCode();
        return result;
    }
}
