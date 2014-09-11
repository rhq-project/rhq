package org.rhq.server.metrics.aggregation;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import com.google.common.base.Stopwatch;
import com.google.common.util.concurrent.ListeningExecutorService;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.joda.time.DateTime;

import org.rhq.server.metrics.AbortedException;
import org.rhq.server.metrics.DateTimeService;
import org.rhq.server.metrics.MetricsConfiguration;
import org.rhq.server.metrics.MetricsDAO;
import org.rhq.server.metrics.domain.AggregateNumericMetric;
import org.rhq.server.metrics.domain.IndexBucket;

/**
 * This class is the driver for metrics aggregation.
 *
 * @author John Sanda
 */
public class AggregationManager {

    private static final Comparator<AggregateNumericMetric> AGGREGATE_COMPARATOR = new Comparator<AggregateNumericMetric>() {
        @Override
        public int compare(AggregateNumericMetric left, AggregateNumericMetric right) {
            if (left.getScheduleId() == right.getScheduleId()) {
                if (left.getTimestamp() < right.getTimestamp()) {
                    return -1;
                } else if (left.getTimestamp() > right.getTimestamp()) {
                    return 1;
                } else {
                    return 0;
                }
            }
            if (left.getScheduleId() < right.getScheduleId()) {
                return -1;
            }
            return 1;
        }
    };

    private final Log log = LogFactory.getLog(AggregationManager.class);

    private MetricsDAO dao;

    private DateTimeService dtService;

    private DateTime startTime;

    private DateTime endTime;

    private ListeningExecutorService aggregationTasks;

    private Set<AggregateNumericMetric> oneHourData;

    private Semaphore permits;

    private MetricsConfiguration configuration;

    public AggregationManager(ListeningExecutorService aggregationTasks, MetricsDAO dao, DateTimeService dtService,
        DateTime startTime, int batchSize, int parallelism, MetricsConfiguration configuration) {

        this.dao = dao;
        this.dtService = dtService;
        this.endTime = dtService.currentHour();
        this.startTime = startTime;
        oneHourData = new ConcurrentSkipListSet<AggregateNumericMetric>(AGGREGATE_COMPARATOR);
        permits = new Semaphore(batchSize * parallelism);
        this.aggregationTasks = aggregationTasks;
        this.configuration = configuration;
    }

    public Set<AggregateNumericMetric> run() {
        log.info("Starting aggregation for time slice " + startTime);
        Stopwatch stopwatch = new Stopwatch().start();
        int numRaw = 0;
        int num1Hour = 0;
        int num6Hour = 0;
        try {
            PersistFunctions persistFunctions = new PersistFunctions(dao, dtService);
            Map<IndexBucket, Integer> counts;
            DateTime end = endTime;
            DateTime start = endTime.minusDays(2);

            counts = createRawAggregator(persistFunctions).execute(start, end);
            numRaw += counts.get(IndexBucket.RAW);

            end = dtService.get6HourTimeSlice(endTime);
            start = dtService.get6HourTimeSlice(endTime).minusDays(7);
            counts = create1HourAggregator(persistFunctions).execute(start, end);
            num1Hour += counts.get(IndexBucket.ONE_HOUR);


            end = dtService.get24HourTimeSlice(endTime);
            start = dtService.get24HourTimeSlice(endTime).minusDays(14);
            counts = create6HourAggregator(persistFunctions).execute(start, end);
            num6Hour += counts.get(IndexBucket.SIX_HOUR);

            return oneHourData;
        } catch (InterruptedException e) {
            log.info("There was an interrupt while waiting for aggregation to finish. Aggregation will be aborted.");
            return Collections.emptySet();
        }
        catch (AbortedException e) {
            log.warn("Aggregation has been aborted: " + e.getMessage());
            return Collections.emptySet();
        } finally {
            stopwatch.stop();
            log.info("Finished aggregation of {\"raw schedules\": " + numRaw + ", \"1 hour schedules\": " + num1Hour +
                ", \"6 hour schedules\": " + num6Hour + "} in " + stopwatch.elapsed(TimeUnit.MILLISECONDS) + " ms");
        }
    }

    private DataAggregator createRawAggregator(PersistFunctions persistFunctions) {
        DataAggregator aggregator = new DataAggregator();
        aggregator.setAggregationTasks(aggregationTasks);
        aggregator.setBucket(IndexBucket.RAW);
        aggregator.setTimeSliceDuration(configuration.getRawTimeSliceDuration());
        aggregator.setDao(dao);
        aggregator.setPermits(permits);
        aggregator.setDateTimeService(dtService);
        aggregator.setPersistMetrics(persistFunctions.persist1HourMetrics());
        aggregator.setBatchFinishedListener(new DataAggregator.BatchFinishedListener() {
            @Override
            public void onFinish(List<AggregateNumericMetric> metrics) {
                oneHourData.addAll(metrics);
            }
        });
        aggregator.setConfiguration(configuration);

        return aggregator;
    }

    private DataAggregator create1HourAggregator(PersistFunctions persistFunctions) {
        DataAggregator aggregator = new DataAggregator();
        aggregator.setAggregationTasks(aggregationTasks);
        aggregator.setBucket(IndexBucket.ONE_HOUR);
        aggregator.setTimeSliceDuration(configuration.getOneHourTimeSliceDuration());
        aggregator.setDao(dao);
        aggregator.setPermits(permits);
        aggregator.setDateTimeService(dtService);
        aggregator.setPersistMetrics(persistFunctions.persist6HourMetrics());
        aggregator.setConfiguration(configuration);

        return aggregator;
    }

    private DataAggregator create6HourAggregator(PersistFunctions persistFunctions) {
        DataAggregator aggregator = new DataAggregator();
        aggregator.setAggregationTasks(aggregationTasks);
        aggregator.setBucket(IndexBucket.SIX_HOUR);
        aggregator.setTimeSliceDuration(configuration.getSixHourTimeSliceDuration());
        aggregator.setDao(dao);
        aggregator.setPermits(permits);
        aggregator.setDateTimeService(dtService);
        aggregator.setPersistMetrics(persistFunctions.persist24HourMetrics());
        aggregator.setConfiguration(configuration);

        return aggregator;
    }

}
