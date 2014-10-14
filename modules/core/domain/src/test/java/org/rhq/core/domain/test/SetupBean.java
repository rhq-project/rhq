/*
 * RHQ Management Platform
 * Copyright (C) 2005-2014 Red Hat, Inc.
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
 * along with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA
 */

package org.rhq.core.domain.test;

import java.lang.reflect.Method;
import java.sql.Connection;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.sql.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.util.jdbc.JDBCUtil;

/**
 * @author Thomas Segismont
 */
@Singleton
@Startup
public class SetupBean {
    private static final Log LOG = LogFactory.getLog(SetupBean.class);

    private static final String RHQ_VERSION = System.getProperty("project.version");

    @Resource(name = "RHQ_DS", mappedName = "java:jboss/datasources/RHQDS")
    private DataSource dataSource;

    @PostConstruct
    public void init() throws RuntimeException {
        LOG.info("Begin core domain tests setup");
        LOG.info("RHQ_VERSION = " + RHQ_VERSION);
        createStorageNodeVersionColumnIfNeeded();
        createServerVersionColumnIfNeeded();
        LOG.info("End core domain tests setup");
    }

    // The version columns is not managed by db-upgrade so its need to be created here if not present
    private void createStorageNodeVersionColumnIfNeeded() {
        Connection connection = null;
        try {
            connection = dataSource.getConnection();
            // Because of a cyclic dependency issue, the core domain module cannot depend on the core dbutils module
            // So I'm falling back to reflection (the dbutils module will be available at runtime)
            Class<?> upgraderClass = Class.forName("org.rhq.core.db.upgrade.StorageNodeVersionColumnUpgrader");
            Object versionColumnUpgrader = upgraderClass.newInstance();
            Method upgradeMethod = upgraderClass.getMethod("upgrade", Connection.class, String.class);
            upgradeMethod.invoke(versionColumnUpgrader, connection, RHQ_VERSION);
            Method setVersionForAllNodesMethod = upgraderClass.getMethod("setVersionForAllNodes", Connection.class,
                String.class);
            setVersionForAllNodesMethod.invoke(versionColumnUpgrader, connection, RHQ_VERSION);
        } catch (Exception e) {
            LOG.error("Could not check storage node version column", e);
        } finally {
            JDBCUtil.safeClose(connection);
        }
    }

    // The version columns is not managed by db-upgrade so its need to be created here if not present
    private void createServerVersionColumnIfNeeded() {
        Connection connection = null;
        try {
            connection = dataSource.getConnection();
            Class<?> upgraderClass = Class.forName("org.rhq.core.db.upgrade.ServerVersionColumnUpgrader");
            Object versionColumnUpgrader = upgraderClass.newInstance();
            Method upgradeMethod = upgraderClass.getMethod("upgrade", Connection.class, String.class);
            upgradeMethod.invoke(versionColumnUpgrader, connection, RHQ_VERSION);
            Method setVersionForAllServersMethod = upgraderClass.getMethod("setVersionForAllServers", Connection.class,
                String.class);
            setVersionForAllServersMethod.invoke(versionColumnUpgrader, connection, RHQ_VERSION);
        } catch (Exception e) {
            LOG.error("Could not check server version column", e);
        } finally {
            JDBCUtil.safeClose(connection);
        }
    }
}
