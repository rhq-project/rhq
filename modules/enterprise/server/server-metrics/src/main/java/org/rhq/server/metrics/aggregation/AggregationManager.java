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
import org.rhq.server.metrics.domain.Bucket;

/**
 * This class is the driver for metrics aggregation.
 *
 * @author John Sanda
 */
public class AggregationManager {

    private static final Comparator<AggregateNumericMetric> AGGREGATE_COMPARATOR = new Comparator<AggregateNumericMetric>() {
        @Override
        public int compare(AggregateNumericMetric left, AggregateNumericMetric right) {
            return (left.getScheduleId() < right.getScheduleId()) ? -1 : ((left.getScheduleId() == right.getScheduleId()) ? 0 : 1);
        }
    };

    private final Log log = LogFactory.getLog(AggregationManager.class);

    private MetricsDAO dao;

    private DateTimeService dtService;

    private DateTime startTime;

    private ListeningExecutorService aggregationTasks;

    private Set<AggregateNumericMetric> oneHourData;

    private Semaphore permits;

    private MetricsConfiguration configuration;

    public AggregationManager(ListeningExecutorService aggregationTasks, MetricsDAO dao, DateTimeService dtService,
        DateTime startTime, int batchSize, int parallelism, MetricsConfiguration configuration) {

        this.dao = dao;
        this.dtService = dtService;
        this.startTime = startTime;
        oneHourData = new ConcurrentSkipListSet<AggregateNumericMetric>(AGGREGATE_COMPARATOR);
        permits = new Semaphore(batchSize * parallelism);
        this.aggregationTasks = aggregationTasks;
        this.configuration = configuration;
    }

    private boolean is6HourTimeSliceFinished() {
        return dtService.is6HourTimeSliceFinished(startTime);
    }

    private boolean is24HourTimeSliceFinished() {
        return dtService.is24HourTimeSliceFinished(startTime);
    }

    public Set<AggregateNumericMetric> run() {
        log.info("Starting aggregation for time slice " + startTime);
        Stopwatch stopwatch = new Stopwatch().start();
        int numRaw = 0;
        int num1Hour = 0;
        int num6Hour = 0;
        try {
            PersistFunctions persistFunctions = new PersistFunctions(dao, dtService);

            Map<AggregationType, Integer> counts = createPastDataAggregator(persistFunctions).execute();
            numRaw += counts.get(AggregationType.RAW);
            num1Hour += counts.get(AggregationType.ONE_HOUR);
            num6Hour += counts.get(AggregationType.SIX_HOUR);

            counts = createRawAggregator(persistFunctions).execute();
            numRaw += counts.get(AggregationType.RAW);

            if (is6HourTimeSliceFinished()) {
                counts = create1HourAggregator(persistFunctions).execute();
                num1Hour += counts.get(AggregationType.ONE_HOUR);
            }
            if (is24HourTimeSliceFinished()) {
                counts = create6HourAggregator(persistFunctions).execute();
                num6Hour += counts.get(AggregationType.SIX_HOUR);
            }

            for (AggregateNumericMetric metric : oneHourData) {
                metric.setBucket(Bucket.ONE_HOUR);
            }

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

    private PastDataAggregator createPastDataAggregator(PersistFunctions persistFunctions) {
        PastDataAggregator aggregator = new PastDataAggregator();
        aggregator.setAggregationTasks(aggregationTasks);
        aggregator.setAggregationType(AggregationType.RAW);
        aggregator.setDao(dao);
        aggregator.setPermits(permits);
        aggregator.setStartTime(startTime);
        aggregator.setDateTimeService(dtService);
        aggregator.setPersistFns(persistFunctions);
        aggregator.setPersistMetrics(persistFunctions.persist1HourMetrics());
        aggregator.setConfiguration(configuration);


        return aggregator;
    }

    private DataAggregator createRawAggregator(PersistFunctions persistFunctions) {
        DataAggregator aggregator = new DataAggregator();
        aggregator.setAggregationTasks(aggregationTasks);
        aggregator.setAggregationType(AggregationType.RAW);
        aggregator.setDao(dao);
        aggregator.setPermits(permits);
        aggregator.setStartTime(startTime);
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
        aggregator.setAggregationType(AggregationType.ONE_HOUR);
        aggregator.setDao(dao);
        aggregator.setPermits(permits);
        aggregator.setStartTime(dtService.get6HourTimeSlice(startTime));
        aggregator.setDateTimeService(dtService);
        aggregator.setPersistMetrics(persistFunctions.persist6HourMetrics());
        aggregator.setConfiguration(configuration);

        return aggregator;
    }

    private DataAggregator create6HourAggregator(PersistFunctions persistFunctions) {
        DataAggregator aggregator = new DataAggregator();
        aggregator.setAggregationTasks(aggregationTasks);
        aggregator.setAggregationType(AggregationType.SIX_HOUR);
        aggregator.setDao(dao);
        aggregator.setPermits(permits);
        aggregator.setStartTime(dtService.get24HourTimeSlice(startTime));
        aggregator.setDateTimeService(dtService);
        aggregator.setPersistMetrics(persistFunctions.persist24HourMetrics());
        aggregator.setConfiguration(configuration);

        return aggregator;
    }

}
