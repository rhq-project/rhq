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

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

import mazz.i18n.Logger;

import org.apache.maven.artifact.versioning.ComparableVersion;

import org.rhq.core.db.DatabaseType;
import org.rhq.core.db.DbUtilsI18NFactory;
import org.rhq.core.db.DbUtilsI18NResourceKeys;

/**
 * Database upgrade task 2.130 to fix BZ 916380 where bundle version orders got disordered when deleting bundles.
 *
 * @author John Mazzitelli
 */
public class BundleVersionOrderUpgradeTask implements DatabaseUpgradeTask {

    private static final Logger LOG = DbUtilsI18NFactory.getLogger(BundleVersionOrderUpgradeTask.class);

    public void execute(DatabaseType databaseType, Connection connection) throws SQLException {
        List<Integer> bundleIds = getAllBundleIds(databaseType, connection);
        if (bundleIds != null && !bundleIds.isEmpty()) {
            for (Integer bundleId : bundleIds) {
                List<VersionInfo> versionInfos = getBundleVersions(bundleId, databaseType, connection);
                if (versionInfos != null && !versionInfos.isEmpty()) {

                    // make sure we order the versions properly by comparing version strings
                    TreeMap<ComparableVersion, VersionInfo> ordered = new TreeMap<ComparableVersion, VersionInfo>();
                    for (VersionInfo versionInfo : versionInfos) {
                        ComparableVersion comparableVersion = new ComparableVersion(versionInfo.version);
                        ordered.put(comparableVersion, versionInfo);
                    }

                    // now make sure all version ordering is correct - starting from 0 and monotonically increasing
                    int expectedNext = 0;
                    for (VersionInfo orderedBundleVersion : ordered.values()) {
                        if (orderedBundleVersion.versionOrder != expectedNext) {
                            String sql = "UPDATE RHQ_BUNDLE_VERSION SET VERSION_ORDER=" + expectedNext + " WHERE ID="
                                + orderedBundleVersion.id;
                            Statement s = null;
                            try {
                                LOG.debug(DbUtilsI18NResourceKeys.MESSAGE,
                                    "Correcting version order for bundle version ID [" + orderedBundleVersion.id + "]");
                                LOG.debug(DbUtilsI18NResourceKeys.EXECUTING_SQL, sql);
                                s = connection.createStatement();
                                int rowsUpdated = s.executeUpdate(sql);
                                if (rowsUpdated != 1) {
                                    LOG.error(DbUtilsI18NResourceKeys.ERROR,
                                        "Failed to update version order for bundle version ID ["
                                            + orderedBundleVersion.id + "]. Will continue but problems may persist.");
                                }
                            } finally {
                                databaseType.closeStatement(s);
                            }
                        }
                        expectedNext++;
                    }
                }
            }
        } else {
            LOG.debug(DbUtilsI18NResourceKeys.MESSAGE, "No bundles exist - nothing to fix.");
        }
    }

    private List<Integer> getAllBundleIds(DatabaseType databaseType, Connection connection) throws SQLException {
        List<Integer> ids = new ArrayList<Integer>();
        Statement s = null;
        try {
            String sql = "SELECT ID FROM RHQ_BUNDLE";
            LOG.debug(DbUtilsI18NResourceKeys.EXECUTING_SQL, sql);

            s = connection.createStatement();
            ResultSet rs = s.executeQuery(sql);

            while (rs.next()) {
                int id = rs.getInt(1);
                ids.add(Integer.valueOf(id));
            }
        } finally {
            databaseType.closeStatement(s);
        }
        return ids;
    }

    private List<VersionInfo> getBundleVersions(int bundleId, DatabaseType databaseType, Connection connection)
        throws SQLException {
        List<VersionInfo> infos = new ArrayList<VersionInfo>();
        Statement s = null;
        try {
            String sql = "SELECT ID, VERSION_ORDER, VERSION FROM RHQ_BUNDLE_VERSION WHERE BUNDLE_ID = " + bundleId;
            LOG.debug(DbUtilsI18NResourceKeys.EXECUTING_SQL, sql);

            s = connection.createStatement();
            ResultSet rs = s.executeQuery(sql);

            while (rs.next()) {
                VersionInfo info = new VersionInfo();
                info.id = rs.getInt(1);
                info.versionOrder = rs.getInt(2);
                info.version = rs.getString(3);
                infos.add(info);
            }
        } finally {
            databaseType.closeStatement(s);
        }
        return infos;
    }

    private class VersionInfo {
        int id;
        int versionOrder;
        String version;
    }
}