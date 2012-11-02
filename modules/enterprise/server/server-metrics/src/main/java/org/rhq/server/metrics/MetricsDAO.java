/*
 *
 *  * RHQ Management Platform
 *  * Copyright (C) 2005-2012 Red Hat, Inc.
 *  * All rights reserved.
 *  *
 *  * This program is free software; you can redistribute it and/or modify
 *  * it under the terms of the GNU General Public License, version 2, as
 *  * published by the Free Software Foundation, and/or the GNU Lesser
 *  * General Public License, version 2.1, also as published by the Free
 *  * Software Foundation.
 *  *
 *  * This program is distributed in the hope that it will be useful,
 *  * but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  * GNU General Public License and the GNU Lesser General Public License
 *  * for more details.
 *  *
 *  * You should have received a copy of the GNU General Public License
 *  * and the GNU Lesser General Public License along with this program;
 *  * if not, write to the Free Software Foundation, Inc.,
 *  * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 *
 */

package org.rhq.server.metrics;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import javax.sql.DataSource;

import org.rhq.core.util.jdbc.JDBCUtil;

/**
 * @author John Sanda
 */
public class MetricsDAO {

    public static final String METRICS_INDEX_TABLE = "metrics_index";

    public static final String ONE_HOUR_METRICS_TABLE = "one_hour_metrics";

    private static final String METRICS_INDEX_QUERY =
        "SELECT time, schedule_id " +
        "FROM " + METRICS_INDEX_TABLE + " " +
        "WHERE bucket = ? " +
        "ORDER BY time";

    private static interface ConnectionCallback {
        void invoke(Connection connection);
    }

    private DataSource dataSource;

    public MetricsDAO(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public List<MetricsIndexEntry> findMetricsIndexEntries(final String bucket) {
        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet resultSet = null;
        try {
            connection = dataSource.getConnection();
            statement = connection.prepareStatement(METRICS_INDEX_QUERY);
            statement.setString(1, bucket);
            resultSet = statement.executeQuery();
            List<MetricsIndexEntry> indexEntries = new ArrayList<MetricsIndexEntry>();
            ResultSetMapper<MetricsIndexEntry> resultSetMapper = new MetricsIndexResultSetMapper(bucket);

            while (resultSet.next()) {
                indexEntries.add(resultSetMapper.map(resultSet));
            }
            return indexEntries;
        } catch (SQLException e) {
            throw new CQLException(e);
        } finally {
            JDBCUtil.safeClose(resultSet);
            JDBCUtil.safeClose(statement);
            JDBCUtil.safeClose(connection);
        }
    }

    private void execute(ConnectionCallback callback) {
        Connection connection = null;
        try {
            connection = dataSource.getConnection();
            callback.invoke(connection);
        } catch(SQLException e) {
            throw new CQLException(e);
        } finally {
            JDBCUtil.safeClose(connection);
        }
    }
}
