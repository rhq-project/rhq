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

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import mazz.i18n.Logger;

/**
 * Superclass of all versions of the Oracle database.
 *
 * @author John Mazzitelli
 *
 */
public abstract class OracleDatabaseType extends DatabaseType {
    private static final Logger LOG = DbUtilsI18NFactory.getLogger(OracleDatabaseType.class);

    /**
     * The vendor name for all Oracle databases.
     */
    public static final String VENDOR = "oracle";

    /**
     * Returns {@link #VENDOR}.
     *
     * @see DatabaseType#getVendor()
     */
    @Override
    public String getVendor() {
        return VENDOR;
    }

    /**
     * Oracle needs 1 for true and 0 for false.
     *
     * @see DatabaseType#getBooleanValue(boolean)
     */
    @Override
    public String getBooleanValue(boolean bool) {
        return bool ? "1" : "0";
    }

    /* (non-Javadoc)
     * @see org.rhq.core.db.DatabaseType#getInteger(java.lang.Object)
     *
     * Oracle stores integer fields as Numbers and returns a BigDecimal.  It is assumed <code>number</code> is actually
     * an integer value, otherwise precision will be lost in this conversion.
     */
    @Override
    public Integer getInteger(Object number) {
        BigDecimal intField = (BigDecimal) number;
        return intField.intValue();
    }

    /* (non-Javadoc)
     * @see org.rhq.core.db.DatabaseType#getLong(java.lang.Object)
     *
     * Oracle stores long fields as Numbers and returns a BigDecimal.  It is assumed <code>number</code> is actually
     * a long value, otherwise precision will be lost in this conversion.
     */
    @Override
    public Long getLong(Object number) {
        BigDecimal longField = (BigDecimal) number;
        return longField.longValue();
    }

    /* (non-Javadoc)
     * @see org.rhq.core.db.DatabaseType#getString(java.lang.String, int)
     *
     * Oracle has a hard limit of 4000 bytes for varchar/varchar2 storage. Make sure the returned String
     * is trimmed as needed to satisfy the constraint.
     */
    @Override
    public String getString(String varchar, int maxLength) {
        if (null == varchar) {
            return null;
        }

        // First meet the character limit.
        String result = super.getString(varchar, maxLength);

        // Now, ensure we can store the resulting number of bytes by clipping off the last character until we reach
        // an acceptable number of bytes. This is not super-efficient but hopefully won't happen all that often. We can't
        // just convert to bytes and clip to 4000, because it could leave an incomplete multi-byte character at the end.
        while (result.getBytes().length > 4000) {
            result = result.substring(0, result.length() - 1);
        }

        return result;
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

    public String getSequenceInsertValue(Connection conn, String sequenceName) {
        return sequenceName + ".NEXTVAL";
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

        try {
            executeSql(conn, sql);
        } catch (SQLException e) {
            // Oracle throws an exception if you try to set nullable to its current setting. Ignore errors
            // generated when a nullable setting is already the way we want it to be.
            // ORA-01442: column to be modified to NOT NULL is already NOT NULL
            // ORA-01451: column to be modified to NULL cannot be modified to NULL
            if (nullable != null) {
                String msg = e.getMessage();
                if (msg.contains("ORA-01442") || msg.contains("ORA-01451")) {
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("Ignoring exception, column already set to nullable=" + nullable, e);
                    }
                } else {
                    throw e;
                }
            }
        }

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

    /* (non-Javadoc)
     * @see org.rhq.core.db.DatabaseType#getLimitClause(int)
     *
     * Oracle processes ORDER BY after the limit, so in general the limit clause is in a sub-query when
     * ordering is required.  See http://www.oracle.com/technetwork/issue-archive/2006/06-sep/o56asktom-086197.html.
     */
    @Override
    public String getLimitClause(int limit) {
        return " ROWNUM <= " + limit + " ";
    }

}