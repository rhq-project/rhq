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
import java.util.ArrayList;
import java.util.List;
import mazz.i18n.Msg;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.rhq.core.db.DatabaseType;
import org.rhq.core.db.ant.DbAntI18NFactory;
import org.rhq.core.db.ant.DbAntI18NResourceKeys;

/**
 * Task that allows direct SQL to be executed. The SQL can be targeted to all databased or a specific one. Usage of this
 * task is easiest to describe via an example:
 *
 * <pre>
 *    &lt;statement desc="Direct SQL only to execute if the database is Postgres (any version)"
 *                  targetDBVendor="postgresql">
 *       INSERT INTO foo VALUES(true)
 *    &lt;/statement>
 *    &lt;statement desc="Direct SQL only to execute if the database is Oracle10"
 *                  targetDBVendor="oracle"
 *                  targetDBVersion="10">
 *       INSERT INTO foo VALUES(1)
 *    &lt;/statement>
 * </pre>
 *
 * If you specify a target DB version, you must specify a target DB vendor.
 */
public class SST_DirectSQL extends SchemaSpecTask {
    private static final Msg MSG = DbAntI18NFactory.getMsg();

    private List<Statement> statements;

    /**
     * Executes each statement defined in this direct-sql task.
     *
     * @see org.apache.tools.ant.Task#execute()
     */
    public void execute() throws BuildException {
        if (!isDBTargeted()) {
            return;
        }

        validateAttributes();

        Connection conn = getConnection();
        DatabaseType db_type = getDatabaseType();

        if (statements != null) {
            for (Statement statement : statements) {
                statement.init(conn, db_type);
                statement.execute();
            }
        }

        return;
    }

    /**
     * Creates a new statement child task.
     *
     * @return new statement task
     */
    public Statement createStatement() {
        Statement statement = new Statement();

        if (statements == null) {
            statements = new ArrayList<Statement>();
        }

        statements.add(statement);

        return statement;
    }

    private void validateAttributes() throws BuildException {
        if (statements == null) {
            throw new BuildException(MSG.getMsg(DbAntI18NResourceKeys.SCHEMA_SPEC_TASK_MISSING_CHILD_ELEMENT,
                "DirectSQL", "statement"));
        }
    }

    /**
     * An individual statement task that is executed as part of the direct-sql schema spec task. One or more of these
     * can be specified in the direct-sql task.
     */
    public class Statement extends Task {
        private Connection databaseConnection = null;
        private DatabaseType databaseType = null;
        private String sqlStatment = null;
        private String sqlDescription = null;
        private String targetDBVendor = null;
        private String targetDBVersion = null;

        /**
         * Initializes this statement task with a database connection and database type.
         *
         * @param conn
         * @param db_type
         */
        public void init(Connection conn, DatabaseType db_type) {
            this.databaseConnection = conn;
            this.databaseType = db_type;
        }

        /**
         * A simple description that is logged when executing this statement.
         *
         * @param s describes what the SQL is attempting to do
         */
        public void setDesc(String s) {
            sqlDescription = s;
        }

        /**
         * Sets the vendor name of the target DB where this SQL should run. Any other DB should not have this SQL
         * executed on it.
         *
         * @param vendor database vendor
         */
        public void setTargetDBVendor(String vendor) {
            targetDBVendor = vendor;
        }

        /**
         * Sets the version string of the target DB where this SQL should run. Any other DB version should not have this
         * SQL executed on it.
         *
         * @param version database version
         */
        public void setTargetDBVersion(String version) {
            targetDBVersion = version;
        }

        /**
         * Defines the actual SQL to be executed
         *
         * @param sql SQL string to execute
         */
        public void addText(String sql) {
            sqlStatment = sql;
        }

        /**
         * @see org.apache.tools.ant.Task#execute()
         */
        public void execute() throws BuildException {
            if (sqlStatment == null) {
                return;
            }

            if ((targetDBVersion != null) && (targetDBVendor == null)) {
                throw new BuildException(MSG.getMsg(
                    DbAntI18NResourceKeys.SCHEMA_SPEC_TASK_TARGET_VERSION_WITHOUT_VENDOR, getTaskName(),
                    targetDBVersion));
            }

            if (targetDBVendor != null) {
                if (!targetDBVendor.equalsIgnoreCase(databaseType.getVendor())) {
                    log(MSG.getMsg(DbAntI18NResourceKeys.SCHEMA_SPEC_TASK_VENDOR_MISMATCH, targetDBVendor, databaseType
                        .getVendor()));
                    return;
                }

                if ((targetDBVersion != null) && !targetDBVersion.equalsIgnoreCase(databaseType.getVersion())) {
                    log(MSG.getMsg(DbAntI18NResourceKeys.SCHEMA_SPEC_TASK_VERSION_MISMATCH, targetDBVendor,
                        targetDBVersion, databaseType.getVersion()));
                    return;
                }
            }

            try {
                log(MSG.getMsg(DbAntI18NResourceKeys.DIRECTSQL_EXECUTING, sqlDescription, sqlStatment));
                databaseType.executeSql(databaseConnection, sqlStatment);
            } catch (Exception e) {
                throw new BuildException(MSG.getMsg(DbAntI18NResourceKeys.SCHEMA_SPEC_TASK_FAILURE, "DirectSQL", e), e);
            }

            return;
        }
    }
}