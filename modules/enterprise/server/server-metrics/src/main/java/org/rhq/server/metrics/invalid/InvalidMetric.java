package org.rhq.server.metrics.invalid;

import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;

import com.google.common.base.Objects;

import org.joda.time.DateTime;

import org.rhq.server.metrics.domain.AggregateNumericMetric;
import org.rhq.server.metrics.domain.MetricsTable;

/**
 * A wrapper around an {@link org.rhq.server.metrics.domain.AggregateNumericMetric} that
 * has either min > avg or max < avg. This wrapper is used because metric timestamps are
 * not directly used for comparison. Instead, the day is used.
 *
* @author John Sanda
*/
class InvalidMetric implements Delayed {

    // Copied from Integer.java since it is not available until Java 1.7
    private static int compareInts(int x, int y) {
        return (x < y) ? -1 : ((x == y) ? 0 : 1);
    }

    public DateTime day;

    public MetricsTable type;

    public AggregateNumericMetric metric;

    private long delay;

    private long ctime;

    public InvalidMetric(DateTime day, MetricsTable type, AggregateNumericMetric metric, long delay) {
        this.day = day;
        this.type = type;
        this.metric = metric;
        this.delay = delay;
        this.ctime = System.currentTimeMillis();
    }

    @Override
    public long getDelay(TimeUnit unit) {
        return unit.convert(delay - (System.currentTimeMillis() - ctime), TimeUnit.MILLISECONDS);
    }

    @Override
    public int compareTo(Delayed other) {
        if (this == other) {
            return 0;
        }

        InvalidMetric that = (InvalidMetric) other;
        long thisDelay = this.getDelay(TimeUnit.MILLISECONDS);
        long thatDelay = that.getDelay(TimeUnit.MILLISECONDS);

        if (thisDelay < thatDelay) {
            return -1;
        } else if (thisDelay > thatDelay) {
            return 1;
        } else if (this.day.compareTo(that.day) < 0) {
            return -1;
        } else if (this.day.compareTo(that.day) > 0) {
            return 1;
        } else {
            return compareInts(this.metric.getScheduleId(), that.metric.getScheduleId());
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        InvalidMetric that = (InvalidMetric) o;

        if (metric.getScheduleId() != that.metric.getScheduleId()) return false;
        if (!day.equals(that.day)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = day.hashCode();
        result = 31 * result + metric.getScheduleId();
        return result;
    }

    @Override
    public String toString() {
        return Objects.toStringHelper(getClass())
            .add("type", getType())
            .add("scheduleId", metric.getScheduleId())
            .add("timestamp", metric.getTimestamp())
            .add("max", metric.getMax())
            .add("min", metric.getMin())
            .add("avg", metric.getAvg())
            .toString();
    }

    private String getType() {
        switch (type) {
        case RAW: return "raw";
        case ONE_HOUR: return "1 hour";
        case SIX_HOUR: return "6 hour";
        case TWENTY_FOUR_HOUR: return "24 hour";
        default: throw new IllegalArgumentException(type + " is not an expected value");
        }
    }
}
