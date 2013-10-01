package org.rhq.metrics.simulator;

import org.joda.time.DateTime;
import org.joda.time.Duration;

import org.rhq.server.metrics.DateTimeService;

/**
 * @author John Sanda
 */
public class SimulatorDateTimeService extends DateTimeService {

    @Override
    public DateTime getTimeSlice(DateTime dt, Duration duration) {
        if (duration.equals(configuration.getRawTimeSliceDuration())) {
            int seconds = ((dt.getMinuteOfHour() * 60) + dt.getSecondOfMinute()) / 150;
            return dt.hourOfDay().roundFloorCopy().plusSeconds(seconds * 150);
        } else if (duration.equals(configuration.getOneHourTimeSliceDuration())) {
            int minutes = dt.minuteOfHour().get() / 15;
            return dt.hourOfDay().roundFloorCopy().plusMinutes(minutes * 15);
        } else if (duration.equals(configuration.getSixHourTimeSliceDuration())) {
            return dt.hourOfDay().roundFloorCopy();
        } else {
            throw new IllegalArgumentException("The duration [" + duration + "] is not supported");
        }
    }
}
