package org.rhq.cassandra.schema;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.ResultSetFuture;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.SimpleStatement;
import com.datastax.driver.core.Statement;
import com.datastax.driver.core.querybuilder.Batch;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import com.google.common.util.concurrent.AsyncFunction;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.RateLimiter;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.joda.time.DateTime;
import org.joda.time.Seconds;

/**
 * @author John Sanda
 */
public class MigrateData implements AsyncFunction<ResultSet, List<ResultSet>> {

    private static final Log log = LogFactory.getLog(MigrateData.class);

    private static final int BATCH_SIZE = 45;

    private Integer scheduleId;

    private MigrateAggregateMetrics.Bucket bucket;

    private RateLimiter writePermits;

    private Session session;

    private Seconds ttl;

    public MigrateData(Integer scheduleId, MigrateAggregateMetrics.Bucket bucket, RateLimiter writePermits,
        Session session, Seconds ttl) {
        this.scheduleId = scheduleId;
        this.bucket = bucket;
        this.writePermits = writePermits;
        this.session = session;
        this.ttl = ttl;
    }

    @Override
    public ListenableFuture<List<ResultSet>> apply(ResultSet resultSet) throws Exception {
        try {
            List<ResultSetFuture> insertFutures = new ArrayList<ResultSetFuture>();
            if (resultSet.isExhausted()) {
                return Futures.allAsList(insertFutures);
            }
            List<Row> rows = resultSet.all();
            Date time = rows.get(0).getDate(0);
            Date nextTime;
            Double max = null;
            Double min = null;
            Double avg = null;
            Seconds elapsedSeconds = Seconds.secondsBetween(DateTime.now(), new DateTime(time));
            List<Statement> statements = new ArrayList<Statement>(BATCH_SIZE);

            for (Row row : rows) {
                nextTime = row.getDate(0);
                if (nextTime.equals(time)) {
                    int type = row.getInt(1);
                    switch (type) {
                    case 0:
                        max = row.getDouble(2);
                        break;
                    case 1:
                        min = row.getDouble(2);
                        break;
                    default:
                        avg = row.getDouble(2);
                    }
                } else {
                    if (elapsedSeconds.isLessThan(ttl)) {
                        if (isDataMissing(avg, max, min)) {
                            if (log.isDebugEnabled()) {
                                log.debug("We only have a partial " + bucket + " metric for {scheduleId: " +
                                    scheduleId + ", time: " + time.getTime() + "}. It will not be migrated.");
                            }
                        } else {
                            int newTTL = ttl.getSeconds() - elapsedSeconds.getSeconds();
                            statements.add(createInsertStatement(time, avg, max, min, newTTL));
                            if (statements.size() == BATCH_SIZE) {
                                insertFutures.add(writeBatch(statements));
                                statements.clear();
                            }
                        }

                        time = nextTime;
                        max = row.getDouble(2);
                        min = null;
                        avg = null;
                    }
                }
            }
            if (!statements.isEmpty()) {
                insertFutures.add(writeBatch(statements));
            }
            return Futures.allAsList(insertFutures);
        } catch (Exception e) {
            log.warn("An error occurred while migrating data", e);
            throw e;
        }
    }

    private boolean isDataMissing(Double avg, Double max, Double min) {
        if (avg == null || Double.isNaN(avg)) return true;
        if (max == null || Double.isNaN(max)) return true;
        if (min == null || Double.isNaN(min)) return true;

        return false;
    }

    private ResultSetFuture writeBatch(List<Statement> statements) {
        Batch batch = QueryBuilder.batch(statements.toArray(new Statement[statements.size()]));
        writePermits.acquire();
        return session.executeAsync(batch);
    }

    private SimpleStatement createInsertStatement(Date time, Double avg, Double max, Double min, int newTTL) {
        return new SimpleStatement("INSERT INTO rhq.aggregate_metrics(schedule_id, bucket, time, avg, max, min) " +
            "VALUES (" + scheduleId + ", '" + bucket + "', " + time.getTime() + ", " + avg + ", " + max + ", " + min +
            ") USING TTL " + newTTL);
    }

}
