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

package org.rhq.helpers.perftest.support.dbunit;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.dbunit.dataset.DataSetException;
import org.dbunit.dataset.ITable;
import org.dbunit.dataset.ITableIterator;
import org.dbunit.dataset.ITableMetaData;
import org.dbunit.dataset.RowOutOfBoundsException;
import org.rhq.helpers.perftest.support.jpa.ColumnValues;
import org.rhq.helpers.perftest.support.jpa.mapping.ColumnValuesTableMap;

/**
 * This is a table iterator able to filter out rows with disallowed primary key values.
 * 
 * @author Lukas Krejci
 */
public class EntityRelationshipTableIterator implements ITableIterator {

    private ITableIterator wrappedIterator;
    private ColumnValuesTableMap allowedPks;

    public EntityRelationshipTableIterator(ITableIterator wrappedIterator,
        ColumnValuesTableMap allowedPks) {

        this.wrappedIterator = wrappedIterator;
        this.allowedPks = allowedPks;
    }

    public boolean next() throws DataSetException {
        while (wrappedIterator.next()) {
            if (allowedPks.containsKey(wrappedIterator.getTableMetaData().getTableName().toUpperCase())) {
                return true;
            }
        }
        return false;
    }

    public ITableMetaData getTableMetaData() throws DataSetException {
        return wrappedIterator.getTableMetaData();
    }

    public ITable getTable() throws DataSetException {
        String tableName = getTableMetaData().getTableName().toUpperCase();
        
        Set<ColumnValues> pks = allowedPks.get(tableName);
        
        ITable wrappedTable = wrappedIterator.getTable();
        
        if (pks == null) {
            //no filtering
            return wrappedTable;
        } else if (pks.isEmpty()) {
            return new EmptyTable(wrappedTable.getTableMetaData());
        } else {
            return new FilteredTable(wrappedTable, pks);
        }
    }

    private static class EmptyTable implements ITable {

        private ITableMetaData metadata;
        
        public EmptyTable(ITableMetaData metadata) {
            this.metadata = metadata;
        }
        
        public ITableMetaData getTableMetaData() {
            return metadata;
        }

        public int getRowCount() {
            return 0;
        }

        public Object getValue(int row, String column) throws DataSetException {
            throw new RowOutOfBoundsException("This table is always empty.");
        }
        
    }
    
    private class FilteredTable implements ITable {

        private ITable wrappedTable;
        private List<Integer> allowedRowNumbers;
        
        public FilteredTable(ITable wrappedTable, Set<ColumnValues> allowedPks) throws DataSetException {
            this.wrappedTable = wrappedTable;
            this.allowedRowNumbers = getRowNumbers(wrappedTable, allowedPks);
        }

        public ITableMetaData getTableMetaData() {
            return wrappedTable.getTableMetaData();
        }

        public int getRowCount() {
            return allowedRowNumbers.size();
        }

        public Object getValue(int row, String column) throws DataSetException {
            if (row >= allowedRowNumbers.size()) {
                throw new RowOutOfBoundsException(); 
            }
            return wrappedTable.getValue(allowedRowNumbers.get(row), column);
        }
        
        private List<Integer> getRowNumbers(ITable table, Set<ColumnValues> pks) throws DataSetException {
            List<Integer> ret = new ArrayList<Integer>();
            
            if (pks.isEmpty()) {
                return ret;
            }

            //now this is extremely inefficient, but retains the order of the pks
            //as defined in the pks set (which is a linked hash set)
            for (ColumnValues pk : pks) {
                for (int i = 0; i < table.getRowCount(); ++i) {
                    boolean add = true;
                    for (ColumnValues.Column c : pk) {
                        Object val = table.getValue(i, c.getName());
                        if (c.getValue() == null ? val != null : !c.getValue().equals(val)) {
                            add = false;
                            break;
                        }
                    }
                    
                    if (add) {
                        ret.add(i);
                    }
                }
            }
            
            return ret;
        }
    }
}
