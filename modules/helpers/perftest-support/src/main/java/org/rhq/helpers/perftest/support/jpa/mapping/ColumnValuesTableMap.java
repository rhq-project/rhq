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

package org.rhq.helpers.perftest.support.jpa.mapping;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Set;

import org.rhq.helpers.perftest.support.jpa.ColumnValues;

/**
 * Represents a map where keys are names of database tables and values are sets of column values ({@link ColumnValues} instances).
 * 
 * Adds {@link #getOrCreate(Object)} method to safely retrieve initialized values even for previously non-existent keys.
 * 
 * @author Lukas Krejci
 */
public class ColumnValuesTableMap extends HashMap<String, Set<ColumnValues>> {

    private static final long serialVersionUID = 1L;

    /**
     * Retrieves a set of column values for given table name.
     * If there was no entry for the table name in this map, a new
     * empty set is created, added to the map and returned.
     * 
     * @param key the name of the table
     * @return a set of column values
     */
    public Set<ColumnValues> getOrCreate(Object key) {
        Set<ColumnValues> ret = super.get(key);
        if (ret == null) {
            ret = new LinkedHashSet<ColumnValues>();
            put((String) key, ret);
        }

        return ret;
    }
}
