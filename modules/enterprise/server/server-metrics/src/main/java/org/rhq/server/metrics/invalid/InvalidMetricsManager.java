package org.rhq.server.metrics.invalid;

import static java.util.Arrays.asList;
import static org.rhq.server.metrics.domain.MetricsTable.ONE_HOUR;
import static org.rhq.server.metrics.domain.MetricsTable.SIX_HOUR;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.joda.time.DateTime;

import org.rhq.server.metrics.ArithmeticMeanCalculator;
import org.rhq.server.metrics.DateTimeService;
import org.rhq.server.metrics.MetricsConfiguration;
import org.rhq.server.metrics.MetricsDAO;
import org.rhq.server.metrics.domain.AggregateNumericMetric;
import org.rhq.server.metrics.domain.Bucket;
import org.rhq.server.metrics.domain.MetricsTable;
import org.rhq.server.metrics.domain.RawNumericMetric;

/**
 * <p>
 * This class tries to deal with invalid aggregate metrics. An invalid metric is one where
 * either min > avg or max < avg. There are a couple different bugs which made these
 * situations possible See https://bugzilla.redhat.com/show_bug.cgi?id=1110462 and
 * https://bugzilla.redhat.com/show_bug.cgi?id=1104885 for details.
 * </p>
 *
 * <p>
 * When an invalid metric is found, it is {@link #submit(AggregateNumericMetric) submitted}
 * to an internal queue for later processing. Metrics will be recomputed if possible;
 * otherwise, they will be deleted.
 * </p>
 *
 * @author John Sanda
 */
public class InvalidMetricsManager {

    private static final Log log = LogFactory.getLog(InvalidMetricsManager.class);

    private static final double THRESHOLD = 0.00001d;

    private DateTimeService dateTimeService;

    private MetricsDAO dao;

    private InvalidMetric current;

    private MetricsConfiguration configuration;

    private DelayQueue<InvalidMetric> queue;

    private ScheduledExecutorService executor;

    /**
     * The queue delay is specified in milliseconds and defaults to 10 minutes. This is the
     * amount of time an invalid metric has to wait in the queue before it can be removed
     * and processed.
     */
    private long delay = Long.parseLong(System.getProperty("rhq.metrics.invalid.queue-delay", "600000"));

    private boolean isShutdown;

    public InvalidMetricsManager(DateTimeService dateTimeService, MetricsDAO dao) {
        this(dateTimeService, dao, Integer.parseInt(System.getProperty("rhq.metrics.invalid.poller.initial-delay",
                "300")), Integer.parseInt(System.getProperty("rhq.metrics.invalid.poller.period", "300")));
    }

    InvalidMetricsManager(DateTimeService dateTimeService, MetricsDAO dao, int pollerDelay, int pollerPeriod) {
        this.dateTimeService = dateTimeService;
        this.dao = dao;
        configuration = new MetricsConfiguration();
        queue = new DelayQueue<InvalidMetric>();
        executor = Executors.newSingleThreadScheduledExecutor();

        executor.scheduleAtFixedRate(new InvalidMetricRunnable(), pollerDelay, pollerPeriod, TimeUnit.SECONDS);
    }

    /**
     * This is a test hook.
     *
     * @param delay The queue delay
     */
    void setDelay(long delay) {
        this.delay = delay;
    }

    /**
     * Shuts down the executor, waiting for any in progress work to finish. Any invalid
     * metrics that are in the queue will not be processed.
     */
    public void shutdown() {
        log.info("Shutting down...");
        isShutdown = true;
        queue.clear();
        executor.shutdown();
    }

    /**
     * Submits an invalid metric for later processing which is done in a separate thread.
     * The queue actually stores instances of {@link InvalidMetric}. Metrics belonging to
     * the same measurement schedule and from the same day will only have one invalid metric
     * stored in the queue. In other words, suppose a 1 hour metric from 14:00 is submitted.
     * Then a 6 hour metric from 12:00 for the same schedule id is submitted. This will result
     * in only one {@link InvalidMetric} stored stored in the queue. The reason being that
     * we need to look at all of the 1 hour, 6 hour, and 24 hour metrics; so, multiple
     * entries in the queue for the same day will only result in duplicate work.
     *
     * @param metric The invalid metric where invalid means either min > avg or max < avg
     * @return true if an invalid metric is added to the queue, false otherwise
     */
    public boolean submit(AggregateNumericMetric metric) {
        DateTime day = dateTimeService.getTimeSlice(new DateTime(metric.getTimestamp()),
            configuration.getSixHourTimeSliceDuration());
        InvalidMetric invalidMetric = new InvalidMetric(day, metric, delay);

        if (isShutdown) {
            log.info(invalidMetric + " will not be submitted since we are already shutdown.");
            return false;
        }

        if (queue.contains(invalidMetric) || (current != null && current.equals(invalidMetric))) {
            log.info(invalidMetric + " is already in the queue. It will not be resubmitted.");
            return false;
        }

        log.info("Adding " + invalidMetric + " to queue for processing");

        queue.offer(invalidMetric);

        return true;
    }

    /**
     * This is a test hook
     *
     * @return The queue of invalid metrics
     */
    public DelayQueue<InvalidMetric> getQueue() {
        return queue;
    }

    /**
     * A test hook that returns the count of invalid metrics including the current one being
     * worked on.
     */
    int getRemainingInvalidMetrics() {
        return current == null ? queue.size() : queue.size() + 1;
    }

    /**
     * When this runs, it drains all invalid metrics (whose delays have expired) from the
     * queue and will recompute and remove each metric.
     */
    private class InvalidMetricRunnable implements Runnable {

        @Override
        public void run() {
            List<InvalidMetric> invalidMetrics = new ArrayList<InvalidMetric>(queue.size());
            queue.drainTo(invalidMetrics);

            for (InvalidMetric invalidMetric : invalidMetrics) {
                current = invalidMetric;
                try {
                    handleInvalidMetric(current);
                } catch (Exception e) {
                    log.warn("An unexpected occurred while processing invalid metric " + current, e);
                }
            }
            current = null;
        }
    }

    private void handleInvalidMetric(InvalidMetric invalidMetric) {
        log.info("Attempting to fix " + invalidMetric +
            ". This may include updates to 1 hour, 6 hour, and 24 hour metrics.");

        if (invalidMetric.metric.getBucket() == Bucket.TWENTY_FOUR_HOUR) {
            update24HourMetric(invalidMetric);
        } else if (invalidMetric.metric.getBucket() == Bucket.SIX_HOUR) {
            if (DateTime.now().isAfter(invalidMetric.day.plusDays(1))) {
                update24HourMetric(invalidMetric);
            } else {
                update6HourMetrics(asList(invalidMetric.metric));
            }
        } else { // 1 hr metric
            DateTime sixHourTimeSlice = dateTimeService.getTimeSlice(new DateTime(invalidMetric.metric.getTimestamp()),
                configuration.getOneHourTimeSliceDuration());

            if (DateTime.now().isAfter(invalidMetric.day.plusDays(1))) {
                update24HourMetric(invalidMetric);
            } else if (DateTime.now().isAfter(sixHourTimeSlice.plusHours(6))) {
                List<AggregateNumericMetric> sixHourMetrics = dao.findAggregateMetrics(
                    invalidMetric.metric.getScheduleId(), Bucket.SIX_HOUR, invalidMetric.day.getMillis(),
                    invalidMetric.day.plusDays(1).getMillis());
                update6HourMetrics(sixHourMetrics);
            } else {
                update1HourMetrics(asList(invalidMetric.metric));
            }
        }
    }

    /**
     * This method first looks for the 6 hour metrics from which the 24 hour metric was
     * computed. If no 6 hour metrics are found, the 24 hour metric is deleted. If we
     * find 6 hour metrics, we then try to update the corresponding 1 hour metrics
     * followed by the 6 hour metrics, and lastly recompute the 24 hour metric.
     */
    private void update24HourMetric(InvalidMetric invalidMetric) {
        List<AggregateNumericMetric> sixHourMetrics = dao.findAggregateMetrics(invalidMetric.metric.getScheduleId(),
            Bucket.SIX_HOUR, invalidMetric.day.getMillis(), invalidMetric.day.plusDays(1).getMillis());

        if (sixHourMetrics.isEmpty()) {
            // This likely means that the 6 hour data has already expired. The best
            // we can do at this point is to delete to the invalid metric.
            log.info("Deleting " + invalidMetric + " since the 6 hour metrics are no longer available.");
            remove24HourMetric(invalidMetric.metric);
        } else {
            List<AggregateNumericMetric> updated6HourMetrics = update6HourMetrics(sixHourMetrics);
            AggregateNumericMetric recomputed24HourMetric = computeAggregate(updated6HourMetrics,
                invalidMetric.metric.getScheduleId(), invalidMetric.day.getMillis(), Bucket.TWENTY_FOUR_HOUR);
            persist24HourMetric(recomputed24HourMetric);

            log.info(invalidMetric + " has been recomputed with a new value of " + getValueText(recomputed24HourMetric));
        }
    }

    /**
     * This method first looks for and deletes any empty 6 hour metrics. It then looks
     * for any invalid metrics. For each invalid metric, it tries to recompute the
     * metric if the 1 hour data is available. If the 1 hour data is available it too
     * is updated as necessary, and then the 6 hour metric is recomputed and persisted.
     * If the 1 hour data is no longer available, the 6 hour metrics is deleted.
     *
     * @param sixHourMetrics The 6 hour metrics to update
     * @return
     */
    private List<AggregateNumericMetric> update6HourMetrics(final List<AggregateNumericMetric> sixHourMetrics) {
        List<AggregateNumericMetric> updated6HourMetrics = removeEmpty6HourMetrics(sixHourMetrics);
        List<AggregateNumericMetric> invalid6HourMetrics = findInvalidMetrics(updated6HourMetrics);

        for (AggregateNumericMetric invalid6HourMetric : invalid6HourMetrics) {
            List<AggregateNumericMetric> oneHourMetrics = dao.findAggregateMetrics(invalid6HourMetric.getScheduleId(),
                Bucket.ONE_HOUR, invalid6HourMetric.getTimestamp(),
                new DateTime(invalid6HourMetric.getTimestamp()).plusHours(6).getMillis());

            if (oneHourMetrics.isEmpty()) {
                // This likely means that the 1 hour data has already expired. The best
                // we can do at this point is to delete the invalid metric.
                log.info("Deleting 6 hour metric " + invalid6HourMetric +
                    " since the 1 hour metrics are no longer available.");
                updated6HourMetrics = remove6HourMetric(invalid6HourMetric, sixHourMetrics);
            } else {
                // Since we have 1 hour metrics, we want to first inspect and update
                // them as best we can. Then we go ahead and recompute and persist the
                // new 6 hour metric.
                List<AggregateNumericMetric> updated1HourMetrics = update1HourMetrics(oneHourMetrics);
                AggregateNumericMetric recomputed6HourMetric = computeAggregate(updated1HourMetrics,
                    invalid6HourMetric.getScheduleId(), invalid6HourMetric.getTimestamp(), Bucket.SIX_HOUR);
                updated6HourMetrics = replace6HourMetric(invalid6HourMetric, recomputed6HourMetric,
                    sixHourMetrics);

                log.info("The invalid 6 hour metric " + invalid6HourMetric +
                    " has been recomputed with a new value of " + getValueText(recomputed6HourMetric));
            }
        }

        return updated6HourMetrics;
    }

    /**
     * This method first looks for and deletes any empty 1 hour metrics. It then looks
     * for any invalid metrics. For each invalid metric, we recompute and persist the
     * metric if the raw data is still available; otherwise, we delete the metric.
     *
     * @param oneHourMetrics The 1 hour metrics to update
     * @return The updated metrics which includes only those metric that have been
     * persisted and not deleted.
     */
    private List<AggregateNumericMetric> update1HourMetrics(final List<AggregateNumericMetric> oneHourMetrics) {
        List<AggregateNumericMetric> updated1HourMetrics = removeEmpty1HourMetrics(oneHourMetrics);
        List<AggregateNumericMetric> invalid1HourMetrics = findInvalidMetrics(updated1HourMetrics);

        for (AggregateNumericMetric invalid1HourMetric : invalid1HourMetrics) {
            // Try to recompute the 1 hour metric. If the raw data is gone, then we
            // simply delete the invalid 1 hour metric; otherwise, we persist the
            // recomputed aggregate.
            AggregateNumericMetric recomputed1HourMetric = recompute1HourAggregateIfPossible(invalid1HourMetric);
            if (recomputed1HourMetric == null) {
                log.info("Deleting 1 hour metric " + invalid1HourMetric + " since the raw data is no longer available.");
                updated1HourMetrics = remove1HourMetric(invalid1HourMetric, updated1HourMetrics);
            } else {
                updated1HourMetrics = replace1HourMetric(invalid1HourMetric, recomputed1HourMetric,
                    updated1HourMetrics);

                log.info("The invalid 1 hour metric " + invalid1HourMetric +
                    " has been recomputed with a new value of " + getValueText(recomputed1HourMetric));
            }
        }

        return updated1HourMetrics;
    }

    /**
     * Filters out empty aggregate metrics and deletes them from the database.
     *
     * @param metrics The metrics to search
     * @return A new collection containing non-empty aggregate metrics. The original collection is not modified.
     */
    private List<AggregateNumericMetric> removeEmpty6HourMetrics(List<AggregateNumericMetric> metrics) {
        List<AggregateNumericMetric> nonEmptyMetrics = new ArrayList<AggregateNumericMetric>();
        for (AggregateNumericMetric metric : metrics) {
            if (isEmptyMetric(metric)) {
                dao.deleteAggregate(metric);
            } else {
                nonEmptyMetrics.add(metric);
            }
        }

        return nonEmptyMetrics;
    }

    /**
     * Filters out empty aggregate metrics and deletes them from the database.
     *
     * @param metrics The metrics to search
     * @return A new collection containing non-empty aggregate metrics. The original collection is not modified.
     */
    private List<AggregateNumericMetric> removeEmpty1HourMetrics(List<AggregateNumericMetric> metrics) {
        List<AggregateNumericMetric> nonEmptyMetrics = new ArrayList<AggregateNumericMetric>();
        for (AggregateNumericMetric metric : metrics) {
            if (isEmptyMetric(metric)) {
                dao.deleteAggregate(metric);
            } else {
                nonEmptyMetrics.add(metric);
            }
        }

        return nonEmptyMetrics;
    }

    private boolean isEmptyMetric(AggregateNumericMetric metric) {
        return metric.getMin().equals(Double.NaN) && metric.getMax().equals(Double.NaN) &&
            metric.getAvg().equals(0d);
    }

    private List<AggregateNumericMetric> findInvalidMetrics(List<AggregateNumericMetric> metrics) {
        List<AggregateNumericMetric> invalidMetrics = new ArrayList<AggregateNumericMetric>();
        for (AggregateNumericMetric metric : metrics) {
            if (isInvalidMetric(metric)) {
                invalidMetrics.add(metric);
            }
        }

        return invalidMetrics;
    }

    public boolean isInvalidMetric(AggregateNumericMetric metric) {
        return (metric.getMax() < metric.getAvg() && Math.abs(metric.getMax() - metric.getAvg()) > THRESHOLD) ||
            (metric.getMin() > metric.getAvg() && Math.abs(metric.getMin() - metric.getAvg()) > THRESHOLD) ||
            (Double.isNaN(metric.getAvg()) || Double.isNaN(metric.getMin()) || Double.isNaN(metric.getMin()));
    }

    /**
     * This method recomputes the 1 hour aggregate if the raw data is still available.

     * @param metric The original 1 hour aggregate metric
     * @return The new 1 hour aggregate or <code>null</code> if the raw data is no longer available
     */
    private AggregateNumericMetric recompute1HourAggregateIfPossible(AggregateNumericMetric metric) {
        List<RawNumericMetric> rawMetrics = dao.findRawMetrics(metric.getScheduleId(), metric.getTimestamp(),
            new DateTime(metric.getTimestamp()).plusHours(1).getMillis());
        if (!rawMetrics.isEmpty()) {
            return computeAggregateFromRaw(rawMetrics, metric);
        } else {
            return null;
        }
    }

    private AggregateNumericMetric computeAggregateFromRaw(List<RawNumericMetric> rawMetrics,
        AggregateNumericMetric metric) {
        double min = Double.NaN;
        double max = min;
        int count = 0;
        ArithmeticMeanCalculator mean = new ArithmeticMeanCalculator();
        double value;

        for (RawNumericMetric rawMetric : rawMetrics) {
            value = rawMetric.getValue();
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
        return new AggregateNumericMetric(metric.getScheduleId(), metric.getBucket(), mean.getArithmeticMean(), min,
            max, metric.getTimestamp());
    }

    private AggregateNumericMetric computeAggregate(List<AggregateNumericMetric> metrics, int scheduleId,
        long timestamp, Bucket bucket) {
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
            }
            if (metric.getMax() > max) {
                max = metric.getMax();
            }
            mean.add(metric.getAvg());
            ++count;
        }

        // We let the caller handle setting the schedule id because in some cases we do
        // not care about it.
        return new AggregateNumericMetric(scheduleId, bucket, mean.getArithmeticMean(), min, max, timestamp);
    }

    private List<AggregateNumericMetric> remove1HourMetric(AggregateNumericMetric metric,
        List<AggregateNumericMetric> metrics) {
        return removeMetric(metric, metrics);
    }

    private List<AggregateNumericMetric> remove6HourMetric(AggregateNumericMetric metric,
        List<AggregateNumericMetric> metrics) {
        return removeMetric(metric, metrics);
    }

    private void remove24HourMetric(AggregateNumericMetric metric) {
        dao.deleteAggregate(metric);
    }

    private List<AggregateNumericMetric> replace1HourMetric(AggregateNumericMetric metric,
        AggregateNumericMetric newMetric, List<AggregateNumericMetric> metrics) {
        return replaceMetric(metric, newMetric, metrics, ONE_HOUR);
    }

    private List<AggregateNumericMetric> replace6HourMetric(AggregateNumericMetric metric,
        AggregateNumericMetric newMetric, List<AggregateNumericMetric> metrics) {
        return replaceMetric(metric, newMetric, metrics, SIX_HOUR);
    }

    private List<AggregateNumericMetric> removeMetric(AggregateNumericMetric metric,
        List<AggregateNumericMetric> metrics) {

        dao.deleteAggregate(metric);
        List<AggregateNumericMetric> updatedMetrics = new ArrayList<AggregateNumericMetric>(metrics);
        updatedMetrics.remove(metric);

        return updatedMetrics;
    }

    private List<AggregateNumericMetric> replaceMetric(AggregateNumericMetric metric,
        AggregateNumericMetric newMetric, List<AggregateNumericMetric> metrics, MetricsTable type) {

        switch (type) {
        case ONE_HOUR:
            persist1HourMetric(newMetric);
            break;
        case SIX_HOUR:
            persist6HourMetric(newMetric);
            break;
        default:
            throw new IllegalArgumentException(type + " cannot be used for this method");
        }

        List<AggregateNumericMetric> updatedMetrics = new ArrayList<AggregateNumericMetric>(metrics);
        updatedMetrics.remove(metric);
        updatedMetrics.add(newMetric);

        return updatedMetrics;
    }

    private void persist1HourMetric(AggregateNumericMetric metric) {
        dao.insert1HourData(metric).get();
    }

    private void persist6HourMetric(AggregateNumericMetric metric) {
        dao.insert6HourData(metric).get();
    }

    private void persist24HourMetric(AggregateNumericMetric metric) {
        dao.insert24HourData(metric).get();
    }

    private String getValueText(AggregateNumericMetric metric) {
        return "{max: " + metric.getMax() + ", min: " + metric.getMin() + ", avg: " + metric.getAvg() + "}";
    }

}
