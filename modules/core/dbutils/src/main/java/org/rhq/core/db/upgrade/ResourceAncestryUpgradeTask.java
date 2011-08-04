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
package org.rhq.core.db.upgrade;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

import org.rhq.core.db.DatabaseType;

/**
 * The introduction of pre-computed disambiguated "ancestry" for Resources requires that existing
 * inventory be updated with pre-computed values.  Rhq_resource.ancestry must be set.
 * 
 * This task queries for each platform, assigns its ancestry string, and then traverses the family tree assigning
 * ancestry strings as it goes. InventoryStatus is not important as the resource hieracrhy is always well-formed.
 *
 * @author Jay Shaughnessy
 */
public class ResourceAncestryUpgradeTask implements DatabaseUpgradeTask {

    public static final String ANCESTRY_ENTRY_DELIM = "_:_"; // delimiter separating entry fields
    public static final String ANCESTRY_DELIM = "_::_"; // delimiter seperating ancestry entries

    private DatabaseType databaseType;
    private Connection connection;

    public void execute(DatabaseType databaseType, Connection connection) throws SQLException {
        this.databaseType = databaseType;
        this.connection = connection;

        String sql = "" //
            + "select rt.ID, res.ID, res.NAME from RHQ_RESOURCE res, RHQ_RESOURCE_TYPE rt where " //
            + "       res.RESOURCE_TYPE_ID=rt.ID and rt.CATEGORY='PLATFORM'";
        List<Object[]> rs = databaseType.executeSelectSql(connection, sql);
        for (Object[] result : rs) {
            int rtId;
            int resId;

            if (result[0] instanceof BigDecimal) {
                rtId = ((BigDecimal) result[0]).intValue();
                resId = ((BigDecimal) result[1]).intValue();
            } else {
                rtId = (Integer) result[0];
                resId = (Integer) result[1];
            }
            String resName = (String) result[2];

            handleChildren(rtId, resId, resName, null);
        }
    }

    private void handleChildren(int parentRtId, int parentResId, String parentResName, String parentAncestry)
        throws SQLException {

        String sql = "" //
            + "select rt.ID, res.ID, res.NAME from RHQ_RESOURCE res, RHQ_RESOURCE_TYPE rt where " //
            + "       res.RESOURCE_TYPE_ID=rt.ID and res.PARENT_RESOURCE_ID=" + parentResId;
        List<Object[]> rs = databaseType.executeSelectSql(connection, sql);
        for (Object[] result : rs) {
            int rtId;
            int resId;

            if (result[0] instanceof BigDecimal) {
                rtId = ((BigDecimal) result[0]).intValue();
                resId = ((BigDecimal) result[1]).intValue();
            } else {
                rtId = (Integer) result[0];
                resId = (Integer) result[1];
            }
            String resName = (String) result[2];
            String ancestry = getAncestry(parentRtId, parentResId, parentResName, parentAncestry);
            String updateSql = "update RHQ_RESOURCE set ANCESTRY='" + ancestry + "' where ID=" + resId;
            databaseType.executeSql(connection, updateSql);

            handleChildren(rtId, resId, resName, ancestry);
        }
    }

    private String getAncestry(int rtId, int resId, String resName, String parentAncestry) {

        StringBuilder ancestry = new StringBuilder();
        ancestry.append(rtId);
        ancestry.append(ANCESTRY_ENTRY_DELIM);
        ancestry.append(resId);
        ancestry.append(ANCESTRY_ENTRY_DELIM);
        ancestry.append(resName);
        if (null != parentAncestry) {
            ancestry.append(ANCESTRY_DELIM);
            ancestry.append(parentAncestry);
        }
        return ancestry.toString();
    }

}
