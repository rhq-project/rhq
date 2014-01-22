package org.rhq.metrics.simulator;

import java.util.Iterator;
import java.util.concurrent.ThreadLocalRandom;

import com.codahale.metrics.Timer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.joda.time.Days;
import org.joda.time.Duration;
import org.joda.time.Hours;

import org.rhq.core.domain.measurement.composite.MeasurementDataNumericHighLowComposite;
import org.rhq.server.metrics.MetricsServer;

/**
 * @author John Sanda
 */
public class MeasurementReader implements Runnable {

    private final Log log = LogFactory.getLog(MeasurementReader.class);

    private long intervalRate;

    private Metrics metrics;

    private MetricsServer metricsServer;

    private int startingSchedule;

    private int batchSize;

    public MeasurementReader(long intervalRate, Metrics metrics, MetricsServer metricsServer, int startingSchedule,
        int batchSize) {
        this.intervalRate = intervalRate;
        this.metrics = metrics;
        this.metricsServer = metricsServer;
        this.startingSchedule = startingSchedule;
        this.batchSize = batchSize;
    }

    @Override
    public void run() {
        Timer.Context context = metrics.totalReadTime.time();
        try {
            log.info("Running metrics queries");

            ThreadLocalRandom random = ThreadLocalRandom.current();
            int bound = startingSchedule + batchSize;

            findResourceDataForPast24Hours(random.nextInt(startingSchedule, bound));
            findResourceDataForPastWeek(random.nextInt(startingSchedule, bound));
            findResourceDataForPast2Weeks(random.nextInt(startingSchedule, bound));
            findResourceDataForPast31Days(random.nextInt(startingSchedule, bound));
            findResourceDataForPastYear(random.nextInt(startingSchedule, bound));
        } finally {
            log.info("Finished running metrics queries");
            context.stop();
        }
    }

    private void findResourceDataForPast24Hours(int scheduleId) {
        Duration duration = Hours.hours(24).toStandardSeconds().toStandardDuration();
        findResourceData(scheduleId, duration, metrics.twentyFourHourResourceQueryTime);
    }

    private void findResourceDataForPastWeek(int scheduleId) {
        Duration duration = Days.SEVEN.toStandardSeconds().minus(5).toStandardDuration();
        findResourceData(scheduleId, duration, metrics.oneWeekResourceQueryTime);
    }

    private void findResourceDataForPast2Weeks(int scheduleId) {
        Duration duration = Days.days(14).toStandardSeconds().minus(5).toStandardDuration();
        findResourceData(scheduleId, duration, metrics.twoWeekResourceQueryTime);
    }

    private void findResourceDataForPast31Days(int scheduleId) {
        Duration duration = Days.days(31).toStandardSeconds().minus(5).toStandardDuration();
        findResourceData(scheduleId, duration, metrics.monthResourceQueryTime);
    }

    private void findResourceDataForPastYear(int scheduleId) {
        Duration duration = Days.days(365).toStandardSeconds().minus(5).toStandardDuration();
        findResourceData(scheduleId, duration, metrics.yearResourceQueryTime);
    }

    private void findResourceData(int scheduleId, Duration duration, Timer timer) {
        long end = System.currentTimeMillis();
        long start = end - (duration.getMillis() / intervalRate);
        Timer.Context context = timer.time();
        try {
            Iterable<MeasurementDataNumericHighLowComposite> data = metricsServer.findDataForResource(scheduleId, start,
                end, 60);
            Iterator<MeasurementDataNumericHighLowComposite> iterator = data.iterator();
            for (MeasurementDataNumericHighLowComposite datum : data) {

            }
        } finally {
            context.stop();
        }
    }
}
