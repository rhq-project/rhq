package org.rhq.server.metrics;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.rhq.core.domain.measurement.MeasurementDataPK;
import org.rhq.core.domain.measurement.MeasurementDataTrait;
import org.rhq.core.domain.measurement.MeasurementSchedule;

import com.datastax.driver.core.BoundStatement;
import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;


/**
 * Manages trait persistence.
 */
public class TraitsDAO {

    private final Log log = LogFactory.getLog(TraitsDAO.class);

    private final StorageSession storageSession;

    private final TraitsConfiguration configuration;

    private PreparedStatement insertTrait;

    PreparedStatement deleteHistory;

    PreparedStatement selectCleanupIds;

    /**
     * Table for traits.
     */
    public static final String TABLE  = "measurement_data_traits";

    /**
     * Unclear if this needs to be configurable or not.
     * These threads are used to notify the alert cache.
     */
    private final ExecutorService executor =
            Executors.newFixedThreadPool(10, new StorageClientThreadFactory("TraitsDAO"));

    private PreparedStatement deleteTrait;

    private PreparedStatement selectHistory;

    private PreparedStatement selectLatest;

    /**
     * Constructs a new instance with session and configuration.
     */
    public TraitsDAO(StorageSession session, TraitsConfiguration configuration) {
        this.storageSession = session;
        this.configuration = configuration;
        initPreparedStatements();
    }

    /**
     * Called to shutdown the thread pools.
     */
    public void shutdown() {
        executor.shutdown();
    }

    /**
     * Initialize statements before use.
     */
    public void initPreparedStatements() {
        log.info("Initializing prepared statements");
        int ttl = configuration.getTTLSeconds();

        insertTrait = storageSession.prepare(
            "INSERT INTO " + TABLE + " (schedule_id, time, value) " +
            "VALUES (?, ?, ?) USING TTL " + ttl);

        String s = "SELECT schedule_id, time, value from " + TABLE + " WHERE schedule_id = ?";
        selectHistory = storageSession.prepare(s);
        selectLatest = storageSession.prepare(s + " LIMIT 1");
        deleteHistory = storageSession.prepare(
            "DELETE FROM " + TABLE + " WHERE schedule_id = ? AND time = ?");
        deleteTrait = storageSession.prepare(
            "DELETE FROM " + TABLE + " WHERE schedule_id = ?");

        log.info("Finished initializing prepared statements");
    }

    /**
     * Removes duplicated history entries after this date. Duplicates may still
     * exist in the history table before this date.
     *
     * New traits may be added before cleanup is completed. This is okay as
     * duplicate history entries are expected and filtered.
     */
    public void cleanup(Date after) {
        TraitsCleanup traitsCleanup = new TraitsCleanup(storageSession);
        traitsCleanup.cleanup(after);
    }

    /**
     * Returns all traits for these schedules using bulk async query.
     * The order is not preserved, however if history is obtained, the
     * history is ordered from latest to oldest.
     */
    public List<MeasurementDataTrait> queryAll(Collection<MeasurementSchedule> schedules, boolean history) {
        List<MeasurementDataTrait> traits = new ArrayList<MeasurementDataTrait>(schedules.size());
        List<StorageResultSetFuture> futures = new ArrayList<StorageResultSetFuture>();
        for (MeasurementSchedule sched : schedules) {
            BoundStatement bind;
            if (history) {
                bind = selectHistory.bind(sched.getId());
            } else {
                bind = selectLatest.bind(sched.getId());
            }
            futures.add(storageSession.executeAsync(bind));
        }
        for (StorageResultSetFuture future : futures) {
            ResultSet resultSet = future.get();
            if (history) {
                traits.addAll(historyFor(resultSet));
            } else {
                MeasurementDataTrait trait = currentTrait(resultSet);
                if (trait != null) {
                    traits.add(trait);
                }
            }
        }
        return traits;
    }

    /**
     * Returns the current trait for this resource and schedule,
     * or null if not found.
     */
    public MeasurementDataTrait currentTrait(int scheduleId) {
        BoundStatement bind = selectLatest.bind(scheduleId);
        ResultSet result = storageSession.execute(bind);
        return currentTrait(result);
    }

    private MeasurementDataTrait currentTrait(ResultSet result) {
        Row row = result.one();
        if (row == null) {
            return null;
        }
        int scheduleId = row.getInt(0);
        Date time = row.getDate(1);
        String value = row.getString(2);
        MeasurementDataPK pk = new MeasurementDataPK(time.getTime(), scheduleId);
        MeasurementDataTrait trait = new MeasurementDataTrait(pk, value);
        return trait;
    }

    /**
     * Returns the history for a particular schedule, where the
     * first entry is the latest value, etc.
     */
    public List<MeasurementDataTrait> historyFor(int scheduleId) {
        ResultSet result = storageSession.execute(selectHistory.bind(scheduleId));
        return historyFor(result);
    }

    private List<MeasurementDataTrait> historyFor(ResultSet result) {
        List<MeasurementDataTrait> history = new ArrayList<MeasurementDataTrait>();
        /*
         * newer --> older
         * (T4, a), (T3, a), (T2, b), (T1, b)
         */
        MeasurementDataTrait trait = null;
        for (Row hrow : result) {
            int scheduleId = hrow.getInt(0);
            Date time = hrow.getDate(1);
            String value = hrow.getString(2);
            if (trait != null && !trait.getValue().equals(value)) {
                history.add(trait);
            }
            MeasurementDataPK pk = new MeasurementDataPK(time.getTime(), scheduleId);
            trait = new MeasurementDataTrait(pk, value);
        }
        if (trait != null) {
            history.add(trait);
        }
        return history;
    }

    /**
     * Inserts a measurement trait.
     */
    public StorageResultSetFuture insertTrait(MeasurementDataTrait data) {
        BoundStatement statement;
        int scheduleId = data.getScheduleId();
        statement = insertTrait.bind(
                scheduleId, new Date(data.getTimestamp()),
                data.getValue());
        return storageSession.executeAsync(statement);
    }

    /**
     * Inserts multiple traits, calling the given callback function when done.
     * Note that the callback is executed in the DAO thread pool.
     */
    public void insertTraits(Set<MeasurementDataTrait> traits, final FutureCallback<Object> callback) {

        if (log.isDebugEnabled()) {
            log.debug("Inserting " + traits.size() + " traits");
        }

        if (traits.isEmpty()) {
            executor.submit(new Runnable() {
                public void run() { callback.onSuccess(null); }
            });
            return;
        }

        final AtomicInteger count = new AtomicInteger(traits.size());
        for (MeasurementDataTrait trait : traits) {
            ListenableFuture<ResultSet> future = insertTrait(trait);
            Futures.addCallback(future, new FutureCallback<ResultSet>() {
                @Override
                public void onSuccess(ResultSet result) {
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
     * Deletes a trait by schedule ID, returning a future.
     */
    public StorageResultSetFuture deleteSchedule(int scheduleId) {
        return storageSession.executeAsync(deleteTrait.bind(scheduleId));
    }

}
