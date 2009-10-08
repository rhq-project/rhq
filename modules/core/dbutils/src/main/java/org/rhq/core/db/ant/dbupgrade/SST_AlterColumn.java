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
package org.rhq.core.db.ant.dbupgrade;

import java.sql.Connection;
import java.sql.SQLException;
import mazz.i18n.Msg;
import org.apache.tools.ant.BuildException;
import org.rhq.core.db.DatabaseType;
import org.rhq.core.db.ant.DbAntI18NFactory;
import org.rhq.core.db.ant.DbAntI18NResourceKeys;

/**
 * Ant task that can alter an existing column. This task requires these attributes:
 *
 * <ul>
 *   <li>table</li>
 *   <li>column</li>
 *   <li>columnType + (the new column's generic type)</li>
 *   <li>nullable + (if <code>true</code>, the column can be NULL; otherwise it is NOT NULL)</li>
 *   <li>default + (the new default value of the column)</li>
 * </ul>
 *
 * + where only one of columnType, nullable or default is required. This task has two optional attributes:
 *
 * <ul>
 *   <li>precision</li>
 *   <li>reindex - if <code>true</code>, the table will be re-indexed</li>
 * </ul>
 */
public class SST_AlterColumn extends SchemaSpecTask {
    private static final Msg MSG = DbAntI18NFactory.getMsg();

    private String table = null;
    private String column = null;
    private String columnType = null;
    private String precision = null;
    private String defval = null;
    private Boolean nullable = null;
    private Boolean reindex = null;

    /**
     * Sets the table name whose column is to be altered.
     *
     * @param t table name
     */
    public void setTable(String t) {
        table = t;
    }

    /**
     * Sets the name of the column that is to be altered.
     *
     * @param c column name
     */
    public void setColumn(String c) {
        column = c;
    }

    /**
     * Sets the column's new generic column type.
     *
     * @param ct generic column type
     */
    public void setColumnType(String ct) {
        columnType = ct;
    }

    /**
     * Sets the column's new precision.
     *
     * @param p precision
     */
    public void setPrecision(String p) {
        precision = p;
    }

    /**
     * Sets the new nullable setting for the column - if true it can be null, if false it cannot. The value of this
     * attribute can be either true/false or respectively "NULL"/"NOT NULL".
     *
     * @param  n nullable boolean string
     *
     * @throws IllegalArgumentException if boolean string is neither true nor false
     */
    public void setNullable(String n) {
        if (n.equalsIgnoreCase("true") || n.equalsIgnoreCase("NULL")) {
            nullable = Boolean.TRUE;
        } else if (n.equalsIgnoreCase("false") || n.equalsIgnoreCase("NOT NULL")) {
            nullable = Boolean.FALSE;
        } else {
            throw new IllegalArgumentException("nullable=" + n);
        }
    }

    /**
     * Sets the new default value of the column.
     *
     * @param d default value
     */
    public void setDefault(String d) {
        defval = d;
    }

    /**
     * Sets the reindex flag - this is a boolean string, "true" or "false".
     *
     * @param  r reindex flag
     *
     * @throws IllegalArgumentException if boolean string is neither true nor false
     */
    public void setReindex(String r) {
        if (r.equalsIgnoreCase("true")) {
            reindex = Boolean.TRUE;
        } else if (r.equalsIgnoreCase("false")) {
            reindex = Boolean.FALSE;
        } else {
            throw new IllegalArgumentException("reindex=" + r);
        }
    }

    /**
     * @see org.apache.tools.ant.Task#execute()
     */
    public void execute() throws BuildException {
        validateAttributes();

        if (!isDBTargeted()) {
            return;
        }

        try {
            DatabaseType db_type = getDatabaseType();
            Connection conn = getConnection();

            checkColumnExistence(conn, db_type);

            db_type.alterColumn(conn, table, column, columnType, defval, precision, nullable, reindex);
        } catch (Exception e) {
            throw new BuildException(MSG.getMsg(DbAntI18NResourceKeys.ALTER_COLUMN_ERROR, e), e);
        }
    }

    /**
     * Checks to see if the column exists. If it does not, we can't alter the column so an exception is thrown.
     *
     * @param  conn
     * @param  db_type
     *
     * @throws SQLException   failed to determine if the column exists or not
     * @throws BuildException column does not exist
     */
    private void checkColumnExistence(Connection conn, DatabaseType db_type) throws BuildException, SQLException {
        boolean exists = db_type.checkColumnExists(conn, table, column);

        if (!exists) {
            throw new BuildException(MSG.getMsg(DbAntI18NResourceKeys.ALTER_COLUMN_DOES_NOT_EXIST, table, column));
        }
    }

    private void validateAttributes() throws BuildException {
        if (table == null) {
            throw new BuildException(MSG.getMsg(DbAntI18NResourceKeys.SCHEMA_SPEC_TASK_MISSING_ATTRIB, "AlterColumn",
                "table"));
        }

        if (column == null) {
            throw new BuildException(MSG.getMsg(DbAntI18NResourceKeys.SCHEMA_SPEC_TASK_MISSING_ATTRIB, "AlterColumn",
                "column"));
        }

        if ((columnType == null) && (nullable == null) && (defval == null)) {
            throw new BuildException(MSG.getMsg(DbAntI18NResourceKeys.SCHEMA_SPEC_TASK_MISSING_ATTRIB, "AlterColumn",
                "columnType|default|nullable"));
        }
    }
}