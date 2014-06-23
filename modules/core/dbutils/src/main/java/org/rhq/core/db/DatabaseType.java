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
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import mazz.i18n.Logger;

import org.rhq.core.db.builders.CreateSequenceExprBuilder;

/**
 * A vendor-specific database with some vendor-specific method implementations in order to do things necessary for each
 * individual type of database. This abstract class provides some default implementations; vendor-specific subclasses
 * are free to override their behavior.
 *
 * @author John Mazzitelli
 *
 */
public abstract class DatabaseType {

    private static final Logger LOG = DbUtilsI18NFactory.getLogger(DatabaseType.class);

    /**
     * Returns a vendor-specific, version-specific database name. This is the name used in the ANT tasks and XML files
     * when needing to specify a specific type of database. You can define {@link TypeMap database-specific typemaps}
     * for a specific version of the vendor's databases if this vendor/version-unique name is used in the mapping.
     *
     * @return name of database, which is also version specific
     */
    public abstract String getName();

    /**
     * Returns a vendor-specific database name. This is the name of the database, without specifying a specific version.
     * For example, this can return "oracle" for the database types that represent Oracle8, Oracle9 and Oracle10. You
     * can define {@link TypeMap database-specific typemaps} across all versions of the vendor's databases if this
     * vendor name is used in the mapping.
     *
     * @return name of database vendor
     */
    public abstract String getVendor();

    /**
     * Returns the version number of the database, as a String.
     *
     * @return database version
     */
    public abstract String getVersion();

    /**
     * Return the hibernate dialect, that can be used in &lt;property name="hibernate.dialect" value="" /  >
     * expressions within persistence.xml
     *
     * @return  classname of the hibernate dialect
     */
    public abstract String getHibernateDialect();

    /**
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return getName() + "(" + getVendor() + ":" + getVersion() + ")";
    }

    /**
     * Returns <code>true</code> if this database type's vendor and version match those of the given vendor and given
     * version strings. If either the vendor or version strings do not match, <code>false</code> is returned. If either
     * <code>vendor</code> or <code>version</code> is <code>null</code>, that argument is ignored and not compared. If
     * you just want to compare vendor strings, for example, pass in a <code>null</code> version string. When comparing
     * the strings, the comparision will be case-insensitive.
     *
     * @param  vendor
     * @param  version
     *
     * @return <code>true</code> if the given vendor and version match this database type
     */
    public boolean matches(String vendor, String version) {
        if (vendor != null) {
            if (!vendor.equalsIgnoreCase(getVendor())) {
                return false;
            }
        }

        if (version != null) {
            if (!version.equalsIgnoreCase(getVersion())) {
                return false;
            }
        }

        return true;
    }

    /**
     * Given a generic type, this will return an analogous type that is specific to this database.
     *
     * @param  generic_type a generic type name
     *
     * @return a database specific type name
     */
    public String getDBTypeFromGenericType(String generic_type) {
        return TypeMap.getMappedType(TypeMap.loadKnownTypeMaps(), generic_type, this);
    }

    /**
     * Close a database connection. No exception is thrown if it fails, but a warning is logged.
     *
     * @param c The connection to close (may be <code>null</code>)
     */
    public void closeConnection(Connection c) {
        if (c == null) {
            return;
        }

        try {
            c.close();
        } catch (Exception e) {
            LOG.warn(e, DbUtilsI18NResourceKeys.DBTYPE_CLOSE_CONN_ERROR, e);
        }
    }

    /**
     * Close a database statement. No exception is thrown if it fails, but a warning is logged.
     *
     * @param s The statement to close (may be <code>null</code>)
     */
    public void closeStatement(Statement s) {
        if (s == null) {
            return;
        }

        try {
            s.close();
        } catch (Exception e) {
            LOG.warn(e, DbUtilsI18NResourceKeys.DBTYPE_CLOSE_STATEMENT_ERROR, e);
        }
    }

    /**
     * Close a database result set. No exception is thrown if it fails, but a warning is logged.
     *
     * @param rs The result set to close (may be <code>null</code>)
     */
    public void closeResultSet(ResultSet rs) {
        if (rs == null) {
            return;
        }

        try {
            rs.close();
        } catch (Exception e) {
            LOG.warn(e, DbUtilsI18NResourceKeys.DBTYPE_CLOSE_RESULTSET_ERROR, e);
        }
    }

    /**
     * Close a connection, statement, and result set in one method call. You can pass <code>null</code> for any argument
     * to ignore it. No exception is thrown if any close fails, but warnings will be logged.
     *
     * @param c  The connection to close.
     * @param s  The statement set to close.
     * @param rs The result set to close.
     */
    public void closeJDBCObjects(Connection c, Statement s, ResultSet rs) {
        closeResultSet(rs);
        closeStatement(s);
        closeConnection(c);
    }

    /**
     * Counts the number of rows in a result set.
     *
     * @param  num_rows_already_read
     * @param  result_set
     *
     * @return number of rows currently in the row set, plus the <code>num_rows_already_read</code>
     *
     * @throws SQLException
     */
    public int countRows(int num_rows_already_read, ResultSet result_set) throws SQLException {
        int row_count = num_rows_already_read;
        int rs_type = result_set.getType();

        switch (rs_type) {
        // Dumb Oracle driver, makes you manually flip thru the whole result set just to count the rows
        case ResultSet.TYPE_FORWARD_ONLY: {
            while (result_set.next()) {
                row_count++;
            }

            return row_count;
        }

        // Nice Postgres driver, JDBC 2.0 single method call...
        default: {
            // we can ignore num_rows_already_read, this gives us the total number
            if (result_set.last()) {
                return result_set.getRow();
            }

            return 0;
        }
        }
    }

    /**
     * Check to see if a column exists in a table.
     *
     * @param     conn   The DB connection to use.
     * @param     table  The table to check.
     * @param     column The column to look for. This is done in a case-insensitive manner.
     *
     * @return    <code>true</code> if the column exists in the table, <code>false</code> otherwise
     *
     * @throws SQLException
     */
    public boolean checkColumnExists(Connection conn, String table, String column) throws SQLException {
        PreparedStatement ps = null;
        ResultSet rs = null;
        ResultSetMetaData rsmd;
        String checkColumnSql = "SELECT * FROM " + table + " WHERE 1=0";

        try {
            ps = conn.prepareStatement(checkColumnSql);
            rs = ps.executeQuery();
            rsmd = rs.getMetaData();
            int numCols = rsmd.getColumnCount();
            for (int i = 0; i < numCols; i++) {
                if (rsmd.getColumnName(i + 1).equalsIgnoreCase(column)) {
                    return true;
                }
            }

            return false;
        } finally {
            closeJDBCObjects(null, ps, rs);
        }
    }

    /**
     * Get the value for a boolean as a string as required for the specific database.
     *
     * @param  bool the boolean value to be converted to the DB string version of the boolean
     *
     * @return the appropriate boolean string for the specific database
     */
    public String getBooleanValue(boolean bool) {
        return bool ? "true" : "false";
    }

    /**
     * Get the Integer representation of the number type supplied by the db vendor for an integer field value.
     * The default implementation simply applies a cast to the passed in number and is appropriate for DB types
     * that support a native integer field type (like Postgres).  Other db types should override this method
     * (like Oracle).  
     * 
     * @param number
     * @return
     */
    public Integer getInteger(Object number) {
        return (Integer) number;
    }

    /**
     * Get the Long representation of the number type supplied by the db vendor for a long field value.
     * The default implementation simply applies a cast to the passed in number and is appropriate for DB types
     * that support a native long field type.  Other db types should override this method (like Oracle).
     *
     * @param number
     * @return
     */
    public Long getLong(Object number) {
        return (Long) number;
    }

    /**
     * Different vendors have different rules regarding varchar/varchar2 string storage.  In particular, Oracle
     * has a hard limit of 4000 bytes (not characters, bytes).  Make sure we trim to maxLength (in characters)
     * while also meeting vendor-specific constraints.
     *
     * @param varchar  The String to be stored as a varchar/varchar2
     * @param maxLength max length of the DB field, in characters.
     * @return The string, safe for storage to the DB field
     */
    public String getString(String varchar, int maxLength) {
        if (null == varchar || varchar.length() <= maxLength) {
            return varchar;
        }

        return varchar.substring(0, maxLength);
    }

    /**
     * Fill out a <code>PreparedStatement</code> correctly with a boolean.
     *
     * @param  bool the boolean you want
     * @param  ps   the prepapred statement where the boolean will be stored
     * @param  idx  the index that corresponds to the boolean parameter in the statement
     *
     * @throws SQLException
     */
    public void setBooleanValue(boolean bool, PreparedStatement ps, int idx) throws SQLException {
        ps.setBoolean(idx, bool);
    }

    /**
     * Determines if the given table exists in the database. Note that if the table does not exist a call
     * to this method will cause an underlying SQLException, which will invalidate the current transaction.
     *
     * @param  conn  connection to the database where the table to check is
     * @param  table the table to check for existence
     *
     * @return <code>true</code> if the table exists in the database, false if the table does not exist AND
     * the implementation determined the information WITHOUT generating an SQLException (meaning the
     * transaction has not been invalidated. Otherwise, throws an exception which should be handled.
     *
     * @throws IllegalStateException if the check generated an expected "table does not exist SQLException (note, the 
     * exception invalidates the transaction).
     * @throws Exception if the table check failed for a reason other than a "table does not exist" error.
     */
    public boolean checkTableExists(Connection conn, String table) throws Exception {
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            ps = conn.prepareStatement("SELECT COUNT(*) FROM " + table);
            rs = ps.executeQuery();
            return true;
        } catch (SQLException e) {
            if (isTableNotFoundException(e)) {
                throw new IllegalStateException(e);
            }

            throw e;
        } finally {
            DatabaseType db = DatabaseTypeFactory.getDatabaseType(conn);
            db.closeJDBCObjects(null, ps, rs);
        }
    }

    /**
     * Updates a column by altering its attributes. The modify_cmd is used as part of the ALTER SQL command; e.g.:
     *
     * <pre>
     * ALTER TABLE table MODIFY ( column modify_cmd )
     * </pre>
     *
     * @param  conn
     * @param  table
     * @param  column
     * @param  modify_cmd
     *
     * @throws SQLException
     */
    public void updateColumn(Connection conn, String table, String column, String modify_cmd) throws SQLException {
        String sql = "ALTER TABLE " + table + " MODIFY (" + column + " " + modify_cmd + ")";
        executeSql(conn, sql);
    }

    /**
     * Drops a table.
     *
     * @param  conn
     * @param  table
     *
     * @throws SQLException
     */
    public void dropTable(Connection conn, String table) throws SQLException {
        String sql = "DROP TABLE " + table;
        executeSql(conn, sql);
    }

    /**
     * Deletes a column.
     *
     * @param  conn
     * @param  table
     * @param  column
     *
     * @throws SQLException
     */
    public void deleteColumn(Connection conn, String table, String column) throws SQLException {
        String sql = "ALTER TABLE " + table + " DROP COLUMN " + column;
        executeSql(conn, sql);
    }

    /**
     * Executes the given SQL on the given connection. Use this method if you want to return the raw results
     * for further processing.  This is especially useful when used in conjunction with {@link SST_JavaTask}.
     *
     * @param  conn the connection to the database that will execute the SQL
     * @param  sql  the actual SQL to execute
     *
     * @throws SQLException
     */
    public List<Object[]> executeSelectSql(Connection conn, String sql) throws SQLException {
        Statement ps = null;

        try {
            LOG.debug(DbUtilsI18NResourceKeys.EXECUTING_SQL, sql);
            ps = conn.createStatement();

            ResultSet resultSet = ps.executeQuery(sql);
            ResultSetMetaData metadata = resultSet.getMetaData();

            int numberColumns = metadata.getColumnCount();
            List<Object[]> results = new ArrayList<Object[]>();
            while (resultSet.next()) {
                Object[] nextRow = new Object[numberColumns];
                for (int i = 0; i < numberColumns; i++) {
                    nextRow[i] = resultSet.getObject(i + 1);
                }
                results.add(nextRow);
            }
            return results;
        } finally {
            closeStatement(ps);
        }
    }

    /**
     * Executes the given SQL on the given connection. Use this method if you don't care about what the results are, you
     * just want to execute the SQL and know if it was successful or not.
     *
     * @param  conn the connection to the database that will execute the SQL
     * @param  sql  the actual SQL to execute
     *
     * @throws SQLException
     */
    public void executeSql(Connection conn, String sql) throws SQLException {
        Statement ps = null;

        try {
            LOG.debug(DbUtilsI18NResourceKeys.EXECUTING_SQL, sql);
            ps = conn.createStatement();
            ps.executeUpdate(sql);
        } finally {
            closeStatement(ps);
        }
    }

    /**
     * Executes each SQL string in the list.
     *
     * @param  conn
     * @param  sql_list the list of SQL strings, each item is assumed to be a standalone and complete SQL statement
     *
     * @throws SQLException
     *
     * @see    #executeSql(Connection, String)
     */
    public void executeSql(Connection conn, List<String> sql_list) throws SQLException {
        for (String sql : sql_list) {
            executeSql(conn, sql);
        }
    }

    /**
     * Drops the sequence with the given name.
     *
     * @param  conn
     * @param  sequence_name
     *
     * @throws SQLException
     */
    public void dropSequence(Connection conn, String sequence_name) throws SQLException {
        String sql = "DROP SEQUENCE " + sequence_name;
        executeSql(conn, sql);
    }

    /**
     * Inserts data into a table. The <code>insert_cmd</code> is the SQL that appears after the <code>INSERT INTO
     * table</code>.
     *
     * @param  conn
     * @param  table
     * @param  insert_cmd
     *
     * @throws SQLException
     */
    public void insert(Connection conn, String table, String insert_cmd) throws SQLException {
        String sql = "INSERT INTO " + table + " " + insert_cmd;
        executeSql(conn, sql);
    }

    /**
     * Updates data in a particular column. The <code>jdbc_type_int</code> is the column's JDBC type as defined in
     * {@link java.sql.Types}. The where clause is optional and may be <code>null</code>; if it is
     * non-<code>null</code>, it does not include the actual WHERE keyword.
     *
     * @param  conn
     * @param  table
     * @param  column
     * @param  where
     * @param  value
     * @param  jdbc_type_int
     *
     * @throws SQLException
     */
    public void update(Connection conn, String table, String column, String where, String value, int jdbc_type_int)
        throws SQLException {
        String sql = "UPDATE " + table + " SET " + column + " = ? ";
        if (where != null) {
            sql += "WHERE " + where;
        }

        PreparedStatement ps = null;

        try {
            ps = conn.prepareStatement(sql);

            // TODO: why do we need to do this special boolean/bigint handling?  Isn't this what setObject is supposed to do?
            if (jdbc_type_int == Types.BOOLEAN) {
                setBooleanValue(Boolean.valueOf(value).booleanValue(), ps, 1);
            } else if (jdbc_type_int == Types.BIGINT) {
                ps.setLong(1, Long.parseLong(value));
            } else {
                ps.setObject(1, value, jdbc_type_int);
            }

            ps.executeUpdate();
        } finally {
            closeStatement(ps);
        }
    }

    /**
     * Adds a column to a table. The column type name is a generic type name.
     *
     * @param  conn           the connection to the database
     * @param  table          the table where the column is to be added
     * @param  column         the new column name
     * @param  generic_column the generic column type name
     * @param  precision      if the column type can take a precision, this can be non-<code>null</code>
     *
     * @throws SQLException
     */
    public void addColumn(Connection conn, String table, String column, String generic_column, Object precision)
        throws SQLException {
        String sql = "ALTER TABLE " + table + " ADD " + column + " " + getDBTypeFromGenericType(generic_column);

        if (precision != null) {
            sql += "(" + precision + ")";
        }

        executeSql(conn, sql);
    }

    /**
     * Determines if the given SQL exception was caused by a "table not found" error.
     *
     * @param  e the SQL exception that occurred
     *
     * @return <code>true</code> if the given exception is the database's "table not found" exception.
     */
    public abstract boolean isTableNotFoundException(SQLException e);

    /**
     * A utility that returns the last value of a sequence. This is useful for when you need the value of a primary key
     * for a row that you just inserted.
     *
     * @param  conn  The connection to use to get the sequence value.
     * @param  table The table where the sequence is defined.
     * @param  key   The column on which the sequence is defined.
     *
     * @return the last value of a sequence
     *
     * @throws SQLException
     */
    public abstract int getSequenceValue(Connection conn, String table, String key) throws SQLException;

    /**
     * A utility that returns the next value of a sequence.
     *
     * @param  conn  The connection to use to get the sequence value.
     * @param  table The table where the sequence is defined.
     * @param  key   The column on which the sequence is defined.
     *
     * @return the last value of a sequence
     *
     * @throws SQLException
     */
    public abstract int getNextSequenceValue(Connection conn, String table, String key) throws SQLException;

    /**
     * A utility that returns the string for sequence use in an Insert statement
     *
     * @param  conn  The connection to use to get the sequence value.
     * @param  sequenceName  The sequence name for the table.
     *
     * @return the sequence generating string
     *
     * @throws SQLException
     */
    public String getSequenceInsertValue(Connection conn, String sequenceName) {
        return "nextval('" + sequenceName + "')";
    }

    /**
     * Creates a sequence with the given name. Its initial value is specified along with its increment (both are
     * specified as Strings).
     *
     * @param conn
     * @param name
     * @param initial
     * @param increment
     * @param seqIdCacheSize
     * @throws SQLException
     */
    public void createSequence(Connection conn, String name, String initial, String increment, String seqIdCacheSize)
        throws SQLException {
        CreateSequenceExprBuilder builder = CreateSequenceExprBuilder.getBuilder(this);
        HashMap<String, Object> terms = new HashMap<String, Object>();
        terms.put(CreateSequenceExprBuilder.KEY_SEQ_NAME, name);
        terms.put(CreateSequenceExprBuilder.KEY_SEQ_START, initial);
        terms.put(CreateSequenceExprBuilder.KEY_SEQ_INCREMENT, increment);
        terms.put(CreateSequenceExprBuilder.KEY_SEQ_CACHE_SIZE,
            CreateSequenceExprBuilder.getSafeSequenceCacheSize(builder, seqIdCacheSize));
        executeSql(conn, builder.build(terms));
    }

    /**
     * Alters an existing column. You can optionally alter the column's type, the default value, precision, and
     * nullability. You can also optionally reindex the table.
     *
     * @param  conn                connection to the database
     * @param  table               the name of the table where the column exists
     * @param  column              the name of the column to alter
     * @param  generic_column_type the new generic type of the column
     * @param  default_value       the new default value
     * @param  precision           the new precision of the column
     * @param  nullable            the new nullable value (if <code>true</code>, it's value can now be NULL)
     * @param  reindex             if <code>true</code>, and the DB supports it, the table will be reindexed
     *
     * @throws SQLException
     */
    public abstract void alterColumn(Connection conn, String table, String column, String generic_column_type,
        String default_value, String precision, Boolean nullable, Boolean reindex) throws SQLException;

    /**
     * Reindexes the given table.
     *
     * @param  conn
     * @param  table
     *
     * @throws SQLException
     */
    public abstract void reindexTable(Connection conn, String table) throws SQLException;

    /**
     * Provides the value to be used for the ESCAPE character in string literals.  The SQL standard is a single
     * character, typically '\', but not every dbType conforms to the standard.  To override the db default set
     * the rhq.server.database.escape-character system property.
     * 
     * @return If set, the value of rhq.server.database.escape-character, otherwise the db default. 
     */
    public String getEscapeCharacter() {
        String result = System.getProperty("rhq.server.database.escape-character");

        return (null == result) ? "\\" : result;
    }

    /**
     * Most vendors support foreign keys to itself that in fact perform cascade delete. But some do not and
     * that currently affects our data model. (see rhq_config_property in content-schema.xml).
     *  
     * @return true unless overriden to return false.
     */
    public boolean supportsSelfReferringCascade() {
        return true;
    }
}
