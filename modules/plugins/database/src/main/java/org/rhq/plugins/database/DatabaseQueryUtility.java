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
package org.rhq.plugins.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * @author Greg Hinkle
 */
public class DatabaseQueryUtility {
    private static final Log LOG = LogFactory.getLog(DatabaseQueryUtility.class);

    public static int executeUpdate(DatabaseComponent databaseComponent, String query, Object... parameters)
        throws SQLException {
        PreparedStatement statement = null;
        try {
            statement = databaseComponent.getConnection().prepareStatement(query);
            bindParameters(statement, parameters);

            return statement.executeUpdate();
        } catch (SQLException e) {
            databaseComponent.removeConnection();
            throw e;
        } finally {
            close(statement, null);
        }
    }

    public static Double getSingleNumericQueryValue(DatabaseComponent databaseComponent, String query,
        Object... parameters) {
        PreparedStatement statement = null;
        ResultSet resultSet = null;
        try {
            statement = databaseComponent.getConnection().prepareStatement(query);
            bindParameters(statement, parameters);
            resultSet = statement.executeQuery();

            if (resultSet.next()) {
                return resultSet.getDouble(1);
            }
        } catch (SQLException e) {
            databaseComponent.removeConnection();
        } finally {
            close(statement, resultSet);
        }

        return Double.NaN;
    }

    /**
     * Used to read a single row of columns into a map
     *
     * @param  databaseComponent
     * @param  query
     * @param  parameters
     *
     * @return
     */
    public static Map<String, Double> getNumericQueryValues(DatabaseComponent databaseComponent, String query,
        Object... parameters) {
        PreparedStatement statement = null;
        ResultSet resultSet = null;
        try {
            statement = databaseComponent.getConnection().prepareStatement(query);
            bindParameters(statement, parameters);

            resultSet = statement.executeQuery();

            Map<String, Double> row = new HashMap<String, Double>();

            ResultSetMetaData md = resultSet.getMetaData();
            String[] names = getColumns(md);

            if (resultSet.next()) {
                for (String name : names) {
                    try {
                        row.put(name, resultSet.getDouble(name));
                    } catch (SQLException e) {
                        // Ignore columns that can't be read as doubles
                    }
                }
            }

            return row;
        } catch (SQLException e) {
            LOG.debug("Unable to read value", e);
            databaseComponent.removeConnection();
        } finally {
            close(statement, resultSet);
        }

        return Collections.emptyMap();
    }

    /**
     * Used to access a set of rows as key, value pairs where the key is the first column and is a string and the second
     * is a value and is numeric
     *
     * @param  databaseComponent the component to execute on
     * @param  query             the sql query to run
     * @param  parameters        any parameters to bind first
     *
     * @return a Map<String,Double> of the keys against the value
     */
    public static Map<String, Double> getNumericQueryValueMap(DatabaseComponent databaseComponent, String query,
        Object... parameters) {
        PreparedStatement statement = null;
        ResultSet resultSet = null;
        try {
            Connection c = databaseComponent.getConnection();
            statement = c.prepareStatement(query);
            bindParameters(statement, parameters);

            resultSet = statement.executeQuery();

            Map<String, Double> map = new HashMap<String, Double>();

            while (resultSet.next()) {
                try {
                    map.put(resultSet.getString(1), resultSet.getDouble(2));
                } catch (SQLException e) {
                    // Ignore columns that can't be read as doubles
                }
            }

            return map;
        } catch (SQLException e) {
            LOG.info("Unable to read value", e);
            databaseComponent.removeConnection();
        } finally {
            close(statement, resultSet);
        }

        return Collections.emptyMap();
    }

    private static void bindParameters(PreparedStatement statement, Object... parameters) throws SQLException {
        int i = 1;
        for (Object p : parameters) {
            if (p instanceof String) {
                statement.setString(i++, (String) p);
            } else if (p instanceof Number) {
                statement.setDouble(i++, ((Number) p).doubleValue());
            } else {
                statement.setObject(i++, p);
            }
        }
    }

    public static String[] getColumns(ResultSetMetaData rsmd) throws SQLException {
        String[] names = new String[rsmd.getColumnCount()];
        for (int i = 0; i < rsmd.getColumnCount(); i++) {
            names[i] = rsmd.getColumnName(i + 1);
        }

        return names;
    }

    public static class StatementParameter {
        private String name;
        private String value;
    }

    public static void close(Statement statement, ResultSet resultSet) {
        if (resultSet != null) {
            try {
                resultSet.close();
            } catch (SQLException e) {
            }
        }

        if (statement != null) {
            try {
                statement.close();
            } catch (SQLException e) {
            }
        }
    }
}