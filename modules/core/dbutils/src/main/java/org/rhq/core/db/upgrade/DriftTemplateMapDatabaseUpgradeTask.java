/*
 * RHQ Management Platform
 * Copyright (C) 2005-2008 Red Hat, Inc.
 * All rights reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */
package org.rhq.core.db.upgrade;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

import mazz.i18n.Logger;

import org.rhq.core.db.DatabaseType;
import org.rhq.core.db.DbUtilsI18NFactory;
import org.rhq.core.db.DbUtilsI18NResourceKeys;

/**
 * Database upgrade task 2.115 adds the rhq_drift_def_template table.  The table is initially populated here
 * by using data in the to-be-removed rhq_drift_template_map table. 
 *
 * @author Jay Shaughnessy
 */
public class DriftTemplateMapDatabaseUpgradeTask implements DatabaseUpgradeTask {

    private static final Logger LOG = DbUtilsI18NFactory.getLogger(DriftPathDirectoryDatabaseUpgradeTask.class);

    public void execute(DatabaseType databaseType, Connection connection) throws SQLException {

        // First generate a drift def template for each of the existing mappings

        String sql = "" //
            + " insert into RHQ_DRIFT_DEF_TEMPLATE (" //
            + "        ID," //            
            + "        RESOURCE_TYPE_ID," //
            + "        CONFIG_ID," //
            + "        NAME," // 
            + "        DESCRIPTION," //
            + "        IS_USER_DEFINED," //
            + "        CTIME )" //
            + " select " + databaseType.getSequenceInsertValue(connection, "RHQ_DRIFT_DEF_TEMPLATE_ID_SEQ") + "," //
            + "        tm.RESOURCE_TYPE_ID, " //                
            + "        ct.CONFIG_ID, " //                
            + "        ct.NAME, " //                
            + "        ct.DESCRIPTION, " //                
            + "        " + databaseType.getBooleanValue(false) + "," //
            + "        " + System.currentTimeMillis() //
            + "   from RHQ_DRIFT_TEMPLATE_MAP tm " //
            + "   join RHQ_RESOURCE_TYPE rt on tm.RESOURCE_TYPE_ID = rt.ID" //
            + "   join RHQ_CONFIG_TEMPLATE ct on tm.CONFIG_TEMPLATE_ID = ct.ID";
        LOG.debug(DbUtilsI18NResourceKeys.EXECUTING_SQL, sql);

        databaseType.executeSql(connection, sql);

        // Second, assign one of the new templates to each of the existing definitions. Note that in the
        // upgrade scenario we are guaranteed to only have 1 template for the given resource type so the
        // select below is safe to return a single value.
        sql = "" //
            + " update RHQ_DRIFT_DEFINITION dd set DRIFT_DEF_TEMPLATE_ID =" // 
            + " (   select dt.ID" // 
            + "       from RHQ_DRIFT_DEF_TEMPLATE dt" //
            + "       join RHQ_RESOURCE_TYPE rt on dt.RESOURCE_TYPE_ID = rt.ID" //
            + "       join RHQ_RESOURCE res     on rt.ID = res.RESOURCE_TYPE_ID" //
            + "      where dd.RESOURCE_ID = res.ID" //
            + " )" //
            + " where dd.DRIFT_DEF_TEMPLATE_ID is null";

        LOG.debug(DbUtilsI18NResourceKeys.EXECUTING_SQL, sql);

        databaseType.executeSql(connection, sql);

        // third, for each initial snapshot, create the newly required rhq_drift_set and map the drift to it
        sql = "select cs.ID from RHQ_DRIFT_CHANGE_SET cs where cs.VERSION = 0";
        LOG.debug(DbUtilsI18NResourceKeys.EXECUTING_SQL, sql);

        List<Object[]> rows = databaseType.executeSelectSql(connection, sql);

        sql = "insert into RHQ_DRIFT_SET ( ID ) values ( "
            + databaseType.getSequenceInsertValue(connection, "RHQ_DRIFT_SET_ID_SEQ") + " )";

        for (Object[] row : rows) {
            int csId = ((Number) row[0]).intValue();

            // insert the new drift set row
            LOG.debug(DbUtilsI18NResourceKeys.EXECUTING_SQL, sql);

            databaseType.executeSql(connection, sql);
            int setId = databaseType.getSequenceValue(connection, "RHQ_DRIFT_SET", "ID");

            // update the drift records for the initial change set to refer to the drift set
            String updateSql = "update RHQ_DRIFT set DRIFT_SET_ID = " + setId + " where DRIFT_CHANGE_SET_ID = " + csId;
            LOG.debug(DbUtilsI18NResourceKeys.EXECUTING_SQL, updateSql);

            databaseType.executeSql(connection, updateSql);

            // update the change set to refer to the drift set 
            updateSql = "update RHQ_DRIFT_CHANGE_SET set DRIFT_SET_ID = " + setId + " where ID = " + csId;
            LOG.debug(DbUtilsI18NResourceKeys.EXECUTING_SQL, updateSql);

            databaseType.executeSql(connection, updateSql);
        }
    }
}
