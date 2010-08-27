/*
 * RHQ Management Platform
 * Copyright (C) 2005-2010 Red Hat, Inc.
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

package org.rhq.helpers.perftest.support.util;

import java.sql.ResultSet;

import org.dbunit.database.IDatabaseConnection;
import org.dbunit.database.PrimaryKeyFilter;
import org.dbunit.database.PrimaryKeyFilter.PkTableMap;
import org.dbunit.database.search.ExportedKeysSearchCallback;
import org.dbunit.database.search.ForeignKeyRelationshipEdge;
import org.dbunit.dataset.filter.ITableFilter;
import org.dbunit.util.search.IEdge;
import org.dbunit.util.search.SearchException;

/**
 *
 * @author Lukas Krejci
 */
public class ExportedKeysSearchCallbackFilteredByPKs extends ExportedKeysSearchCallback {

    private final PrimaryKeyFilter pksFilter;

    public ExportedKeysSearchCallbackFilteredByPKs(IDatabaseConnection connection, PkTableMap allowedPKs) {
        super(connection);
        this.pksFilter = new PrimaryKeyFilter(connection, allowedPKs, true);
    }

    /**
     * Get the primary key filter associated with the call back
     * @return primary key filter associated with the call back
     */
    public ITableFilter getFilter() {
        return this.pksFilter;
    }

    public void nodeAdded(Object node) throws SearchException {
        this.pksFilter.nodeAdded(node);
    }

    protected IEdge newEdge(ResultSet rs, int type, String from, String to, String fkColumn, String pkColumn)
        throws SearchException {

        ForeignKeyRelationshipEdge edge = createFKEdge(rs, type, from, to, fkColumn, pkColumn);
        this.pksFilter.edgeAdded(edge);
        return edge;
    }
}
