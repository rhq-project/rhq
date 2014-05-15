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

import org.rhq.core.pluginapi.inventory.ResourceComponent;
import org.rhq.core.util.exception.ThrowableUtil;

/**
 * @author Thomas Segismont
 */
public class DatabasePluginUtil {
    private static final Log LOG = LogFactory.getLog(DatabasePluginUtil.class);

    public static final class ComponentCannotProvideConnectionException extends IllegalArgumentException {
        private final ResourceComponent component;

        public ComponentCannotProvideConnectionException(ResourceComponent component) {
            super(component + " cannot provide a JDBC Connection");
            this.component = component;
        }

        public ResourceComponent getComponent() {
            return component;
        }
    }

    /**
     * @return false unless <code>component</code> is not null and supports connection pooling or is an instance of
     * {@link DatabaseComponent}. 
     */
    public static boolean canProvideConnection(ResourceComponent component) {
        return hasConnectionPoolingSupport(component) || component instanceof DatabaseComponent;
    }

    /**
     * Determines if a resource component supports connection pooling.
     *
     * @param component the resource component
     * @return false unless <code>component</code> is not null, implements
     * {@link org.rhq.plugins.database.ConnectionPoolingSupport} and a call to
     * {@link ConnectionPoolingSupport#supportsConnectionPooling()} returns true.
     */
    public static boolean hasConnectionPoolingSupport(ResourceComponent component) {
        if (component instanceof ConnectionPoolingSupport) {
            return ((ConnectionPoolingSupport) component).supportsConnectionPooling();
        }
        return false;
    }

    /**
     * Gets a {@link Connection} from the <code>component</code>.
     *
     * @param component a resource component that must be able to provide a {@link Connection}.
     * @throws SQLException if <code>component</code> supports connection pooling and a pooled connection could not be
     * retrieved.
     * @throws IllegalArgumentException if resource component is null or cannot provide a {@link Connection} (does not
     * support connection pooling and does not implement {@link DatabaseComponent}).
     */
    public static Connection getConnectionFromComponent(ResourceComponent component) throws SQLException {
        if (hasConnectionPoolingSupport(component)) {
            return getConnectionFromPool((ConnectionPoolingSupport) component);
        }
        if (component instanceof DatabaseComponent) {
            return getConnectionFromDatabaseComponent((DatabaseComponent) component);
        }
        throw new ComponentCannotProvideConnectionException(component);
    }

    /**
     * Executes a query, returning the results as a map where the keys are the column names and values are the value of
     * that column. Note that depending on the database, the column names may be uppercase (Oracle) or lowercase.
     *
     * @param  component
     * @param  query SQL query string
     * @param  parameters optional bind parameters
     *
     * @return a map of query results
     */
    public static Map<String, Double> getNumericQueryValues(ResourceComponent component, String query,
        Object... parameters) {

        boolean componentHasConnectionPoolingSupport = hasConnectionPoolingSupport(component);
        checkComponent(component, componentHasConnectionPoolingSupport);

        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet resultSet = null;
        try {
            connection = getConnection0(component, componentHasConnectionPoolingSupport);
            statement = connection.prepareStatement(query);
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
            if (!componentHasConnectionPoolingSupport) {
                ((DatabaseComponent) component).removeConnection();
            }
        } finally {
            safeClose(null, statement, resultSet);
            if (componentHasConnectionPoolingSupport) {
                safeClose(connection);
            }
        }

        return Collections.emptyMap();
    }

    /**
     * Returns a mapping of rows as key-value pairs where the key is the first column (a string) and the second column
     * is a value numeric.
     *
     * @param  component the component to execute on
     * @param  query             the sql query to run
     * @param  parameters        any parameters to bind first
     *
     * @return a Map<String,Double> of the keys against the value
     */
    public static Map<String, Double> getNumericQueryValueMap(ResourceComponent component, String query,
        Object... parameters) {

        boolean componentHasConnectionPoolingSupport = hasConnectionPoolingSupport(component);
        checkComponent(component, componentHasConnectionPoolingSupport);

        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet resultSet = null;
        try {
            connection = getConnection0(component, componentHasConnectionPoolingSupport);
            statement = connection.prepareStatement(query);
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
            if (!componentHasConnectionPoolingSupport) {
                ((DatabaseComponent) component).removeConnection();
            }
        } finally {
            safeClose(null, statement, resultSet);
            if (componentHasConnectionPoolingSupport) {
                safeClose(connection);
            }
        }

        return Collections.emptyMap();
    }

    /**
     * Returns the result of a query as a single Double value.
     * Returns {@link Double#NaN} if the query fails.
     */
    public static Double getSingleNumericQueryValue(ResourceComponent component, String query, Object... parameters) {

        boolean componentHasConnectionPoolingSupport = hasConnectionPoolingSupport(component);
        checkComponent(component, componentHasConnectionPoolingSupport);

        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet resultSet = null;
        try {
            connection = getConnection0(component, componentHasConnectionPoolingSupport);
            statement = connection.prepareStatement(query);
            bindParameters(statement, parameters);
            resultSet = statement.executeQuery();

            if (resultSet.next()) {
                return resultSet.getDouble(1);
            }
        } catch (SQLException e) {
            if (!componentHasConnectionPoolingSupport) {
                ((DatabaseComponent) component).removeConnection();
            }
        } finally {
            safeClose(null, statement, resultSet);
            if (componentHasConnectionPoolingSupport) {
                safeClose(connection);
            }
        }

        return Double.NaN;
    }

    /**
     * Returns a list of values, one per row, containing a map of column names to values of that row.
     * Note depending on the database, the column names may be uppercase (Oracle) or lowercase.
     * @param component database to query
     * @param query SQL query
     * @param parameters parameters to bind to the query
     *
     * @throws SQLException if query fails
     */
    public static List<Map<String, Object>> getGridValues(ResourceComponent component, String query,
        Object... parameters) throws SQLException {

        boolean componentHasConnectionPoolingSupport = hasConnectionPoolingSupport(component);
        checkComponent(component, componentHasConnectionPoolingSupport);

        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet resultSet = null;
        List<Map<String, Object>> l = new ArrayList<Map<String, Object>>();
        try {
            connection = getConnection0(component, componentHasConnectionPoolingSupport);
            statement = connection.prepareStatement(query);
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
            safeClose(null, statement, resultSet);
            if (componentHasConnectionPoolingSupport) {
                safeClose(connection);
            }
        }
        return l;

    }

    public static void safeClose(Connection connection) {
        if (connection != null) {
            try {
                connection.close();
            } catch (Exception ignore) {
            }
        }
    }

    public static void safeClose(Connection connection, Statement statement) {
        safeClose(statement);
        safeClose(connection);
    }

    public static void safeClose(Connection connection, Statement statement, ResultSet resultSet) {
        safeClose(resultSet);
        safeClose(statement);
        safeClose(connection);
    }

    public static void safeClose(Statement statement) {
        if (statement != null) {
            try {
                statement.close();
            } catch (Exception ignore) {
            }
        }
    }

    public static void safeClose(ResultSet resultSet) {
        if (resultSet != null) {
            try {
                resultSet.close();
            } catch (Exception ignore) {
            }
        }
    }

    private static void checkComponent(ResourceComponent component, boolean componentHasConnectionPoolingSupport) {
        if (!componentHasConnectionPoolingSupport && !(component instanceof DatabaseComponent)) {
            throw new ComponentCannotProvideConnectionException(component);
        }
    }

    private static Connection getConnection0(ResourceComponent component, boolean componentHasConnectionPoolingSupport)
        throws SQLException {
        return componentHasConnectionPoolingSupport ? getConnectionFromPool((ConnectionPoolingSupport) component)
            : getConnectionFromDatabaseComponent((DatabaseComponent) component);
    }

    private static Connection getConnectionFromPool(ConnectionPoolingSupport component) throws SQLException {
        return component.getPooledConnectionProvider().getPooledConnection();
    }

    private static Connection getConnectionFromDatabaseComponent(DatabaseComponent component) {
        return component.getConnection();
    }

    private static void bindParameters(PreparedStatement statement, Object... parameters) throws SQLException {
        int i = 1;
        for (Object p : parameters) {
            if (p instanceof String) {
                statement.setString(i++, (String) p);
            } else if (p instanceof Byte) {
                statement.setByte(i++, (Byte) p);
            } else if (p instanceof Short) {
                statement.setShort(i++, (Short) p);
            } else if (p instanceof Integer) {
                statement.setInt(i++, (Integer) p);
            } else if (p instanceof Long) {
                statement.setLong(i++, (Long) p);
            } else if (p instanceof Float) {
                statement.setFloat(i++, (Float) p);
            } else if (p instanceof Double) {
                statement.setDouble(i++, (Double) p);
            } else {
                statement.setObject(i++, p);
            }
        }
    }

    private static String[] getColumns(ResultSetMetaData rsmd) throws SQLException {
        String[] names = new String[rsmd.getColumnCount()];
        for (int i = 0; i < rsmd.getColumnCount(); i++) {
            names[i] = rsmd.getColumnName(i + 1);
        }
        return names;
    }

    private DatabasePluginUtil() {
        // Utility class
    }
}
