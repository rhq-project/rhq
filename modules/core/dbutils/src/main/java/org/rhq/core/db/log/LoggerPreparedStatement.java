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

import java.io.InputStream;
import java.io.Reader;
import java.math.BigDecimal;
import java.net.URL;
import java.sql.Array;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.Date;
import java.sql.ParameterMetaData;
import java.sql.PreparedStatement;
import java.sql.Ref;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.List;
import java.util.StringTokenizer;
import java.util.Vector;

/**
 * Wraps a prepared statement.
 */
public class LoggerPreparedStatement extends LoggerStatement implements PreparedStatement {
    private String itsSQL = null;
    private List<Object> itsArgs = new Vector<Object>();

    /**
     * Creates a new {@link LoggerPreparedStatement} object.
     *
     * @param driver
     * @param conn
     * @param ps
     * @param sql
     */
    public LoggerPreparedStatement(LoggerDriver driver, Connection conn, PreparedStatement ps, String sql) {
        super(driver, conn, ps);
        itsSQL = sql;
    }

    private PreparedStatement getRealPreparedStatement() {
        return (PreparedStatement) getRealStatement();
    }

    /**
     * Sets the argument at the given index, expanding the size of the vector if it needs to be expanded.
     *
     * @param index
     * @param o
     */
    private void setArg(int index, Object o) {
        while (index >= itsArgs.size()) {
            itsArgs.add(null);
        }

        itsArgs.set(index, o);

        return;
    }

    /**
     * Tries to replace all ? in the prepared statement with their actual values. If there is a literal "?" in the
     * query, this will return bogus SQL. At most 64 arguments can be replaced.
     *
     * @return SQL with parameter values replacing the ? placeholders
     */
    public String getProcessedSQL() {
        // Works as long as the only '?' chars
        // in the query are there for substituting variables (in other words,
        // if it also contains a literal '?', then this will break)
        StringTokenizer st = new StringTokenizer(itsSQL, "?");
        String sql = "";
        for (int i = 1; st.hasMoreTokens() && (i <= (itsArgs.size() - 1)); i++) {
            sql += st.nextToken();
            sql += formatSQLString(itsArgs.get(i));
        }

        // Add the rest of the stuff (there should really be only 1 more
        // token, unless there were not enough args to substitute for).
        while (st.hasMoreTokens()) {
            sql += st.nextToken();
        }

        return sql;
    }

    /**
     * Tries to format the SQL argument by quoting Strings, Dates and Times.
     *
     * @param  arg
     *
     * @return the SQL argument, possibly quoted
     */
    private String formatSQLString(Object arg) {
        if (arg == null) {
            return "NULL";
        }

        if ((arg instanceof String) || (arg instanceof URL)) {
            String s = (String) arg;
            return "'" + s.replaceAll("'", "\\\\'") + "'";
        }

        if ((arg instanceof Date) || (arg instanceof Time) || (arg instanceof Timestamp)) {
            return "'" + arg.toString() + "'";
        }

        return arg.toString();
    }

    /**
     * @see java.sql.PreparedStatement#executeQuery()
     */
    public ResultSet executeQuery() throws SQLException {
        String processed_sql = getProcessedSQL();
        logSQL("executeQuery(" + processed_sql + ")");

        long start = System.currentTimeMillis();
        ResultSet rs = getRealPreparedStatement().executeQuery();
        long duration = System.currentTimeMillis() - start;

        logSQL("executeQuery() -> " + rs + ": " + processed_sql, duration);
        return rs;
    }

    /**
     * @see java.sql.PreparedStatement#executeUpdate()
     */
    public int executeUpdate() throws SQLException {
        String processed_sql = getProcessedSQL();
        logSQL("executeUpdate(" + processed_sql + ")");

        long start = System.currentTimeMillis();
        int rval = getRealPreparedStatement().executeUpdate();
        long duration = System.currentTimeMillis() - start;

        logSQL("executeUpdate() -> " + rval + ": " + processed_sql, duration);
        return rval;
    }

    /**
     * @see java.sql.PreparedStatement#setNull(int, int)
     */
    public void setNull(int i, int j) throws SQLException {
        log("setNull(" + i + ',' + j + ')');
        setArg(i, null);
        getRealPreparedStatement().setNull(i, j);
    }

    /**
     * @see java.sql.PreparedStatement#setBoolean(int, boolean)
     */
    public void setBoolean(int i, boolean flag) throws SQLException {
        log("setBoolean(" + i + ',' + flag + ')');
        setArg(i, new Boolean(flag));
        getRealPreparedStatement().setBoolean(i, flag);
    }

    /**
     * @see java.sql.PreparedStatement#setByte(int, byte)
     */
    public void setByte(int i, byte byte0) throws SQLException {
        log("setByte(" + i + ',' + byte0 + ')');
        setArg(i, new Byte(byte0));
        getRealPreparedStatement().setByte(i, byte0);
    }

    /**
     * @see java.sql.PreparedStatement#setShort(int, short)
     */
    public void setShort(int i, short word0) throws SQLException {
        log("setShort(" + i + ',' + word0 + ')');
        setArg(i, new Short(word0));
        getRealPreparedStatement().setShort(i, word0);
    }

    /**
     * @see java.sql.PreparedStatement#setInt(int, int)
     */
    public void setInt(int i, int j) throws SQLException {
        log("setInt(" + i + ',' + j + ')');
        setArg(i, new Integer(j));
        getRealPreparedStatement().setInt(i, j);
    }

    /**
     * @see java.sql.PreparedStatement#setLong(int, long)
     */
    public void setLong(int i, long l) throws SQLException {
        log("setLong(" + i + ',' + l + ')');
        setArg(i, new Long(l));
        getRealPreparedStatement().setLong(i, l);
    }

    /**
     * @see java.sql.PreparedStatement#setFloat(int, float)
     */
    public void setFloat(int i, float f) throws SQLException {
        log("setFloat(" + i + ',' + f + ')');
        setArg(i, new Float(f));
        getRealPreparedStatement().setFloat(i, f);
    }

    /**
     * @see java.sql.PreparedStatement#setDouble(int, double)
     */
    public void setDouble(int i, double d) throws SQLException {
        log("setDouble(" + i + ',' + d + ')');
        setArg(i, new Double(d));
        getRealPreparedStatement().setDouble(i, d);
    }

    /**
     * @see java.sql.PreparedStatement#setBigDecimal(int, java.math.BigDecimal)
     */
    public void setBigDecimal(int i, BigDecimal bigdecimal) throws SQLException {
        log("setBigDecimal(" + i + ',' + bigdecimal + ')');
        setArg(i, bigdecimal);
        getRealPreparedStatement().setBigDecimal(i, bigdecimal);
    }

    /**
     * @see java.sql.PreparedStatement#setString(int, java.lang.String)
     */
    public void setString(int i, String s) throws SQLException {
        log("setString(" + i + ',' + s + ')');
        setArg(i, s);
        getRealPreparedStatement().setString(i, s);
    }

    /**
     * @see java.sql.PreparedStatement#setBytes(int, byte[])
     */
    public void setBytes(int i, byte[] abyte0) throws SQLException {
        log("setBytes(" + i + ',' + abyte0 + ')');
        setArg(i, abyte0);
        getRealPreparedStatement().setBytes(i, abyte0);
    }

    /**
     * @see java.sql.PreparedStatement#setDate(int, java.sql.Date)
     */
    public void setDate(int i, Date date) throws SQLException {
        log("setDate(" + i + ',' + date + ')');
        setArg(i, date);
        getRealPreparedStatement().setDate(i, date);
    }

    /**
     * @see java.sql.PreparedStatement#setTime(int, java.sql.Time)
     */
    public void setTime(int i, Time time) throws SQLException {
        log("setTime(" + i + ',' + time + ')');
        setArg(i, time);
        getRealPreparedStatement().setTime(i, time);
    }

    /**
     * @see java.sql.PreparedStatement#setTimestamp(int, java.sql.Timestamp)
     */
    public void setTimestamp(int i, Timestamp timestamp) throws SQLException {
        log("setTimestamp(" + i + ',' + timestamp + ')');
        setArg(i, timestamp);
        getRealPreparedStatement().setTimestamp(i, timestamp);
    }

    /**
     * @see java.sql.PreparedStatement#setAsciiStream(int, java.io.InputStream, int)
     */
    public void setAsciiStream(int i, InputStream inputstream, int j) throws SQLException {
        log("setAsciiStream(" + i + ",<STREAM>," + j + ')');
        getRealPreparedStatement().setAsciiStream(i, inputstream, j);
    }

    /**
     * @see java.sql.PreparedStatement#setUnicodeStream(int, java.io.InputStream, int)
     */
    public void setUnicodeStream(int i, InputStream inputstream, int j) throws SQLException {
        log("setUnicodeStream(" + i + ",<STREAM>," + j + ')');
        getRealPreparedStatement().setUnicodeStream(i, inputstream, j);
    }

    /**
     * @see java.sql.PreparedStatement#setBinaryStream(int, java.io.InputStream, int)
     */
    public void setBinaryStream(int i, InputStream inputstream, int j) throws SQLException {
        log("setBinaryStream(" + i + ",<STREAM>," + j + ')');
        getRealPreparedStatement().setBinaryStream(i, inputstream, j);
    }

    /**
     * @see java.sql.PreparedStatement#clearParameters()
     */
    public void clearParameters() throws SQLException {
        log("clearParameters()");
        itsArgs.clear();
        getRealPreparedStatement().clearParameters();
    }

    /**
     * @see java.sql.PreparedStatement#setObject(int, java.lang.Object, int, int)
     */
    public void setObject(int i, Object obj, int j, int k) throws SQLException {
        log("setObject(" + i + ',' + obj + ',' + j + ',' + k + ')');
        setArg(i, obj);
        getRealPreparedStatement().setObject(i, obj, j, k);
    }

    /**
     * @see java.sql.PreparedStatement#setObject(int, java.lang.Object, int)
     */
    public void setObject(int i, Object obj, int j) throws SQLException {
        log("setObject(" + i + ',' + obj + ',' + j + ')');
        setArg(i, obj);
        getRealPreparedStatement().setObject(i, obj, j);
    }

    /**
     * @see java.sql.PreparedStatement#setObject(int, java.lang.Object)
     */
    public void setObject(int i, Object obj) throws SQLException {
        log("setObject(" + i + ',' + obj + ')');
        setArg(i, obj);
        getRealPreparedStatement().setObject(i, obj);
    }

    /**
     * @see java.sql.PreparedStatement#execute()
     */
    public boolean execute() throws SQLException {
        String processed_sql = getProcessedSQL();
        logSQL("execute(" + processed_sql + ")");

        long start = System.currentTimeMillis();
        boolean rval = getRealPreparedStatement().execute();
        long duration = System.currentTimeMillis() - start;

        logSQL("execute() -> " + rval + ": " + processed_sql, duration);
        return rval;
    }

    /**
     * @see java.sql.PreparedStatement#addBatch()
     */
    public void addBatch() throws SQLException {
        log("addBatch()");
        getRealPreparedStatement().addBatch();
    }

    /**
     * @see java.sql.PreparedStatement#setCharacterStream(int, java.io.Reader, int)
     */
    public void setCharacterStream(int i, Reader reader, int j) throws SQLException {
        log("setCharacterStream(" + i + ",<READER>," + j + ')');
        getRealPreparedStatement().setCharacterStream(i, reader, j);
    }

    /**
     * @see java.sql.PreparedStatement#setRef(int, java.sql.Ref)
     */
    public void setRef(int i, Ref ref) throws SQLException {
        log("setRef(" + i + ',' + ref + ')');
        getRealPreparedStatement().setRef(i, ref);
    }

    /**
     * @see java.sql.PreparedStatement#setBlob(int, java.sql.Blob)
     */
    public void setBlob(int i, Blob blob) throws SQLException {
        log("setBlob(" + i + ',' + blob.getClass() + ')');
        getRealPreparedStatement().setBlob(i, blob);
    }

    /**
     * @see java.sql.PreparedStatement#setClob(int, java.sql.Clob)
     */
    public void setClob(int i, Clob clob) throws SQLException {
        log("setClob(" + i + ',' + clob.getClass() + ')');
        getRealPreparedStatement().setClob(i, clob);
    }

    /**
     * @see java.sql.PreparedStatement#setArray(int, java.sql.Array)
     */
    public void setArray(int i, Array array) throws SQLException {
        log("setArray(" + i + ',' + array.getClass() + ')');
        getRealPreparedStatement().setArray(i, array);
    }

    /**
     * @see java.sql.PreparedStatement#getMetaData()
     */
    public ResultSetMetaData getMetaData() throws SQLException {
        ResultSetMetaData results = getRealPreparedStatement().getMetaData();
        log("getMetaData() -> " + results);
        return results;
    }

    /**
     * @see java.sql.PreparedStatement#setDate(int, java.sql.Date, java.util.Calendar)
     */
    public void setDate(int i, Date date, Calendar calendar) throws SQLException {
        log("setDate(" + i + ',' + date + ',' + calendar + ')');
        getRealPreparedStatement().setDate(i, date, calendar);
    }

    /**
     * @see java.sql.PreparedStatement#setTime(int, java.sql.Time, java.util.Calendar)
     */
    public void setTime(int i, Time time, Calendar calendar) throws SQLException {
        log("setTime(" + i + ',' + time + ',' + calendar + ')');
        getRealPreparedStatement().setTime(i, time, calendar);
    }

    /**
     * @see java.sql.PreparedStatement#setTimestamp(int, java.sql.Timestamp, java.util.Calendar)
     */
    public void setTimestamp(int i, Timestamp timestamp, Calendar calendar) throws SQLException {
        log("setTimestamp(" + i + ',' + timestamp + ',' + calendar + ')');
        getRealPreparedStatement().setTimestamp(i, timestamp, calendar);
    }

    /**
     * @see java.sql.PreparedStatement#setNull(int, int, java.lang.String)
     */
    public void setNull(int i, int j, String s) throws SQLException {
        log("setNull(" + i + ',' + j + ',' + s + ')');
        setArg(i, null);
        getRealPreparedStatement().setNull(i, j, s);
    }

    /**
     * @see java.sql.PreparedStatement#setURL(int, java.net.URL)
     */
    public void setURL(int i, URL url) throws SQLException {
        log("setURL(" + i + ',' + url + ')');
        setArg(i, url);
        getRealPreparedStatement().setURL(i, url);
    }

    /**
     * @see java.sql.PreparedStatement#getParameterMetaData()
     */
    public ParameterMetaData getParameterMetaData() throws SQLException {
        ParameterMetaData results = getRealPreparedStatement().getParameterMetaData();
        log("getParameterMetaData() -> " + results);
        return results;
    }
}