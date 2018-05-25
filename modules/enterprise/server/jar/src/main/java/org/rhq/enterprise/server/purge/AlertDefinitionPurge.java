/*
 * RHQ Management Platform
 * Copyright (C) 2005-2018 Red Hat, Inc.
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
 * along with this program; if not, write to the Free Software Foundation, Inc,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA
 */
package org.rhq.enterprise.server.purge;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import javax.sql.DataSource;
import javax.transaction.UserTransaction;

import org.rhq.core.db.DatabaseType;

import static org.rhq.core.db.DatabaseTypeFactory.isOracle;
import static org.rhq.core.db.DatabaseTypeFactory.isPostgres;

/**
 * @author miburman
 */
public class AlertDefinitionPurge extends PurgeTemplate<Integer> {

    private static final String ENTITY_NAME = "AlertDefinition";

    // This causes two full table scans to both rhq_alert_definition as well as to rhq_alert
    private static final String QUERY_SELECT_KEYS_FOR_PURGE_POSTGRES =
            "SELECT ad.id " +
                    "FROM rhq_alert_definition ad " +
                    "WHERE ad.deleted = 'true' AND ad.id NOT IN (SELECT a.alert_definition_id FROM rhq_alert a)";

    // Oracle does not have boolean datatype, so JPA uses a mapping to NUMBER(0,1)
    private static final String QUERY_SELECT_KEYS_FOR_PURGE_ORACLE =
            "SELECT ad.id " +
                    "FROM rhq_alert_definition ad " +
                    "WHERE ad.deleted = 1 AND ad.id NOT IN (SELECT a.alert_definition_id FROM rhq_alert a)";

    private static final String QUERY_PURGE_BY_KEY = "DELETE FROM rhq_alert_definition WHERE id = ?";

    /**
     * @param dataSource      the source of JDBC connections to the database
     * @param userTransaction the transaction management interface
     */
    public AlertDefinitionPurge(DataSource dataSource, UserTransaction userTransaction) {
        super(dataSource, userTransaction);
    }

    @Override
    protected String getEntityName() {
        return ENTITY_NAME;
    }

    @Override
    protected String getFindRowKeysQuery(DatabaseType databaseType) {
        if (isPostgres(databaseType)) {
            return QUERY_SELECT_KEYS_FOR_PURGE_POSTGRES;
        } else if (isOracle(databaseType)) {
            return QUERY_SELECT_KEYS_FOR_PURGE_ORACLE;
        }
        throw new UnsupportedOperationException(databaseType.getName());
    }

    @Override
    protected void setFindRowKeysQueryParams(PreparedStatement preparedStatement) throws SQLException { }

    @Override
    protected Integer getKeyFromResultSet(ResultSet resultSet) throws SQLException {
        return resultSet.getInt(1);
    }

    @Override
    protected String getDeleteRowByKeyQuery(DatabaseType databaseType) {
        return QUERY_PURGE_BY_KEY;
    }

    @Override
    protected void setDeleteRowByKeyQueryParams(PreparedStatement preparedStatement, Integer key) throws SQLException {
        preparedStatement.setInt(1, key);
    }
}
