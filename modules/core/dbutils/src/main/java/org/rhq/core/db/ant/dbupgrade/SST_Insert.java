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
 * Ant task to insert data into a table. This task takes three attributes:
 *
 * <ul>
 *   <li>table</li>
 *   <li>insertCmd</li>
 *   <li>dupFail <i>(optional)</i></li>
 * </ul>
 *
 * <p>The optional <code>dupFail</code> attribute is a boolean - if TRUE, the ANT task with throw a build exception if
 * the inserted rows already exist in the table. If FALSE, a warning will be logged but the task will not fail the
 * build. The default is TRUE.</p>
 *
 * <p>The <code>insertCmd</code> is SQL that will appear after "INSERT INTO table ".</p>
 */
public class SST_Insert extends SchemaSpecTask {
    private static final Msg MSG = DbAntI18NFactory.getMsg();

    private String table = null;
    private String insertCmd = null;
    private boolean dupFail = true;

    /**
     * The name of the table where the data is to be inserted.
     *
     * @param t table name
     */
    public void setTable(String t) {
        table = t;
    }

    /**
     * The insert SQL to indicate what is to be inserted. This is the SQL text to appear after <code>INSERT INTO
     * table</code>.
     *
     * @param i insert SQL
     */
    public void setInsertCmd(String i) {
        insertCmd = i;
    }

    /**
     * Flag to indicate if a duplicate row getting inserted into the DB causes the build to fail or not.
     *
     * @param df TRUE or FALSE boolean string
     */
    public void setDupFail(String df) {
        dupFail = Boolean.getBoolean(df);
    }

    /**
     * @see org.apache.tools.ant.Task#execute()
     */
    public void execute() throws BuildException {
        if (!isDBTargeted()) {
            return;
        }

        validateAttributes();

        Connection conn = getConnection();
        DatabaseType db_type = getDatabaseType();

        try {
            log(MSG.getMsg(DbAntI18NResourceKeys.INSERT_EXECUTING, table, insertCmd));
            db_type.insert(conn, table, insertCmd);
        } catch (SQLException e) {
            if (dupFail || (e.getMessage().toLowerCase().indexOf("constraint") == -1)) {
                throw new BuildException(MSG.getMsg(DbAntI18NResourceKeys.SCHEMA_SPEC_TASK_FAILURE, "Insert", e), e);
            }

            try {
                log(MSG.getMsg(DbAntI18NResourceKeys.INSERT_IGNORE_DUPLICATE, table));
                conn.rollback();
            } catch (SQLException ex) {
                throw new BuildException(MSG.getMsg(DbAntI18NResourceKeys.INSERT_ROLLBACK_ERROR, ex), ex);
            }
        } catch (Exception e) {
            throw new BuildException(MSG.getMsg(DbAntI18NResourceKeys.SCHEMA_SPEC_TASK_FAILURE, "Insert", e), e);
        }
    }

    private void validateAttributes() throws BuildException {
        if (table == null) {
            throw new BuildException(MSG.getMsg(DbAntI18NResourceKeys.SCHEMA_SPEC_TASK_MISSING_ATTRIB, "Insert",
                "table"));
        }

        if (insertCmd == null) {
            throw new BuildException(MSG.getMsg(DbAntI18NResourceKeys.SCHEMA_SPEC_TASK_MISSING_ATTRIB, "Insert",
                "insertCmd"));
        }
    }
}