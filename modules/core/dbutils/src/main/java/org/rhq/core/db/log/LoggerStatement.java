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

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.Statement;

/**
 * Wraps a statement.
 *
 * @author John Mazzitelli
 *
 */
public class LoggerStatement implements Statement {
    private LoggerDriver loggerDriver;
    private Connection realConnection;
    private Statement realStatement;
    private LoggerListener listener;

    /**
     * Constructor for {@link LoggerStatement}.
     *
     * @param driver
     * @param conn
     * @param stmt
     */
    public LoggerStatement(LoggerDriver driver, Connection conn, Statement stmt) {
        loggerDriver = driver;
        listener = loggerDriver.getLoggerListener();
        realStatement = stmt;
        realConnection = conn;
    }

    /**
     * Logs a non-SQL notification, prefixing it with the statement toString.
     *
     * @param str
     */
    protected void log(String str) {
        listener.log(realStatement.toString() + ':' + str);
    }

    /**
     * Logs a SQL notification, prefixing it with the statement toString.
     *
     * @param str
     */
    protected void logSQL(String str) {
        listener.logSQL(realStatement.toString() + ':' + str);
    }

    /**
     * Logs a SQL notification that was executed along with the time it took to execute, prefixing it with the statement
     * toString.
     *
     * @param str
     * @param duration the time it took to execute the SQL
     */
    protected void logSQL(String str, long duration) {
        listener.logSQL(realStatement + ": (DURATION=" + duration + " ms) " + str);
    }

    /**
     * Returns the real statement that this wrapper is wrapping.
     *
     * @return statement
     */
    protected Statement getRealStatement() {
        return realStatement;
    }

    /**
     * @see java.sql.Statement#executeQuery(java.lang.String)
     */
    public ResultSet executeQuery(String s) throws SQLException {
        logSQL("executeQuery(" + s + ")");

        long start = System.currentTimeMillis();
        ResultSet rs = realStatement.executeQuery(s);
        long duration = System.currentTimeMillis() - start;

        logSQL("executeQuery(" + s + ") -> " + rs, duration);

        return rs;
    }

    /**
     * @see java.sql.Statement#executeUpdate(java.lang.String)
     */
    public int executeUpdate(String s) throws SQLException {
        logSQL("executeUpdate(" + s + ")");

        long start = System.currentTimeMillis();
        int rval = realStatement.executeUpdate(s);
        long duration = System.currentTimeMillis() - start;

        logSQL("executeUpdate(" + s + ") -> " + rval, duration);

        return rval;
    }

    /**
     * @see java.sql.Statement#close()
     */
    public void close() throws SQLException {
        log("close()");
        realStatement.close();
    }

    /**
     * @see java.sql.Statement#getMaxFieldSize()
     */
    public int getMaxFieldSize() throws SQLException {
        int results = realStatement.getMaxFieldSize();
        log("getMaxFieldSize() -> " + results);
        return results;
    }

    /**
     * @see java.sql.Statement#setMaxFieldSize(int)
     */
    public void setMaxFieldSize(int i) throws SQLException {
        log("setMaxFieldSize(" + i + ')');
        realStatement.setMaxFieldSize(i);
    }

    /**
     * @see java.sql.Statement#getMaxRows()
     */
    public int getMaxRows() throws SQLException {
        int results = realStatement.getMaxRows();
        log("getMaxRows() -> " + results);
        return results;
    }

    /**
     * @see java.sql.Statement#setMaxRows(int)
     */
    public void setMaxRows(int i) throws SQLException {
        log("getMaxRows(" + i + ')');
        realStatement.setMaxRows(i);
    }

    /**
     * @see java.sql.Statement#setEscapeProcessing(boolean)
     */
    public void setEscapeProcessing(boolean flag) throws SQLException {
        log("setEscapeProcessing(" + flag + ')');
        realStatement.setEscapeProcessing(flag);
    }

    /**
     * @see java.sql.Statement#getQueryTimeout()
     */
    public int getQueryTimeout() throws SQLException {
        int results = realStatement.getQueryTimeout();
        log("getQueryTimeout() -> " + results);
        return results;
    }

    /**
     * @see java.sql.Statement#setQueryTimeout(int)
     */
    public void setQueryTimeout(int i) throws SQLException {
        log("setQueryTimeout(" + i + ')');
        realStatement.setQueryTimeout(i);
    }

    /**
     * @see java.sql.Statement#cancel()
     */
    public void cancel() throws SQLException {
        log("cancel()");
        realStatement.close();
    }

    /**
     * @see java.sql.Statement#getWarnings()
     */
    public SQLWarning getWarnings() throws SQLException {
        SQLWarning results = realStatement.getWarnings();
        log("getWarnings() -> " + results);
        return results;
    }

    /**
     * @see java.sql.Statement#clearWarnings()
     */
    public void clearWarnings() throws SQLException {
        log("clearWarnings()");
        realStatement.clearWarnings();
    }

    /**
     * @see java.sql.Statement#setCursorName(java.lang.String)
     */
    public void setCursorName(String s) throws SQLException {
        log("setCursorName(" + s + ')');
        realStatement.setCursorName(s);
    }

    /**
     * @see java.sql.Statement#execute(java.lang.String)
     */
    public boolean execute(String s) throws SQLException {
        logSQL("execute(" + s + ")");

        long start = System.currentTimeMillis();
        boolean rval = realStatement.execute(s);
        long duration = System.currentTimeMillis() - start;

        logSQL("execute(" + s + ") -> " + rval, duration);
        return rval;
    }

    /**
     * @see java.sql.Statement#getResultSet()
     */
    public ResultSet getResultSet() throws SQLException {
        ResultSet rs = realStatement.getResultSet();
        log("getResultSet() -> " + rs);
        return rs;
    }

    /**
     * @see java.sql.Statement#getUpdateCount()
     */
    public int getUpdateCount() throws SQLException {
        int results = realStatement.getUpdateCount();
        log("getUpdateCount() -> " + results);
        return results;
    }

    /**
     * @see java.sql.Statement#getMoreResults()
     */
    public boolean getMoreResults() throws SQLException {
        boolean results = realStatement.getMoreResults();
        log("getMoreResults() -> " + results);
        return results;
    }

    /**
     * @see java.sql.Statement#setFetchDirection(int)
     */
    public void setFetchDirection(int i) throws SQLException {
        log("setFetchDirection(" + i + ')');
        realStatement.setFetchDirection(i);
    }

    /**
     * @see java.sql.Statement#getFetchDirection()
     */
    public int getFetchDirection() throws SQLException {
        int results = realStatement.getFetchDirection();
        log("getFetchDirection() -> " + results);
        return results;
    }

    /**
     * @see java.sql.Statement#setFetchSize(int)
     */
    public void setFetchSize(int i) throws SQLException {
        log("setFetchSize(" + i + ')');
        realStatement.setFetchSize(i);
    }

    /**
     * @see java.sql.Statement#getFetchSize()
     */
    public int getFetchSize() throws SQLException {
        int results = realStatement.getFetchSize();
        log("getFetchSize() -> " + results);
        return results;
    }

    /**
     * @see java.sql.Statement#getResultSetConcurrency()
     */
    public int getResultSetConcurrency() throws SQLException {
        int results = realStatement.getResultSetConcurrency();
        log("getResultSetConcurrency() -> " + results);
        return results;
    }

    /**
     * @see java.sql.Statement#getResultSetType()
     */
    public int getResultSetType() throws SQLException {
        int results = realStatement.getResultSetType();
        log("getResultSetType() -> " + results);
        return results;
    }

    /**
     * @see java.sql.Statement#addBatch(java.lang.String)
     */
    public void addBatch(String s) throws SQLException {
        log("addBatch(" + s + ')');
        realStatement.addBatch(s);
    }

    /**
     * @see java.sql.Statement#clearBatch()
     */
    public void clearBatch() throws SQLException {
        log("clearBatch()");
        realStatement.clearBatch();
    }

    /**
     * @see java.sql.Statement#executeBatch()
     */
    public int[] executeBatch() throws SQLException {
        int[] results = realStatement.executeBatch();
        log("executeBatch() -> " + results);
        return results;
    }

    /**
     * @see java.sql.Statement#getConnection()
     */
    public Connection getConnection() throws SQLException {
        log("getConnection()");
        return realConnection;
    }

    /**
     * @see java.sql.Statement#getMoreResults(int)
     */
    public boolean getMoreResults(int i) throws SQLException {
        boolean results = realStatement.getMoreResults(i);
        log("getMoreResults(" + i + ") -> " + results);
        return results;
    }

    /**
     * @see java.sql.Statement#getGeneratedKeys()
     */
    public ResultSet getGeneratedKeys() throws SQLException {
        ResultSet results = realStatement.getGeneratedKeys();
        log("getGeneratedKeys() -> " + results);
        return results;
    }

    /**
     * @see java.sql.Statement#executeUpdate(java.lang.String, int)
     */
    public int executeUpdate(String s, int i) throws SQLException {
        logSQL("executeUpdate(" + s + ")");

        long start = System.currentTimeMillis();
        int rval = realStatement.executeUpdate(s, i);
        long duration = System.currentTimeMillis() - start;

        logSQL("executeUpdate(" + s + ',' + i + ") -> " + rval, duration);

        return rval;
    }

    /**
     * @see java.sql.Statement#executeUpdate(java.lang.String, int[])
     */
    public int executeUpdate(String s, int[] ai) throws SQLException {
        logSQL("executeUpdate(" + s + ")");

        long start = System.currentTimeMillis();
        int rval = realStatement.executeUpdate(s, ai);
        long duration = System.currentTimeMillis() - start;

        logSQL("executeUpdate(" + s + ',' + ai + ") -> " + rval, duration);

        return rval;
    }

    /**
     * @see java.sql.Statement#executeUpdate(java.lang.String, java.lang.String[])
     */
    public int executeUpdate(String s, String[] as) throws SQLException {
        logSQL("executeUpdate(" + s + ")");

        long start = System.currentTimeMillis();
        int rval = realStatement.executeUpdate(s, as);
        long duration = System.currentTimeMillis() - start;

        logSQL("executeUpdate(" + s + ',' + as + ") -> " + rval, duration);

        return rval;
    }

    /**
     * @see java.sql.Statement#execute(java.lang.String, int)
     */
    public boolean execute(String s, int i) throws SQLException {
        logSQL("execute(" + s + ")");

        long start = System.currentTimeMillis();
        boolean rval = realStatement.execute(s, i);
        long duration = System.currentTimeMillis() - start;

        logSQL("execute(" + s + ',' + i + ") -> " + rval, duration);

        return rval;
    }

    /**
     * @see java.sql.Statement#execute(java.lang.String, int[])
     */
    public boolean execute(String s, int[] ai) throws SQLException {
        logSQL("execute(" + s + ")");

        long start = System.currentTimeMillis();
        boolean rval = realStatement.execute(s, ai);
        long duration = System.currentTimeMillis() - start;

        logSQL("execute(" + s + ',' + ai + ") -> " + rval, duration);

        return rval;
    }

    /**
     * @see java.sql.Statement#execute(java.lang.String, java.lang.String[])
     */
    public boolean execute(String s, String[] as) throws SQLException {
        logSQL("execute(" + s + ")");

        long start = System.currentTimeMillis();
        boolean rval = realStatement.execute(s, as);
        long duration = System.currentTimeMillis() - start;

        logSQL("execute(" + s + ',' + as + ") -> " + rval, duration);

        return rval;
    }

    /**
     * @see java.sql.Statement#getResultSetHoldability()
     */
    public int getResultSetHoldability() throws SQLException {
        int results = realStatement.getResultSetHoldability();
        log("getResultSetHoldability() -> " + results);
        return results;
    }
}