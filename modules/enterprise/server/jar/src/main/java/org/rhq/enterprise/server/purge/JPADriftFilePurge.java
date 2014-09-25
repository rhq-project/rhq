/*
 * RHQ Management Platform
 * Copyright (C) 2005-2014 Red Hat, Inc.
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
 * along with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA
 */

package org.rhq.enterprise.server.purge;

import static org.rhq.core.db.DatabaseTypeFactory.isOracle;
import static org.rhq.core.db.DatabaseTypeFactory.isPostgres;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import javax.sql.DataSource;
import javax.transaction.UserTransaction;

import org.rhq.core.db.DatabaseType;

/**
 * @author Thomas Segismont
 */
public class JPADriftFilePurge extends PurgeTemplate<String> {
    private static final String ENTITY_NAME = "JPADriftFile";

    // Don't use NOT IN as NOT IN(NULL) will always return false
    // See http://stackoverflow.com/questions/17150208/sql-query-with-not-in-returning-no-rows
    // On Postgres, this query performs better than its equivalent with one NOT EXISTS clause only:
    //    NOT EXISTS(SELECT 1 FROM RHQ_DRIFT d WHERE d.OLD_DRIFT_FILE = f.HASH_ID OR d.NEW_DRIFT_FILE = f.HASH_ID)
    private static final String QUERY_SELECT_KEYS_FOR_PURGE = "" //
        + " SELECT " //
        + "   f.HASH_ID " //
        + " FROM RHQ_DRIFT_FILE f " //
        + " WHERE NOT EXISTS(SELECT " //
        + "                    1 " //
        + "                  FROM RHQ_DRIFT o " //
        + "                  WHERE o.OLD_DRIFT_FILE = f.HASH_ID) " //
        + "       AND NOT EXISTS(SELECT " //
        + "                        1 " //
        + "                      FROM RHQ_DRIFT n " //
        + "                      WHERE n.NEW_DRIFT_FILE = f.HASH_ID) " //
        + "       AND f.CTIME < ? ";

    private static final String QUERY_PURGE_BY_KEY = "DELETE FROM RHQ_DRIFT_FILE WHERE hash_id = ?";

    private final long deleteUpToTime;

    JPADriftFilePurge(DataSource dataSource, UserTransaction userTransaction, long deleteUpToTime) {
        super(dataSource, userTransaction);
        this.deleteUpToTime = deleteUpToTime;
    }

    @Override
    protected String getEntityName() {
        return ENTITY_NAME;
    }

    @Override
    protected String getFindRowKeysQuery(DatabaseType databaseType) {
        if (isPostgres(databaseType) || isOracle(databaseType)) {
            return QUERY_SELECT_KEYS_FOR_PURGE;
        }
        throw new UnsupportedOperationException(databaseType.getName());
    }

    @Override
    protected void setFindRowKeysQueryParams(PreparedStatement preparedStatement) throws SQLException {
        preparedStatement.setLong(1, deleteUpToTime);
    }

    @Override
    protected String getKeyFromResultSet(ResultSet resultSet) throws SQLException {
        return resultSet.getString(1);
    }

    @Override
    protected String getDeleteRowByKeyQuery(DatabaseType databaseType) {
        if (isPostgres(databaseType) || isOracle(databaseType)) {
            return QUERY_PURGE_BY_KEY;
        }
        throw new UnsupportedOperationException(databaseType.getName());
    }

    @Override
    protected void setDeleteRowByKeyQueryParams(PreparedStatement preparedStatement, String key) throws SQLException {
        preparedStatement.setString(1, key);
    }
}
