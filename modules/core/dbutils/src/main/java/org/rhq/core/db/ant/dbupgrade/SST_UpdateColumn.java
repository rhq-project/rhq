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
 * Ant task to modify attributes of a given column. This takes three required attributes:
 *
 * <ul>
 *   <li>table</li>
 *   <li>column</li>
 *   <li>modifyCmd</li>
 * </ul>
 *
 * The modifyCmd is used as part of the ALTER SQL command:
 *
 * <pre>
 * ALTER TABLE table MODIFY ( column modifyCmd )
 * </pre>
 */
public class SST_UpdateColumn extends SchemaSpecTask {
    private static final Msg MSG = DbAntI18NFactory.getMsg();

    private String table = null;
    private String column = null;
    private String modifyCmd = null;

    /**
     * The table that owns the column to be updated.
     *
     * @param t table name
     */
    public void setTable(String t) {
        table = t;
    }

    /**
     * The column to be updated.
     *
     * @param c column name
     */
    public void setColumn(String c) {
        column = c;
    }

    /**
     * The actual portion of the modify SQL that indicates what needs to be altered.
     *
     * @param m modify command
     */
    public void setModifyCmd(String m) {
        modifyCmd = m;
    }

    /**
     * @see org.apache.tools.ant.Task#execute()
     */
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
                throw new BuildException(MSG.getMsg(DbAntI18NResourceKeys.ERROR_UPDATING_NONEXISTING_COLUMN, column,
                    table));
            }

            log(MSG.getMsg(DbAntI18NResourceKeys.UPDATING_COLUMN, table, column, modifyCmd));

            db_type.updateColumn(conn, table, column, modifyCmd);
        } catch (Exception e) {
            throw new BuildException(MSG.getMsg(DbAntI18NResourceKeys.ERROR_UPDATING_COLUMN, column, table, e), e);
        }
    }

    private void validateAttributes() throws BuildException {
        if (table == null) {
            throw new BuildException(MSG.getMsg(DbAntI18NResourceKeys.SCHEMA_SPEC_TASK_MISSING_ATTRIB, "UpdateColumn",
                "table"));
        }

        if (column == null) {
            throw new BuildException(MSG.getMsg(DbAntI18NResourceKeys.SCHEMA_SPEC_TASK_MISSING_ATTRIB, "UpdateColumn",
                "column"));
        }

        if (modifyCmd == null) {
            throw new BuildException(MSG.getMsg(DbAntI18NResourceKeys.SCHEMA_SPEC_TASK_MISSING_ATTRIB, "UpdateColumn",
                "modifyCmd"));
        }
    }
}