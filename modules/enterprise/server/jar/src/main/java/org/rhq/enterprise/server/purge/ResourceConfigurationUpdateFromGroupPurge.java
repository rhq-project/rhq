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
public class ResourceConfigurationUpdateFromGroupPurge extends PurgeTemplate<Integer> {
    private static final String ENTITY_NAME = "ResourceConfigurationUpdateFromGroup";

    private static final String QUERY_SELECT_KEYS_FOR_PURGE = "" //
        + "  " //
        + " SELECT " //
        + "   u.CONFIGURATION_ID " //
        + " FROM " //
        + "   RHQ_CONFIG_UPDATE u " //
        + " WHERE " //
        + "   u.AGG_RES_UPDATE_ID IN( " //
        + "     SELECT " //
        + "       gu.ID " //
        + "     FROM " //
        + "       RHQ_CONFIG_GROUP_UPDATE gu " //
        + "     WHERE " //
        + "       gu.DTYPE = 'resource' " //
        + "       AND gu.MTIME < ? " //
        // Make sure we'll not delete the config referenced in the resource table
        + "       AND NOT EXISTS( " //
        + "           SELECT " //
        + "             1 " //
        + "           FROM " //
        + "             RHQ_CONFIG_UPDATE u, RHQ_RESOURCE r " //
        + "           WHERE " //
        + "             gu.ID = u.AGG_RES_UPDATE_ID " //
        + "             AND u.CONFIG_RES_ID = r.ID " //
        + "             AND u.CONFIGURATION_ID = r.RES_CONFIGURATION_ID " //
        + "       ) " //
        // Leave along group updates which have at least one child update which is the latest successful resource
        // configuration update
        + "       AND NOT EXISTS( " //
        + "           SELECT " //
        + "             1 " //
        + "           FROM " //
        + "             RHQ_CONFIG_UPDATE uu " //
        + "           WHERE " //
        + "             gu.DTYPE = uu.DTYPE " //
        + "             AND gu.ID = uu.AGG_RES_UPDATE_ID " //
        + "             AND NOT EXISTS( " //
        + "                 SELECT " //
        + "                   1 " //
        + "                 FROM " //
        + "                   RHQ_CONFIG_UPDATE uuu " //
        + "                 WHERE " //
        + "                   uu.CONFIG_RES_ID = uuu.CONFIG_RES_ID " //
        + "                   AND uuu.STATUS = 'SUCCESS' " //
        + "                   AND(uuu.AGG_RES_UPDATE_ID IS NULL OR uuu.AGG_RES_UPDATE_ID <> gu.ID) " //
        + "                   AND uuu.MTIME > uu.MTIME " //
        + "             ) " //
        + "       ) " //
        + "   ) ";

    // FK cascade delete will delete config, config properties and resource config udpate all at once
    private static final String QUERY_PURGE_BY_KEY = "DELETE FROM RHQ_CONFIG WHERE ID = ?";

    private final long deleteUpToTime;

    ResourceConfigurationUpdateFromGroupPurge(DataSource dataSource, UserTransaction userTransaction,
        long deleteUpToTime) {
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
    protected Integer getKeyFromResultSet(ResultSet resultSet) throws SQLException {
        return resultSet.getInt(1);
    }

    @Override
    protected String getDeleteRowByKeyQuery(DatabaseType databaseType) {
        if (isPostgres(databaseType) || isOracle(databaseType)) {
            return QUERY_PURGE_BY_KEY;
        }
        throw new UnsupportedOperationException(databaseType.getName());
    }

    @Override
    protected void setDeleteRowByKeyQueryParams(PreparedStatement preparedStatement, Integer key) throws SQLException {
        preparedStatement.setInt(1, key);
    }
}
