/*
 * RHQ Management Platform
 * Copyright (C) 2005-2013 Red Hat, Inc.
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
package org.rhq.enterprise.server.plugin.pc;

import java.io.File;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ejb.ConcurrencyManagement;
import javax.ejb.ConcurrencyManagementType;
import javax.ejb.LocalBean;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.enterprise.server.util.JMXUtil;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * This is the singleton management service responsible for managing the lifecycle of the
 * {@link MasterServerPluginContainer}. It will be started when the entire server starts and shutdown when the entire
 * server shuts down. It has a management interface to allow it to be recycled on demand when desired.
 *
 * @author John Mazzitelli
 */
@Singleton
@Startup
@LocalBean
@ConcurrencyManagement(ConcurrencyManagementType.BEAN)
@TransactionAttribute(TransactionAttributeType.SUPPORTS)
public class ServerPluginService implements ServerPluginServiceMBean {
    private static final Log LOG = LogFactory.getLog(ServerPluginService.class);

    private boolean started;
    private MasterServerPluginContainer masterPluginContainer;

    public synchronized void start() {
        LOG.debug("The server plugin service has been deployed (but master plugin container will not be started yet)");
        this.started = true;
        return;
    }

    public synchronized void startMasterPluginContainer() {
        if (!this.started) {
            throw new IllegalStateException(
                "The server plugin service is not started - cannot start the master plugin container");
        }

        // only initialize if not already started; if already started/initialized, just ignore
        if (this.masterPluginContainer == null) {
            LOG.debug("The server plugin service is now starting the master server plugin container");
            this.masterPluginContainer = createMasterPluginContainer();
            this.masterPluginContainer.scheduleAllPluginJobs();
        }

        return;
    }

    public synchronized void stopMasterPluginContainer() {
        if (this.started && (this.masterPluginContainer != null)) {
            LOG.info("The server plugin service is now stopping - the master plugin container will be shutdown now");

            this.masterPluginContainer.shutdown();
            this.masterPluginContainer = null;
        }

        return;
    }

    public synchronized void stop() {
        if (this.started) {
            stopMasterPluginContainer();
            this.started = false;
        }

        return;
    }

    public synchronized void restartMasterPluginContainer() {
        stopMasterPluginContainer();
        startMasterPluginContainer();
    }

    public synchronized void startMasterPluginContainerWithoutSchedulingJobs() {
        if (!this.started) {
            throw new IllegalStateException(
                "The server plugin service is not started - cannot start the master plugin container!");
        }

        // only initialize if not already started; if already started/initialized, just ignore
        if (this.masterPluginContainer == null) {
            LOG.debug("The server plugin service is now starting the master server plugin container!");
            this.masterPluginContainer = createMasterPluginContainer();
        }

        return;
    }

    public MasterServerPluginContainer getMasterPluginContainer() {
        return this.masterPluginContainer;
    }

    public boolean isStarted() {
        return this.started;
    }

    public boolean isMasterPluginContainerStarted() {
        return this.masterPluginContainer != null;
    }

    public File getServerPluginsDirectory() {
        File earDir = LookupUtil.getCoreServer().getEarDeploymentDir();
        File pluginDir = new File(earDir, "rhq-serverplugins");
        return pluginDir;
    }

    /**
     * This will create, configure and initialize the plugin container and return it.
     *
     * <p>This is protected to allow subclasses to override the PC that is created by this service (mainly to support
     * tests).</p>
     *
     * @return the PC that this service will use
     */
    protected MasterServerPluginContainer createMasterPluginContainer() {
        MasterServerPluginContainer pc = new MasterServerPluginContainer();

        File pluginDir = getServerPluginsDirectory();

        File serverDataDir = LookupUtil.getCoreServer().getJBossServerDataDir();
        File dataDir = new File(serverDataDir, "rhq-masterpc-server-plugins");
        dataDir.mkdirs(); // make sure the data directory exists

        File tmpDir = LookupUtil.getCoreServer().getJBossServerTempDir();

        // determine what things to hide from our war classloader
        StringBuilder defaultRegex = new StringBuilder();
        defaultRegex.append("(org\\.apache\\.tools\\..*)"); // ant.jar

        MasterServerPluginContainerConfiguration config;
        config = new MasterServerPluginContainerConfiguration(pluginDir, dataDir, tmpDir, defaultRegex.toString());
        pc.initialize(config);
        return pc;
    }

    @PostConstruct
    private void init() {
        JMXUtil.registerMBean(this, OBJECT_NAME);
        start();
    }

    @PreDestroy
    private void destroy() {
        stop();
        JMXUtil.unregisterMBeanQuietly(OBJECT_NAME);
    }
}
