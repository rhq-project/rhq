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

package org.rhq.enterprise.server.storage;

import static org.rhq.server.metrics.StorageClientConstants.DATA_CENTER;
import static org.rhq.server.metrics.StorageClientConstants.LOAD_BALANCING;
import static org.rhq.server.metrics.StorageClientConstants.REQUEST_LIMIT;
import static org.rhq.server.metrics.StorageClientConstants.REQUEST_LIMIT_MIN;
import static org.rhq.server.metrics.StorageClientConstants.REQUEST_TIMEOUT_DAMPENING;
import static org.rhq.server.metrics.StorageClientConstants.REQUEST_TIMEOUT_DELTA;
import static org.rhq.server.metrics.StorageClientConstants.REQUEST_TOPOLOGY_CHANGE_DELTA;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.ejb.ConcurrencyManagement;
import javax.ejb.ConcurrencyManagementType;
import javax.ejb.EJB;
import javax.ejb.LocalBean;
import javax.ejb.Singleton;
import javax.ejb.Timeout;
import javax.ejb.Timer;
import javax.ejb.TimerConfig;
import javax.ejb.TimerService;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.management.ObjectName;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.HostDistance;
import com.datastax.driver.core.PoolingOptions;
import com.datastax.driver.core.ProtocolOptions;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.exceptions.NoHostAvailableException;
import com.datastax.driver.core.policies.DCAwareRoundRobinPolicy;
import com.datastax.driver.core.policies.DefaultRetryPolicy;
import com.datastax.driver.core.policies.LoadBalancingPolicy;
import com.datastax.driver.core.policies.LoggingRetryPolicy;
import com.datastax.driver.core.policies.RoundRobinPolicy;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.cassandra.schema.SchemaManager;
import org.rhq.cassandra.util.ClusterBuilder;
import org.rhq.core.domain.cloud.StorageNode;
import org.rhq.core.domain.common.composite.SystemSetting;
import org.rhq.core.domain.common.composite.SystemSettings;
import org.rhq.core.util.ObjectNameFactory;
import org.rhq.core.util.PropertiesFileUpdate;
import org.rhq.core.util.exception.ThrowableUtil;
import org.rhq.enterprise.server.auth.SubjectManagerLocal;
import org.rhq.enterprise.server.cloud.StorageNodeManagerLocal;
import org.rhq.enterprise.server.core.CoreServer;
import org.rhq.enterprise.server.measurement.MeasurementScheduleManagerLocal;
import org.rhq.enterprise.server.system.SystemManagerLocal;
import org.rhq.enterprise.server.util.JMXUtil;
import org.rhq.server.metrics.DateTimeService;
import org.rhq.server.metrics.MetricsConfiguration;
import org.rhq.server.metrics.MetricsConstants;
import org.rhq.server.metrics.MetricsDAO;
import org.rhq.server.metrics.MetricsServer;
import org.rhq.server.metrics.StorageSession;

/**
 * @author John Sanda
 */
@Singleton
@LocalBean
@ConcurrencyManagement(ConcurrencyManagementType.BEAN)
public class StorageClientManager implements StorageClientManagerMBean{

    private static final ObjectName OBJECT_NAME = ObjectNameFactory.create("rhq:service=StorageClientManager");

    private static final Log LOG = LogFactory.getLog(StorageClientManager.class);

    private static final String RHQ_KEYSPACE = "rhq";

    @EJB
    private SubjectManagerLocal subjectManager;
    @EJB
    private StorageNodeManagerLocal storageNodeManager;
    @EJB
    private SystemManagerLocal systemManager;
    @EJB
    private CoreServer coreServer;

    @EJB
    private MeasurementScheduleManagerLocal measurementScheduleManager;

    @javax.annotation.Resource
    private TimerService timerService;

    private Cluster cluster;
    private StorageSession session;
    private MetricsConfiguration metricsConfiguration;
    private MetricsDAO metricsDAO;
    private MetricsServer metricsServer;
    private boolean initialized;
    private StorageClusterMonitor storageClusterMonitor;

    private String cachedStorageUsername;
    private String cachedStoragePassword;

    public void scheduleStorageSessionMaintenance() {
        // each time the webapp is reloaded, we don't want to create duplicate jobs
        Collection<Timer> timers = timerService.getTimers();
        for (Timer existingTimer : timers) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Found timer - attempting to cancel: " + existingTimer.toString());
            }
            try {
                existingTimer.cancel();
            } catch (Exception e) {
                LOG.warn("Failed in attempting to cancel timer: " + existingTimer.toString());
            }
        }

        // timer that will trigger every 90 seconds after an initial wait of 30 seconds
        timerService.createIntervalTimer(30000L, 90000L, new TimerConfig(null, false));
    }

    /**
     * If the session is not initialized then attempt to initialize it.
     * If the session is initialized then verify to ensure that the
     * session uses the latest set of credentials.
     *
     * @param timer
     */
    @Timeout
    public void storageSessionMaintenance(Timer timer) {
        if (!initialized) {
            this.init();
        } else {
            boolean refreshResult = this.refreshCredentialsAndSession();
            if (!refreshResult) {
                LOG.error("Storage session credentials not succesfully refreshed!");
            } else {
                LOG.debug("Storage session credentials refreshed.");
            }
        }
    }

    /**
     * @return <true> if the storage subsystem is running or if a session was initialized, <false> otherwise
     */
    public synchronized boolean init() {
        if (initialized) {
            LOG.debug("Storage client subsystem is already initialized. Skipping initialization.");
            return initialized;
        }

        LOG.info("Initializing storage client subsystem");

        try {
            Session wrappedSession = createSession();
            session = new StorageSession(wrappedSession);

            storageClusterMonitor = new StorageClusterMonitor(session);
            storageClusterMonitor.updateRequestLimit();
            session.addStorageStateListener(storageClusterMonitor);

            metricsConfiguration = new MetricsConfiguration();
            metricsDAO = new MetricsDAO(session, metricsConfiguration);

            initMetricsServer();
            JMXUtil.registerMBean(this, OBJECT_NAME);
            initialized = true;

            initialized = true;
            LOG.info("Storage client subsystem is now initialized");
        } catch (NoHostAvailableException e) {
            initialized = false;
            if (cluster != null) {
                cluster.shutdown();
            }

            LOG.warn("Storage client subsystem wasn't initialized because it wasn't possible to connect to the"
                + " storage cluster. The RHQ server is set to MAINTENANCE mode. Please start the storage cluster"
                + " as soon as possible.", e);
        } catch (Throwable t) {
            initialized = false;
            if (cluster != null) {
                cluster.shutdown();
            }

            LOG.warn("Storage client subsystem wasn't initialized. The RHQ server will be set to MAINTENANCE mode. Please verify "
                    + " that the storage cluster is operational.", t);
        }

        return initialized;
    }

    /**
     * Checks the system configuration to see if the storage credentials
     * changed from when the current session got initialized.
     *
     * 1) If the credentials are identical then no changes are required and
     *    the current session is good.
     * 2) If the credentials are different then create a new session with the
     *    new credentials and register it with the session manager.
     *
     * @return <true> if a new session was sucessfully created or no new session is required; <false> otherwise
     */
    public synchronized boolean refreshCredentialsAndSession() {
        if (!initialized) {
            LOG.debug("Storage client subsystem not initialized. Skipping session refresh.");
            return false;
        }

        SystemSettings settings = systemManager.getObfuscatedSystemSettings(true);
        String username = settings.get(SystemSetting.STORAGE_USERNAME);
        String password = settings.get(SystemSetting.STORAGE_PASSWORD);

        if ((username != null && !username.equals(this.cachedStorageUsername))
            || (password != null && !password.equals(this.cachedStoragePassword))) {

            Session wrappedSession;
            try {
                wrappedSession = createSession();
            } catch (NoHostAvailableException e) {
                if (cluster != null) {
                    cluster.shutdown();
                }

                LOG.warn("Storage client subsystem wasn't initialized because it wasn't possible to connect to the"
                    + " storage cluster. The RHQ server is set to MAINTENANCE mode. Please start the storage cluster"
                    + " as soon as possible.", e);
                return false;
            }

            session.registerNewSession(wrappedSession);
            metricsDAO.initPreparedStatements();
            return true;
        }

        return true;
    }

    /**
     * Checks storage node schema compatibility.
     *
     * @param username username
     * @param password password
     * @param storageNodes storage nodes
     */
    private void checkSchemaCompability(String username, String password, List<StorageNode> storageNodes) {
        String[] nodes = new String[storageNodes.size()];
        for (int index = 0; index < storageNodes.size(); index++) {
            nodes[index] = storageNodes.get(index).getAddress();
        }
        int cqlPort = storageNodes.get(0).getCqlPort();

        SchemaManager schemaManager = new SchemaManager(username, password, nodes, cqlPort);
        try {
            schemaManager.checkCompatibility();
        } catch (NoHostAvailableException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage(), e);
        } finally {
            schemaManager.shutdown();
        }
    }

    public synchronized void shutdown() {
        LOG.info("Shutting down storage client subsystem");

        if (metricsServer != null) {
            metricsServer.shutdown();
            metricsServer = null;
        }

        metricsDAO = null;

        try {
            if (cluster != null) {
                cluster.shutdown();
            }
        } catch (Exception e) {
            LOG.error("Failed to shutdown the cluster connection manager for the storage cluster.", e);
        }

        cluster = null;
        session = null;
        JMXUtil.unregisterMBeanQuietly(OBJECT_NAME);
        initialized = false;
    }

    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public MetricsDAO getMetricsDAO() {
        return metricsDAO;
    }

    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public MetricsServer getMetricsServer() {
        return metricsServer;
    }

    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public StorageSession getSession() {
        return session;
    }

    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public MetricsConfiguration getMetricsConfiguration() {
        return metricsConfiguration;
    }

    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public boolean isClusterAvailable() {
        return storageClusterMonitor != null && storageClusterMonitor.isClusterAvailable();
    }

    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public int getAggregationBatchSize() {
        return metricsServer.getAggregationBatchSize();
    }

    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public void setAggregationBatchSize(int batchSize) {
        metricsServer.setAggregationBatchSize(batchSize);
        persistStorageProperty(MetricsConstants.AGGREGATION_BATCH_SIZE, Integer.toString(batchSize));
    }

    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public int getAggregationParallelism() {
        return metricsServer.getAggregationParallelism();
    }

    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public void setAggregationParallelism(int parallelism) {
        metricsServer.setAggregationParallelism(parallelism);
        persistStorageProperty(MetricsConstants.AGGREGATION_PARALLELISM, Integer.toString(parallelism));
    }

    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public int getAggregationWorkers() {
        return metricsServer.getNumAggregationWorkers();
    }

    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public void setAggregationWorkers(int numWorkers) {
        persistStorageProperty(MetricsConstants.AGGREGATION_WORKERS, Integer.toString(numWorkers));
    }

    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public int getRawDataAgeLimit() {
        return metricsServer.getRawDataAgeLimit();
    }

    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public void setRawDataAgeLimit(int ageLimit) {
        metricsServer.setRawDataAgeLimit(ageLimit);
        persistStorageProperty(MetricsConstants.RAW_DATA_AGE_LIMIT, Integer.toString(ageLimit));
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public double getRequestLimit() {
        return session.getRequestLimit();
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public void setRequestLimit(double requestLimit) {
        session.setRequestLimit(requestLimit);
        persistStorageProperty(REQUEST_LIMIT, Double.toString(requestLimit));
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public double getMinRequestLimit() {
        return session.getMinRequestLimit();
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public void setMinRequestLimit(double minRequestLimit) {
        session.setMinRequestLimit(minRequestLimit);
        persistStorageProperty(REQUEST_LIMIT_MIN, Double.toString(minRequestLimit));
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public double getRequestLimitTopologyDelta() {
        return session.getTopologyDelta();
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public void setRequestLimitTopologyDelta(double delta) {
        session.setTopologyDelta(delta);
        persistStorageProperty(REQUEST_TOPOLOGY_CHANGE_DELTA, Double.toString(delta));
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public double getRequestTimeoutDelta() {
        return session.getTimeoutDelta();
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public void setRequestTimeoutDelta(double requestTimeoutDelta) {
        session.setTimeoutDelta(requestTimeoutDelta);
        persistStorageProperty(REQUEST_TIMEOUT_DELTA, Double.toString(requestTimeoutDelta));
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public long getRequestTimeoutDampening() {
        return session.getTimeoutDampening();
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public void setRequestTimeoutDampening(long requestTimeoutDampening) {
        session.setTimeoutDampening(requestTimeoutDampening);
        persistStorageProperty(REQUEST_TIMEOUT_DAMPENING, Long.toString(requestTimeoutDampening));
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public long getRequestTimeouts() {
        return session.getTimeouts();
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public long getTotalRequests() {
        return session.getTimeouts();
    }

    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public void persistStorageProperty(String key, String value) {
        if (Boolean.getBoolean("running.itests-2")) {
            // When running itests-2, there is no server props file, so avoid logging a confusing exception
            return;
        }
        PropertiesFileUpdate updater = new PropertiesFileUpdate(getServerPropsFile().getAbsolutePath());
        try {
            updater.update(key, value);
        } catch (IOException e) {
            // TODO should we propagate the exception?
            LOG.warn("Failed to persist property " + key + " due to unexpected I/O error",
                ThrowableUtil.getRootCause(e));
        }
    }

    private File getServerPropsFile() {
        File installDir = coreServer.getInstallDir();
        File binDir = new File(installDir, "bin");
        return new File(binDir, "rhq-server.properties");
    }

    private Session createSession() {
        // Always get the creds from the DB, system props may not be up to date at install time
        // the code assumes the passwords to be obfuscated, because they can also come that way from other sources
        // (like property files). So let's make our lives easy and always use obfuscated passwords.
        SystemSettings settings = systemManager.getObfuscatedSystemSettings(true);
        this.cachedStorageUsername = settings.get(SystemSetting.STORAGE_USERNAME);
        this.cachedStoragePassword = settings.get(SystemSetting.STORAGE_PASSWORD);

        List<StorageNode> storageNodes = new ArrayList<StorageNode>();
        for (StorageNode storageNode : storageNodeManager.getStorageNodes()) {
            // We only want clustered nodes here because we won't be able to connect to
            // node that is not part of the cluster. The filtering here on the operation
            // mode is somewhat convservative because we could also include ADD_MAINTENANCE
            // and REMOVE_MAINTENANCE, but this errors on the side of being safe. Lastly,
            // if a storage node does not have a resource, then that means it was was
            // deployed prior to installing the server.
            if (storageNode.getOperationMode() == StorageNode.OperationMode.NORMAL
                || storageNode.getOperationMode() == StorageNode.OperationMode.MAINTENANCE
                || storageNode.getResource() == null) {
                storageNodes.add(storageNode);
            }
        }

        if (storageNodes.isEmpty()) {
            throw new IllegalStateException(
                "There is no storage node metadata stored in the relational database. This may have happened as a "
                    + "result of running dbsetup or deleting rows from rhq_storage_node table. Please re-install the "
                    + "storage node to fix this issue.");
        }

        checkSchemaCompability(this.cachedStorageUsername, this.cachedStoragePassword, storageNodes);

        LOG.debug("Initializing session to connect to storage node cluster");
        List<String> hostNames = new ArrayList<String>();
        for (StorageNode storageNode : storageNodes) {
            hostNames.add(storageNode.getAddress());
        }
        int port = storageNodes.get(0).getCqlPort();

        cluster = new ClusterBuilder().addContactPoints(hostNames.toArray(new String[hostNames.size()]))
            .withCredentialsObfuscated(this.cachedStorageUsername, this.cachedStoragePassword).withPort(port)
            .withLoadBalancingPolicy(getLoadBalancingPolicy())
            .withRetryPolicy(new LoggingRetryPolicy(DefaultRetryPolicy.INSTANCE)).withCompression(
                ProtocolOptions.Compression.NONE).build();

        PoolingOptions poolingOptions = cluster.getConfiguration().getPoolingOptions();
        poolingOptions.setCoreConnectionsPerHost(HostDistance.LOCAL, Integer.parseInt(
            System.getProperty("rhq.storage.client.local-connections", "24")));
        poolingOptions.setCoreConnectionsPerHost(HostDistance.REMOTE, Integer.parseInt(
            System.getProperty("rhq.storage.client.remote-connections", "16")));
        poolingOptions.setMaxConnectionsPerHost(HostDistance.LOCAL, Integer.parseInt(
            System.getProperty("rhq.storage.client.max-local-connections", "32")));
        poolingOptions.setMaxConnectionsPerHost(HostDistance.REMOTE, Integer.parseInt(
            System.getProperty("rhq.storage.client.max-remote-connections", "24")));

        return cluster.connect(RHQ_KEYSPACE);
    }

    private LoadBalancingPolicy getLoadBalancingPolicy() {
        String policy = System.getProperty(LOAD_BALANCING);
        if (policy == null || policy.equals("RoundRobin")) {
            return new RoundRobinPolicy();
        }
        if (policy.equals("DCAwareRoundRobin")) {
            String dataCenter = System.getProperty(DATA_CENTER);
            if (dataCenter == null) {
                LOG.warn(policy + " was specified for " + LOAD_BALANCING + " but " + DATA_CENTER + " is undefined." +
                    "Reverting to RoundRobin load balancing policy.");
                return new RoundRobinPolicy();
            } else {
                return new DCAwareRoundRobinPolicy(dataCenter);
            }
        }
        LOG.warn(policy + " is not a supported load balancing policy. Reverting to RoundRobin load balancing policy.");

        return new RoundRobinPolicy();
    }

    private void initMetricsServer() {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Initializing " + MetricsServer.class.getName());
        }
        metricsServer = new MetricsServer();
        metricsServer.setDAO(metricsDAO);
        metricsServer.setConfiguration(metricsConfiguration);

        DateTimeService dateTimeService = new DateTimeService();
        dateTimeService.setConfiguration(metricsConfiguration);
        metricsServer.setDateTimeService(dateTimeService);
        metricsServer.init();
    }

}
