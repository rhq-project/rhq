package org.rhq.server.metrics;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.cassandra.utils.UUIDGen;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.rhq.core.domain.measurement.calltime.CallTimeData;
import org.rhq.core.domain.measurement.calltime.CallTimeDataValue;

import com.datastax.driver.core.BoundStatement;
import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;


/**
 * Manages call time persistence.
 */
public class CallTimeDAO {

    private static final long MIN_CLOCK_SEQ_AND_NODE = 0x8080808080808080L;

    private final Log log = LogFactory.getLog(CallTimeDAO.class);

    private final StorageSession storageSession;

    private final CallTimeConfiguration configuration;

    private PreparedStatement insert;

    private PreparedStatement selectRange;

    private PreparedStatement selectRangeDest;

    private PreparedStatement deleteSchedule;

    /**
     * Cassandra table name.
     */
    public static final String TABLE  = "calltime";

    /**
     * UUID counter.
     */
    private static final AtomicInteger nanos = new AtomicInteger();

    /**
     * Unclear if this needs to be configurable or not.
     * These threads are used to notify the alert cache.
     */
    private final ExecutorService executor =
            Executors.newFixedThreadPool(10, new StorageClientThreadFactory("CallTimeDAO"));

    /**
     * Constructs a new instance with session and configuration.
     */
    public CallTimeDAO(StorageSession session, CallTimeConfiguration configuration) {
        this.storageSession = session;
        this.configuration = configuration;
        initPreparedStatements();
    }

    /**
     * Called to shutdown.
     */
    public void shutdown() {
        executor.shutdown();
    }

    /**
     * Initialize statements before use.
     */
    public void initPreparedStatements() {
        log.info("Initializing prepared statements");
        long startTime = System.currentTimeMillis();
        int ttl = configuration.getTTLSeconds();

        // CallTimeDataComposite
        insert = storageSession.prepare(
            "INSERT INTO " + TABLE + " (schedule_id, dest, start, end, min, max, total, count) " +
            "VALUES (?,?,?,?, ?,?,?,?) USING TTL " + ttl);

        String s = "SELECT dest, start, end, min, max, total, count from " + TABLE +
            " WHERE schedule_id = ? AND start >= ? AND start <= ?";
        selectRange = storageSession.prepare(s);
        selectRangeDest = storageSession.prepare(s + " AND dest = ?");

        deleteSchedule = storageSession.prepare(
            "DELETE FROM " + TABLE + " WHERE schedule_id = ?");

        long endTime = System.currentTimeMillis();
        log.info("Finished initializing prepared statements in " + (endTime - startTime) + " ms");
    }

    /**
     * Select a range of call times.
     * The list will be sorted by begin time for an event, i.e. oldest first.
     *
     * @param scheduleId
     * @param destination if not null, destination name to filter with
     * @param start
     * @param end
     * @return list of rows
     */
    public List<CallTimeRow> select(int scheduleId, String destination, Date start, Date end) {

        List<CallTimeRow> list = new ArrayList<CallTimeRow>();
        BoundStatement bind;
        UUID startU = UUIDGen.minTimeUUID(start.getTime());
        UUID endU = UUIDGen.minTimeUUID(end.getTime());
        if (destination == null) {
            bind = selectRange.bind(scheduleId, startU, endU);
        } else {
            bind = selectRangeDest.bind(scheduleId, startU, endU, destination);
        }
        ResultSet result = storageSession.execute(bind);
        for (Row row : result) {
            String dest = row.getString(0);
            long t1 = UUIDGen.getAdjustedTimestamp(row.getUUID(1));
            Date t2 = row.getDate(2);
            if (t2.after(end)) {
                break;
            }
            double min = row.getDouble(3);
            double max = row.getDouble(4);
            double total = row.getDouble(5);
            long count = row.getLong(6);
            CallTimeRow ct = new CallTimeRow(scheduleId, dest, new Date(t1), t2, min, max, total, count);
            list.add(ct);
        }
        return list;
    }

    /**
     * Insert call times.
     * The callback will be called when the insert is complete.
     */
    public void insert(Collection<CallTimeData> calltime, final FutureCallback<Object> callback) {

        if (calltime.isEmpty()) {
            executor.submit(new Runnable() {
                public void run() { callback.onSuccess(null); }
            });
            return;
        }

        final AtomicInteger count = new AtomicInteger(calltime.size());
        for (CallTimeData data : calltime) {
            ListenableFuture<List<ResultSet>> future = insert(data);
            Futures.addCallback(future, new FutureCallback<List<ResultSet>>() {
                @Override
                public void onSuccess(List<ResultSet> result) {
                    if (count.decrementAndGet() == 0) {
                        log.trace("completed inserts");
                        callback.onSuccess(result);
                    }
                }

                @Override
                public void onFailure(Throwable throwable) {
                    log.trace("failure inserting", throwable);
                    callback.onFailure(throwable);
                }

            }, this.executor);
        }
    }

    /**
     * Inserts one call time, returning a future.
     */
    public ListenableFuture<List<ResultSet>> insert(CallTimeData data) {
        BoundStatement statement;
        List<StorageResultSetFuture> insertFutures = new ArrayList<StorageResultSetFuture>(3);
        for (Entry<String, CallTimeDataValue> call : data.getValues().entrySet()) {
            int scheduleId = data.getScheduleId();
            String dest = call.getKey();
            CallTimeDataValue value = call.getValue();
            UUID begin = uuidForCall(value.getBeginTime(), dest);
            statement = insert.bind(
            // " (schedule_id, dest, start, end, min, max, total, count) " +
                    scheduleId, dest, begin, new Date(value.getEndTime()),
                    value.getMinimum(), value.getMaximum(),
                    value.getTotal(), value.getCount());
            StorageResultSetFuture future = storageSession.executeAsync(statement);
            insertFutures.add(future);
        }
        return Futures.successfulAsList(insertFutures);
    }

    /**
     * Selects schedules by date range only.
     */
    public List<CallTimeRow> select(int scheduleId, Date begin, Date end) {
        return select(scheduleId, null, begin, end);
    }

    /**
     * Delete call time data by ID.
     */
    public StorageResultSetFuture deleteSchedule(int id) {
        BoundStatement bind = deleteSchedule.bind(id);
        return storageSession.executeAsync(bind);
    }

    /**
     * Converts time into a UUID, adding a unique 100-nanosecond value.
     * If configured as an idempotent insert, hashes the destination string.
     */
    private UUID uuidForCall(long time, String dest) {
        if (configuration.isIdempotentInsert()) {
            UUID uuid = UUIDGen.minTimeUUID(time);
            int hash = Math.abs(dest.hashCode());
            return new UUID(uuid.getMostSignificantBits(), MIN_CLOCK_SEQ_AND_NODE + hash);
        }

        int nano = nanos.incrementAndGet();
        nano = Math.abs(nano % 10000);
        byte[] raw = UUIDGen.getTimeUUIDBytes(time, nano);
        return org.apache.cassandra.utils.UUIDGen.getUUID(ByteBuffer.wrap(raw));
    }

}
