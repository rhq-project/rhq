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
package org.rhq.enterprise.server.test;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Hashtable;
import java.util.Properties;

import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.transaction.TransactionManager;

import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.testng.AssertJUnit;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeSuite;

import org.jboss.ejb3.embedded.EJB3StandaloneBootstrap;
import org.jboss.ejb3.embedded.EJB3StandaloneDeployer;
import org.jboss.mx.util.MBeanServerLocator;

import org.rhq.core.db.DatabaseTypeFactory;
import org.rhq.core.db.PostgresqlDatabaseType;
import org.rhq.core.domain.auth.Subject;
import org.rhq.core.server.PersistenceUtility;
import org.rhq.enterprise.server.RHQConstants;
import org.rhq.enterprise.server.auth.SessionManager;
import org.rhq.enterprise.server.content.ContentSourceManagerBean;
import org.rhq.enterprise.server.core.comm.ServerCommunicationsServiceMBean;
import org.rhq.enterprise.server.plugin.pc.ServerPluginService;
import org.rhq.enterprise.server.plugin.pc.ServerPluginServiceManagement;
import org.rhq.enterprise.server.scheduler.SchedulerService;
import org.rhq.enterprise.server.scheduler.SchedulerServiceMBean;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * This is the abstract test base for server jar tests.
 *
 * @author Greg Hinkle
 */
public abstract class AbstractEJB3Test extends AssertJUnit {
    //@BeforeSuite(groups = {"integration.ejb3","PERF"}) // TODO investigate again
    @BeforeSuite(alwaysRun = true)
    public static void startupEmbeddedJboss() throws Exception {
        // Setting content location to the tmp dir
        System.setProperty(ContentSourceManagerBean.FILESYSTEM_PROPERTY, System.getProperty("java.io.tmpdir"));

        System.out.println("Starting ejb3...");
        String deployDir = System.getProperty("deploymentDirectory", "target/classes");
        System.out.println("Loading EJB3 deployments from directory: " + deployDir);
        try {
            EJB3StandaloneBootstrap.boot(null);
            //         EJB3StandaloneBootstrap.scanClasspath();

            /*Properties p = System.getProperties();
             * for (Object name : p.keySet()) { System.out.println(" " + name + " = " +
             * p.getProperty((String)name));}*/

            System.err.println("...... embedded-jboss-beans deployed....");

            // Add all EJBs found in the archive that has this file
            EJB3StandaloneDeployer deployer = EJB3StandaloneBootstrap.createDeployer(); //new EJB3StandaloneDeployer();

            deployer.setClassLoader(AbstractEJB3Test.class.getClassLoader());
            deployer.getArchivesByResource().add("META-INF/persistence.xml");
            deployer.getArchivesByResource().add("META-INF/ejb-jar.xml");

            /*
             *       File core = new File(deployDir, "on-core-domain-ejb.ejb3");      if (!core.exists())
             * System.err.println("Deployment directory does not exist: " + core.getAbsolutePath());
             * deployer.getArchives().add(core.toURI().toURL());
             *
             * File server = new File(deployDir, "on-enterprise-server-ejb.ejb3");      if (!server.exists())
             * System.err.println("Deployment directory does not exist: " + server.getAbsolutePath());
             * deployer.getArchives().add(server.toURI().toURL());
             *
             */

            System.err.println("...... deploying MM ejb3.....");
            System.err.println("...... ejb3 deployed....");

            // Deploy everything we got
            //deployer.setKernel(EJB3StandaloneBootstrap.getKernel());
            deployer.create();
            System.err.println("...... deployer created....");

            deployer.start();
            System.err.println("...... deployer started....");

            System.err.println("...... start statistics");
            SessionFactory sessionFactory = PersistenceUtility.getHibernateSession(getEntityManager())
                .getSessionFactory();
            stats = sessionFactory.getStatistics();
            stats.setStatisticsEnabled(true);

        } catch (Throwable t) {
            // Catch RuntimeExceptions and Errors and dump their stack trace, because Surefire will completely swallow them
            // and throw a cryptic NPE (see http://jira.codehaus.org/browse/SUREFIRE-157)!
            t.printStackTrace();
            throw new RuntimeException(t);
        }
    }

    //@Configuration(groups = "integration.ejb3", afterSuite = true)
    @AfterSuite
    public static void shutdownEmbeddedJboss() {
        EJB3StandaloneBootstrap.shutdown();
    }

    private static long start;

    @BeforeMethod
    public static void startTest() {
        if (DatabaseTypeFactory.getDefaultDatabaseType() == null) {
            try {
                Connection conn = getConnection();
                DatabaseTypeFactory.setDefaultDatabaseType(DatabaseTypeFactory.getDatabaseType(conn));
            } catch (Exception e) {
                System.err.println("!!! WARNING !!! cannot set default database type, some tests may fail");
                e.printStackTrace();
            }
        }

        if (stats!=null)
            start = stats.getQueryExecutionCount();
        else
            start = 0;
    }

    public static Connection getConnection() throws SQLException {
        return LookupUtil.getDataSource().getConnection();
    }

    @AfterMethod
    public static void endTest() {
        //System.out.println("Connections used: " + (stats.getQueryExecutionCount() - start));
    }

    private TransactionManager tm;
    private MBeanServer dummyJBossMBeanServer;
    private static Statistics stats;

    public TransactionManager getTransactionManager() {
        try {
            tm = (TransactionManager) getInitialContext().lookup("java:/TransactionManager");
            return tm;
        } catch (NamingException e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to load transaction manager", e);
        }
    }

    public static EntityManager getEntityManager() {
        try {
            return ((EntityManagerFactory) getInitialContext().lookup(RHQConstants.ENTITY_MANAGER_JNDI_NAME))
                .createEntityManager();
        } catch (NamingException e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to load entity manager", e);
        }
    }

    public static InitialContext getInitialContext() {
        Hashtable<String, String> env = new Hashtable<String, String>();
        env.put("java.naming.factory.initial", "org.jnp.interfaces.LocalOnlyContextFactory");
        env.put("java.naming.factory.url.pkgs", "org.jboss.naming:org.jnp.interfaces");
        try {
            return new InitialContext(env);
        } catch (NamingException e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to load initial context", e);
        }
    }

    public boolean isPostgres() throws Exception {
        return DatabaseTypeFactory.getDatabaseType(getInitialContext(), RHQConstants.DATASOURCE_JNDI_NAME) instanceof PostgresqlDatabaseType;
    }

    /**
     * This creates a session for the given user and associates that session with the subject. You can test the security
     * annotations by creating sessions for different users with different permissions.
     *
     * @param subject a JON subject
     */
    public void createSession(Subject subject) {
        SessionManager.getInstance().put(subject);
    }

    /**
     * Returns an MBeanServer that simulates the JBossAS MBeanServer.
     *
     * @return MBeanServer instance
     */
    public MBeanServer getJBossMBeanServer() {
        if (dummyJBossMBeanServer == null) {
            dummyJBossMBeanServer = MBeanServerFactory.createMBeanServer("jboss");
            MBeanServerLocator.setJBoss(dummyJBossMBeanServer);
        }

        return dummyJBossMBeanServer;
    }

    /**
     * If you need to test round trips from server to agent and back, you first must install the server communications
     * service that houses all the agent clients. Call this method and add your test agent services to the public fields
     * in the returned object.
     *
     * @return the object that will house your test agent service impls and the agent clients.
     *
     * @throws RuntimeException
     */
    public TestServerCommunicationsService prepareForTestAgents() {
        try {
            MBeanServer mbs = getJBossMBeanServer();
            if (mbs.isRegistered(ServerCommunicationsServiceMBean.OBJECT_NAME)) {
                mbs.unregisterMBean(ServerCommunicationsServiceMBean.OBJECT_NAME);
            }
            TestServerCommunicationsService testAgentContainer = new TestServerCommunicationsService();
            mbs.registerMBean(testAgentContainer, ServerCommunicationsServiceMBean.OBJECT_NAME);
            return testAgentContainer;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Call this after your tests have finished. You only need to call this if your test previously called
     * {@link #prepareForTestAgents()}.
     */
    public void unprepareForTestAgents() {
        try {
            if (dummyJBossMBeanServer != null) {
                dummyJBossMBeanServer.unregisterMBean(ServerCommunicationsServiceMBean.OBJECT_NAME);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private ServerPluginService serverPluginService;

    /**
     * If you need to test server plugins, you must first prepare the server plugin service.
     * After this returns, the caller must explicitly start the PC by using the appropriate API
     * on the given mbean; this method will only start the service, it will NOT start the master PC.
     *
     * @param testServiceMBean the object that will house your test server plugins
     *
     * @throws RuntimeException
     */
    public void prepareCustomServerPluginService(ServerPluginService testServiceMBean) {
        try {
            MBeanServer mbs = getJBossMBeanServer();
            testServiceMBean.start();
            mbs.registerMBean(testServiceMBean, ServerPluginServiceManagement.OBJECT_NAME);
            serverPluginService = testServiceMBean;
            return;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void unprepareServerPluginService() throws Exception {
        if (serverPluginService != null) {
            serverPluginService.stopMasterPluginContainer();
            serverPluginService.stop();
            getJBossMBeanServer().unregisterMBean(ServerPluginService.OBJECT_NAME);
            serverPluginService = null;
        }
    }

    private SchedulerService schedulerService;

    public SchedulerService getSchedulerService() {
        return schedulerService;
    }

    public void prepareScheduler() {
        try {
            if (schedulerService != null) {
                return;
            }

            Properties quartzProps = new Properties();
            quartzProps.load(this.getClass().getClassLoader().getResourceAsStream("test-scheduler.properties"));

            schedulerService = new SchedulerService();
            schedulerService.setQuartzProperties(quartzProps);
            schedulerService.start();
            getJBossMBeanServer().registerMBean(schedulerService, SchedulerServiceMBean.SCHEDULER_MBEAN_NAME);
            schedulerService.startQuartzScheduler();
            return;
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    public void unprepareScheduler() throws Exception {
        if (schedulerService != null) {
            schedulerService.stop();
            getJBossMBeanServer().unregisterMBean(SchedulerServiceMBean.SCHEDULER_MBEAN_NAME);
            schedulerService = null;
        }
    }
}
