package org.rhq.server.metrics;

import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
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
import org.rhq.server.metrics.domain.AggregateNumericMetricMapper;
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
    private AtomicInteger remainingOneHourData;
    private AtomicInteger remainingSixHourData;

    private CountDownLatch allAggregationFinished;
    private CountDownLatch oneHourIndexEntriesArrival;
    private CountDownLatch sixHourIndexEntriesArrival;

    private ConcurrentHashMap<Integer, MetricsIndexEntry> oneHourIndexEntries;
    private ConcurrentHashMap<Integer, MetricsIndexEntry> sixHourIndexEntries;

    public Aggregator(ListeningExecutorService workers, MetricsDAO dao, MetricsConfiguration configuration,
        DateTimeService dtService, DateTime startTime) {
        this.workers = workers;
        this.dao = dao;
        this.configuration = configuration;
        this.dtService = dtService;
        this.startTime = startTime;

        DateTime oneHourTimeSlice = getOneHourTimeSlice();
        DateTime sixHourTimeSlice = getSixHourTimeSlice();

        oneHourDataReady = hasTimeSliceEnded(oneHourTimeSlice, configuration.getOneHourTimeSliceDuration());
        sixHourDataReady = hasTimeSliceEnded(sixHourTimeSlice, configuration.getSixHourTimeSliceDuration());

        int count = 1;
        if (oneHourDataReady) {
            count++;
            oneHourIndexEntriesArrival = new CountDownLatch(1);
        }
        if (sixHourDataReady) {
            count++;
            sixHourIndexEntriesArrival = new CountDownLatch(1);
        }

        allAggregationFinished = new CountDownLatch(count);
    }

    private DateTime getSixHourTimeSlice() {
        return dtService.getTimeSlice(startTime, configuration.getSixHourTimeSliceDuration());
    }

    private DateTime getOneHourTimeSlice() {
        return dtService.getTimeSlice(startTime, configuration.getOneHourTimeSliceDuration());
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
                log.warn("Failed to retrieve raw data index entries for time slice [" + startTime +
                    "]. Data aggregation cannot proceed.", t);
            }
        }, workers);

        if (oneHourDataReady) {
            StorageResultSetFuture oneHourFuture = dao.findMetricsIndexEntriesAsync(MetricsTable.SIX_HOUR,
                startTime.getMillis());
            Futures.addCallback(oneHourFuture, new FutureCallback<ResultSet>() {
                @Override
                public void onSuccess(ResultSet result) {
                    List<Row> rows = result.all();
                    oneHourIndexEntries = new ConcurrentHashMap<Integer, MetricsIndexEntry>(rows.size());
                    MetricsIndexEntryMapper mapper = new MetricsIndexEntryMapper(MetricsTable.SIX_HOUR);

                    for (Row row : result) {
                        MetricsIndexEntry indexEntry = mapper.map(row);
                        oneHourIndexEntries.put(indexEntry.getScheduleId(), indexEntry);
                    }
                    remainingOneHourData = new AtomicInteger(oneHourIndexEntries.size());
                    oneHourIndexEntriesArrival.countDown();
                }

                @Override
                public void onFailure(Throwable t) {
                    log.warn("Failed to retrieve one hour aggregate index entries for time slice [" + startTime +
                        "]. Some six hour aggregates may not get generated.");
                }
            });
        }

        allAggregationFinished.await();

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
                        DateTime sixHourTimeSlice = dtService.getTimeSlice(new DateTime(oneHourAggregate.getTimestamp()),
                            configuration.getOneHourTimeSliceDuration());
                        StorageResultSetFuture indexFuture = dao.updateMetricsIndex(MetricsTable.SIX_HOUR,
                            oneHourAggregate.getScheduleId(), sixHourTimeSlice.getMillis());
                        Futures.addCallback(indexFuture, new FutureCallback<ResultSet>() {
                            @Override
                            public void onSuccess(ResultSet result) {
                                updateRemainingRawDataCount();
                                try {
                                    if (oneHourDataReady) {
                                        oneHourIndexEntriesArrival.await();
                                        calculateAggregate(indexEntry.getScheduleId(), MetricsTable.SIX_HOUR);
                                    }
                                } catch (InterruptedException e) {
                                    log.warn("An interrupt occurred while waiting one hour data index entries. " +
                                        "No 6 hour data will be generated for " + oneHourAggregate.getScheduleId() +
                                        " for time slice starting at " + getOneHourTimeSlice());
                                }
                            }

                            @Override
                            public void onFailure(Throwable t) {
                                log.warn("Failed to update 6 hour data index for one hour aggregate " +
                                    oneHourAggregate);
                                updateRemainingRawDataCount();
                            }
                        });
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

    private void calculateAggregate(final int scheduleId, final MetricsTable toTable) {
        StorageResultSetFuture future;
        final DateTime startTime;
        final DateTime endTime;

        if (toTable == MetricsTable.SIX_HOUR) {
            startTime = getOneHourTimeSlice();
            endTime = startTime.plus(configuration.getOneHourTimeSliceDuration());
            future = dao.findOneHourMetricsAsync(scheduleId, startTime.getMillis(), endTime.getMillis());
        } else { // toTable == 24 hr table
            startTime = getSixHourTimeSlice();
            endTime = startTime.plus(configuration.getSixHourTimeSliceDuration());
            future = dao.findSixHourMetricsAsync(scheduleId, startTime.getMillis(), endTime.getMillis());
        }

        Futures.addCallback(future, new FutureCallback<ResultSet>() {
            @Override
            public void onSuccess(ResultSet result) {
                AggregateNumericMetricMapper mapper = new AggregateNumericMetricMapper();
                List<AggregateNumericMetric> metrics = mapper.mapAll(result);
                final AggregateNumericMetric aggregate = calculateAggregate(metrics, startTime.getMillis());

                if (toTable == MetricsTable.SIX_HOUR) {
                    ListenableFuture<List<ResultSet>> insertFuture = Futures.allAsList(
                        dao.insertSixHourDataAsync(aggregate.getScheduleId(), startTime.getMillis(), AggregateType.MIN,
                            aggregate.getMin()),
                        dao.insertSixHourDataAsync(aggregate.getScheduleId(), startTime.getMillis(), AggregateType.MAX,
                            aggregate.getMax()),
                        dao.insertSixHourDataAsync(aggregate.getScheduleId(), startTime.getMillis(), AggregateType.AVG,
                            aggregate.getAvg())
                    );
                    Futures.addCallback(insertFuture, new FutureCallback<List<ResultSet>>() {
                        @Override
                        public void onSuccess(List<ResultSet> result) {
                            if (result.get(0) == null) {
                                log.warn("Failed to store the minimum of the 6 hour aggregate " + aggregate);
                            }
                            if (result.get(1) == null) {
                                log.warn("Failed to store the maximum of the 6 hour aggregate " + aggregate);
                            }
                            if (result.get(2) == null) {
                                log.warn("Failed to store the average of the 6 hour aggregate " + aggregate);
                            }
                            DateTime twentyFourHourTimeSlice = dtService.getTimeSlice(new DateTime(
                                aggregate.getTimestamp()), configuration.getSixHourTimeSliceDuration());
                            StorageResultSetFuture indexFuture = dao.updateMetricsIndex(MetricsTable.TWENTY_FOUR_HOUR,
                                aggregate.getScheduleId(), twentyFourHourTimeSlice.getDayOfMonth());
                            Futures.addCallback(indexFuture, new FutureCallback<ResultSet>() {
                                @Override
                                public void onSuccess(ResultSet result) {
                                    oneHourIndexEntries.remove(aggregate.getScheduleId());
                                    updateRemainingOneHourDataCount();
                                    // TODO compute 24 hr aggregate if necessary
                                }

                                @Override
                                public void onFailure(Throwable t) {
                                    log.warn("Failed to update 24 hour data index for 6 hour aggregate " + aggregate);
                                    oneHourIndexEntries.remove(aggregate.getScheduleId());
                                    updateRemainingOneHourDataCount();
                                }
                            });
                        }

                        @Override
                        public void onFailure(Throwable t) {
                            log.warn("Failed to store six hour aggregate " + aggregate);
                            oneHourIndexEntries.remove(aggregate.getScheduleId());
                            updateRemainingOneHourDataCount();
                        }
                    });
                } else {
                    // TODO handle 6 hr data
                }
            }

            @Override
            public void onFailure(Throwable t) {
                if (toTable == MetricsTable.SIX_HOUR) {
                    log.warn("Failed to retrieve one hour data for scheduleId " + scheduleId + ". 6 hour aggregates " +
                        "for time slice [startTime: " + startTime + ", endTime: " + endTime + "] will not be generated.");
                } else {
                    log.warn("Failed to retrieve six hour data for scheduleId " + scheduleId + ". 24 hour aggregates " +
                        "for time slice [startTime: " + startTime + ", endTime: " + endTime + "] will not be generated.");
                }
            }
        });
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

        return new AggregateNumericMetric(rawMetrics.get(0).getScheduleId(), mean.getArithmeticMean(), min, max,
            timestamp);
    }

    private AggregateNumericMetric calculateAggregate(List<AggregateNumericMetric> metrics, long timestamp) {
        double min = Double.NaN;
        double max = min;
        int count = 0;
        ArithmeticMeanCalculator mean = new ArithmeticMeanCalculator();

        for (AggregateNumericMetric metric : metrics) {
            if (count == 0) {
                min = metric.getMin();
                max = metric.getMax();
            }
            if (metric.getMin() < min) {
                min = metric.getMin();
            } else if (metric.getMax() > max) {
                max = metric.getMax();
            }
            mean.add(metric.getAvg());
            ++count;
        }

        return new AggregateNumericMetric(metrics.get(0).getScheduleId(), mean.getArithmeticMean(), min, max,
            timestamp);
    }

    private void updateRemainingRawDataCount() {
        if (remainingRawData.decrementAndGet() == 0) {
            if (log.isDebugEnabled()) {
                log.debug("Finished aggregating raw data for time slice [" + startTime + "]");
            }
            deleteIndexEntries(MetricsTable.ONE_HOUR);
        }
    }

    private void updateRemainingOneHourDataCount() {
        if (remainingOneHourData.decrementAndGet() == 0) {
            if (log.isDebugEnabled()) {
                DateTime start = getOneHourTimeSlice();
                DateTime end = start.plus(configuration.getOneHourTimeSliceDuration());
                log.debug("Finished aggregating one hour data for time slice [startTime: " + start +
                    ", endTime: " + end + "]");
            }
            deleteIndexEntries(MetricsTable.SIX_HOUR);
        }
    }

    private void deleteIndexEntries(final MetricsTable table) {
        StorageResultSetFuture future = dao.deleteMetricsIndexEntriesAsync(table, startTime.getMillis());
        Futures.addCallback(future, new FutureCallback<ResultSet>() {
            @Override
            public void onSuccess(ResultSet result) {
                allAggregationFinished.countDown();
            }

            @Override
            public void onFailure(Throwable t) {
                log.warn("Failed to delete index entries for table " + table + " at time [" + startTime + "]");
                allAggregationFinished.countDown();
            }
        });
    }

}
