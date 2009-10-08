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

/**
 * The MBean management interface that defines the lifecycle methods for the {@link ContentSourcePluginService} service.
 *
 * @author John Mazzitelli
 */
public interface ContentSourcePluginServiceManagement {
    String OBJECT_NAME_STR = "rhq.serverplugin:service=ContentSourcePluginService";

    /**
     * Starts the service but will <b>not</b> start the {@link #getPluginContainer() plugin container}. After the server
     * fully initializes, it should only then {@link #startPluginContainer() start the plugin container}.
     */
    void start();

    /**
     * Shuts down this service along with the {@link #stopPluginContainer() plugin container}.
     */
    void stop();

    /**
     * Starts the {@link #getPluginContainer() plugin container} which will load in all plugins and start them. You
     * cannot start the plugin container unless this service has {@link #start() been started}. If the plugin container
     * {@link #isPluginContainerStarted() is already started}, this does nothing and returns.
     */
    void startPluginContainer();

    /**
     * Stops the {@link #getPluginContainer() plugin container} which will shuts down all plugins. If the plugin
     * container {@link #isPluginContainerStarted() is already shutdown}, this does nothing and returns.
     */
    void stopPluginContainer();

    /**
     * Convienence method that first does a {@link #stopPluginContainer()} and then a {@link #startPluginContainer()}.
     */
    void restartPluginContainer();

    /**
     * Returns the server side content source plugin container that will be responsible for managing all plugins and
     * their classloaders.
     *
     * @return the plugin container, if started. Will be <code>null</code> if not started
     */
    ContentSourcePluginContainer getPluginContainer();

    /**
     * Returns <code>true</code> if this service has been started. This does not necessarily mean the
     * {@link #getPluginContainer() plugin container} has be started - see {@link #isPluginContainerStarted()} for that.
     *
     * @return <code>true</code> if this service has been started
     */
    boolean isStarted();

    /**
     * Returns <code>true</code> if the {@link #getPluginContainer() plugin container} has be started. Note that this is
     * <b>not</b> an indication if this service has started - see {@link #isStarted()} for that. But, if the plugin
     * container has been started, then by definition this service has also been started.
     *
     * @return <code>true</code> if the plugin container has been started
     */
    boolean isPluginContainerStarted();
}