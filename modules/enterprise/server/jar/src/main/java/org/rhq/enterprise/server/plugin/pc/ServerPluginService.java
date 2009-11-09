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
package org.rhq.enterprise.server.plugin.pc;

import java.io.File;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.jboss.annotation.ejb.Management;
import org.jboss.annotation.ejb.Service;
import org.jboss.system.server.ServerConfig;

import org.rhq.enterprise.server.RHQConstants;

/**
 * This is the singleton management service responsible for managing the lifecycle of the
 * {@link MasterServerPluginContainer}. It will be started when the entire server starts and shutdown when the entire
 * server shuts down. It has a management interface to allow it to be recycled on demand when desired.
 *
 * @author John Mazzitelli
 */
@Management(ServerPluginServiceManagement.class)
@Service(objectName = ServerPluginServiceManagement.OBJECT_NAME_STR)
public class ServerPluginService implements ServerPluginServiceManagement {
    private static final Log log = LogFactory.getLog(ServerPluginService.class);

    private boolean started;
    private MasterServerPluginContainer masterPluginContainer;

    public synchronized void start() {
        log.debug("The server plugin service has been deployed (but master plugin container will not be started yet)");
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
            log.debug("The server plugin service is now starting the master server plugin container");
            this.masterPluginContainer = createMasterPluginContainer();
        }

        return;
    }

    public synchronized void stopMasterPluginContainer() {
        if (this.started && (this.masterPluginContainer != null)) {
            log.info("The server plugin service is now stopping - the master plugin container will be shutdown now");

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

    public MasterServerPluginContainer getMasterPluginContainer() {
        return this.masterPluginContainer;
    }

    public boolean isStarted() {
        return this.started;
    }

    public boolean isMasterPluginContainerStarted() {
        return this.masterPluginContainer != null;
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

        String pluginDirStr = System.getProperty(ServerConfig.SERVER_HOME_DIR);
        File pluginDir = new File(pluginDirStr, "deploy/" + RHQConstants.EAR_FILE_NAME + "/rhq-serverplugins");

        String dataDirStr = System.getProperty(ServerConfig.SERVER_DATA_DIR);
        File dataDir = new File(dataDirStr, "server-plugins");
        dataDir.mkdirs(); // make sure the data directory exists

        String tmpDirStr = System.getProperty(ServerConfig.SERVER_TEMP_DIR);
        File tmpDir = new File(tmpDirStr);

        // TODO: determine what things to hide from our war classloader
        //StringBuilder defaultRegex = new StringBuilder();
        //defaultRegex.append("(package\\.with\\.classes\\.to\\.hide\\..*)|");

        MasterServerPluginContainerConfiguration config;
        config = new MasterServerPluginContainerConfiguration(pluginDir, dataDir, tmpDir, null);
        pc.initialize(config);

        return pc;
    }
}