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
package org.rhq.enterprise.server.core;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.sql.Connection;
import java.util.Date;
import java.util.List;
import java.util.Properties;

import javax.annotation.Resource;
import javax.ejb.ConcurrencyManagement;
import javax.ejb.ConcurrencyManagementType;
import javax.ejb.EJB;
import javax.ejb.Singleton;
import javax.ejb.Timeout;
import javax.ejb.Timer;
import javax.ejb.TimerConfig;
import javax.ejb.TimerService;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.management.Attribute;
import javax.management.MBeanServer;
import javax.management.MBeanServerInvocationHandler;
import javax.management.ObjectName;
import javax.sql.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.quartz.CronTrigger;
import org.quartz.SchedulerException;

import org.rhq.core.db.DatabaseTypeFactory;
import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.cloud.Server;
import org.rhq.core.domain.cloud.StorageNode;
import org.rhq.core.domain.common.ProductInfo;
import org.rhq.core.domain.criteria.ServerCriteria;
import org.rhq.core.domain.criteria.StorageNodeCriteria;
import org.rhq.core.domain.resource.Agent;
import org.rhq.core.util.ObjectNameFactory;
import org.rhq.core.util.exception.ThrowableUtil;
import org.rhq.core.util.stream.StreamUtil;
import org.rhq.enterprise.communications.ServiceContainerConfigurationConstants;
import org.rhq.enterprise.communications.util.SecurityUtil;
import org.rhq.enterprise.server.RHQConstants;
import org.rhq.enterprise.server.alert.engine.internal.AlertConditionCacheCoordinator;
import org.rhq.enterprise.server.auth.SessionManager;
import org.rhq.enterprise.server.auth.SubjectManagerLocal;
import org.rhq.enterprise.server.cloud.StorageNodeManagerLocal;
import org.rhq.enterprise.server.cloud.TopologyManagerLocal;
import org.rhq.enterprise.server.cloud.instance.CacheConsistencyManagerLocal;
import org.rhq.enterprise.server.cloud.instance.ServerManagerLocal;
import org.rhq.enterprise.server.cloud.instance.SyncEndpointAddressException;
import org.rhq.enterprise.server.core.comm.ServerCommunicationsServiceUtil;
import org.rhq.enterprise.server.core.plugin.PluginDeploymentScannerMBean;
import org.rhq.enterprise.server.naming.NamingHack;
import org.rhq.enterprise.server.plugin.pc.MasterServerPluginContainer;
import org.rhq.enterprise.server.plugin.pc.ServerPluginServiceMBean;
import org.rhq.enterprise.server.resource.ResourceTypeManagerLocal;
import org.rhq.enterprise.server.scheduler.SchedulerLocal;
import org.rhq.enterprise.server.scheduler.jobs.AsyncResourceDeleteJob;
import org.rhq.enterprise.server.scheduler.jobs.CheckForSuspectedAgentsJob;
import org.rhq.enterprise.server.scheduler.jobs.CheckForTimedOutConfigUpdatesJob;
import org.rhq.enterprise.server.scheduler.jobs.CheckForTimedOutContentRequestsJob;
import org.rhq.enterprise.server.scheduler.jobs.CheckForTimedOutOperationsJob;
import org.rhq.enterprise.server.scheduler.jobs.CloudManagerJob;
import org.rhq.enterprise.server.scheduler.jobs.DataCalcJob;
import org.rhq.enterprise.server.scheduler.jobs.DataPurgeJob;
import org.rhq.enterprise.server.scheduler.jobs.DynaGroupAutoRecalculationJob;
import org.rhq.enterprise.server.scheduler.jobs.PurgePluginsJob;
import org.rhq.enterprise.server.scheduler.jobs.PurgeResourceTypesJob;
import org.rhq.enterprise.server.scheduler.jobs.ReplicationFactorCheckJob;
import org.rhq.enterprise.server.scheduler.jobs.SavedSearchResultCountRecalculationJob;
import org.rhq.enterprise.server.scheduler.jobs.StorageClusterReadRepairJob;
import org.rhq.enterprise.server.storage.StorageClientManager;
import org.rhq.enterprise.server.system.SystemManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;
import org.rhq.enterprise.server.util.concurrent.AlertSerializer;
import org.rhq.enterprise.server.util.concurrent.AvailabilityReportSerializer;
import org.rhq.server.metrics.DateTimeService;

/**
 * This startup singleton EJB performs the rest of the RHQ Server startup initialization.
 * In order for it to do its work properly, we must ensure everything has been deployed and started;
 * specifically, all EJBs must have been deployed and available.
 *
 * This bean is not meant for client consumption - it is only for startup initialization.
 *
 * BEAN ConcurrencyManagement is enough: the {@link #initialized} property is only modified on startup.
 */
@Singleton
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
@ConcurrencyManagement(ConcurrencyManagementType.BEAN)
public class StartupBean implements StartupLocal {
    private Log log = LogFactory.getLog(this.getClass());

    private volatile boolean initialized = false;

    private String error = "";

    @EJB
    private AgentManagerLocal agentManager;

    @EJB
    private CacheConsistencyManagerLocal cacheConsistencyManager;

    @EJB
    private TopologyManagerLocal topologyManager;

    @EJB
    private StorageNodeManagerLocal storageNodeManager;

    @EJB
    private ResourceTypeManagerLocal resourceTypeManager;

    @EJB
    private SchedulerLocal schedulerBean;

    @EJB
    private ServerManagerLocal serverManager;

    @EJB
    private SubjectManagerLocal subjectManager;

    @EJB
    private SystemManagerLocal systemManager;

    @EJB
    private ShutdownListener shutdownListener;

    @EJB
    private StorageClientManager storageClientManager;

    @Resource
    private TimerService timerService; // needed to schedule our plugin scanner

    @Resource(name = "RHQ_DS", mappedName = RHQConstants.DATASOURCE_JNDI_NAME)
    private DataSource dataSource;

    @Override
    public boolean isInitialized() {
        return this.initialized;
    }

    @Override
    public String getError() {
        return error;
    }

    /**
     * Modifies the naming subsystem to be able to check for Java security permissions on JNDI lookup.
     */
    private void secureNaming() {
        NamingHack.bruteForceInitialContextFactoryBuilder();
    }

    /**
     * Performs the final RHQ Server initialization work that needs to talk place. EJBs are available in this method.
     *
     * @throws RuntimeException
     */
    @Override
    public void init() throws RuntimeException {

        //[BZ 1161806] Make sure the default time zone is set to UTC prior to use
        @SuppressWarnings("unused")
        DateTimeService temp = new DateTimeService();

        checkTempDir();

        checkCluster();

        secureNaming();

        initialized = false;

        log.info("All business tier deployments are complete - finishing the startup...");

        // get singletons right now so we load the classes immediately into our classloader
        AlertConditionCacheCoordinator.getInstance();
        SessionManager.getInstance();
        AlertSerializer.getSingleton();
        AvailabilityReportSerializer.getSingleton();

        // load resource facets cache
        try {
            resourceTypeManager.reloadResourceFacetsCache();
        } catch (Throwable t) {
            error += (error.isEmpty() ? "" : ", ") + "reloading facets cache";
            log.error("Could not load ResourceFacets cache.", t);
        }

        //Server depends on the storage cluster availability. Since the storage client init just
        //establishes connectivity with the storage cluster, then run it before the server init.
        initStorageClient();

        // Before starting determine the operating mode of this server and
        // take any necessary initialization action. Must happen before comm startup since listeners
        // may be added.
        initializeServer();

        // The order here is important!!!
        // IF YOU WANT TO CHANGE THE ORDER YOU MUST GET THE CHANGE PEER-REVIEWED FIRST BEFORE COMMITTING IT!!!
        //
        // If we start the scheduler before the comm layer, what happens if a stored job needs to send a message?
        // But if we start the comm layer before the scheduler, what happens if a message is received that needs
        // a job scheduled for it? I think the former is more likely to happen than the latter
        // (that is, a scheduled job would more likely need to send a message; as opposed to an incoming message
        // causing a job to be scheduled), so that explains the ordering of the comm layer and the scheduler.
        startHibernateStatistics();
        initScheduler(); // make sure this is initialized before starting the plugin deployer
        startPluginDeployer(); // make sure this is initialized before starting the server plugin container
        startServerPluginContainer(); // before comm in case an agent wants to talk to it
        upgradeRhqUserSecurityDomainIfNeeded();
        startServerCommunicationServices();
        startScheduler();
        scheduleJobs();
        //startAgentClients(); // this could be expensive if we have large number of agents so skip it and we'll create them lazily
        //startEmbeddedAgent(); // this is obsolete - we no longer have an embedded agent
        registerShutdownListener();
        registerPluginDeploymentScannerJob();

        logServerStartedMessage();

        initialized = true;
        return;
    }

    private void checkTempDir() {
        File tmpDir = new File(System.getProperty("java.io.tmpdir"));
        if (!tmpDir.exists()) {
            log.warn("Invalid java.io.tmpdir: [" + tmpDir.getAbsolutePath() + "] does not exist.");
            try {
                log.info("Creating java.io.tmpdir: [" + tmpDir.getAbsolutePath() + "]");
                tmpDir.mkdir();
            } catch (Throwable t) {
                throw new RuntimeException("Startup failed: Could not create missing java.io.tmpdir ["
                    + tmpDir.getAbsolutePath() + "]", t);
            }
        }
        if (!tmpDir.isDirectory()) {
            throw new RuntimeException("Startup failed: java.io.tmpdir [" + tmpDir.getAbsolutePath()
                + "] is not a directory");
        }
        if (!tmpDir.canRead() || !tmpDir.canExecute()) {
            throw new RuntimeException("Startup failed: java.io.tmpdir [" + tmpDir.getAbsolutePath()
                + "] is not readable");
        }
        if (!tmpDir.canWrite()) {
            throw new RuntimeException("Startup failed: java.io.tmpdir [" + tmpDir.getAbsolutePath()
                + "] is not writable");
        }
    }

    /**
     * Ensure all Servers and StorageNodes are at the same version.  This prevents startup when an
     * upgrade of all cluster members is still in progress.
     */
    private void checkCluster() {
        try {
            Subject overlord = subjectManager.getOverlord();
            String version = this.getClass().getPackage().getImplementationVersion();

            ServerCriteria sc = new ServerCriteria();
            sc.clearPaging();
            List<Server> servers = topologyManager.findServersByCriteria(overlord, sc);
            for (Server server : servers) {
                //BZ-1184719: Exact match is too strict for cumulative patches. Also accept "-redhat-N" only differences.
                if (!version.equals(server.getVersion())
                    && !acceptableCpVersionDifference(version, server.getVersion())) {
                    throw new RuntimeException(
                        "Startup failed: Could not start Server because not all Servers are running the same version. This Server is running version ["
                            + version
                            + "] but Server ["
                            + server.getName()
                            + "] is running version ["
                            + server.getVersion()
                            + "] Please complete the upgrade for all Servers and StorageNodes before trying to start a server.");
                }
            }

            StorageNodeCriteria snc = new StorageNodeCriteria();
            snc.clearPaging();
            List<StorageNode> storageNodes = storageNodeManager.findStorageNodesByCriteria(overlord, snc);
            for (StorageNode storageNode : storageNodes) {
                //BZ-1184719: Exact match is too strict for cumulative patches. Also accept "-redhat-N" only differences.
                if (!version.equals(storageNode.getVersion())
                    && !acceptableCpVersionDifference(version, storageNode.getVersion())) {
                    throw new RuntimeException(
                        "Startup failed: Could not start Server because not all Storage Nodes are running the same version. This Server is running version ["
                            + version
                            + "] but Storage Node ["
                            + storageNode.getAddress()
                            + "] is running version ["
                            + storageNode.getVersion()
                            + "] Please complete the upgrade for all Servers and StorageNodes before trying to start a server.");
                }
            }
        } catch (Throwable t) {
            throw new RuntimeException("Startup failed: Could not validate Server or Storage Node versions", t);
        }
    }

    //BZ-1184719: Tests whether the only difference between the the RHQ versioned
    //  components is '-redhat-N' as used by cumulative patched components.
    /** BZ-1184719: Tests whether the Server or Storage Node versions
     *  differ by cumulative patch versioning only.
     *  Ex. 4.12.0.JON330GA-redhat-1 vs. 4.12.0.JON330GA is acceptably different.
     *  Ex. 4.12.0.JON330GA-redhat-3 vs. 4.12.0.JON330GA-redhat-9 is acceptably different.
     * @param version
     * @param secondVersion
     * @return boolean about whether difference is acceptable.
     */
    private boolean acceptableCpVersionDifference(String version, String secondVersion) {
        boolean acceptableCpVersionDelta = false;
        String cpFrag = "-redhat-";
        //fails early if neither version contains cp identifier.
        if ((version.indexOf(cpFrag) > -1) || (secondVersion.indexOf(cpFrag) > -1)) {
            String[] versionComponents = version.split(cpFrag);
            String[] secondVersionComponents = secondVersion.split(cpFrag);
            //Ex. 4.12.0.JON330GA-redhat-3 vs. 4.12.0.JON330GA-redhat-9
            if (versionComponents.length == secondVersionComponents.length) {
                if ((versionComponents.length > 0) && (versionComponents[0].equals(secondVersionComponents[0]))) {//compare prefix
                    String index1 = versionComponents[1];
                    String index2 = secondVersionComponents[1];
                    if (versionComponents.length > 1) {
                        try {
                            Integer.parseInt(index1);
                            Integer.parseInt(index2);
                            acceptableCpVersionDelta = true;
                        } catch (NumberFormatException e) {
                        }
                    }
                }
            } else { //4.12.0.JON330GA-redhat-1 vs. 4.12.0.JON330GA
                if ((versionComponents.length > 0) && (versionComponents[0].equals(secondVersionComponents[0]))) {
                    String index1 = "0";
                    String index2 = "0";
                    if (versionComponents.length > 1) {
                        index1 = versionComponents[1];
                    } else {
                        index2 = secondVersionComponents[1];
                    }
                    try {
                        Integer.parseInt(index1);
                        Integer.parseInt(index2);
                        acceptableCpVersionDelta = true;
                    } catch (NumberFormatException e) {
                    }
                }
            }
        }
        return acceptableCpVersionDelta;
    }

    private long readShutdownTimeLogFile() throws Exception {
        File timeFile = shutdownListener.getShutdownTimeLogFile();
        if (!timeFile.exists()) {
            // this is probably ok, perhaps its the first time we started this server, so this exception
            // just forces the caller to use startup time instead
            throw new FileNotFoundException();
        }

        try {
            FileInputStream input = new FileInputStream(timeFile);
            String timeString = new String(StreamUtil.slurp(input));
            return Long.parseLong(timeString);
        } catch (Exception e) {
            if (log.isDebugEnabled()) {
                log.warn("Failed to read the shutdown time log file", e);
            } else {
                log.warn("Failed to read the shutdown time log file: " + e.getMessage());
            }
            throw e;
        } finally {
            // since we are starting again, we want to remove the now obsolete shutdown time file
            timeFile.delete();
        }
    }

    private void initializeServer() {
        // Ensure the class is loaded and the dbType is set for our current db
        Connection conn = null;
        try {
            conn = dataSource.getConnection();
            DatabaseTypeFactory.setDefaultDatabaseType(DatabaseTypeFactory.getDatabaseType(conn));
        } catch (Exception e) {
            error += (error.isEmpty() ? "" : ", ") + "server";
            log.error("Could not initialize server.", e);
        } finally {
            if (conn != null) {
                try {
                    conn.close();
                } catch (Exception e) {
                    log.error("Failed to close temporary connection used for server initialization.", e);
                }
            }
        }

        // Ensure that this server is registered in the database.
        createDefaultServerIfNecessary();

        // immediately put the server into MM if configured to do so
        if (ServerCommunicationsServiceUtil.getService().getMaintenanceModeAtStartup()) {
            log.info("Server is configured to start up in MAINTENANCE mode.");
            Server server = serverManager.getServer();
            Integer[] serverId = new Integer[] { server.getId() };
            topologyManager.updateServerManualMaintenance(LookupUtil.getSubjectManager().getOverlord(), serverId, true);
        }

        // Establish the current server mode for the server. This will move the server to NORMAL
        // mode from DOWN if necessary.  This can also affect comm layer behavior.
        serverManager.establishCurrentServerMode();
        if ("true".equals(System.getProperty("rhq.sync.endpoint-address", "false"))) {
            try {
                serverManager.syncEndpointAddress();
            } catch (SyncEndpointAddressException e) {
                log.error("Failed to sync server endpoint address.", e);
            }
        }
    }

    /**
     * For developer builds that don't use the HA installer to write a localhost entry into the {@link Server}
     * table, we will create a default one here.  Then, if the "rhq.high-availability.name" property is missing, the
     * {@link ServerManagerLocal} will return this localhost entry.
     *
     * If the installer was already run, then this method should be a no-op because a row would already exist
     * in the {@link Server} table
     */
    private void createDefaultServerIfNecessary() {
        String identity = serverManager.getIdentity();
        Server server = topologyManager.getServerByName(identity);
        if (server == null) {
            server = new Server();
            server.setName(identity);

            String address;
            try {
                address = InetAddress.getLocalHost().getCanonicalHostName();
            } catch (UnknownHostException e) {
                address = "localhost";
            }
            server.setAddress(address);
            server.setPort(7080);
            server.setSecurePort(7443);
            server.setComputePower(1);
            server.setOperationMode(Server.OperationMode.INSTALLED);
            server.setVersion(this.getClass().getPackage().getImplementationVersion());
            serverManager.create(server);
            log.info("Default HA server created: " + server);
        }
    }

    /**
     * Starts monitoring hibernate by attaching a statistics mbean to the entity manager injected by ejb3.
     *
     * @throws RuntimeException
     */
    private void startHibernateStatistics() throws RuntimeException {
        log.info("Starting hibernate statistics monitoring...");
        try {
            systemManager.enableHibernateStatistics();
        } catch (Exception e) {
            error += (error.isEmpty() ? "" : ", ") + "hibernate statistics";
            throw new RuntimeException("Cannot start hibernate statistics monitoring!", e);
        }
    }

    /**
     * Starts the plugin deployer which will effectively ask the plugin deployer to persist information about all
     * detected agent and server plugins.
     *
     * Because this will scan and register the initial plugins right now, make sure this is called prior
     * to starting the master plugin container; otherwise, the master PC will not have any plugins to start.
     *
     * @throws RuntimeException
     */
    private void startPluginDeployer() throws RuntimeException {
        log.info("Starting the agent/server plugin deployer...");
        try {
            PluginDeploymentScannerMBean deployer = getPluginDeploymentScanner();
            deployer.startDeployment();
        } catch (Exception e) {
            error += (error.isEmpty() ? "" : ", ") + "plugin deployer";
            throw new RuntimeException("Cannot start the agent/server plugin deployer!", e);
        }
    }

    /**
     * Creates the timer that will trigger periodic scans for new plugins.
     * @throws RuntimeException
     */
    private void registerPluginDeploymentScannerJob() throws RuntimeException {
        log.info("Creating timer to begin scanning for plugins...");

        try {
            PluginDeploymentScannerMBean deployer = getPluginDeploymentScanner();

            long scanPeriod = 5 * 60000L;
            try {
                String scanPeriodString = deployer.getScanPeriod();
                scanPeriod = Long.parseLong(scanPeriodString);
            } catch (Exception e) {
                log.warn("could not determine plugin scanner scan period - using: " + scanPeriod, e);
            }

            // create a non-persistent periodic timer (we'll reset it ever startup) with the scan period as configured in our scanner object
            timerService.createIntervalTimer(scanPeriod, scanPeriod, new TimerConfig(null, false));
        } catch (Exception e) {
            error += (error.isEmpty() ? "" : ", ") + "plugin scanner";
            throw new RuntimeException("Cannot schedule plugin scanning timer - new plugins will not be detected!", e);
        }
    }

    @Timeout
    public void scanForPlugins(final Timer timer) {
        try {
            PluginDeploymentScannerMBean deployer = getPluginDeploymentScanner();
            deployer.scanAndRegister();
        } catch (Throwable t) {
            log.error("Plugin scan failed. Cause: " + ThrowableUtil.getAllMessages(t));
            if (log.isDebugEnabled()) {
                log.debug("Plugin scan failure stack trace follows:", t);
            }
        }
    }

    private PluginDeploymentScannerMBean getPluginDeploymentScanner() {
        PluginDeploymentScannerMBean deployer;
        MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
        ObjectName name = PluginDeploymentScannerMBean.OBJECT_NAME;
        Class<?> iface = PluginDeploymentScannerMBean.class;
        deployer = (PluginDeploymentScannerMBean) MBeanServerInvocationHandler
            .newProxyInstance(mbs, name, iface, false);
        return deployer;
    }

    /**
     * Installs the JAAS login modules so our users can login.
     *
     * @throws RuntimeException
     */
    private void upgradeRhqUserSecurityDomainIfNeeded() throws RuntimeException {

        try {
            CustomJaasDeploymentServiceMBean jaas_mbean;
            MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
            ObjectName name = CustomJaasDeploymentServiceMBean.OBJECT_NAME;
            Class<?> iface = CustomJaasDeploymentServiceMBean.class;
            jaas_mbean = (CustomJaasDeploymentServiceMBean) MBeanServerInvocationHandler.newProxyInstance(mbs, name,
                iface, false);
            jaas_mbean.upgradeRhqUserSecurityDomainIfNeeded();
        } catch (Exception e) {
            error += (error.isEmpty() ? "" : ", ") + "security domain upgrade";
            throw new RuntimeException("Cannot upgrade JAAS login modules!", e);
        }
    }

    /**
     * Initializes, but doesn't start, the Quartz scheduler now.
     *
     * @throws RuntimeException
     */
    private void initScheduler() throws RuntimeException {
        log.info("Initializing the scheduler....");

        try {
            schedulerBean.initQuartzScheduler();
        } catch (SchedulerException e) {
            error += (error.isEmpty() ? "" : ", ") + "scheduler initialization";
            throw new RuntimeException("Cannot initialize the scheduler!", e);
        }
    }

    /**
     * Initializes the storage client subsystem which is needed for reading/writing metric data.
     *
     * @return true if the storage subsystem is running
     */
    private boolean initStorageClient() {
        boolean isStorageRunning = storageClientManager.init();
        if (!isStorageRunning) {
            error += (error.isEmpty() ? "" : ", ") + "storage";
        }
        return isStorageRunning;
    }

    /**
     * Starts the Quartz scheduler now. We are assured that all EJBs are deployed now, so any jobs that have to be
     * executed now will have those EJBs available.
     *
     * @throws RuntimeException
     */
    private void startScheduler() throws RuntimeException {
        log.info("Starting the scheduler...");

        try {
            schedulerBean.startQuartzScheduler();
        } catch (SchedulerException e) {
            error += (error.isEmpty() ? "" : ", ") + "scheduler";
            throw new RuntimeException("Cannot start the scheduler!", e);
        }
    }

    /**
     * Initializes the server-side communications services. Once complete, agents can talk to the server.
     *
     * @throws RuntimeException
     */
    private void startServerCommunicationServices() throws RuntimeException {

        // under a rare case, if the server starts up really fast as soon as it dies, any connected
        // agents will not realize the server has bounced and will not know to re-connect. When this
        // happens the server's caches will not be refreshed and bad things will happen (e.g. alerts not firing).
        // make sure we are down for a certain amount of time to ensure the agent's know the server was down.
        long ensureDownTimeSecs;
        try {
            ensureDownTimeSecs = Long.parseLong(System.getProperty("rhq.server.ensure-down-time-secs", "70"));
        } catch (Exception e) {
            ensureDownTimeSecs = 70;
        }
        long elapsed = getElapsedTimeSinceLastShutdown();
        long sleepTime = (ensureDownTimeSecs * 1000L) - elapsed;
        if (sleepTime > 0) {
            try {
                log.info("Forcing the server to wait [" + sleepTime + "]ms to ensure agents know we went down...");
                Thread.sleep(sleepTime);
            } catch (InterruptedException ignore) {
            }
        }

        // now start our comm layer
        log.info("Starting the server-agent communications services...");

        try {
            ServerCommunicationsServiceUtil.getService().startCommunicationServices();
            ServerCommunicationsServiceUtil
                .getService()
                .getServiceContainer()
                .addCommandListener(
                    new ExternalizableStrategyCommandListener(
                        org.rhq.core.domain.server.ExternalizableStrategy.Subsystem.AGENT));
        } catch (Exception e) {
            error += (error.isEmpty() ? "" : ", ") + "communications services";
            throw new RuntimeException("Cannot start the server-side communications services.", e);
        }
    }

    /**
      * This seeds the agent clients cache with clients for all known agents. These clients will be started so they can
      * immediately begin to send any persisted guaranteed messages that might already exist. This method must be called
      * at a time when the server is ready to accept messages from agents because any guaranteed messages that are
      * delivered might trigger the agents to send messages back to the server.
      *
      * NOTE: we don't need to do this - so far, none of the messages the server sends to the agent are marked
      * with "guaranteed delivery" (this is on purpose and a good thing) so we don't need to start all the agent clients
      * in case they have persisted messages. Since the number of agents could be large this cache could be huge and
      * take some time to initialize. If we don't call this, it speeds up start up, and doesn't bloat memory with
      * clients we might not ever need (since agents might have affinity to other servers). Agent clients
      * can be created lazily at runtime when the server needs it.
      */
    private void startAgentClients() {
        log.info("Starting agent clients - any persisted messages with guaranteed delivery will be sent...");

        List<Agent> agents = agentManager.getAllAgents();

        if (agents != null) {
            for (Agent agent : agents) {
                agentManager.getAgentClient(agent); // this caches and starts the client
            }
        }

        return;
    }

    /**
     * This will make sure all jobs that need to periodically run are scheduled.
     *
     * @throws RuntimeException if unable to schedule a job
     */
    private void scheduleJobs() throws RuntimeException {
        log.info("Scheduling asynchronous jobs...");

        /*
         * All jobs need to be set as non-volatile since a volatile job in a clustered environment is effectively
         * non-volatile;
         */

        // TODO [mazz]: make all of the intervals here configurable via something like SystemManagerBean

        serverManager.scheduleServerHeartbeat();
        cacheConsistencyManager.scheduleServerCacheReloader();
        systemManager.scheduleConfigCacheReloader();
        subjectManager.scheduleSessionPurgeJob();
        storageClientManager.scheduleStorageSessionMaintenance();

        try {
            // Do not check until we are up at least 1 min, and every minute thereafter.
            final long initialDelay = 1000L * 60;
            final long interval = 1000L * 60;
            schedulerBean.scheduleSimpleRepeatingJob(SavedSearchResultCountRecalculationJob.class, true, false,
                initialDelay, interval);
        } catch (Exception e) {
            log.error("Cannot schedule asynchronous resource deletion job.", e);
        }

        try {
            // Do not check until we are up at least 1 min, and every 5 minutes thereafter.
            final long initialDelay = 1000L * 60;
            final long interval = 1000L * 60 * 5;
            schedulerBean.scheduleSimpleRepeatingJob(AsyncResourceDeleteJob.class, true, false, initialDelay, interval);
        } catch (Exception e) {
            log.error("Cannot schedule asynchronous resource deletion job.", e);
        }

        try {
            // Do not check until we are up at least 1 min, and every 5 minutes thereafter.
            final long initialDelay = 1000L * 60;
            final long interval = 1000L * 60 * 5;
            schedulerBean.scheduleSimpleRepeatingJob(PurgeResourceTypesJob.class, true, false, initialDelay, interval);
        } catch (Exception e) {
            log.error("Cannot schedule purge resource types job.", e);
        }

        try {
            // Do not check until we are up at least 1 min, and every 3 minutes thereafter.
            final long initialDelay = 1000L * 60;
            final long interval = 1000L * 60 * 3;
            schedulerBean.scheduleSimpleRepeatingJob(PurgePluginsJob.class, true, false, initialDelay, interval);
        } catch (Exception e) {
            log.error("Cannot schedule purge plugins job.", e);
        }

        // DynaGroup Auto-Recalculation Job
        try {
            // Do not check until we are up at least 1 min, and every minute thereafter.
            final long initialDelay = 1000L * 60;
            final long interval = 1000L * 60;
            schedulerBean.scheduleSimpleRepeatingJob(DynaGroupAutoRecalculationJob.class, true, false, initialDelay,
                interval);
        } catch (Exception e) {
            log.error("Cannot schedule DynaGroup auto-recalculation job.", e);
        }

        // Cluster Manager Job
        try {
            String oldJobName = "org.rhq.enterprise.server.scheduler.jobs.ClusterManagerJob";
            boolean foundAndDeleted = schedulerBean.deleteJob(oldJobName, oldJobName);
            if (foundAndDeleted) {
                log.info("Unscheduling deprecated job references for " + oldJobName + "...");
            } else {
                log.debug("No deprecated job references found for " + oldJobName + ".");
            }

            // Wait long enough to allow the Server instance jobs to start executing first.
            final long initialDelay = 1000L * 60 * 2; // 2 mins
            final long interval = 1000L * 30; // 30 secs
            schedulerBean.scheduleSimpleRepeatingJob(CloudManagerJob.class, true, false, initialDelay, interval);
        } catch (Exception e) {
            log.error("Cannot schedule cloud management job.", e);
        }

        // Suspected Agents Job
        try {
            // Do not check until we are up at least 10 mins, but check every 60 secs thereafter.
            final long initialDelay = 1000L * 60 * 10; // 10 mins
            final long interval = 1000L * 60; // 60 secs
            schedulerBean.scheduleSimpleRepeatingJob(CheckForSuspectedAgentsJob.class, true, false, initialDelay,
                interval);
        } catch (Exception e) {
            log.error("Cannot schedule suspected Agents job.", e);
        }

        // Timed Out Operations Job
        try {
            final long initialDelay = 1000L * 60 * 3; // 3 min
            final long interval = 1000L * 60 * 10; // 10 minutes
            schedulerBean.scheduleSimpleRepeatingJob(CheckForTimedOutOperationsJob.class, true, false, initialDelay,
                interval);
        } catch (Exception e) {
            log.error("Cannot schedule check-for-timed-out-operations job.", e);
        }

        // Timed Out Resource Configuration Update Requests Job
        // (NOTE: We don't need to check for timed out plugin Cofiguration updates, since those are executed synchronously.)
        try {
            final long initialDelay = 1000L * 60 * 4; // 4 mins
            final long interval = 1000L * 60 * 10; // 10 mins
            schedulerBean.scheduleSimpleRepeatingJob(CheckForTimedOutConfigUpdatesJob.class, true, false, initialDelay,
                interval);
        } catch (Exception e) {
            log.error("Cannot schedule check-for-timed-out-configuration-update-requests job.", e);
        }

        // Timed Out Content Requests Job
        try {
            final long initialDelay = 1000L * 60 * 5; // 5 mins
            final long interval = 1000L * 60 * 15; // 15 mins
            schedulerBean.scheduleSimpleRepeatingJob(CheckForTimedOutContentRequestsJob.class, true, false,
                initialDelay, interval);
        } catch (Exception e) {
            log.error("Cannot schedule check-for-timed-out-artifact-requests job.", e);
        }

        // Data Purge Job
        try {
            // TODO [mazz]: make the data purge job's cron string configurable via SystemManagerBean
            // For Quartz cron syntax, see: http://www.quartz-scheduler.org/documentation/quartz-2.1.x/tutorials/crontrigger
            String cronString = "0 30 * * * ?"; // every hour, on the half-hour (to offset from DataCalcJob)
            schedulerBean.scheduleSimpleCronJob(DataPurgeJob.class, true, false, cronString,
                CronTrigger.MISFIRE_INSTRUCTION_DO_NOTHING);
        } catch (Exception e) {
            log.error("Cannot schedule data purge job.", e);
        }

        // Data Calc Job
        try {
            // TODO [mazz]: make the data calc job's cron string configurable via SystemManagerBean
            // For Quartz cron syntax, see: http://www.quartz-scheduler.org/documentation/quartz-2.1.x/tutorials/crontrigger
            String cronString = "0 0 * * * ?"; // every hour, on the hour
            schedulerBean.scheduleSimpleCronJob(DataCalcJob.class, true, false, cronString,
                CronTrigger.MISFIRE_INSTRUCTION_DO_NOTHING);
        } catch (Exception e) {
            log.error("Cannot schedule data calc job.", e);
        }

        // Server Plugin Jobs
        try {
            ServerPluginServiceMBean mbean = LookupUtil.getServerPluginService();
            MasterServerPluginContainer masterPC = mbean.getMasterPluginContainer();
            masterPC.scheduleAllPluginJobs();
        } catch (Exception e) {
            log.error("Cannot schedule server plugin jobs.", e);
        }

        try {
            // Wait long enough to allow the Server instance jobs to start executing first.
            String cronString = "0 30 0 ? * SUN *"; // every sunday starting at 00:30.
            schedulerBean.scheduleSimpleCronJob(StorageClusterReadRepairJob.class, true, true, cronString, null);
        } catch (Exception e) {
            log.error("Cannot create storage cluster read repair job", e);
        }

        // Storage cluster replication factor check Job
        try {
            final long initialDelay = 1000L * 60 * 2; // 2 mins
            final long interval = 1000L * 60 * 5; // 5 mins
            schedulerBean.scheduleSimpleRepeatingJob(ReplicationFactorCheckJob.class, true, false, initialDelay,
                interval);
        } catch (Exception e) {
            log.error("Cannot schedule Storage cluster replication factor check job.", e);
        }
    }

    /**
     * Starts the embedded agent, but only if the embedded agent is installed and it is enabled.
     *
     * @throws RuntimeException if the agent is installed and enabled but failed to start
     *
     * @deprecated we don't have an embedded agent anymore, leaving this in case we resurrect it
     */
    @Deprecated
    private void startEmbeddedAgent() throws RuntimeException {
        // we can't use EmbeddedAgentBootstrapServiceMBean because if the embedded agent
        // isn't installed, that class will not be available; we must use JMX API
        final ObjectName agentBootstrapMBean = ObjectNameFactory.create("rhq:service=EmbeddedAgentBootstrap");
        final String agentEnabledAttribute = "AgentEnabled";
        final String startAgentMethod = "startAgent";
        final String configurationOverridesAttribute = "ConfigurationOverrides";
        final MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();

        try {
            // this will fail if the embedded agent isn't installed
            String enabled = (String) mbs.getAttribute(agentBootstrapMBean, agentEnabledAttribute);

            // if we got this far, the embedded agent is at least installed
            // now check to see if its enabled - if so start it; any startup exceptions now are thrown
            try {
                if (Boolean.valueOf(enabled)) {
                    log.info("The embedded Agent is installed and enabled - it will now be started...");

                    // NOTE: we cannot directly import AgentConfigurationConstants, so we hardcode the
                    // actual constant values here - need to keep an eye on these in the unlikely event
                    // the constant values change.
                    String AgentConfigurationConstants_SERVER_TRANSPORT = "rhq.agent.server.transport";
                    String AgentConfigurationConstants_SERVER_BIND_ADDRESS = "rhq.agent.server.bind-address";
                    String AgentConfigurationConstants_SERVER_BIND_PORT = "rhq.agent.server.bind-port";

                    // Get the configuration overrides as set in the configuration file.
                    // If the agent's bind address isn't overridden with a non-empty value,
                    // then we need to get the Server bind address and use it for the agent's bind address.
                    // If the agent's server endpoint address/port are empty, we again use the values
                    // appropriate for the Server this agent is embedded in.
                    // Note that we don't look for the values in persisted preferences - we assume they
                    // are always present in the configuration overrides (which they should always be);
                    Properties overrides;
                    String serverTransport;
                    String serverAddress;
                    String serverPort;
                    String agentAddress;

                    overrides = (Properties) mbs.getAttribute(agentBootstrapMBean, configurationOverridesAttribute);

                    serverTransport = overrides.getProperty(AgentConfigurationConstants_SERVER_TRANSPORT);
                    serverAddress = overrides.getProperty(AgentConfigurationConstants_SERVER_BIND_ADDRESS);
                    serverPort = overrides.getProperty(AgentConfigurationConstants_SERVER_BIND_PORT);
                    agentAddress = overrides.getProperty(ServiceContainerConfigurationConstants.CONNECTOR_BIND_ADDRESS);

                    Server server = serverManager.getServer();

                    if (agentAddress == null || agentAddress.trim().equals("")) {
                        overrides.setProperty(ServiceContainerConfigurationConstants.CONNECTOR_BIND_ADDRESS,
                            server.getAddress());
                    }
                    if (serverAddress == null || serverAddress.trim().equals("")) {
                        overrides.setProperty(AgentConfigurationConstants_SERVER_BIND_ADDRESS, server.getAddress());
                    }
                    if (serverPort == null || serverPort.trim().equals("")) {
                        if (SecurityUtil.isTransportSecure(serverTransport)) {
                            overrides.setProperty(AgentConfigurationConstants_SERVER_BIND_PORT,
                                Integer.toString(server.getSecurePort()));
                        } else {
                            overrides.setProperty(AgentConfigurationConstants_SERVER_BIND_PORT,
                                Integer.toString(server.getPort()));
                        }
                    }

                    mbs.setAttribute(agentBootstrapMBean, new Attribute(configurationOverridesAttribute, overrides));

                    // We need to do the agent startup in a separate thread so we do not hang
                    // this startup servlet.  JBossAS 4.2 will not begin accepting HTTP requests
                    // until this startup servlet has finished (this is different from JBossAS 4.0).
                    // The agent needs to submit an HTTP request in order to complete its startup
                    // (it needs to register with the server).
                    // The side effect of this is the RHQ Server will still start even if the embedded
                    // agent fails to start - this may not be a bad thing.  We probably do not want
                    // the entire RHQ Server to go down if its agent fails to start.
                    Runnable agentStartRunnable = new Runnable() {
                        public void run() {
                            // this returns only when the agent has started and is registered (sends HTTP request)
                            try {
                                mbs.invoke(agentBootstrapMBean, startAgentMethod, new Object[0], new String[0]);
                            } catch (Throwable t) {
                                log.error("Failed to start the embedded Agent - it will not be available!", t);
                            }
                        }
                    };

                    Thread agentStartThread = new Thread(agentStartRunnable, "Embedded Agent Startup");
                    agentStartThread.setDaemon(true);
                    agentStartThread.start();
                } else {
                    log.debug("The embedded Agent is not enabled, so it will not be started.");
                }
            } catch (Throwable t) {
                throw new RuntimeException("Failed to start the embedded Agent.", t);
            }
        } catch (RuntimeException se) {
            throw se;
        } catch (Throwable t) {
            log.info("The embedded Agent is not installed, so it will not be started (" + t + ").");
        }

        return;
    }

    /**
     * Starts the server-side plugin container.
     *
     * @throws RuntimeException
     */
    private void startServerPluginContainer() throws RuntimeException {
        log.info("Starting the master server plugin container...");

        try {
            ServerPluginServiceMBean mbean = LookupUtil.getServerPluginService();
            mbean.startMasterPluginContainerWithoutSchedulingJobs();
        } catch (Exception e) {
            error += (error.isEmpty() ? "" : ", ") + "server plugin container";
            throw new RuntimeException("Cannot start the master server plugin container!", e);
        }
    }

    /**
     * Registers a listener to the system shutdown notification so some components can be cleaned up in an
     * orderly fashion when the server is shutdown.
     *
     * @throws RuntimeException if cannot register a shutdown listener
     */
    private void registerShutdownListener() throws RuntimeException {
        // as of JBossAS 4.0.5, this is the known MBean name of the service that notifies when the server is shutting down
        // AS7 today does not have notifications like this. So we have a new EJB singleton ShutdownListener with a PreDestroy method.
        // If that doesn't work, we can try to create a system shutdown hook in here. Thus I'm leaving this method in here in case
        // we need it later. Just add a Runtime.addShutdownHook call in here that calls our ShutdownListener.
        return;
    }

    /**
     * Gets the number of milliseconds since the time when the server was last shutdown.
     * If we don't know, then return the time since it was started.
     * @return elapsed time since server started, 0 if not known
     */
    private long getElapsedTimeSinceLastShutdown() throws RuntimeException {
        long elapsed;

        try {
            long shutdownTime = readShutdownTimeLogFile();
            long currentTime = System.currentTimeMillis();
            elapsed = currentTime - shutdownTime;
        } catch (Exception ignore) {
            // we will have already logged an error, don't bother logging more
            // but now at least try to see how long its been since we've started
            try {
                CoreServerMBean coreServer = LookupUtil.getCoreServer();
                Date startTime = coreServer.getBootTime();
                long currentTime = System.currentTimeMillis();
                elapsed = currentTime - startTime.getTime();
            } catch (Exception e1) {
                elapsed = 0;
            }
        }

        return elapsed;
    }

    private void logServerStartedMessage() {
        Subject overlord = subjectManager.getOverlord();
        ProductInfo productInfo = systemManager.getProductInfo(overlord);
        log.info("--------------------------------------------------"); // 50 dashes
        log.info(productInfo.getFullName() + " " + productInfo.getVersion() + " (build " + productInfo.getBuildNumber()
            + ") Server started.");
        log.info("--------------------------------------------------"); // 50 dashes
    }
}
