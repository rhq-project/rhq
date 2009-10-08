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
package org.rhq.enterprise.server.plugin.content;

import java.io.File;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.annotation.ejb.Management;
import org.jboss.annotation.ejb.Service;
import org.jboss.system.server.ServerConfig;
import org.rhq.enterprise.server.RHQConstants;

/**
 * This is the singleton management service responsible for managing the lifecycle of the
 * {@link ContentSourcePluginContainer}. It will be started when the entire server starts and shutdown when the entire
 * server shuts down. It has a management interface to allow it to be recycled on demand when desired.
 *
 * @author John Mazzitelli
 */
@Management(ContentSourcePluginServiceManagement.class)
@Service(objectName = ContentSourcePluginServiceManagement.OBJECT_NAME_STR)
public class ContentSourcePluginService implements ContentSourcePluginServiceManagement {
    private static final Log log = LogFactory.getLog(ContentSourcePluginService.class);

    private boolean started;
    private ContentSourcePluginContainer pluginContainer;

    public synchronized void start() {
        log.debug("The content source plugin service has been deployed (but plugin container will not be started yet)");
        this.started = true;
        return;
    }

    public synchronized void startPluginContainer() {
        if (!this.started) {
            throw new IllegalStateException(
                "The content source plugin service is not started - cannot start the plugin container");
        }

        // only initialize if not already started; if already started/initialized, just ignore
        if (this.pluginContainer == null) {
            log.debug("The content source plugin service is now starting the content server plugin container");
            this.pluginContainer = createPluginContainer();
        }

        return;
    }

    public synchronized void stopPluginContainer() {
        if (this.started && (this.pluginContainer != null)) {
            log
                .info("The content source plugin service is now stopping - the content server plugin container will be shutdown now");

            this.pluginContainer.shutdown();
            this.pluginContainer = null;
        }

        return;
    }

    public synchronized void stop() {
        if (this.started) {
            stopPluginContainer();
            this.started = false;
        }

        return;
    }

    public synchronized void restartPluginContainer() {
        stopPluginContainer();
        startPluginContainer();
    }

    public ContentSourcePluginContainer getPluginContainer() {
        return this.pluginContainer;
    }

    public boolean isStarted() {
        return this.started;
    }

    public boolean isPluginContainerStarted() {
        return this.pluginContainer != null;
    }

    /**
     * This will create, configure and initialize the plugin container and return it.
     *
     * <p>This is protected to allow subclasses to override the PC that is created by this service (mainly to support
     * tests).</p>
     *
     * @return the PC that this service will use
     */
    protected ContentSourcePluginContainer createPluginContainer() {
        ContentSourcePluginContainer pc = new ContentSourcePluginContainer();

        ContentSourcePluginContainerConfiguration config = new ContentSourcePluginContainerConfiguration();

        String pluginDirStr = System.getProperty(ServerConfig.SERVER_HOME_DIR);
        config
            .setPluginDirectory(new File(pluginDirStr, "deploy/" + RHQConstants.EAR_FILE_NAME + "/rhq-serverplugins"));

        String tmpDirStr = System.getProperty(ServerConfig.SERVER_TEMP_DIR);
        config.setTemporaryDirectory(new File(tmpDirStr));

        pc.initialize(config);

        return pc;
    }
}