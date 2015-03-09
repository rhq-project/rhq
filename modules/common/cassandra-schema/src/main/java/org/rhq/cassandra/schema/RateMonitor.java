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
        public double requests;
        public double failedRequests;

        public RequestStats(double requests, double failedRequests) {
            this.requests = requests;
            this.failedRequests = failedRequests;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            RequestStats that = (RequestStats) o;

            if (Double.compare(that.failedRequests, failedRequests) != 0) return false;
            if (Double.compare(that.requests, requests) != 0) return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result;
            long temp;
            temp = Double.doubleToLongBits(requests);
            result = (int) (temp ^ (temp >>> 32));
            temp = Double.doubleToLongBits(failedRequests);
            result = 31 * result + (int) (temp ^ (temp >>> 32));
            return result;
        }
    }

    private static class AggregateRequestStats {
        public boolean thresholdExceeded;
        public double failedRequests;

        public AggregateRequestStats(boolean thresholdExceeded, double failedRequests) {
            this.thresholdExceeded = thresholdExceeded;
            this.failedRequests = failedRequests;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            AggregateRequestStats that = (AggregateRequestStats) o;

            if (failedRequests != that.failedRequests) return false;
            if (thresholdExceeded != that.thresholdExceeded) return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result;
            long temp;
            result = (thresholdExceeded ? 1 : 0);
            temp = Double.doubleToLongBits(failedRequests);
            result = 31 * result + (int) (temp ^ (temp >>> 32));
            return result;
        }
    }

    private static final double FAILURE_THRESHOLD = 0.01;

    private static final double MIN_READ_RATE = 25.0;

    private static final double MIN_WRITE_RATE = 2500;

    private static final double DEFAULT_WRITE_RATE_STEP_INCREASE = 25;

    private static final double DEFAULT_READ_RATE_STEP_INCREASE = 10;

    private static final double RATE_DECREASE_FACTOR = 0.9;

    private static final int DEFAULT_RATE_INCREASE_CHECKPOINT = 60;

    private static final int FIVE_SECOND_WINDOW_SIZE = 60;

    private static final int STABLE_RATE_WINDOW = 90;

    private LinkedList<RequestStats> oneSecondStats = new LinkedList<RequestStats>();

    private LinkedList<AggregateRequestStats> fiveSecondStats = new LinkedList<AggregateRequestStats>();

    private int stableRateTick;

    private AtomicInteger requests = new AtomicInteger();

    private AtomicInteger failRequests = new AtomicInteger();

    private boolean shutdown;

    private int warmUp = MigrateAggregateMetrics.DEFAULT_WARM_UP;

    private AtomicReference<RateLimiter> readPermitsRef;

    private AtomicReference<RateLimiter> writePermitsRef;

    private double writeRateStepIncrease = DEFAULT_WRITE_RATE_STEP_INCREASE;

    private double readRateStepIncrease = DEFAULT_READ_RATE_STEP_INCREASE;

    private int rateIncreaseCheckpoint = DEFAULT_RATE_INCREASE_CHECKPOINT;

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
                oneSecondStats.addFirst(new RequestStats(requests.getAndSet(0), failRequests.getAndSet(0)));
                if (oneSecondStats.size() > 4) {
                    aggregateStats();
                    if (isRateDecreaseNeeded()) {
                        decreaseRates();
                        clearStats();
                        stableRateTick = 0;
                        writeRateStepIncrease = DEFAULT_WRITE_RATE_STEP_INCREASE;
                        readRateStepIncrease = DEFAULT_READ_RATE_STEP_INCREASE;
                        rateIncreaseCheckpoint = DEFAULT_RATE_INCREASE_CHECKPOINT;
                    } else if (fiveSecondStats.peek().thresholdExceeded) {
                        increaseWarmup();
                        oneSecondStats.clear();
                        stableRateTick = 0;
                        writeRateStepIncrease = DEFAULT_WRITE_RATE_STEP_INCREASE;
                        readRateStepIncrease = DEFAULT_READ_RATE_STEP_INCREASE;
                        rateIncreaseCheckpoint = DEFAULT_RATE_INCREASE_CHECKPOINT;
                    } else if (isLongTermRateStable()) {
                        writeRateStepIncrease += DEFAULT_WRITE_RATE_STEP_INCREASE;
                        readRateStepIncrease += DEFAULT_READ_RATE_STEP_INCREASE;
                        rateIncreaseCheckpoint = Math.max(30, rateIncreaseCheckpoint - 15);
                        stableRateTick = 0;

                        log.info("Rates are stable. The read rate step increase is now " + readRateStepIncrease +
                            " . The write rate step increase is now " + writeRateStepIncrease +
                            ". The rate increase checkpoint is now " + rateIncreaseCheckpoint);

                        increaseRates();
                        clearStats();
                    } else if (isShortTermRateStable()) {
                        increaseRates();
                        clearStats();
                    }
                }
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                log.info("Stopping request monitoring due to interrupt", e);
            } catch (Exception e) {
                log.warn("There was an unexpected error", e);
            }
        }
    }

    protected void clearStats() {
        oneSecondStats.clear();
        fiveSecondStats.clear();
    }

    private void aggregateStats() {
        double totalRequests = 0;
        double totalFailures = 0;

        stableRateTick++;

        for (RequestStats stats : oneSecondStats) {
            totalRequests += stats.requests;
            totalFailures += stats.failedRequests;
        }
        fiveSecondStats.addFirst(new AggregateRequestStats((totalFailures / totalRequests) > FAILURE_THRESHOLD,
            totalFailures));
        oneSecondStats.removeLast();
        if (fiveSecondStats.size() > FIVE_SECOND_WINDOW_SIZE) {
            fiveSecondStats.removeLast();
        }
    }

    private boolean isRateDecreaseNeeded() {
        if (fiveSecondStats.size() < 30) {
            return false;
        }

        int i = 0;
        int failures = 0;
        for (AggregateRequestStats stats : fiveSecondStats) {
            // We are looking for 3 occurrences of the threshold being exceeded in the last
            // 30 samples.
            if (failures > 2 || i > 29) {
                break;
            }
            if (stats.thresholdExceeded) {
                ++failures;
            }
            ++i;
        }
        return failures > 2;
    }

    private boolean isShortTermRateStable() {
        if (fiveSecondStats.size() < rateIncreaseCheckpoint) {
            return false;
        }

        int i = 0;
        for (AggregateRequestStats stats : fiveSecondStats) {
            if (stats.failedRequests > 0) {
                return false;
            }
            if (i > rateIncreaseCheckpoint - 1) {
                break;
            }
            ++i;
        }
        return true;
    }

    private boolean isLongTermRateStable() {
        return stableRateTick == STABLE_RATE_WINDOW;
    }

    private void decreaseRates() {
        double readRate = readPermitsRef.get().getRate();
        double writeRate = Math.max(writePermitsRef.get().getRate(), MIN_WRITE_RATE);
        double newWriteRate = writeRate * RATE_DECREASE_FACTOR;
        double newReadRate = Math.max(newWriteRate * 0.04, MIN_READ_RATE);

        log.info("Decreasing request rates:\n" +
            readRate + " reads/sec --> " + newReadRate + " reads/sec\n" +
            writeRate + " writes/sec --> " + newWriteRate + " writes/sec\n");

        warmUp = MigrateAggregateMetrics.DEFAULT_WARM_UP;
        readPermitsRef.set(RateLimiter.create(newReadRate, warmUp, TimeUnit.SECONDS));
        writePermitsRef.set(RateLimiter.create(newWriteRate, warmUp, TimeUnit.SECONDS));
    }

    private void increaseRates() {
        double readRate = readPermitsRef.get().getRate();
        double writeRate = writePermitsRef.get().getRate();
        double newWriteRate = writeRate + writeRateStepIncrease;
        double newReadRate = newWriteRate * 0.04;

        log.info("Increasing request rates:\n" +
            readRate + " reads/sec --> " + newReadRate + " reads/sec\n" +
            writeRate + " writes/sec --> " + newWriteRate + " writes/sec\n");

        warmUp = MigrateAggregateMetrics.DEFAULT_WARM_UP;
        readPermitsRef.set(RateLimiter.create(newReadRate, warmUp, TimeUnit.SECONDS));
        writePermitsRef.set(RateLimiter.create(newWriteRate, warmUp, TimeUnit.SECONDS));
    }

    private void increaseWarmup() {
        warmUp *= 2;
        double readRate = readPermitsRef.get().getRate();
        double writeRate = writePermitsRef.get().getRate();

        log.info("Resetting request rates with new warm up of " + warmUp + " sec");

        readPermitsRef.set(RateLimiter.create(readRate, warmUp, TimeUnit.SECONDS));
        writePermitsRef.set(RateLimiter.create(writeRate, warmUp, TimeUnit.SECONDS));
    }

}
