package org.rhq.server.metrics.aggregation;

import java.util.Collections;
import java.util.List;

import com.datastax.driver.core.ResultSet;

import org.joda.time.DateTime;

/**
 * @author John Sanda
 */
class BatchResult {

    private List<ResultSet> insertResultSets;

    private DateTime timeSlice;

    private int startScheduleId;

    private ResultSet purgeCacheResultSet;

    private boolean empty;

    public BatchResult(List<ResultSet> insertResultSets, DateTime timeSlice, int startScheduleId,
        ResultSet purgeCacheResultSet) {
        this.insertResultSets = insertResultSets;
        this.timeSlice = timeSlice;
        this.startScheduleId = startScheduleId;
        this.purgeCacheResultSet = purgeCacheResultSet;
    }

    public BatchResult(DateTime timeSlice, int startScheduleId) {
        this.timeSlice = timeSlice;
        this.startScheduleId = startScheduleId;
        insertResultSets = Collections.emptyList();
        empty = true;
    }

    boolean isEmpty() {
        return empty;
    }

    public List<ResultSet> getInsertResultSets() {
        return insertResultSets;
    }

    public DateTime getTimeSlice() {
        return timeSlice;
    }

    public int getStartScheduleId() {
        return startScheduleId;
    }

    public ResultSet getPurgeCacheResultSet() {
        return purgeCacheResultSet;
    }
}
