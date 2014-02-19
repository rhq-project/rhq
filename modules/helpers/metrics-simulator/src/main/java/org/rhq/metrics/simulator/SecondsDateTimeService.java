package org.rhq.metrics.simulator;

import org.joda.time.DateTime;
import org.joda.time.Duration;

/**
 * @author John Sanda
 */
public class SecondsDateTimeService extends SimulatedDateTimeService {

    @Override
    public DateTime getTimeSlice(DateTime dt, Duration duration) {
        if (duration.equals(configuration.getRawTimeSliceDuration())) {
            int milliseconds = (dt.getSecondOfMinute() * 1000) / 2500;
            return dt.minuteOfHour().roundFloorCopy().plus(milliseconds * 2500);
        } else if (duration.equals(configuration.getOneHourTimeSliceDuration())) {
            int seconds = dt.getSecondOfMinute() / 15;
            return dt.minuteOfHour().roundFloorCopy().plusSeconds(seconds * 15);
        } else if (duration.equals(configuration.getSixHourTimeSliceDuration())) {
            return dt.minuteOfHour().roundFloorCopy();
        } else {
            throw new IllegalArgumentException("The duration [" + duration + "] is not supported");
        }
    }
}
