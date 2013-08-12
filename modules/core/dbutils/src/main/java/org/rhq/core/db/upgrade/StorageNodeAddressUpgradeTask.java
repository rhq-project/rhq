package org.rhq.core.db.upgrade;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

import mazz.i18n.Logger;

import org.rhq.core.db.DatabaseType;
import org.rhq.core.db.DbUtilsI18NFactory;
import org.rhq.core.db.DbUtilsI18NResourceKeys;

/**
 * Updates the address field of storage node entities to ensure we are storing IP addresses and not hostnames. We want
 * to store the IP address since that is what Cassandra uses for inter-node communication. JMX operations that return
 * nodes will return the node IP addresses and not hostnames.
 *
 * @author John Sanda
 */
public class StorageNodeAddressUpgradeTask implements DatabaseUpgradeTask {

    private final Logger log = DbUtilsI18NFactory.getLogger(StorageNodeAddressUpgradeTask.class);

    @Override
    public void execute(DatabaseType databaseType, Connection connection) throws SQLException {
        String sql = "SELECT id, address FROM rhq_storage_node";

        log.debug(DbUtilsI18NResourceKeys.EXECUTING_SQL, sql);
        List<Object[]> results = databaseType.executeSelectSql(connection, sql);

        Integer id = null;
        String storageNodeAddress = null;
        try {
            for (Object[] row : results) {
                id = databaseType.getInteger(row[0]);
                storageNodeAddress = (String) row[1];
                InetAddress address = InetAddress.getByName(storageNodeAddress);
                if (!storageNodeAddress.equals(address.getHostAddress())) {
                    log.debug(DbUtilsI18NResourceKeys.MESSAGE, "Updating address for  StorageNode[id= " + id + ", ]" +
                        "address= " + storageNodeAddress + "]");
                    String update = "UPDATE rhq_storage_node SET address = '" + address.getHostAddress() + "' " +
                        "WHERE id = " + id;
                    log.debug(DbUtilsI18NResourceKeys.EXECUTING_SQL, update);
                    databaseType.executeSql(connection, update);
                }
            }
        } catch (UnknownHostException e) {
            throw new RuntimeException("Failed to look up IP address for StorageNode[id =" + id + ", address=" +
                storageNodeAddress + "]", e);
        }
    }
}
