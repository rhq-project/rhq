/*
 * RHQ Management Platform
 * Copyright 2012, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
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

package org.rhq.enterprise.server.test;

import java.sql.Connection;

import javax.annotation.Resource;
import javax.ejb.Singleton;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.sql.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.db.upgrade.ServerVersionColumnUpgrader;
import org.rhq.core.db.upgrade.StorageNodeVersionColumnUpgrader;
import org.rhq.core.domain.cloud.Server;
import org.rhq.core.domain.cloud.StorageNode;
import org.rhq.core.util.jdbc.JDBCUtil;
import org.rhq.enterprise.server.RHQConstants;
import org.rhq.enterprise.server.naming.NamingHack;

/**
 * This is a replacement for the fullblown {@link StartupBean} of the actual RHQ server.
 * @author Lukas Krejci
 */
@Singleton
public class StrippedDownStartupBean {
    private static final Log LOG = LogFactory.getLog(StrippedDownStartupBean.class);

    public static final String RHQ_SERVER_NAME_PROPERTY = "rhq.server.high-availability.name";

    static final String RHQ_VERSION = System.getProperty("project.version");

    @PersistenceContext(unitName = RHQConstants.PERSISTENCE_UNIT_NAME)
    private EntityManager entityManager;

    @Resource(name = "RHQ_DS", mappedName = RHQConstants.DATASOURCE_JNDI_NAME)
    private DataSource dataSource;

    private void secureNaming() {
        NamingHack.bruteForceInitialContextFactoryBuilder();
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void init() {
        secureNaming();
    }

    /**
     * <p>
     * Purges the test server and any storage nodes created during server initialization from a prior test run.
     * </p>
     */
    public void purgeTestServerAndStorageNodes() {
        entityManager.createQuery("DELETE FROM " + StorageNode.class.getName()).executeUpdate();
        entityManager.createQuery("DELETE FROM " + Server.class.getName() + " WHERE name = :serverName")
            .setParameter("serverName", TestConstants.RHQ_TEST_SERVER_NAME)
            .executeUpdate();
    }

    // The version columns is not managed by db-upgrade so its need to be created here if not present
    // We don't support a Tx here so that any DDL executed by createXxxVersionColumnIfNeeded
    // succeeds on Oracle, which in-effect applies autocommit=true for DDL changes.
    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public void createStorageNodeVersionColumnIfNeeded() {
        Connection connection = null;
        try {
            connection = dataSource.getConnection();
            StorageNodeVersionColumnUpgrader versionColumnUpgrader = new StorageNodeVersionColumnUpgrader();
            versionColumnUpgrader.upgrade(connection, RHQ_VERSION);
            versionColumnUpgrader.setVersionForAllNodes(connection, RHQ_VERSION);
        } catch (Exception e) {
            LOG.error("Could not check storage node version column", e);
        } finally {
            JDBCUtil.safeClose(connection);
        }
    }

    // The version columns is not managed by db-upgrade so its need to be created here if not present
    // We don't support a Tx here so that any DDL executed by createXxxVersionColumnIfNeeded
    // succeeds on Oracle, which in-effect applies autocommit=true for DDL changes.
    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public void createServerVersionColumnIfNeeded() {
        Connection connection = null;
        try {
            connection = dataSource.getConnection();
            ServerVersionColumnUpgrader versionColumnUpgrader = new ServerVersionColumnUpgrader();
            versionColumnUpgrader.upgrade(connection, RHQ_VERSION);
            versionColumnUpgrader.setVersionForAllServers(connection, RHQ_VERSION);
        } catch (Exception e) {
            LOG.error("Could not check server version column", e);
        } finally {
            JDBCUtil.safeClose(connection);
        }
    }

}
