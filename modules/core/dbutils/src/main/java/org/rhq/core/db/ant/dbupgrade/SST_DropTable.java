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
 * Drops a table. This task takes one attribute: "table".
 */
public class SST_DropTable extends SchemaSpecTask {
    private static final Msg MSG = DbAntI18NFactory.getMsg();

    private String table = null;

    /**
     * Sets the table name to be dropped.
     *
     * @param t table name
     */
    public void setTable(String t) {
        table = t;
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

        try {
            Connection new_conn = null;
            boolean table_exists = false;

            try {
                new_conn = getNewConnection();
                table_exists = db_type.checkTableExists(new_conn, table);
            } finally {
                db_type.closeConnection(new_conn);
            }

            if (!table_exists) {
                log(MSG.getMsg(DbAntI18NResourceKeys.DROP_TABLE_TABLE_DOES_NOT_EXIST, table));
                return;
            }

            log(MSG.getMsg(DbAntI18NResourceKeys.DROP_TABLE_EXECUTING, table));
            db_type.dropTable(conn, table);
        } catch (Exception e) {
            throw new BuildException(MSG.getMsg(DbAntI18NResourceKeys.DROP_TABLE_ERROR, table, e), e);
        }
    }

    private void validateAttributes() throws BuildException {
        if (table == null) {
            throw new BuildException(MSG.getMsg(DbAntI18NResourceKeys.SCHEMA_SPEC_TASK_MISSING_ATTRIB, "DropTable",
                "table"));
        }
    }
}