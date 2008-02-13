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
 * Ant task to add a column to a database table. This task requires three attributes:
 *
 * <ul>
 *   <li>table</li>
 *   <li>column</li>
 *   <li>columnType</li>
 * </ul>
 *
 * This task accepts one optional attribute:
 *
 * <ul>
 *   <li>precision - for use if the new columnType can take a precision specifier</li>
 * </ul>
 */
public class SST_AddColumn extends SchemaSpecTask {
    private static final Msg MSG = DbAntI18NFactory.getMsg();

    private String table;
    private String column;
    private String columnType;
    private String precision;

    /**
     * Sets the name of the table that will get the new column added to it.
     *
     * @param t table name
     */
    public void setTable(String t) {
        table = t;
    }

    /**
     * Sets the new column name.
     *
     * @param c name of the new column
     */
    public void setColumn(String c) {
        column = c;
    }

    /**
     * Sets the generic DB column type of the new column.
     *
     * @param ct new column's generic column type
     */
    public void setColumnType(String ct) {
        columnType = ct;
    }

    /**
     * If the new column holds numeric data or data that can be limited to a particular precision, this defines its
     * precision.
     *
     * @param p new column's precision
     */
    public void setPrecision(String p) {
        precision = p;
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

            log(MSG.getMsg(DbAntI18NResourceKeys.ADD_COLUMN_EXECUTING, table, column, columnType, precision));

            db_type.addColumn(conn, table, column, columnType, precision);
        } catch (Exception e) {
            throw new BuildException(MSG.getMsg(DbAntI18NResourceKeys.ADD_COLUMN_ERROR, e), e);
        }
    }

    private void validateAttributes() throws BuildException {
        if (table == null) {
            throw new BuildException(MSG.getMsg(DbAntI18NResourceKeys.SCHEMA_SPEC_TASK_MISSING_ATTRIB, "AddColumn",
                "table"));
        }

        if (column == null) {
            throw new BuildException(MSG.getMsg(DbAntI18NResourceKeys.SCHEMA_SPEC_TASK_MISSING_ATTRIB, "AddColumn",
                "column"));
        }

        if (columnType == null) {
            throw new BuildException(MSG.getMsg(DbAntI18NResourceKeys.SCHEMA_SPEC_TASK_MISSING_ATTRIB, "AddColumn",
                "columnType"));
        }
    }
}