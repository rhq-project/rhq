package org.rhq.core.db.upgrade;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
        String sql = "SELECT id, name, resource_type_id FROM  rhq_resource_subcat";

        log.debug(DbUtilsI18NResourceKeys.EXECUTING_SQL, sql);
        List<Object[]> results = databaseType.executeSelectSql(connection, sql);

        Integer primaryId;
        Integer duplicateId;
        Integer resourceTypeId;
        String name = null;
        Map<String, Object[]> primaryNameMap = new HashMap<String, Object[]>();

        for (Object[] row : results) {
            name = (String) row[1];
            if (!primaryNameMap.containsKey(name)) {
                primaryNameMap.put(name, row);
                primaryId = databaseType.getInteger(row[0]);

                if (row[2] != null) {
                    resourceTypeId = databaseType.getInteger(row[2]);

                    //Create the linking entry to link resource to it's proper subcategories
                    log.debug(DbUtilsI18NResourceKeys.MESSAGE,
                        "Create subcategory to parent resource entry for resource type [id= " + resourceTypeId
                            + "] with subcategory [id= " + primaryId + "]");
                    String insert = "INSERT INTO rhq_resource_type_subcat ( RESOURCE_TYPE_ID,  RESOURCE_SUBCAT_ID) values"
                        + " ( " + resourceTypeId + " , " + primaryId + " )";
                    log.debug(DbUtilsI18NResourceKeys.EXECUTING_SQL, insert);
                    databaseType.executeSql(connection, insert);
                }
            } else {
                duplicateId = databaseType.getInteger(row[0]);
                primaryId = databaseType.getInteger(primaryNameMap.get(name)[0]);

                if (row[2] != null) {
                    resourceTypeId = databaseType.getInteger(row[2]);

                    //Create the linking entry to link resource to it's proper subcategories
                    log.debug(DbUtilsI18NResourceKeys.MESSAGE,
                        "Create subcategory to parent resource entry for resource type [id= " + resourceTypeId
                            + "] with subcategory [id= " + primaryId + "]");
                    String insert = "INSERT INTO rhq_resource_type_subcat ( RESOURCE_TYPE_ID,  RESOURCE_SUBCAT_ID) values"
                        + " ( " + resourceTypeId + " , " + primaryId + " )";
                    log.debug(DbUtilsI18NResourceKeys.EXECUTING_SQL, insert);
                    databaseType.executeSql(connection, insert);
                }

                //Make resources that were pointing to the duplicate subcategory to this other subcategory
                log.debug(DbUtilsI18NResourceKeys.MESSAGE, "Replacing subcategory [id= " + duplicateId + "] with [id= "
                    + primaryId + "]");
                String update = "UPDATE rhq_resource_type SET subcategory = " + primaryId + " WHERE subcategory = "
                    + duplicateId;
                log.debug(DbUtilsI18NResourceKeys.EXECUTING_SQL, update);
                databaseType.executeSql(connection, update);

                //delete duplicate subcategory
                log.debug(DbUtilsI18NResourceKeys.MESSAGE, "Delete subcategory [id= " + duplicateId
                    + "] because it is duplicated.");
                String delete = "DELETE from rhq_resource_subcat WHERE id = " + duplicateId;
                log.debug(DbUtilsI18NResourceKeys.EXECUTING_SQL, delete);
                databaseType.executeSql(connection, delete);
            }
        }
    }
}
