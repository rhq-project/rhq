package org.rhq.server.metrics;

import static org.rhq.server.metrics.TraitsDAO.TABLE;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.google.common.util.concurrent.Futures;

class TraitsCleanup {

    private final Log log = LogFactory.getLog(TraitsCleanup.class);

    /**
     * For cleanup, how many delete statements to wait for at submit.
     */
    public static final int BULK_COUNT = 25;

    /**
     * Limit size. Cassandra 2.0 doesn't need this.
     */
    static int LIMIT = 1000;

    private StorageSession storageSession;
    private int scheduleIdOld = -1;
    private String preValue = null;
    private Date preTime = null;
    private List<StorageResultSetFuture> futures = new ArrayList<StorageResultSetFuture>(BULK_COUNT);

    private PreparedStatement selectCleanupIds;

    private PreparedStatement selectCleanupIds2;

    private PreparedStatement deleteHistory;

    /**
     * Constructs a new instance.
     */
    public TraitsCleanup(StorageSession session) {
        this.storageSession = session;

        String s = "SELECT schedule_id, time, value from " + TABLE +
            " WHERE time > ? LIMIT " + LIMIT + " ALLOW FILTERING";
        selectCleanupIds = storageSession.prepare(s);

        s = "SELECT schedule_id, time, value from " + TABLE +
            " WHERE time > ? AND token(schedule_id) > token(?) LIMIT " + LIMIT + " ALLOW FILTERING";
        selectCleanupIds2 = storageSession.prepare(s);

        deleteHistory = storageSession.prepare(
                "DELETE FROM " + TABLE + " WHERE schedule_id = ? AND time = ?");
    }

    /**
     * Run cleanup.
     */
    public void cleanup(Date after) {
        log.debug("cleanup traits after " + after);
        ResultSet result = storageSession.execute(selectCleanupIds.bind(after));
        while (cleanup(result)) {
            log.debug("fetching more rows...");
            result = storageSession.execute(selectCleanupIds2.bind(after, scheduleIdOld));
        }
        complete(futures);
        log.debug("done");
    }

    /**
     * Returns false if no more results.
     */
    private boolean cleanup(ResultSet result) {
        boolean debug = log.isDebugEnabled();
        if (result.isExhausted())
            return false;
        for (Row row : result) {
            int scheduleId = row.getInt(0);
            if (scheduleId != scheduleIdOld) {
                preValue = null;
            }
            scheduleIdOld = scheduleId;

            if (debug) {
                log.debug("cleanup sid=" + scheduleId);
            }

            /*
             * newer -> older
             * (T4, a), (T3, a), (T2, b), (T1, b)
             * remote T3, T1
             */
            Date time = row.getDate(1);
            String value = row.getString(2);
            if (preValue != null && preValue.equals(value)) {
                if (debug) {
                    log.debug("remove time=" + preTime);
                }
                StorageResultSetFuture future = storageSession.executeAsync(
                        deleteHistory.bind(scheduleId, preTime));
                futures.add(future);
                if (futures.size() > BULK_COUNT) {
                    complete(futures);
                }
            }
            preValue = value;
            preTime = time;
        }
        return true;
    }

    /**
     * Complete these tasks, ignoring the result.
     */
    private void complete(List<StorageResultSetFuture> futures) {
        try {
            log.debug("wait for results " + futures.size());
            Futures.successfulAsList(futures).get();
            futures.clear();
        } catch (Exception e) {
            log.error("clean failed", e);
        }
    }
}
