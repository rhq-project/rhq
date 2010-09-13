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

package org.rhq.helpers.perftest.support.jpa;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Represents a number of columns along with their values.
 * 
 * @author Lukas Krejci
 */
public class ColumnValues implements Iterable<ColumnValues.Column>, Cloneable {
    
    /**
     * Presents a single column-value pair.
     *
     * @author Lukas Krejci
     */
    public static class Column {
        private String name;
        private Object value;
        
        private Column(String name, Object value) {
            this.name = name == null ? null : name.toUpperCase();
            this.value = value;
        }
        
        /**
         * @return the name
         */
        public String getName() {
            return name;
        }
        
        /**
         * @param name the name to set
         */
        public void setName(String name) {
            this.name = name == null ? null : name.toUpperCase();
        }
        
        /**
         * @return the value
         */
        public Object getValue() {
            return value;
        }

        /**
         * @param value the value to set
         */
        public void setValue(Object value) {
            this.value = value;
        }
        
        public int hashCode() {
            if (name != null) {
                return name.hashCode();
            } else if (value != null) {
                return value.hashCode();
            } else {
                return 1;
            }
        }
        
        public boolean equals(Object other) {
            if (!(other instanceof Column)) {
                return false;                    
            }
            
            Column o = (Column) other;
            
            boolean nameEquals = name == null ? o.name == null : name.equalsIgnoreCase(o.name);
            boolean valueEquals = value == null ? o.value == null : value.equals(o.value);
            
            return nameEquals && valueEquals;
        }
        
        @Override
        public String toString() {
            return  "Column[name='" + name + "', value='" + value + "']";
        }
    }
    
    List<Column> columns = new ArrayList<Column>();
    
    public ColumnValues() {
        
    }
    
    public ColumnValues(Object pk) {
        columns.add(new Column(null, pk));
    }
    
    public ColumnValues(Object... pks) {
        for (Object pk : pks) {
            columns.add(new Column(null, pk));
        }
    }
    
    public ColumnValues(Column... columns) {
        this.columns.addAll(Arrays.asList(columns));
    }
    
    public ColumnValues(Map<String, Object> pks) {
        for (Map.Entry<String, Object> entry : pks.entrySet()) {
            columns.add(new Column(entry.getKey(), entry.getValue()));
        }
    }
    
    public static Set<ColumnValues> setOf(Object... values) {
        Set<ColumnValues> ret = new HashSet<ColumnValues>();
        for (Object val : values) {
            ret.add(new ColumnValues(val));
        }
        
        return ret;
    }
    
    public List<Column> getColumns() {
        return columns;
    }
    
    public void add(Object value) {
        add(null, value);
    }
    
    public void add(String colName, Object value) {
        columns.add(new Column(colName, value));
    }
    
    public Column getColumnByName(String columnName) {
        int idx = -1;
        
        int i = 0;
        for (Column c : columns) {
            if (columnName.equalsIgnoreCase(c.getName())) {
                idx = i;
                break;
            }
            ++i;
        }
        
        if (idx < 0) {
            return null;
        } else {
            return columns.get(idx);
        }
    }
    
    public Iterator<Column> iterator() {
        return columns.iterator();
    }
    
    public int hashCode() {
        return columns.hashCode();
    }
    
    public boolean equals(Object other) {
        if (!(other instanceof ColumnValues)) {
            return false;
        }
        
        ColumnValues o = (ColumnValues) other;
        
        return columns.equals(o.columns);
    }
    
    @Override
    public String toString() {
        return "ColumnValues" + columns.toString();
    }
    
    public ColumnValues clone() {
        ColumnValues ret = new ColumnValues();
        for (Column c : getColumns()) {
            ret.add(c.getName(), c.getValue());
        }
        
        return ret;
    }
}
