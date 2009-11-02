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

/**
 * The MBean management interface that defines the lifecycle methods for the {@link ServerPluginService} service.
 * This essentially is the server that starts and stops the {@link #getMasterPluginContainer() master plugin container}
 * that manages all server-side plugins.
 *
 * @author John Mazzitelli
 */
public interface ServerPluginServiceManagement {
    String OBJECT_NAME_STR = "rhq.serverplugin:service=ServerPluginService";

    /**
     * Starts the service but will <b>not</b> start the {@link #getMasterPluginContainer() master plugin container}. After the server
     * fully initializes, it should only then {@link #startMasterPluginContainer() start the master server-side plugin container}.
     */
    void start();

    /**
     * Shuts down this service along with the {@link #stopMasterPluginContainer() master plugin container}.
     */
    void stop();

    /**
     * Starts the {@link #getMasterPluginContainer() master plugin container} which will load in all plugins and start them. You
     * cannot start the plugin container unless this service has {@link #start() been started}. If the master plugin container
     * {@link #isMasterPluginContainerStarted() is already started}, this does nothing and returns.
     */
    void startMasterPluginContainer();

    /**
     * Stops the {@link #getMasterPluginContainer() master plugin container} which will shuts down all plugins. If the master 
     * plugin container {@link #isMasterPluginContainerStarted() is already shutdown}, this does nothing and returns.
     */
    void stopMasterPluginContainer();

    /**
     * Convienence method that first does a {@link #stopMasterPluginContainer()} and then a {@link #startMasterPluginContainer()}.
     */
    void restartMasterPluginContainer();

    /**
     * Returns the master server plugin container that will be responsible for managing all plugins of all types and their classloaders.
     *
     * @return the master plugin container, if started. Will be <code>null</code> if not started
     */
    MasterServerPluginContainer getMasterPluginContainer();

    /**
     * Returns <code>true</code> if this service has been started. This does not necessarily mean the
     * {@link #getMasterPluginContainer() master plugin container} has be started - see {@link #isMasterPluginContainerStarted()} for that.
     *
     * @return <code>true</code> if this service has been started
     */
    boolean isStarted();

    /**
     * Returns <code>true</code> if the {@link #getMasterPluginContainer() master plugin container} has be started.
     * Note that this is <b>not</b> an indication if this service has started - see {@link #isStarted()} for that.
     * But, if the master plugin container has been started, then by definition this service has also been started.
     *
     * @return <code>true</code> if the master plugin container has been started
     */
    boolean isMasterPluginContainerStarted();
}