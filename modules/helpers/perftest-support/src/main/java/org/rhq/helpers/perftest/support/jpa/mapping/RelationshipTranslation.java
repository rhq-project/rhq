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

import java.util.Arrays;

public class RelationshipTranslation {
    private String[] fromColumns;
    private String[] toColumns;
    private String relationTable;
    private String[] relationTableFromColumns;
    private String[] relationTableToColumns;
    
    public void setFromColumns(String[] fromColumns) {
        this.fromColumns = fromColumns;
    }

    public String[] getFromColumns() {
        return fromColumns;
    }

    public void setToColumns(String[] toColumns) {
        this.toColumns = toColumns;
    }

    public String[] getToColumns() {
        return toColumns;
    }

    public void setRelationTable(String relationTable) {
        this.relationTable = relationTable;
    }

    public String getRelationTable() {
        return relationTable;
    }

    public void setRelationTableFromColumns(String[] relationTableFromColumns) {
        this.relationTableFromColumns = relationTableFromColumns;
    }

    public String[] getRelationTableFromColumns() {
        return relationTableFromColumns;
    }

    public void setRelationTableToColumns(String[] relationTableToColumns) {
        this.relationTableToColumns = relationTableToColumns;
    }

    public String[] getRelationTableToColumns() {
        return relationTableToColumns;
    }

    public String toString() {
        String ret = "RelationshipTranslation[from=" + Arrays.asList(getFromColumns()) + ", to="
            + Arrays.asList(getToColumns()) + "";
        if (getRelationTable() != null) {
            ret += ", relationTable='" + getRelationTable() + "', relationTableFrom="
                + Arrays.asList(getRelationTableFromColumns()) + ", relationTableTo="
                + Arrays.asList(getRelationTableToColumns()) + "";
        }
        ret += "]";

        return ret;
    }
}