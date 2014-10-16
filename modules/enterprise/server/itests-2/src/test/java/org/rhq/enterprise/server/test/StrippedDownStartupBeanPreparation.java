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

package org.rhq.enterprise.server.test;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.ejb.Timeout;
import javax.ejb.TimerConfig;
import javax.ejb.TimerService;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.domain.cloud.Server;
import org.rhq.core.domain.cloud.StorageNode;
import org.rhq.enterprise.server.RHQConstants;
import org.rhq.enterprise.server.cloud.instance.ServerManagerLocal;

/**
 * This startup singleton EJB is here to work around bug AS7-5530 and to
 * schedule the real StartupBean's work in a delayed fashion (this is to allow
 * AS7 to complete its deployment work before we do our work).
 *
 * See https://issues.jboss.org/browse/AS7-5530
 *
 * This version is a replacement for the original code (identical) code that uses the StrippedDownStartupBean instead
 * of the fullblown original.
 */
@Singleton
@Startup
public class StrippedDownStartupBeanPreparation {
    private static final Log LOG = LogFactory.getLog(StrippedDownStartupBeanPreparation.class);

    @EJB
    private StrippedDownStartupBean startupBean;

    @EJB
    private ServerManagerLocal serverManager;

    @PersistenceContext(unitName = RHQConstants.PERSISTENCE_UNIT_NAME)
    private EntityManager entityManager;

    @Resource
    private TimerService timerService; // needed to schedule our startup bean init call

    @PostConstruct
    public void initWithTransactionBecauseAS75530() throws RuntimeException {
        startupBean.createStorageNodeVersionColumnIfNeeded();
        startupBean.createServerVersionColumnIfNeeded();
        LOG.info("Scheduling the initialization of the testing RHQ deployment");
        timerService.createSingleActionTimer(1, new TimerConfig(null, false)); // call StartupBean in 1ms
        startupBean.purgeTestServerAndStorageNodes();
        createTestServer();
        loadCassandraConnectionProps();
        createStorageNodes();
    }

    /**
     * The storage client initialization performed by {@link org.rhq.enterprise.server.storage.StorageClientManager#init()}
     * requires having a server entity in the database which will be the case with a regular deployment. This method
     * persists a server before any tests execute. If the server entity does not exist or cannot be loaded, then the
     * storage client will not initialize properly and any tests that depend on the storage client will fail.
     */
    private void createTestServer() {
        Server server = new Server();
        server.setName(TestConstants.RHQ_TEST_SERVER_NAME);
        server.setAddress("127.0.0.1");
        server.setOperationMode(Server.OperationMode.INSTALLED);
        server.setPort(7080);
        server.setSecurePort(7443);
        server.setVersion(StrippedDownStartupBean.RHQ_VERSION);

        serverManager.create(server);
        System.setProperty(TestConstants.RHQ_SERVER_NAME_PROPERTY, TestConstants.RHQ_TEST_SERVER_NAME);
    }

    private void createStorageNodes() {
        String[] nodes = System.getProperty("rhq.storage.nodes").split(",");
        String cqlPort = System.getProperty("rhq.storage.cql-port");
        for (String node : nodes) {
            StorageNode storageNode = new StorageNode();
            storageNode.setAddress(node);
            storageNode.setCqlPort(Integer.parseInt(cqlPort));
            storageNode.setOperationMode(StorageNode.OperationMode.NORMAL);
            storageNode.setVersion(StrippedDownStartupBean.RHQ_VERSION);
            entityManager.persist(storageNode);
        }
    }

    public void loadCassandraConnectionProps() {
        InputStream stream;
        try {
            stream = getClass().getResourceAsStream("/cassandra-test.properties");
            Properties props = new Properties();
            props.load(stream);

            // DO NOT use System.setProperties(Properties). I previously tried that and it
            // caused some arquillian deployment exception.
            //
            // jsanda
            System.setProperty("rhq.storage.username", props.getProperty("rhq.storage.username"));
            System.setProperty("rhq.storage.password", props.getProperty("rhq.storage.password"));
            System.setProperty("rhq.storage.nodes", props.getProperty("rhq.storage.nodes"));
            System.setProperty("rhq.storage.cql-port", props.getProperty("rhq.storage.cql-port"));
            System.setProperty("rhq.storage.gossip-port", props.getProperty("rhq.storage.gossip-port"));
        } catch (IOException e) {
            throw new RuntimeException(("Failed to load cassandra-test.properties"));
        }

        String cqlPort = System.getProperty("rhq.storage.cql-port");
        entityManager.createNativeQuery(
            "update rhq_system_config set property_value = '" + cqlPort + "', default_property_value = '" + cqlPort
                + "' where property_key = 'STORAGE_CQL_PORT'").executeUpdate();

        String gossipPort = System.getProperty("rhq.storage.gossip-port");
        entityManager.createNativeQuery(
            "update rhq_system_config set property_value = '" + gossipPort + "', default_property_value = '"
                + gossipPort + "' where property_key = 'STORAGE_GOSSIP_PORT'").executeUpdate();

        String storageUserName = System.getProperty("rhq.storage.username");
        entityManager.createNativeQuery(
            "update rhq_system_config set property_value = '" + storageUserName + "', default_property_value = '"
                + storageUserName + "' where property_key = 'STORAGE_USERNAME'").executeUpdate();

        String storagePassword = System.getProperty("rhq.storage.password");
        entityManager.createNativeQuery(
            "update rhq_system_config set property_value = '" + storagePassword + "', default_property_value = '"
                + storagePassword + "' where property_key = 'STORAGE_PASSWORD'").executeUpdate();
    }

    @Timeout
    public void initializeServer() throws RuntimeException {
        try {
            LOG.info("Initializing the testing RHQ deployment");
            this.startupBean.init();
            LOG.info("Initialization complete");
        } catch (Throwable t) {
            // do NOT allow exceptions to bubble out of our method because then
            // the EJB container would simply re-trigger the timer and call us again
            // and we don't want to keep failing over and over filling the logs
            // in an infinite loop.
            LOG.fatal("The server failed to start up properly", t);
        }
    }

}
