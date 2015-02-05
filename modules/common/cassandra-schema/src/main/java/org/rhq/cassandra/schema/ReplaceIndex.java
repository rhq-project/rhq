package org.rhq.cassandra.schema;

import java.util.Properties;

import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Session;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.joda.time.DateTime;
import org.joda.time.Days;
import org.joda.time.Hours;

/**
 * For RHQ 4.9 - 4.11 installations this migrates data from the metrics_index table into the new metrics_idx table. For
 * 4.12 installations, it migrates data from metrics_cache_index into metrics_idx. The old index tables are deleted
 * after successfully migrating data.
 *
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
        dateRanges.oneHourStartTime = DateUtils.getTimeSlice(dateRanges.rawStartTime, Hours.SIX.toStandardDuration());
        dateRanges.oneHourEndTime = DateUtils.getTimeSlice(dateRanges.rawEndTime, Hours.SIX.toStandardDuration());
        dateRanges.sixHourStartTime = DateUtils.getTimeSlice(dateRanges.rawStartTime, Days.ONE.toStandardDuration());
        dateRanges.sixHourEndTime = DateUtils.getTimeSlice(dateRanges.rawEndTime, Days.ONE.toStandardDuration());

        if (cacheIndexExists()) {
            log.info("Preparing to replace metrics_cache_index");
            new Replace412Index(session).execute(dateRanges);
        } else {
            log.info("Preparing to replace metrics_index");
            new ReplaceRHQ411Index(session).execute(dateRanges);
        }
    }

    private boolean cacheIndexExists() {
        ResultSet resultSet = session.execute("SELECT columnfamily_name FROM system.schema_columnfamilies " +
            "WHERE keyspace_name = 'rhq' AND columnfamily_name = 'metrics_cache_index'");
        return !resultSet.isExhausted();
    }

}
