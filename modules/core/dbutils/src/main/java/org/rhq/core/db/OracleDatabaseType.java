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
package org.rhq.core.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Superclass of all versions of the Oracle database.
 *
 * @author John Mazzitelli
 *
 */
public abstract class OracleDatabaseType extends DatabaseType {
    /**
     * The vendor name for all Oracle databases.
     */
    public static final String VENDOR = "oracle";

    /**
     * Returns {@link #VENDOR}.
     *
     * @see DatabaseType#getVendor()
     */
    public String getVendor() {
        return VENDOR;
    }

    /**
     * Oracle needs 1 for true and 0 for false.
     *
     * @see DatabaseType#getBooleanValue(boolean)
     */
    public String getBooleanValue(boolean bool) {
        return bool ? "1" : "0";
    }

    /**
     * For Oracle databases, the boolean parameter will actually be of type "int" with a value of 0 or 1.
     *
     * @see DatabaseType#setBooleanValue(boolean, PreparedStatement, int)
     */
    public void setBooleanValue(boolean bool, PreparedStatement ps, int idx) throws SQLException {
        ps.setInt(idx, (bool) ? 1 : 0);
    }

    /**
     * @see DatabaseType#isTableNotFoundException(SQLException)
     */
    public boolean isTableNotFoundException(SQLException e) {
        // ORA-00942: table or view does not exist
        return (e.getErrorCode() == 942);
    }

    /**
     * @see DatabaseType#getSequenceValue(Connection, String, String)
     */
    public int getSequenceValue(Connection conn, String table, String key) throws SQLException {
        String query = "SELECT " + table + "_" + key + "_seq.currval FROM DUAL";
        PreparedStatement selectPS = null;
        ResultSet rs = null;

        try {
            selectPS = conn.prepareStatement(query);
            rs = selectPS.executeQuery();

            if (rs.next()) {
                return rs.getInt(1);
            }

            throw new RuntimeException(DbUtilsI18NFactory.getMsg()
                .getMsg(DbUtilsI18NResourceKeys.NOT_A_SEQUENCE, query));
        } finally {
            closeJDBCObjects(null, selectPS, rs);
        }
    }

    /**
     * @see DatabaseType#getNextSequenceValue(Connection, String, String)
     */
    public int getNextSequenceValue(Connection conn, String table, String key) throws SQLException {
        String query = "SELECT " + table + "_" + key + "_seq.nextval FROM DUAL";

        PreparedStatement selectPS = null;
        ResultSet rs = null;

        try {
            selectPS = conn.prepareStatement(query);
            rs = selectPS.executeQuery();

            if (rs.next()) {
                return rs.getInt(1);
            }

            throw new RuntimeException(DbUtilsI18NFactory.getMsg()
                .getMsg(DbUtilsI18NResourceKeys.NOT_A_SEQUENCE, query));
        } finally {
            closeJDBCObjects(null, selectPS, rs);
        }
    }

    /**
     * @see DatabaseType#createSequence(Connection, String, String, String)
     */
    public void createSequence(Connection conn, String name, String initial, String increment) throws SQLException {
        String sql = "CREATE SEQUENCE " + name + " START WITH " + initial + " INCREMENT BY " + increment
            + " NOMAXVALUE NOCYCLE CACHE 10";

        executeSql(conn, sql);
    }

    /**
     * @see DatabaseType#alterColumn(Connection, String, String, String, String, String, Boolean, Boolean)
     */
    public void alterColumn(Connection conn, String table, String column, String generic_column_type,
        String default_value, String precision, Boolean nullable, Boolean reindex) throws SQLException {
        String db_column_type;
        String sql = "ALTER TABLE " + table + " MODIFY (" + column;

        if (generic_column_type != null) {
            db_column_type = getDBTypeFromGenericType(generic_column_type);
            sql += " " + db_column_type;
        }

        if (default_value != null) {
            sql += " DEFAULT '" + default_value + "'";
        }

        if (precision != null) {
            sql += " (" + precision + ")";
        }

        if (nullable != null) {
            sql += (nullable.booleanValue() ? " NULL" : " NOT NULL");
        }

        sql += ")";

        executeSql(conn, sql);

        if ((reindex != null) && reindex.booleanValue()) {
            reindexTable(conn, table);
        }

        return;
    }

    /**
     * @see DatabaseType#reindexTable(Connection, String)
     */
    public void reindexTable(Connection conn, String table) throws SQLException {
        PreparedStatement selectPS = null;
        ResultSet rs = null;
        List<String> sql_list = new ArrayList<String>();

        try {
            selectPS = conn.prepareStatement("SELECT index_name FROM user_indexes WHERE table_name = '" + table
                + "' AND index_type = 'NORMAL'");
            rs = selectPS.executeQuery();
            while (rs.next()) {
                String indexName = rs.getString(1);
                sql_list.add("ALTER INDEX " + indexName + " REBUILD");
            }

            executeSql(conn, sql_list);
        } finally {
            closeResultSet(rs);
            closeStatement(selectPS);
        }
    }
}