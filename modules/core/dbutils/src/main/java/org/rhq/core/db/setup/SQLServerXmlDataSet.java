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

import java.sql.SQLException;

import org.w3c.dom.Node;

/**
 * @author Joseph Marques
 */
public class SQLServerXmlDataSet extends XmlDataSet {

    protected SQLServerXmlDataSet(Table table, Node nodeTable) {
        super(table, nodeTable);
    }

    protected void executePreCreateCommands() throws SQLException {
        // allow our data insert to set data into identity columns
        doSQL("SET IDENTITY_INSERT " + getTableName() + " ON");

    }

    protected void executePostCreateCommands() throws SQLException {
        // turn the identity property back on after we're done inserting data
        doSQL("SET IDENTITY_INSERT " + getTableName() + " OFF");
    }

    protected void doSQL(String sql) throws SQLException {
        try {
            super.doSQL(sql);
        } catch (SQLException sqle) {
            // ignore error if this table doesn't have an identity column
            if (!identityDoesNotExist(sqle)) {
                throw sqle;
            }
        }
    }

    private boolean identityDoesNotExist(SQLException sqle) {
        String errorMessage = sqle.getMessage().toLowerCase();
        return errorMessage.indexOf("does not have the identity property") != -1;
    }
}
