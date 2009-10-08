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

import org.w3c.dom.Node;

/**
 * Represents a single column value within a single row.
 */
class Data {
    private String m_strRefColName; // Referenced Column Name
    private String m_strValue;

    protected Data(Node nodeMap) {
        this.m_strRefColName = nodeMap.getNodeName();
        this.m_strValue = nodeMap.getNodeValue();
    }

    protected Data(String columnName, String value) {
        this.m_strRefColName = columnName;
        this.m_strValue = value;
    }

    /**
     * If this {@link #isKeyColumn() is a key column}, this column name will be prefixed with "__". Call
     * {@link #getActualColumnName()} if you want the actual column name, with the "__" stripped if the column is a key.
     * This method returns the same value as {@link #getActualColumnName()} if the column does not identify a key
     * column.
     *
     * @return column name
     */
    protected String getColumnName() {
        return this.m_strRefColName;
    }

    protected String getValue() {
        return this.m_strValue;
    }

    protected static boolean isData(Node node) {
        return node.getNodeName().equalsIgnoreCase("data");
    }

    /**
     * Returns the true column name - if this is a {@link #isKeyColumn() key column}, the actual column name is returned
     * (the "__" prefix will be stripped).
     *
     * @return column name that can be used in SQL
     */
    protected String getActualColumnName() {
        if (isKeyColumn()) {
            return getColumnName().substring(2); // strip the "__"
        }

        return getColumnName();
    }

    /**
     * &lt;data> nodes usually cause a row to be INSERTed. However, it is possible to have those nodes do an UPDATE
     * instead. You do this by providing the row's key column(s) in the &data> node itself - just prefix the column name
     * attribute with two underscores - the attribute value is the key value. Example:
     *
     * <pre>
     *    &lt;data ID="1" NAME="bob" />
     *    ...
     *    &lt;data __ID="1" COUNTRY="US" />
     * </pre>
     *
     * The first node causes an insert into the database where the key is "1" and name is "bob" but the country is left
     * unspecified (meaning the country column must be NULLable). The second node specifies that this node updates an
     * existing row where they key is ID="1". You can specify composite keys by specifying multiple __ prefixed columns:
     *
     * <pre>
     *    &lt;data __ID="11" __IDPART2="22" FOO="bar" />
     * </pre>
     *
     * @return true if this data column represents a key column
     */
    protected boolean isKeyColumn() {
        return getColumnName().startsWith("__");
    }
}