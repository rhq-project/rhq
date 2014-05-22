package org.rhq.cassandra.schema;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * This interface essentially is a callback mechanism to provide access to the RHQ relational database. Custom upgrade
 * {@link Step steps} may need access to the database. The DBUtil class in the rhq-core-dbutils module encapsulates the
 * logic for creating connections to all of the RHQ-supported databases which is particularly useful when running
 * outside of the server. rhq-core-dbutils already has a dependency on this module; so, DBUtil cannot be used directly.
 *
 * @author John Sanda
 */
public interface DBConnectionFactory {

    /**
     * @return A new, live connection to the RHQ relational database
     */
    Connection newConnection() throws SQLException;

}
