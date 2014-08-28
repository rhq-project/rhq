package org.rhq.cassandra.schema;

import java.util.Properties;

import com.datastax.driver.core.Session;

/**
 * @author John Sanda
 */
public class MigrateAggregateMetrics implements Step {

    private Session session;

    @Override
    public void setSession(Session session) {
        this.session = session;
    }

    @Override
    public void bind(Properties properties) {

    }

    @Override
    public void execute() {
        session.execute("DROP table rhq.one_hour_metrics");
        session.execute("DROP table rhq.six_hour_metrics");
        session.execute("DROP table rhq.twenty_four_hour_metrics");
    }
}
