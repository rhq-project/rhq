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
package org.rhq.core.db.setup;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.rhq.core.db.DatabaseTypeFactory;

/**
 * Tracks a ResultSet. Call {@link #next()} to go to the next row. Call {@link #getData(int)} to get a column within the
 * current row.
 */
abstract class DataSet {
    private DBSetup m_parent;
    private String m_strTableName;

    protected DataSet(String tableName, DBSetup dbsetup) {
        this.m_strTableName = tableName;
        this.m_parent = dbsetup;
    }

    protected int create() throws SQLException {
        PreparedStatement stmt = null;
        int rowcnt = 0;

        try {
            executePreCreateCommands();

            // loop through each row
            for (; this.next(); rowcnt++) {
                String strCmd = this.getCreateCommand();
                doSQL(strCmd);
            }

            executePostCreateCommands();
        } catch (SQLException e) {
            try {
                this.m_parent.getConnection().rollback();
            } catch (Exception e2) {
                e2.printStackTrace();
            }

            throw e;
        } finally {
            if (stmt != null) {
                stmt.close();
            }
        }

        return rowcnt; // The number of rows created.
    }

    protected void doSQL(String sql) throws SQLException {
        m_parent.doSQL(sql);
    }

    protected void executePreCreateCommands() throws SQLException {
    }

    protected void executePostCreateCommands() throws SQLException {
    }

    /**
     * Get the statement to create the data - this may be an INSERT or it may be an UPDATE. If key columns where
     * specified in the data, then assume we want to UPDATE the row using they key columns for the WHERE clause.
     * Otherwise, assume its a new row to be INSERTED.
     *
     * @return SQL statement to create the data.
     *
     * @throws SQLException
     */
    protected String getCreateCommand() throws SQLException {
        String cmd;

        if (keyColumnsExist()) {
            cmd = getUpdateCommand();
        } else {
            cmd = getInsertCommand();
        }

        return cmd;
    }

    protected String getInsertCommand() throws SQLException {
        int iCols = this.getNumberColumns();

        StringBuffer strCmd = new StringBuffer("INSERT INTO ");
        strCmd.append(this.getTableName());
        strCmd.append(" (");

        for (int i = 0; i < iCols; i++) {
            Data data = this.getData(i);

            if (i > 0) {
                strCmd.append(',');
            }

            strCmd.append(data.getColumnName());
        }

        strCmd.append(") VALUES (");

        for (int i = 0; i < iCols; i++) {
            if (i > 0) {
                strCmd.append(',');
            }

            String strValue = getData(i).getValue();
            if (strValue != null) {
                // We need to replace the value 'TRUE' with '1' if this is
                // Oracle. It's a hack to have this code in this class until I
                // do some restructuring of the code
                if (DatabaseTypeFactory.isOracle(m_parent.getDatabaseType())
                    || DatabaseTypeFactory.isSQLServer(m_parent.getDatabaseType())) {
                    if (strValue.equalsIgnoreCase("TRUE")) {
                        strValue = "1";
                    } else if (strValue.equalsIgnoreCase("FALSE")) {
                        strValue = "0";
                    }
                }

                strCmd.append('\'');
                strCmd.append(strValue);
                strCmd.append('\'');
            } else {
                strCmd.append("NULL");
            }
        }

        strCmd.append(')');

        return strCmd.toString();
    }

    protected String getUpdateCommand() throws SQLException {
        int iCols = this.getNumberColumns();

        StringBuffer strCmd = new StringBuffer("UPDATE ");
        strCmd.append(this.getTableName());
        strCmd.append(" SET ");

        boolean need_comma = false;
        for (int i = 0; i < iCols; i++) {
            Data data = this.getData(i);
            if (data.isKeyColumn()) {
                continue;
            }

            if (need_comma) {
                strCmd.append(',');
            }

            need_comma = true;

            strCmd.append(data.getColumnName());
            strCmd.append(" = ");

            String strValue = data.getValue();
            if (strValue != null) {
                // We need to replace the value 'TRUE' with '1' if this is
                // Oracle. It's a hack to have this code in this class until I
                // do some restructuring of the code
                if (DatabaseTypeFactory.isOracle(m_parent.getDatabaseType())
                    || DatabaseTypeFactory.isSQLServer(m_parent.getDatabaseType())) {
                    if (strValue.equalsIgnoreCase("TRUE")) {
                        strValue = "1";
                    } else if (strValue.equalsIgnoreCase("FALSE")) {
                        strValue = "0";
                    }
                }

                strCmd.append('\'');
                strCmd.append(strValue);
                strCmd.append('\'');
            } else {
                strCmd.append("NULL");
            }
        }

        strCmd.append(" WHERE ");

        boolean need_AND_keyword = false;
        List<Data> keys = getKeyColumns();
        for (Data data : keys) {
            if (need_AND_keyword) {
                strCmd.append(" AND ");
            }

            need_AND_keyword = true;
            strCmd.append(data.getActualColumnName() + " = ");

            String key_value = data.getValue();
            if (key_value != null) {
                // We need to replace the value 'TRUE' with '1' if this is
                // Oracle. It's a hack to have this code in this class until I
                // do some restructuring of the code
                if (DatabaseTypeFactory.isOracle(m_parent.getDatabaseType())
                    || DatabaseTypeFactory.isSQLServer(m_parent.getDatabaseType())) {
                    if (key_value.equalsIgnoreCase("TRUE")) {
                        key_value = "1";
                    } else if (key_value.equalsIgnoreCase("FALSE")) {
                        key_value = "0";
                    }
                }

                strCmd.append('\'');
                strCmd.append(key_value);
                strCmd.append('\'');
            } else {
                strCmd.append("NULL");
            }
        }

        return strCmd.toString();
    }

    protected int getNumberColumns() {
        return 0;
    }

    protected String getTableName() {
        return this.m_strTableName;
    }

    protected boolean keyColumnsExist() {
        int num_col = getNumberColumns();
        for (int i = 0; i < num_col; i++) {
            Data data = getData(i);
            if (data == null) {
                System.err.println("Data at column index for table " + m_strTableName + " is null");
            }
            else if (data.isKeyColumn()) {
                return true;
            }
        }

        return false;
    }

    protected List<Data> getKeyColumns() {
        List<Data> keys = new ArrayList<Data>();
        int num_col = getNumberColumns();
        for (int i = 0; i < num_col; i++) {
            Data data = getData(i);
            if (data == null) {
                System.err.println("Data at column index for table " + m_strTableName + " is null");
            }
            else if (data.isKeyColumn()) {
                keys.add(data);
            }
        }

        return keys;
    }

    protected abstract Data getData(int columnIndex);

    protected abstract boolean next();
}