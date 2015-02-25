package org.rhq.cassandra.schema;

import java.util.LinkedList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import com.google.common.util.concurrent.RateLimiter;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * @author John Sanda
 */
public class RateMonitor implements Runnable {

    private static final Log log = LogFactory.getLog(RateMonitor.class);

    private static class RequestStats {
        public int requests;
        public int failedRequests;

        public RequestStats(int requests, int failedRequests) {
            this.requests = requests;
            this.failedRequests = failedRequests;
        }
    }

    private static final double FAILURE_THRESHOLD = 0.01;

    private static final Boolean SUCCESS = Boolean.TRUE;

    private static final Boolean FAILURE = Boolean.FALSE;

    private LinkedList<RequestStats> recentStats = new LinkedList<RequestStats>();

    private LinkedList<Boolean> aggregatedStatsHistory = new LinkedList<Boolean>();

    private AtomicInteger requests = new AtomicInteger();

    private AtomicInteger failRequests = new AtomicInteger();

    private boolean shutdown;

    private int warmUp = MigrateAggregateMetrics.DEFAULT_WARM_UP;

    private AtomicReference<RateLimiter> readPermitsRef;

    private AtomicReference<RateLimiter> writePermitsRef;

    public RateMonitor(AtomicReference<RateLimiter> readPermitsRef, AtomicReference<RateLimiter> writePermitsRef) {
        this.readPermitsRef = readPermitsRef;
        this.writePermitsRef = writePermitsRef;
    }

    public void requestSucceeded() {
        requests.incrementAndGet();
    }

    public void requestFailed() {
        failRequests.incrementAndGet();
        requests.incrementAndGet();
    }

    public void shutdown() {
        shutdown = true;
    }

    @Override
    public void run() {
        while (!shutdown) {
            try {
                if (requests.get() == 0) {
                    continue;
                }
                recentStats.addFirst(new RequestStats(requests.getAndSet(0), failRequests.getAndSet(0)));
                aggregateStats();

                if (isRateDecreaseNeeded()) {
                    decreaseRates();
                    aggregatedStatsHistory.clear();
                } else if (aggregatedStatsHistory.peek() == FAILURE) {
                    increaseWarmup();
                } else if (isRateIncreaseNeeded()) {
                    increaseRates();
                    aggregatedStatsHistory.clear();
                }

                if (recentStats.size() > 5) {
                    recentStats.removeLast();
                }

                while (aggregatedStatsHistory.size() > 60) {
                    aggregatedStatsHistory.removeLast();
                }

                Thread.sleep(1000);
            } catch (InterruptedException e) {
                log.info("Stopping request monitoring due to interrupt", e);
            } catch (Exception e) {
                log.warn("There was an unexpected error", e);
            }
        }
    }

    private void aggregateStats() {
        int i = 0;
        int totalRequests = 0;
        int totalFailures = 0;

        for (RequestStats stats : recentStats) {
            if (i > 4) {
                break;
            }
            totalRequests += stats.requests;
            totalFailures += stats.failedRequests;
            ++i;
        }

        aggregatedStatsHistory.addFirst((totalFailures / totalRequests) < FAILURE_THRESHOLD);
    }

    private boolean isRateDecreaseNeeded() {
        if (aggregatedStatsHistory.size() > 30) {
            return false;
        }

        int i = 0;
        int failures = 0;

        for (Boolean result : aggregatedStatsHistory) {
            if (failures > 2 || i > 5) {
                break;
            }
            if (result == FAILURE) {
                ++failures;
            }
            ++i;
        }

        return failures > 2;
    }

    private boolean isRateIncreaseNeeded() {
        if (aggregatedStatsHistory.size() < 60) {
            // We want to make sure we have at least a minute's worth of stats in order to
            // decide if we should whether or not a rate increase is needed.
            return false;
        }

        int i = 0;
        for (Boolean result : aggregatedStatsHistory) {
            if (result == FAILURE) {
                return false;
            }
            if (i > 11) {
                break;
            }
            ++i;
        }
        return true;
    }

    private void decreaseRates() {
        double readRate = readPermitsRef.get().getRate();
        double newReadRate = readRate * 0.8;
        double writeRate = writePermitsRef.get().getRate();
        double newWriteRate = writeRate * 0.8;

        log.info("Decreasing read rate from " + readRate + " reads/sec to " + newReadRate + " reads/sec");
        log.info("Decreasing write rate from " + writeRate + " writes/sec to " + newWriteRate + " writes/sec");

        warmUp = MigrateAggregateMetrics.DEFAULT_WARM_UP;
        readPermitsRef.set(RateLimiter.create(newReadRate, warmUp, TimeUnit.SECONDS));
        writePermitsRef.set(RateLimiter.create(newWriteRate, warmUp, TimeUnit.SECONDS));
    }

    private void increaseRates() {
        double readRate = readPermitsRef.get().getRate();
        double newReadRate = readRate * 1.1;
        double writeRate = writePermitsRef.get().getRate();
        double newWriteRate = writeRate * 1.1;

        log.info("Increasing read rate from " + readRate + " reads/sec to " + newReadRate + " reads/sec");
        log.info("Increasing write rate from " + writeRate + " writes/sec to " + newWriteRate + " writes/sec");

        warmUp = MigrateAggregateMetrics.DEFAULT_WARM_UP;
        readPermitsRef.set(RateLimiter.create(newReadRate, warmUp, TimeUnit.SECONDS));
        writePermitsRef.set(RateLimiter.create(newWriteRate, warmUp, TimeUnit.SECONDS));
    }

    private void increaseWarmup() {
        warmUp *= 2;
        double readRate = readPermitsRef.get().getRate();
        double writeRate = writePermitsRef.get().getRate();

        log.info("Resetting read rate to " + readRate + " reads/sec with an increased warm up of " + warmUp + " sec");
        log.info("Resetting write rate to " + writeRate + " writes/sec with an increased warm up of " + warmUp +
            " sec");

        readPermitsRef.set(RateLimiter.create(readRate, warmUp, TimeUnit.SECONDS));
        writePermitsRef.set(RateLimiter.create(writeRate, warmUp, TimeUnit.SECONDS));
    }

}
