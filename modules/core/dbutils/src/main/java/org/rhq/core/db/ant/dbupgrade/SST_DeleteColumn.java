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
 * Ant task to delete a given column from a given table. This takes two required attributes:
 *
 * <ul>
 *   <li>table</li>
 *   <li>column</li>
 * </ul>
 *
 * <pre>
 * ALTER TABLE table DROP COLUMN column
 * </pre>
 */
public class SST_DeleteColumn extends SchemaSpecTask {
    private static final Msg MSG = DbAntI18NFactory.getMsg();

    private String table = null;
    private String column = null;

    /**
     * The table that owns the column to be deleted.
     *
     * @param t table name
     */
    public void setTable(String t) {
        table = t;
    }

    /**
     * The column to be deleted.
     *
     * @param c column name
     */
    public void setColumn(String c) {
        column = c;
    }

    /**
     * @see org.apache.tools.ant.Task#execute()
     */
    @Override
    public void execute() throws BuildException {
        if (!isDBTargeted()) {
            return;
        }

        validateAttributes();

        try {
            DatabaseType db_type = getDatabaseType();
            Connection conn = getConnection();
            boolean exists = db_type.checkColumnExists(conn, table, column);

            if (!exists) {
                return; // good for us, its already been deleted
            }

            log(MSG.getMsg(DbAntI18NResourceKeys.DELETING_COLUMN, table, column));

            db_type.deleteColumn(conn, table, column);
        } catch (Exception e) {
            throw new BuildException(MSG.getMsg(DbAntI18NResourceKeys.ERROR_DELETING_COLUMN, column, table, e), e);
        }
    }

    private void validateAttributes() throws BuildException {
        if (table == null) {
            throw new BuildException(MSG.getMsg(DbAntI18NResourceKeys.SCHEMA_SPEC_TASK_MISSING_ATTRIB, "DeleteColumn",
                "table"));
        }

        if (column == null) {
            throw new BuildException(MSG.getMsg(DbAntI18NResourceKeys.SCHEMA_SPEC_TASK_MISSING_ATTRIB, "DeleteColumn",
                "column"));
        }
    }
}