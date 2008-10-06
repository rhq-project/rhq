 /*
  * RHQ Management Platform
  * Copyright (C) 2005-2008 Red Hat, Inc.
  * All rights reserved.
  *
  * This program is free software; you can redistribute it and/or modify
  * it under the terms of the GNU General Public License, version 2, as
  * published by the Free Software Foundation, and/or the GNU Lesser
  * General Public License, version 2.1, also as published by the Free
  * Software Foundation.
  *
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
  * GNU General Public License and the GNU Lesser General Public License
  * for more details.
  *
  * You should have received a copy of the GNU General Public License
  * and the GNU Lesser General Public License along with this program;
  * if not, write to the Free Software Foundation, Inc.,
  * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
  */
package org.rhq.core.util.jdbc;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Take care of some JDBC related stuff. Mostly closing resources
 *
 * @author Heiko W. Rupp
 */
public class JDBCUtil {

    private static final Log log = LogFactory.getLog(JDBCUtil.class);
    private static final String SQL_ERROR = "Error closing a resource: ";

    /*
     * The next constants specify how to generate the next value in a sequence for
     * table %s
     */
    private static final String POSTGRES_NEXTVAL_SQL = "(SELECT nextval('%s_id_seq'::text))";
    private static final String ORACLE_NEXTVAL_SQL = "%s_id_seq.nextval";

    public static void safeClose(Statement stm, ResultSet rs) {
        safeClose(rs);
        safeClose(stm);
    }

    public static void safeClose(Connection conn, Statement stm, ResultSet rs) {
        safeClose(rs);
        safeClose(stm);
        safeClose(conn);
    }

    public static void safeClose(Connection con) {
        if (con != null) {
            try {
                con.close();
            } catch (Exception e) {
                log.error(SQL_ERROR, e);
            }
        }
    }

    public static void safeClose(ResultSet rs) {
        if (rs != null) {
            try {
                rs.close();
            } catch (Exception e) {
                log.error(SQL_ERROR, e);
            }
        }
    }

    public static void safeClose(Statement statement) {
        if (statement != null) {
            try {
                statement.close();
            } catch (Exception e) {
                log.error(SQL_ERROR, e);
            }
        }
    }

    /**
     * Bind the passed values to the parameters of the {@link PreparedStatement}, starting at position startBinding
     *
     * @param  ps              PreparedStatement to use
     * @param  values          the array of int values to fill in
     * @param  startingBinding the starting position of the parameter to fill in
     *
     * @throws SQLException if the {@link Statement}.setInt() call fails
     */
    public static void bindNTimes(PreparedStatement ps, int[] values, int startingBinding) throws SQLException {
        for (int i = 0; i < values.length; i++) {
            ps.setInt(startingBinding + i, values[i]);
        }
    }

    /**
     * Populate the passed query with bindCount number of placeholders '?'. The method will not put necessary parenteses
     * around the placeholders.
     *
     * @param  query       the original query including text to replace
     * @param  replaceable the part of the original query that should be replaced by '?'s.
     * @param  bindCount   how many placeholders '?' should be generated
     *
     * @return The modified query
     */
    public static String transformQueryForMultipleInParameters(String query, String replaceable, int bindCount) {
        String replacement = generateInBinds(bindCount);
        return query.replace(replaceable, replacement);
    }

    /**
     * Generate count '? separated by comma
     */
    public static String generateInBinds(int count) {
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < count; i++) {
            if (i > 0) {
                b.append(",");
            }

            b.append("?");
        }

        return b.toString();
    }

    /**
     * Generate the correct SQL statement to obtain the next value from a sequence/table
     * generator for the passed table. The passed connection gets closed in case of an error.
     * @param conn  A valid database connection
     * @param tableName The name of the table to use
     * @return A statement that obtains the next value for the passed table 
     */
    public static String getNextValSql(Connection conn, String tableName) {
        String nextvalSql;
        try {
            DatabaseMetaData meta = conn.getMetaData();
            String name = meta.getDatabaseProductName().toLowerCase();
            if (name.contains("postgres")) {
                nextvalSql = POSTGRES_NEXTVAL_SQL;
            } else if (name.contains("oracle")) {
                nextvalSql = ORACLE_NEXTVAL_SQL;
            } else {
                JDBCUtil.safeClose(conn);
                throw new IllegalStateException("Unsupported database type: " + name);
            }
        } catch (Exception e) {
            JDBCUtil.safeClose(conn);
            throw new IllegalStateException("Failed to determine database type.");
        }

        nextvalSql = String.format(nextvalSql, tableName);
        return nextvalSql;
    }

    /**
     * @deprecated This should correctly be handled at the place where the stream is about to be closed JBNADM-2600
     */
    @Deprecated
    public static void safeClose(InputStream in) {
        if (in != null) {
            try {
                in.close();
            } catch (Exception e) {
                log.error(SQL_ERROR, e);
            }
        }
    }

    /**
     * @deprecated This should correctly be handled at the place where the stream is about to be closed JBNADM-2600
     */
    @Deprecated
    public static void safeClose(OutputStream out) {
        if (out != null) {
            try {
                out.close();
            } catch (Exception e) {
                log.error(SQL_ERROR, e);
            }
        }
    }

    /**
     * @deprecated This should correctly be handled at the place where the stream is about to be closed JBNADM-2600
     */
    @Deprecated
    public static void safeClose(Reader reader) {
        if (reader != null) {
            try {
                reader.close();
            } catch (Exception e) {
                log.error(SQL_ERROR, e);
            }
        }
    }

    /**
     * @deprecated This should correctly be handled at the place where the stream is about to be closed JBNADM-2600
     */
    @Deprecated
    public static void safeClose(Writer writer) {
        if (writer != null) {
            try {
                writer.close();
            } catch (Exception e) {
                log.error(SQL_ERROR, e);
            }
        }
    }
}