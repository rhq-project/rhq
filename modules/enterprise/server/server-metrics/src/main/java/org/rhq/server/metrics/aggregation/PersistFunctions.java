package org.rhq.server.metrics.aggregation;

import static org.rhq.server.metrics.domain.MetricsTable.ONE_HOUR;
import static org.rhq.server.metrics.domain.MetricsTable.SIX_HOUR;

import java.util.ArrayList;
import java.util.List;

import com.datastax.driver.core.ResultSet;
import com.google.common.util.concurrent.AsyncFunction;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import org.joda.time.DateTime;

import org.rhq.server.metrics.DateTimeService;
import org.rhq.server.metrics.MetricsDAO;
import org.rhq.server.metrics.StorageResultSetFuture;
import org.rhq.server.metrics.domain.AggregateNumericMetric;
import org.rhq.server.metrics.domain.IndexEntry;

/**
 * This is a utility class of functions for persisting aggregate metrics.
 *
 * @author John Sanda
 */
class PersistFunctions {

    private MetricsDAO dao;

    private DateTimeService dateTimeService;

    private AsyncFunction<List<AggregateNumericMetric>, List<ResultSet>> persist1HourMetrics;

    private AsyncFunction<List<AggregateNumericMetric>, List<ResultSet>> persist6HourMetrics;

    private AsyncFunction<List<AggregateNumericMetric>, List<ResultSet>> persist24HourMetrics;

    public PersistFunctions(MetricsDAO dao, DateTimeService dateTimeService) {
        this.dao = dao;
        this.dateTimeService = dateTimeService;
        initFunctions();
    }

    private void initFunctions() {
        persist1HourMetrics = new AsyncFunction<List<AggregateNumericMetric>, List<ResultSet>>() {
            @Override
            public ListenableFuture<List<ResultSet>> apply(List<AggregateNumericMetric> metrics) throws Exception {
                List<StorageResultSetFuture> futures = new ArrayList<StorageResultSetFuture>(metrics.size());
                for (AggregateNumericMetric metric : metrics) {
                    DateTime timeSlice = dateTimeService.get6HourTimeSlice(metric.getTimestamp());
                    futures.add(dao.insert1HourData(metric));
                    futures.add(dao.insertIndexEntry(new IndexEntry(ONE_HOUR, 0, timeSlice.getMillis(),
                        metric.getScheduleId())));
                }
                return Futures.allAsList(futures);
            }
        };

        persist6HourMetrics = new AsyncFunction<List<AggregateNumericMetric>, List<ResultSet>>() {
            @Override
            public ListenableFuture<List<ResultSet>> apply(List<AggregateNumericMetric> metrics) throws Exception {
                List<StorageResultSetFuture> futures = new ArrayList<StorageResultSetFuture>(metrics.size());
                for (AggregateNumericMetric metric : metrics) {
                    DateTime timeSlice = dateTimeService.get24HourTimeSlice(metric.getTimestamp());
                    futures.add(dao.insert6HourData(metric));
                    futures.add(dao.insertIndexEntry(new IndexEntry(SIX_HOUR, 0, timeSlice.getMillis(),
                        metric.getScheduleId())));
                }
                return Futures.allAsList(futures);
            }
        };

        persist24HourMetrics = new AsyncFunction<List<AggregateNumericMetric>, List<ResultSet>>() {
            @Override
            public ListenableFuture<List<ResultSet>> apply(List<AggregateNumericMetric> metrics) throws Exception {
                List<StorageResultSetFuture> futures = new ArrayList<StorageResultSetFuture>(metrics.size());
                for (AggregateNumericMetric metric : metrics) {
                    futures.add(dao.insert24HourData(metric));
                }
                return Futures.allAsList(futures);
            }
        };
    }

    public AsyncFunction<List<AggregateNumericMetric>, List<ResultSet>> persist1HourMetrics() {
        return persist1HourMetrics;
    }

    public AsyncFunction<List<AggregateNumericMetric>, List<ResultSet>> persist6HourMetrics() {
        return persist6HourMetrics;
    }

    public AsyncFunction<List<AggregateNumericMetric>, List<ResultSet>> persist24HourMetrics() {
        return persist24HourMetrics;
    }
}
