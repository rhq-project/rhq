/*
 * RHQ Management Platform
 * Copyright (C) 2005-2014 Red Hat, Inc.
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
 * along with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA
 */

package org.rhq.core.db.ant.dbupgrade;

import static org.apache.tools.ant.Project.MSG_INFO;
import static org.apache.tools.ant.Project.MSG_WARN;
import static org.rhq.core.db.ant.DbAntI18NResourceKeys.ALTER_COLUMN_ERROR;
import static org.rhq.core.db.ant.DbAntI18NResourceKeys.SCHEMA_SPEC_TASK_MISSING_ATTRIB;
import static org.rhq.core.util.StringUtil.isBlank;
import static org.rhq.core.util.jdbc.JDBCUtil.safeClose;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import mazz.i18n.Msg;

import org.apache.tools.ant.BuildException;

import org.rhq.core.db.DatabaseType;
import org.rhq.core.db.OracleDatabaseType;
import org.rhq.core.db.PostgresqlDatabaseType;
import org.rhq.core.db.ant.DbAntI18NFactory;
import org.rhq.core.util.exception.ThrowableUtil;

/**
 * Drop all foreign keys of a column
 *
 * @author Thomas Segismont
 */
public class SST_DropForeignKeys extends SchemaSpecTask {
    private static final Msg MSG = DbAntI18NFactory.getMsg();

    private static final String FIND_FOREIGN_KEYS_ORACLE = "" //
        + "SELECT " //
        + "  a.constraint_name " //
        + "FROM all_cons_columns a " //
        + "  JOIN all_constraints c ON a.owner = c.owner " //
        + "                            AND a.constraint_name = c.constraint_name " //
        + "  JOIN all_constraints c_pk ON c.r_owner = c_pk.owner " //
        + "                               AND c.r_constraint_name = c_pk.constraint_name " //
        + "WHERE c.constraint_type = 'R' " //
        + "      AND a.table_name = ? " //
        + "      AND a.column_name = ?";
    private static final String FIND_FOREIGN_KEYS_POSTGRES = "" //
        + "SELECT " //
        + "  tc.constraint_name " //
        + "FROM " //
        + "  information_schema.table_constraints tc " //
        + "  JOIN information_schema.key_column_usage kcu " //
        + "    ON tc.constraint_name = kcu.constraint_name " //
        + "WHERE tc.constraint_type = 'FOREIGN KEY' AND tc.table_name = ? " //
        + "      AND kcu.column_name = ?";

    private String table;
    private String column;

    @Override
    public void execute() throws BuildException {
        if (!isDBTargeted()) {
            return;
        }

        validateAttributes();

        Set<String> foreignKeys;

        DatabaseType databaseType = getDatabaseType();
        if (databaseType instanceof OracleDatabaseType) {

            foreignKeys = findForeignKeys( //
                FIND_FOREIGN_KEYS_ORACLE, //
                table.toUpperCase(Locale.US), //
                column.toUpperCase(Locale.US) //
            );

        } else if (databaseType instanceof PostgresqlDatabaseType) {

            foreignKeys = findForeignKeys( //
                FIND_FOREIGN_KEYS_POSTGRES, //
                table.toLowerCase(Locale.US), //
                column.toLowerCase(Locale.US) //
            );

        } else {
            String msg = MSG.getMsg(ALTER_COLUMN_ERROR, databaseType);
            if (!isIgnoreError()) {
                log(msg, MSG_WARN);
                return;
            } else {
                throw new BuildException(msg);
            }
        }

        for (String foreignKey : foreignKeys) {
            dropForeignKey(foreignKey);
        }
    }

    private Set<String> findForeignKeys(String query, String tableParam, String columnParam) {
        Set<String> foreignKeys = new HashSet<String>();

        PreparedStatement statement = null;
        ResultSet resultSet = null;
        try {

            statement = getConnection().prepareStatement(query);
            statement.setString(1, tableParam);
            statement.setString(2, columnParam);

            resultSet = statement.executeQuery();

            while (resultSet.next()) {
                foreignKeys.add(resultSet.getString(1));
            }

        } catch (SQLException e) {
            if (!isIgnoreError()) {
                log(ThrowableUtil.getAllSqlExceptionMessages(e), MSG_WARN);
            } else {
                throw new BuildException(e);
            }
        } finally {
            safeClose(statement, resultSet);
        }

        return foreignKeys;
    }

    private void dropForeignKey(String foreignKey) {
        Statement statement = null;
        try {
            String sql = "ALTER TABLE " + table + " DROP CONSTRAINT " + foreignKey;
            log(sql, MSG_INFO);
            statement = getConnection().createStatement();
            statement.executeUpdate(sql);
        } catch (SQLException e) {
            if (!isIgnoreError()) {
                log(ThrowableUtil.getAllSqlExceptionMessages(e), MSG_WARN);
            } else {
                throw new BuildException(e);
            }
        } finally {
            safeClose(statement);
        }
    }

    private void validateAttributes() throws BuildException {
        if (isBlank(table)) {
            throw new BuildException(MSG.getMsg(SCHEMA_SPEC_TASK_MISSING_ATTRIB, "DropForeignKeys", "table"));
        }
        if (isBlank(column)) {
            throw new BuildException(MSG.getMsg(SCHEMA_SPEC_TASK_MISSING_ATTRIB, "DropForeignKeys", "column"));
        }
    }

    public void setTable(String table) {
        this.table = table;
    }

    public void setColumn(String column) {
        this.column = column;
    }
}
