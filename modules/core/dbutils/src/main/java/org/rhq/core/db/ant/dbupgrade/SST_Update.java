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
import mazz.i18n.Msg;
import org.apache.tools.ant.BuildException;
import org.rhq.core.db.DatabaseType;
import org.rhq.core.db.ant.DbAntI18NFactory;
import org.rhq.core.db.ant.DbAntI18NResourceKeys;

/**
 * Ant task that updates data in a table. This takes four required attributes:
 *
 * <ul>
 *   <li>table - the name of the table whose column is to be updated</li>
 *   <li>column - the column whose data is to be updated</li>
 *   <li>value - the new value</li>
 *   <li>columnType - defines the generic type of the column that is being updated</li>
 * </ul>
 *
 * There is one optional attribute:
 *
 * <ul>
 *   <li>where - a where clause (excluding the WHERE keyword) to limit the scope of the update</li>
 * </ul>
 */
public class SST_Update extends SchemaSpecTask {
    private static final Msg MSG = DbAntI18NFactory.getMsg();

    private String table;
    private String column;
    private String columnType;
    private String value;
    private String where;

    /**
     * Sets the name of the table whose column is being updated.
     *
     * @param t table name
     */
    public void setTable(String t) {
        table = t;
    }

    /**
     * Sets the name of the column whose data is being updated.
     *
     * @param c column name
     */
    public void setColumn(String c) {
        column = c;
    }

    /**
     * Sets the generic type of the column that is being updated.
     *
     * @param ct generic data type name of the column
     */
    public void setColumnType(String ct) {
        columnType = ct;
    }

    /**
     * Sets the new value that will be stored in the column.
     *
     * @param v the new value, as a String
     */
    public void setValue(String v) {
        value = v;
    }

    /**
     * Sets a where clause (excluding the WHERE keyword) that limits the rows that will be updated.
     *
     * @param w where clause
     */
    public void setWhere(String w) {
        where = w;
    }

    /**
     * @see org.apache.tools.ant.Task#execute()
     */
    public void execute() throws BuildException {
        if (!isDBTargeted()) {
            return;
        }

        validateAttributes();

        DatabaseType db_type = getDatabaseType();
        Connection conn = getConnection();
        int jdbc_type_int = translateSqlType(columnType);

        try {
            // Check to see if the column exists.
            boolean foundColumn = db_type.checkColumnExists(conn, table, column);
            if (!foundColumn) {
                throw new BuildException(MSG.getMsg(DbAntI18NResourceKeys.UPDATE_COLUMN_DOES_NOT_EXIST, column, table));
            }

            log(MSG.getMsg(DbAntI18NResourceKeys.UPDATE_EXECUTING, columnType, jdbc_type_int, table, column, value,
                where));
            db_type.update(conn, table, column, where, value, jdbc_type_int);
        } catch (Exception e) {
            throw new BuildException(MSG.getMsg(DbAntI18NResourceKeys.UPDATE_ERROR, table, column, e), e);
        }
    }

    private void validateAttributes() throws BuildException {
        if (table == null) {
            throw new BuildException(MSG.getMsg(DbAntI18NResourceKeys.SCHEMA_SPEC_TASK_MISSING_ATTRIB, "Update",
                "table"));
        }

        if (column == null) {
            throw new BuildException(MSG.getMsg(DbAntI18NResourceKeys.SCHEMA_SPEC_TASK_MISSING_ATTRIB, "Update",
                "column"));
        }

        if (columnType == null) {
            throw new BuildException(MSG.getMsg(DbAntI18NResourceKeys.SCHEMA_SPEC_TASK_MISSING_ATTRIB, "Update",
                "columnType"));
        }

        if (value == null) {
            throw new BuildException(MSG.getMsg(DbAntI18NResourceKeys.SCHEMA_SPEC_TASK_MISSING_ATTRIB, "Update",
                "value"));
        }
    }
}