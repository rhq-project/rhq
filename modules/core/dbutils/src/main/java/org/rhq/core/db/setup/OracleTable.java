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

import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;
import org.rhq.core.db.DatabaseType;

class OracleTable extends Table {
    public OracleTable(Node node, DatabaseType dbtype, DBSetup dbsetup) throws SAXException {
        super(node, dbtype, dbsetup);
    }

    public OracleTable(ResultSet set, DatabaseMetaData meta, DBSetup dbsetup) throws SQLException {
        super(set, meta, dbsetup);
    }

    protected static List<Table> getTables(DBSetup parent, String username) throws SQLException {
        if (username != null) {
            username = username.toUpperCase();
        }

        List<Table> coll = new ArrayList<Table>();

        String[] types = { "TABLE" };
        DatabaseMetaData meta = parent.getConnection().getMetaData();
        ResultSet setTabs = meta.getTables(null, username, "%", types);

        // Find Tables
        while (setTabs.next()) {
            coll.add(new OracleTable(setTabs, meta, parent));
        }

        return coll;
    }

    protected String getTableSpaceSyntax() {
        return " TABLESPACE ";
    }

    // Overwrite getParallelSyntax() in super class
    protected String getParallelSyntax() {
        return " PARALLEL (DEGREE DEFAULT)";
    }

    protected String getLoggingSyntax() {
        return " NOLOGGING";
    }

    protected String getCacheSyntax() {
        return " CACHE";
    }

    protected String getStorageSyntax() {
        return " STORAGE ";
    }
}