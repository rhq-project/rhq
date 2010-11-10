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

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

class SqlDataSet extends DataSet {
    private final ResultSet m_resRows;
    private final Table m_table;

    protected SqlDataSet(Table table) throws SQLException {
        super(table.getName(), table.getDBSetup());

        this.m_table = table;
        String strCmd = table.getQueryCommand();

        // TODO where is the ResultSet of next line cleaned up?
        Statement statement = table.getDBSetup().getConnection().createStatement();
        this.m_resRows = statement.executeQuery(strCmd);
        statement.close();
    }

    @Override
    protected Data getData(int columnIndex) {
        Data dataResult = null;

        try {
            Column col = (Column) this.m_table.getColumns().get(columnIndex);
            dataResult = new Data(col.getName(), this.m_resRows.getString(columnIndex + 1));
        } catch (SQLException e) {
        }

        return dataResult;
    }

    @Override
    protected boolean next() {
        boolean bResult;

        try {
            bResult = this.m_resRows.next();
        } catch (SQLException e) {
            bResult = false;
        }

        return bResult;
    }
}