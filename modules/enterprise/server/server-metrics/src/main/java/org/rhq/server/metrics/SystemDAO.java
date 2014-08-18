package org.rhq.server.metrics;

import com.datastax.driver.core.BoundStatement;
import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.ResultSet;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * @author John Sanda
 */
public class SystemDAO {

    public static enum Keyspace {
        SYSTEM_AUTH, RHQ;

        @Override
        public String toString() {
            return super.toString().toLowerCase();
        }

    }

    private final Log log = LogFactory.getLog(SystemDAO.class);

    private StorageSession session;

    private PreparedStatement findTables;

    public SystemDAO(StorageSession session) {
        this.session = session;
        initPreparedStatements();
    }

    public void initPreparedStatements() {
        log.info("Initializing prepared statements");
        findTables = session.prepare("SELECT columnfamily_name FROM system.schema_columnfamilies WHERE keyspace_name = ?");
    }

    public ResultSet findTables(Keyspace keyspace) {
        BoundStatement statement = findTables.bind(keyspace.toString());
        return session.execute(statement);
    }

}
