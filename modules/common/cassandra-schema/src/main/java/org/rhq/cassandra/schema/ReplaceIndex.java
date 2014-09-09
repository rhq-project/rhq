package org.rhq.cassandra.schema;

import java.util.Properties;

import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Session;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.joda.time.DateTime;
import org.joda.time.Days;
import org.joda.time.Duration;
import org.joda.time.Hours;
import org.joda.time.Period;

/**
 * @author John Sanda
 */
public class ReplaceIndex implements Step {

    private static final Log log = LogFactory.getLog(ReplaceIndex.class);

    private Session session;

    @Override
    public void setSession(Session session) {
        this.session = session;
    }

    @Override
    public void bind(Properties properties) {
    }

    @Override
    public String toString() {
        return getClass().getSimpleName();
    }

    @Override
    public void execute() {
        DateRanges dateRanges = new DateRanges();
        dateRanges.rawEndTime = DateTime.now().hourOfDay().roundFloorCopy();
        dateRanges.rawStartTime = dateRanges.rawEndTime.minusDays(3);
        dateRanges.oneHourStartTime = getTimeSlice(dateRanges.rawStartTime, Hours.SIX.toStandardDuration());
        dateRanges.oneHourEndTime = getTimeSlice(dateRanges.rawEndTime, Hours.SIX.toStandardDuration());
        dateRanges.sixHourStartTime = getTimeSlice(dateRanges.rawStartTime, Days.ONE.toStandardDuration());
        dateRanges.sixHourEndTime = getTimeSlice(dateRanges.rawEndTime, Days.ONE.toStandardDuration());

        if (cacheIndexExists()) {
            new Replace412Index(session).execute(dateRanges);
        } else {
            new ReplaceRHQ411Index(session).execute(dateRanges);
        }
    }

    private DateTime getTimeSlice(DateTime dt, Duration duration) {
        Period p = duration.toPeriod();

        if (p.getYears() != 0) {
            return dt.yearOfEra().roundFloorCopy().minusYears(dt.getYearOfEra() % p.getYears());
        } else if (p.getMonths() != 0) {
            return dt.monthOfYear().roundFloorCopy().minusMonths((dt.getMonthOfYear() - 1) % p.getMonths());
        } else if (p.getWeeks() != 0) {
            return dt.weekOfWeekyear().roundFloorCopy().minusWeeks((dt.getWeekOfWeekyear() - 1) % p.getWeeks());
        } else if (p.getDays() != 0) {
            return dt.dayOfMonth().roundFloorCopy().minusDays((dt.getDayOfMonth() - 1) % p.getDays());
        } else if (p.getHours() != 0) {
            return dt.hourOfDay().roundFloorCopy().minusHours(dt.getHourOfDay() % p.getHours());
        } else if (p.getMinutes() != 0) {
            return dt.minuteOfHour().roundFloorCopy().minusMinutes(dt.getMinuteOfHour() % p.getMinutes());
        } else if (p.getSeconds() != 0) {
            return dt.secondOfMinute().roundFloorCopy().minusSeconds(dt.getSecondOfMinute() % p.getSeconds());
        }
        return dt.millisOfSecond().roundCeilingCopy().minusMillis(dt.getMillisOfSecond() % p.getMillis());
    }

    private boolean cacheIndexExists() {
        ResultSet resultSet = session.execute("SELECT columnfamily_name FROM system.schema_columnfamilies " +
            "WHERE keyspace_name = 'rhq' AND columnfamily_name = 'metrics_cache_index'");
        return !resultSet.isExhausted();
    }

}
