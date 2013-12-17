/*
 * RHQ Management Platform
 * Copyright (C) 2005-2013 Red Hat, Inc.
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

package org.rhq.plugins.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.util.exception.ThrowableUtil;

/**
 * Various database (JDBC) query functions.
 *
 * @author Greg Hinkle
 * @deprecated as of RHQ 4.10, use {@link DatabasePluginUtil} instead.
 */
@Deprecated
public class DatabaseQueryUtility {

    private static final Log LOG = LogFactory.getLog(DatabaseQueryUtility.class);

    /**
     * @deprecated instantiating a static utility class doesn't make sense. Don't do it.
     */
    @Deprecated
    public DatabaseQueryUtility() {}

    /**
     * Executes a database update.
     *
     * @param databaseComponent
     * @param query
     * @param parameters
     * @return
     * @throws SQLException
     */
    @Deprecated
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

    /**
     * Returns the result of a query as a single Double value.
     * Returns {@link Double#NaN} if the query fails.
     */
    @Deprecated
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
     * Executes a query, returning the results as a map where the keys
     * are the column names and values are the value of that column.
     * Note depending on the database, the column names may be uppercase (Oracle) or lowercase.
     *
     * @param  databaseComponent
     * @param  query SQL query string
     * @param  parameters optional bind parameters
     *
     * @return a map of query results
     */
    @Deprecated
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
     * Returns a list of values, one per row, containing a map of column names to values of that row.
     * Note depending on the database, the column names may be uppercase (Oracle) or lowercase.
     * @param databaseComponent database to query
     * @param query SQL query
     * @param parameters parameters to bind to the query
     *
     * @throws SQLException if query fails
     */
    @Deprecated
    public static List<Map<String, Object>> getGridValues(DatabaseComponent databaseComponent, String query,
        Object... parameters) throws SQLException {
        PreparedStatement statement = null;
        ResultSet resultSet = null;
        List<Map<String, Object>> l = new ArrayList<Map<String, Object>>();
        try {
            statement = databaseComponent.getConnection().prepareStatement(query);
            bindParameters(statement, parameters);

            resultSet = statement.executeQuery();

            while (resultSet.next()) {
                Map<String, Object> row = new HashMap<String, Object>();
                l.add(row);

                ResultSetMetaData md = resultSet.getMetaData();
                String[] names = getColumns(md);

                for (String name : names) {
                    Object o = resultSet.getObject(name);
                    row.put(name, o);
                }
            }

        } finally {
            close(statement, resultSet);
        }
        return l;

    }

    /**
     * Returns a mapping of rows as key-value pairs where the key is the
     * first column (a string) and the second column is a value numeric.
     *
     * @param  databaseComponent the component to execute on
     * @param  query             the sql query to run
     * @param  parameters        any parameters to bind first
     *
     * @return a Map<String,Double> of the keys against the value
     */
    @Deprecated
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
                    if (LOG.isTraceEnabled()) {
                        LOG.trace("A query column value is not a double, ignoring:" + ThrowableUtil.getAllMessages(e));
                    }
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

    /**
     * Binds arguments to a prepared statement.
     */
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

    /**
     * Returns an array of strings as upper-case column names.
     */
    @Deprecated
    public static String[] getColumns(ResultSetMetaData rsmd) throws SQLException {
        String[] names = new String[rsmd.getColumnCount()];
        for (int i = 0; i < rsmd.getColumnCount(); i++) {
            names[i] = rsmd.getColumnName(i + 1);
        }

        return names;
    }

    /**
     * Closes statements and result sets.
     */
    @Deprecated
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

    /**
     * @deprecated This class is not used for anything in the codebase of the database plugin. If you are using it
     * in some way or another, move it to your own code, because this class will be removed in future.
     */
    @Deprecated
    public static class StatementParameter {
        private String name;
        private String value;
    }
}
