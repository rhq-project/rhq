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
package org.rhq.core.db.log;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.Savepoint;
import java.sql.Statement;
import java.util.Map;

/**
 * Wraps a connection.
 */
public class LoggerConnection implements Connection {
    private LoggerDriver loggerDriver;
    private Connection realConnection;
    private LoggerListener listener;

    /**
     * Creates a new {@link LoggerConnection} object that wraps a connection.
     *
     * @param driver the driver that created this connection object
     * @param conn   the actual connection being wrapped
     */
    public LoggerConnection(LoggerDriver driver, Connection conn) {
        loggerDriver = driver;
        listener = loggerDriver.getLoggerListener();
        realConnection = conn;
    }

    /**
     * Given an isolation level number, simply returns its string representation.
     *
     * @param  iso_level
     *
     * @return the name of the isolation level
     */
    private String getIsolationLevelString(int iso_level) {
        switch (iso_level) {
        case Connection.TRANSACTION_NONE: {
            return "NONE";
        }

        case Connection.TRANSACTION_READ_UNCOMMITTED: {
            return "READ_UNCOMMITTED";
        }

        case Connection.TRANSACTION_READ_COMMITTED: {
            return "READ_COMMITTED";
        }

        case Connection.TRANSACTION_REPEATABLE_READ: {
            return "REPEATABLE_READ";
        }

        case Connection.TRANSACTION_SERIALIZABLE: {
            return "SERIALIZABLE";
        }

        default: {
            return "UNKNOWN ISOLATION LEVEL!";
        }
        }
    }

    /**
     * Logs a non-SQL notification, prefixing it with the connection toString.
     *
     * @param str
     */
    private void log(String str) {
        listener.log(realConnection.toString() + ':' + str);
    }

    /**
     * @see java.sql.Connection#createStatement()
     */
    public Statement createStatement() throws SQLException {
        Statement s = realConnection.createStatement();

        log("createStatement() -> " + s);

        return new LoggerStatement(loggerDriver, realConnection, s);
    }

    /**
     * @see java.sql.Connection#prepareStatement(java.lang.String)
     */
    public PreparedStatement prepareStatement(String s) throws SQLException {
        PreparedStatement ps = realConnection.prepareStatement(s);

        log("prepareStatement(" + s + ") -> " + ps);

        return new LoggerPreparedStatement(loggerDriver, realConnection, ps, s);
    }

    /**
     * @see java.sql.Connection#prepareCall(java.lang.String)
     */
    public CallableStatement prepareCall(String s) throws SQLException {
        CallableStatement cs = realConnection.prepareCall(s);

        log("prepareCall(" + s + ") -> " + cs);

        return cs;
    }

    /**
     * @see java.sql.Connection#nativeSQL(java.lang.String)
     */
    public String nativeSQL(String s) throws SQLException {
        String native_sql = realConnection.nativeSQL(s);

        log("nativeSQL(" + s + ") -> " + native_sql);

        return native_sql;
    }

    /**
     * @see java.sql.Connection#setAutoCommit(boolean)
     */
    public void setAutoCommit(boolean flag) throws SQLException {
        log("setAutoCommmit(" + flag + ')');

        realConnection.setAutoCommit(flag);
    }

    /**
     * @see java.sql.Connection#getAutoCommit()
     */
    public boolean getAutoCommit() throws SQLException {
        boolean result = realConnection.getAutoCommit();

        log("getAutoCommmit() -> " + result);

        return result;
    }

    /**
     * @see java.sql.Connection#commit()
     */
    public void commit() throws SQLException {
        log("commit()");

        realConnection.commit();
    }

    /**
     * @see java.sql.Connection#rollback()
     */
    public void rollback() throws SQLException {
        log("rollback()");

        realConnection.rollback();
    }

    /**
     * @see java.sql.Connection#close()
     */
    public void close() throws SQLException {
        log("close()");

        realConnection.close();
    }

    /**
     * @see java.sql.Connection#isClosed()
     */
    public boolean isClosed() throws SQLException {
        boolean results = realConnection.isClosed();

        log("isClosed() -> " + results);

        return results;
    }

    /**
     * @see java.sql.Connection#getMetaData()
     */
    public DatabaseMetaData getMetaData() throws SQLException {
        DatabaseMetaData results = realConnection.getMetaData();

        log("getMetaData() -> " + results);

        return results;
    }

    /**
     * @see java.sql.Connection#setReadOnly(boolean)
     */
    public void setReadOnly(boolean flag) throws SQLException {
        log("setReadOnly(" + flag + ')');

        realConnection.setReadOnly(flag);
    }

    /**
     * @see java.sql.Connection#isReadOnly()
     */
    public boolean isReadOnly() throws SQLException {
        boolean results = realConnection.isReadOnly();

        log("isReadOnly() -> " + results);

        return results;
    }

    /**
     * @see java.sql.Connection#setCatalog(java.lang.String)
     */
    public void setCatalog(String s) throws SQLException {
        log("setCatalog(" + s + ')');

        realConnection.setCatalog(s);
    }

    /**
     * @see java.sql.Connection#getCatalog()
     */
    public String getCatalog() throws SQLException {
        String results = realConnection.getCatalog();

        log("getCatalog() -> " + results);

        return results;
    }

    /**
     * @see java.sql.Connection#setTransactionIsolation(int)
     */
    public void setTransactionIsolation(int i) throws SQLException {
        log("setTransactionIsolation(" + getIsolationLevelString(i) + ')');

        realConnection.setTransactionIsolation(i);
    }

    /**
     * @see java.sql.Connection#getTransactionIsolation()
     */
    public int getTransactionIsolation() throws SQLException {
        int results = realConnection.getTransactionIsolation();

        log("getTransactionIsolation() -> " + getIsolationLevelString(results));

        return results;
    }

    /**
     * @see java.sql.Connection#getWarnings()
     */
    public SQLWarning getWarnings() throws SQLException {
        SQLWarning results = realConnection.getWarnings();

        log("getWarnings() -> " + results);

        return results;
    }

    /**
     * @see java.sql.Connection#clearWarnings()
     */
    public void clearWarnings() throws SQLException {
        log("clearWarnings()");

        realConnection.clearWarnings();
    }

    /**
     * @see java.sql.Connection#createStatement(int, int)
     */
    public Statement createStatement(int i, int j) throws SQLException {
        Statement s = realConnection.createStatement(i, j);

        log("createStatement(" + i + ',' + j + ") -> " + s);

        return new LoggerStatement(loggerDriver, realConnection, s);
    }

    /**
     * @see java.sql.Connection#prepareStatement(java.lang.String, int, int)
     */
    public PreparedStatement prepareStatement(String sql, int i, int j) throws SQLException {
        PreparedStatement ps = realConnection.prepareStatement(sql, i, j);

        log("prepareStatement(" + sql + ',' + i + ',' + j + ") -> " + ps);

        return new LoggerPreparedStatement(loggerDriver, realConnection, ps, sql);
    }

    /**
     * @see java.sql.Connection#prepareCall(java.lang.String, int, int)
     */
    public CallableStatement prepareCall(String sql, int i, int j) throws SQLException {
        CallableStatement cs = realConnection.prepareCall(sql, i, j);

        log("prepareCall(" + sql + ',' + i + ',' + j + ") -> " + cs);

        return cs;
    }

    /**
     * @see java.sql.Connection#getTypeMap()
     */
    @SuppressWarnings("unchecked")
    public Map getTypeMap() throws SQLException {
        Map<String, Class<?>> results = realConnection.getTypeMap();

        log("getTypeMap() -> " + results);

        return results;
    }

    /**
     * @see java.sql.Connection#setTypeMap(java.util.Map)
     */
    @SuppressWarnings("unchecked")
    public void setTypeMap(Map map) throws SQLException {
        log("setTypeMap(" + map + ')');

        realConnection.setTypeMap(map);
    }

    /**
     * @see java.sql.Connection#setHoldability(int)
     */
    public void setHoldability(int i) throws SQLException {
        log("setHoldability(" + i + ')');

        realConnection.setHoldability(i);
    }

    /**
     * @see java.sql.Connection#getHoldability()
     */
    public int getHoldability() throws SQLException {
        int results = realConnection.getHoldability();

        log("getHoldability() -> " + results);

        return results;
    }

    /**
     * @see java.sql.Connection#setSavepoint()
     */
    public Savepoint setSavepoint() throws SQLException {
        Savepoint sp = realConnection.setSavepoint();

        log("setSavepoint() -> " + sp);

        return sp;
    }

    /**
     * @see java.sql.Connection#setSavepoint(java.lang.String)
     */
    public Savepoint setSavepoint(String s) throws SQLException {
        Savepoint sp = realConnection.setSavepoint(s);

        log("setSavepoint(" + s + ") -> " + sp);

        return sp;
    }

    /**
     * @see java.sql.Connection#rollback(java.sql.Savepoint)
     */
    public void rollback(Savepoint sp) throws SQLException {
        log("rollback(" + sp + ')');

        realConnection.rollback(sp);
    }

    /**
     * @see java.sql.Connection#releaseSavepoint(java.sql.Savepoint)
     */
    public void releaseSavepoint(Savepoint sp) throws SQLException {
        log("releaseSavepoint(" + sp + ')');

        realConnection.releaseSavepoint(sp);
    }

    /**
     * @see java.sql.Connection#createStatement(int, int, int)
     */
    public Statement createStatement(int i, int j, int k) throws SQLException {
        Statement results = realConnection.createStatement(i, j, k);

        log("createStatement(" + i + ',' + j + ',' + k + ") -> " + results);

        return new LoggerStatement(loggerDriver, realConnection, results);
    }

    /**
     * @see java.sql.Connection#prepareStatement(java.lang.String, int, int, int)
     */
    public PreparedStatement prepareStatement(String sql, int i, int j, int k) throws SQLException {
        PreparedStatement results = realConnection.prepareStatement(sql, i, j, k);

        log("prepareStatement(" + sql + ',' + i + ',' + j + ',' + k + ") -> " + results);

        return new LoggerPreparedStatement(loggerDriver, realConnection, results, sql);
    }

    /**
     * @see java.sql.Connection#prepareCall(java.lang.String, int, int, int)
     */
    public CallableStatement prepareCall(String sql, int i, int j, int k) throws SQLException {
        CallableStatement results = realConnection.prepareCall(sql, i, j, k);

        log("prepareCall(" + sql + ',' + i + ',' + j + ',' + k + ") -> " + results);

        return results;
    }

    /**
     * @see java.sql.Connection#prepareStatement(java.lang.String, int)
     */
    public PreparedStatement prepareStatement(String sql, int i) throws SQLException {
        PreparedStatement results = realConnection.prepareStatement(sql, i);

        log("prepareStatement(" + sql + ',' + i + ") -> " + results);

        return new LoggerPreparedStatement(loggerDriver, realConnection, results, sql);
    }

    /**
     * @see java.sql.Connection#prepareStatement(java.lang.String, int[])
     */
    public PreparedStatement prepareStatement(String sql, int[] ai) throws SQLException {
        PreparedStatement results = realConnection.prepareStatement(sql, ai);

        log("prepareStatement(" + sql + ',' + ai + ") -> " + results);

        return new LoggerPreparedStatement(loggerDriver, realConnection, results, sql);
    }

    /**
     * @see java.sql.Connection#prepareStatement(java.lang.String, java.lang.String[])
     */
    public PreparedStatement prepareStatement(String sql, String[] as) throws SQLException {
        PreparedStatement results = realConnection.prepareStatement(sql, as);

        log("prepareStatement(" + sql + ',' + as + ") -> " + results);

        return new LoggerPreparedStatement(loggerDriver, realConnection, results, sql);
    }
}