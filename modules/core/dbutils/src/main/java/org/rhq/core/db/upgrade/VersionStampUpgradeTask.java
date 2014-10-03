package org.rhq.core.db.upgrade;

import java.sql.Connection;
import java.sql.SQLException;

import mazz.i18n.Logger;

import org.rhq.core.db.DatabaseType;
import org.rhq.core.db.DbUtilsI18NFactory;
import org.rhq.core.db.DbUtilsI18NResourceKeys;

/**
 * Updates the StorageNode and Server version fields to an initial value of the current version.
 *
 * @author Jay Shaughnessy
 */
public class VersionStampUpgradeTask implements DatabaseUpgradeTask {

    private final Logger log = DbUtilsI18NFactory.getLogger(VersionStampUpgradeTask.class);

    @Override
    public void execute(DatabaseType databaseType, Connection connection) throws SQLException {
        String version = this.getClass().getPackage().getImplementationVersion();
        String update = "UPDATE rhq_server SET version = '" + version + "'";
        log.debug(DbUtilsI18NResourceKeys.EXECUTING_SQL, update);
        databaseType.executeSql(connection, update);

        update = "UPDATE rhq_storage_node SET version = '" + version + "'";
        log.debug(DbUtilsI18NResourceKeys.EXECUTING_SQL, update);
        databaseType.executeSql(connection, update);
    }
}
