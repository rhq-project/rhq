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
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import mazz.i18n.Logger;

import org.rhq.core.db.DatabaseType;
import org.rhq.core.db.DbUtilsI18NFactory;
import org.rhq.core.db.DbUtilsI18NResourceKeys;

/**
 * Database upgrade task 2.115 adds rhq_drift.path_directory column to store the directory portion of the path
 * column.  This code sets the value appropriately. 
 *
 * @author Jay Shaughnessy
 */
public class DriftPathDirectoryDatabaseUpgradeTask implements DatabaseUpgradeTask {

    private static final Logger LOG = DbUtilsI18NFactory.getLogger(DriftPathDirectoryDatabaseUpgradeTask.class);

    public void execute(DatabaseType databaseType, Connection connection) throws SQLException {

        Statement s = null;

        try {
            String sql = "select d.ID, d.PATH, d.PATH_DIRECTORY from RHQ_DRIFT d";
            LOG.debug(DbUtilsI18NResourceKeys.EXECUTING_SQL, sql);

            s = connection.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE);
            ResultSet rs = s.executeQuery(sql);

            while (rs.next()) {
                // note, this is guaranteed to be non-null and valid
                String path = rs.getString(2);
                String dir = getDir(path);
                rs.updateString(3, dir);
                rs.updateRow();
            }
        } finally {
            databaseType.closeStatement(s);
        }
    }

    private String getDir(String path) {
        int i = path.lastIndexOf("/");
        String result = (i != -1) ? path.substring(0, i) : "./";

        return result;
    }
}
