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
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Superclass of all versions of the SQL Server database.
 *
 * @author Joseph Marques
 */
public abstract class SQLServerDatabaseType extends DatabaseType {

    public static final String SEQ_SUFFIX = "_ID_SEQ";
    public static final String BAD_SEQ_NAME = "Sequences named must end in " + SEQ_SUFFIX;
    public static final String SEQ_ERROR_MSG = "SQL Server does not support sequences, use identifier columns instead";

    /**
     * The vendor name for all SQL Server databases.
     */
    public static final String VENDOR_NAME = "sqlserver";

    /**
     * Returns {@link #VENDOR_NAME}.
     *
     * @see DatabaseType#getVendor()
     */
    public String getVendor() {
        return VENDOR_NAME;
    }

    public String getHibernateDialect() {
        return "org.hibernate.dialect.SQLServerDialect";
    }

    /**
     * @see DatabaseType#isTableNotFoundException(SQLException)
     */
    public boolean isTableNotFoundException(SQLException e) {
        String msg = e.getMessage();
        msg = (msg == null) ? "" : msg.trim().toLowerCase();
        return msg.contains("invalid object name");
    }

    /**
     * Assumes the sequence table is the value of the table argument followed by "_", the value of the key argument and
     * then "_seq".
     *
     * @see DatabaseType#getSequenceValue(Connection, String, String)
     */
    public int getSequenceValue(Connection conn, String table, String key) throws SQLException {
        // if (true) so the compiler is fooled into believing there can be a return value
        if (true) {
            throw new FeatureNotSupportedException(SEQ_ERROR_MSG);
        }
        return 0;
    }

    /**
     * @see DatabaseType#getNextSequenceValue(Connection, String, String)
     */
    public int getNextSequenceValue(Connection conn, String table, String key) throws SQLException {
        // if (true) so the compiler is fooled into believing there can be a return value
        if (true) {
            throw new FeatureNotSupportedException(SEQ_ERROR_MSG);
        }
        return 0;
    }

    /**
     * @see DatabaseType#createSequence(Connection, String, String, String)
     */
    public void createSequence(Connection conn, String name, String initial, String increment) throws SQLException {
        // since this method backs the SST_CreateSequence task, which is used several times in db-upgrade.xml already,
        // we'll fake it here by adding an identity column to the table, derived from the consistent sequence name 
        name = name.toUpperCase();
        if (!name.endsWith(SEQ_SUFFIX)) {
            throw new FeatureNotSupportedException(SEQ_ERROR_MSG);
        }

        String tableName = name.substring(0, name.length() - SEQ_SUFFIX.length());
        String alterTableStatement = "ALTER TABLE " + tableName + " ALTER COLUMN ID IDENTITY(" + initial + ", "
            + increment + ")";

        executeSql(conn, alterTableStatement);
    }

    /**
     * @see DatabaseType#alterColumn(Connection, String, String, String, String, String, Boolean, Boolean)
     */
    public void alterColumn(Connection conn, String table, String column, String generic_column_type,
        String default_value, String precision, Boolean nullable, Boolean reindex) throws SQLException {
        String db_column_type = null;
        List<String> sql_list = new ArrayList<String>();

        if (generic_column_type != null) {
            db_column_type = getDBTypeFromGenericType(generic_column_type);

            if (precision != null) {
                db_column_type += " (" + precision + ")";
            }

            sql_list.add("ALTER TABLE " + table + " ALTER COLUMN " + column + " " + db_column_type);
        }

        if (default_value != null) {
            sql_list.add("ALTER TABLE " + table + " ALTER COLUMN " + column + " SET DEFAULT '" + default_value + "'");
        }

        if (nullable != null) {
            if (nullable.booleanValue()) {
                sql_list.add("ALTER TABLE " + table + " ALTER " + column + " SET NULL");
            } else {
                sql_list.add("ALTER TABLE " + table + " ALTER " + column + " SET NOT NULL");
            }
        }

        executeSql(conn, sql_list);

        // now that we've altered the column, let's reindex if we were told to do so
        if ((reindex != null) && reindex.booleanValue()) {
            reindexTable(conn, table);
        }

        return;
    }

    /**
     * @see DatabaseType#reindexTable(Connection, String)
     */
    public void reindexTable(Connection conn, String table) throws SQLException {
        // DBREINDEX( table_name [ , index_name [ , fillfactor ] ]
        // quote table name
        // pass " " for index_name, indicating all indexes should be rebuilt
        // pass 0 for fillfactor, indicating previous fillfactor should be used
        String reindexSql = "DBCC DBREINDEX (\"" + table + "\", \" \", 0)";
        executeSql(conn, reindexSql);
    }
}