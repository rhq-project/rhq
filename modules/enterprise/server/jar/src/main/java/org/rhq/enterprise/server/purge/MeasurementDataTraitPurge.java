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
import org.rhq.core.domain.measurement.MeasurementDataPK;

/**
 * @author Thomas Segismont
 */
class MeasurementDataTraitPurge extends PurgeTemplate<MeasurementDataPK> {
    private static final String ENTITY_NAME = "MeasurementDataTrait";

    /*
     * NOTE: Avoid using the AS keyword in the FROM clauses in this query, because Oracle barfs on it
     *       (see http://download.oracle.com/docs/cd/B19306_01/server.102/b14200/ap_standard_sql003.htm, subfeature id
     *       E051-08).
     */
    private static final String QUERY_SELECT_KEYS_FOR_PURGE = "" //
        + "SELECT schedule_id, time_stamp FROM rhq_measurement_data_trait " // SQL Server doesn't like aliases, use full table name
        + "WHERE EXISTS " // rewritten as exists because H2 doesn't support multi-column conditions
        + "  (SELECT t2.schedule_id, t2.time_stamp " //
        + "   FROM rhq_measurement_data_trait t2, " //
        + "     (SELECT max(t4.time_stamp) as mx, t4.schedule_id as schedule_id " //
        + "      FROM rhq_measurement_data_trait t4 " //
        + "      WHERE t4.time_stamp < ? " //
        + "      GROUP BY t4.schedule_id) t3 " //
        + "   WHERE t2.schedule_id = t3.schedule_id " //
        + "   AND t2.time_stamp < t3.mx " //
        + "   AND rhq_measurement_data_trait.time_stamp = t2.time_stamp " // rewrote multi-column conditions as additional
        + "   AND rhq_measurement_data_trait.schedule_id = t2.schedule_id) "; // correlated restrictions to the delete table;

    private static final String QUERY_PURGE_BY_KEY = "" //
        + "DELETE FROM rhq_measurement_data_trait WHERE schedule_id = ? AND time_stamp = ?";

    private final long oldest;

    MeasurementDataTraitPurge(DataSource dataSource, UserTransaction userTransaction, long oldest) {
        super(dataSource, userTransaction);
        this.oldest = oldest;
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
        preparedStatement.setLong(1, oldest);
    }

    @Override
    protected MeasurementDataPK getKeyFromResultSet(ResultSet resultSet) throws SQLException {
        return new MeasurementDataPK(resultSet.getLong(2), resultSet.getInt(1));
    }

    @Override
    protected String getDeleteRowByKeyQuery(DatabaseType databaseType) {
        if (isPostgres(databaseType) || isOracle(databaseType)) {
            return QUERY_PURGE_BY_KEY;
        }
        throw new UnsupportedOperationException(databaseType.getName());
    }

    @Override
    protected void setDeleteRowByKeyQueryParams(PreparedStatement preparedStatement, MeasurementDataPK key)
        throws SQLException {
        preparedStatement.setInt(1, key.getScheduleId());
        preparedStatement.setLong(2, key.getTimestamp());
    }
}
