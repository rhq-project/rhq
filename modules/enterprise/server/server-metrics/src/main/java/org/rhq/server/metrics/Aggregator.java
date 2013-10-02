package org.rhq.server.metrics;

import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.joda.time.DateTime;
import org.joda.time.DateTimeComparator;
import org.joda.time.Duration;

import org.rhq.server.metrics.domain.AggregateNumericMetric;
import org.rhq.server.metrics.domain.AggregateType;
import org.rhq.server.metrics.domain.MetricsIndexEntry;
import org.rhq.server.metrics.domain.MetricsIndexEntryMapper;
import org.rhq.server.metrics.domain.MetricsTable;
import org.rhq.server.metrics.domain.RawNumericMetric;
import org.rhq.server.metrics.domain.RawNumericMetricMapper;

/**
 * @author John Sanda
 */
public class Aggregator {

    private final Log log = LogFactory.getLog(Aggregator.class);

    private ListeningExecutorService workers;

    private MetricsDAO dao;

    private Set<AggregateNumericMetric> oneHourAggregates = new ConcurrentSkipListSet<AggregateNumericMetric>(new Comparator<AggregateNumericMetric>() {
        @Override
        public int compare(AggregateNumericMetric left, AggregateNumericMetric right) {
            return Integer.compare(left.getScheduleId(), right.getScheduleId());
        }
    });

    private MetricsConfiguration configuration;

    private DateTimeService dtService;

    private DateTime startTime;

    private boolean oneHourDataReady;

    private boolean sixHourDataReady;

    private AtomicInteger remainingRawData;
//    private AtomicInteger remainingOneHourData

    private CountDownLatch countDownLatch;

    public Aggregator(ListeningExecutorService workers, MetricsDAO dao, MetricsConfiguration configuration,
        DateTimeService dtService, DateTime startTime) {
        this.workers = workers;
        this.dao = dao;
        this.configuration = configuration;
        this.dtService = dtService;
        this.startTime = startTime;

        DateTime oneHourTimeSlice = dtService.getTimeSlice(startTime, configuration.getOneHourTimeSliceDuration());
        DateTime sixHourTimeSlice = dtService.getTimeSlice(startTime, configuration.getSixHourTimeSliceDuration());

        oneHourDataReady = hasTimeSliceEnded(oneHourTimeSlice, configuration.getOneHourTimeSliceDuration());
        sixHourDataReady = hasTimeSliceEnded(sixHourTimeSlice, configuration.getSixHourTimeSliceDuration());

        int count = 1;
//        count += oneHourDataReady ? 1 : 0;
//        count += sixHourDataReady ? 1 : 0;

        countDownLatch = new CountDownLatch(count);
    }

    private boolean hasTimeSliceEnded(DateTime startTime, Duration duration) {
        DateTime endTime = startTime.plus(duration);
        return DateTimeComparator.getInstance().compare(currentHour(), endTime) >= 0;
    }

    protected DateTime currentHour() {
        return dtService.getTimeSlice(dtService.now(), configuration.getRawTimeSliceDuration());
    }

    public Set<AggregateNumericMetric> run() throws InterruptedException {
        StorageResultSetFuture rawFuture = dao.findMetricsIndexEntriesAsync(MetricsTable.ONE_HOUR, startTime.getMillis());
        Futures.addCallback(rawFuture, new FutureCallback<ResultSet>() {
            @Override
            public void onSuccess(ResultSet result) {
                List<Row> rows = result.all();
                remainingRawData = new AtomicInteger(rows.size());
                MetricsIndexEntryMapper mapper = new MetricsIndexEntryMapper(MetricsTable.ONE_HOUR);
                for (Row row : rows) {
                    MetricsIndexEntry indexEntry = mapper.map(row);
                    aggregateRawData(indexEntry);
                }
            }

            @Override
            public void onFailure(Throwable t) {
                log.warn("Failed to retrieve raw data index entries for time slice " + startTime +
                    ". Data aggregation cannot proceed.", t);
            }
        }, workers);
        countDownLatch.await();

        return oneHourAggregates;
    }

    private void aggregateRawData(final MetricsIndexEntry indexEntry) {
        final DateTime startTime = indexEntry.getTime();
        DateTime endTime = startTime.plus(configuration.getRawTimeSliceDuration());
        StorageResultSetFuture future = dao.findRawMetricsAsync(indexEntry.getScheduleId(), startTime.getMillis(),
            endTime.getMillis());
        Futures.addCallback(future, new FutureCallback<ResultSet>() {
            @Override
            public void onSuccess(ResultSet result) {
                RawNumericMetricMapper mapper = new RawNumericMetricMapper();
                List<RawNumericMetric> metrics = mapper.mapAll(result);
                final AggregateNumericMetric oneHourAggregate = calculateAggregatedRaw(metrics, startTime.getMillis());
                oneHourAggregates.add(oneHourAggregate);

                ListenableFuture<List<ResultSet>> insertFuture = Futures.allAsList(
                    dao.insertOneHourDataAsync(oneHourAggregate.getScheduleId(), oneHourAggregate.getTimestamp(),
                        AggregateType.MIN, oneHourAggregate.getMin()),
                    dao.insertOneHourDataAsync(oneHourAggregate.getScheduleId(), oneHourAggregate.getTimestamp(),
                        AggregateType.MAX, oneHourAggregate.getMax()),
                    dao.insertOneHourDataAsync(oneHourAggregate.getScheduleId(), oneHourAggregate.getTimestamp(),
                        AggregateType.AVG, oneHourAggregate.getAvg())
                );
                Futures.addCallback(insertFuture, new FutureCallback<List<ResultSet>>() {
                    @Override
                    public void onSuccess(List<ResultSet> result) {
                        if (result.get(0) == null) {
                            log.warn("Failed to store the minimum of the one hour aggregate " + oneHourAggregate);
                        }
                        if (result.get(1) == null) {
                            log.warn("Failed to store the maximum of the one hour aggregate " + oneHourAggregate);
                        }
                        if (result.get(2) == null) {
                            log.warn("Failed to store the average of the one hour aggregate " + oneHourAggregate);
                        }
                        updateRemainingRawDataCount();
                        // TODO compute six hour aggregate
                    }

                    @Override
                    public void onFailure(Throwable t) {
                        log.warn("Failed to store one hour aggregate " + oneHourAggregate, t);
                        updateRemainingRawDataCount();
                    }
                }, workers);
            }

            @Override
            public void onFailure(Throwable t) {
                log.warn("Failed to fetch raw data for " + indexEntry);
            }
        }, workers);
    }

    private AggregateNumericMetric calculateAggregatedRaw(List<RawNumericMetric> rawMetrics, long timestamp) {
        double min = Double.NaN;
        double max = min;
        int count = 0;
        ArithmeticMeanCalculator mean = new ArithmeticMeanCalculator();
        double value;

        for (RawNumericMetric metric : rawMetrics) {
            value = metric.getValue();
            if (count == 0) {
                min = value;
                max = min;
            }
            if (value < min) {
                min = value;
            } else if (value > max) {
                max = value;
            }
            mean.add(value);
            ++count;
        }

        // We let the caller handle setting the schedule id because in some cases we do
        // not care about it.
        return new AggregateNumericMetric(rawMetrics.get(0).getScheduleId(), mean.getArithmeticMean(), min, max,
            timestamp);
    }

    private void updateRemainingRawDataCount() {
        if (remainingRawData.decrementAndGet() == 0) {
            countDownLatch.countDown();
        }
    }

}
