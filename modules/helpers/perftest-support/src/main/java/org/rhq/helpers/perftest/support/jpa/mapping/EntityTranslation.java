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

public class EntityTranslation {
    private String tableName;
    private String[] pkColumns;

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    public String getTableName() {
        return tableName;
    }

    public void setPkColumns(String[] pkColumns) {
        this.pkColumns = pkColumns;
    }

    public String[] getPkColumns() {
        return pkColumns;
    }

    public String toString() {
        return "EntityTranslation[table='" + getTableName() + "', pk=" + Arrays.asList(getPkColumns()) + "]";
    }
}