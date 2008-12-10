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
package org.rhq.enterprise.gui.startup;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Properties;

import javax.management.MBeanServer;
import javax.management.MBeanServerInvocationHandler;
import javax.management.ObjectName;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;

import org.quartz.SchedulerException;

import org.jboss.mx.util.MBeanServerLocator;

import org.rhq.core.domain.cluster.Server;
import org.rhq.core.domain.resource.Agent;
import org.rhq.core.util.ObjectNameFactory;
import org.rhq.enterprise.server.cluster.instance.ServerManagerLocal;
import org.rhq.enterprise.server.core.AgentManagerLocal;
import org.rhq.enterprise.server.core.CustomJaasDeploymentServiceMBean;
import org.rhq.enterprise.server.core.comm.ServerCommunicationsServiceUtil;
import org.rhq.enterprise.server.core.plugin.ProductPluginDeployerMBean;
import org.rhq.enterprise.server.plugin.content.ContentSourcePluginServiceManagement;
import org.rhq.enterprise.server.scheduler.SchedulerLocal;
import org.rhq.enterprise.server.scheduler.jobs.AutoBaselineCalculationJob;
import org.rhq.enterprise.server.scheduler.jobs.CheckForSuspectedAgentsJob;
import org.rhq.enterprise.server.scheduler.jobs.CheckForTimedOutConfigUpdatesJob;
import org.rhq.enterprise.server.scheduler.jobs.CheckForTimedOutContentRequestsJob;
import org.rhq.enterprise.server.scheduler.jobs.CheckForTimedOutOperationsJob;
import org.rhq.enterprise.server.scheduler.jobs.ClusterManagerJob;
import org.rhq.enterprise.server.scheduler.jobs.DataPurgeJob;
import org.rhq.enterprise.server.system.SystemManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * This servlet is ensured to be initialized after the rest of the RHQ Server has been deployed and started.
 * Specifically, we know that at {@link #init()} time, all EJBs have been deployed and available.
 */
public class StartupServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    /**
     * Performs the final RHQ Server initialization work that needs to talk place. EJBs are available in this method.
     *
     * @throws ServletException
     */
    @Override
    public void init() throws ServletException {
        log("All business tier deployments are complete - finishing the startup");

        // As a security measure, make sure the installer has been undeployed
        LookupUtil.getSystemManager().undeployInstaller();

        // Before starting determine the operating mode of this server and
        // take any necessary initialization action. Must happen before comm startup since listeners
        // may be added.
        initializeServer();

        // The order here is important - make sure if you change this you know what you are doing.
        // I'm not even sure this is ok.  If we start the scheduler before the comm layer, what happens
        // if a stored job needs to send a message?  But if we start the comm layer before the scheduler,
        // what happens if a message is received that needs a job scheduled for it? I think the former
        // is more likely to happen than the latter (that is, a scheduled job would more likely need
        // to send a message; as opposed to an incoming message causing a job to be scheduled), so
        // that explains the ordering of the comm layer and the scheduler.
        startHibernateStatistics();
        startServerPluginContainer(); // before comm in case an agent wants to talk to it
        installJaasModules();
        startServerCommunicationServices();
        startScheduler();
        scheduleJobs();
        startAgentClients();
        startPluginDeployer();
        startEmbeddedAgent();
        registerShutdownListener();
        registerBootTime();

        return;
    }

    private void initializeServer() {
        // Ensure that this server is registered in the database.
        createDefaultServerIfNecessary();

        // Establish the current server mode for the server. This will move the server to NORMAL
        // mode from DOWN if necessary.  This can also affect comm layer behavior.
        LookupUtil.getServerManager().establishCurrentServerMode();
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
        String identity = LookupUtil.getServerManager().getIdentity();
        Server server = LookupUtil.getClusterManager().getServerByName(identity);
        if (server == null) {
            server = new Server();
            server.setName(identity);

            String address = "localhost";
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
            LookupUtil.getServerManager().create(server);
            log("Default server created: " + server);
        }
    }

    /**
     * Register the time when the server was booted
     */
    private void registerBootTime() {
        // TODO Auto-generated method stub
        SystemManagerLocal sysMan = LookupUtil.getSystemManager();
        Properties conf = sysMan.getSystemConfiguration();
        Date now = new Date();
        String nowS = new SimpleDateFormat("yy-MM-dd hh:mm:ss").format(now);
        conf.setProperty("LAST_BOOT_TIME", nowS);
        sysMan.setSystemConfiguration(LookupUtil.getSubjectManager().getOverlord(), conf);
    }

    /**
     * Starts monitoring hibernate by attaching a statistics mbean to the entity manager injected by ejb3.
     *
     * @throws ServletException
     */
    private void startHibernateStatistics() throws ServletException {
        log("Starting hibernate statistics monitoring");
        try {
            LookupUtil.getSystemManager().enableHibernateStatistics();
        } catch (Exception e) {
            throw new ServletException("Cannot start hibernate statistics monitoring", e);
        }
    }

    /**
     * Starts the plugin deployer which will effectively ask the plugin deployer to persist information about all
     * detected plugins.
     *
     * @throws ServletException
     */
    private void startPluginDeployer() throws ServletException {
        log("Starting the plugin deployer");

        try {
            ProductPluginDeployerMBean deployer_mbean;
            MBeanServer mbs = MBeanServerLocator.locateJBoss();
            ObjectName name = ProductPluginDeployerMBean.OBJECT_NAME;
            Class<?> iface = ProductPluginDeployerMBean.class;
            deployer_mbean = (ProductPluginDeployerMBean) MBeanServerInvocationHandler.newProxyInstance(mbs, name,
                iface, false);
            deployer_mbean.startDeployer();
        } catch (Exception e) {
            throw new ServletException("Cannot start the plugin deployer", e);
        }
    }

    /**
     * Installs the JAAS login modules so our users can login.
     *
     * @throws ServletException
     */
    private void installJaasModules() throws ServletException {
        log("Installing JAAS Modules");

        try {
            CustomJaasDeploymentServiceMBean jaas_mbean;
            MBeanServer mbs = MBeanServerLocator.locateJBoss();
            ObjectName name = CustomJaasDeploymentServiceMBean.OBJECT_NAME;
            Class<?> iface = CustomJaasDeploymentServiceMBean.class;
            jaas_mbean = (CustomJaasDeploymentServiceMBean) MBeanServerInvocationHandler.newProxyInstance(mbs, name,
                iface, false);
            jaas_mbean.installJaasModules();
        } catch (Exception e) {
            throw new ServletException("Cannot deploy our JAAS login modules!", e);
        }
    }

    /**
     * Starts the Quartz scheduler now. We are assured that all EJBs are deployed now, so any jobs that have to be
     * executed now will have those EJBs available.
     *
     * @throws ServletException
     */
    private void startScheduler() throws ServletException {
        log("Starting the scheduler");

        try {
            LookupUtil.getSchedulerBean().startQuartzScheduler();
        } catch (SchedulerException e) {
            throw new ServletException("Cannot start the scheduler!", e);
        }
    }

    /**
     * Initializes the server-side communications services. Once complete, agents can talk to the server.
     *
     * @throws ServletException
     */
    private void startServerCommunicationServices() throws ServletException {
        log("Starting the server-agent communications services");

        try {
            ServerCommunicationsServiceUtil.getService().startCommunicationServices();
        } catch (Exception e) {
            throw new ServletException("Cannot start the server-side communications services", e);
        }
    }

    /**
     * This will make sure all jobs that need to periodically run are scheduled.
     *
     * @throws ServletException if unable to schedule a job
     */
    private void scheduleJobs() throws ServletException {
        log("Scheduling some jobs that need to be run");

        /*
         * All jobs need to be set as non-volatile since a volatile job in a clustered environment is effectively
         * non-volatile;
         */

        SchedulerLocal scheduler = LookupUtil.getSchedulerBean();

        // TODO [mazz]: make all of the intervals here configurable via something like SystemManagerBean

        LookupUtil.getServerManager().scheduleServerHeartbeat();
        LookupUtil.getCacheConsistenyManager().scheduleServerCacheReloader();

        // Cluster Manager Job
        try {

            // Long enough to allow the server instance jobs to start executing first.
            final long initialDelay = 1000L * 60 * 2;
            final long interval = 1000L * 30;
            scheduler.scheduleSimpleRepeatingJob(ClusterManagerJob.class, true, false, initialDelay, interval);
        } catch (Exception e) {
            throw new ServletException("Cannot schedule cluster management job", e);
        }

        // Suspected Agents Job
        try {
            // do not check until we are up at least 10mins, but check every 30s thereafter
            final long initialDelay = 1000L * 60 * 10;
            final long interval = 1000L * 30;
            scheduler.scheduleSimpleRepeatingJob(CheckForSuspectedAgentsJob.class, true, false, initialDelay, interval);
        } catch (Exception e) {
            throw new ServletException("Cannot schedule suspected agents job", e);
        }

        // Check for Timed Out Operations
        try {
            scheduler.scheduleSimpleRepeatingJob(CheckForTimedOutOperationsJob.class, true, false, 20000L, 60000L);
        } catch (Exception e) {
            throw new ServletException("Cannot schedule check-for-timed-out-operations job", e);
        }

        // Check for Timed Out Configuration Update Requests
        try {
            scheduler.scheduleSimpleRepeatingJob(CheckForTimedOutConfigUpdatesJob.class, true, false, 30000L, 60000L);
        } catch (Exception e) {
            throw new ServletException("Cannot schedule check-for-timed-out-configuration-update-requests job", e);
        }

        // Check for Timed Out Content requests
        try {
            scheduler.scheduleSimpleRepeatingJob(CheckForTimedOutContentRequestsJob.class, true, false, 40000L, 60000L);
        } catch (Exception e) {
            throw new ServletException("Cannot schedule check-for-timed-out-artifact-requests job", e);
        }

        // Data Purge Job
        try {
            // Let's leave this non-volatile - in case we fail to schedule any jobs above, then at least
            // our old schedule from a previous run will still be there - we want to be able to make sure we run
            // db maintenance when the server starts.
            // TODO [mazz]: make the data purge job's cron string configurable via SystemManagerBean
            // For Quartz cron syntax, see: http://www.opensymphony.com/quartz/wikidocs/CronTriggers%20Tutorial.html
            String cronString = "0 0 * * * ?"; // every hour, on the hour
            scheduler.scheduleSimpleCronJob(DataPurgeJob.class, true, false, cronString);
        } catch (Exception e) {
            throw new ServletException("Cannot schedule data purge job", e);
        }

        // Baseline calculation Job
        try {
            // Check the need to calculate baselines every 77 minutes
            scheduler.scheduleSimpleRepeatingJob(AutoBaselineCalculationJob.class, true, false, 11 * 60 * 1000L,
                77 * 60 * 1000L);
        } catch (Exception e) {
            throw new ServletException("Cannot schedule baseline calculation job", e);
        }

        // Content source sync jobs
        try {
            ContentSourcePluginServiceManagement mbean;
            MBeanServer mbs = MBeanServerLocator.locateJBoss();
            ObjectName name = ObjectNameFactory.create(ContentSourcePluginServiceManagement.OBJECT_NAME_STR);
            Class<?> iface = ContentSourcePluginServiceManagement.class;
            mbean = (ContentSourcePluginServiceManagement) MBeanServerInvocationHandler.newProxyInstance(mbs, name,
                iface, false);
            mbean.getPluginContainer().scheduleSyncJobs();
        } catch (Exception e) {
            throw new ServletException("Cannot schedule content source sync jobs!", e);
        }
    }

    /**
     * This seeds the agent clients cache with clients for all known agents. These clients will be started so they can
     * immediately begin to send any persisted guaranteed messages that might already exist. This method must be called
     * at a time when the server is ready to accept messages from agents because any guaranteed messages that are
     * delivered might trigger the agents to send messages back to the server.
     */
    private void startAgentClients() {
        log("Starting agent clients - any persisted messages with guaranteed delivery will be sent");

        AgentManagerLocal agentManager = LookupUtil.getAgentManager();
        List<Agent> agents = agentManager.getAllAgents();

        if (agents != null) {
            for (Agent agent : agents) {
                agentManager.getAgentClient(agent); // this caches and starts the client
            }
        }

        return;
    }

    /**
     * Starts the embedded agent, but only if the embedded agent is installed and it is enabled.
     *
     * @throws ServletException if the agent is installed and enabled but failed to start
     */
    private void startEmbeddedAgent() throws ServletException {
        // we can't use EmbeddedAgentBootstrapServiceMBean because if the embedded agent
        // isn't installed, that class will not be available; we must use JMX API
        final ObjectName agentBootstrapMBean = ObjectNameFactory.create("rhq:service=EmbeddedAgentBootstrap");
        final String agentEnabledAttribute = "AgentEnabled";
        final String startAgentMethod = "startAgent";
        final MBeanServer mbs = MBeanServerLocator.locateJBoss();

        try {
            // this will fail if the embedded agent isn't installed
            String enabled = (String) mbs.getAttribute(agentBootstrapMBean, agentEnabledAttribute);

            // if we got this far, the embedded agent is at least installed
            // now check to see if its enabled - if so start it; any startup exceptions now are thrown
            try {
                if (Boolean.valueOf(enabled)) {
                    log("The embedded Agent is installed and enabled - it will now be started...");

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
                                log("Failed to start the embedded Agent - it will not be available!", t);
                            }
                        }
                    };

                    Thread agentStartThread = new Thread(agentStartRunnable, "Embedded Agent Startup");
                    agentStartThread.setDaemon(true);
                    agentStartThread.start();
                } else {
                    log("The embedded Agent is not enabled, so it will not be started.");
                }
            } catch (Throwable t) {
                throw new ServletException("Failed to start the embedded Agent.", t);
            }
        } catch (ServletException se) {
            throw se;
        } catch (Throwable t) {
            log("The embedded Agent is not installed, so it will not be started (" + t + ").");
        }

        return;
    }

    /**
     * Starts the server-side plugin container.
     *
     * @throws ServletException
     */
    private void startServerPluginContainer() throws ServletException {
        log("Starting the server plugin container...");

        try {
            ContentSourcePluginServiceManagement mbean;
            MBeanServer mbs = MBeanServerLocator.locateJBoss();
            ObjectName name = ObjectNameFactory.create(ContentSourcePluginServiceManagement.OBJECT_NAME_STR);
            Class<?> iface = ContentSourcePluginServiceManagement.class;
            mbean = (ContentSourcePluginServiceManagement) MBeanServerInvocationHandler.newProxyInstance(mbs, name,
                iface, false);
            mbean.startPluginContainer();
        } catch (Exception e) {
            throw new ServletException("Cannot start the server plugin container!", e);
        }
    }

    /**
     * Registers a listener to the JBoss server's shutdown notification so some components can be cleaned up in an
     * orderly fashion when the server is shutdown.
     *
     * @throws ServletException if cannot register this service as a shutdown listener
     */
    private void registerShutdownListener() throws ServletException {
        // as of JBossAS 4.0.5, this is the known MBean name of the service that notifies when the server is shutting down
        try {
            ObjectName jbossServerName = new ObjectName("jboss.system:type=Server");
            MBeanServer jbossServer = MBeanServerLocator.locateJBoss();
            jbossServer.addNotificationListener(jbossServerName, new ShutdownListener(), null, null);
        } catch (Exception e) {
            throw new ServletException("Failed to register the Server Shutdown Listener", e);
        }
    }
}
