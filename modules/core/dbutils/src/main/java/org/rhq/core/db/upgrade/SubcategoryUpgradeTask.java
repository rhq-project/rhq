package org.rhq.core.db.upgrade;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

import mazz.i18n.Logger;

import org.rhq.core.db.DatabaseType;
import org.rhq.core.db.DbUtilsI18NFactory;
import org.rhq.core.db.DbUtilsI18NResourceKeys;

/**
 * Updates the subcategories in preparation of dropping the subcategories table.
 *
 * @author Stefan Negrea
 */
public class SubcategoryUpgradeTask implements DatabaseUpgradeTask {

    private final Logger log = DbUtilsI18NFactory.getLogger(StorageNodeAddressUpgradeTask.class);

    @Override
    public void execute(DatabaseType databaseType, Connection connection) throws SQLException {
        String sql = "SELECT RHQ_RESOURCE_TYPE.id, RHQ_RESOURCE_SUBCAT.name"
                   + " FROM RHQ_RESOURCE_TYPE "
                   + " LEFT JOIN RHQ_RESOURCE_SUBCAT "
                   + " ON RHQ_RESOURCE_TYPE.subcategory_id = RHQ_RESOURCE_SUBCAT.id;";

        log.debug(DbUtilsI18NResourceKeys.EXECUTING_SQL, sql);
        List<Object[]> results = databaseType.executeSelectSql(connection, sql);

        Integer rowId;
        String subcategoryName;
        for (Object[] row : results) {
            rowId = (Integer) row[0];
            subcategoryName = (String) row[1];

            if (subcategoryName != null && !subcategoryName.isEmpty()) {
                log.debug(DbUtilsI18NResourceKeys.MESSAGE, "Updating resource [id= " + rowId
                    + "] to have subcategory " + subcategoryName);
                String update = "UPDATE RHQ_RESOURCE_TYPE SET SUBCATEGORY = '" + subcategoryName + "' WHERE id = "
                    + rowId;
                log.debug(DbUtilsI18NResourceKeys.EXECUTING_SQL, update);
                databaseType.executeSql(connection, update);
            }
        }
    }
}
